package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.Coupon
import com.example.data.model.CustomerReview
import com.example.data.model.EgyptStoreCatalog
import com.example.data.model.InstallationBooking
import com.example.data.model.LoyaltyEntry
import com.example.data.model.Order
import com.example.data.model.Product
import com.example.data.model.StaffPermission
import com.example.data.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class BareeqRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        Firebase.auth
    )

    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("يجب تسجيل الدخول بحساب Google أولاً")

    private fun <T> Flow<T>.logFirestoreErrors(op: OperationType, path: String): Flow<T> =
        catch { error ->
            val fsError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (fsError == null && error is CancellationException) throw error
            handleFirestoreError(fsError ?: error, op, path, auth)
            throw error
        }

    // --- User Profile & Loyalty ---
    suspend fun ensureUserProfile(fullName: String?, email: String?): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        val snap = docRef.get().await()
        if (!snap.exists()) {
            val payload = buildMap<String, Any> {
                put("userId", uid)
                email?.takeIf { it.isNotBlank() }?.let { put("email", it.lowercase()) }
                put("fullName", fullName?.takeIf { it.isNotBlank() } ?: "عميل بريق")
                put("role", "customer")
                put("loyaltyPoints", 0L)
                put("createdAt", FieldValue.serverTimestamp())
                put("updatedAt", FieldValue.serverTimestamp())
            }
            docRef.set(payload).await()
        }
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.WRITE, "users/${auth.currentUser?.uid}", auth)
    }

    fun observeUserProfile(userId: String): Flow<UserProfile?> =
        db.collection("users").document(userId)
            .snapshots()
            .map { it.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .logFirestoreErrors(OperationType.GET, "users/$userId")

    fun observeLoyaltyLedger(userId: String): Flow<List<LoyaltyEntry>> =
        db.collection("users").document(userId).collection("loyalty_ledger")
            .snapshots()
            .map { snap ->
                snap.toObjects(LoyaltyEntry::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .logFirestoreErrors(OperationType.LIST, "users/$userId/loyalty_ledger")

    // --- Staff Permissions ---
    fun observeMyStaffPermission(email: String): Flow<StaffPermission?> = flow {
        val cleanEmail = email.trim().lowercase()
        if (cleanEmail.isBlank()) {
            emit(null)
            return@flow
        }
        emitAll(
            db.collection("staff_permissions").document(cleanEmail)
                .snapshots()
                .map { snap ->
                    if (snap.exists()) {
                        snap.toObject(StaffPermission::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    } else null
                }
                .logFirestoreErrors(OperationType.GET, "staff_permissions/$cleanEmail")
        )
    }

    fun observeAllStaffPermissions(): Flow<List<StaffPermission>> =
        db.collection("staff_permissions")
            .snapshots()
            .map { snap ->
                snap.toObjects(StaffPermission::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedBy { it.email }
            }
            .logFirestoreErrors(OperationType.LIST, "staff_permissions")

    suspend fun upsertStaffPermission(
        email: String,
        displayName: String,
        canAddProducts: Boolean,
        canDeleteProducts: Boolean,
        canManageDiscounts: Boolean,
        canManageOrders: Boolean,
        isActive: Boolean = true
    ): Result<Unit> = runCatching {
        requireUserId()
        val cleanEmail = email.trim().lowercase()
        require(cleanEmail.contains("@") && cleanEmail.contains(".")) { "البريد الإلكتروني غير صالح" }
        val docRef = db.collection("staff_permissions").document(cleanEmail)
        val existing = docRef.get().await()
        if (existing.exists()) {
            docRef.update(
                mapOf(
                    "displayName" to displayName.trim().ifBlank { cleanEmail },
                    "canAddProducts" to canAddProducts,
                    "canDeleteProducts" to canDeleteProducts,
                    "canManageDiscounts" to canManageDiscounts,
                    "canManageOrders" to canManageOrders,
                    "isActive" to isActive,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } else {
            docRef.set(
                mapOf(
                    "email" to cleanEmail,
                    "displayName" to displayName.trim().ifBlank { cleanEmail },
                    "canAddProducts" to canAddProducts,
                    "canDeleteProducts" to canDeleteProducts,
                    "canManageDiscounts" to canManageDiscounts,
                    "canManageOrders" to canManageOrders,
                    "isActive" to isActive,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.WRITE, "staff_permissions/$email", auth)
    }

    suspend fun removeStaffPermission(email: String): Result<Unit> = runCatching {
        requireUserId()
        val cleanEmail = email.trim().lowercase()
        db.collection("staff_permissions").document(cleanEmail).delete().await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.DELETE, "staff_permissions/$email", auth)
    }

    // --- Products ---
    fun observeProducts(): Flow<List<Product>> =
        db.collection("products")
            .whereEqualTo("isActive", true)
            .snapshots()
            .map { snap ->
                snap.toObjects(Product::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.isFeatured }
            }
            .logFirestoreErrors(OperationType.LIST, "products")

    suspend fun addOrUpdateProduct(product: Product): Result<String> = runCatching {
        val uid = requireUserId()
        val docId = product.id.takeIf { it.isNotBlank() } ?: "prod_${UUID.randomUUID().toString().take(8)}"
        val docRef = db.collection("products").document(docId)
        val existing = docRef.get().await()
        if (existing.exists()) {
            val updates = buildMap<String, Any> {
                put("name", product.name.trim())
                put("category", product.category.trim())
                put("price", product.price)
                product.oldPrice?.let { put("oldPrice", it) }
                put("stock", product.stock)
                product.badge?.takeIf { it.isNotBlank() }?.let { put("badge", it) }
                put("description", product.description.trim())
                put("drawableKey", product.drawableKey)
                put("isFeatured", product.isFeatured)
                put("isActive", true)
                put("updatedAt", FieldValue.serverTimestamp())
            }
            docRef.update(updates).await()
        } else {
            docRef.set(product.copy(createdBy = uid, isActive = true).toCreateMap()).await()
        }
        docId
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.WRITE, "products", auth)
    }

    suspend fun deleteProduct(productId: String): Result<Unit> = runCatching {
        requireUserId()
        require(productId.isNotBlank())
        db.collection("products").document(productId).delete().await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.DELETE, "products/$productId", auth)
    }

    // --- Coupons ---
    fun observeActiveCoupons(): Flow<List<Coupon>> =
        db.collection("coupons")
            .whereEqualTo("isActive", true)
            .snapshots()
            .map { snap -> snap.toObjects(Coupon::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .logFirestoreErrors(OperationType.LIST, "coupons")

    suspend fun getCoupon(code: String): Coupon? {
        val cleanCode = code.trim().uppercase()
        if (cleanCode.isBlank()) return null
        return try {
            val snap = db.collection("coupons").document(cleanCode).get().await()
            snap.toObject(Coupon::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            handleFirestoreError(e, OperationType.GET, "coupons/$cleanCode", auth)
            null
        }
    }

    suspend fun upsertCoupon(
        code: String,
        discountType: String,
        discountValue: Double,
        minOrderTotal: Double,
        isActive: Boolean
    ): Result<Unit> = runCatching {
        requireUserId()
        val cleanCode = code.trim().uppercase()
        require(cleanCode.matches(Regex("^[A-Z0-9_\\-]{2,40}$"))) { "كود الخصم يجب أن يكون بالإنجليزية والأرقام" }
        val docRef = db.collection("coupons").document(cleanCode)
        val existing = docRef.get().await()
        if (existing.exists()) {
            docRef.update(
                mapOf(
                    "discountType" to discountType,
                    "discountValue" to discountValue,
                    "minOrderTotal" to minOrderTotal,
                    "isActive" to isActive,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } else {
            docRef.set(
                mapOf(
                    "code" to cleanCode,
                    "discountType" to discountType,
                    "discountValue" to discountValue,
                    "minOrderTotal" to minOrderTotal,
                    "isActive" to isActive,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.WRITE, "coupons/$code", auth)
    }

    suspend fun deleteCoupon(code: String): Result<Unit> = runCatching {
        requireUserId()
        val cleanCode = code.trim().uppercase()
        db.collection("coupons").document(cleanCode).delete().await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.DELETE, "coupons/$code", auth)
    }

    // --- Orders ---
    fun observeMyOrders(userId: String): Flow<List<Order>> =
        db.collection("orders")
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snap ->
                snap.toObjects(Order::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .logFirestoreErrors(OperationType.LIST, "orders")

    fun observeAllOrders(): Flow<List<Order>> =
        db.collection("orders")
            .snapshots()
            .map { snap ->
                snap.toObjects(Order::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .logFirestoreErrors(OperationType.LIST, "orders")

    suspend fun createOrder(order: Order): Result<Order> = runCatching {
        val uid = requireUserId()
        val orderId = "ord_${UUID.randomUUID().toString().take(10)}"
        val orderNum = "BRQ-${(10000..99999).random()}"
        val payload = buildMap<String, Any> {
            put("userId", uid)
            put("orderNumber", orderNum)
            put("customerName", order.customerName.trim())
            put("customerPhone", order.customerPhone.trim())
            put("governorate", order.governorate)
            put("shippingAddress", order.shippingAddress.trim())
            order.notes?.takeIf { it.isNotBlank() }?.let { put("notes", it.trim()) }
            put("paymentMethod", order.paymentMethod)
            order.transferReference?.takeIf { it.isNotBlank() }?.let { put("transferReference", it.trim()) }
            order.couponCode?.takeIf { it.isNotBlank() }?.let { put("couponCode", it.trim().uppercase()) }
            put("itemsSummary", order.itemsSummary)
            put("primaryProductId", order.primaryProductId)
            put("primaryProductName", order.primaryProductName)
            put("subtotal", order.subtotal)
            put("shippingFee", order.shippingFee)
            put("discountAmount", order.discountAmount)
            put("total", order.total)
            put("status", "pending")
            put("createdAt", FieldValue.serverTimestamp())
            put("updatedAt", FieldValue.serverTimestamp())
        }
        db.collection("orders").document(orderId).set(payload).await()

        // Grant loyalty points (1 point per 50 EGP)
        val earnedPoints = (order.total / 50.0).toLong().coerceAtLeast(1L)
        val userRef = db.collection("users").document(uid)
        val userSnap = userRef.get().await()
        if (userSnap.exists()) {
            val currentPoints = userSnap.getLong("loyaltyPoints") ?: 0L
            userRef.update(
                mapOf(
                    "loyaltyPoints" to currentPoints + earnedPoints,
                    "phone" to order.customerPhone.trim(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            val entryId = "loy_${UUID.randomUUID().toString().take(8)}"
            userRef.collection("loyalty_ledger").document(entryId).set(
                mapOf(
                    "userId" to uid,
                    "orderNumber" to orderNum,
                    "pointsDelta" to earnedPoints,
                    "reason" to "مكافأة طلب رقم $orderNum",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }

        order.copy(id = orderId, userId = uid, orderNumber = orderNum, status = "pending")
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.CREATE, "orders", auth)
    }

    suspend fun updateOrderStatus(orderId: String, newStatus: String): Result<Unit> = runCatching {
        requireUserId()
        db.collection("orders").document(orderId)
            .update(mapOf("status" to newStatus, "updatedAt" to FieldValue.serverTimestamp()))
            .await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.UPDATE, "orders/$orderId", auth)
    }

    // --- Reviews ---
    fun observeReviews(): Flow<List<CustomerReview>> =
        db.collection("reviews")
            .whereEqualTo("isApproved", true)
            .snapshots()
            .map { snap ->
                snap.toObjects(CustomerReview::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .logFirestoreErrors(OperationType.LIST, "reviews")

    suspend fun submitReview(review: CustomerReview): Result<Unit> = runCatching {
        val uid = requireUserId()
        val reviewId = "rev_${UUID.randomUUID().toString().take(10)}"
        val payload = buildMap<String, Any> {
            put("userId", uid)
            put("orderNumber", review.orderNumber)
            review.productId?.takeIf { it.isNotBlank() }?.let { put("productId", it) }
            put("productName", review.productName)
            put("customerName", review.customerName.trim())
            put("governorate", review.governorate)
            put("storeRating", review.storeRating.coerceIn(1L, 5L))
            put("productRating", review.productRating.coerceIn(1L, 5L))
            put("comment", review.comment.trim())
            review.photoBase64?.takeIf { it.isNotBlank() }?.let { put("photoBase64", it) }
            put("isApproved", true)
            put("createdAt", FieldValue.serverTimestamp())
        }
        db.collection("reviews").document(reviewId).set(payload).await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.CREATE, "reviews", auth)
    }

    // --- Bookings ---
    fun observeMyBookings(userId: String): Flow<List<InstallationBooking>> =
        db.collection("bookings")
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snap ->
                snap.toObjects(InstallationBooking::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .logFirestoreErrors(OperationType.LIST, "bookings")

    fun observeAllBookings(): Flow<List<InstallationBooking>> =
        db.collection("bookings")
            .snapshots()
            .map { snap ->
                snap.toObjects(InstallationBooking::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .logFirestoreErrors(OperationType.LIST, "bookings")

    suspend fun createBooking(booking: InstallationBooking): Result<Unit> = runCatching {
        val uid = requireUserId()
        val bookingId = "bk_${UUID.randomUUID().toString().take(10)}"
        val payload = buildMap<String, Any> {
            put("userId", uid)
            put("customerName", booking.customerName.trim())
            put("customerPhone", booking.customerPhone.trim())
            put("governorate", booking.governorate)
            put("carModel", booking.carModel.trim())
            put("serviceType", booking.serviceType)
            put("preferredDate", booking.preferredDate.trim())
            booking.notes?.takeIf { it.isNotBlank() }?.let { put("notes", it.trim()) }
            put("status", "pending")
            put("createdAt", FieldValue.serverTimestamp())
            put("updatedAt", FieldValue.serverTimestamp())
        }
        db.collection("bookings").document(bookingId).set(payload).await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.CREATE, "bookings", auth)
    }

    suspend fun updateBookingStatus(bookingId: String, newStatus: String): Result<Unit> = runCatching {
        requireUserId()
        db.collection("bookings").document(bookingId)
            .update(mapOf("status" to newStatus, "updatedAt" to FieldValue.serverTimestamp()))
            .await()
        Unit
    }.onFailure { e ->
        if (e is CancellationException) throw e
        handleFirestoreError(e, OperationType.UPDATE, "bookings/$bookingId", auth)
    }

    // --- Initial Catalog Seeding (Owner only) ---
    suspend fun seedDefaultCatalogIfEmpty(): Result<Unit> = runCatching {
        val email = auth.currentUser?.email?.lowercase()
        if (email != EgyptStoreCatalog.BOOTSTRAPPED_OWNER_EMAIL) return@runCatching
        val existing = db.collection("products").whereEqualTo("isActive", true).limit(1).get().await()
        if (existing.isEmpty) {
            defaultSeedProducts().forEach { prod ->
                db.collection("products").document(prod.id).set(prod.toCreateMap()).await()
            }
            upsertCoupon("BAREEQ10", "percent", 10.0, 200.0, true)
        }
    }

    companion object {
        fun defaultSeedProducts(): List<Product> = listOf(
            Product(
                id = "p1",
                name = "منظم مقعد السيارة الفاخر",
                category = "تنظيم",
                price = 420.0,
                oldPrice = 520.0,
                stock = 8L,
                badge = "عرض خاص",
                description = "خامة جلدية مبطنة بخياطة ذهبية ومساحات عملية لتنظيم أغراضك داخل المقصورة.",
                drawableKey = "organizer",
                isFeatured = true
            ),
            Product(
                id = "p2",
                name = "إضاءة داخلية محيطية متعددة الألوان",
                category = "إضاءة",
                price = 350.0,
                stock = 14L,
                badge = "الأكثر طلباً",
                description = "إضاءة LED ناعمة بـ 64 لوناً تمنح المقصورة أجواء فاخرة بتحكم من الموبايل.",
                drawableKey = "lighting",
                isFeatured = true
            ),
            Product(
                id = "p3",
                name = "طقم عناية ولمعان السيارة المتكامل",
                category = "عناية",
                price = 275.0,
                oldPrice = 320.0,
                stock = 5L,
                badge = "كمية محدودة",
                description = "شامبو نانو سيراميك، ملمع داخلي، ومناشف مايكروفايبر عالية الكثافة.",
                drawableKey = "care",
                isFeatured = true
            ),
            Product(
                id = "p4",
                name = "حامل هاتف مغناطيسي معدني 360°",
                category = "إلكترونيات",
                price = 185.0,
                stock = 21L,
                description = "تثبيت فائق القوة مع دوران 360 درجة أثناء القيادة دون اهتزاز.",
                drawableKey = "hero",
                isFeatured = false
            ),
            Product(
                id = "p5",
                name = "طقم دعاسات جلد مبطنة 3D",
                category = "تنظيم",
                price = 690.0,
                oldPrice = 790.0,
                stock = 6L,
                badge = "جديد بريق",
                description = "تغطية كاملة لأرضية السيارة ومقاومة للماء والأتربة بتفصيل دقيق.",
                drawableKey = "organizer",
                isFeatured = true
            ),
            Product(
                id = "p6",
                name = "غطاء مقود رياضي مخيط يدوياً",
                category = "اكسسوارات داخلية",
                price = 240.0,
                stock = 12L,
                description = "تحكم مريح وثبات ممتاز بلمسة رياضية فاخرة من الجلد الطبيعي.",
                drawableKey = "hero",
                isFeatured = false
            )
        )

        fun defaultSeedReviews(): List<CustomerReview> = listOf(
            CustomerReview(
                id = "seed_r1",
                orderNumber = "BRQ-261110",
                productId = "p2",
                productName = "إضاءة داخلية محيطية متعددة الألوان",
                customerName = "أحمد محمود",
                governorate = "القاهرة",
                storeRating = 5L,
                productRating = 5L,
                comment = "الإضاءة غيرت شكل العربية تماماً والشحن وصل تاني يوم في مدينة نصر."
            ),
            CustomerReview(
                id = "seed_r2",
                orderNumber = "BRQ-261112",
                productId = "p1",
                productName = "منظم مقعد السيارة الفاخر",
                customerName = "كريم طارق",
                governorate = "الإسكندرية",
                storeRating = 5L,
                productRating = 5L,
                comment = "خامة جلد محترمة جداً وتعامل راقي، حولت إنستا باي والطلب اتأكد في دقائق."
            ),
            CustomerReview(
                id = "seed_r3",
                orderNumber = "BRQ-261114",
                productId = "p3",
                productName = "طقم عناية ولمعان السيارة المتكامل",
                customerName = "محمد سامي",
                governorate = "الجيزة",
                storeRating = 5L,
                productRating = 4L,
                comment = "لمعان ممتاز وريحة حلوة جداً، التغليف كان آمن ومرتب."
            )
        )
    }
}
