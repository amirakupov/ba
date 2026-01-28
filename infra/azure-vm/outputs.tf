output "vm_public_ip" {
  value = var.allocate_static_ip ? azurerm_public_ip.pip[0].ip_address : null
}

output "vm_http_url" {
  value = var.allocate_static_ip ? "http://${azurerm_public_ip.pip[0].ip_address}:8080" : null
}

output "vm_grpc_target" {
  value = var.allocate_static_ip ? "${azurerm_public_ip.pip[0].ip_address}:9091" : null
}

output "vm_ssh" {
  value = var.allocate_static_ip ? "ssh ${var.admin_username}@${azurerm_public_ip.pip[0].ip_address}" : null
}
