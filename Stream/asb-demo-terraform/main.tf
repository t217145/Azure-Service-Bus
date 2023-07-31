terraform {
  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = "3.9.0"
    }
    /* azurecaf = {
      source  = "aztfmod/azurecaf"
      version = "1.2.16"
    }     */
  }
}

provider "azurerm" {
  features {}
}

data "azurerm_subscription" "current" { }

/* resource "azurecaf_name" "resource_group" {
  name          = var.application_name
  resource_type = "azurerm_resource_group"
  random_length = 5
  clean_input   = true
} */

resource "azurerm_resource_group" "main" {
  name     = var.resource_group_name #azurecaf_name.resource_group.result
  location = var.location

  tags = {
    "domain"         = var.tag_domain
    "owner"          = var.tag_owner
  }
}

/* resource "azurecaf_name" "azurecaf_name_servicebus" {
  name          = var.application_name
  resource_type = "azurerm_servicebus_namespace"
  random_length = 5
  clean_input   = true
} */

resource "azurerm_servicebus_namespace" "servicebus_namespace" {
  name                = var.asb_ns_name #azurecaf_name.azurecaf_name_servicebus.result
  location            = var.location
  resource_group_name = azurerm_resource_group.main.name

  sku                 = "Standard"
  zone_redundant      = false

  local_auth_enabled = false

  tags = {
    "domain"         = var.tag_domain
    "owner"          = var.tag_owner
  }
}

resource "azurerm_servicebus_topic" "servicebus_topic" {
  name                                    = var.topic_name
  namespace_id                            = azurerm_servicebus_namespace.servicebus_namespace.id

  max_size_in_megabytes                   = var.topic_size
  default_message_ttl                     = "P0DT1H0M0S" # 1 hour

  requires_duplicate_detection            = true
  duplicate_detection_history_time_window = "PT3M" # 3 minutes
  #max_message_size_in_kilobytes           = 256 #256kb premium only
}

resource "azurerm_servicebus_subscription" "servicebus_subscription" {
  name                                      = var.subscription_name
  topic_id                                  = azurerm_servicebus_topic.servicebus_topic.id
  max_delivery_count                        = 10
  dead_lettering_on_message_expiration      = true
  dead_lettering_on_filter_evaluation_error = true
  lock_duration                             = "P0DT0H0M30S"
  default_message_ttl                       = "PT5M" # 3 minutes
}

resource "azurerm_role_assignment" "role_servicebus_data_receiver" {
  scope                = azurerm_servicebus_topic.servicebus_topic.id
  role_definition_name = "Azure Service Bus Data Receiver"
  principal_id         = var.principal_id
}

resource "azurerm_role_assignment" "role_servicebus_data_sender" {
  scope                = azurerm_servicebus_topic.servicebus_topic.id
  role_definition_name = "Azure Service Bus Data Sender"
  principal_id         = var.principal_id
}