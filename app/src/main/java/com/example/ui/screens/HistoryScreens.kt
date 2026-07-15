package com.example.ui.screens

import android.text.format.DateUtils
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PosViewModel
import com.example.data.Transaction
import com.example.data.TransactionDetail
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionHistoryScreen(
    viewModel: PosViewModel,
    onBack: () -> Unit
) {
    val transactions by viewModel.allTransactions.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("Semua") } // "Hari", "Minggu", "Bulan", "Tahun", "Semua"

    // Dialog state for viewing full receipt details
    var selectedTransaction by remember { mutableStateOf<Transaction?>(null) }
    var selectedDetails by remember { mutableStateOf<List<TransactionDetail>>(emptyList()) }

    val filteredTransactions = remember(transactions, searchQuery, selectedFilter) {
        val now = System.currentTimeMillis()
        transactions.filter { tx ->
            val matchesQuery = tx.id.toString().contains(searchQuery) ||
                    tx.kasir.contains(searchQuery, ignoreCase = true) ||
                    tx.metode_pembayaran.contains(searchQuery, ignoreCase = true)

            val matchesFilter = when (selectedFilter) {
                "Hari" -> tx.tanggal >= now - DateUtils.DAY_IN_MILLIS
                "Minggu" -> tx.tanggal >= now - (DateUtils.DAY_IN_MILLIS * 7)
                "Bulan" -> tx.tanggal >= now - (DateUtils.DAY_IN_MILLIS * 30)
                "Tahun" -> tx.tanggal >= now - (DateUtils.DAY_IN_MILLIS * 365)
                else -> true
            }

            matchesQuery && matchesFilter
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Riwayat Transaksi", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Search Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Cari ID Transaksi / Kasir / Metode") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("history_search_input")
            )

            // Filter Row (Hari, Minggu, Bulan, Tahun, Semua)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val filterOptions = listOf("Hari", "Minggu", "Bulan", "Tahun", "Semua")
                filterOptions.forEach { opt ->
                    FilterChip(
                        selected = selectedFilter == opt,
                        onClick = { selectedFilter = opt },
                        label = { Text(opt) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Transaction List
            if (filteredTransactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Filled.ReceiptLong,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tidak ada transaksi dalam riwayat",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            fontSize = 15.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(filteredTransactions) { tx ->
                        TransactionHistoryCard(
                            transaction = tx,
                            onClick = {
                                selectedTransaction = tx
                                viewModel.loadTransactionDetails(tx.id) { details ->
                                    selectedDetails = details
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // View Receipt Details Dialog
    selectedTransaction?.let { tx ->
        ReceiptDetailsDialog(
            transaction = tx,
            details = selectedDetails,
            viewModel = viewModel,
            onDismiss = { selectedTransaction = null }
        )
    }
}

@Composable
fun TransactionHistoryCard(
    transaction: Transaction,
    onClick: () -> Unit
) {
    val dateStr = remember(transaction.tanggal) {
        SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID")).format(Date(transaction.tanggal))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .testTag("transaction_card_${transaction.id}"),
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
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Receipt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "TX-${transaction.id}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "$dateStr • Kasir: ${transaction.kasir}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(2.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = when (transaction.metode_pembayaran) {
                                    "Tunai" -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                                    else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                },
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = transaction.metode_pembayaran,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = when (transaction.metode_pembayaran) {
                                "Tunai" -> MaterialTheme.colorScheme.secondary
                                else -> MaterialTheme.colorScheme.primary
                            }
                        )
                    }
                    if (transaction.catatan?.isNotBlank() == true) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Filled.Notes,
                            contentDescription = "Ada catatan",
                            tint = Color.Gray,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }

            Text(
                text = formatRupiah(transaction.total),
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
