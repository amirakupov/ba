terraform {
  required_providers {
    google = { source = "hashicorp/google", version = "~> 5.0" }
  }
}

provider "google" {
  project = var.project
  region  = var.region
}

variable "project" {}
variable "region"  { default = "europe-west1" }
variable "service_name" { default = "ba-cpu" }
variable "container_image" { description = "amirkukei/ba-cpu:latest" }
variable "max_instances" { default = 10 }
variable "concurrency"   { default = 80 }
resource "google_project_service" "run" {
  service = "run.googleapis.com"
}

resource "google_cloud_run_v2_service" "app" {
  name     = var.service_name
  location = var.region

  template {
    containers {
      image = var.container_image
      ports { container_port = 8080 }
      resources {
        limits = {
          cpu    = "1"
          memory = "512Mi"
        }
      }
      env {
        name  = "JAVA_OPTS"
        value = "-XX:+UseG1GC -XX:MaxRAMPercentage=70.0"
      }
    }
    scaling {
      max_instance_count = var.max_instances
    }
    max_instance_request_concurrency = var.concurrency
  }

  ingress = "INGRESS_TRAFFIC_ALL"

  depends_on = [google_project_service.run]
}


resource "google_cloud_run_service_iam_member" "invoker" {
  location = google_cloud_run_v2_service.app.location
  service  = google_cloud_run_v2_service.app.name
  role     = "roles/run.invoker"
  member   = "allUsers"
}

output "url" {
  value = google_cloud_run_v2_service.app.uri
}
