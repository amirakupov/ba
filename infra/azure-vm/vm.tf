resource "azurerm_public_ip" "pip" {
  count               = var.allocate_static_ip ? 1 : 0
  name                = "ba-pip-${var.env}"
  location            = azurerm_resource_group.rg.location
  resource_group_name = azurerm_resource_group.rg.name

  allocation_method = "Static"
  sku               = "Standard"

  tags = {
    Name = "ba-pip-${var.env}"
  }
}

resource "azurerm_network_interface" "nic" {
  name                = "ba-nic-${var.env}"
  location            = azurerm_resource_group.rg.location
  resource_group_name = azurerm_resource_group.rg.name

  ip_configuration {
    name                          = "ipcfg"
    subnet_id                     = azurerm_subnet.subnet.id
    private_ip_address_allocation = "Dynamic"
    public_ip_address_id          = var.allocate_static_ip ? azurerm_public_ip.pip[0].id : null
  }

  tags = {
    Name = "ba-nic-${var.env}"
  }
}

locals {
  cloud_init = <<-EOT
    #cloud-config
    package_update: true
    packages:
      - docker.io

    runcmd:
      - systemctl enable docker
      - systemctl start docker
      - docker rm -f ba-cpu || true
      - docker pull ${var.container_image}
      - >
        docker run -d --restart unless-stopped
        --name ba-cpu
        -p 8080:8080
        -p 9091:9091
        -e EGRESS_MAX_MB=${var.egress_max_mb}
        -e CLOUD_PROVIDER=azure
        -e DEPLOY_PROFILE=vm
        -e RUN_ID=${var.run_id}
        ${var.container_image}
  EOT
}

resource "azurerm_linux_virtual_machine" "vm" {
  name                = "ba-vm-${var.env}"
  location            = azurerm_resource_group.rg.location
  resource_group_name = azurerm_resource_group.rg.name
  size                = var.vm_size

  admin_username                  = var.admin_username
  disable_password_authentication = true

  network_interface_ids = [azurerm_network_interface.nic.id]

  admin_ssh_key {
    username   = var.admin_username
    public_key = file(var.ssh_public_key_path)
  }

  # Azure требует base64 для custom_data
  custom_data = base64encode(local.cloud_init)

  os_disk {
    name                 = "ba-osdisk-${var.env}"
    caching              = "ReadWrite"
    storage_account_type = "Standard_LRS"
  }

  source_image_reference {
    publisher = "Canonical"
    offer     = "0001-com-ubuntu-server-jammy"
    sku       = "22_04-lts-gen2"
    version   = "latest"
  }

  tags = {
    Name = "ba-vm-${var.env}"
  }
}
