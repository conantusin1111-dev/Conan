package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.BankAccountEntity
import com.example.data.model.BillerEntity
import com.example.data.model.ContactEntity
import com.example.data.model.NotificationEntity
import com.example.data.model.RewardEntity
import com.example.data.model.TransactionEntity
import com.example.data.repository.PaymentRepository
import com.example.util.NotificationHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PaymentUiState(
    val transactions: List<TransactionEntity> = emptyList(),
    val bankAccounts: List<BankAccountEntity> = emptyList(),
    val contacts: List<ContactEntity> = emptyList(),
    val rewards: List<RewardEntity> = emptyList(),
    val billers: List<BillerEntity> = emptyList(),
    val notifications: List<NotificationEntity> = emptyList(),
    val unreadNotificationCount: Int = 0,
    val activePopupNotification: NotificationEntity? = null,
    val upiLiteBalance: Double = 850.0,
    val selectedBankId: String = "bank_hdfc",
    val activeTransaction: TransactionEntity? = null,
    val activeTrackingStep: String = "IDLE", // "IDLE", "INITIATED", "BANK_PROCESSING", "NPCI_SWITCHING", "SUCCESS"
    val isProcessingPayment: Boolean = false,
    val searchQuery: String = "",
    val filterCategory: String = "ALL",
    val upiPin: String = "",
    val isPinError: Boolean = false,
    val totalCashbackEarned: Double = 75.0
)

class PaymentViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val repository = PaymentRepository(
        db.transactionDao(),
        db.bankAccountDao(),
        db.contactDao(),
        db.rewardDao(),
        db.billerDao(),
        db.notificationDao()
    )

    private val _upiLiteBalance = MutableStateFlow(850.0)
    private val _selectedBankId = MutableStateFlow("bank_hdfc")
    private val _activeTransaction = MutableStateFlow<TransactionEntity?>(null)
    private val _activeTrackingStep = MutableStateFlow("IDLE")
    private val _isProcessingPayment = MutableStateFlow(false)
    private val _searchQuery = MutableStateFlow("")
    private val _filterCategory = MutableStateFlow("ALL")
    private val _upiPin = MutableStateFlow("")
    private val _isPinError = MutableStateFlow(false)
    private val _activePopupNotification = MutableStateFlow<NotificationEntity?>(null)

    val uiState: StateFlow<PaymentUiState> = combine(
        repository.allTransactions,
        repository.allBankAccounts,
        repository.allContacts,
        repository.allRewards,
        repository.allBillers,
        repository.allNotifications,
        _upiLiteBalance,
        _selectedBankId,
        _activeTransaction,
        _activeTrackingStep,
        _isProcessingPayment,
        _searchQuery,
        _filterCategory,
        _upiPin,
        _isPinError,
        _activePopupNotification
    ) { args ->
        val txns = args[0] as List<TransactionEntity>
        val banks = args[1] as List<BankAccountEntity>
        val contacts = args[2] as List<ContactEntity>
        val rewards = args[3] as List<RewardEntity>
        val billers = args[4] as List<BillerEntity>
        val notifs = args[5] as List<NotificationEntity>
        val liteBal = args[6] as Double
        val bankId = args[7] as String
        val activeTxn = args[8] as TransactionEntity?
        val step = args[9] as String
        val isProc = args[10] as Boolean
        val search = args[11] as String
        val filter = args[12] as String
        val pin = args[13] as String
        val pinErr = args[14] as Boolean
        val popupNotif = args[15] as NotificationEntity?

        val unreadCount = notifs.count { !it.isRead }
        val totalCashback = rewards.filter { it.isScratched }.sumOf { it.rewardAmount }

        val filteredTxns = txns.filter { txn ->
            val matchesSearch = search.isEmpty() ||
                    txn.title.contains(search, ignoreCase = true) ||
                    txn.upiId.contains(search, ignoreCase = true) ||
                    txn.utrNumber.contains(search, ignoreCase = true)
            val matchesFilter = when (filter) {
                "ALL" -> true
                "DEBIT" -> txn.type == "DEBIT"
                "CREDIT" -> txn.type == "CREDIT"
                else -> txn.category.equals(filter, ignoreCase = true)
            }
            matchesSearch && matchesFilter
        }

        PaymentUiState(
            transactions = filteredTxns,
            bankAccounts = banks,
            contacts = contacts,
            rewards = rewards,
            billers = billers,
            notifications = notifs,
            unreadNotificationCount = unreadCount,
            activePopupNotification = popupNotif,
            upiLiteBalance = liteBal,
            selectedBankId = bankId,
            activeTransaction = activeTxn,
            activeTrackingStep = step,
            isProcessingPayment = isProc,
            searchQuery = search,
            filterCategory = filter,
            upiPin = pin,
            isPinError = pinErr,
            totalCashbackEarned = totalCashback
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PaymentUiState()
    )

    fun setSelectedBank(bankId: String) {
        _selectedBankId.value = bankId
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilterCategory(category: String) {
        _filterCategory.value = category
    }

    fun updateUpiPin(pin: String) {
        if (pin.length <= 6) {
            _upiPin.value = pin
            _isPinError.value = false
        }
    }

    fun clearUpiPin() {
        _upiPin.value = ""
        _isPinError.value = false
    }

    fun dismissPopupNotification() {
        _activePopupNotification.value = null
    }

    fun markNotificationRead(id: String) {
        viewModelScope.launch {
            repository.markNotificationAsRead(id)
        }
    }

    fun markAllNotificationsRead() {
        viewModelScope.launch {
            repository.markAllNotificationsAsRead()
        }
    }

    fun clearAllNotifications() {
        viewModelScope.launch {
            repository.clearAllNotifications()
        }
    }

    fun simulatePaymentSuccessAlert(
        recipientName: String = "Merchant / Contact",
        amount: Double = 450.00
    ) {
        viewModelScope.launch {
            val title = "Payment Successful"
            val message = "Paid ₹${String.format("%.2f", amount)} to $recipientName via UPI"
            val notif = repository.addNotification(
                title = title,
                message = message,
                type = "PAYMENT_SUCCESS",
                amount = amount,
                actionRoute = "DETAIL"
            )
            triggerPopupAndSystemNotification(notif)
        }
    }

    fun simulateBillReminderNotification(
        billerName: String = "BESCOM Electricity",
        amount: Double = 1420.00,
        dueDate: String = "Due Tomorrow"
    ) {
        viewModelScope.launch {
            val title = "⚡ Bill Payment Reminder"
            val message = "$billerName bill of ₹${String.format("%.2f", amount)} is $dueDate. Tap to pay instantly."
            val notif = repository.addNotification(
                title = title,
                message = message,
                type = "BILL_REMINDER",
                amount = amount,
                actionRoute = "BILLS"
            )
            triggerPopupAndSystemNotification(notif)
        }
    }

    private fun triggerPopupAndSystemNotification(notification: NotificationEntity) {
        _activePopupNotification.value = notification
        NotificationHelper.showSystemNotification(
            context = getApplication(),
            title = notification.title,
            message = notification.message
        )
        viewModelScope.launch {
            delay(4500)
            if (_activePopupNotification.value?.id == notification.id) {
                _activePopupNotification.value = null
            }
        }
    }

    fun scratchReward(rewardId: String) {
        viewModelScope.launch {
            repository.scratchReward(rewardId)
        }
    }

    fun setDefaultBank(bankId: String) {
        viewModelScope.launch {
            repository.setDefaultBank(bankId)
            _selectedBankId.value = bankId
        }
    }

    fun topUpUpiLite(amount: Double) {
        _upiLiteBalance.value += amount
    }

    fun processPayment(
        recipientTitle: String,
        upiId: String,
        amount: Double,
        category: String,
        note: String,
        enteredPin: String,
        onSuccess: (String) -> Unit
    ) {
        if (enteredPin.length < 4) {
            _isPinError.value = true
            return
        }

        viewModelScope.launch {
            _isProcessingPayment.value = true
            _activeTrackingStep.value = "INITIATED"

            val txn = repository.processRealtimeUpiPayment(
                recipientTitle = recipientTitle,
                upiId = upiId,
                amount = amount,
                category = category,
                note = note,
                selectedBankAccountId = _selectedBankId.value
            ) { stepStatus, currentTxn ->
                _activeTransaction.value = currentTxn
                _activeTrackingStep.value = when (stepStatus) {
                    "PENDING" -> "INITIATED"
                    "PROCESSING" -> "BANK_PROCESSING"
                    "SUCCESS" -> "SUCCESS"
                    else -> "FAILED"
                }
            }

            _isProcessingPayment.value = false
            _upiPin.value = ""

            // Trigger popup notification banner for completed payment
            val successNotif = NotificationEntity(
                id = "PAY_" + txn.id,
                title = "Payment Successful",
                message = "Paid ₹${String.format("%.2f", amount)} to $recipientTitle via UPI",
                type = "PAYMENT_SUCCESS",
                amount = amount,
                actionRoute = "DETAIL"
            )
            triggerPopupAndSystemNotification(successNotif)

            onSuccess(txn.id)
        }
    }

    fun resetActiveTransaction() {
        _activeTransaction.value = null
        _activeTrackingStep.value = "IDLE"
    }
}

