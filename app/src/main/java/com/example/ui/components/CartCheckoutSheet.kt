package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.Coupon
import com.example.data.model.EgyptPaymentMethod
import com.example.data.model.EgyptStoreCatalog
import com.example.data.model.Order
import com.example.data.model.Product
import com.example.ui.theme.CardSurface
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.DeepCarbon
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.GoldBorder
import com.example.ui.theme.LuxuryGold
import com.example.ui.theme.MetallicSilver
import com.example.ui.theme.MutedSlate
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.PearlIvory
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartCheckoutSheet(
    products: List<Product>,
    cart: Map<String, Int>,
    appliedCoupon: Coupon?,
    isBusy: Boolean,
    lastCompletedOrder: Order?,
    defaultName: String,
    defaultPhone: String,
    onDismiss: () -> Unit,
    onQuantityChange: (String, Int) -> Unit,
    onApplyCoupon: (String) -> Unit,
    onSubmitOrder: (
        name: String,
        phone: String,
        governorate: String,
        address: String,
        paymentMethod: EgyptPaymentMethod,
        transferRef: String,
        notes: String
    ) -> Unit,
    onCompressPhoto: suspend (Uri) -> String?,
    onSubmitReview: (Order, Int, Int, String, String?) -> Unit,
    onClearCompletedOrder: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var customerName by remember(defaultName) { mutableStateOf(defaultName) }
    var customerPhone by remember(defaultPhone) { mutableStateOf(defaultPhone) }
    var selectedGov by remember { mutableStateOf(EgyptStoreCatalog.governorates.first()) }
    var showGovMenu by remember { mutableStateOf(false) }
    var address by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf(EgyptPaymentMethod.COD) }
    var showPaymentMenu by remember { mutableStateOf(false) }
    var transferRef by remember { mutableStateOf("") }
    var couponInput by remember { mutableStateOf(appliedCoupon?.code ?: "") }
    var notes by remember { mutableStateOf("") }

    // Post-order review states
    var storeRating by remember { mutableIntStateOf(5) }
    var productRating by remember { mutableIntStateOf(5) }
    var reviewComment by remember { mutableStateOf("") }
    var photoBase64 by remember { mutableStateOf<String?>(null) }
    var compressingPhoto by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                compressingPhoto = true
                photoBase64 = onCompressPhoto(uri)
                compressingPhoto = false
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DeepCarbon,
        contentColor = PearlIvory
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (lastCompletedOrder != null) "تم تأكيد الطلب — قيّم تجربتك" else "سلة المشتريات وإتمام الطلب",
                    style = MaterialTheme.typography.titleLarge,
                    color = ChampagneGold
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "إغلاق السلة", tint = MetallicSilver)
                }
            }

            if (lastCompletedOrder != null) {
                Surface(
                    color = Color(0x1FD8AD45),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, GoldBorder, RoundedCornerShape(20.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = LuxuryGold, modifier = Modifier.size(42.dp))
                        Text("تم تسجيل طلبك بنجاح!", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("رقم الطلب: ${lastCompletedOrder.orderNumber}", color = ChampagneGold)
                        Text(
                            "الإجمالي شامل الشحن (${lastCompletedOrder.governorate}): ${lastCompletedOrder.total.toInt()} ${EgyptStoreCatalog.CURRENCY}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = PearlIvory
                        )
                    }
                }

                Surface(
                    color = CardSurface,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0x1FFFFFFF), RoundedCornerShape(20.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "قيّم المنتج (${lastCompletedOrder.primaryProductName}) ومتجر بريق",
                            style = MaterialTheme.typography.titleMedium,
                            color = ChampagneGold
                        )

                        StarRatingRow(
                            label = "تقييم المنتج:",
                            rating = productRating,
                            onSelect = { productRating = it }
                        )
                        StarRatingRow(
                            label = "تقييم المتجر:",
                            rating = storeRating,
                            onSelect = { storeRating = it }
                        )

                        OutlinedTextField(
                            value = reviewComment,
                            onValueChange = { reviewComment = it },
                            label = { Text("اكتب رأيك في المنتج والتوصيل") },
                            minLines = 2,
                            colors = bareeqFieldColors(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("review_comment_input")
                        )

                        OutlinedButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, GoldBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("pick_review_photo_button")
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, tint = ChampagneGold, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = when {
                                    compressingPhoto -> "جاري تجهيز الصورة..."
                                    photoBase64 != null -> "تم إرفاق صورة المنتج ✓"
                                    else -> "إضافة صورة المنتج (اختياري)"
                                },
                                color = ChampagneGold
                            )
                        }

                        Button(
                            onClick = {
                                onSubmitReview(
                                    lastCompletedOrder,
                                    storeRating,
                                    productRating,
                                    reviewComment,
                                    photoBase64
                                )
                            },
                            enabled = !isBusy && !compressingPhoto,
                            colors = ButtonDefaults.buttonColors(containerColor = LuxuryGold, contentColor = ObsidianBlack),
                            shape = CircleShape,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("submit_review_button")
                        ) {
                            Text("نشر التقييم في آراء العملاء", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                OutlinedButton(
                    onClick = {
                        onClearCompletedOrder()
                        onDismiss()
                    },
                    shape = CircleShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("تخطي والعودة للمتجر", color = MetallicSilver)
                }
            } else {
                val cartProducts = products.filter { (cart[it.id] ?: 0) > 0 }
                if (cartProducts.isEmpty()) {
                    Text(
                        text = "سلة المشتريات فارغة حالياً. أضف منتجاتك المفضلة من المتجر.",
                        color = MutedSlate,
                        modifier = Modifier.padding(vertical = 32.dp)
                    )
                } else {
                    cartProducts.forEach { prod ->
                        val qty = cart[prod.id] ?: 0
                        Surface(
                            color = CardSurface,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(prod.name, style = MaterialTheme.typography.titleMedium, color = PearlIvory)
                                    Text("${prod.price.toInt()} ${EgyptStoreCatalog.CURRENCY}", color = ChampagneGold, style = MaterialTheme.typography.bodyMedium)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = { onQuantityChange(prod.id, qty - 1) }, modifier = Modifier.size(44.dp)) {
                                        Icon(Icons.Default.Remove, contentDescription = "تقليل", tint = ChampagneGold)
                                    }
                                    Text("$qty", fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp))
                                    IconButton(onClick = { onQuantityChange(prod.id, qty + 1) }, modifier = Modifier.size(44.dp)) {
                                        Icon(Icons.Default.Add, contentDescription = "زيادة", tint = ChampagneGold)
                                    }
                                    IconButton(onClick = { onQuantityChange(prod.id, 0) }, modifier = Modifier.size(44.dp)) {
                                        Icon(Icons.Default.Delete, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }

                    // Coupon
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = couponInput,
                            onValueChange = { couponInput = it },
                            label = { Text("كود الخصم (مثال: BAREEQ10)") },
                            singleLine = true,
                            colors = bareeqFieldColors(),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("coupon_input")
                        )
                        Button(
                            onClick = { onApplyCoupon(couponInput) },
                            colors = ButtonDefaults.buttonColors(containerColor = LuxuryGold, contentColor = ObsidianBlack),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .height(54.dp)
                                .testTag("apply_coupon_button")
                        ) {
                            Text("تطبيق", fontWeight = FontWeight.Bold)
                        }
                    }

                    // Shipping & Egypt Info
                    OutlinedTextField(
                        value = customerName,
                        onValueChange = { customerName = it },
                        label = { Text("الاسم الكامل *") },
                        singleLine = true,
                        colors = bareeqFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("checkout_name_input")
                    )

                    OutlinedTextField(
                        value = customerPhone,
                        onValueChange = { customerPhone = it },
                        label = { Text("رقم الموبايل المصري (01xxxxxxxxx) *") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        colors = bareeqFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("checkout_phone_input")
                    )

                    // Governorate Picker
                    Box {
                        Surface(
                            color = CardSurface,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showGovMenu = true }
                                .border(1.dp, GoldBorder, RoundedCornerShape(14.dp))
                                .padding(14.dp)
                                .testTag("governorate_selector")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("المحافظة (مصر)", style = MaterialTheme.typography.labelSmall, color = LuxuryGold)
                                    Text(
                                        "${selectedGov.name} — شحن ${selectedGov.shippingFee.toInt()} ${EgyptStoreCatalog.CURRENCY} (${selectedGov.eta})",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = PearlIvory
                                    )
                                }
                                Icon(Icons.Default.ExpandMore, contentDescription = "اختر المحافظة", tint = ChampagneGold)
                            }
                        }
                        DropdownMenu(
                            expanded = showGovMenu,
                            onDismissRequest = { showGovMenu = false },
                            modifier = Modifier
                                .background(CardSurface)
                                .height(320.dp)
                        ) {
                            EgyptStoreCatalog.governorates.forEach { gov ->
                                DropdownMenuItem(
                                    text = {
                                        Text("${gov.name} (${gov.shippingFee.toInt()} ج.م - ${gov.eta})", color = PearlIvory)
                                    },
                                    onClick = {
                                        selectedGov = gov
                                        showGovMenu = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("العنوان بالتفصيل (المركز / الحي / الشارع) *") },
                        colors = bareeqFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("checkout_address_input")
                    )

                    // Payment Method Picker
                    Box {
                        Surface(
                            color = CardSurface,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showPaymentMenu = true }
                                .border(1.dp, GoldBorder, RoundedCornerShape(14.dp))
                                .padding(14.dp)
                                .testTag("payment_method_selector")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("طريقة الدفع المصرية", style = MaterialTheme.typography.labelSmall, color = LuxuryGold)
                                    Text(paymentMethod.label, style = MaterialTheme.typography.bodyLarge, color = PearlIvory)
                                    Text(paymentMethod.hint, style = MaterialTheme.typography.bodyMedium, color = MutedSlate)
                                }
                                Icon(Icons.Default.ExpandMore, contentDescription = "اختر طريقة الدفع", tint = ChampagneGold)
                            }
                        }
                        DropdownMenu(
                            expanded = showPaymentMenu,
                            onDismissRequest = { showPaymentMenu = false },
                            modifier = Modifier.background(CardSurface)
                        ) {
                            EgyptPaymentMethod.entries.forEach { method ->
                                DropdownMenuItem(
                                    text = { Text(method.label, color = PearlIvory) },
                                    onClick = {
                                        paymentMethod = method
                                        showPaymentMenu = false
                                    }
                                )
                            }
                        }
                    }

                    if (paymentMethod.needsReference) {
                        OutlinedTextField(
                            value = transferRef,
                            onValueChange = { transferRef = it },
                            label = { Text("رقم مرجع التحويل / رقم المحفظة المحوّل منها *") },
                            singleLine = true,
                            colors = bareeqFieldColors(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("transfer_reference_input")
                        )
                    }

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("ملاحظات (نوع السيارة أو موعد مفضل)") },
                        colors = bareeqFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    HorizontalDivider(color = Color(0x1FFFFFFF))

                    val subtotal = cartProducts.sumOf { it.price * (cart[it.id] ?: 0) }
                    val discount = appliedCoupon?.computeDiscount(subtotal) ?: 0.0
                    val total = (subtotal - discount).coerceAtLeast(0.0) + selectedGov.shippingFee

                    SummaryRow("المجموع الفرعي", "${subtotal.toInt()} ${EgyptStoreCatalog.CURRENCY}")
                    SummaryRow("الشحن (${selectedGov.name})", "${selectedGov.shippingFee.toInt()} ${EgyptStoreCatalog.CURRENCY}")
                    if (discount > 0) {
                        SummaryRow("الخصم (${appliedCoupon?.code})", "-${discount.toInt()} ${EgyptStoreCatalog.CURRENCY}", EmeraldSuccess)
                    }
                    SummaryRow("الإجمالي النهائي", "${total.toInt()} ${EgyptStoreCatalog.CURRENCY}", ChampagneGold, bold = true)

                    Button(
                        onClick = {
                            onSubmitOrder(
                                customerName,
                                customerPhone,
                                selectedGov.name,
                                address,
                                paymentMethod,
                                transferRef,
                                notes
                            )
                        },
                        enabled = !isBusy,
                        colors = ButtonDefaults.buttonColors(containerColor = LuxuryGold, contentColor = ObsidianBlack),
                        shape = CircleShape,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("confirm_order_button")
                    ) {
                        if (isBusy) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp), color = ObsidianBlack, strokeWidth = 2.dp)
                        } else {
                            Text("تأكيد الطلب الآن", fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun StarRatingRow(label: String, rating: Int, onSelect: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = PearlIvory)
        Row {
            (1..5).forEach { star ->
                IconButton(
                    onClick = { onSelect(star) },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = if (star <= rating) Icons.Filled.Star else Icons.Outlined.StarOutline,
                        contentDescription = "تقييم $star نجوم",
                        tint = LuxuryGold
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String, valueColor: Color = PearlIvory, bold: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = if (bold) PearlIvory else MutedSlate, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)
        Text(value, color = valueColor, fontWeight = if (bold) FontWeight.ExtraBold else FontWeight.SemiBold)
    }
}

@Composable
fun bareeqFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = LuxuryGold,
    unfocusedBorderColor = Color(0x33FFFFFF),
    focusedLabelColor = ChampagneGold,
    unfocusedLabelColor = MutedSlate,
    focusedTextColor = PearlIvory,
    unfocusedTextColor = PearlIvory,
    cursorColor = LuxuryGold
)
