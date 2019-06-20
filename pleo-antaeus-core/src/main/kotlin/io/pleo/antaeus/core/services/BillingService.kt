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
import java.util.Timer
import kotlin.concurrent.fixedRateTimer

// Set the max number of tries for the invoice payment attempt before failure to 3
const val MAX_NO_OF_PAYMENT_TRIES: Int = 3

// Day of the month to run the payment of the invoices
const val DAY_OF_THE_MONTH = 1

// How many invoices to process as a batch
const val INVOICE_PROCESSING_BATCH_SIZE = 10

// This is the number of days before the next retry attempt for each invoice that needs to be retried
const val NO_OF_DAYS_BEFORE_NEXT_RETRY = 2

class BillingService(
        private val invoiceService: InvoiceService,
        private val paymentProvider: PaymentProvider
) {
    private val logger = LoggerFactory.getLogger("BillingService");

    /**
     * Kick start the scheduler for processing pending invoices
     */
    fun startPendingInvoicesScheduler(): Timer {
        logger.info("Starting the BillingService-PendingInvoices-Scheduler")
        return fixedRateTimer("BillingService-PendingInvoices-Scheduler",
                daemon = false,
                // Kick start now
                startAt = DateTime.now().toDate(),
                period = 216_000_000L   // Run this daily
        ) {
            // Kick start only if it is the set day of the month
            if (DateTime.now().dayOfMonth == DAY_OF_THE_MONTH) {
                processPendingInvoices(INVOICE_PROCESSING_BATCH_SIZE)
            }
        }
    }

    /**
     * Kick start the scheduler for processing invoices for retry
     */
    fun startInvoicesForRetryScheduler(): Timer {
        logger.info("Starting the BillingService-InvoicesForRetry-Scheduler")
        return fixedRateTimer("BillingService-InvoicesForRetry-Scheduler",
                daemon = false,
                // Kick start now
                startAt = DateTime.now().toDate(),
                period = 216_000_000L   // Run this daily
        ) {
            processRetryInvoices(INVOICE_PROCESSING_BATCH_SIZE, NO_OF_DAYS_BEFORE_NEXT_RETRY)
        }
    }

    /**
     * Fetches all invoices for retry from the DB in batches of [batchSize] and attempts to pay these invoices. It
     * will only attempt invoices that have a last payment date of [retryFrequency] days ago.
     */
    fun processRetryInvoices(batchSize: Int, retryFrequency: Int) {
        // Fetch the initial batch
        var invoices = invoiceService.fetchPendingInvoicesForRetryByBatch(-1, batchSize, retryFrequency)
        var lastInvoiceId: Int = 0

        logger.info("Got " + invoices.size + " invoice(s) to be retried")

        // Loop through the invoices and keep fetching until there is no more to be fetched
        while (invoices != null && invoices.size > 0) {
            // process the batch of invoices concurrently
            processInvoicesConcurrently(invoices)

            // Set the new offset for the next batch
            lastInvoiceId = invoices.get(invoices.size - 1).id

            // Fetch the next batch of invoices
            invoices = invoiceService.fetchPendingInvoicesForRetryByBatch(lastInvoiceId, batchSize, retryFrequency)
        }
    }

    /**
     * Fetches all pending invoices from the DB in batches of [batchSize] and attempts to pay these invoices.
     */
    fun processPendingInvoices(batchSize: Int) {
        // Fetch the initial batch
        var invoices = invoiceService.fetchPendingInvoicesByBatch(-1, batchSize)
        var lastInvoiceId: Int = 0

        logger.info("Got " + invoices.size + " invoice(s) to be billed")

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
     * Given a list of [invoices] if will start off a new thread for each of these invoices.
     * Finish processing this batch before moving on to the new batch.
     */
    private fun processInvoicesConcurrently(invoices: List<Invoice>) = runBlocking {
        // Process each invoice concurrently
        for (invoice: Invoice in invoices) {
            // Start a new Coroutine to pay the invoice in a new thread
            coroutineScope {
                launch {
                    try {
                        logger.info("Inside the routine about to process invoice ID: " + invoice.id)
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
    fun payInvoice(invoice: Invoice): Invoice {
        logger.info("Attempting to bill invoice ID: " + invoice.id)
        var updatedInvoice = invoice

        // Process the invoice here
        val isPaymentSuccessful = paymentProvider.charge(invoice)

        // Set the invoice status accordingly if the payment was successful
        if (isPaymentSuccessful) {
            updatedInvoice = invoiceService.update(invoice.id, InvoiceStatus.PAID, DateTime.now(), invoice.noOfPaymentTries + 1)

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

            updatedInvoice = invoiceService.update(invoice.id, invoiceStatus, DateTime.now(), invoice.noOfPaymentTries + 1)
        }

        logger.info("After billing invoice ID: " + invoice.id + ", status is: " + updatedInvoice.status.toString())

        return updatedInvoice
    }

}