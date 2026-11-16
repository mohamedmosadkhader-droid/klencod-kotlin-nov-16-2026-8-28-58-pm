package com.example.data.model

data class GovernorateOption(
    val name: String,
    val shippingFee: Double,
    val eta: String
)

enum class EgyptPaymentMethod(
    val id: String,
    val label: String,
    val hint: String,
    val needsReference: Boolean
) {
    COD(
        id = "cod",
        label = "الدفع عند الاستلام (كاش)",
        hint = "ادفع لمندوب التوصيل نقداً عند استلام الطلب.",
        needsReference = false
    ),
    WALLET(
        id = "wallet",
        label = "محفظة إلكترونية (فودافون / أورانج / اتصالات / WE)",
        hint = "حوّل على محفظة بريق: 01000000000 وأدخل رقم المرجع أو الرقم المحوّل منه.",
        needsReference = true
    ),
    INSTAPAY(
        id = "instapay",
        label = "إنستا باي (InstaPay)",
        hint = "حوّل على عنوان الدفع: bareeq@instapay وأدخل رقم مرجع العملية.",
        needsReference = true
    ),
    BANK(
        id = "bank",
        label = "تحويل بنكي (الأهلي / بنك مصر / CIB)",
        hint = "البنك الأهلي المصري — حساب رقم 00001234567890 — أدخل رقم المرجع.",
        needsReference = true
    ),
    WHATSAPP(
        id = "whatsapp",
        label = "تأكيد وإرسال الطلب عبر واتساب",
        hint = "سيتم حفظ الطلب وفتح رسالة الطلب جاهزة على واتساب المتجر.",
        needsReference = false
    );

    companion object {
        fun fromId(id: String): EgyptPaymentMethod =
            entries.firstOrNull { it.id == id } ?: COD
    }
}

data class PortfolioItem(
    val id: String,
    val title: String,
    val carModel: String,
    val summary: String,
    val beforeLabel: String,
    val afterLabel: String
)

object EgyptStoreCatalog {
    const val CURRENCY = "ج.م"
    const val BOOTSTRAPPED_OWNER_EMAIL = "mohamedmosadkhader@gmail.com"
    const val STORE_WHATSAPP_NUMBER = "201000000000"

    val governorates: List<GovernorateOption> = listOf(
        GovernorateOption("القاهرة", 55.0, "24 - 48 ساعة"),
        GovernorateOption("الجيزة", 55.0, "24 - 48 ساعة"),
        GovernorateOption("الإسكندرية", 75.0, "2 - 3 أيام"),
        GovernorateOption("القليوبية", 75.0, "2 - 3 أيام"),
        GovernorateOption("الدقهلية", 75.0, "2 - 3 أيام"),
        GovernorateOption("الشرقية", 75.0, "2 - 3 أيام"),
        GovernorateOption("الغربية", 75.0, "2 - 3 أيام"),
        GovernorateOption("المنوفية", 75.0, "2 - 3 أيام"),
        GovernorateOption("البحيرة", 75.0, "2 - 3 أيام"),
        GovernorateOption("كفر الشيخ", 75.0, "2 - 3 أيام"),
        GovernorateOption("دمياط", 75.0, "2 - 3 أيام"),
        GovernorateOption("بورسعيد", 75.0, "2 - 3 أيام"),
        GovernorateOption("الإسماعيلية", 75.0, "2 - 3 أيام"),
        GovernorateOption("السويس", 75.0, "2 - 3 أيام"),
        GovernorateOption("الفيوم", 95.0, "3 - 4 أيام"),
        GovernorateOption("بني سويف", 95.0, "3 - 4 أيام"),
        GovernorateOption("المنيا", 95.0, "3 - 4 أيام"),
        GovernorateOption("أسيوط", 95.0, "3 - 4 أيام"),
        GovernorateOption("سوهاج", 95.0, "3 - 4 أيام"),
        GovernorateOption("قنا", 120.0, "4 - 5 أيام"),
        GovernorateOption("الأقصر", 120.0, "4 - 5 أيام"),
        GovernorateOption("أسوان", 120.0, "4 - 5 أيام"),
        GovernorateOption("البحر الأحمر", 120.0, "4 - 5 أيام"),
        GovernorateOption("مطروح", 120.0, "4 - 5 أيام"),
        GovernorateOption("الوادي الجديد", 120.0, "4 - 6 أيام"),
        GovernorateOption("شمال سيناء", 120.0, "4 - 6 أيام"),
        GovernorateOption("جنوب سيناء", 120.0, "4 - 6 أيام")
    )

    fun shippingFeeFor(governorate: String): Double =
        governorates.firstOrNull { it.name == governorate }?.shippingFee ?: 75.0

    fun isValidEgyptianPhone(phone: String): Boolean {
        val normalized = phone.trim().replace(" ", "").replace("-", "")
        return Regex("^(010|011|012|015)[0-9]{8}$").matches(normalized)
    }

    fun orderStatusLabel(status: String): String = when (status) {
        "pending" -> "قيد المراجعة"
        "confirmed" -> "تم التأكيد"
        "processing" -> "جاري التجهيز"
        "shipped" -> "تم الشحن"
        "delivered" -> "تم التسليم"
        "cancelled" -> "ملغي"
        else -> status
    }

    val portfolioItems: List<PortfolioItem> = listOf(
        PortfolioItem(
            id = "bf1",
            title = "تحديث إضاءة ومقاعد المقصورة",
            carModel = "تويوتا كورولا 2024",
            summary = "تركيب إضاءة محيطية 64 لوناً مع طقم دعاسات جلد مبطنة وتنظيم كامل للكونسول.",
            beforeLabel = "مقصورة قياسية بدون إضاءة",
            afterLabel = "إضاءة ذهبية محيطية + تغليف فاخر"
        ),
        PortfolioItem(
            id = "bf2",
            title = "حماية وتلميع نانو سيراميك",
            carModel = "مرسيدس C200",
            summary = "معالجة الخدوش السطحية وتطبيق طبقة حماية ولمعان مع تلميع الجنوط.",
            beforeLabel = "بهتان وخدوش غسيل",
            afterLabel = "لمعان مرآتي وحماية طويلة"
        ),
        PortfolioItem(
            id = "bf3",
            title = "تجهيز مقصورة السائق والترفيه",
            carModel = "هيونداي توسان 2025",
            summary = "تثبيت حامل مغناطيسي مخفي، كسوة مقود رياضية، ومنظم أمتعة ذكي.",
            beforeLabel = "كابلات مكشوفة وفوضى",
            afterLabel = "تنظيم متكامل ومظهر أنيق"
        )
    )
}
