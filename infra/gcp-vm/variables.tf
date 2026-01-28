variable "project" {}

variable "region" {
  default = "europe-west8"
}

variable "zone" {
  default = "europe-west8-b"
}

variable "env" {
  description = "Environment name"
  default     = "vm-dev"
}

variable "container_image" {
  description = "Container image for BA CPU service"
  default     = "amirkukei/ba-cpu:2025-12-31-02"
}

variable "egress_max_mb" {
  default = 100
}

variable "run_id" {
  description = "Tag for metrics to distinguish test runs"
  default     = "vm-01"
}
