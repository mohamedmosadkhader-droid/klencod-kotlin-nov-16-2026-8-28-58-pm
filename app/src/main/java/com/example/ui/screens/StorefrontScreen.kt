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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.CustomerReview
import com.example.data.model.EgyptStoreCatalog
import com.example.data.model.Product
import com.example.ui.components.Interactive3DHeroCard
import com.example.ui.components.Interactive3DProductCard
import com.example.ui.components.InteractiveScrollReviewsSection
import com.example.ui.components.bareeqFieldColors
import com.example.ui.theme.CardSurface
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.DeepCarbon
import com.example.ui.theme.GoldBorder
import com.example.ui.theme.LuxuryGold
import com.example.ui.theme.MetallicSilver
import com.example.ui.theme.MutedSlate
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.PearlIvory
import kotlinx.coroutines.launch

@Composable
fun StorefrontScreen(
    products: List<Product>,
    reviews: List<CustomerReview>,
    cart: Map<String, Int>,
    searchQuery: String,
    selectedCategory: String,
    defaultName: String,
    defaultPhone: String,
    isBusy: Boolean,
    onSearchChange: (String) -> Unit,
    onSelectCategory: (String) -> Unit,
    onQuantityChange: (String, Int) -> Unit,
    onSubmitBooking: (
        name: String,
        phone: String,
        governorate: String,
        carModel: String,
        serviceType: String,
        preferredDate: String,
        notes: String,
        onDone: () -> Unit
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val categories = remember(products) {
        listOf("الكل") + products.map { it.category }.distinct()
    }
    val filteredProducts = remember(products, searchQuery, selectedCategory) {
        products.filter { p ->
            (selectedCategory == "الكل" || p.category == selectedCategory) &&
                (searchQuery.isBlank() ||
                    p.name.contains(searchQuery, ignoreCase = true) ||
                    p.description.contains(searchQuery, ignoreCase = true))
        }
    }

    // Booking form state
    var bName by remember(defaultName) { mutableStateOf(defaultName) }
    var bPhone by remember(defaultPhone) { mutableStateOf(defaultPhone) }
    var bGov by remember { mutableStateOf("القاهرة") }
    var showGovMenu by remember { mutableStateOf(false) }
    var bCar by remember { mutableStateOf("") }
    var bService by remember { mutableStateOf("تركيب إضاءة محيطية") }
    var showServiceMenu by remember { mutableStateOf(false) }
    var bDate by remember { mutableStateOf("2026-11-25") }
    var bNotes by remember { mutableStateOf("") }

    val serviceTypes = listOf(
        "تركيب إضاءة محيطية",
        "تركيب دعاسات وتنظيم داخلي",
        "تلميع وحماية نانو سيراميك",
        "تجهيز إلكترونيات السيارة"
    )

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                Interactive3DHeroCard(
                    onExploreClick = { scope.launch { listState.animateScrollToItem(2) } },
                    onBookClick = { scope.launch { listState.animateScrollToItem(filteredProducts.size + 4) } }
                )
            }
        }

        item {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("استخدم كود BAREEQ10 لخصم 10%", style = MaterialTheme.typography.labelSmall, color = LuxuryGold)
                        Text("منتجات بريق المختارة", style = MaterialTheme.typography.headlineMedium, color = PearlIvory)
                    }
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    placeholder = { Text("ابحث عن منتج أو إكسسوار...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "بحث", tint = LuxuryGold) },
                    singleLine = true,
                    shape = CircleShape,
                    colors = bareeqFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("product_search_input")
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(categories) { cat ->
                        val selected = cat == selectedCategory
                        FilterChip(
                            selected = selected,
                            onClick = { onSelectCategory(cat) },
                            label = { Text(cat, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = LuxuryGold,
                                selectedLabelColor = ObsidianBlack,
                                containerColor = CardSurface,
                                labelColor = MetallicSilver
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selected,
                                borderColor = Color(0x26FFFFFF),
                                selectedBorderColor = LuxuryGold
                            )
                        )
                    }
                }
            }
        }

        if (filteredProducts.isEmpty()) {
            item {
                Text(
                    text = "لا توجد منتجات مطابقة للبحث الحالي.",
                    color = MutedSlate,
                    modifier = Modifier.padding(24.dp)
                )
            }
        } else {
            items(filteredProducts, key = { it.id }) { prod ->
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Interactive3DProductCard(
                        product = prod,
                        quantityInCart = cart[prod.id] ?: 0,
                        onQuantityChange = { qty -> onQuantityChange(prod.id, qty) }
                    )
                }
            }
        }

        // Interactive Scroll Animation Customer Reviews
        item {
            InteractiveScrollReviewsSection(reviews = reviews)
        }

        // Before & After Showcase
        item {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("نتائج ملموسة في مقصورة سيارتك", style = MaterialTheme.typography.labelSmall, color = LuxuryGold)
                Text("معرض قبل وبعد", style = MaterialTheme.typography.headlineMedium, color = PearlIvory)

                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(EgyptStoreCatalog.portfolioItems, key = { it.id }) { item ->
                        Surface(
                            color = CardSurface,
                            shape = RoundedCornerShape(22.dp),
                            modifier = Modifier
                                .width(290.dp)
                                .border(1.dp, Color(0x1FFFFFFF), RoundedCornerShape(22.dp))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Surface(
                                        color = DeepCarbon,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text("قبل", style = MaterialTheme.typography.labelSmall, color = MutedSlate)
                                            Text(item.beforeLabel, style = MaterialTheme.typography.bodyMedium, color = MetallicSilver)
                                        }
                                    }
                                    Surface(
                                        color = Color(0x22D8AD45),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .border(0.8.dp, GoldBorder, RoundedCornerShape(12.dp))
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text("بعد بريق", style = MaterialTheme.typography.labelSmall, color = LuxuryGold)
                                            Text(item.afterLabel, style = MaterialTheme.typography.bodyMedium, color = ChampagneGold)
                                        }
                                    }
                                }
                                Text(item.carModel, style = MaterialTheme.typography.labelSmall, color = LuxuryGold)
                                Text(item.title, style = MaterialTheme.typography.titleMedium, color = PearlIvory)
                                Text(item.summary, style = MaterialTheme.typography.bodyMedium, color = MutedSlate)
                            }
                        }
                    }
                }
            }
        }

        // Installation Booking Form
        item {
            Surface(
                color = CardSurface,
                shape = RoundedCornerShape(26.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .border(1.dp, GoldBorder, RoundedCornerShape(26.dp))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Build, contentDescription = null, tint = LuxuryGold)
                        Text("احجز موعد تركيب احترافي", style = MaterialTheme.typography.titleLarge, color = ChampagneGold)
                    }
                    Text(
                        "فريقنا يجهز الإكسسوارات والإضاءة المحيطية باحترافية مع ضمان على التركيب.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MutedSlate
                    )

                    OutlinedTextField(
                        value = bName,
                        onValueChange = { bName = it },
                        label = { Text("الاسم الكامل *") },
                        singleLine = true,
                        colors = bareeqFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = bPhone,
                        onValueChange = { bPhone = it },
                        label = { Text("رقم الموبايل المصري (01xxxxxxxxx) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        colors = bareeqFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Box {
                        Surface(
                            color = DeepCarbon,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showGovMenu = true }
                                .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(14.dp))
                                .padding(14.dp)
                        ) {
                            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                                Text("المحافظة: $bGov", color = PearlIvory)
                                Icon(Icons.Default.ExpandMore, contentDescription = null, tint = ChampagneGold)
                            }
                        }
                        DropdownMenu(
                            expanded = showGovMenu,
                            onDismissRequest = { showGovMenu = false },
                            modifier = Modifier
                                .background(DeepCarbon)
                                .height(260.dp)
                        ) {
                            EgyptStoreCatalog.governorates.forEach { gov ->
                                DropdownMenuItem(
                                    text = { Text(gov.name, color = PearlIvory) },
                                    onClick = { bGov = gov.name; showGovMenu = false }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = bCar,
                        onValueChange = { bCar = it },
                        label = { Text("نوع وموديل السيارة (مثال: نيسان صني 2024) *") },
                        singleLine = true,
                        colors = bareeqFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Box {
                        Surface(
                            color = DeepCarbon,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showServiceMenu = true }
                                .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(14.dp))
                                .padding(14.dp)
                        ) {
                            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                                Text("الخدمة: $bService", color = PearlIvory)
                                Icon(Icons.Default.ExpandMore, contentDescription = null, tint = ChampagneGold)
                            }
                        }
                        DropdownMenu(
                            expanded = showServiceMenu,
                            onDismissRequest = { showServiceMenu = false },
                            modifier = Modifier.background(DeepCarbon)
                        ) {
                            serviceTypes.forEach { svc ->
                                DropdownMenuItem(
                                    text = { Text(svc, color = PearlIvory) },
                                    onClick = { bService = svc; showServiceMenu = false }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = bDate,
                        onValueChange = { bDate = it },
                        label = { Text("التاريخ المفضل (مثال: 2026-11-25) *") },
                        singleLine = true,
                        colors = bareeqFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = bNotes,
                        onValueChange = { bNotes = it },
                        label = { Text("ملاحظات إضافية") },
                        colors = bareeqFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            onSubmitBooking(bName, bPhone, bGov, bCar, bService, bDate, bNotes) {
                                bCar = ""
                                bNotes = ""
                            }
                        },
                        enabled = !isBusy,
                        colors = ButtonDefaults.buttonColors(containerColor = LuxuryGold, contentColor = ObsidianBlack),
                        shape = CircleShape,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("submit_booking_button")
                    ) {
                        Text("تأكيد حجز التركيب", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
