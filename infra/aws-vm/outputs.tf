output "vm_public_ip" {
  value = var.allocate_eip ? aws_eip.ba_eip[0].public_ip : aws_instance.ba_vm.public_ip
}

output "vm_http_url" {
  value = "http://${var.allocate_eip ? aws_eip.ba_eip[0].public_ip : aws_instance.ba_vm.public_ip}:8080"
}

output "vm_grpc_target" {
  value = "${var.allocate_eip ? aws_eip.ba_eip[0].public_ip : aws_instance.ba_vm.public_ip}:9091"
}

