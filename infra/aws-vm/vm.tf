data "aws_ami" "al2023" {
  most_recent = true
  owners      = ["amazon"]

  filter {
    name   = "name"
    values = ["al2023-ami-*-kernel-6.1-x86_64"]
  }
}

resource "aws_instance" "ba_vm" {
  ami                    = data.aws_ami.al2023.id
  instance_type          = var.instance_type
  subnet_id              = aws_subnet.ba_subnet.id
  vpc_security_group_ids = [aws_security_group.ba_sg.id]

  user_data = <<-EOT
    #!/bin/bash
    set -xe

    dnf update -y
    dnf install -y docker

    systemctl enable docker
    systemctl start docker

    docker rm -f ba-cpu || true

    docker run -d \
      --name ba-cpu \
      -p 8080:8080 \
      -p 9091:9091 \
      -e "EGRESS_MAX_MB=${var.egress_max_mb}" \
      -e "CLOUD_PROVIDER=aws" \
      -e "DEPLOY_PROFILE=vm" \
      -e "RUN_ID=${var.run_id}" \
      ${var.container_image}
  EOT

  tags = {
    Name = "ba-cpu-${var.env}"
  }
}

resource "aws_eip" "ba_eip" {
  count  = var.allocate_eip ? 1 : 0
  domain = "vpc"

  tags = {
    Name = "ba-eip-${var.env}"
  }
}

resource "aws_eip_association" "ba_eip_assoc" {
  count         = var.allocate_eip ? 1 : 0
  instance_id   = aws_instance.ba_vm.id
  allocation_id = aws_eip.ba_eip[0].id
}

