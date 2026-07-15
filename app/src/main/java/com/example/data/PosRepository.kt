package com.example.data

import kotlinx.coroutines.flow.Flow

class PosRepository(
    private val userDao: UserDao,
    private val categoryDao: CategoryDao,
    private val productDao: ProductDao,
    private val supplierDao: SupplierDao,
    private val transactionDao: TransactionDao,
    private val transactionDetailDao: TransactionDetailDao
) {
    // User functions
    suspend fun getUserByEmail(email: String): User? = userDao.getUserByEmail(email)
    fun getUserById(id: Int): Flow<User?> = userDao.getUserById(id)
    suspend fun insertUser(user: User): Long = userDao.insertUser(user)
    suspend fun updateUser(user: User) = userDao.updateUser(user)

    // Category functions
    val allCategories: Flow<List<Category>> = categoryDao.getAllCategories()
    suspend fun insertCategory(category: Category): Long = categoryDao.insertCategory(category)
    suspend fun deleteCategory(category: Category) = categoryDao.deleteCategory(category)

    // Product functions
    val allProducts: Flow<List<Product>> = productDao.getAllProducts()
    suspend fun getProductByBarcode(barcode: String): Product? = productDao.getProductByBarcode(barcode)
    suspend fun getProductById(id: Int): Product? = productDao.getProductById(id)
    suspend fun insertProduct(product: Product): Long = productDao.insertProduct(product)
    suspend fun updateProduct(product: Product) = productDao.updateProduct(product)
    suspend fun deleteProduct(product: Product) = productDao.deleteProduct(product)

    // Supplier functions
    val allSuppliers: Flow<List<Supplier>> = supplierDao.getAllSuppliers()
    suspend fun insertSupplier(supplier: Supplier): Long = supplierDao.insertSupplier(supplier)
    suspend fun updateSupplier(supplier: Supplier) = supplierDao.updateSupplier(supplier)
    suspend fun deleteSupplier(supplier: Supplier) = supplierDao.deleteSupplier(supplier)

    // Transaction functions
    val allTransactions: Flow<List<Transaction>> = transactionDao.getAllTransactions()
    fun getTransactionsInRange(start: Long, end: Long): Flow<List<Transaction>> =
        transactionDao.getTransactionsInRange(start, end)
    suspend fun insertTransaction(transaction: Transaction): Long =
        transactionDao.insertTransaction(transaction)
    suspend fun deleteTransaction(transaction: Transaction) =
        transactionDao.deleteTransaction(transaction)

    // Transaction Detail functions
    suspend fun getDetailsForTransaction(transactionId: Int): List<TransactionDetail> =
        transactionDetailDao.getDetailsForTransaction(transactionId)
    suspend fun insertTransactionDetails(details: List<TransactionDetail>) =
        transactionDetailDao.insertTransactionDetails(details)
}
