package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PosViewModel
import com.example.data.Product
import com.example.data.Supplier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManagementScreen(
    viewModel: PosViewModel,
    onBack: () -> Unit
) {
    val currentUser by viewModel.currentUser.collectAsState()

    if (currentUser?.role != "Owner") {
        AccessDeniedScreen(title = "Logistik & Manajemen", onBack = onBack)
        return
    }

    val suppliers by viewModel.suppliers.collectAsState()
    val products by viewModel.products.collectAsState()

    var selectedTab by remember { mutableStateOf(0) } // 0 = Suppliers, 1 = Stock
    val tabs = listOf("Supplier", "Stok Barang")

    // Supplier dialog states
    var showSupplierDialog by remember { mutableStateOf(false) }
    var editingSupplier by remember { mutableStateOf<Supplier?>(null) }

    // Stock direct modifier states
    var adjustingProduct by remember { mutableStateOf<Product?>(null) }
    var isIncomingStock by remember { mutableStateOf(true) } // true = Inflow, false = Outflow
    var showStockAdjustmentDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Logistik & Manajemen", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                }
            )
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(
                    onClick = {
                        editingSupplier = null
                        showSupplierDialog = true
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Tambah Supplier")
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
            // Tab row
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, fontWeight = FontWeight.Bold) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (selectedTab == 0) {
                // Supplier Screen Tab
                if (suppliers.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Belum ada supplier. Klik '+' untuk menambah.", color = Color.Gray)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(suppliers) { sup ->
                            SupplierCard(
                                supplier = sup,
                                onEdit = {
                                    editingSupplier = sup
                                    showSupplierDialog = true
                                },
                                onDelete = {
                                    viewModel.deleteSupplier(sup)
                                    Toast.makeText(context, "Supplier dihapus", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }
            } else {
                // Stock Screen Tab
                val outOfStockCount = products.count { it.stok == 0 }
                val lowStockCount = products.count { it.stok in 1..10 }

                // Low / Out of Stock Banner Notifications
                if (outOfStockCount > 0 || lowStockCount > 0) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                if (outOfStockCount > 0) {
                                    Text(
                                        text = "$outOfStockCount Produk Kehabisan Stok!",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                                if (lowStockCount > 0) {
                                    Text(
                                        text = "$lowStockCount Produk Mendekati Batas Stok Minimum (<=10 pcs)",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(products) { item ->
                        StockManagementRow(
                            product = item,
                            onInflow = {
                                adjustingProduct = item
                                isIncomingStock = true
                                showStockAdjustmentDialog = true
                            },
                            onOutflow = {
                                adjustingProduct = item
                                isIncomingStock = false
                                showStockAdjustmentDialog = true
                            }
                        )
                    }
                }
            }
        }
    }

    // Add Edit Supplier Dialog
    if (showSupplierDialog) {
        AddEditSupplierDialog(
            supplier = editingSupplier,
            onDismiss = { showSupplierDialog = false },
            onSave = { sup ->
                viewModel.saveSupplier(sup)
                showSupplierDialog = false
            }
        )
    }

    // Stock Adjustments Dialog
    if (showStockAdjustmentDialog && adjustingProduct != null) {
        val prod = adjustingProduct!!
        var quantityInput by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showStockAdjustmentDialog = false },
            title = { Text(if (isIncomingStock) "Barang Masuk (Inflow)" else "Barang Keluar (Outflow)") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Produk: ${prod.nama}", fontWeight = FontWeight.Bold)
                    Text("Stok Sekarang: ${prod.stok} pcs", fontSize = 13.sp, color = Color.Gray)
                    
                    OutlinedTextField(
                        value = quantityInput,
                        onValueChange = { quantityInput = it },
                        label = { Text("Jumlah Perubahan Stok") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("stock_adjust_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val changeQty = quantityInput.toIntOrNull() ?: 0
                        if (changeQty > 0) {
                            val finalStok = if (isIncomingStock) {
                                prod.stok + changeQty
                            } else {
                                (prod.stok - changeQty).coerceAtLeast(0)
                            }
                            viewModel.saveProduct(prod.copy(stok = finalStok))
                            Toast.makeText(context, "Stok ${prod.nama} diperbarui menjadi $finalStok", Toast.LENGTH_SHORT).show()
                            showStockAdjustmentDialog = false
                        } else {
                            Toast.makeText(context, "Jumlah harus di atas 0", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Perbarui")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStockAdjustmentDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
fun SupplierCard(
    supplier: Supplier,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(supplier.nama, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Phone, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(supplier.telepon, fontSize = 13.sp, color = Color.DarkGray)
                }
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Place, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(supplier.alamat, fontSize = 12.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }

            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "Hapus", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
fun AddEditSupplierDialog(
    supplier: Supplier?,
    onDismiss: () -> Unit,
    onSave: (Supplier) -> Unit
) {
    var nama by remember { mutableStateOf(supplier?.nama ?: "") }
    var telepon by remember { mutableStateOf(supplier?.telepon ?: "") }
    var alamat by remember { mutableStateOf(supplier?.alamat ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (supplier == null) "Tambah Supplier Baru" else "Edit Supplier") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = nama,
                    onValueChange = { nama = it },
                    label = { Text("Nama Supplier / Perusahaan") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("supplier_name_input")
                )

                OutlinedTextField(
                    value = telepon,
                    onValueChange = { telepon = it },
                    label = { Text("Nomor HP / Telepon") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = alamat,
                    onValueChange = { alamat = it },
                    label = { Text("Alamat Kantor / Toko") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (nama.isNotBlank() && telepon.isNotBlank()) {
                        onSave(
                            Supplier(
                                id = supplier?.id ?: 0,
                                nama = nama,
                                telepon = telepon,
                                alamat = alamat
                            )
                        )
                    }
                },
                modifier = Modifier.testTag("save_supplier_button")
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

@Composable
fun StockManagementRow(
    product: Product,
    onInflow: () -> Unit,
    onOutflow: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(product.nama, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text("SKU/Barcode: ${product.barcode}", fontSize = 11.sp, color = Color.Gray)
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    product.stok == 0 -> MaterialTheme.colorScheme.error
                                    product.stok <= 10 -> MaterialTheme.colorScheme.tertiary
                                    else -> MaterialTheme.colorScheme.secondary
                                }
                            )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Stok: ${product.stok} unit",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            product.stok == 0 -> MaterialTheme.colorScheme.error
                            product.stok <= 10 -> MaterialTheme.colorScheme.tertiary
                            else -> Color.Gray
                        }
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onInflow,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Masuk", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onOutflow,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(Icons.Filled.Remove, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Keluar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

