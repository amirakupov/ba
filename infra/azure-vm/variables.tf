variable "location" {
  description = "Azure region"
  type        = string
  default     = "italynorth"
}

variable "env" {
  description = "Environment name"
  type        = string
  default     = "vm-dev"
}

variable "container_image" {
  description = "Container image for BA CPU service"
  type        = string
  default     = "amirkukei/ba-cpu:2025-12-31-02"
}

variable "egress_max_mb" {
  type    = number
  default = 100
}

variable "run_id" {
  description = "Tag for metrics to distinguish test runs"
  type        = string
  default     = "azure-vm-01"
}

variable "subscription_id" {
  type        = string
  description = "Azure Subscription ID"
}


variable "vm_size" {
  description = "Azure VM size"
  type        = string
  default     = "Standard_B2s_v2"
}

variable "admin_username" {
  description = "VM admin username"
  type        = string
  default     = "azureuser"
}

variable "ssh_public_key_path" {
  description = "Path to your SSH public key (e.g., ~/.ssh/id_rsa.pub)"
  type        = string
}

variable "ssh_cidrs" {
  description = "List of CIDRs allowed for SSH (22). Put your public IP/32."
  type        = list(string)
  default     = ["0.0.0.0/0"]
}

variable "allocate_static_ip" {
  description = "Create a Static Public IP (stable like AWS EIP)"
  type        = bool
  default     = true
}
