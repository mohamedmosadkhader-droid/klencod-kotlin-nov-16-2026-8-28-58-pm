package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.model.EgyptStoreCatalog
import com.example.data.model.Order
import com.example.data.repository.BareeqRepository
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.authStateFlow
import com.example.ui.auth.signOutUser
import com.example.ui.components.CartCheckoutSheet
import com.example.ui.screens.AccountTrackingScreen
import com.example.ui.screens.AdminStaffScreen
import com.example.ui.screens.StorefrontScreen
import com.example.ui.theme.BareeqTheme
import com.example.ui.theme.CardSurface
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.DeepCarbon
import com.example.ui.theme.GoldBorder
import com.example.ui.theme.LuxuryGold
import com.example.ui.theme.MetallicSilver
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.PearlIvory
import com.example.ui.viewmodel.BareeqViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

private enum class BareeqTab { STORE, ACCOUNT, ADMIN }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BareeqTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val auth = Firebase.auth
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        AuthScreen(onAuthSuccess = {})
    } else {
        key(user.uid) {
            BareeqAuthenticatedApp(
                currentUserId = user.uid,
                currentUserEmail = user.email,
                currentUserDisplayName = user.displayName
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BareeqAuthenticatedApp(
    currentUserId: String,
    currentUserEmail: String?,
    currentUserDisplayName: String?
) {
    val viewModel: BareeqViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val dbId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(dbId)
                BareeqViewModel(
                    repository = BareeqRepository(db, Firebase.auth),
                    currentUserId = currentUserId,
                    currentUserEmail = currentUserEmail,
                    currentUserDisplayName = currentUserDisplayName
                )
            }
        }
    )

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val snackbarHostState = remember { SnackbarHostState() }

    val permissions by viewModel.permissions.collectAsStateWithLifecycle()
    val products by viewModel.products.collectAsStateWithLifecycle()
    val reviews by viewModel.reviews.collectAsStateWithLifecycle()
    val coupons by viewModel.coupons.collectAsStateWithLifecycle()
    val myOrders by viewModel.myOrders.collectAsStateWithLifecycle()
    val myBookings by viewModel.myBookings.collectAsStateWithLifecycle()
    val loyaltyLedger by viewModel.loyaltyLedger.collectAsStateWithLifecycle()
    val allOrders by viewModel.allOrders.collectAsStateWithLifecycle()
    val allBookings by viewModel.allBookings.collectAsStateWithLifecycle()
    val allStaff by viewModel.allStaff.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()

    val cart by viewModel.cart.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val appliedCoupon by viewModel.appliedCoupon.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()
    val lastCompletedOrder by viewModel.lastCompletedOrder.collectAsStateWithLifecycle()

    var currentTab by remember { mutableStateOf(BareeqTab.STORE) }
    var showCartSheet by remember { mutableStateOf(false) }

    if (currentTab != BareeqTab.STORE) {
        BackHandler { currentTab = BareeqTab.STORE }
    }

    LaunchedEffect(statusMessage) {
        statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    val cartCount = cart.values.sum()
    val cartSubtotal = products.sumOf { it.price * (cart[it.id] ?: 0) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(ObsidianBlack)) {
        val isWide = maxWidth >= 700.dp

        Scaffold(
            containerColor = ObsidianBlack,
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = DeepCarbon,
                        titleContentColor = ChampagneGold
                    ),
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = ChampagneGold)
                            Text("مصر", style = MaterialTheme.typography.labelSmall, color = MetallicSilver)
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { showCartSheet = true },
                            modifier = Modifier.size(48.dp).testTag("top_cart_button")
                        ) {
                            BadgedBox(
                                badge = {
                                    if (cartCount > 0) {
                                        Badge(containerColor = LuxuryGold, contentColor = ObsidianBlack) {
                                            Text("$cartCount")
                                        }
                                    }
                                }
                            ) {
                                Icon(Icons.Default.ShoppingBag, contentDescription = "السلة", tint = ChampagneGold)
                            }
                        }
                    }
                )
            },
            bottomBar = {
                if (!isWide) {
                    NavigationBar(containerColor = DeepCarbon, contentColor = ChampagneGold) {
                        NavigationBarItem(
                            selected = currentTab == BareeqTab.STORE,
                            onClick = { currentTab = BareeqTab.STORE },
                            icon = { Icon(Icons.Default.Storefront, contentDescription = "المتجر") },
                            label = { Text(stringResource(R.string.nav_store)) },
                            colors = bareeqNavBarColors()
                        )
                        NavigationBarItem(
                            selected = currentTab == BareeqTab.ACCOUNT,
                            onClick = { currentTab = BareeqTab.ACCOUNT },
                            icon = { Icon(Icons.Default.Person, contentDescription = "حسابي") },
                            label = { Text(stringResource(R.string.nav_account)) },
                            colors = bareeqNavBarColors()
                        )
                        if (permissions.hasAnyAdminAccess) {
                            NavigationBarItem(
                                selected = currentTab == BareeqTab.ADMIN,
                                onClick = { currentTab = BareeqTab.ADMIN },
                                icon = { Icon(Icons.Default.AdminPanelSettings, contentDescription = "الإدارة") },
                                label = { Text(stringResource(R.string.nav_admin)) },
                                colors = bareeqNavBarColors()
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isWide) {
                    NavigationRail(containerColor = DeepCarbon, modifier = Modifier.fillMaxHeight()) {
                        NavigationRailItem(
                            selected = currentTab == BareeqTab.STORE,
                            onClick = { currentTab = BareeqTab.STORE },
                            icon = { Icon(Icons.Default.Storefront, contentDescription = "المتجر") },
                            label = { Text(stringResource(R.string.nav_store)) },
                            colors = NavigationRailItemDefaults.colors(selectedIconColor = ObsidianBlack, selectedTextColor = ChampagneGold, indicatorColor = LuxuryGold)
                        )
                        NavigationRailItem(
                            selected = currentTab == BareeqTab.ACCOUNT,
                            onClick = { currentTab = BareeqTab.ACCOUNT },
                            icon = { Icon(Icons.Default.Person, contentDescription = "حسابي") },
                            label = { Text(stringResource(R.string.nav_account)) },
                            colors = NavigationRailItemDefaults.colors(selectedIconColor = ObsidianBlack, selectedTextColor = ChampagneGold, indicatorColor = LuxuryGold)
                        )
                        if (permissions.hasAnyAdminAccess) {
                            NavigationRailItem(
                                selected = currentTab == BareeqTab.ADMIN,
                                onClick = { currentTab = BareeqTab.ADMIN },
                                icon = { Icon(Icons.Default.AdminPanelSettings, contentDescription = "الإدارة") },
                                label = { Text(stringResource(R.string.nav_admin)) },
                                colors = NavigationRailItemDefaults.colors(selectedIconColor = ObsidianBlack, selectedTextColor = ChampagneGold, indicatorColor = LuxuryGold)
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Box(modifier = Modifier.widthIn(max = 760.dp).fillMaxSize()) {
                        when (currentTab) {
                            BareeqTab.STORE -> StorefrontScreen(
                                products = products,
                                reviews = reviews,
                                cart = cart,
                                searchQuery = searchQuery,
                                selectedCategory = selectedCategory,
                                defaultName = userProfile?.fullName ?: currentUserDisplayName.orEmpty(),
                                defaultPhone = userProfile?.phone.orEmpty(),
                                isBusy = isBusy,
                                onSearchChange = viewModel::updateSearchQuery,
                                onSelectCategory = viewModel::selectCategory,
                                onQuantityChange = viewModel::setCartQuantity,
                                onSubmitBooking = viewModel::submitBooking
                            )

                            BareeqTab.ACCOUNT -> AccountTrackingScreen(
                                userProfile = userProfile,
                                userEmail = currentUserEmail,
                                orders = myOrders,
                                bookings = myBookings,
                                loyaltyLedger = loyaltyLedger,
                                onSignOut = {
                                    signOutUser(credentialManager, scope)
                                }
                            )

                            BareeqTab.ADMIN -> AdminStaffScreen(
                                permissions = permissions,
                                products = products,
                                coupons = coupons,
                                orders = allOrders,
                                bookings = allBookings,
                                staffList = allStaff,
                                isBusy = isBusy,
                                onAddProduct = viewModel::addProduct,
                                onDeleteProduct = viewModel::deleteProduct,
                                onSaveCoupon = viewModel::saveCoupon,
                                onToggleCoupon = viewModel::toggleCoupon,
                                onDeleteCoupon = viewModel::deleteCoupon,
                                onUpdateOrderStatus = viewModel::updateOrderStatus,
                                onUpdateBookingStatus = viewModel::updateBookingStatus,
                                onSaveStaff = viewModel::saveStaffPermission,
                                onRemoveStaff = viewModel::removeStaff
                            )
                        }

                        if (cartCount > 0 && currentTab == BareeqTab.STORE) {
                            Surface(
                                color = CardSurface,
                                shape = RoundedCornerShape(22.dp),
                                shadowElevation = 10.dp,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                                    .fillMaxWidth()
                                    .border(1.2.dp, GoldBorder, RoundedCornerShape(22.dp))
                                    .clickable { showCartSheet = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("السلة · $cartCount منتج", style = MaterialTheme.typography.labelSmall, color = MetallicSilver)
                                        Text("${cartSubtotal.toInt()} ${EgyptStoreCatalog.CURRENCY}", style = MaterialTheme.typography.titleMedium, color = ChampagneGold, fontWeight = FontWeight.ExtraBold)
                                    }
                                    Button(
                                        onClick = { showCartSheet = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = LuxuryGold, contentColor = ObsidianBlack),
                                        shape = CircleShape,
                                        modifier = Modifier.height(48.dp).testTag("floating_checkout_button")
                                    ) {
                                        Text("إتمام الطلب ($cartCount)", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCartSheet) {
        CartCheckoutSheet(
            products = products,
            cart = cart,
            appliedCoupon = appliedCoupon,
            isBusy = isBusy,
            lastCompletedOrder = lastCompletedOrder,
            defaultName = userProfile?.fullName ?: currentUserDisplayName.orEmpty(),
            defaultPhone = userProfile?.phone.orEmpty(),
            onDismiss = { showCartSheet = false },
            onQuantityChange = viewModel::setCartQuantity,
            onApplyCoupon = viewModel::applyCouponCode,
            onSubmitOrder = { name, phone, gov, address, method, ref, notes ->
                viewModel.submitOrder(name, phone, gov, address, method, ref, notes) { createdOrder ->
                    launchWhatsAppOrder(context, createdOrder)
                }
            },
            onCompressPhoto = { uri -> viewModel.compressPickedPhoto(context, uri) },
            onSubmitReview = { order, storeRate, prodRate, comment, photo ->
                viewModel.submitPostOrderReview(order, storeRate, prodRate, comment, photo) {
                    showCartSheet = false
                }
            },
            onClearCompletedOrder = viewModel::clearLastCompletedOrder
        )
    }
}

@Composable
private fun bareeqNavBarColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = ObsidianBlack,
    selectedTextColor = ChampagneGold,
    indicatorColor = LuxuryGold,
    unselectedIconColor = MetallicSilver,
    unselectedTextColor = MetallicSilver
)

private fun launchWhatsAppOrder(context: android.content.Context, order: Order) {
    val msg = buildString {
        appendLine("مرحباً بريق مصر، طلب رقم: ${order.orderNumber}")
        appendLine("الاسم: ${order.customerName}")
        appendLine("الموبايل: ${order.customerPhone}")
        appendLine("المحافظة: ${order.governorate}")
        appendLine("العنوان: ${order.shippingAddress}")
        appendLine("المنتجات: ${order.itemsSummary}")
        appendLine("الشحن: ${order.shippingFee.toInt()} ج.م")
        if (order.discountAmount > 0) appendLine("الخصم: -${order.discountAmount.toInt()} ج.م")
        appendLine("الإجمالي النهائي: ${order.total.toInt()} ج.م")
    }
    val url = "https://wa.me/${EgyptStoreCatalog.STORE_WHATSAPP_NUMBER}?text=${Uri.encode(msg)}"
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    try {
        context.startActivity(intent)
    } catch (_: Exception) {
        // Handled gracefully if WhatsApp is not installed on emulator/device
    }
}