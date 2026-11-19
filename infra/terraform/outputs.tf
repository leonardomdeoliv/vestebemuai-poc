output "ip_publico" {
  value = aws_instance.poc.public_ip
}

output "instancia" {
  value = aws_instance.poc.id
}

output "ip_liberado" {
  value = local.meu_ip
}

output "ssh" {
  value = "ssh -i ${local_sensitive_file.pem.filename} ec2-user@${aws_instance.poc.public_ip}"
}

output "enderecos" {
  value = {
    api          = "http://${aws_instance.poc.public_ip}:8080/actuator/health"
    notificacoes = "http://${aws_instance.poc.public_ip}:8081/actuator/metrics"
    mailpit      = "http://${aws_instance.poc.public_ip}:8025"
    rabbitmq     = "http://${aws_instance.poc.public_ip}:15672"
  }
}
