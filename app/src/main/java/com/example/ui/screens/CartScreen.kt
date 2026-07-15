package com.example.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.PosViewModel
import com.example.data.Product
import coil.compose.AsyncImage
import com.example.data.Transaction
import com.example.data.TransactionDetail
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
    viewModel: PosViewModel,
    onBackToDashboard: () -> Unit,
    onNavigateToProducts: () -> Unit
) {
    val cartItems by viewModel.cartItems.collectAsState()
    val products by viewModel.products.collectAsState()
    val notes by viewModel.cartNotes.collectAsState()
    val discountPercent by viewModel.discountPercent.collectAsState()
    val taxPercent by viewModel.taxPercent.collectAsState()

    // Screen States
    var showPaymentDialog by remember { mutableStateOf(false) }
    var currentReceiptTransaction by remember { mutableStateOf<Transaction?>(null) }
    var currentReceiptDetails by remember { mutableStateOf<List<TransactionDetail>>(emptyList()) }
    var showScannerDialog by remember { mutableStateOf(false) }
    var mobileSelectedTab by remember { mutableStateOf(0) }

    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Keranjang / Kasir", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackToDashboard) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Dashboard")
                    }
                },
                actions = {
                    IconButton(onClick = { showScannerDialog = true }) {
                        Icon(Icons.Filled.QrCodeScanner, contentDescription = "Scan Barcode")
                    }
                    IconButton(onClick = { viewModel.clearCart() }) {
                        Icon(Icons.Filled.DeleteSweep, contentDescription = "Bersihkan Keranjang", tint = MaterialTheme.colorScheme.error)
                    }
                }
            )
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            val isTablet = maxWidth >= 600.dp
            
            if (isTablet) {
                Row(modifier = Modifier.fillMaxSize()) {
                    // LEFT COLUMN (Catalog selection)
                    Column(
                        modifier = Modifier
                            .weight(1.1f)
                            .fillMaxHeight()
                            .border(1.dp, MaterialTheme.colorScheme.surfaceVariant)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "Pilih Produk",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
                        )

                        if (products.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                TextButton(onClick = onNavigateToProducts) {
                                    Text("Tambah produk di Inventori")
                                }
                            }
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(products) { prod ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.surface)
                                            .clickable { viewModel.addToCart(prod) }
                                            .padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Product Photo Thumbnail
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(
                                                    when (prod.kategori) {
                                                        "Makanan" -> Color(0xFFFFECEF)
                                                        "Minuman" -> Color(0xFFE3F2FD)
                                                        "Snack" -> Color(0xFFFFF8E1)
                                                        "Pakaian" -> Color(0xFFEDE7F6)
                                                        else -> Color(0xFFE8F5E9)
                                                    }
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (!prod.foto.isNullOrBlank()) {
                                                AsyncImage(
                                                    model = prod.foto,
                                                    contentDescription = prod.nama,
                                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = when (prod.kategori) {
                                                        "Makanan" -> Icons.Filled.Restaurant
                                                        "Minuman" -> Icons.Filled.LocalCafe
                                                        "Snack" -> Icons.Filled.Cookie
                                                        "Pakaian" -> Icons.Filled.Checkroom
                                                        else -> Icons.Filled.Inventory2
                                                    },
                                                    contentDescription = null,
                                                    tint = when (prod.kategori) {
                                                        "Makanan" -> Color(0xFFD81B60)
                                                        "Minuman" -> Color(0xFF1E88E5)
                                                        "Snack" -> Color(0xFFFFB300)
                                                        "Pakaian" -> Color(0xFF5E35B1)
                                                        else -> Color(0xFF43A047)
                                                    },
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(10.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = prod.nama,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = formatRupiah(prod.harga) + " • Stok: ${prod.stok}",
                                                fontSize = 11.sp,
                                                color = if (prod.stok <= 10) MaterialTheme.colorScheme.error else Color.Gray
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(6.dp))

                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Add,
                                                contentDescription = "Add",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // RIGHT COLUMN (Checkout Cart)
                    Column(
                        modifier = Modifier
                            .weight(1.2f)
                            .fillMaxHeight()
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "Keranjang Belanja (${cartItems.values.sum()} item)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        if (cartItems.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Filled.AddShoppingCart,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                        modifier = Modifier.size(56.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Keranjang kosong",
                                        fontSize = 13.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                items(cartItems.entries.toList()) { (product, qty) ->
                                    CartItemRow(
                                        product = product,
                                        qty = qty,
                                        onAdd = { viewModel.addToCart(product) },
                                        onRemove = { viewModel.removeFromCart(product) },
                                        onDelete = { viewModel.deleteFromCart(product) }
                                    )
                                }
                            }
                        }

                        Divider(modifier = Modifier.padding(vertical = 8.dp))

                        // Notes, Discount, and Tax fields
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = notes,
                                onValueChange = { viewModel.setCartNotes(it) },
                                label = { Text("Catatan", fontSize = 11.sp) },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )

                            OutlinedTextField(
                                value = if (discountPercent > 0) discountPercent.toString() else "",
                                onValueChange = { viewModel.setDiscount(it.toDoubleOrNull() ?: 0.0) },
                                label = { Text("Diskon %", fontSize = 11.sp) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(0.7f).testTag("discount_input")
                            )

                            OutlinedTextField(
                                value = taxPercent.toString(),
                                onValueChange = { viewModel.setTax(it.toDoubleOrNull() ?: 0.0) },
                                label = { Text("Pajak %", fontSize = 11.sp) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(0.7f)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Summary calculations display
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Subtotal", fontSize = 12.sp, color = Color.Gray)
                                    Text(formatRupiah(viewModel.cartSubtotal), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                if (viewModel.cartDiscountAmount > 0) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Diskon", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                                        Text("- " + formatRupiah(viewModel.cartDiscountAmount), fontSize = 12.sp, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                                    }
                                }
                                if (viewModel.cartTaxAmount > 0) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Pajak (PPN ${taxPercent}%)", fontSize = 12.sp, color = Color.Gray)
                                        Text(formatRupiah(viewModel.cartTaxAmount), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Divider(modifier = Modifier.padding(vertical = 4.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Grand Total", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    Text(formatRupiah(viewModel.cartGrandTotal), fontSize = 15.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                if (cartItems.isNotEmpty()) {
                                    viewModel.setPaymentMethod("Tunai") // default
                                    showPaymentDialog = true
                                } else {
                                    Toast.makeText(context, "Keranjang masih kosong!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("checkout_button"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Bayar & Proses", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                // MOBILE SINGLE-COLUMN ADAPTIVE LAYOUT
                Column(modifier = Modifier.fillMaxSize()) {
                    // Premium Material 3 Tabs
                    TabRow(
                        selectedTabIndex = mobileSelectedTab,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary
                    ) {
                        Tab(
                            selected = mobileSelectedTab == 0,
                            onClick = { mobileSelectedTab = 0 },
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Store,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text("Pilih Produk", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        )
                        Tab(
                            selected = mobileSelectedTab == 1,
                            onClick = { mobileSelectedTab = 1 },
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    BadgedBox(
                                        badge = {
                                            if (cartItems.isNotEmpty()) {
                                                Badge { Text("${cartItems.values.sum()}") }
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.ShoppingCart,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Text("Keranjang", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        if (mobileSelectedTab == 0) {
                            // Catalog List for Mobile
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp)
                            ) {
                                if (products.isEmpty()) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        TextButton(onClick = onNavigateToProducts) {
                                            Text("Tambah produk di Inventori")
                                        }
                                    }
                                } else {
                                    LazyColumn(
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        items(products) { prod ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(MaterialTheme.colorScheme.surface)
                                                    .clickable { viewModel.addToCart(prod) }
                                                    .padding(10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                // Product Photo Thumbnail
                                                Box(
                                                    modifier = Modifier
                                                        .size(46.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(
                                                            when (prod.kategori) {
                                                                "Makanan" -> Color(0xFFFFECEF)
                                                                "Minuman" -> Color(0xFFE3F2FD)
                                                                "Snack" -> Color(0xFFFFF8E1)
                                                                "Pakaian" -> Color(0xFFEDE7F6)
                                                                else -> Color(0xFFE8F5E9)
                                                            }
                                                        ),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    if (!prod.foto.isNullOrBlank()) {
                                                        AsyncImage(
                                                            model = prod.foto,
                                                            contentDescription = prod.nama,
                                                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                                            modifier = Modifier.fillMaxSize()
                                                        )
                                                    } else {
                                                        Icon(
                                                            imageVector = when (prod.kategori) {
                                                                "Makanan" -> Icons.Filled.Restaurant
                                                                "Minuman" -> Icons.Filled.LocalCafe
                                                                "Snack" -> Icons.Filled.Cookie
                                                                "Pakaian" -> Icons.Filled.Checkroom
                                                                else -> Icons.Filled.Inventory2
                                                            },
                                                            contentDescription = null,
                                                            tint = when (prod.kategori) {
                                                                "Makanan" -> Color(0xFFD81B60)
                                                                "Minuman" -> Color(0xFF1E88E5)
                                                                "Snack" -> Color(0xFFFFB300)
                                                                "Pakaian" -> Color(0xFF5E35B1)
                                                                else -> Color(0xFF43A047)
                                                            },
                                                            modifier = Modifier.size(22.dp)
                                                        )
                                                    }
                                                }

                                                Spacer(modifier = Modifier.width(12.dp))

                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = prod.nama,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 14.sp,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = formatRupiah(prod.harga) + " • Stok: ${prod.stok}",
                                                        fontSize = 12.sp,
                                                        color = if (prod.stok <= 10) MaterialTheme.colorScheme.error else Color.Gray
                                                    )
                                                }

                                                Spacer(modifier = Modifier.width(8.dp))

                                                Box(
                                                    modifier = Modifier
                                                        .size(32.dp)
                                                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.Add,
                                                        contentDescription = "Add",
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        }
                                        
                                        // Extra padding to avoid floating card overlap
                                        if (cartItems.isNotEmpty()) {
                                            item {
                                                Spacer(modifier = Modifier.height(80.dp))
                                            }
                                        }
                                    }
                                }
                            }

                            // Gorgeous floating card for quick checkout
                            if (cartItems.isNotEmpty()) {
                                Card(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(16.dp)
                                        .fillMaxWidth()
                                        .clickable { mobileSelectedTab = 1 },
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(
                                                text = "${cartItems.values.sum()} Item Terpilih",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                            )
                                            Text(
                                                text = formatRupiah(viewModel.cartGrandTotal),
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = "Lihat Keranjang",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Icon(
                                                imageVector = Icons.Filled.ArrowForward,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            // Checkout Cart Tab for Mobile (Full spacious single column)
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp)
                            ) {
                                if (cartItems.isEmpty()) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(
                                                imageVector = Icons.Filled.AddShoppingCart,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                                modifier = Modifier.size(64.dp)
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = "Keranjang belanja kosong",
                                                fontSize = 14.sp,
                                                color = Color.Gray,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Button(onClick = { mobileSelectedTab = 0 }) {
                                                Text("Mulai Belanja 🛍️", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                } else {
                                    Column(modifier = Modifier.fillMaxSize()) {
                                        // Cart Items List (taking max space)
                                        LazyColumn(
                                            verticalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            items(cartItems.entries.toList()) { (product, qty) ->
                                                CartItemRow(
                                                    product = product,
                                                    qty = qty,
                                                    onAdd = { viewModel.addToCart(product) },
                                                    onRemove = { viewModel.removeFromCart(product) },
                                                    onDelete = { viewModel.deleteFromCart(product) }
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))
                                        Divider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 1.dp)
                                        Spacer(modifier = Modifier.height(8.dp))

                                        // Notes, Discount, and Tax fields
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            OutlinedTextField(
                                                value = notes,
                                                onValueChange = { viewModel.setCartNotes(it) },
                                                label = { Text("Catatan", fontSize = 11.sp) },
                                                singleLine = true,
                                                modifier = Modifier.weight(1f)
                                            )

                                            OutlinedTextField(
                                                value = if (discountPercent > 0) discountPercent.toString() else "",
                                                onValueChange = { viewModel.setDiscount(it.toDoubleOrNull() ?: 0.0) },
                                                label = { Text("Diskon %", fontSize = 11.sp) },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                singleLine = true,
                                                modifier = Modifier.weight(0.7f).testTag("discount_input")
                                            )

                                            OutlinedTextField(
                                                value = taxPercent.toString(),
                                                onValueChange = { viewModel.setTax(it.toDoubleOrNull() ?: 0.0) },
                                                label = { Text("Pajak %", fontSize = 11.sp) },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                singleLine = true,
                                                modifier = Modifier.weight(0.7f)
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        // Summary calculations display
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                    Text("Subtotal", fontSize = 12.sp, color = Color.Gray)
                                                    Text(formatRupiah(viewModel.cartSubtotal), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                }
                                                if (viewModel.cartDiscountAmount > 0) {
                                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                        Text("Diskon", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                                                        Text("- " + formatRupiah(viewModel.cartDiscountAmount), fontSize = 12.sp, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                                if (viewModel.cartTaxAmount > 0) {
                                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                        Text("Pajak (PPN ${taxPercent}%)", fontSize = 12.sp, color = Color.Gray)
                                                        Text(formatRupiah(viewModel.cartTaxAmount), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                                Divider(modifier = Modifier.padding(vertical = 4.dp))
                                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                    Text("Grand Total", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                                    Text(formatRupiah(viewModel.cartGrandTotal), fontSize = 15.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold)
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        Button(
                                            onClick = {
                                                if (cartItems.isNotEmpty()) {
                                                    viewModel.setPaymentMethod("Tunai") // default
                                                    showPaymentDialog = true
                                                } else {
                                                    Toast.makeText(context, "Keranjang masih kosong!", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(48.dp)
                                                .testTag("checkout_button"),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Text("Bayar & Proses", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Payment Processing Dialog
    if (showPaymentDialog) {
        PaymentSelectionDialog(
            viewModel = viewModel,
            onDismiss = { showPaymentDialog = false },
            onCheckoutSuccess = { tx ->
                currentReceiptTransaction = tx
                // Capture details
                viewModel.loadTransactionDetails(tx.id) { details ->
                    currentReceiptDetails = details
                }
                showPaymentDialog = false
            }
        )
    }

    // Receipt Dialog
    currentReceiptTransaction?.let { tx ->
        ReceiptDetailsDialog(
            transaction = tx,
            details = currentReceiptDetails,
            viewModel = viewModel,
            onDismiss = { currentReceiptTransaction = null }
        )
    }

    if (showScannerDialog) {
        BarcodeScannerDialog(
            viewModel = viewModel,
            onDismiss = { showScannerDialog = false }
        )
    }
}

@Composable
fun CartItemRow(
    product: Product,
    qty: Int,
    onAdd: () -> Unit,
    onRemove: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail Image
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(
                    when (product.kategori) {
                        "Makanan" -> Color(0xFFFFECEF)
                        "Minuman" -> Color(0xFFE3F2FD)
                        "Snack" -> Color(0xFFFFF8E1)
                        "Pakaian" -> Color(0xFFEDE7F6)
                        else -> Color(0xFFE8F5E9)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (!product.foto.isNullOrBlank()) {
                AsyncImage(
                    model = product.foto,
                    contentDescription = product.nama,
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = when (product.kategori) {
                        "Makanan" -> Icons.Filled.Restaurant
                        "Minuman" -> Icons.Filled.LocalCafe
                        "Snack" -> Icons.Filled.Cookie
                        "Pakaian" -> Icons.Filled.Checkroom
                        else -> Icons.Filled.Inventory2
                    },
                    contentDescription = null,
                    tint = when (product.kategori) {
                        "Makanan" -> Color(0xFFD81B60)
                        "Minuman" -> Color(0xFF1E88E5)
                        "Snack" -> Color(0xFFFFB300)
                        "Pakaian" -> Color(0xFF5E35B1)
                        else -> Color(0xFF43A047)
                    },
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = product.nama,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = formatRupiah(product.harga * qty),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            IconButton(
                onClick = onRemove,
                modifier = Modifier
                    .size(26.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
            ) {
                Icon(Icons.Filled.Remove, contentDescription = "Kurang", modifier = Modifier.size(14.dp))
            }

            Text(
                text = "$qty",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            IconButton(
                onClick = onAdd,
                modifier = Modifier
                    .size(26.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Tambah", modifier = Modifier.size(14.dp))
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(26.dp)
            ) {
                Icon(Icons.Filled.Delete, contentDescription = "Hapus", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentSelectionDialog(
    viewModel: PosViewModel,
    onDismiss: () -> Unit,
    onCheckoutSuccess: (Transaction) -> Unit
) {
    val grandTotal = viewModel.cartGrandTotal
    val paymentMethod by viewModel.paymentMethod.collectAsState()
    val nominalBayar by viewModel.nominalBayar.collectAsState()
    val changeAmount = viewModel.cartChangeAmount

    val context = LocalContext.current
    var inputAmountStr by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Pilih Metode Pembayaran",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "Tagihan: ${formatRupiah(grandTotal)}",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Divider()

                // Payment Method Chips Row
                val methods = listOf("Tunai", "QRIS", "Transfer Bank", "E-Wallet")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    methods.forEach { method ->
                        val isSelected = paymentMethod == method
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable {
                                    viewModel.setPaymentMethod(method)
                                    errorMessage = null
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = method,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // Conditional inputs
                if (paymentMethod == "Tunai") {
                    Text("Nominal Bayar Tunai (Rp):", fontWeight = FontWeight.Bold, fontSize = 13.sp)

                    // Quick Cash Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val quickAmounts = listOf(grandTotal, 10000.0 * Math.ceil(grandTotal / 10000.0), 50000.0, 100000.0)
                        quickAmounts.distinct().take(4).forEach { amt ->
                            SuggestionChip(
                                onClick = {
                                    inputAmountStr = amt.toInt().toString()
                                    viewModel.setNominalBayar(amt)
                                },
                                label = { Text(formatRupiah(amt)) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = inputAmountStr,
                        onValueChange = {
                            inputAmountStr = it
                            val amt = it.toDoubleOrNull() ?: 0.0
                            viewModel.setNominalBayar(amt)
                        },
                        label = { Text("Uang Diterima") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("payment_amount_input")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Kembalian:", fontSize = 13.sp, color = Color.Gray)
                        Text(
                            text = formatRupiah(changeAmount),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                } else if (paymentMethod == "QRIS") {
                    // Show a stunning dynamic simulation QRIS scanner box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                            .background(Color.White)
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            // Draw QR reticle
                            Canvas(modifier = Modifier.size(110.dp)) {
                                val w = size.width
                                val h = size.height
                                drawRect(Color.Black, size = this.size, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4.dp.toPx()))
                                // Inner QR blocks mockup
                                drawRect(Color.Black, topLeft = Offset(12.dp.toPx(), 12.dp.toPx()), size = androidx.compose.ui.geometry.Size(30.dp.toPx(), 30.dp.toPx()))
                                drawRect(Color.Black, topLeft = Offset(w - 42.dp.toPx(), 12.dp.toPx()), size = androidx.compose.ui.geometry.Size(30.dp.toPx(), 30.dp.toPx()))
                                drawRect(Color.Black, topLeft = Offset(12.dp.toPx(), h - 42.dp.toPx()), size = androidx.compose.ui.geometry.Size(30.dp.toPx(), 30.dp.toPx()))
                                // Middle blocks
                                drawRect(Color.Black, topLeft = Offset(w / 2f - 10.dp.toPx(), h / 2f - 10.dp.toPx()), size = androidx.compose.ui.geometry.Size(20.dp.toPx(), 20.dp.toPx()))
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("QRIS DYNAMIC SIGMA", fontSize = 12.sp, color = Color.Black, fontWeight = FontWeight.ExtraBold)
                            Text("Tunjukkan QR ini ke Pelanggan", fontSize = 10.sp, color = Color.Gray)
                        }
                    }
                } else {
                    // Bank or E-Wallet notice
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Silakan gesek kartu mesin EDC atau konfirmasi pembayaran digital Anda sebelum memproses transaksi ini.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                errorMessage?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Batal")
                    }

                    Button(
                        onClick = {
                            viewModel.submitCheckout(
                                onSuccess = { tx ->
                                    Toast.makeText(context, "Transaksi berhasil disimpan!", Toast.LENGTH_SHORT).show()
                                    onCheckoutSuccess(tx)
                                },
                                onError = { err ->
                                    errorMessage = err
                                }
                            )
                        },
                        modifier = Modifier.weight(1.2f).testTag("confirm_payment_button")
                    ) {
                        Text("Konfirmasi")
                    }
                }
            }
        }
    }
}

@Composable
fun ReceiptDetailsDialog(
    transaction: Transaction,
    details: List<TransactionDetail>,
    viewModel: PosViewModel,
    onDismiss: () -> Unit
) {
    val shopName by viewModel.shopName.collectAsState()
    val shopAddress by viewModel.shopAddress.collectAsState()
    val shopPhone by viewModel.shopPhone.collectAsState()
    val context = LocalContext.current

    val dateFormatted = remember(transaction.tanggal) {
        SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date(transaction.tanggal))
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Receipt Header
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(48.dp)
                )

                Text(
                    text = "TRANSAKSI BERHASIL",
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.secondary,
                    fontSize = 16.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Printable Area Paper Wrapper
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFAFAFA))
                        .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(8.dp))
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(shopName, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
                    Text(shopAddress, fontSize = 11.sp, color = Color.DarkGray, textAlign = TextAlign.Center)
                    Text("Telp: $shopPhone", fontSize = 11.sp, color = Color.DarkGray)

                    Text("---------------------------------", fontFamily = FontFamily.Monospace, color = Color.Gray)

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("No Tx:", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color.Black)
                        Text("TX-${transaction.id}", fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = Color.Black)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Waktu:", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color.Black)
                        Text(dateFormatted, fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color.Black)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Kasir:", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color.Black)
                        Text(transaction.kasir, fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color.Black)
                    }

                    Text("---------------------------------", fontFamily = FontFamily.Monospace, color = Color.Gray)

                    // Details products
                    details.forEach { det ->
                        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                            Text(det.produk_nama, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("${det.qty} x ${formatRupiah(det.harga)}", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color.DarkGray)
                                Text(formatRupiah(det.harga * det.qty), fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = Color.Black)
                            }
                        }
                    }

                    Text("---------------------------------", fontFamily = FontFamily.Monospace, color = Color.Gray)

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Diskon:", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color.Black)
                        Text("- " + formatRupiah(transaction.diskon), fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color.Black)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Pajak:", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color.Black)
                        Text(formatRupiah(transaction.pajak), fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color.Black)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("TOTAL:", fontSize = 13.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = Color.Black)
                        Text(formatRupiah(transaction.total), fontSize = 14.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.ExtraBold, color = Color.Black)
                    }

                    Text("---------------------------------", fontFamily = FontFamily.Monospace, color = Color.Gray)

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Metode:", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color.Black)
                        Text(transaction.metode_pembayaran, fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color.Black)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Bayar:", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color.Black)
                        Text(formatRupiah(transaction.nominal_bayar), fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color.Black)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Kembali:", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color.Black)
                        val change = (transaction.nominal_bayar - transaction.total).coerceAtLeast(0.0)
                        Text(formatRupiah(change), fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = Color.Black)
                    }

                    transaction.catatan?.let {
                        if (it.isNotBlank()) {
                            Text("- - - - - - - - - - - - - - - -", fontFamily = FontFamily.Monospace, color = Color.Gray)
                            Text("Catatan: $it", fontSize = 11.sp, color = Color.DarkGray, modifier = Modifier.align(Alignment.Start))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Terima Kasih atas Kunjungan Anda!", fontSize = 10.sp, color = Color.Gray)
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Action share buttons
                Text("Cetak & Bagikan:", fontWeight = FontWeight.Bold, fontSize = 12.sp)

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. Sim Bluetooth Printer
                    ShareOptionItem(
                        icon = Icons.Filled.Print,
                        label = "Printer",
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        onClick = {
                            Toast.makeText(context, "Mencari Printer Bluetooth...\nMencetak struk ke printer thermal...", Toast.LENGTH_LONG).show()
                        }
                    )

                    // 2. Share PDF
                    ShareOptionItem(
                        icon = Icons.Filled.PictureAsPdf,
                        label = "PDF",
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        onClick = {
                            try {
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, "STRUK BELANJA SIGMA POS\nNo: TX-${transaction.id}\nTotal: ${formatRupiah(transaction.total)}\nTerima kasih!")
                                    type = "text/plain"
                                }
                                val shareIntent = Intent.createChooser(sendIntent, "Bagikan PDF Struk")
                                context.startActivity(shareIntent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Gagal membagikan struk", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )

                    // 3. Share WhatsApp
                    ShareOptionItem(
                        icon = Icons.Filled.Share,
                        label = "WhatsApp",
                        containerColor = Color(0xFFE8F5E9),
                        contentColor = Color(0xFF2E7D32),
                        onClick = {
                            try {
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, "Halo, berikut struk transaksi Anda di $shopName:\n\n*TX-${transaction.id}*\nTotal: *${formatRupiah(transaction.total)}*\n\nTerima Kasih!")
                                    type = "text/plain"
                                    `package` = "com.whatsapp"
                                }
                                context.startActivity(sendIntent)
                            } catch (e: Exception) {
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, "Struk Transaksi:\nTX-${transaction.id}\nTotal: ${formatRupiah(transaction.total)}")
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Bagikan via WhatsApp"))
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().testTag("close_receipt_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text("Tutup")
                }
            }
        }
    }
}

@Composable
fun ShareOptionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(containerColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
