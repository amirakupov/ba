resource "google_compute_network" "ba_vpc" {
  name = "ba-vpc"
  auto_create_subnetworks = true
}
resource "google_compute_firewall" "ba_allow_http_grpc" {
  name  = "ba-cpu-allow-8080-9091"
  network = google_compute_network.ba_vpc.name

  direction     = "INGRESS"
  source_ranges = ["0.0.0.0/0"]

  allow {
    protocol = "tcp"
    ports    = ["8080", "9091"]
  }

  target_tags = ["ba-cpu"]
}
resource "google_compute_firewall" "allow_ssh_iap" {
  name    = "ba-allow-ssh-iap"
  network = google_compute_network.ba_vpc.name

  direction     = "INGRESS"
  source_ranges = ["35.235.240.0/20"]

  allow {
    protocol = "tcp"
    ports    = ["22"]
  }

  target_tags = ["ba-cpu"]
}
