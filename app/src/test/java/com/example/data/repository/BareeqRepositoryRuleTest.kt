package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.CustomerReview
import com.example.data.model.InstallationBooking
import com.example.data.model.Order
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.UUID

class BareeqRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun createProfileAndOrder_authenticatedCustomer_succeedsAndReadsOwnOrders() = runBlocking {
        val uid = signInTestUser("customer_${UUID.randomUUID().toString().take(6)}@test.com")
        val repo = BareeqRepository(firestore, auth)

        val profileRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.ensureUserProfile("محمد أحمد", "customer@test.com")
        }
        assertTrue(profileRes.isSuccess)

        val orderRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.createOrder(
                Order(
                    customerName = "محمد أحمد",
                    customerPhone = "01012345678",
                    governorate = "القاهرة",
                    shippingAddress = "المعادي شارع 9",
                    paymentMethod = "cod",
                    itemsSummary = "منظم مقعد السيارة × 1",
                    primaryProductId = "p1",
                    primaryProductName = "منظم مقعد السيارة الفاخر",
                    subtotal = 420.0,
                    shippingFee = 55.0,
                    discountAmount = 0.0,
                    total = 475.0
                )
            )
        }
        assertTrue(orderRes.isSuccess)
        val created = orderRes.getOrThrow()

        val observed = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeMyOrders(uid).first { list -> list.any { it.id == created.id } }
        }
        assertTrue(observed.any { it.orderNumber == created.orderNumber })
    }

    @Test
    fun submitReviewAndBooking_authenticatedCustomer_succeeds() = runBlocking {
        val uid = signInTestUser("reviewer_${UUID.randomUUID().toString().take(6)}@test.com")
        val repo = BareeqRepository(firestore, auth)

        val reviewRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.submitReview(
                CustomerReview(
                    orderNumber = "BRQ-99102",
                    productId = "p2",
                    productName = "إضاءة داخلية محيطية",
                    customerName = "محمود حسن",
                    governorate = "الإسكندرية",
                    storeRating = 5L,
                    productRating = 5L,
                    comment = "خدمة ممتازة ومنتج فاخر جداً"
                )
            )
        }
        assertTrue(reviewRes.isSuccess)

        val bookingRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.createBooking(
                InstallationBooking(
                    customerName = "محمود حسن",
                    customerPhone = "01122334455",
                    governorate = "الجيزة",
                    carModel = "هيونداي توسان 2024",
                    serviceType = "تركيب إضاءة محيطية",
                    preferredDate = "2026-12-01"
                )
            )
        }
        assertTrue(bookingRes.isSuccess)

        val bookings = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeMyBookings(uid).first { it.isNotEmpty() }
        }
        assertTrue(bookings.isNotEmpty())
    }

    @Test
    fun observeOrders_unauthenticatedCaller_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repo = BareeqRepository(firestore, auth)

        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repo.observeMyOrders("some_uid").first()
            }
            fail("Expected PERMISSION_DENIED when unauthenticated")
        } catch (e: Exception) {
            val fsError = checkNotNull(e.unwrapFirestoreException()) {
                "Expected nested FirebaseFirestoreException, got $e"
            }
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsError.code)
        }
    }

    @Test
    fun crossUserOrderRead_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser("alice_${UUID.randomUUID().toString().take(6)}@test.com")
        val aliceRepo = BareeqRepository(firestore, auth)
        val created = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createOrder(
                Order(
                    customerName = "علياء",
                    customerPhone = "01234567890",
                    governorate = "الجيزة",
                    shippingAddress = "الدقي شارع التحرير",
                    paymentMethod = "instapay",
                    transferReference = "IPA-1234",
                    itemsSummary = "طقم عناية × 1",
                    primaryProductId = "p3",
                    primaryProductName = "طقم عناية ولمعان",
                    subtotal = 275.0,
                    shippingFee = 55.0,
                    discountAmount = 0.0,
                    total = 330.0
                )
            ).getOrThrow()
        }

        signInTestUser("bob_${UUID.randomUUID().toString().take(6)}@test.com")
        val bobRepo = BareeqRepository(firestore, auth)
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                bobRepo.observeMyOrders(aliceUid).first()
            }
            fail("Expected PERMISSION_DENIED for cross-user order query")
        } catch (e: Exception) {
            val fsError = checkNotNull(e.unwrapFirestoreException()) {
                "Expected nested FirebaseFirestoreException, got $e"
            }
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsError.code)
        }
        assertTrue(created.id.isNotBlank())
    }

    private companion object {
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3500L
    }
}
