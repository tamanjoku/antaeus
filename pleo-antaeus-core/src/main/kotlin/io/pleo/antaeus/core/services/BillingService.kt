package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.InvoiceNotFoundException
import io.pleo.antaeus.core.exceptions.InvoiceStatusUpdateFailedException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.joda.time.DateTime
import org.slf4j.LoggerFactory

class BillingService(
        private val invoiceService: InvoiceService,
        private val paymentProvider: PaymentProvider
) {

    private val logger = LoggerFactory.getLogger("BillingService");

    // Set the max number of tries for the invoice payment attempt before failure to 3
    private val MAX_NO_OF_PAYMENT_TRIES: Int = 3

//    // Create a timer task here to kick off the function to execute the bills
//    inline fun Timer.schedule(
//            time: Date,
//            crossinline action: TimerTask.() -> Unit
//    ): TimerTask {
//
//    }

    /**
     * Fetches all invoices for retry from the DB in batches of [batchSize] and attempts to pay these invoices.
     */
    fun processRetryInvoices(batchSize: Int) = runBlocking {
        // Fetch the initial batch
        var invoices = invoiceService.fetchPendingInvoicesByBatch(-1, batchSize)
        var lastInvoiceId: Int = 0

        // Loop through the invoices and keep fetching until there is no more to be fetched
        while (invoices.size > 0) {
            // process the batch of invoices concurrently
            processInvoicesConcurrently(invoices)

            // Set the new offset for the next batch
            lastInvoiceId = invoices.get(invoices.size - 1).id

            // Fetch the next batch of invoices
            invoices = invoiceService.fetchPendingInvoicesByBatch(lastInvoiceId, batchSize)
        }
    }

    /**
     * Fetches all pending invoices from the DB in batches of [batchSize] and attempts to pay these invoices.
     */
    fun processPendingInvoices(batchSize: Int) = runBlocking {
        // Fetch the initial batch
        var invoices = invoiceService.fetchPendingInvoicesByBatch(-1, batchSize)
        var lastInvoiceId: Int = 0

        // Loop through the invoices and keep fetching until there is no more to be fetched
        while (invoices != null && invoices.size > 0) {
            // process the batch of invoices concurrently
            processInvoicesConcurrently(invoices)

            // Set the new offset for the next batch
            lastInvoiceId = invoices.get(invoices.size - 1).id

            // Fetch the next batch of invoices
            invoices = invoiceService.fetchPendingInvoicesByBatch(lastInvoiceId, batchSize)
        }
    }

    /**
     * Given a list of [invoices] if will start off a new thread for each of these invoices
     */
    fun processInvoicesConcurrently(invoices: List<Invoice>) = runBlocking {
        // Process each invoice concurrently
        for (invoice: Invoice in invoices) {
            // Start a new Coroutine to pay the invoice in a new thread
            coroutineScope {
                launch {
                    try {
                        payInvoice(invoice)
                    } catch (e: Exception) {
                        logger.error("Exception while executing invoice payment: " + e.message)
                    }
                }
            }
        }
    }

    /**
     * Calls the payment provider to make an attempt at paying the [invoice]
     * @throws InvoiceStatusUpdateFailedException if the status update failed
     * @throws InvoiceNotFoundException id the given invoice does not exist
     */
    fun payInvoice(invoice: Invoice) {
        // Process the invoice here
        val isPaymentSuccessful = paymentProvider.charge(invoice)

        // Set the invoice status accordingly if the payment was successful
        if (isPaymentSuccessful) {
            val updatedInvoice = invoiceService.update(invoice.id, InvoiceStatus.PAID, DateTime.now(), invoice.noOfPaymentTries + 1)

            // Check if the invoice was correctly updated
            if (!updatedInvoice.status.equals(InvoiceStatus.PAID)) {
                throw InvoiceStatusUpdateFailedException(invoice.id, invoice.status.toString(), InvoiceStatus.PAID.toString())
            }
        } else {
            // Update the retry here or fail if it has exceeded the max number of retries
            var invoiceStatus:InvoiceStatus = InvoiceStatus.RETRY
            if (invoice.noOfPaymentTries >= MAX_NO_OF_PAYMENT_TRIES) {
                invoiceStatus = InvoiceStatus.FAILED
            }

            val updatedInvoice = invoiceService.update(invoice.id, invoiceStatus, DateTime.now(), invoice.noOfPaymentTries + 1)
        }
    }

}