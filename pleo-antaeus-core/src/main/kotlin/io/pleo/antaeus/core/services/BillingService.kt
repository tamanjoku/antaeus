package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.InvoiceNotFoundException
import io.pleo.antaeus.core.exceptions.InvoiceStatusUpdateFailedException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import java.util.Timer
import java.util.Date
import java.util.TimerTask

class BillingService(
        private val invoiceService: InvoiceService,
        private val paymentProvider: PaymentProvider
) {

//    // Create a timer task here to kick off the function to execute the bills
//    inline fun Timer.schedule(
//            time: Date,
//            crossinline action: TimerTask.() -> Unit
//    ): TimerTask {
//
//    }

    /**
     * Fetches all pending invoices from the DB in batches of [batchSize] and attempts to pay these invoices.
     * Should the payment attempt fail, the error will be logged.
     */
    fun processPendingInvoices(batchSize: Int) {
        // Fetch the initial batch
        val invoices = invoiceService.fetchInvoicesBatch(-1, batchSize)
        val maxInvoiceId: Int = 0

        // Loop through the invoices and keep fetching until there is no more to be fetched
        while (invoices.size > 0) {
            // Process each invoice concurrently
            for (invoice: Invoice in invoices) {

            }
        }
    }

    /**
     * Calls the payment provider to make an attempt at paying the [invoice]
     * @throws InvoiceStatusUpdateFailedException if the status update failed
     * @throws InvoiceNotFoundException id the given invoice does not exist
     */
    fun payInvoice(invoice: Invoice) {
        // Check if the invoice is already paid, if it is, do nothing
        if (invoice.status.equals(InvoiceStatus.PAID)) {
            return
        }

        // Process the invoice here
        val isPaymentSuccessful = paymentProvider.charge(invoice)

        // Set the invoice status accordingly if the payment was successful
        if (isPaymentSuccessful) {
            val updatedInvoice = invoiceService.update(invoice.id, InvoiceStatus.PAID)

            // Check if the invoice was correctly updated
            if (!updatedInvoice.status.equals(invoice.status)) {
                throw InvoiceStatusUpdateFailedException(invoice.id, invoice.status.toString(), InvoiceStatus.PAID.toString())
            }
        }
    }

}