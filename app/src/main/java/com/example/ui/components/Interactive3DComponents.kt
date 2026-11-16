package com.example.ui.components

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.CustomerReview
import com.example.data.model.EgyptStoreCatalog
import com.example.data.model.Product
import com.example.ui.theme.CardSurface
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.DeepCarbon
import com.example.ui.theme.GoldBorder
import com.example.ui.theme.LuxuryGold
import com.example.ui.theme.MetallicSilver
import com.example.ui.theme.MutedSlate
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.PearlIvory
import kotlin.math.abs
import kotlin.math.roundToInt

fun drawableResForKey(key: String): Int = when (key) {
    "organizer" -> R.drawable.img_prod_organizer_1794849455049
    "lighting" -> R.drawable.img_prod_lighting_1794849455546
    "care" -> R.drawable.img_prod_care_1794849456331
    else -> R.drawable.img_hero_bareeq_1794849440661
}

@Composable
fun Interactive3DHeroCard(
    onExploreClick: () -> Unit,
    onBookClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var targetRotX by remember { mutableFloatStateOf(0f) }
    var targetRotY by remember { mutableFloatStateOf(0f) }

    val animRotX by animateFloatAsState(
        targetValue = targetRotX,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "heroRotX"
    )
    val animRotY by animateFloatAsState(
        targetValue = targetRotY,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "heroRotY"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "heroIdle")
    val floatY by infiniteTransition.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(tween(2600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "floatY"
    )
    val density = LocalDensity.current.density

    Box(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        targetRotX = 0f
                        targetRotY = 0f
                    },
                    onDragCancel = {
                        targetRotX = 0f
                        targetRotY = 0f
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        targetRotY = (targetRotY + dragAmount.x * 0.14f).coerceIn(-16f, 16f)
                        targetRotX = (targetRotX - dragAmount.y * 0.14f).coerceIn(-12f, 12f)
                    }
                )
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    rotationX = animRotX
                    rotationY = animRotY
                    cameraDistance = 14f * density
                }
                .clip(RoundedCornerShape(28.dp))
                .background(DeepCarbon)
                .border(1.2.dp, GoldBorder, RoundedCornerShape(28.dp))
                .drawBehind {
                    val centerX = size.width * (0.5f + animRotY / 40f)
                    val centerY = size.height * (0.35f - animRotX / 40f)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0x40D8AD45), Color.Transparent),
                            center = Offset(centerX, centerY),
                            radius = size.maxDimension * 0.65f
                        )
                    )
                }
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp)
                        .clip(RoundedCornerShape(20.dp))
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_hero_bareeq_1794849440661),
                        contentDescription = "عرض بريق 3D الفاخر",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = 1.04f
                                scaleY = 1.04f
                                translationX = animRotY * 1.4f
                                translationY = -animRotX * 1.2f
                            }
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color(0xCC090909))
                                )
                            )
                    )

                    Surface(
                        color = Color(0xCC090909),
                        shape = CircleShape,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(10.dp)
                            .offset { IntOffset(0, floatY.roundToInt()) }
                            .border(1.dp, GoldBorder, CircleShape)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(Icons.Default.TouchApp, contentDescription = null, tint = LuxuryGold, modifier = Modifier.size(14.dp))
                            Text("حرّك بإصبعك 3D", style = MaterialTheme.typography.labelSmall, color = ChampagneGold)
                        }
                    }

                    Surface(
                        color = LuxuryGold,
                        shape = CircleShape,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "شحن 27 محافظة",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = ObsidianBlack,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }

                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(14.dp)
                    ) {
                        Text(
                            text = "كل ما يضيف لمسة إلى سيارتك",
                            style = MaterialTheme.typography.titleLarge,
                            color = PearlIvory,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "فودافون كاش · إنستا باي · تحويل بنكي · كاش",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ChampagneGold
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onExploreClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LuxuryGold,
                            contentColor = ObsidianBlack
                        ),
                        shape = CircleShape,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("hero_shop_button")
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("اكتشف المنتجات", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onBookClick,
                        shape = CircleShape,
                        border = androidx.compose.foundation.BorderStroke(1.dp, GoldBorder),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("hero_book_button")
                    ) {
                        Text("حجز تركيب", color = ChampagneGold, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
fun Interactive3DProductCard(
    product: Product,
    quantityInCart: Int,
    onQuantityChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var tiltX by remember { mutableFloatStateOf(0f) }
    var tiltY by remember { mutableFloatStateOf(0f) }
    val animRotX by animateFloatAsState(tiltX, spring(stiffness = Spring.StiffnessMediumLow), label = "pRotX")
    val animRotY by animateFloatAsState(tiltY, spring(stiffness = Spring.StiffnessMediumLow), label = "pRotY")
    val density = LocalDensity.current.density

    Surface(
        color = CardSurface,
        shape = RoundedCornerShape(22.dp),
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                rotationX = animRotX
                rotationY = animRotY
                cameraDistance = 12f * density
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = { tiltX = 0f; tiltY = 0f },
                    onDragCancel = { tiltX = 0f; tiltY = 0f },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        tiltY = (tiltY + dragAmount.x * 0.12f).coerceIn(-10f, 10f)
                        tiltX = (tiltX - dragAmount.y * 0.12f).coerceIn(-8f, 8f)
                    }
                )
            }
            .border(1.dp, if (quantityInCart > 0) LuxuryGold else Color(0x1FFFFFFF), RoundedCornerShape(22.dp))
            .testTag("product_card_${product.id}")
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(165.dp)
            ) {
                Image(
                    painter = painterResource(id = drawableResForKey(product.drawableKey)),
                    contentDescription = product.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color(0xAA090909))
                            )
                        )
                )
                product.badge?.takeIf { it.isNotBlank() }?.let { badge ->
                    Surface(
                        color = LuxuryGold,
                        shape = CircleShape,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(10.dp)
                    ) {
                        Text(
                            text = badge,
                            color = ObsidianBlack,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
                Surface(
                    color = Color(0xBF090909),
                    shape = CircleShape,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                ) {
                    Text(
                        text = product.category,
                        color = MetallicSilver,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = PearlIvory,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = product.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedSlate,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "${product.price.toInt()} ${EgyptStoreCatalog.CURRENCY}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = ChampagneGold
                        )
                        product.oldPrice?.let { old ->
                            Text(
                                text = "${old.toInt()} ${EgyptStoreCatalog.CURRENCY}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MutedSlate,
                                textDecoration = TextDecoration.LineThrough
                            )
                        }
                    }
                    Text(
                        text = if (product.stock > 0) "متبقي ${product.stock}" else "نفدت الكمية",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (product.stock > 0) MetallicSilver else MaterialTheme.colorScheme.error
                    )
                }

                AnimatedVisibility(visible = quantityInCart > 0) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(CircleShape)
                            .border(1.dp, LuxuryGold, CircleShape)
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { onQuantityChange(quantityInCart - 1) },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "تقليل الكمية", tint = ChampagneGold)
                        }
                        Text(
                            text = "$quantityInCart في السلة",
                            style = MaterialTheme.typography.labelLarge,
                            color = PearlIvory
                        )
                        IconButton(
                            onClick = { onQuantityChange((quantityInCart + 1).coerceAtMost(product.stock.toInt())) },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "زيادة الكمية", tint = ChampagneGold)
                        }
                    }
                }

                if (quantityInCart == 0) {
                    OutlinedButton(
                        onClick = { onQuantityChange(1) },
                        enabled = product.stock > 0,
                        shape = CircleShape,
                        border = androidx.compose.foundation.BorderStroke(1.dp, GoldBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("add_to_cart_${product.id}")
                    ) {
                        Text(
                            text = "أضف للسلة",
                            color = ChampagneGold,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun InteractiveScrollReviewsSection(
    reviews: List<CustomerReview>,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val density = LocalDensity.current.density

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column {
                Text(
                    text = "تجارب حقيقية لعملاء بريق في مصر",
                    style = MaterialTheme.typography.labelSmall,
                    color = LuxuryGold
                )
                Text(
                    text = "آراء العملاء وصور المنتجات",
                    style = MaterialTheme.typography.headlineMedium,
                    color = PearlIvory
                )
            }
            Text(
                text = "اسحب للتفاعل ✦",
                style = MaterialTheme.typography.labelSmall,
                color = ChampagneGold
            )
        }

        Spacer(Modifier.height(14.dp))

        LazyRow(
            state = listState,
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            itemsIndexed(reviews, key = { idx, item -> "${item.id}_$idx" }) { index, review ->
                val scrollFactor by remember {
                    derivedStateOf {
                        val info = listState.layoutInfo
                        val itemInfo = info.visibleItemsInfo.firstOrNull { it.index == index }
                        if (itemInfo == null || info.viewportSize.width == 0) 0f
                        else {
                            val center = info.viewportStartOffset + info.viewportSize.width / 2f
                            val itemCenter = itemInfo.offset + itemInfo.size / 2f
                            ((itemCenter - center) / (info.viewportSize.width / 2f)).coerceIn(-1.2f, 1.2f)
                        }
                    }
                }

                val decodedPhoto: ImageBitmap? = remember(review.photoBase64) {
                    review.photoBase64?.let { b64 ->
                        try {
                            val bytes = Base64.decode(b64, Base64.DEFAULT)
                            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                        } catch (_: Exception) {
                            null
                        }
                    }
                }

                Surface(
                    color = CardSurface,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .width(295.dp)
                        .graphicsLayer {
                            rotationY = scrollFactor * 14f
                            rotationX = abs(scrollFactor) * -4f
                            val scale = (1f - abs(scrollFactor) * 0.08f).coerceIn(0.88f, 1f)
                            scaleX = scale
                            scaleY = scale
                            translationY = abs(scrollFactor) * 14f
                            alpha = (1f - abs(scrollFactor) * 0.25f).coerceIn(0.65f, 1f)
                            cameraDistance = 12f * density
                        }
                        .border(1.dp, GoldBorder, RoundedCornerShape(24.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (decodedPhoto != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(130.dp)
                                    .clip(RoundedCornerShape(16.dp))
                            ) {
                                Image(
                                    bitmap = decodedPhoto,
                                    contentDescription = "صورة منتج العميل ${review.productName}",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Surface(
                                    color = Color(0xCC090909),
                                    shape = CircleShape,
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(Icons.Default.CameraAlt, contentDescription = null, tint = LuxuryGold, modifier = Modifier.size(12.dp))
                                        Text("صورة العميل", style = MaterialTheme.typography.labelSmall, color = ChampagneGold)
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.Verified, contentDescription = null, tint = LuxuryGold, modifier = Modifier.size(15.dp))
                                Text(review.customerName, style = MaterialTheme.typography.titleMedium, color = PearlIvory)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                Icon(Icons.Default.LocationOn, contentDescription = null, tint = LuxuryGold, modifier = Modifier.size(13.dp))
                                Text(review.governorate, style = MaterialTheme.typography.labelSmall, color = MetallicSilver)
                            }
                        }

                        Text(
                            text = review.productName,
                            style = MaterialTheme.typography.labelSmall,
                            color = LuxuryGold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = "“${review.comment}”",
                            style = MaterialTheme.typography.bodyMedium,
                            color = PearlIvory,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(0.5.dp, Color(0x1FFFFFFF), RoundedCornerShape(12.dp))
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text("المنتج:", style = MaterialTheme.typography.labelSmall, color = MutedSlate)
                                repeat(review.productRating.toInt().coerceIn(1, 5)) {
                                    Icon(Icons.Default.Star, contentDescription = null, tint = LuxuryGold, modifier = Modifier.size(13.dp))
                                }
                            }
                            Text(
                                text = "المتجر: ${review.storeRating}/5 ★",
                                style = MaterialTheme.typography.labelSmall,
                                color = ChampagneGold,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
