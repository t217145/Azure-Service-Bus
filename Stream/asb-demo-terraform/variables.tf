variable "resource_group_name" {
  type        = string
  description = "The name of resource group."
  default     = "mfc-asb-demo"
}

variable "location" {
  type        = string
  description = "The Azure region where all resources in this example should be created."
  default     = "eastasia"
}

variable "tag_domain" {
  type        = string
  description = "The value of resource tag - domain."
  default     = "Customer"
}

variable "tag_owner" {
  type        = string
  description = "The value of resource tag - owner."
  default     = "cyrus_kc_cheng@manulife.com"
}

variable "principal_id" {
  type        = string
  description = "The value of principal id (object id)."
  default     = "3942a5ee-4d31-44f4-af98-698d24c99b62"
}

variable "asb_ns_name" {
  type        = string
  description = "The name of Azure Service Bus Namespace."
  default     = "mfc-ess-demo"
}

variable "topic_name" {
  type        = string
  description = "The name of topic."
  default     = "test-topic"
}

variable "topic_size" {
  type        = number
  description = "The topic size in Metabyte."
  default     = 1024
}

variable "subscription_name" {
  type        = string
  description = "The name of topic subscription."
  default     = "test-topic-subscription"
}