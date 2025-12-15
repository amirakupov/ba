resource "google_compute_instance" "ba_vm" {
  name         = "ba-cpu-${var.env}"
  machine_type = "e2-small"
  zone         = var.zone

  service_account {
    email  = google_service_account.vm_sa.email
    scopes = ["https://www.googleapis.com/auth/cloud-platform"]
  }

  tags = ["ba-cpu"]

  boot_disk {
    initialize_params {
      image = "projects/debian-cloud/global/images/family/debian-12"
      size  = 20
    }
  }
  network_interface {
    network = google_compute_network.ba_vpc.self_link

    access_config {}
  }
  metadata_startup_script = <<-EOT
    #!/bin/bash
    set -xe

    apt-get update -y
    apt-get install -y docker.io

    systemctl enable docker
    systemctl start docker

    docker rm -f ba-cpu || true

    docker run -d \
      --name ba-cpu \
      -p 8080:8080 \
      -p 9091:9091 \
      -e "EGRESS_MAX_MB=${var.egress_max_mb}" \
      -e "CLOUD_PROVIDER=gcp" \
      -e "DEPLOY_PROFILE=vm" \
      -e "RUN_ID=${var.run_id}" \
      ${var.container_image}
  EOT

  depends_on = [
    google_project_service.compute
  ]
}
