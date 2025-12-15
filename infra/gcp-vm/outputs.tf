output "vm_external_ip" {
  description = "Public IP address of the BA CPU VM"
  value       = google_compute_instance.ba_vm.network_interface[0].access_config[0].nat_ip
}

output "vm_http_url" {
  description = "HTTP URL for the BA CPU service on the VM"
  value       = "http://${google_compute_instance.ba_vm.network_interface[0].access_config[0].nat_ip}:8080"
}

output "vm_grpc_target" {
  description = "Host:port for gRPC"
  value       = "${google_compute_instance.ba_vm.network_interface[0].access_config[0].nat_ip}:9091"
}