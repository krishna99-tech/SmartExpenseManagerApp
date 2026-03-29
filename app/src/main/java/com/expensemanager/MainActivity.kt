package com.expensemanager

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.expensemanager.data.preferences.UserPreferences
import com.expensemanager.ui.screen.AddExpenseScreen
import com.expensemanager.ui.screen.DashboardScreen
import com.expensemanager.ui.screen.ReportScreen
import com.expensemanager.ui.security.LockScreen
import com.expensemanager.ui.theme.SmartExpenseTheme
import com.expensemanager.utils.ThemeMode
import com.expensemanager.viewmodel.ExpenseViewModel
import kotlinx.coroutines.launch

private object NavRoutes {
    const val Dashboard = "dashboard"
    const val Add = "add"
    const val Reports = "reports"
}

class MainActivity : FragmentActivity() {

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val app = application as ExpenseManagerApp
            val prefs = app.userPreferences
            val themeMode by prefs.themeMode.collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)
            val useDynamicColor by prefs.useDynamicColor.collectAsStateWithLifecycle(initialValue = false)
            val systemDark = isSystemInDarkTheme()
            val darkTheme = ThemeMode.isDark(themeMode, systemDark)
            SmartExpenseTheme(darkTheme = darkTheme, dynamicColor = useDynamicColor) {
                val viewModel: ExpenseViewModel = viewModel(factory = ExpenseViewModel.Factory(app))
                val snackbarHostState = remember { SnackbarHostState() }
                val scope = rememberCoroutineScope()
                val context = LocalContext.current

                val appLockEnabled by prefs.appLockEnabled.collectAsStateWithLifecycle(initialValue = false)
                val biometricEnabled by prefs.biometricEnabled.collectAsStateWithLifecycle(initialValue = false)
                val sessionUnlocked by viewModel.sessionUnlocked.collectAsStateWithLifecycle()
                val hasPin = prefs.hasPin()

                // If lock is enabled and a PIN exists, we must keep the session locked.
                // Otherwise (lock disabled or no PIN), allow the user through.
                LaunchedEffect(appLockEnabled, hasPin) {
                    if (!appLockEnabled || !hasPin) {
                        viewModel.unlockSession()
                    } else {
                        viewModel.lockSession()
                    }
                }

                val shouldLock = appLockEnabled && hasPin && !sessionUnlocked
                val message by viewModel.message.collectAsStateWithLifecycle()
                LaunchedEffect(message) {
                    val m = message ?: return@LaunchedEffect
                    snackbarHostState.showSnackbar(m)
                    viewModel.clearMessage()
                }

                var showSettings by remember { mutableStateOf(false) }

                Box(modifier = Modifier.fillMaxSize()) {
                    MainScaffold(
                        viewModel = viewModel,
                        snackbarHostState = snackbarHostState,
                        onOpenSettings = { showSettings = true },
                    )
                    if (shouldLock) {
                        LockScreen(
                            biometricEnabled = biometricEnabled,
                            onUnlocked = { viewModel.unlockSession() },
                            onVerifyPin = { viewModel.verifyPin(it) },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }

                if (showSettings) {
                    SettingsDialog(
                        prefs = prefs,
                        onDismiss = { showSettings = false },
                        onExport = {
                            val file = viewModel.exportCsv()
                            val uri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                file,
                            )
                            val send = Intent(Intent.ACTION_SEND).apply {
                                type = "text/csv"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(
                                Intent.createChooser(
                                    send,
                                    context.getString(R.string.export_share_title),
                                ),
                            )
                        },
                        scope = scope,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScaffold(
    viewModel: ExpenseViewModel,
    snackbarHostState: SnackbarHostState,
    onOpenSettings: () -> Unit,
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: NavRoutes.Dashboard

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_title_bar)) },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Outlined.Settings, contentDescription = stringResource(R.string.cd_settings))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentRoute == NavRoutes.Dashboard,
                    onClick = {
                        navController.navigate(NavRoutes.Dashboard) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = { Icon(Icons.Outlined.Home, contentDescription = null) },
                    label = { Text(stringResource(R.string.nav_dashboard)) },
                )
                NavigationBarItem(
                    selected = currentRoute == NavRoutes.Add,
                    onClick = {
                        navController.navigate(NavRoutes.Add) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = { Icon(Icons.Outlined.AddCircleOutline, contentDescription = null) },
                    label = { Text(stringResource(R.string.nav_add)) },
                )
                NavigationBarItem(
                    selected = currentRoute == NavRoutes.Reports,
                    onClick = {
                        navController.navigate(NavRoutes.Reports) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = { Icon(Icons.Outlined.BarChart, contentDescription = null) },
                    label = { Text(stringResource(R.string.nav_reports)) },
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = NavRoutes.Dashboard,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(NavRoutes.Dashboard) {
                DashboardScreen(viewModel = viewModel)
            }
            composable(NavRoutes.Add) {
                AddExpenseScreen(viewModel = viewModel)
            }
            composable(NavRoutes.Reports) {
                ReportScreen(viewModel = viewModel)
            }
        }
    }
}

@Composable
private fun SettingsDialog(
    prefs: UserPreferences,
    onDismiss: () -> Unit,
    onExport: () -> Unit,
    scope: kotlinx.coroutines.CoroutineScope,
) {
    val context = LocalContext.current
    val monthly by prefs.monthlyBudget.collectAsStateWithLifecycle(initialValue = null)
    val appLockSaved by prefs.appLockEnabled.collectAsStateWithLifecycle(initialValue = false)
    val biometricSaved by prefs.biometricEnabled.collectAsStateWithLifecycle(initialValue = false)
    val themeSaved by prefs.themeMode.collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)
    val dynamicSaved by prefs.useDynamicColor.collectAsStateWithLifecycle(initialValue = false)

    var budgetText by remember { mutableStateOf("") }
    var lock by remember { mutableStateOf(false) }
    var biometric by remember { mutableStateOf(false) }
    var themePick by remember { mutableStateOf(ThemeMode.SYSTEM) }
    var dynamicPick by remember { mutableStateOf(false) }
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(monthly, appLockSaved, biometricSaved, themeSaved, dynamicSaved) {
        budgetText = monthly?.toString().orEmpty()
        lock = appLockSaved
        biometric = biometricSaved
        themePick = themeSaved
        dynamicPick = dynamicSaved
        error = null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_title)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                OutlinedTextField(
                    value = budgetText,
                    onValueChange = { budgetText = it; error = null },
                    label = { Text(stringResource(R.string.settings_monthly_budget)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = stringResource(R.string.settings_budget_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp),
                )

                Text(
                    text = stringResource(R.string.settings_theme),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = themePick == ThemeMode.SYSTEM,
                        onClick = { themePick = ThemeMode.SYSTEM },
                        label = { Text(stringResource(R.string.theme_system)) },
                    )
                    FilterChip(
                        selected = themePick == ThemeMode.LIGHT,
                        onClick = { themePick = ThemeMode.LIGHT },
                        label = { Text(stringResource(R.string.theme_light)) },
                    )
                    FilterChip(
                        selected = themePick == ThemeMode.DARK,
                        onClick = { themePick = ThemeMode.DARK },
                        label = { Text(stringResource(R.string.theme_dark)) },
                    )
                }
                RowSwitch(
                    label = stringResource(R.string.settings_dynamic_color),
                    checked = dynamicPick,
                    onCheckedChange = { dynamicPick = it },
                    modifier = Modifier.padding(top = 12.dp),
                )

                RowSwitch(
                    label = stringResource(R.string.settings_app_lock),
                    checked = lock,
                    onCheckedChange = { lock = it; error = null },
                )
                if (lock) {
                    OutlinedTextField(
                        value = newPin,
                        onValueChange = {
                            if (it.length <= 8 && it.all { d -> d.isDigit() }) newPin = it
                            error = null
                        },
                        label = {
                            Text(
                                stringResource(
                                    if (prefs.hasPin()) R.string.settings_new_pin_optional
                                    else R.string.settings_new_pin,
                                ),
                            )
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                    )
                    OutlinedTextField(
                        value = confirmPin,
                        onValueChange = {
                            if (it.length <= 8 && it.all { d -> d.isDigit() }) confirmPin = it
                            error = null
                        },
                        label = { Text(stringResource(R.string.settings_confirm_pin)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                RowSwitch(
                    label = stringResource(R.string.settings_biometric),
                    checked = biometric,
                    onCheckedChange = { biometric = it; error = null },
                    modifier = Modifier.padding(top = 12.dp),
                )

                error?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }

                Button(
                    onClick = onExport,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                ) {
                    Text(stringResource(R.string.action_export_csv))
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    scope.launch {
                        val budget = budgetText.toDoubleOrNull()
                        if (budgetText.isNotBlank() && (budget == null || budget < 0)) {
                            error = context.getString(R.string.error_budget)
                            return@launch
                        }
                        if (budgetText.isBlank()) {
                            prefs.setMonthlyBudget(null)
                        } else {
                            prefs.setMonthlyBudget(budget)
                        }
                        if (lock) {
                            val hasExisting = prefs.hasPin()
                            val wantsChange = newPin.isNotEmpty() || confirmPin.isNotEmpty()
                            if (!hasExisting && !wantsChange) {
                                error = context.getString(R.string.error_pin_required)
                                return@launch
                            }
                            if (wantsChange) {
                                if (newPin.length < 4) {
                                    error = context.getString(R.string.error_pin_short)
                                    return@launch
                                }
                                if (newPin != confirmPin) {
                                    error = context.getString(R.string.error_pin_mismatch)
                                    return@launch
                                }
                                prefs.savePin(newPin)
                            }
                            prefs.setAppLockEnabled(true)
                        } else {
                            prefs.setAppLockEnabled(false)
                            prefs.clearPin()
                        }
                        prefs.setBiometricEnabled(biometric && lock)
                        prefs.setThemeMode(themePick)
                        prefs.setUseDynamicColor(dynamicPick)
                        onDismiss()
                    }
                },
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

@Composable
private fun RowSwitch(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
