package io.pleo.antaeus.core.services

import io.mockk.*
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import org.joda.time.DateTime
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class BillingServiceTest {

    private val invoiceList1 = ArrayList<Invoice>()
    private val invoiceList2 = ArrayList<Invoice>()
    private val invoiceList3 = ArrayList<Invoice>()
    private val invoiceListRetry1 = ArrayList<Invoice>()
    private val invoiceListRetry2 = ArrayList<Invoice>()

    init {
        invoiceList1.add(Invoice(id = 1, status = InvoiceStatus.PENDING,
                amount = Money(BigDecimal.valueOf(10), Currency.EUR),
                customerId = 1, noOfPaymentTries = 0, lastPaymentDate = DateTime.now()))
        invoiceList1.add(Invoice(id = 2, status = InvoiceStatus.PENDING,
                amount = Money(BigDecimal.valueOf(10), Currency.EUR),
                customerId = 2, noOfPaymentTries = 0, lastPaymentDate = DateTime.now()))
        invoiceList1.add(Invoice(id = 3, status = InvoiceStatus.PENDING,
                amount = Money(BigDecimal.valueOf(10), Currency.EUR),
                customerId = 3, noOfPaymentTries = 0, lastPaymentDate = DateTime.now()))

        invoiceList2.add(Invoice(id = 4, status = InvoiceStatus.PENDING,
                amount = Money(BigDecimal.valueOf(10), Currency.EUR),
                customerId = 1, noOfPaymentTries = 0, lastPaymentDate = DateTime.now()))
        invoiceList2.add(Invoice(id = 5, status = InvoiceStatus.PENDING,
                amount = Money(BigDecimal.valueOf(10), Currency.EUR),
                customerId = 3, noOfPaymentTries = 0, lastPaymentDate = DateTime.now()))
        invoiceList2.add(Invoice(id = 6, status = InvoiceStatus.PENDING,
                amount = Money(BigDecimal.valueOf(10), Currency.EUR),
                customerId = 1, noOfPaymentTries = 0, lastPaymentDate = DateTime.now()))

        invoiceList3.add(Invoice(id = 7, status = InvoiceStatus.PENDING,
                amount = Money(BigDecimal.valueOf(10), Currency.EUR),
                customerId = 2, noOfPaymentTries = 0, lastPaymentDate = DateTime.now()))
        invoiceList3.add(Invoice(id = 8, status = InvoiceStatus.PENDING,
                amount = Money(BigDecimal.valueOf(10), Currency.EUR),
                customerId = 2, noOfPaymentTries = 0, lastPaymentDate = DateTime.now()))
        invoiceList3.add(Invoice(id = 9, status = InvoiceStatus.PENDING,
                amount = Money(BigDecimal.valueOf(10), Currency.EUR),
                customerId = 3, noOfPaymentTries = 0, lastPaymentDate = DateTime.now()))

        invoiceListRetry1.add(Invoice(id = 1, status = InvoiceStatus.RETRY,
                amount = Money(BigDecimal.valueOf(10), Currency.EUR),
                customerId = 1, noOfPaymentTries = 1, lastPaymentDate = DateTime.now()))
        invoiceListRetry1.add(Invoice(id = 2, status = InvoiceStatus.RETRY,
                amount = Money(BigDecimal.valueOf(10), Currency.EUR),
                customerId = 2, noOfPaymentTries = 2, lastPaymentDate = DateTime.now()))
        invoiceListRetry1.add(Invoice(id = 7, status = InvoiceStatus.RETRY,
                amount = Money(BigDecimal.valueOf(10), Currency.EUR),
                customerId = 2, noOfPaymentTries = 3, lastPaymentDate = DateTime.now()))

        invoiceListRetry2.add(Invoice(id = 9, status = InvoiceStatus.RETRY,
                amount = Money(BigDecimal.valueOf(10), Currency.EUR),
                customerId = 3, noOfPaymentTries = 2, lastPaymentDate = DateTime.now()))
    }

    @Test
    fun `will process pending invoices successfully`() {
        // Setup the data access layer mock for invoices returned
        val dal = mockk<AntaeusDal> {
            every { fetchPendingInvoicesByBatch(-1, 3) } returns invoiceList1
            every { fetchPendingInvoicesByBatch(3, 3) } returns invoiceList2
            every { fetchPendingInvoicesByBatch(6, 3) } returns invoiceList3
            every { fetchPendingInvoicesByBatch(9, 3) } returns ArrayList<Invoice>()

            every { updateInvoice(id = 2, lastPaymentDate = any(), noOfPaymentTries = any(), status = InvoiceStatus.PAID) } returns null
            every { updateInvoice(id = 1, lastPaymentDate = any(), noOfPaymentTries = any(), status = InvoiceStatus.RETRY) } returns
                    Invoice(id = 1, status = InvoiceStatus.RETRY,
                            amount = Money(BigDecimal.valueOf(10), Currency.EUR),
                            customerId = 1, noOfPaymentTries = 1, lastPaymentDate = DateTime.now())
            every { updateInvoice(id = 3, lastPaymentDate = any(), noOfPaymentTries = any(), status = InvoiceStatus.RETRY) } returns
                    Invoice(id = 3, status = InvoiceStatus.RETRY,
                            amount = Money(BigDecimal.valueOf(10), Currency.EUR),
                            customerId = 3, noOfPaymentTries = 1, lastPaymentDate = DateTime.now())
            every { updateInvoice(id = 4, lastPaymentDate = any(), noOfPaymentTries = any(), status = InvoiceStatus.PAID) } returns
                    Invoice(id = 4, status = InvoiceStatus.PAID,
                            amount = Money(BigDecimal.valueOf(10), Currency.EUR),
                            customerId = 1, noOfPaymentTries = 1, lastPaymentDate = DateTime.now())
            every { updateInvoice(id = 5, lastPaymentDate = any(), noOfPaymentTries = any(), status = InvoiceStatus.RETRY) } returns
                    Invoice(id = 5, status = InvoiceStatus.RETRY,
                            amount = Money(BigDecimal.valueOf(10), Currency.EUR),
                            customerId = 3, noOfPaymentTries = 1, lastPaymentDate = DateTime.now())
            every { updateInvoice(id = 6, lastPaymentDate = any(), noOfPaymentTries = any(), status = InvoiceStatus.PAID) } returns
                    Invoice(id = 6, status = InvoiceStatus.PAID,
                            amount = Money(BigDecimal.valueOf(10), Currency.EUR),
                            customerId = 1, noOfPaymentTries = 1, lastPaymentDate = DateTime.now())
            every { updateInvoice(id = 7, lastPaymentDate = any(), noOfPaymentTries = any(), status = InvoiceStatus.RETRY) } returns
                    Invoice(id = 7, status = InvoiceStatus.RETRY,
                            amount = Money(BigDecimal.valueOf(10), Currency.EUR),
                            customerId = 2, noOfPaymentTries = 1, lastPaymentDate = DateTime.now())
            every { updateInvoice(id = 8, lastPaymentDate = any(), noOfPaymentTries = any(), status = InvoiceStatus.PAID) } returns
                    Invoice(id = 8, status = InvoiceStatus.PAID,
                            amount = Money(BigDecimal.valueOf(10), Currency.EUR),
                            customerId = 2, noOfPaymentTries = 1, lastPaymentDate = DateTime.now())
            every { updateInvoice(id = 9, lastPaymentDate = any(), noOfPaymentTries = any(), status = InvoiceStatus.RETRY) } returns
                    Invoice(id = 9, status = InvoiceStatus.RETRY,
                            amount = Money(BigDecimal.valueOf(10), Currency.EUR),
                            customerId = 3, noOfPaymentTries = 1, lastPaymentDate = DateTime.now())
        }

        val invoiceService = InvoiceService(dal)
        val paymentProvider = object : PaymentProvider {
            override fun charge(invoice: Invoice): Boolean {
                return invoice.id % 2 == 0
            }
        }

        val billingService = BillingService(invoiceService, paymentProvider)

        billingService.processPendingInvoices(3);

        // Verify that the the offset was correctly being set everytime we fetched a new batch to process
        verifyOrder {
            dal.fetchPendingInvoicesByBatch(-1, 3) to invoiceList1
            dal.fetchPendingInvoicesByBatch(3, 3) to invoiceList2
            dal.fetchPendingInvoicesByBatch(6, 3) to invoiceList3
            dal.fetchPendingInvoicesByBatch(9, 3) to ArrayList<Invoice>()
        }

        // Verify that we only tried to fetch the batch 4 times, on the third try there should have been no more batches
        // to get for the retry frequency
        verify(atLeast = 4, atMost = 4) {
            dal.fetchPendingInvoicesByBatch(any(), any())
        }

        // Verify that only 9 update attempts were made because we only tried to process 9 invoices in all the batches
        // that were received.
        verify(atLeast = 9, atMost = 9) {
            dal.updateInvoice(any(), any(), any(), any())
        }

        // Verify that the invoice that the invoice that did not exist was not fetched from the DB
        verify {
            dal.updateInvoice(id = 2, lastPaymentDate = any(), noOfPaymentTries = any(), status = InvoiceStatus.PAID) to null
        }
    }

    @Test
    fun `will process invoices for retry successfully`() {
        val dal = mockk<AntaeusDal> {
            every { fetchPendingInvoicesForRetryByBatch(-1, 3, 1) } returns invoiceListRetry1
            every { fetchPendingInvoicesForRetryByBatch(7, 3, 1) } returns invoiceListRetry2
            every { fetchPendingInvoicesForRetryByBatch(9, 3, 1) } returns ArrayList<Invoice>()

            every { updateInvoice(id = 1, lastPaymentDate = any(), noOfPaymentTries = 2, status = InvoiceStatus.RETRY) } returns
                    Invoice(id = 1, status = InvoiceStatus.RETRY,
                            amount = Money(BigDecimal.valueOf(10), Currency.EUR),
                            customerId = 1, noOfPaymentTries = 2, lastPaymentDate = DateTime.now())
            every { updateInvoice(id = 2, lastPaymentDate = any(), noOfPaymentTries = 3, status = InvoiceStatus.PAID) } returns
                    Invoice(id = 2, status = InvoiceStatus.PAID,
                            amount = Money(BigDecimal.valueOf(10), Currency.EUR),
                            customerId = 2, noOfPaymentTries = 3, lastPaymentDate = DateTime.now())
            every { updateInvoice(id = 7, lastPaymentDate = any(), noOfPaymentTries = 4, status = InvoiceStatus.FAILED) } returns
                    Invoice(id = 7, status = InvoiceStatus.FAILED,
                            amount = Money(BigDecimal.valueOf(10), Currency.EUR),
                            customerId = 2, noOfPaymentTries = 4, lastPaymentDate = DateTime.now())
            every { updateInvoice(id = 9, lastPaymentDate = any(), noOfPaymentTries = 3, status = InvoiceStatus.RETRY) } returns
                    Invoice(id = 9, status = InvoiceStatus.RETRY,
                            amount = Money(BigDecimal.valueOf(10), Currency.EUR),
                            customerId = 3, noOfPaymentTries = 3, lastPaymentDate = DateTime.now())
        }

        val invoiceService = InvoiceService(dal)
        val paymentProvider = object : PaymentProvider {
            override fun charge(invoice: Invoice): Boolean {
                return invoice.id % 2 == 0
            }
        }

        val billingService = BillingService(invoiceService, paymentProvider)

        billingService.processRetryInvoices(3, 1);

        // Verify that the the offset was correctly being set everytime we fetched a new batch to process
        verifyOrder {
            dal.fetchPendingInvoicesForRetryByBatch(-1, 3, 1) to invoiceListRetry1
            dal.fetchPendingInvoicesForRetryByBatch(7, 3, 1) to invoiceListRetry2
            dal.fetchPendingInvoicesForRetryByBatch(9, 3, 1) to ArrayList<Invoice>()
        }

        // Verify that we only tried to fetch the batch 3 times, on the third try there should have been no more batches
        // to get for the retry frequency
        verify(atLeast = 3, atMost = 3) {
            dal.fetchPendingInvoicesForRetryByBatch(any(), any(), any())
        }

        // Verify that only 4 update attempts were made because we only tried to process 4 invoices in all the batches
        // that were received.
        verify(atLeast = 4, atMost = 4) {
            dal.updateInvoice(any(), any(), any(), any())
        }

        // Verify that the invoice that had exceeded the maximum number of retries before failure actually failed.
        verify(atLeast = 1, atMost = 1) {
            dal.updateInvoice(id = 7, lastPaymentDate = any(), noOfPaymentTries = 4, status = InvoiceStatus.FAILED)
        }

        // Verify that a successful invoice that was retried was paid.
        verify {
            dal.updateInvoice(id = 2, lastPaymentDate = any(), noOfPaymentTries = 3, status = InvoiceStatus.PAID) to
                    Invoice(id = 2, status = InvoiceStatus.PAID,
                            amount = Money(BigDecimal.valueOf(10), Currency.EUR),
                            customerId = 2, noOfPaymentTries = 3, lastPaymentDate = DateTime.now())
        }
    }
}
