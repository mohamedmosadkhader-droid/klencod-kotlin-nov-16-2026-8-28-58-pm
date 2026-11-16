package com.example.ui.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Coupon
import com.example.data.model.CustomerReview
import com.example.data.model.EgyptPaymentMethod
import com.example.data.model.EgyptStoreCatalog
import com.example.data.model.InstallationBooking
import com.example.data.model.LoyaltyEntry
import com.example.data.model.Order
import com.example.data.model.Product
import com.example.data.model.StaffPermission
import com.example.data.model.UserProfile
import com.example.data.repository.BareeqRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

data class EffectivePermissions(
    val isOwner: Boolean = false,
    val canAddProducts: Boolean = false,
    val canDeleteProducts: Boolean = false,
    val canManageDiscounts: Boolean = false,
    val canManageOrders: Boolean = false
) {
    val hasAnyAdminAccess: Boolean
        get() = isOwner || canAddProducts || canDeleteProducts || canManageDiscounts || canManageOrders
}

@OptIn(ExperimentalCoroutinesApi::class)
class BareeqViewModel(
    private val repository: BareeqRepository,
    private val currentUserId: String,
    private val currentUserEmail: String?,
    private val currentUserDisplayName: String?
) : ViewModel() {

    private fun <T> Flow<T>.safeCollect(fallback: T, tag: String): StateFlow<T> =
        this.catch { error ->
            val fsError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (fsError == null && error is CancellationException) throw error
            Log.w(TAG, "Stream error in $tag", fsError ?: error)
            emit(fallback)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = fallback
        )

    val userProfile: StateFlow<UserProfile?> =
        repository.observeUserProfile(currentUserId).safeCollect(null, "userProfile")

    private val myStaffPermission: StateFlow<StaffPermission?> =
        (currentUserEmail?.let { repository.observeMyStaffPermission(it) } ?: flowOf(null))
            .safeCollect(null, "staffPermission")

    val permissions: StateFlow<EffectivePermissions> =
        combine(userProfile, myStaffPermission) { profile, staff ->
            val owner = currentUserEmail?.lowercase() == EgyptStoreCatalog.BOOTSTRAPPED_OWNER_EMAIL ||
                profile?.role == "owner"
            val activeStaff = staff?.takeIf { it.isActive }
            EffectivePermissions(
                isOwner = owner,
                canAddProducts = owner || (activeStaff?.canAddProducts == true),
                canDeleteProducts = owner || (activeStaff?.canDeleteProducts == true),
                canManageDiscounts = owner || (activeStaff?.canManageDiscounts == true),
                canManageOrders = owner || (activeStaff?.canManageOrders == true)
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = EffectivePermissions(
                isOwner = currentUserEmail?.lowercase() == EgyptStoreCatalog.BOOTSTRAPPED_OWNER_EMAIL
            )
        )

    val products: StateFlow<List<Product>> =
        repository.observeProducts()
            .map { list -> list.ifEmpty { BareeqRepository.defaultSeedProducts() } }
            .safeCollect(BareeqRepository.defaultSeedProducts(), "products")

    val reviews: StateFlow<List<CustomerReview>> =
        repository.observeReviews()
            .map { list -> list.ifEmpty { BareeqRepository.defaultSeedReviews() } }
            .safeCollect(BareeqRepository.defaultSeedReviews(), "reviews")

    val coupons: StateFlow<List<Coupon>> =
        repository.observeActiveCoupons()
            .map { list ->
                list.ifEmpty {
                    listOf(
                        Coupon(
                            id = "BAREEQ10",
                            code = "BAREEQ10",
                            discountType = "percent",
                            discountValue = 10.0,
                            minOrderTotal = 200.0,
                            isActive = true
                        )
                    )
                }
            }
            .safeCollect(emptyList(), "coupons")

    val myOrders: StateFlow<List<Order>> =
        repository.observeMyOrders(currentUserId).safeCollect(emptyList(), "myOrders")

    val myBookings: StateFlow<List<InstallationBooking>> =
        repository.observeMyBookings(currentUserId).safeCollect(emptyList(), "myBookings")

    val loyaltyLedger: StateFlow<List<LoyaltyEntry>> =
        repository.observeLoyaltyLedger(currentUserId).safeCollect(emptyList(), "loyaltyLedger")

    val allOrders: StateFlow<List<Order>> =
        permissions.flatMapLatest { perms ->
            if (perms.isOwner || perms.canManageOrders) repository.observeAllOrders()
            else flowOf(emptyList())
        }.safeCollect(emptyList(), "allOrders")

    val allBookings: StateFlow<List<InstallationBooking>> =
        permissions.flatMapLatest { perms ->
            if (perms.isOwner || perms.canManageOrders) repository.observeAllBookings()
            else flowOf(emptyList())
        }.safeCollect(emptyList(), "allBookings")

    val allStaff: StateFlow<List<StaffPermission>> =
        permissions.flatMapLatest { perms ->
            if (perms.isOwner) repository.observeAllStaffPermissions()
            else flowOf(emptyList())
        }.safeCollect(emptyList(), "allStaff")

    // --- UI Filter & Cart State ---
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("الكل")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _cart = MutableStateFlow<Map<String, Int>>(emptyMap())
    val cart: StateFlow<Map<String, Int>> = _cart.asStateFlow()

    private val _appliedCoupon = MutableStateFlow<Coupon?>(null)
    val appliedCoupon: StateFlow<Coupon?> = _appliedCoupon.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _lastCompletedOrder = MutableStateFlow<Order?>(null)
    val lastCompletedOrder: StateFlow<Order?> = _lastCompletedOrder.asStateFlow()

    init {
        viewModelScope.launch {
            repository.ensureUserProfile(currentUserDisplayName, currentUserEmail)
            repository.seedDefaultCatalogIfEmpty()
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
    }

    fun clearMessage() {
        _statusMessage.value = null
    }

    fun setCartQuantity(productId: String, qty: Int) {
        val product = products.value.firstOrNull { it.id == productId }
        val maxStock = (product?.stock ?: 99L).toInt()
        _cart.value = _cart.value.toMutableMap().apply {
            if (qty <= 0) remove(productId)
            else put(productId, qty.coerceAtMost(maxStock))
        }
    }

    fun applyCouponCode(code: String) {
        val clean = code.trim().uppercase()
        if (clean.isBlank()) {
            _appliedCoupon.value = null
            return
        }
        viewModelScope.launch {
            val found = repository.getCoupon(clean)
                ?: coupons.value.firstOrNull { it.code.equals(clean, ignoreCase = true) && it.isActive }
            if (found != null && found.isActive) {
                _appliedCoupon.value = found
                _statusMessage.value = "تم تفعيل كود الخصم ${found.code} بنجاح"
            } else {
                _statusMessage.value = "كود الخصم غير صالح أو غير مفعل"
            }
        }
    }

    fun clearLastCompletedOrder() {
        _lastCompletedOrder.value = null
    }

    fun submitOrder(
        customerName: String,
        customerPhone: String,
        governorate: String,
        shippingAddress: String,
        paymentMethod: EgyptPaymentMethod,
        transferReference: String,
        notes: String,
        onWhatsAppLaunch: (Order) -> Unit
    ) {
        val currentCart = _cart.value
        val cartProducts = products.value.filter { (currentCart[it.id] ?: 0) > 0 }
        if (cartProducts.isEmpty()) {
            _statusMessage.value = "سلة المشتريات فارغة"
            return
        }
        if (customerName.trim().length < 2) {
            _statusMessage.value = "يرجى كتابة الاسم الكامل"
            return
        }
        if (!EgyptStoreCatalog.isValidEgyptianPhone(customerPhone)) {
            _statusMessage.value = "أدخل رقم موبايل مصري صحيح (11 رقم يبدأ بـ 010/011/012/015)"
            return
        }
        if (shippingAddress.trim().length < 5) {
            _statusMessage.value = "يرجى كتابة العنوان التفصيلي"
            return
        }
        if (paymentMethod.needsReference && transferReference.trim().length < 3) {
            _statusMessage.value = "يرجى إدخال رقم مرجع التحويل"
            return
        }

        val subtotal = cartProducts.sumOf { it.price * (currentCart[it.id] ?: 0) }
        val shippingFee = EgyptStoreCatalog.shippingFeeFor(governorate)
        val discount = _appliedCoupon.value?.computeDiscount(subtotal) ?: 0.0
        val total = (subtotal - discount).coerceAtLeast(0.0) + shippingFee
        val summary = cartProducts.joinToString(" | ") { p ->
            "${p.name} × ${currentCart[p.id] ?: 1}"
        }
        val firstProd = cartProducts.first()

        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.createOrder(
                Order(
                    customerName = customerName,
                    customerPhone = customerPhone,
                    governorate = governorate,
                    shippingAddress = shippingAddress,
                    notes = notes.takeIf { it.isNotBlank() },
                    paymentMethod = paymentMethod.id,
                    transferReference = transferReference.takeIf { it.isNotBlank() },
                    couponCode = _appliedCoupon.value?.code,
                    itemsSummary = summary,
                    primaryProductId = firstProd.id,
                    primaryProductName = firstProd.name,
                    subtotal = subtotal,
                    shippingFee = shippingFee,
                    discountAmount = discount,
                    total = total
                )
            )
            _isBusy.value = false
            res.onSuccess { created ->
                _cart.value = emptyMap()
                _appliedCoupon.value = null
                _lastCompletedOrder.value = created
                _statusMessage.value = "تم تأكيد الطلب ${created.orderNumber} وإضافة نقاط الولاء"
                if (paymentMethod == EgyptPaymentMethod.WHATSAPP) {
                    onWhatsAppLaunch(created)
                }
            }.onFailure { err ->
                _statusMessage.value = err.localizedMessage ?: "تعذر إتمام الطلب"
            }
        }
    }

    fun submitPostOrderReview(
        order: Order,
        storeRating: Int,
        productRating: Int,
        comment: String,
        photoBase64: String?,
        onDone: () -> Unit
    ) {
        if (comment.trim().length < 2) {
            _statusMessage.value = "اكتب تعليقاً قصيراً عن تجربتك"
            return
        }
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.submitReview(
                CustomerReview(
                    orderNumber = order.orderNumber,
                    productId = order.primaryProductId,
                    productName = order.primaryProductName,
                    customerName = order.customerName,
                    governorate = order.governorate,
                    storeRating = storeRating.toLong(),
                    productRating = productRating.toLong(),
                    comment = comment.trim(),
                    photoBase64 = photoBase64
                )
            )
            _isBusy.value = false
            res.onSuccess {
                _statusMessage.value = "شكراً لك! تم نشر تقييمك وصورتك في آراء العملاء"
                _lastCompletedOrder.value = null
                onDone()
            }.onFailure {
                _statusMessage.value = it.localizedMessage ?: "تعذر حفظ التقييم"
            }
        }
    }

    suspend fun compressPickedPhoto(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val stream = context.contentResolver.openInputStream(uri) ?: return@withContext null
            val original = stream.use { BitmapFactory.decodeStream(it) } ?: return@withContext null
            val maxDim = 480
            val ratio = minOf(maxDim.toFloat() / original.width, maxDim.toFloat() / original.height, 1f)
            val targetW = (original.width * ratio).toInt().coerceAtLeast(1)
            val targetH = (original.height * ratio).toInt().coerceAtLeast(1)
            val scaled = Bitmap.createScaledBitmap(original, targetW, targetH, true)
            val out = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, 62, out)
            if (scaled != original) scaled.recycle()
            original.recycle()
            Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.w(TAG, "Image compression failed", e)
            null
        }
    }

    fun submitBooking(
        customerName: String,
        customerPhone: String,
        governorate: String,
        carModel: String,
        serviceType: String,
        preferredDate: String,
        notes: String,
        onDone: () -> Unit
    ) {
        if (customerName.trim().length < 2 || carModel.trim().length < 2 || preferredDate.trim().length < 4) {
            _statusMessage.value = "يرجى استكمال بيانات الحجز المطلوبة"
            return
        }
        if (!EgyptStoreCatalog.isValidEgyptianPhone(customerPhone)) {
            _statusMessage.value = "يرجى إدخال رقم موبايل مصري صحيح (11 رقم)"
            return
        }
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.createBooking(
                InstallationBooking(
                    customerName = customerName,
                    customerPhone = customerPhone,
                    governorate = governorate,
                    carModel = carModel,
                    serviceType = serviceType,
                    preferredDate = preferredDate,
                    notes = notes.takeIf { it.isNotBlank() }
                )
            )
            _isBusy.value = false
            res.onSuccess {
                _statusMessage.value = "تم تسجيل موعد التركيب بنجاح"
                onDone()
            }.onFailure {
                _statusMessage.value = it.localizedMessage ?: "تعذر تسجيل الحجز"
            }
        }
    }

    // --- Owner & Staff Operations ---
    fun addProduct(
        name: String,
        category: String,
        priceText: String,
        oldPriceText: String,
        stockText: String,
        badge: String,
        description: String,
        drawableKey: String
    ) {
        val price = priceText.toDoubleOrNull()
        val stock = stockText.toLongOrNull()
        if (name.trim().length < 2 || price == null || price < 0 || stock == null || stock < 0) {
            _statusMessage.value = "تحقق من اسم المنتج والسعر والمخزون"
            return
        }
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.addOrUpdateProduct(
                Product(
                    name = name.trim(),
                    category = category.trim().ifBlank { "تنظيم" },
                    price = price,
                    oldPrice = oldPriceText.toDoubleOrNull()?.takeIf { it >= price },
                    stock = stock,
                    badge = badge.trim().ifBlank { null },
                    description = description.trim(),
                    drawableKey = drawableKey,
                    isFeatured = true,
                    isActive = true
                )
            )
            _isBusy.value = false
            res.onSuccess { _statusMessage.value = "تمت إضافة المنتج بنجاح" }
                .onFailure { _statusMessage.value = it.localizedMessage ?: "ليست لديك صلاحية إضافة منتجات" }
        }
    }

    fun deleteProduct(productId: String) {
        viewModelScope.launch {
            repository.deleteProduct(productId)
                .onSuccess { _statusMessage.value = "تم حذف المنتج" }
                .onFailure { _statusMessage.value = it.localizedMessage ?: "تعذر حذف المنتج" }
        }
    }

    fun saveCoupon(code: String, discountType: String, valueText: String, minTotalText: String, isActive: Boolean) {
        val value = valueText.toDoubleOrNull()
        val minTotal = minTotalText.toDoubleOrNull() ?: 0.0
        if (code.trim().length < 2 || value == null || value <= 0) {
            _statusMessage.value = "أدخل كود وقيمة خصم صحيحة"
            return
        }
        viewModelScope.launch {
            repository.upsertCoupon(code, discountType, value, minTotal, isActive)
                .onSuccess { _statusMessage.value = "تم حفظ كود الخصم ${code.uppercase()}" }
                .onFailure { _statusMessage.value = it.localizedMessage ?: "تعذر حفظ الكوبون" }
        }
    }

    fun toggleCoupon(coupon: Coupon) {
        viewModelScope.launch {
            repository.upsertCoupon(
                coupon.code,
                coupon.discountType,
                coupon.discountValue,
                coupon.minOrderTotal,
                !coupon.isActive
            )
        }
    }

    fun deleteCoupon(code: String) {
        viewModelScope.launch {
            repository.deleteCoupon(code)
                .onSuccess { _statusMessage.value = "تم حذف الكوبون" }
        }
    }

    fun updateOrderStatus(orderId: String, status: String) {
        viewModelScope.launch {
            repository.updateOrderStatus(orderId, status)
                .onSuccess { _statusMessage.value = "تم تحديث حالة الطلب" }
        }
    }

    fun updateBookingStatus(bookingId: String, status: String) {
        viewModelScope.launch {
            repository.updateBookingStatus(bookingId, status)
                .onSuccess { _statusMessage.value = "تم تحديث حالة الحجز" }
        }
    }

    fun saveStaffPermission(
        email: String,
        displayName: String,
        canAddProducts: Boolean,
        canDeleteProducts: Boolean,
        canManageDiscounts: Boolean,
        canManageOrders: Boolean
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            repository.upsertStaffPermission(
                email = email,
                displayName = displayName,
                canAddProducts = canAddProducts,
                canDeleteProducts = canDeleteProducts,
                canManageDiscounts = canManageDiscounts,
                canManageOrders = canManageOrders
            ).onSuccess {
                _statusMessage.value = "تم حفظ صلاحيات الموظف بنجاح"
            }.onFailure {
                _statusMessage.value = it.localizedMessage ?: "تعذر حفظ صلاحيات الموظف"
            }
            _isBusy.value = false
        }
    }

    fun removeStaff(email: String) {
        viewModelScope.launch {
            repository.removeStaffPermission(email)
                .onSuccess { _statusMessage.value = "تم سحب صلاحيات الموظف" }
        }
    }

    private companion object {
        const val TAG = "BareeqVM"
    }
}
