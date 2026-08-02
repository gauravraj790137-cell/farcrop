package com.example.farcrop.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.farcrop.model.BuyLink
import com.example.farcrop.model.V2StandardResponse
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    response: V2StandardResponse,
    cycleId: String,
    onBack: () -> Unit
) {
    val prediction = response.data?.prediction

    if (prediction == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Invalid prediction data.")
        }
        return
    }

    val confidencePct = (prediction.confidence * 100).roundToInt().coerceIn(0, 100)
    val isHealthy     = prediction.disease.contains("healthy", ignoreCase = true)

    val severity = when {
        isHealthy                                                              -> "None"
        prediction.disease.contains("late_blight",  ignoreCase = true)       -> "High"
        prediction.disease.contains("early_blight", ignoreCase = true)       -> "Medium"
        prediction.disease.contains("spot",         ignoreCase = true)       -> "Medium"
        else                                                                   -> "Low"
    }

    val severityColor = when (severity) {
        "High"   -> Color(0xFFC62828)
        "Medium" -> Color(0xFFEF6C00)
        "Low"    -> Color(0xFFFBC02D)
        else     -> MaterialTheme.colorScheme.secondary
    }

    val chipColor = if (isHealthy) MaterialTheme.colorScheme.secondary else Color(0xFFC62828)
    val chipText  = if (isHealthy) "Healthy" else "Disease Detected"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scan Result", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor            = MaterialTheme.colorScheme.primary,
                    titleContentColor         = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ── Diagnosis Card ────────────────────────────────────────────
            Card(
                modifier  = Modifier.fillMaxWidth(),
                shape     = RoundedCornerShape(16.dp),
                colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier            = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment    = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier             = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text     = prediction.disease
                                .replace("___", " — ")
                                .replace("_", " "),
                            style        = MaterialTheme.typography.titleLarge,
                            fontWeight   = FontWeight.Bold,
                            modifier     = Modifier.weight(1f)
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(chipColor)
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(chipText, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    // Confidence bar
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Confidence", style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$confidencePct%", style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        LinearProgressIndicator(
                            progress     = { confidencePct / 100f },
                            modifier     = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                            color        = MaterialTheme.colorScheme.primary,
                            trackColor   = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }

                    // Severity row
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Filled.Warning, contentDescription = null,
                                tint = severityColor, modifier = Modifier.size(16.dp))
                            Text("Severity", style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(severity, fontWeight = FontWeight.Bold, color = severityColor, fontSize = 14.sp)
                    }

                    // Cause
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Filled.BugReport, contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        Text("Cause: ${prediction.cause}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // ── Description ───────────────────────────────────────────────
            ResultSection(icon = Icons.Filled.Info, title = "Description") {
                Text(prediction.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface)
            }

            // ── AI Explanation ────────────────────────────────────────────
            if (prediction.explanation.isNotBlank()) {
                ResultSection(icon = Icons.Filled.AutoAwesome, title = "AI Explanation") {
                    Text(prediction.explanation,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface)
                }
            }

            // ── Treatment ─────────────────────────────────────────────────
            if (prediction.treatment.isNotEmpty()) {
                ResultSection(icon = Icons.Filled.Healing, title = "Treatment Recommended") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        prediction.treatment.forEach { step ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.Top) {
                                Text("•", color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text(step, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }

            // ── Recommended Products Carousel ─────────────────────────────
            // Show rich product cards when buy_links available, text chips otherwise.
            if (prediction.buyLinks.isNotEmpty()) {
                ProductCarouselSection(products = prediction.buyLinks)
            } else if (prediction.products.isNotEmpty()) {
                ResultSection(icon = Icons.Filled.Agriculture, title = "Recommended Products") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        prediction.products.forEach { product ->
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape    = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text     = product,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                    style    = MaterialTheme.typography.bodyMedium,
                                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick  = onBack,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape    = RoundedCornerShape(12.dp)
            ) {
                Text("Return to Crop Cycle", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ── Product Carousel ──────────────────────────────────────────────────────────

@Composable
private fun ProductCarouselSection(products: List<BuyLink>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Section header
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier              = Modifier.padding(horizontal = 0.dp)
        ) {
            Icon(
                imageVector  = Icons.Filled.ShoppingCart,
                contentDescription = null,
                tint         = MaterialTheme.colorScheme.primary,
                modifier     = Modifier.size(20.dp)
            )
            Text(
                text       = "Buy Products",
                style      = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }

        // Horizontal scrolling carousel
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding        = PaddingValues(horizontal = 2.dp, vertical = 4.dp)
        ) {
            items(products) { product ->
                ProductCard(product = product)
            }
        }
    }
}

@Composable
private fun ProductCard(product: BuyLink) {
    val context = LocalContext.current
    val hasUrl  = product.url.isNotBlank()

    Card(
        modifier  = Modifier
            .width(180.dp)
            .wrapContentHeight(),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier            = Modifier.padding(bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // Thumbnail
            if (!product.thumbnail.isNullOrBlank()) {
                AsyncImage(
                    model              = product.thumbnail,
                    contentDescription = product.title,
                    contentScale       = ContentScale.Crop,
                    modifier           = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                )
            } else {
                Box(
                    modifier           = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
                    contentAlignment   = Alignment.Center
                ) {
                    Icon(
                        imageVector        = Icons.Filled.Agriculture,
                        contentDescription = null,
                        tint               = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                        modifier           = Modifier.size(48.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Column(
                modifier            = Modifier.padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Title
                Text(
                    text       = product.title.ifBlank { "Product" },
                    style      = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines   = 2,
                    overflow   = TextOverflow.Ellipsis,
                    color      = MaterialTheme.colorScheme.onSurface
                )

                // Brand
                if (!product.brand.isNullOrBlank()) {
                    Text(
                        text     = product.brand,
                        style    = MaterialTheme.typography.bodySmall,
                        color    = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Price + Rating row
                if (!product.price.isNullOrBlank() || !product.rating.isNullOrBlank()) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically,
                        modifier              = Modifier.fillMaxWidth()
                    ) {
                        if (!product.price.isNullOrBlank()) {
                            Text(
                                text       = product.price,
                                style      = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color      = MaterialTheme.colorScheme.primary
                            )
                        }
                        if (!product.rating.isNullOrBlank()) {
                            Row(
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(
                                    imageVector        = Icons.Filled.Star,
                                    contentDescription = null,
                                    tint               = Color(0xFFFBC02D),
                                    modifier           = Modifier.size(12.dp)
                                )
                                Text(
                                    text  = product.rating,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Buy Now button
                Button(
                    onClick  = {
                        if (hasUrl) {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(product.url))
                            context.startActivity(intent)
                        }
                    },
                    enabled  = hasUrl,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp),
                    shape    = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text     = "Buy Now",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

// ── Section wrapper ───────────────────────────────────────────────────────────

@Composable
private fun ResultSection(
    icon: ImageVector,
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier            = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(imageVector = icon, contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            content()
        }
    }
}
