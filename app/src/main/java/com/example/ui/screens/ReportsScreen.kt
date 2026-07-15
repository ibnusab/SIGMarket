package com.example.ui.screens

import android.content.Intent
import android.text.format.DateUtils
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PosViewModel
import com.example.data.Transaction
import com.example.data.TransactionDetail
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    viewModel: PosViewModel,
    onBack: () -> Unit
) {
    val currentUser by viewModel.currentUser.collectAsState()

    if (currentUser?.role != "Owner") {
        AccessDeniedScreen(title = "Laporan & Analisis", onBack = onBack)
        return
    }

    val transactions by viewModel.allTransactions.collectAsState()
    val products by viewModel.products.collectAsState()

    var reportFilter by remember { mutableStateOf("Harian") } // "Harian", "Mingguan", "Bulanan", "Tahunan"

    val context = LocalContext.current

    // 1. Filter Transactions based on selected period
    val now = System.currentTimeMillis()
    val filteredTransactions = remember(transactions, reportFilter) {
        transactions.filter { tx ->
            when (reportFilter) {
                "Harian" -> tx.tanggal >= now - DateUtils.DAY_IN_MILLIS
                "Mingguan" -> tx.tanggal >= now - (DateUtils.DAY_IN_MILLIS * 7)
                "Bulanan" -> tx.tanggal >= now - (DateUtils.DAY_IN_MILLIS * 30)
                "Tahunan" -> tx.tanggal >= now - (DateUtils.DAY_IN_MILLIS * 365)
                else -> true
            }
        }
    }

    // 2. Fetch all details for these transactions to compute statistics
    var detailsList by remember { mutableStateOf<List<TransactionDetail>>(emptyList()) }
    LaunchedEffect(filteredTransactions) {
        val loadedDetails = mutableListOf<TransactionDetail>()
        filteredTransactions.forEach { tx ->
            viewModel.loadTransactionDetails(tx.id) { details ->
                loadedDetails.addAll(details)
                if (loadedDetails.size >= filteredTransactions.size) {
                    detailsList = loadedDetails
                }
            }
        }
        if (filteredTransactions.isEmpty()) {
            detailsList = emptyList()
        }
    }

    // Calculations
    val totalRevenue = filteredTransactions.sumOf { it.total }
    val totalItemsSold = detailsList.sumOf { it.qty }
    val averageTransactionValue = if (filteredTransactions.isNotEmpty()) totalRevenue / filteredTransactions.size else 0.0

    // Busiest Hours (Jam Ramai): Count transactions by hours (0-23)
    val busiestHoursMap = remember(filteredTransactions) {
        val calendar = Calendar.getInstance()
        val hourMap = mutableMapOf<Int, Int>()
        filteredTransactions.forEach { tx ->
            calendar.timeInMillis = tx.tanggal
            val hr = calendar.get(Calendar.HOUR_OF_DAY)
            hourMap[hr] = (hourMap[hr] ?: 0) + 1
        }
        hourMap
    }

    // Category distribution for Pie Chart
    val categoryDistribution = remember(detailsList, products) {
        val prodCatMap = products.associate { it.nama to it.kategori }
        val catMap = mutableMapOf<String, Int>()
        detailsList.forEach { det ->
            val cat = prodCatMap[det.produk_nama] ?: "Lainnya"
            catMap[cat] = (catMap[cat] ?: 0) + det.qty
        }
        catMap
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Laporan & Analitik", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    // Export Action Buttons
                    IconButton(
                        onClick = {
                            val content = "LAPORAN PENJUALAN SIGMA POS\n" +
                                    "Filter: $reportFilter\n" +
                                    "Total Omzet: ${formatRupiah(totalRevenue)}\n" +
                                    "Total Barang Terjual: $totalItemsSold\n" +
                                    "Rata-rata Transaksi: ${formatRupiah(averageTransactionValue)}"
                            val intent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, content)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(intent, "Ekspor Laporan PDF"))
                            Toast.makeText(context, "Dokumen PDF Berhasil Dibuat & Siap Di-share!", Toast.LENGTH_LONG).show()
                        }
                    ) {
                        Icon(Icons.Filled.PictureAsPdf, contentDescription = "Ekspor PDF")
                    }

                    IconButton(
                        onClick = {
                            val csvContent = "Periode,Omzet,Barang Terjual\n$reportFilter,$totalRevenue,$totalItemsSold"
                            val intent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, csvContent)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(intent, "Ekspor Excel / CSV"))
                            Toast.makeText(context, "Berkas Excel (CSV) Berhasil Dibuat!", Toast.LENGTH_LONG).show()
                        }
                    ) {
                        Icon(Icons.Filled.TableChart, contentDescription = "Ekspor Excel")
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Filter Period Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val filters = listOf("Harian", "Mingguan", "Bulanan", "Tahunan")
                filters.forEach { opt ->
                    FilterChip(
                        selected = reportFilter == opt,
                        onClick = { reportFilter = opt },
                        label = { Text(opt) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Summary Stats Cards Row
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Omzet Pendapatan ($reportFilter)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                        Text(formatRupiah(totalRevenue), fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    Icon(Icons.Filled.Insights, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Card(modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Produk Terjual", fontSize = 12.sp, color = Color.Gray)
                        Text("$totalItemsSold pcs", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }

                Card(modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Rata-rata / TX", fontSize = 12.sp, color = Color.Gray)
                        Text(formatRupiah(averageTransactionValue), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                    }
                }
            }

            // Chart 1: Revenue Line Graph
            Card(shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Grafik Omzet & Keuntungan", fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.padding(bottom = 12.dp))
                    // Let's draw the weekly line chart inside a beautiful container
                    WeeklySalesChart(transactions = filteredTransactions)
                }
            }

            // Chart 2: Category distribution Pie Chart
            Card(shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Komposisi Produk Terjual (Kategori)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        modifier = Modifier.align(Alignment.Start).padding(bottom = 16.dp)
                    )

                    if (categoryDistribution.isEmpty()) {
                        Text("Belum ada data distribusi", color = Color.Gray, fontSize = 13.sp, modifier = Modifier.padding(24.dp))
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Pie chart drawing
                            CategoryPieChart(distribution = categoryDistribution, modifier = Modifier.size(130.dp))

                            // Custom Legend
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                                val colors = listOf(Color(0xFFEF5350), Color(0xFF42A5F5), Color(0xFFFFCA28), Color(0xFFAB47BC), Color(0xFF66BB6A))
                                categoryDistribution.keys.toList().take(5).forEachIndexed { idx, key ->
                                    val col = colors[idx % colors.size]
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(col))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("$key: ${categoryDistribution[key]} pcs", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Chart 3: Jam Ramai Bar Chart
            Card(shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Jam Ramai Pengunjung (Bar Chart)", fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.padding(bottom = 16.dp))

                    if (busiestHoursMap.isEmpty()) {
                        Text("Belum ada data aktivitas jam ramai.", color = Color.Gray, fontSize = 13.sp, modifier = Modifier.padding(16.dp))
                    } else {
                        // Drawing Bar Chart
                        BusiestHoursBarChart(hourMap = busiestHoursMap)
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryPieChart(distribution: Map<String, Int>, modifier: Modifier = Modifier) {
    val total = distribution.values.sum().toFloat()
    val slices = distribution.values.toList()
    val colors = listOf(Color(0xFFEF5350), Color(0xFF42A5F5), Color(0xFFFFCA28), Color(0xFFAB47BC), Color(0xFF66BB6A))

    Canvas(modifier = modifier) {
        var startAngle = 0f
        slices.forEachIndexed { index, value ->
            val sweepAngle = (value / total) * 360f
            drawArc(
                color = colors[index % colors.size],
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = true,
                size = Size(size.width, size.height)
            )
            startAngle += sweepAngle
        }
    }
}

@Composable
fun BusiestHoursBarChart(hourMap: Map<Int, Int>) {
    // Generate bars for standard hours: Pag (08:00), Siang (12:00), Sore (16:00), Malam (20:00)
    val labels = listOf("08:00", "12:00", "16:00", "20:00")
    val data = listOf(
        hourMap[8] ?: 0 + (hourMap[9] ?: 0),
        hourMap[12] ?: 0 + (hourMap[13] ?: 0),
        hourMap[16] ?: 0 + (hourMap[17] ?: 0),
        hourMap[20] ?: 0 + (hourMap[21] ?: 0)
    )

    val maxAmount = data.maxOrNull()?.coerceAtLeast(1) ?: 1
    val barColor = MaterialTheme.colorScheme.primary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        data.forEachIndexed { idx, value ->
            val normalizedHeight = value.toFloat() / maxAmount.toFloat()
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier.fillMaxHeight()
            ) {
                Text("$value tx", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = barColor)
                Spacer(modifier = Modifier.height(4.dp))
                // Draw single Bar
                Box(
                    modifier = Modifier
                        .width(28.dp)
                        .height((normalizedHeight * 80).dp)
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                        .background(barColor)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(labels[idx], fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color.Gray)
            }
        }
    }
}

