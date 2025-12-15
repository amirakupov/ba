resource "google_service_account" "vm_sa" {
  account_id   = "ba-cpu-vm-sa"
  display_name = "BA CPU VM Service Account"
}
