package io.pleo.antaeus.core.exceptions

class InvoiceStatusUpdateFailedException(id: Int, oldStatus: String, newStatus: String) :
        Exception("Failed to update invoice '$id' status from '$oldStatus' to '$newStatus'")