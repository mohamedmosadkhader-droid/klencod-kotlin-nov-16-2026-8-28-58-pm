package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.Coupon
import com.example.data.model.EgyptStoreCatalog
import com.example.data.model.InstallationBooking
import com.example.data.model.Order
import com.example.data.model.Product
import com.example.data.model.StaffPermission
import com.example.ui.components.bareeqFieldColors
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
import com.example.ui.viewmodel.EffectivePermissions

@Composable
fun AdminStaffScreen(
    permissions: EffectivePermissions,
    products: List<Product>,
    coupons: List<Coupon>,
    orders: List<Order>,
    bookings: List<InstallationBooking>,
    staffList: List<StaffPermission>,
    isBusy: Boolean,
    onAddProduct: (name: String, category: String, price: String, oldPrice: String, stock: String, badge: String, desc: String, drawableKey: String) -> Unit,
    onDeleteProduct: (String) -> Unit,
    onSaveCoupon: (code: String, type: String, valText: String, minText: String, active: Boolean) -> Unit,
    onToggleCoupon: (Coupon) -> Unit,
    onDeleteCoupon: (String) -> Unit,
    onUpdateOrderStatus: (String, String) -> Unit,
    onUpdateBookingStatus: (String, String) -> Unit,
    onSaveStaff: (email: String, name: String, addP: Boolean, delP: Boolean, disc: Boolean, ord: Boolean) -> Unit,
    onRemoveStaff: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val availableTabs = remember(permissions) {
        buildList {
            if (permissions.canAddProducts || permissions.canDeleteProducts) add(0 to "المنتجات")
            if (permissions.canManageDiscounts) add(1 to "الخصومات")
            if (permissions.canManageOrders) add(2 to "الطلبات والحجوزات")
            if (permissions.isOwner) add(3 to "الموظفين والصلاحيات")
        }
    }
    var selectedTab by remember(availableTabs) { mutableIntStateOf(availableTabs.firstOrNull()?.first ?: 0) }

    // Add product fields
    var pName by remember { mutableStateOf("") }
    var pCategory by remember { mutableStateOf("تنظيم") }
    var pPrice by remember { mutableStateOf("") }
    var pOldPrice by remember { mutableStateOf("") }
    var pStock by remember { mutableStateOf("10") }
    var pBadge by remember { mutableStateOf("") }
    var pDesc by remember { mutableStateOf("") }
    var pDrawable by remember { mutableStateOf("organizer") }

    // Coupon fields
    var cCode by remember { mutableStateOf("") }
    var cType by remember { mutableStateOf("percent") }
    var cVal by remember { mutableStateOf("") }
    var cMin by remember { mutableStateOf("0") }

    // Staff fields
    var sEmail by remember { mutableStateOf("") }
    var sName by remember { mutableStateOf("") }
    var sAdd by remember { mutableStateOf(true) }
    var sDel by remember { mutableStateOf(false) }
    var sDisc by remember { mutableStateOf(false) }
    var sOrd by remember { mutableStateOf(true) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Surface(
                color = CardSurface,
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, GoldBorder, RoundedCornerShape(22.dp))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = LuxuryGold, modifier = Modifier.size(30.dp))
                    Column {
                        Text(
                            text = if (permissions.isOwner) "لوحة المالك (تحكم كامل)" else "لوحة الموظف",
                            style = MaterialTheme.typography.titleLarge,
                            color = ChampagneGold
                        )
                        Text(
                            text = if (permissions.isOwner) "إدارة المنتجات، الخصومات، الطلبات، وصلاحيات الموظفين"
                            else "تعرض الصلاحيات الممنوحة لك فقط من المالك",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MetallicSilver
                        )
                    }
                }
            }
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(availableTabs) { (id, title) ->
                    FilterChip(
                        selected = selectedTab == id,
                        onClick = { selectedTab = id },
                        label = { Text(title, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = LuxuryGold,
                            selectedLabelColor = ObsidianBlack,
                            containerColor = CardSurface,
                            labelColor = MetallicSilver
                        )
                    )
                }
            }
        }

        // TAB 0: Products
        if (selectedTab == 0) {
            if (permissions.canAddProducts) {
                item {
                    Surface(color = CardSurface, shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("إضافة منتج جديد (بالجنيه المصري)", style = MaterialTheme.typography.titleMedium, color = ChampagneGold)
                            OutlinedTextField(value = pName, onValueChange = { pName = it }, label = { Text("اسم المنتج *") }, colors = bareeqFieldColors(), modifier = Modifier.fillMaxWidth().testTag("admin_product_name"))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(value = pCategory, onValueChange = { pCategory = it }, label = { Text("القسم") }, colors = bareeqFieldColors(), modifier = Modifier.weight(1f))
                                OutlinedTextField(value = pBadge, onValueChange = { pBadge = it }, label = { Text("شارة (اختياري)") }, colors = bareeqFieldColors(), modifier = Modifier.weight(1f))
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(value = pPrice, onValueChange = { pPrice = it }, label = { Text("السعر ج.م *") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), colors = bareeqFieldColors(), modifier = Modifier.weight(1f).testTag("admin_product_price"))
                                OutlinedTextField(value = pOldPrice, onValueChange = { pOldPrice = it }, label = { Text("قبل الخصم") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), colors = bareeqFieldColors(), modifier = Modifier.weight(1f))
                                OutlinedTextField(value = pStock, onValueChange = { pStock = it }, label = { Text("الكمية") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), colors = bareeqFieldColors(), modifier = Modifier.weight(1f))
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("organizer" to "تنظيم", "lighting" to "إضاءة", "care" to "عناية", "hero" to "عام").forEach { (key, label) ->
                                    FilterChip(
                                        selected = pDrawable == key,
                                        onClick = { pDrawable = key },
                                        label = { Text(label) }
                                    )
                                }
                            }
                            OutlinedTextField(value = pDesc, onValueChange = { pDesc = it }, label = { Text("وصف المنتج") }, colors = bareeqFieldColors(), modifier = Modifier.fillMaxWidth())
                            Button(
                                onClick = {
                                    onAddProduct(pName, pCategory, pPrice, pOldPrice, pStock, pBadge, pDesc, pDrawable)
                                    pName = ""; pPrice = ""; pOldPrice = ""; pDesc = ""
                                },
                                enabled = !isBusy,
                                colors = ButtonDefaults.buttonColors(containerColor = LuxuryGold, contentColor = ObsidianBlack),
                                shape = CircleShape,
                                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("admin_add_product_button")
                            ) {
                                Text("حفظ وإضافة المنتج", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            items(products, key = { it.id }) { prod ->
                Surface(color = CardSurface, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(prod.name, fontWeight = FontWeight.Bold, color = PearlIvory)
                            Text("${prod.price.toInt()} ${EgyptStoreCatalog.CURRENCY} · مخزون ${prod.stock}", color = ChampagneGold, style = MaterialTheme.typography.bodyMedium)
                        }
                        if (permissions.canDeleteProducts) {
                            IconButton(onClick = { onDeleteProduct(prod.id) }, modifier = Modifier.size(48.dp)) {
                                Icon(Icons.Default.Delete, contentDescription = "حذف المنتج", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }

        // TAB 1: Discounts & Coupons
        if (selectedTab == 1 && permissions.canManageDiscounts) {
            item {
                Surface(color = CardSurface, shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("إضافة أو تعديل كوبون خصم", style = MaterialTheme.typography.titleMedium, color = ChampagneGold)
                        OutlinedTextField(value = cCode, onValueChange = { cCode = it }, label = { Text("كود الخصم بالإنجليزية (مثال: EGYPT15)") }, colors = bareeqFieldColors(), modifier = Modifier.fillMaxWidth().testTag("admin_coupon_code"))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = cType == "percent", onClick = { cType = "percent" }, label = { Text("نسبة مئوية (%)") })
                            FilterChip(selected = cType == "fixed", onClick = { cType = "fixed" }, label = { Text("مبلغ ثابت (ج.م)") })
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = cVal, onValueChange = { cVal = it }, label = { Text("قيمة الخصم *") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), colors = bareeqFieldColors(), modifier = Modifier.weight(1f))
                            OutlinedTextField(value = cMin, onValueChange = { cMin = it }, label = { Text("الحد الأدنى ج.م") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), colors = bareeqFieldColors(), modifier = Modifier.weight(1f))
                        }
                        Button(
                            onClick = {
                                onSaveCoupon(cCode, cType, cVal, cMin, true)
                                cCode = ""; cVal = ""
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = LuxuryGold, contentColor = ObsidianBlack),
                            shape = CircleShape,
                            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("admin_save_coupon_button")
                        ) {
                            Text("حفظ الكوبون", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            items(coupons, key = { it.code }) { coupon ->
                Surface(color = CardSurface, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(coupon.code, fontWeight = FontWeight.ExtraBold, color = ChampagneGold)
                            val unit = if (coupon.discountType == "percent") "%" else " ج.م"
                            Text("خصم ${coupon.discountValue.toInt()}$unit (أقل طلب ${coupon.minOrderTotal.toInt()} ج.م)", color = MetallicSilver, style = MaterialTheme.typography.bodyMedium)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Switch(
                                checked = coupon.isActive,
                                onCheckedChange = { onToggleCoupon(coupon) },
                                colors = SwitchDefaults.colors(checkedThumbColor = ObsidianBlack, checkedTrackColor = LuxuryGold)
                            )
                            IconButton(onClick = { onDeleteCoupon(coupon.code) }, modifier = Modifier.size(48.dp)) {
                                Icon(Icons.Default.Delete, contentDescription = "حذف الكوبون", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }

        // TAB 2: Orders & Bookings
        if (selectedTab == 2 && permissions.canManageOrders) {
            item {
                val totalRevenue = orders.filter { it.status != "cancelled" }.sumOf { it.total }
                Surface(color = Color(0x1FD8AD45), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), Arrangement.SpaceBetween) {
                        Column {
                            Text("إجمالي المبيعات", style = MaterialTheme.typography.labelSmall, color = ChampagneGold)
                            Text("${totalRevenue.toInt()} ${EgyptStoreCatalog.CURRENCY}", style = MaterialTheme.typography.titleLarge, color = PearlIvory, fontWeight = FontWeight.ExtraBold)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("عدد الطلبات", style = MaterialTheme.typography.labelSmall, color = ChampagneGold)
                            Text("${orders.size}", style = MaterialTheme.typography.titleLarge, color = PearlIvory, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }

            items(orders, key = { it.id }) { order ->
                var showStatusMenu by remember { mutableStateOf(false) }
                Surface(color = CardSurface, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                            Text("${order.orderNumber} — ${order.customerName}", fontWeight = FontWeight.Bold, color = ChampagneGold)
                            Box {
                                Surface(
                                    color = DeepCarbon,
                                    shape = CircleShape,
                                    modifier = Modifier
                                        .clickable { showStatusMenu = true }
                                        .border(1.dp, GoldBorder, CircleShape)
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(EgyptStoreCatalog.orderStatusLabel(order.status), style = MaterialTheme.typography.labelSmall, color = EmeraldSuccess)
                                        Icon(Icons.Default.ExpandMore, contentDescription = null, tint = ChampagneGold, modifier = Modifier.size(16.dp))
                                    }
                                }
                                DropdownMenu(expanded = showStatusMenu, onDismissRequest = { showStatusMenu = false }, modifier = Modifier.background(DeepCarbon)) {
                                    listOf("pending", "confirmed", "processing", "shipped", "delivered", "cancelled").forEach { st ->
                                        DropdownMenuItem(
                                            text = { Text(EgyptStoreCatalog.orderStatusLabel(st), color = PearlIvory) },
                                            onClick = { onUpdateOrderStatus(order.id, st); showStatusMenu = false }
                                        )
                                    }
                                }
                            }
                        }
                        Text("${order.governorate} · ${order.customerPhone} · ${order.total.toInt()} ج.م", color = MetallicSilver, style = MaterialTheme.typography.bodyMedium)
                        Text(order.itemsSummary, color = PearlIvory, style = MaterialTheme.typography.bodyMedium)
                        order.transferReference?.let { Text("مرجع التحويل: $it", color = LuxuryGold, style = MaterialTheme.typography.labelSmall) }
                    }
                }
            }

            if (bookings.isNotEmpty()) {
                item { Text("حجوزات التركيب (${bookings.size})", style = MaterialTheme.typography.titleMedium, color = ChampagneGold) }
                items(bookings, key = { it.id }) { bk ->
                    Surface(color = CardSurface, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("${bk.customerName} (${bk.customerPhone}) — ${bk.governorate}", fontWeight = FontWeight.Bold, color = PearlIvory)
                            Text("${bk.carModel} · ${bk.serviceType} · ${bk.preferredDate}", color = ChampagneGold, style = MaterialTheme.typography.bodyMedium)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("pending" to "معلق", "confirmed" to "مؤكد", "completed" to "مكتمل").forEach { (st, lbl) ->
                                    FilterChip(selected = bk.status == st, onClick = { onUpdateBookingStatus(bk.id, st) }, label = { Text(lbl) })
                                }
                            }
                        }
                    }
                }
            }
        }

        // TAB 3: Owner Staff Permissions Management
        if (selectedTab == 3 && permissions.isOwner) {
            item {
                Surface(color = CardSurface, shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("إضافة موظف وتحديد صلاحياته", style = MaterialTheme.typography.titleMedium, color = ChampagneGold)
                        Text("أدخل البريد الإلكتروني لحساب Google الخاص بالموظف واختر الصلاحيات:", style = MaterialTheme.typography.bodyMedium, color = MutedSlate)

                        OutlinedTextField(value = sEmail, onValueChange = { sEmail = it }, label = { Text("بريد Google للموظف *") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), colors = bareeqFieldColors(), modifier = Modifier.fillMaxWidth().testTag("staff_email_input"))
                        OutlinedTextField(value = sName, onValueChange = { sName = it }, label = { Text("اسم الموظف") }, colors = bareeqFieldColors(), modifier = Modifier.fillMaxWidth())

                        PermissionCheckRow("إضافة وتعديل المنتجات", sAdd) { sAdd = it }
                        PermissionCheckRow("حذف المنتجات", sDel) { sDel = it }
                        PermissionCheckRow("إدارة الخصومات والكوبونات", sDisc) { sDisc = it }
                        PermissionCheckRow("إدارة الطلبات وحجوزات التركيب", sOrd) { sOrd = it }

                        Button(
                            onClick = {
                                onSaveStaff(sEmail, sName, sAdd, sDel, sDisc, sOrd)
                                sEmail = ""; sName = ""
                            },
                            enabled = !isBusy,
                            colors = ButtonDefaults.buttonColors(containerColor = LuxuryGold, contentColor = ObsidianBlack),
                            shape = CircleShape,
                            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("save_staff_button")
                        ) {
                            Text("حفظ صلاحيات الموظف", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            items(staffList, key = { it.email }) { staff ->
                Surface(color = CardSurface, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("${staff.displayName} (${staff.email})", fontWeight = FontWeight.Bold, color = PearlIvory)
                            val badges = buildList {
                                if (staff.canAddProducts) add("إضافة منتجات")
                                if (staff.canDeleteProducts) add("حذف منتجات")
                                if (staff.canManageDiscounts) add("خصومات")
                                if (staff.canManageOrders) add("طلبات")
                            }.joinToString(" · ").ifBlank { "بدون صلاحيات" }
                            Text(badges, style = MaterialTheme.typography.labelSmall, color = ChampagneGold)
                        }
                        IconButton(onClick = { onRemoveStaff(staff.email) }, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = "حذف الموظف", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionCheckRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(checkedColor = LuxuryGold, checkmarkColor = ObsidianBlack)
        )
        Text(label, color = PearlIvory, style = MaterialTheme.typography.bodyMedium)
    }
}
