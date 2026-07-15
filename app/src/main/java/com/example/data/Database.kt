package com.example.data

import android.content.Context
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.Executors

// ==========================================
// 1. Entities
// ==========================================

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nama: String,
    val email: String,
    val password: String,
    val role: String // "Owner", "Kasir"
)

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nama: String
)

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val barcode: String,
    val nama: String,
    val kategori: String, // linked to Category nama
    val harga: Double,
    val stok: Int,
    val foto: String? = null // Can store image URI or custom icon name
)

@Entity(tableName = "suppliers")
data class Supplier(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nama: String,
    val telepon: String,
    val alamat: String
)

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val tanggal: Long, // timestamp
    val total: Double,
    val metode_pembayaran: String, // "Tunai", "QRIS", "Transfer Bank", "E-Wallet"
    val kasir: String,
    val diskon: Double = 0.0,
    val pajak: Double = 0.0,
    val catatan: String? = null,
    val nominal_bayar: Double = 0.0
)

@Entity(tableName = "transaction_details")
data class TransactionDetail(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val transaksi_id: Int,
    val produk_id: Int,
    val produk_nama: String, // historical name preservation
    val qty: Int,
    val harga: Double // historical price preservation
)

// ==========================================
// 2. DAOs
// ==========================================

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): User?

    @Query("SELECT * FROM users WHERE id = :id")
    fun getUserById(id: Int): Flow<User?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User): Long

    @Update
    suspend fun updateUser(user: User)
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY nama ASC")
    fun getAllCategories(): Flow<List<Category>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: Category): Long

    @Delete
    suspend fun deleteCategory(category: Category)
}

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY nama ASC")
    fun getAllProducts(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE barcode = :barcode LIMIT 1")
    suspend fun getProductByBarcode(barcode: String): Product?

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getProductById(id: Int): Product?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: Product): Long

    @Update
    suspend fun updateProduct(product: Product)

    @Delete
    suspend fun deleteProduct(product: Product)
}

@Dao
interface SupplierDao {
    @Query("SELECT * FROM suppliers ORDER BY nama ASC")
    fun getAllSuppliers(): Flow<List<Supplier>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSupplier(supplier: Supplier): Long

    @Update
    suspend fun updateSupplier(supplier: Supplier)

    @Delete
    suspend fun deleteSupplier(supplier: Supplier)
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY tanggal DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE tanggal >= :startOfDay AND tanggal <= :endOfDay ORDER BY tanggal DESC")
    fun getTransactionsInRange(startOfDay: Long, endOfDay: Long): Flow<List<Transaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction): Long

    @Delete
    suspend fun deleteTransaction(transaction: Transaction)
}

@Dao
interface TransactionDetailDao {
    @Query("SELECT * FROM transaction_details WHERE transaksi_id = :transactionId")
    suspend fun getDetailsForTransaction(transactionId: Int): List<TransactionDetail>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactionDetails(details: List<TransactionDetail>)
}

// ==========================================
// 3. App Database
// ==========================================

@Database(
    entities = [
        User::class,
        Category::class,
        Product::class,
        Supplier::class,
        Transaction::class,
        TransactionDetail::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun categoryDao(): CategoryDao
    abstract fun productDao(): ProductDao
    abstract fun supplierDao(): SupplierDao
    abstract fun transactionDao(): TransactionDao
    abstract fun transactionDetailDao(): TransactionDetailDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "sigma_pos_database"
                )
                    .addCallback(DatabaseCallback(context))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(private val context: Context) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // Populate default data on database creation using background executor
                Executors.newSingleThreadExecutor().execute {
                    val database = getDatabase(context)
                    
                    // Prepopulate User
                    val defaultUser = User(
                        nama = "Admin Sigma",
                        email = "admin@sigma.com",
                        password = "admin", // simple text for trial
                        role = "Owner"
                    )
                    
                    // Prepopulate Categories
                    val defaultCategories = listOf(
                        Category(nama = "Makanan"),
                        Category(nama = "Minuman"),
                        Category(nama = "Snack"),
                        Category(nama = "Pakaian"),
                        Category(nama = "Jasa")
                    )

                    // Prepopulate Suppliers
                    val defaultSuppliers = listOf(
                        Supplier(nama = "Cahaya Abadi Sembako", telepon = "08123456789", alamat = "Jl. Merdeka No. 10, Jakarta"),
                        Supplier(nama = "Distributor Minuman Sehat", telepon = "08567890123", alamat = "Jl. Sudirman No. 45, Bandung"),
                        Supplier(nama = "Snack Nusantara", telepon = "08219876543", alamat = "Jl. Diponegoro No. 12, Surabaya")
                    )

                    // Prepopulate Products
                    val defaultProducts = listOf(
                        Product(barcode = "8999999002235", nama = "Indomie Goreng", kategori = "Makanan", harga = 3500.0, stok = 150),
                        Product(barcode = "8992761100114", nama = "AQUA Air Mineral 600ml", kategori = "Minuman", harga = 4000.0, stok = 80),
                        Product(barcode = "8999999035622", nama = "Chitato Sapi Panggang", kategori = "Snack", harga = 11500.0, stok = 45),
                        Product(barcode = "8991389221147", nama = "Kopi Kenangan Mantan Bottle", kategori = "Minuman", harga = 9500.0, stok = 30),
                        Product(barcode = "123456789", nama = "Kaos Polos Cotton Combed", kategori = "Pakaian", harga = 65000.0, stok = 20),
                        Product(barcode = "000000000", nama = "Potong Rambut (Barber)", kategori = "Jasa", harga = 25000.0, stok = 999)
                    )

                    // Insert using blocking calls inside thread
                    kotlinx.coroutines.runBlocking {
                        database.userDao().insertUser(defaultUser)
                        for (cat in defaultCategories) {
                            database.categoryDao().insertCategory(cat)
                        }
                        for (sup in defaultSuppliers) {
                            database.supplierDao().insertSupplier(sup)
                        }
                        for (prod in defaultProducts) {
                            database.productDao().insertProduct(prod)
                        }
                    }
                }
            }
        }
    }
}
