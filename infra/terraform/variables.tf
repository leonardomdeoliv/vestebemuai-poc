variable "regiao" {
  description = "Regiao da AWS"
  type        = string
  default     = "us-east-1"
}

variable "tipo_instancia" {
  description = "Tipo da EC2 (ADR-004: t3.medium, 2 vCPU e 4 GiB, cerca de US$ 0,04/h)"
  type        = string
  default     = "t3.medium"
}

variable "repositorio" {
  description = "Repositorio clonado na EC2"
  type        = string
  default     = "https://github.com/leonardomdeoliv/vestebemuai-poc.git"
}

variable "branch" {
  description = "Branch do repositorio que sobe na EC2"
  type        = string
  default     = "main"
}

variable "meu_ip" {
  description = "IP publico liberado no security group (CIDR /32). Vazio = descobre o IP atual automaticamente."
  type        = string
  default     = ""
}
