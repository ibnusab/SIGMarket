package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.example.data.Category
import com.example.data.PosViewModel
import com.example.data.Product
import coil.compose.AsyncImage

data class PremiumPhotoTemplate(
    val nama: String,
    val kategori: String,
    val imageUrl: String,
    val defaultHarga: Double
)

val premiumPhotoTemplates = listOf(
    PremiumPhotoTemplate(
        nama = "Indomie Goreng (Mie Instan)",
        kategori = "Makanan",
        imageUrl = "https://images.unsplash.com/photo-1569718212165-3a8278d5f624?w=500&auto=format&fit=crop&q=60",
        defaultHarga = 3500.0
    ),
    PremiumPhotoTemplate(
        nama = "Nasi Goreng Spesial",
        kategori = "Makanan",
        imageUrl = "https://images.unsplash.com/photo-1603133872878-685519c7f1c4?w=500&auto=format&fit=crop&q=60",
        defaultHarga = 18000.0
    ),
    PremiumPhotoTemplate(
        nama = "Ayam Geprek Sambal Korek",
        kategori = "Makanan",
        imageUrl = "https://images.unsplash.com/photo-1562967914-608f82629710?w=500&auto=format&fit=crop&q=60",
        defaultHarga = 15000.0
    ),
    PremiumPhotoTemplate(
        nama = "Roti Bakar Cokelat Keju",
        kategori = "Makanan",
        imageUrl = "https://images.unsplash.com/photo-1584776296944-ab6fb57b0bdd?w=500&auto=format&fit=crop&q=60",
        defaultHarga = 12000.0
    ),
    PremiumPhotoTemplate(
        nama = "AQUA Air Mineral 600ml",
        kategori = "Minuman",
        imageUrl = "https://images.unsplash.com/photo-1608885898957-a599fb1b4600?w=500&auto=format&fit=crop&q=60",
        defaultHarga = 4000.0
    ),
    PremiumPhotoTemplate(
        nama = "Es Kopi Susu Gula Aren",
        kategori = "Minuman",
        imageUrl = "https://images.unsplash.com/photo-1513558161293-cdaf765ed2fd?w=500&auto=format&fit=crop&q=60",
        defaultHarga = 15000.0
    ),
    PremiumPhotoTemplate(
        nama = "Es Teh Manis Segar",
        kategori = "Minuman",
        imageUrl = "https://images.unsplash.com/photo-1556679343-c7306c1976bc?w=500&auto=format&fit=crop&q=60",
        defaultHarga = 5000.0
    ),
    PremiumPhotoTemplate(
        nama = "Chitato Sapi Panggang",
        kategori = "Snack",
        imageUrl = "https://images.unsplash.com/photo-1599599810769-bcde5a160d32?w=500&auto=format&fit=crop&q=60",
        defaultHarga = 11500.0
    ),
    PremiumPhotoTemplate(
        nama = "Kripik Kentang Crispy",
        kategori = "Snack",
        imageUrl = "https://images.unsplash.com/photo-1566478989037-eec170784d4b?w=500&auto=format&fit=crop&q=60",
        defaultHarga = 8000.0
    ),
    PremiumPhotoTemplate(
        nama = "Biskuit Cokelat Sandwich",
        kategori = "Snack",
        imageUrl = "https://images.unsplash.com/photo-1558961309-dbdf71791f54?w=500&auto=format&fit=crop&q=60",
        defaultHarga = 7500.0
    ),
    PremiumPhotoTemplate(
        nama = "Kaos Polos Cotton Combed",
        kategori = "Pakaian",
        imageUrl = "https://images.unsplash.com/photo-1521572267360-ee0c2909d518?w=500&auto=format&fit=crop&q=60",
        defaultHarga = 65000.0
    ),
    PremiumPhotoTemplate(
        nama = "Kemeja Flanel Premium",
        kategori = "Pakaian",
        imageUrl = "https://images.unsplash.com/photo-1598033129183-c4f50c736f10?w=500&auto=format&fit=crop&q=60",
        defaultHarga = 120000.0
    ),
    PremiumPhotoTemplate(
        nama = "Topi Baseball Casual",
        kategori = "Pakaian",
        imageUrl = "https://images.unsplash.com/photo-1588850561407-ed78c282e89b?w=500&auto=format&fit=crop&q=60",
        defaultHarga = 35000.0
    ),
    PremiumPhotoTemplate(
        nama = "Potong Rambut (Barber)",
        kategori = "Jasa",
        imageUrl = "https://images.unsplash.com/photo-1503951914875-452162b0f3f1?w=500&auto=format&fit=crop&q=60",
        defaultHarga = 25000.0
    ),
    PremiumPhotoTemplate(
        nama = "Jasa Cuci Sepatu (Clean)",
        kategori = "Jasa",
        imageUrl = "https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=500&auto=format&fit=crop&q=60",
        defaultHarga = 30000.0
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductManagementScreen(
    viewModel: PosViewModel,
    onBack: () -> Unit
) {
    val products by viewModel.products.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val isOwner = currentUser?.role == "Owner"

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf("Semua") }

    // Dialog flags
    var showProductDialog by remember { mutableStateOf(false) }
    var showCategoryDialog by remember { mutableStateOf(false) }
    var editingProduct by remember { mutableStateOf<Product?>(null) }
    var showScannerDialog by remember { mutableStateOf(false) }

    val filteredProducts = remember(products, searchQuery, selectedCategoryFilter) {
        products.filter { prod ->
            val matchQuery = prod.nama.contains(searchQuery, ignoreCase = true) ||
                    prod.barcode.contains(searchQuery)
            val matchCategory = selectedCategoryFilter == "Semua" || prod.kategori == selectedCategoryFilter
            matchQuery && matchCategory
        }
    }

    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Daftar Produk", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    if (isOwner) {
                        IconButton(onClick = { showCategoryDialog = true }) {
                            Icon(Icons.Filled.Category, contentDescription = "Kelola Kategori")
                        }
                    }
                    IconButton(onClick = { showScannerDialog = true }) {
                        Icon(Icons.Filled.QrCodeScanner, contentDescription = "Scan Barcode")
                    }
                }
            )
        },
        floatingActionButton = {
            if (isOwner) {
                FloatingActionButton(
                    onClick = {
                        editingProduct = null
                        showProductDialog = true
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("add_product_fab")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Tambah Produk")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Cari Produk (Nama / Barcode)") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Filled.Clear, contentDescription = "Bersihkan")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("product_search_input")
            )

            // Category filter chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                item {
                    FilterChip(
                        selected = selectedCategoryFilter == "Semua",
                        onClick = { selectedCategoryFilter = "Semua" },
                        label = { Text("Semua") }
                    )
                }
                items(categories) { cat ->
                    FilterChip(
                        selected = selectedCategoryFilter == cat.nama,
                        onClick = { selectedCategoryFilter = cat.nama },
                        label = { Text(cat.nama) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Products Grid
            if (filteredProducts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Filled.Inventory,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tidak ada produk ditemukan",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            fontSize = 15.sp
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 160.dp),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredProducts) { product ->
                        ProductGridItem(
                            product = product,
                            isOwner = isOwner,
                            onEdit = {
                                editingProduct = product
                                showProductDialog = true
                            },
                            onDelete = {
                                viewModel.deleteProduct(product)
                                Toast.makeText(context, "${product.nama} dihapus", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        }
    }

    // Dialogs Setup
    if (showProductDialog) {
        AddEditProductDialog(
            product = editingProduct,
            categories = categories,
            onDismiss = { showProductDialog = false },
            onSave = { prod ->
                viewModel.saveProduct(prod)
                showProductDialog = false
            }
        )
    }

    if (showCategoryDialog) {
        ManageCategoriesDialog(
            categories = categories,
            onDismiss = { showCategoryDialog = false },
            onAddCategory = { catName -> viewModel.saveCategory(catName) },
            onDeleteCategory = { cat -> viewModel.deleteCategory(cat) }
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
fun ProductGridItem(
    product: Product,
    isOwner: Boolean = true,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("product_item_${product.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // Beautiful Photo Area with high-contrast fallback
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
            ) {
                if (!product.foto.isNullOrBlank()) {
                    AsyncImage(
                        model = product.foto,
                        contentDescription = product.nama,
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
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
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                // Sleek barcode badge overlay
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.QrCode,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(10.dp)
                        )
                        Text(
                            text = product.barcode,
                            fontSize = 9.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = product.nama,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = product.kategori,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = formatRupiah(product.harga),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Stok: ${product.stok}",
                        fontSize = 12.sp,
                        color = if (product.stok <= 10) MaterialTheme.colorScheme.error else Color.Gray,
                        fontWeight = FontWeight.Bold
                    )

                    if (isOwner) {
                        Row {
                            IconButton(
                                onClick = onEdit,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Edit,
                                    contentDescription = "Edit",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            IconButton(
                                onClick = onDelete,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription = "Hapus",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Lock,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.size(10.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "Readonly",
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditProductDialog(
    product: Product?,
    categories: List<Category>,
    onDismiss: () -> Unit,
    onSave: (Product) -> Unit
) {
    var nama by remember { mutableStateOf(product?.nama ?: "") }
    var barcode by remember { mutableStateOf(product?.barcode ?: "") }
    var kategori by remember { mutableStateOf(product?.kategori ?: categories.firstOrNull()?.nama ?: "Makanan") }
    var harga by remember { mutableStateOf(product?.harga?.toString() ?: "") }
    var stok by remember { mutableStateOf(product?.stok?.toString() ?: "") }
    var foto by remember { mutableStateOf(product?.foto) }

    var expandedDropdown by remember { mutableStateOf(false) }
    var showTemplatePicker by remember { mutableStateOf(false) }

    if (showTemplatePicker) {
        PremiumPhotoTemplatePickerDialog(
            onDismiss = { showTemplatePicker = false },
            onSelect = { template ->
                foto = template.imageUrl
                if (nama.isBlank()) {
                    nama = template.nama
                }
                kategori = template.kategori
                if (harga.isBlank() || harga == "0" || harga == "0.0") {
                    harga = template.defaultHarga.toInt().toString()
                }
                showTemplatePicker = false
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (product == null) "Tambah Produk Baru" else "Edit Produk") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Foto Produk Section
                Text(
                    text = "Foto Produk",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 4.dp)
                )
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (!foto.isNullOrBlank()) {
                            AsyncImage(
                                model = foto,
                                contentDescription = "Foto Produk",
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp))
                            )
                            IconButton(
                                onClick = { foto = null },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                                    .size(28.dp)
                                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                            ) {
                                Icon(Icons.Filled.Close, contentDescription = "Hapus Foto", tint = Color.White, modifier = Modifier.size(14.dp))
                            }
                        } else {
                            Column(
                                modifier = Modifier.fillMaxSize().padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AddPhotoAlternate,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Belum Ada Foto",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                                Text(
                                    text = "Pilih dari galeri hp atau gunakan template siap pakai",
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                // Photo Picker Row
                val galleryLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.GetContent()
                ) { uri ->
                    if (uri != null) {
                        foto = uri.toString()
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { galleryLauncher.launch("image/*") },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 8.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Dari Galeri 📱", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = { showTemplatePicker = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 8.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Template 🎨", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 4.dp))

                OutlinedTextField(
                    value = nama,
                    onValueChange = { nama = it },
                    label = { Text("Nama Produk") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("product_name_input")
                )

                OutlinedTextField(
                    value = barcode,
                    onValueChange = { barcode = it },
                    label = { Text("Barcode / SKU") },
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = { barcode = (100000000000..999999999999).random().toString() }) {
                            Icon(Icons.Filled.Autorenew, contentDescription = "Acak Barcode")
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("product_barcode_input")
                )

                // Category Dropdown
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = kategori,
                        onValueChange = {},
                        label = { Text("Kategori") },
                        readOnly = true,
                        trailingIcon = {
                            IconButton(onClick = { expandedDropdown = true }) {
                                Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                            }
                        },
                        modifier = Modifier.fillMaxWidth().clickable { expandedDropdown = true }
                    )
                    DropdownMenu(
                        expanded = expandedDropdown,
                        onDismissRequest = { expandedDropdown = false }
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.nama) },
                                onClick = {
                                    kategori = cat.nama
                                    expandedDropdown = false
                                }
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = harga,
                        onValueChange = { harga = it },
                        label = { Text("Harga Jual") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1.2f).testTag("product_price_input")
                    )

                    OutlinedTextField(
                        value = stok,
                        onValueChange = { stok = it },
                        label = { Text("Stok") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(0.8f).testTag("product_stock_input")
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (nama.isNotBlank() && harga.isNotBlank() && stok.isNotBlank()) {
                        onSave(
                            Product(
                                id = product?.id ?: 0,
                                barcode = if (barcode.isBlank()) "NO-BARCODE" else barcode,
                                nama = nama,
                                kategori = kategori,
                                harga = harga.toDoubleOrNull() ?: 0.0,
                                stok = stok.toIntOrNull() ?: 0,
                                foto = foto
                            )
                        )
                    }
                },
                modifier = Modifier.testTag("save_product_button")
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumPhotoTemplatePickerDialog(
    onDismiss: () -> Unit,
    onSelect: (PremiumPhotoTemplate) -> Unit
) {
    var selectedCategoryTab by remember { mutableStateOf("Semua") }
    val categories = listOf("Semua", "Makanan", "Minuman", "Snack", "Pakaian", "Jasa")

    val filteredTemplates = remember(selectedCategoryTab) {
        if (selectedCategoryTab == "Semua") {
            premiumPhotoTemplates
        } else {
            premiumPhotoTemplates.filter { it.kategori == selectedCategoryTab }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Pilih Gambar Premium 🎨",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    text = "Foto produk katalog modern siap pakai",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Normal
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .width(360.dp)
                    .height(380.dp)
            ) {
                // Category Tabs Row
                ScrollableTabRow(
                    selectedTabIndex = categories.indexOf(selectedCategoryTab),
                    edgePadding = 0.dp,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                ) {
                    categories.forEachIndexed { index, cat ->
                        Tab(
                            selected = selectedCategoryTab == cat,
                            onClick = { selectedCategoryTab = cat },
                            text = { Text(cat, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )
                    }
                }

                // Grid of Premium templates
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredTemplates) { template ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
                                .clickable { onSelect(template) },
                            shape = RoundedCornerShape(10.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.fillMaxSize()) {
                                AsyncImage(
                                    model = template.imageUrl,
                                    contentDescription = template.nama,
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(80.dp)
                                )
                                Column(modifier = Modifier.padding(6.dp)) {
                                    Text(
                                        text = template.nama,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = template.kategori,
                                        fontSize = 9.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Tutup")
            }
        }
    )
}

@Composable
fun ManageCategoriesDialog(
    categories: List<Category>,
    onDismiss: () -> Unit,
    onAddCategory: (String) -> Unit,
    onDeleteCategory: (Category) -> Unit
) {
    var newCategoryName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Kelola Kategori") },
        text = {
            Column(modifier = Modifier.width(320.dp)) {
                // Add New row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newCategoryName,
                        onValueChange = { newCategoryName = it },
                        label = { Text("Tambah Kategori") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (newCategoryName.isNotBlank()) {
                                onAddCategory(newCategoryName)
                                newCategoryName = ""
                            }
                        }
                    ) {
                        Icon(Icons.Filled.AddCircle, contentDescription = "Add", tint = MaterialTheme.colorScheme.primary)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("Daftar Kategori:", fontWeight = FontWeight.Bold, fontSize = 13.sp)

                LazyColumn(modifier = Modifier.height(200.dp)) {
                    items(categories) { cat ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(cat.nama, fontSize = 14.sp)
                            IconButton(onClick = { onDeleteCategory(cat) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Hapus", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Tutup")
            }
        }
    )
}

@Composable
fun BarcodeScannerDialog(
    viewModel: PosViewModel,
    onDismiss: () -> Unit
) {
    val products by viewModel.products.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Permission tracking
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasCameraPermission = granted }
    )

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    // Mock Simulation Mode Input
    var simulatedBarcode by remember { mutableStateOf("") }
    var scanSuccessMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.QrCodeScanner, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                Text("Scanner Kamera")
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .width(320.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Interactive Camera Area
                if (hasCameraPermission) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black)
                    ) {
                        // In actual build, we hook CameraX
                        AndroidView(
                            factory = { ctx ->
                                val previewView = PreviewView(ctx).apply {
                                    layoutParams = ViewGroup.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                                }
                                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                                cameraProviderFuture.addListener({
                                    try {
                                        val cameraProvider = cameraProviderFuture.get()
                                        val preview = Preview.Builder().build().also {
                                            it.surfaceProvider = previewView.surfaceProvider
                                        }
                                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                                        
                                        val imageAnalysis = ImageAnalysis.Builder()
                                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                            .build()

                                        var isScanning = true
                                        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(ctx)) { imageProxy ->
                                            @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
                                            val mediaImage = imageProxy.image
                                            if (mediaImage != null && isScanning) {
                                                val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                                                val scanner = BarcodeScanning.getClient()
                                                scanner.process(image)
                                                    .addOnSuccessListener { barcodes ->
                                                        if (isScanning && barcodes.isNotEmpty()) {
                                                            for (barcode in barcodes) {
                                                                val rawValue = barcode.rawValue
                                                                if (!rawValue.isNullOrBlank()) {
                                                                    isScanning = false
                                                                    viewModel.scanBarcode(rawValue) { success, item ->
                                                                        if (success && item != null) {
                                                                            scanSuccessMessage = "Berhasil: ${item.nama} dimasukkan ke keranjang!"
                                                                            Toast.makeText(ctx, "Scan: ${item.nama}", Toast.LENGTH_SHORT).show()
                                                                        } else {
                                                                            scanSuccessMessage = "Scan Barcode: $rawValue\n(Gagal: Stok habis atau tidak terdaftar)"
                                                                        }
                                                                        previewView.postDelayed({
                                                                            isScanning = true
                                                                        }, 2000)
                                                                    }
                                                                    break
                                                                }
                                                            }
                                                        }
                                                    }
                                                    .addOnFailureListener {
                                                        Log.e("MLKit", "Barcode scan failure", it)
                                                    }
                                                    .addOnCompleteListener {
                                                        imageProxy.close()
                                                    }
                                            } else {
                                                imageProxy.close()
                                            }
                                        }

                                        cameraProvider.unbindAll()
                                        cameraProvider.bindToLifecycle(
                                            lifecycleOwner,
                                            cameraSelector,
                                            preview,
                                            imageAnalysis
                                        )
                                    } catch (e: Exception) {
                                        Log.e("CameraX", "Failed to start camera", e)
                                    }
                                }, ContextCompat.getMainExecutor(ctx))
                                previewView
                            },
                            modifier = Modifier.fillMaxSize()
                        )

                        // Reticle Overlay
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .drawBehind {
                                    val canvasSize = this.size
                                    val rectW = canvasSize.width * 0.7f
                                    val rectH = canvasSize.height * 0.4f
                                    val left = (canvasSize.width - rectW) / 2f
                                    val top = (canvasSize.height - rectH) / 2f

                                    // Outer dim shading
                                    drawRect(Color.Black.copy(alpha = 0.4f))

                                    // Clear target box
                                    drawRect(
                                        color = Color.Transparent,
                                        topLeft = Offset(left, top),
                                        size = androidx.compose.ui.geometry.Size(rectW, rectH),
                                        blendMode = androidx.compose.ui.graphics.BlendMode.Clear
                                    )

                                    // Boundary highlight
                                    drawRect(
                                        color = Color.Green,
                                        topLeft = Offset(left, top),
                                        size = androidx.compose.ui.geometry.Size(rectW, rectH),
                                        style = Stroke(width = 4f)
                                    )
                                }
                        )

                        Text(
                            text = "Arahkan kamera ke Barcode",
                            color = Color.White,
                            fontSize = 12.sp,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(8.dp)
                                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.DarkGray),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Memerlukan izin kamera\nuntuk scan barcode.",
                            color = Color.White,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Barcode Simulator (EXCELLENT FOR EMULATORS / MANUAL TESTING)
                Divider()
                Text(
                    text = "Simulasi Scanner (Uji Coba)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                AnimatedVisibility(visible = scanSuccessMessage != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = scanSuccessMessage ?: "",
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(10.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Produk Terdaftar:", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    
                    // Quick simulation buttons for existing products
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(products.take(4)) { prod ->
                            SuggestionChip(
                                onClick = {
                                    simulatedBarcode = prod.barcode
                                    viewModel.scanBarcode(prod.barcode) { success, item ->
                                        if (success && item != null) {
                                            scanSuccessMessage = "Scan Berhasil: ${item.nama} dimasukkan ke keranjang!"
                                            Toast.makeText(context, "${item.nama} ditambahkan ke Keranjang", Toast.LENGTH_SHORT).show()
                                        } else {
                                            scanSuccessMessage = "Gagal: Stok habis atau produk tidak ditemukan!"
                                        }
                                    }
                                },
                                label = { Text(prod.nama) }
                            )
                        }
                    }

                    Text("Produk Toko Lain (Auto-Data / Registrasi Otomatis):", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        SuggestionChip(
                            onClick = {
                                simulatedBarcode = "8993053121407"
                                viewModel.scanBarcode("8993053121407") { success, item ->
                                    if (success && item != null) {
                                        scanSuccessMessage = "✨ Barcode Terdeteksi!\nBerhasil mendaftarkan produk baru:\n${item.nama}\nKategori: ${item.kategori} | Harga: Rp ${item.harga.toInt()}\n(Telah dimasukkan ke keranjang!)"
                                        Toast.makeText(context, "${item.nama} berhasil diregistrasi!", Toast.LENGTH_LONG).show()
                                    } else {
                                        scanSuccessMessage = "Gagal memindai atau mendaftarkan barcode tisu."
                                    }
                                }
                            },
                            label = { Text("🧼 Tisu Paseo (8993053121407)") }
                        )

                        SuggestionChip(
                            onClick = {
                                simulatedBarcode = "8992001101234"
                                viewModel.scanBarcode("8992001101234") { success, item ->
                                    if (success && item != null) {
                                        scanSuccessMessage = "✨ Barcode Terdeteksi!\nBerhasil mendaftarkan produk baru:\n${item.nama}\nKategori: ${item.kategori} | Harga: Rp ${item.harga.toInt()}\n(Telah dimasukkan ke keranjang!)"
                                        Toast.makeText(context, "${item.nama} berhasil diregistrasi!", Toast.LENGTH_LONG).show()
                                    } else {
                                        scanSuccessMessage = "Gagal memindai atau mendaftarkan barcode teh."
                                    }
                                }
                            },
                            label = { Text("🍵 Teh Pucuk (8992001101234)") }
                        )
                    }

                    OutlinedTextField(
                        value = simulatedBarcode,
                        onValueChange = { simulatedBarcode = it },
                        label = { Text("Masukkan Barcode Manual / Scan") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    if (simulatedBarcode.isNotBlank()) {
                                        viewModel.scanBarcode(simulatedBarcode) { success, item ->
                                            if (success && item != null) {
                                                scanSuccessMessage = "✨ Sukses!\nProduk: ${item.nama}\nKategori: ${item.kategori}\nHarga: Rp ${item.harga.toInt()}\n(Telah didata & masuk keranjang)"
                                                Toast.makeText(context, "${item.nama} berhasil dimasukkan", Toast.LENGTH_SHORT).show()
                                            } else {
                                                scanSuccessMessage = "Gagal mendeteksi barcode. Pastikan kode benar."
                                            }
                                        }
                                    }
                                }
                            ) {
                                Icon(Icons.Filled.Check, contentDescription = "Simulasikan Scan")
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("barcode_simulator_input")
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Selesai")
            }
        }
    )
}
