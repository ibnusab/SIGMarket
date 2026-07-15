package com.example.data

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class PosViewModel(private val repository: PosRepository, private val context: Context) : ViewModel() {

    // Shared Preferences for Shop Profile, Theme, and Preferences
    private val prefs = context.getSharedPreferences("sigma_pos_prefs", Context.MODE_PRIVATE)

    // Auth State
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _rememberMe = MutableStateFlow(prefs.getBoolean("remember_me", false))
    val rememberMe: StateFlow<Boolean> = _rememberMe.asStateFlow()

    // Shop Profile State
    private val _shopName = MutableStateFlow(prefs.getString("shop_name", "SIGMA POS Shop") ?: "SIGMA POS Shop")
    val shopName: StateFlow<String> = _shopName.asStateFlow()

    private val _shopAddress = MutableStateFlow(prefs.getString("shop_address", "Jl. Utama No. 8, Jakarta") ?: "Jl. Utama No. 8, Jakarta")
    val shopAddress: StateFlow<String> = _shopAddress.asStateFlow()

    private val _shopPhone = MutableStateFlow(prefs.getString("shop_phone", "081234567800") ?: "081234567800")
    val shopPhone: StateFlow<String> = _shopPhone.asStateFlow()

    private val _shopEmail = MutableStateFlow(prefs.getString("shop_email", "owner@sigmapos.com") ?: "owner@sigmapos.com")
    val shopEmail: StateFlow<String> = _shopEmail.asStateFlow()

    // Settings State
    private val _isDarkMode = MutableStateFlow(prefs.getBoolean("dark_mode", false))
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _language = MutableStateFlow(prefs.getString("language", "Bahasa Indonesia") ?: "Bahasa Indonesia")
    val language: StateFlow<String> = _language.asStateFlow()

    // DB Observables
    val categories: StateFlow<List<Category>> = repository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val products: StateFlow<List<Product>> = repository.allProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val suppliers: StateFlow<List<Supplier>> = repository.allSuppliers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTransactions: StateFlow<List<Transaction>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Cart (Keranjang) State
    // Using Product list of quantities to maintain reactive UI easily
    private val _cartItems = MutableStateFlow<Map<Product, Int>>(emptyMap())
    val cartItems: StateFlow<Map<Product, Int>> = _cartItems.asStateFlow()

    private val _cartNotes = MutableStateFlow("")
    val cartNotes: StateFlow<String> = _cartNotes.asStateFlow()

    private val _discountPercent = MutableStateFlow(0.0) // in % (e.g. 10.0 for 10%)
    val discountPercent: StateFlow<Double> = _discountPercent.asStateFlow()

    private val _taxPercent = MutableStateFlow(11.0) // PPN 11% by default
    val taxPercent: StateFlow<Double> = _taxPercent.asStateFlow()

    // Payment States
    private val _paymentMethod = MutableStateFlow("Tunai") // "Tunai", "QRIS", "Transfer Bank", "E-Wallet"
    val paymentMethod: StateFlow<String> = _paymentMethod.asStateFlow()

    private val _nominalBayar = MutableStateFlow(0.0)
    val nominalBayar: StateFlow<Double> = _nominalBayar.asStateFlow()

    // UI state feedback
    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    init {
        // Auto-login if remember_me and user email was stored
        val savedEmail = prefs.getString("saved_user_email", null)
        if (_rememberMe.value && savedEmail != null) {
            viewModelScope.launch {
                val user = repository.getUserByEmail(savedEmail)
                if (user != null) {
                    _currentUser.value = user
                }
            }
        }
    }

    // ==========================================
    // Authentication Operations
    // ==========================================
    fun login(email: String, password: CharSequence, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            _authError.value = null
            val user = repository.getUserByEmail(email)
            if (user != null && user.password == password.toString()) {
                _currentUser.value = user
                if (_rememberMe.value) {
                    prefs.edit()
                        .putString("saved_user_email", email)
                        .apply()
                } else {
                    prefs.edit()
                        .remove("saved_user_email")
                        .apply()
                }
                onResult(true)
            } else {
                _authError.value = "Email atau Password salah!"
                onResult(false)
            }
        }
    }

    fun register(nama: String, email: String, password: CharSequence, role: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            _authError.value = null
            val existing = repository.getUserByEmail(email)
            if (existing != null) {
                _authError.value = "Email sudah terdaftar!"
                onResult(false)
            } else {
                val newUser = User(nama = nama, email = email, password = password.toString(), role = role)
                repository.insertUser(newUser)
                _currentUser.value = newUser
                onResult(true)
            }
        }
    }

    fun logout() {
        _currentUser.value = null
        if (!_rememberMe.value) {
            prefs.edit().remove("saved_user_email").apply()
        }
    }

    fun setRememberMe(enabled: Boolean) {
        _rememberMe.value = enabled
        prefs.edit().putBoolean("remember_me", enabled).apply()
    }

    // ==========================================
    // Shop Profile Operations
    // ==========================================
    fun updateShopProfile(name: String, address: String, phone: String, email: String) {
        _shopName.value = name
        _shopAddress.value = address
        _shopPhone.value = phone
        _shopEmail.value = email
        prefs.edit()
            .putString("shop_name", name)
            .putString("shop_address", address)
            .putString("shop_phone", phone)
            .putString("shop_email", email)
            .apply()
    }

    // ==========================================
    // Theme and Language Preferences
    // ==========================================
    fun setDarkMode(enabled: Boolean) {
        _isDarkMode.value = enabled
        prefs.edit().putBoolean("dark_mode", enabled).apply()
    }

    fun setLanguage(lang: String) {
        _language.value = lang
        prefs.edit().putString("language", lang).apply()
    }

    // ==========================================
    // Cart (Keranjang) Operations
    // ==========================================
    fun addToCart(product: Product) {
        if (product.stok <= 0) return
        val currentMap = _cartItems.value.toMutableMap()
        val currentQty = currentMap[product] ?: 0
        if (currentQty < product.stok) {
            currentMap[product] = currentQty + 1
            _cartItems.value = currentMap
        }
    }

    fun removeFromCart(product: Product) {
        val currentMap = _cartItems.value.toMutableMap()
        val currentQty = currentMap[product] ?: 0
        if (currentQty <= 1) {
            currentMap.remove(product)
        } else {
            currentMap[product] = currentQty - 1
        }
        _cartItems.value = currentMap
    }

    fun deleteFromCart(product: Product) {
        val currentMap = _cartItems.value.toMutableMap()
        currentMap.remove(product)
        _cartItems.value = currentMap
    }

    fun clearCart() {
        _cartItems.value = emptyMap()
        _cartNotes.value = ""
        _discountPercent.value = 0.0
        _nominalBayar.value = 0.0
    }

    fun setCartNotes(notes: String) {
        _cartNotes.value = notes
    }

    fun setDiscount(percent: Double) {
        _discountPercent.value = percent
    }

    fun setTax(percent: Double) {
        _taxPercent.value = percent
    }

    // Dynamic cart computations
    val cartSubtotal: Double
        get() = _cartItems.value.entries.sumOf { it.key.harga * it.value }

    val cartDiscountAmount: Double
        get() = cartSubtotal * (_discountPercent.value / 100.0)

    val cartTaxAmount: Double
        get() = (cartSubtotal - cartDiscountAmount) * (_taxPercent.value / 100.0)

    val cartGrandTotal: Double
        get() = cartSubtotal - cartDiscountAmount + cartTaxAmount

    val cartChangeAmount: Double
        get() = if (_nominalBayar.value >= cartGrandTotal) _nominalBayar.value - cartGrandTotal else 0.0

    // ==========================================
    // Checkout & Checkout Operations
    // ==========================================
    fun setPaymentMethod(method: String) {
        _paymentMethod.value = method
        if (method != "Tunai") {
            // Auto-fill payment amount for digital methods
            _nominalBayar.value = cartGrandTotal
        } else {
            _nominalBayar.value = 0.0
        }
    }

    fun setNominalBayar(amount: Double) {
        _nominalBayar.value = amount
    }

    fun submitCheckout(onSuccess: (Transaction) -> Unit, onError: (String) -> Unit) {
        if (_cartItems.value.isEmpty()) {
            onError("Keranjang masih kosong!")
            return
        }
        if (_paymentMethod.value == "Tunai" && _nominalBayar.value < cartGrandTotal) {
            onError("Nominal bayar kurang!")
            return
        }

        viewModelScope.launch {
            try {
                val totalAmount = cartGrandTotal
                val discountAmount = cartDiscountAmount
                val taxAmount = cartTaxAmount
                
                // 1. Save Transaction record
                val newTransaction = Transaction(
                    tanggal = System.currentTimeMillis(),
                    total = totalAmount,
                    metode_pembayaran = _paymentMethod.value,
                    kasir = _currentUser.value?.nama ?: "Kasir Utama",
                    diskon = discountAmount,
                    pajak = taxAmount,
                    catatan = _cartNotes.value,
                    nominal_bayar = if (_paymentMethod.value == "Tunai") _nominalBayar.value else totalAmount
                )

                val txId = repository.insertTransaction(newTransaction)
                val finalTransaction = newTransaction.copy(id = txId.toInt())

                // 2. Save Transaction Detail Records & Update Product Stock
                val details = mutableListOf<TransactionDetail>()
                for ((product, qty) in _cartItems.value) {
                    details.add(
                        TransactionDetail(
                            transaksi_id = txId.toInt(),
                            produk_id = product.id,
                            produk_nama = product.nama,
                            qty = qty,
                            harga = product.harga
                        )
                    )
                    
                    // Decrease product stock
                    val updatedStock = (product.stok - qty).coerceAtLeast(0)
                    repository.updateProduct(product.copy(stok = updatedStock))
                }
                repository.insertTransactionDetails(details)

                // 3. Clear cart
                clearCart()

                onSuccess(finalTransaction)
            } catch (e: Exception) {
                onError("Gagal memproses transaksi: ${e.localizedMessage}")
            }
        }
    }

    // ==========================================
    // Barcode Scanner Integration
    // ==========================================
    fun scanBarcode(barcode: String, onResult: (Boolean, Product?) -> Unit) {
        viewModelScope.launch {
            val product = repository.getProductByBarcode(barcode)
            if (product != null) {
                if (product.stok > 0) {
                    addToCart(product)
                    onResult(true, product)
                } else {
                    onResult(false, null) // out of stock
                }
            } else {
                // Not found locally. Try resolving online/offline registry!
                try {
                    val resolvedProduct = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        BarcodeResolver.resolveOnline(barcode)
                    }
                    if (resolvedProduct != null) {
                        // Ensure category exists
                        val currentCategories = categories.value
                        val categoryExists = currentCategories.any { it.nama.equals(resolvedProduct.kategori, ignoreCase = true) }
                        if (!categoryExists) {
                            repository.insertCategory(Category(nama = resolvedProduct.kategori))
                        }
                        
                        // Insert resolved product into database
                        val newId = repository.insertProduct(resolvedProduct)
                        val finalProduct = resolvedProduct.copy(id = newId.toInt())
                        
                        // Add to cart and notify UI success!
                        addToCart(finalProduct)
                        onResult(true, finalProduct)
                    } else {
                        onResult(false, null)
                    }
                } catch (e: Exception) {
                    Log.e("PosViewModel", "Failed to auto-resolve barcode: ${e.localizedMessage}", e)
                    onResult(false, null)
                }
            }
        }
    }

    // ==========================================
    // Product & Category Management
    // ==========================================
    fun saveProduct(product: Product) {
        viewModelScope.launch {
            if (product.id == 0) {
                repository.insertProduct(product)
            } else {
                repository.updateProduct(product)
            }
        }
    }

    fun deleteProduct(product: Product) {
        viewModelScope.launch {
            repository.deleteProduct(product)
        }
    }

    fun saveCategory(categoryName: String) {
        viewModelScope.launch {
            if (categoryName.isNotBlank()) {
                repository.insertCategory(Category(nama = categoryName))
            }
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            repository.deleteCategory(category)
        }
    }

    // ==========================================
    // Supplier Management
    // ==========================================
    fun saveSupplier(supplier: Supplier) {
        viewModelScope.launch {
            if (supplier.id == 0) {
                repository.insertSupplier(supplier)
            } else {
                repository.updateSupplier(supplier)
            }
        }
    }

    fun deleteSupplier(supplier: Supplier) {
        viewModelScope.launch {
            repository.deleteSupplier(supplier)
        }
    }

    // ==========================================
    // Transaction Details Query (reprinting / viewing history details)
    // ==========================================
    fun loadTransactionDetails(transactionId: Int, onResult: (List<TransactionDetail>) -> Unit) {
        viewModelScope.launch {
            val list = repository.getDetailsForTransaction(transactionId)
            onResult(list)
        }
    }

    // ==========================================
    // Backup & Restore Database Operations
    // ==========================================
    fun backupDatabase(onResult: (Boolean, String) -> Unit) {
        try {
            val dbFile = context.getDatabasePath("sigma_pos_database")
            if (dbFile.exists()) {
                val backupDir = context.getExternalFilesDir(null) ?: context.filesDir
                val backupFile = File(backupDir, "SIGMA_POS_BACKUP_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.db")
                
                FileInputStream(dbFile).use { input ->
                    FileOutputStream(backupFile).use { output ->
                        input.copyTo(output)
                    }
                }
                onResult(true, "Database berhasil dicadangkan ke:\n${backupFile.absolutePath}")
            } else {
                onResult(false, "Database tidak ditemukan!")
            }
        } catch (e: Exception) {
            onResult(false, "Pencadangan gagal: ${e.localizedMessage}")
        }
    }

    fun restoreDatabase(backupUri: Uri, onResult: (Boolean, String) -> Unit) {
        try {
            context.contentResolver.openInputStream(backupUri)?.use { input ->
                val dbFile = context.getDatabasePath("sigma_pos_database")
                // Close database connection before restoring
                AppDatabase.getDatabase(context).close()
                
                FileOutputStream(dbFile).use { output ->
                    input.copyTo(output)
                }
                onResult(true, "Database berhasil dipulihkan! Silakan mulai ulang aplikasi.")
            } ?: onResult(false, "Gagal membuka berkas backup.")
        } catch (e: Exception) {
            onResult(false, "Pemulihan gagal: ${e.localizedMessage}")
        }
    }
}

// ViewModel Factory
class PosViewModelFactory(
    private val repository: PosRepository,
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PosViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PosViewModel(repository, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
