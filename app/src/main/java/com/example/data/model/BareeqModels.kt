package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val email: String? = null,
    val fullName: String = "",
    val phone: String? = null,
    val role: String = "customer",
    val loyaltyPoints: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class LoyaltyEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val orderNumber: String = "",
    val pointsDelta: Long = 0L,
    val reason: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class StaffPermission(
    @DocumentId val id: String = "",
    val email: String = "",
    val displayName: String = "",
    val canAddProducts: Boolean = false,
    val canDeleteProducts: Boolean = false,
    val canManageDiscounts: Boolean = false,
    val canManageOrders: Boolean = false,
    val isActive: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Product(
    @DocumentId val id: String = "",
    val name: String = "",
    val category: String = "",
    val price: Double = 0.0,
    val oldPrice: Double? = null,
    val stock: Long = 0L,
    val badge: String? = null,
    val description: String = "",
    val drawableKey: String = "organizer",
    val isFeatured: Boolean = false,
    val isActive: Boolean = true,
    val createdBy: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = buildMap {
        put("name", name)
        put("category", category)
        put("price", price)
        oldPrice?.let { put("oldPrice", it) }
        put("stock", stock)
        badge?.takeIf { it.isNotBlank() }?.let { put("badge", it) }
        put("description", description)
        put("drawableKey", drawableKey)
        put("isFeatured", isFeatured)
        put("isActive", isActive)
        createdBy?.let { put("createdBy", it) }
        put("createdAt", FieldValue.serverTimestamp())
        put("updatedAt", FieldValue.serverTimestamp())
    }
}

@IgnoreExtraProperties
data class Coupon(
    @DocumentId val id: String = "",
    val code: String = "",
    val discountType: String = "percent",
    val discountValue: Double = 0.0,
    val minOrderTotal: Double = 0.0,
    val isActive: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun computeDiscount(subtotal: Double): Double {
        if (!isActive || subtotal < minOrderTotal) return 0.0
        return if (discountType == "fixed") {
            minOf(subtotal, discountValue)
        } else {
            kotlin.math.round((subtotal * discountValue / 100.0) * 100.0) / 100.0
        }
    }
}

@IgnoreExtraProperties
data class Order(
    @DocumentId val id: String = "",
    val userId: String = "",
    val orderNumber: String = "",
    val customerName: String = "",
    val customerPhone: String = "",
    val governorate: String = "القاهرة",
    val shippingAddress: String = "",
    val notes: String? = null,
    val paymentMethod: String = "cod",
    val transferReference: String? = null,
    val couponCode: String? = null,
    val itemsSummary: String = "",
    val primaryProductId: String = "",
    val primaryProductName: String = "",
    val subtotal: Double = 0.0,
    val shippingFee: Double = 0.0,
    val discountAmount: Double = 0.0,
    val total: Double = 0.0,
    val status: String = "pending",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CustomerReview(
    @DocumentId val id: String = "",
    val userId: String = "",
    val orderNumber: String = "",
    val productId: String? = null,
    val productName: String = "",
    val customerName: String = "",
    val governorate: String = "القاهرة",
    val storeRating: Long = 5L,
    val productRating: Long = 5L,
    val comment: String = "",
    val photoBase64: String? = null,
    val isApproved: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class InstallationBooking(
    @DocumentId val id: String = "",
    val userId: String = "",
    val customerName: String = "",
    val customerPhone: String = "",
    val governorate: String = "القاهرة",
    val carModel: String = "",
    val serviceType: String = "",
    val preferredDate: String = "",
    val notes: String? = null,
    val status: String = "pending",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
