variable "region" {
  description = "AWS region"
  default     = "eu-south-1"
}

variable "env" {
  description = "Environment name"
  default     = "vm-dev"
}

variable "container_image" {
  description = "Container image"
  default     = "amirkukei/ba-cpu:2025-12-31-02"
}

variable "egress_max_mb" {
  default = 100
}

variable "run_id" {
  description = "Tag for metrics to distinguish test runs"
  default     = "aws-vm-01"
}

variable "instance_type" {
  description = "EC2 instance type"
  default     = "t3.small"
}

variable "key_name" {
  description = "Existing AWS EC2 Key Pair name (for SSH)"
  type        = string
}

variable "ssh_cidr" {
  description = "CIDR allowed to SSH (22). Put your public IP/32."
  type        = string
  default     = "0.0.0.0/0"
}

variable "allocate_eip" {
  description = "Allocate Elastic IP for stable public IP"
  type        = bool
  default     = true
}
