package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.example.data.*
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // Initialize Database, Repository and ViewModel
            val database = AppDatabase.getDatabase(applicationContext)
            val repository = PosRepository(
                userDao = database.userDao(),
                categoryDao = database.categoryDao(),
                productDao = database.productDao(),
                supplierDao = database.supplierDao(),
                transactionDao = database.transactionDao(),
                transactionDetailDao = database.transactionDetailDao()
            )
            val viewModel: PosViewModel = viewModel(
                factory = PosViewModelFactory(repository, applicationContext)
            )

            val isDarkMode by viewModel.isDarkMode.collectAsState()

            MyApplicationTheme(darkTheme = isDarkMode) {
                MainAppNavHost(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainAppNavHost(viewModel: PosViewModel) {
    val navController = rememberNavController()
    val currentUser by viewModel.currentUser.collectAsState()

    // Determine current route to show/hide bottom bar
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Tabs that show the Bottom Navigation Bar
    val bottomBarRoutes = listOf("dashboard", "cart", "history", "reports", "profile")

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (currentUser != null && currentRoute in bottomBarRoutes) {
                NavigationBar(
                    modifier = Modifier.testTag("bottom_nav_bar")
                ) {
                    NavigationBarItem(
                        selected = currentRoute == "dashboard",
                        onClick = {
                            navController.navigate("dashboard") {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Filled.Home, contentDescription = "Beranda") },
                        label = { Text("Beranda", fontSize = 11.sp) }
                    )

                    NavigationBarItem(
                        selected = currentRoute == "cart",
                        onClick = {
                            navController.navigate("cart") {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Filled.PointOfSale, contentDescription = "Kasir") },
                        label = { Text("Kasir", fontSize = 11.sp) }
                    )

                    NavigationBarItem(
                        selected = currentRoute == "history",
                        onClick = {
                            navController.navigate("history") {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Filled.ReceiptLong, contentDescription = "Riwayat") },
                        label = { Text("Riwayat", fontSize = 11.sp) }
                    )

                    if (currentUser?.role == "Owner") {
                        NavigationBarItem(
                            selected = currentRoute == "reports",
                            onClick = {
                                navController.navigate("reports") {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(Icons.Filled.BarChart, contentDescription = "Laporan") },
                            label = { Text("Laporan", fontSize = 11.sp) }
                        )
                    }

                    NavigationBarItem(
                        selected = currentRoute == "profile",
                        onClick = {
                            navController.navigate("profile") {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Filled.Settings, contentDescription = "Profil") },
                        label = { Text("Profil", fontSize = 11.sp) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = if (currentUser == null) "login" else "dashboard",
            modifier = Modifier.padding(innerPadding)
        ) {
            // Authentication Routes
            composable("login") {
                LoginScreen(
                    viewModel = viewModel,
                    onLoginSuccess = {
                        navController.navigate("dashboard") {
                            popUpTo("login") { inclusive = true }
                        }
                    },
                    onNavigateToRegister = { navController.navigate("register") },
                    onNavigateToForgotPassword = { navController.navigate("forgot_password") }
                )
            }

            composable("register") {
                RegisterScreen(
                    viewModel = viewModel,
                    onRegisterSuccess = {
                        navController.navigate("dashboard") {
                            popUpTo("register") { inclusive = true }
                        }
                    },
                    onNavigateToLogin = { navController.navigate("login") }
                )
            }

            composable("forgot_password") {
                ForgotPasswordScreen(
                    onNavigateToLogin = { navController.navigate("login") }
                )
            }

            // Dashboard Screens
            composable("dashboard") {
                DashboardScreen(
                    viewModel = viewModel,
                    onNavigateToCheckout = { navController.navigate("cart") },
                    onNavigateToProducts = { navController.navigate("products") },
                    onNavigateToManagement = { navController.navigate("management") }
                )
            }

            // Products Management Screen
            composable("products") {
                ProductManagementScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            // Logistics & Supplier Management Screen
            composable("management") {
                ManagementScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            // Checkout Cart Screen
            composable("cart") {
                CartScreen(
                    viewModel = viewModel,
                    onBackToDashboard = { navController.navigate("dashboard") },
                    onNavigateToProducts = { navController.navigate("products") }
                )
            }

            // Transaction History Screen
            composable("history") {
                TransactionHistoryScreen(
                    viewModel = viewModel,
                    onBack = { navController.navigate("dashboard") }
                )
            }

            // Analytics Reports Screen
            composable("reports") {
                ReportsScreen(
                    viewModel = viewModel,
                    onBack = { navController.navigate("dashboard") }
                )
            }

            // Shop Settings Screen
            composable("profile") {
                ProfileSettingsScreen(
                    viewModel = viewModel,
                    onBack = { navController.navigate("dashboard") },
                    onLogout = {
                        viewModel.logout()
                        navController.navigate("login") {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}
