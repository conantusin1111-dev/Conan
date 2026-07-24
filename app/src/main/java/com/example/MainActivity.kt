package com.example

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.auth.BiometricAuthManager
import com.example.data.model.TransactionEntity
import com.example.ui.PaymentViewModel
import com.example.ui.components.BiometricLockScreen
import com.example.ui.components.NotificationCenterSheet
import com.example.ui.components.NotificationHeadsUpBanner
import com.example.ui.components.UpiPinDialog
import com.example.ui.screens.BankBalanceSheet
import com.example.ui.screens.BillPaymentsScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.PaymentEntrySheet
import com.example.ui.screens.RewardsScratchSheet
import com.example.ui.screens.RewardsScreen
import com.example.ui.screens.ScanQrDialog
import com.example.ui.screens.TransactionDetailScreen
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {

    private lateinit var authManager: BiometricAuthManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        authManager = BiometricAuthManager(applicationContext)

        setContent {
            MyApplicationTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PaymentApp(
                        activity = this,
                        authManager = authManager
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentApp(
    activity: FragmentActivity,
    authManager: BiometricAuthManager,
    viewModel: PaymentViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val authUiState by authManager.uiState.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()

    var currentScreen by remember { mutableStateOf("HOME") } // "HOME", "HISTORY", "DETAIL", "REWARDS", "BILLS"
    var selectedTxn by remember { mutableStateOf<TransactionEntity?>(null) }

    // Dialog & Sheet States
    var showScanQr by remember { mutableStateOf(false) }
    var showPaymentEntry by remember { mutableStateOf(false) }
    var showBankBalance by remember { mutableStateOf(false) }
    var showPinDialog by remember { mutableStateOf(false) }
    var showRewardsSheet by remember { mutableStateOf(false) }
    var showNotificationSheet by remember { mutableStateOf(false) }

    // Active Payment Input State
    var activeRecipientTitle by remember { mutableStateOf("") }
    var activeUpiId by remember { mutableStateOf("") }
    var activeAmount by remember { mutableStateOf(0.0) }
    var activeCategory by remember { mutableStateOf("Transfer") }
    var activeNote by remember { mutableStateOf("") }

    if (!authUiState.isAuthenticated) {
        BiometricLockScreen(
            biometricStatus = authManager.checkBiometricStatus(),
            authError = authUiState.authError,
            isAuthenticating = authUiState.isAuthenticating,
            onUnlockWithBiometrics = {
                authManager.authenticateWithBiometrics(
                    activity = activity,
                    onSuccess = {},
                    onError = {}
                )
            },
            onUnlockWithCredentials = {
                coroutineScope.launch {
                    authManager.authenticateWithCredentials(
                        activity = activity,
                        onSuccess = {},
                        onError = {}
                    )
                }
            },
            onBypassForDemo = {
                authManager.bypassAuthForDemo()
            }
        )
    } else {
        androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize()) {
            Crossfade(targetState = currentScreen, label = "ScreenTransition") { screen ->
                when (screen) {
                    "HOME" -> {
                        HomeScreen(
                            uiState = uiState,
                            onSearchChange = { viewModel.setSearchQuery(it) },
                            onScanQrClick = { showScanQr = true },
                            onPayContactClick = { contact ->
                                if (contact != null) {
                                    activeRecipientTitle = contact.name
                                    activeUpiId = contact.upiId
                                } else {
                                    activeRecipientTitle = "Mobile Contact"
                                    activeUpiId = "contact@upi"
                                }
                                showPaymentEntry = true
                            },
                            onPayUpiIdClick = {
                                activeRecipientTitle = "UPI Recipient"
                                activeUpiId = "pay.user@upi"
                                showPaymentEntry = true
                            },
                            onCheckBalanceClick = { showBankBalance = true },
                            onMobileRechargeClick = {
                                activeRecipientTitle = "Airtel Prepaid Recharge"
                                activeUpiId = "airtel.recharge@icici"
                                activeCategory = "Recharge"
                                showPaymentEntry = true
                            },
                            onBillPaymentsClick = { currentScreen = "BILLS" },
                            onNotificationClick = { showNotificationSheet = true },
                            onLockAppClick = { authManager.lockApp() },
                            onRewardsClick = { currentScreen = "REWARDS" },
                            onHistoryClick = { currentScreen = "HISTORY" },
                            onTxnClick = { txn ->
                                selectedTxn = txn
                                currentScreen = "DETAIL"
                            }
                        )
                    }

                    "HISTORY" -> {
                        HistoryScreen(
                            uiState = uiState,
                            onBackClick = { currentScreen = "HOME" },
                            onSearchChange = { viewModel.setSearchQuery(it) },
                            onFilterSelect = { viewModel.setFilterCategory(it) },
                            onTxnClick = { txn ->
                                selectedTxn = txn
                                currentScreen = "DETAIL"
                            }
                        )
                    }

                    "REWARDS" -> {
                        RewardsScreen(
                            uiState = uiState,
                            onBackClick = { currentScreen = "HOME" },
                            onScratchReward = { rewardId ->
                                viewModel.scratchReward(rewardId)
                            }
                        )
                    }

                    "BILLS" -> {
                        BillPaymentsScreen(
                            onBackClick = { currentScreen = "HOME" },
                            onInitiateBillPayment = { title, upiId, amount, category, note ->
                                activeRecipientTitle = title
                                activeUpiId = upiId
                                activeAmount = amount
                                activeCategory = category
                                activeNote = note
                                showPinDialog = true
                            }
                        )
                    }

                    "DETAIL" -> {
                        val txn = selectedTxn ?: uiState.activeTransaction
                        if (txn != null) {
                            TransactionDetailScreen(
                                transaction = txn,
                                trackingStep = if (uiState.activeTransaction?.id == txn.id) uiState.activeTrackingStep else "SUCCESS",
                                onBackClick = {
                                    viewModel.resetActiveTransaction()
                                    currentScreen = "HOME"
                                },
                                onClaimRewardClick = { currentScreen = "REWARDS" }
                            )
                        } else {
                            currentScreen = "HOME"
                        }
                    }
                }
            }

            // Top Dropdown Heads-Up Notification Banner
            NotificationHeadsUpBanner(
                notification = uiState.activePopupNotification,
                onDismiss = { viewModel.dismissPopupNotification() },
                onActionClick = { route ->
                    when (route) {
                        "BILLS" -> currentScreen = "BILLS"
                        "DETAIL" -> {
                            if (uiState.transactions.isNotEmpty()) {
                                selectedTxn = uiState.transactions.first()
                                currentScreen = "DETAIL"
                            }
                        }
                    }
                }
            )
        }

        // Modal Sheets and Dialogs

        if (showNotificationSheet) {
            NotificationCenterSheet(
                notifications = uiState.notifications,
                onDismiss = { showNotificationSheet = false },
                onMarkAsRead = { viewModel.markNotificationRead(it) },
                onMarkAllAsRead = { viewModel.markAllNotificationsRead() },
                onClearAll = { viewModel.clearAllNotifications() },
                onSimulatePaymentSuccess = { viewModel.simulatePaymentSuccessAlert() },
                onSimulateBillReminder = { viewModel.simulateBillReminderNotification() },
                onNotificationClick = { notif ->
                    when (notif.actionRoute) {
                        "BILLS" -> {
                            showNotificationSheet = false
                            currentScreen = "BILLS"
                        }
                        "DETAIL" -> {
                            showNotificationSheet = false
                            if (uiState.transactions.isNotEmpty()) {
                                selectedTxn = uiState.transactions.first()
                                currentScreen = "DETAIL"
                            }
                        }
                    }
                }
            )
        }

    // Modal Sheets and Dialogs

    if (showScanQr) {
        ScanQrDialog(
            onDismiss = { showScanQr = false },
            onQrScanned = { merchantName, merchantUpi ->
                showScanQr = false
                activeRecipientTitle = merchantName
                activeUpiId = merchantUpi
                showPaymentEntry = true
            }
        )
    }

    if (showPaymentEntry) {
        PaymentEntrySheet(
            recipientTitle = activeRecipientTitle,
            upiId = activeUpiId,
            bankAccounts = uiState.bankAccounts,
            selectedBankId = uiState.selectedBankId,
            onBankSelect = { viewModel.setSelectedBank(it) },
            onDismiss = { showPaymentEntry = false },
            onProceedToPin = { amount, category, note ->
                activeAmount = amount
                activeCategory = category
                activeNote = note
                showPaymentEntry = false
                showPinDialog = true
            }
        )
    }

    if (showBankBalance) {
        BankBalanceSheet(
            bankAccounts = uiState.bankAccounts,
            upiLiteBalance = uiState.upiLiteBalance,
            onTopUpUpiLite = { viewModel.topUpUpiLite(it) },
            onDismiss = { showBankBalance = false }
        )
    }

    if (showPinDialog) {
        UpiPinDialog(
            recipientTitle = activeRecipientTitle,
            upiId = activeUpiId,
            amount = activeAmount,
            bankAccounts = uiState.bankAccounts,
            selectedBankId = uiState.selectedBankId,
            currentPin = uiState.upiPin,
            isPinError = uiState.isPinError,
            onPinChange = { viewModel.updateUpiPin(it) },
            onBankSelect = { viewModel.setSelectedBank(it) },
            onDismiss = {
                viewModel.clearUpiPin()
                showPinDialog = false
            },
            onConfirm = { enteredPin ->
                viewModel.processPayment(
                    recipientTitle = activeRecipientTitle,
                    upiId = activeUpiId,
                    amount = activeAmount,
                    category = activeCategory,
                    note = activeNote,
                    enteredPin = enteredPin,
                    onSuccess = { txnId ->
                        showPinDialog = false
                        viewModel.clearUpiPin()
                        currentScreen = "DETAIL"
                    }
                )
            }
        )
    }

    if (showRewardsSheet) {
        RewardsScratchSheet(
            rewards = uiState.rewards,
            totalCashback = uiState.totalCashbackEarned,
            onScratch = { viewModel.scratchReward(it) },
            onDismiss = { showRewardsSheet = false }
        )
    }
    }
}

