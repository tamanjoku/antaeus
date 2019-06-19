/*
    Implements the data access layer (DAL).
    This file implements the database queries used to fetch and insert rows in our database tables.

    See the `mappings` module for the conversions between database rows and Kotlin objects.
 */

package io.pleo.antaeus.data

import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Customer
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime

class AntaeusDal(private val db: Database) {
    fun fetchInvoice(id: Int): Invoice? {
        // transaction(db) runs the internal query as a new database transaction.
        return transaction(db) {
            // Returns the first invoice with matching id.
            InvoiceTable
                .select { InvoiceTable.id.eq(id) }
                .firstOrNull()
                ?.toInvoice()
        }
    }

    fun fetchInvoices(): List<Invoice> {
        return transaction(db) {
            InvoiceTable
                .selectAll()
                .map { it.toInvoice() }
        }
    }

    /**
     * Get a list of invoices in batch size of [limit] starting from [offsetId]
     */
    fun fetchPendingInvoicesByBatch(offsetId: Int, limit: Int): List<Invoice> {
        if (!offsetId.equals(-1)) {
            return transaction(db) {
                InvoiceTable
                        .select { InvoiceTable.id.greater<Int, Int>(offsetId) and
                                InvoiceTable.status.eq(InvoiceStatus.PENDING.toString()) }
                        .limit(limit)
                        .orderBy(InvoiceTable.id, true)
                        .map { it.toInvoice() }
            }
        } else {
            return transaction(db) {
                InvoiceTable
                        .select { InvoiceTable.status.eq(InvoiceStatus.PENDING.toString()) }
                        .limit(limit)
                        .orderBy(InvoiceTable.id, true)
                        .map { it.toInvoice() }
            }
        }
    }

    /**
     * Get a list of invoices in batch size of [limit] starting from [offsetId]
     */
    fun fetchPendingInvoicesForRetryByBatch(offsetId: Int, limit: Int, retryFrequency: Int): List<Invoice> {
        if (!offsetId.equals(-1)) {
            return transaction(db) {
                InvoiceTable
                        .select { InvoiceTable.id.greater<Int, Int>(offsetId) and
                                InvoiceTable.status.eq(InvoiceStatus.RETRY.toString()) and
                                InvoiceTable.lastPaymentDate.greaterEq(DateTime.now().minusDays(2)) }
                        .limit(limit)
                        .orderBy(InvoiceTable.id, true)
                        .map { it.toInvoice() }
            }
        } else {
            return transaction(db) {
                InvoiceTable
                        .select { InvoiceTable.status.eq(InvoiceStatus.RETRY.toString()) and
                                InvoiceTable.lastPaymentDate.greaterEq(DateTime.now().minusDays(2)) }
                        .limit(limit)
                        .orderBy(InvoiceTable.id, true)
                        .map { it.toInvoice() }
            }
        }
    }

    fun createInvoice(amount: Money, customer: Customer, status: InvoiceStatus = InvoiceStatus.PENDING): Invoice? {
        val id = transaction(db) {
            // Insert the invoice and returns its new id.
            InvoiceTable
                .insert {
                    it[this.value] = amount.value
                    it[this.currency] = amount.currency.toString()
                    it[this.status] = status.toString()
                    it[this.customerId] = customer.id
                    it[this.noOfPaymentTries] = 0
                } get InvoiceTable.id
        }

        return fetchInvoice(id!!)
    }

    fun updateInvoice(id: Int, status: InvoiceStatus, lastPaymentDate: DateTime, noOfPaymentTries: Int): Invoice? {
        transaction(db) {
            // Update the invoice with the new details.
            InvoiceTable
                    .update ({InvoiceTable.id eq id}) {
                        it[this.status] = status.toString()
                        it[this.lastPaymentDate] = lastPaymentDate
                        it[this.noOfPaymentTries] = noOfPaymentTries
                    }
        }

        return fetchInvoice(id)
    }

    fun fetchCustomer(id: Int): Customer? {
        return transaction(db) {
            CustomerTable
                .select { CustomerTable.id.eq(id) }
                .firstOrNull()
                ?.toCustomer()
        }
    }

    fun fetchCustomers(): List<Customer> {
        return transaction(db) {
            CustomerTable
                .selectAll()
                .map { it.toCustomer() }
        }
    }

    fun createCustomer(currency: Currency): Customer? {
        val id = transaction(db) {
            // Insert the customer and return its new id.
            CustomerTable.insert {
                it[this.currency] = currency.toString()
            } get CustomerTable.id
        }

        return fetchCustomer(id!!)
    }
}
