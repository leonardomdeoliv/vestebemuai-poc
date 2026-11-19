# S3.5 / ADR-004: uma EC2 temporaria com o mesmo docker-compose.yml da maquina local.
# Roteiro: terraform apply -> teste dos 100 pedidos -> prints -> terraform destroy (mesmo dia).

data "http" "meu_ip" {
  url = "https://checkip.amazonaws.com"
}

locals {
  meu_ip = var.meu_ip != "" ? var.meu_ip : "${chomp(data.http.meu_ip.response_body)}/32"
  # Portas da demonstracao: SSH, API do monolito, servico de notificacoes, Mailpit e painel do RabbitMQ
  portas = { ssh = 22, monolito = 8080, notificacoes = 8081, mailpit = 8025, rabbitmq = 15672 }
}

# Amazon Linux 2023 mais recente (x86_64), pelo parametro publico do SSM
data "aws_ssm_parameter" "al2023" {
  name = "/aws/service/ami-amazon-linux-latest/al2023-ami-kernel-default-x86_64"
}

data "aws_vpc" "padrao" {
  default = true
}

# Chave SSH gerada pelo Terraform; o arquivo .pem fica so na maquina do autor (fora do Git)
resource "tls_private_key" "ssh" {
  algorithm = "ED25519"
}

resource "aws_key_pair" "ssh" {
  key_name   = "vestebemuai-poc"
  public_key = tls_private_key.ssh.public_key_openssh
}

resource "local_sensitive_file" "pem" {
  filename        = "${path.module}/vestebemuai-poc.pem"
  content         = tls_private_key.ssh.private_key_openssh
  file_permission = "0600"
}

resource "aws_security_group" "poc" {
  name        = "vestebemuai-poc"
  description = "PoC VesteBemUai: portas da demonstracao so para o IP do autor"
  vpc_id      = data.aws_vpc.padrao.id

  dynamic "ingress" {
    for_each = local.portas
    content {
      description = ingress.key
      from_port   = ingress.value
      to_port     = ingress.value
      protocol    = "tcp"
      cidr_blocks = [local.meu_ip]
    }
  }

  egress {
    description = "Saida para baixar imagens Docker, dependencias Maven e o repositorio"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "aws_instance" "poc" {
  ami                         = data.aws_ssm_parameter.al2023.value
  instance_type               = var.tipo_instancia
  key_name                    = aws_key_pair.ssh.key_name
  vpc_security_group_ids      = [aws_security_group.poc.id]
  associate_public_ip_address = true

  root_block_device {
    volume_size           = 20
    volume_type           = "gp3"
    delete_on_termination = true
  }

  metadata_options {
    http_tokens = "required" # IMDSv2
  }

  user_data = templatefile("${path.module}/user-data.sh.tftpl", {
    repositorio = var.repositorio
    branch      = var.branch
  })
  user_data_replace_on_change = true

  tags = { Name = "vestebemuai-poc" }
}
