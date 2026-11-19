terraform {
  required_version = ">= 1.6"
  required_providers {
    aws   = { source = "hashicorp/aws", version = "~> 5.70" }
    tls   = { source = "hashicorp/tls", version = "~> 4.0" }
    local = { source = "hashicorp/local", version = "~> 2.5" }
    http  = { source = "hashicorp/http", version = "~> 3.4" }
  }
}

provider "aws" {
  region = var.regiao
  default_tags {
    tags = {
      Projeto = "vestebemuai-poc"
      Sprint  = "3"
      Dono    = "leonardomdeoliv"
      Criado  = "terraform"
    }
  }
}
