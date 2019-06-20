/*
    Implements endpoints related to invoices.
 */

package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.InvoiceNotFoundException
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import org.joda.time.DateTime

class InvoiceService(private val dal: AntaeusDal) {
    fun fetchAll(): List<Invoice> {
       return dal.fetchInvoices()
    }

    /**
     * This will fetch all pending invoices from the DB in batches of [limit] and id is greater than [offsetId]
     */
    fun fetchPendingInvoicesByBatch(offsetId: Int, limit: Int): List<Invoice> {
        return dal.fetchPendingInvoicesByBatch(offsetId, limit)
    }

    /**
     * This will fetch all invoices that have been marked for retry from the DB in batches of [limit] and id is greater
     * than [offsetId] and last payment try date was [retryFrequency] days ago.
     */
    fun fetchPendingInvoicesForRetryByBatch(offsetId: Int, limit: Int, retryFrequency: Int): List<Invoice> {
        return dal.fetchPendingInvoicesForRetryByBatch(offsetId, limit, retryFrequency)
    }

    fun update(id: Int, status: InvoiceStatus, lastPaymentDate: DateTime, noOfPaymentTries: Int): Invoice {
        return dal.updateInvoice(id, status, lastPaymentDate, noOfPaymentTries) ?: throw InvoiceNotFoundException(id)
    }

    fun fetch(id: Int): Invoice {
        return dal.fetchInvoice(id) ?: throw InvoiceNotFoundException(id)
    }
}
