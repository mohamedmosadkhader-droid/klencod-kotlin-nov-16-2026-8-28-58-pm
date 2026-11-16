package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Loyalty
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.EgyptPaymentMethod
import com.example.data.model.EgyptStoreCatalog
import com.example.data.model.InstallationBooking
import com.example.data.model.LoyaltyEntry
import com.example.data.model.Order
import com.example.data.model.UserProfile
import com.example.ui.components.bareeqFieldColors
import com.example.ui.theme.CardSurface
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.GoldBorder
import com.example.ui.theme.LuxuryGold
import com.example.ui.theme.MetallicSilver
import com.example.ui.theme.MutedSlate
import com.example.ui.theme.PearlIvory

@Composable
fun AccountTrackingScreen(
    userProfile: UserProfile?,
    userEmail: String?,
    orders: List<Order>,
    bookings: List<InstallationBooking>,
    loyaltyLedger: List<LoyaltyEntry>,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    var trackQuery by remember { mutableStateOf("") }
    val trackedOrder = remember(trackQuery, orders) {
        val q = trackQuery.trim()
        if (q.isBlank()) null
        else orders.firstOrNull {
            it.orderNumber.equals(q, ignoreCase = true) || it.customerPhone.contains(q)
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Surface(
                color = CardSurface,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, GoldBorder, RoundedCornerShape(24.dp))
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = userProfile?.fullName ?: "عميل بريق",
                                style = MaterialTheme.typography.titleLarge,
                                color = ChampagneGold
                            )
                            Text(
                                text = userEmail ?: userProfile?.phone ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MetallicSilver
                            )
                        }
                        OutlinedButton(
                            onClick = onSignOut,
                            shape = CircleShape,
                            border = BorderStroke(1.dp, Color(0x44E74C3C)),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("sign_out_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "تسجيل الخروج", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.size(6.dp))
                            Text("خروج", color = MaterialTheme.colorScheme.error)
                        }
                    }

                    Surface(
                        color = Color(0x1FD8AD45),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.Loyalty, contentDescription = null, tint = LuxuryGold)
                                Text("نقاط الولاء المتاحة", color = PearlIvory)
                            }
                            Text(
                                text = "${userProfile?.loyaltyPoints ?: 0} نقطة",
                                style = MaterialTheme.typography.titleLarge,
                                color = ChampagneGold,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }
        }

        // Order Tracking
        item {
            Surface(
                color = CardSurface,
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.LocalShipping, contentDescription = null, tint = LuxuryGold)
                        Text("تتبع حالة الطلب", style = MaterialTheme.typography.titleMedium, color = ChampagneGold)
                    }
                    OutlinedTextField(
                        value = trackQuery,
                        onValueChange = { trackQuery = it },
                        label = { Text("ابحث برقم الطلب (BRQ-xxxxx) أو رقم الموبايل") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = LuxuryGold) },
                        singleLine = true,
                        colors = bareeqFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("track_order_input")
                    )
                    if (trackQuery.isNotBlank()) {
                        if (trackedOrder != null) {
                            Surface(
                                color = Color(0x222ECC71),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("رقم الطلب: ${trackedOrder.orderNumber}", fontWeight = FontWeight.Bold, color = PearlIvory)
                                    Text("الحالة: ${EgyptStoreCatalog.orderStatusLabel(trackedOrder.status)}", color = EmeraldSuccess, fontWeight = FontWeight.Bold)
                                    Text("المحافظة: ${trackedOrder.governorate} · الإجمالي: ${trackedOrder.total.toInt()} ${EgyptStoreCatalog.CURRENCY}", color = MetallicSilver)
                                }
                            }
                        } else {
                            Text("لم يتم العثور على طلب مطابق في سجل طلباتك.", color = MutedSlate, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }

        item {
            Text("سجل طلباتك (${orders.size})", style = MaterialTheme.typography.titleLarge, color = PearlIvory)
        }

        if (orders.isEmpty()) {
            item { Text("لا توجد طلبات مسجلة بعد.", color = MutedSlate) }
        } else {
            items(orders, key = { it.id }) { order ->
                Surface(
                    color = CardSurface,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(18.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                            Text(order.orderNumber, fontWeight = FontWeight.Bold, color = ChampagneGold)
                            Text(EgyptStoreCatalog.orderStatusLabel(order.status), color = LuxuryGold, fontWeight = FontWeight.Bold)
                        }
                        Text(order.itemsSummary, style = MaterialTheme.typography.bodyMedium, color = PearlIvory)
                        Text(
                            "${order.governorate} · ${EgyptPaymentMethod.fromId(order.paymentMethod).label}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MutedSlate
                        )
                        Text(
                            "الإجمالي: ${order.total.toInt()} ${EgyptStoreCatalog.CURRENCY} (شامل شحن ${order.shippingFee.toInt()} ج.م)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MetallicSilver
                        )
                    }
                }
            }
        }

        if (bookings.isNotEmpty()) {
            item {
                Text("حجوزات التركيب (${bookings.size})", style = MaterialTheme.typography.titleLarge, color = PearlIvory)
            }
            items(bookings, key = { it.id }) { bk ->
                Surface(color = CardSurface, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                            Text(bk.serviceType, fontWeight = FontWeight.Bold, color = ChampagneGold)
                            Text(bk.preferredDate, color = MetallicSilver, style = MaterialTheme.typography.labelSmall)
                        }
                        Text("${bk.carModel} · ${bk.governorate}", color = PearlIvory, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        if (loyaltyLedger.isNotEmpty()) {
            item {
                Text("حركة نقاط الولاء", style = MaterialTheme.typography.titleLarge, color = PearlIvory)
            }
            items(loyaltyLedger, key = { it.id }) { entry ->
                Surface(color = CardSurface, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(entry.reason, color = PearlIvory, style = MaterialTheme.typography.bodyMedium)
                        Text("+${entry.pointsDelta}", color = EmeraldSuccess, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
