package com.example.data.repository

import com.example.data.dao.BankAccountDao
import com.example.data.dao.BillerDao
import com.example.data.dao.ContactDao
import com.example.data.dao.NotificationDao
import com.example.data.dao.RewardDao
import com.example.data.dao.TransactionDao
import com.example.data.model.BankAccountEntity
import com.example.data.model.BillerEntity
import com.example.data.model.ContactEntity
import com.example.data.model.NotificationEntity
import com.example.data.model.RewardEntity
import com.example.data.model.TransactionEntity
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import java.util.UUID
import kotlin.random.Random

class PaymentRepository(
    private val transactionDao: TransactionDao,
    private val bankAccountDao: BankAccountDao,
    private val contactDao: ContactDao,
    private val rewardDao: RewardDao,
    private val billerDao: BillerDao,
    private val notificationDao: NotificationDao
) {
    val allTransactions: Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()
    val allBankAccounts: Flow<List<BankAccountEntity>> = bankAccountDao.getAllBankAccounts()
    val allContacts: Flow<List<ContactEntity>> = contactDao.getAllContacts()
    val allRewards: Flow<List<RewardEntity>> = rewardDao.getAllRewards()
    val allBillers: Flow<List<BillerEntity>> = billerDao.getAllBillers()
    val allNotifications: Flow<List<NotificationEntity>> = notificationDao.getAllNotifications()

    fun getTransactionById(id: String): Flow<TransactionEntity?> = transactionDao.getTransactionById(id)

    suspend fun scratchReward(rewardId: String) {
        rewardDao.markScratched(rewardId)
    }

    suspend fun setDefaultBank(accountId: String) {
        bankAccountDao.setDefaultAccount(accountId)
    }

    suspend fun markNotificationAsRead(id: String) {
        notificationDao.markAsRead(id)
    }

    suspend fun markAllNotificationsAsRead() {
        notificationDao.markAllAsRead()
    }

    suspend fun clearAllNotifications() {
        notificationDao.clearAll()
    }

    suspend fun addNotification(
        title: String,
        message: String,
        type: String,
        amount: Double? = null,
        actionRoute: String? = null
    ): NotificationEntity {
        val notif = NotificationEntity(
            id = "NOTIF_" + System.currentTimeMillis().toString().takeLast(8),
            title = title,
            message = message,
            type = type,
            timestamp = System.currentTimeMillis(),
            isRead = false,
            amount = amount,
            actionRoute = actionRoute
        )
        notificationDao.insertNotification(notif)
        return notif
    }

    suspend fun processRealtimeUpiPayment(
        recipientTitle: String,
        upiId: String,
        amount: Double,
        category: String,
        note: String,
        selectedBankAccountId: String,
        onStatusChange: suspend (String, TransactionEntity) -> Unit
    ): TransactionEntity {
        val txnId = "TXN_" + System.currentTimeMillis().toString().takeLast(8) + Random.nextInt(100, 999)
        val utr = "4" + Random.nextLong(10000000001L, 99999999999L).toString()
        
        // Find bank name
        val banks = bankAccountDao.getAllBankAccounts().first()
        val bank = banks.find { it.id == selectedBankAccountId } ?: banks.firstOrNull()
        val bankName = if (bank != null) "${bank.bankName} - ${bank.accountNumberLast4}" else "HDFC Bank - 4821"

        var txn = TransactionEntity(
            id = txnId,
            title = recipientTitle,
            upiId = upiId,
            amount = amount,
            type = "DEBIT",
            status = "PENDING",
            category = category,
            timestamp = System.currentTimeMillis(),
            bankAccountName = bankName,
            utrNumber = utr,
            note = note,
            isScratchCardEarned = false
        )

        // 1. Initialized
        transactionDao.insertTransaction(txn)
        onStatusChange("PENDING", txn)
        delay(1200)

        // 2. Processing with NPCI
        txn = txn.copy(status = "PROCESSING")
        transactionDao.updateStatus(txnId, "PROCESSING")
        onStatusChange("PROCESSING", txn)
        delay(1600)

        // Deduct balance
        if (bank != null) {
            val newBalance = (bank.balance - amount).coerceAtLeast(0.0)
            bankAccountDao.updateBalance(bank.id, newBalance)
        }

        // Earn reward if amount >= 100
        val earnedReward = amount >= 100.0
        if (earnedReward) {
            val cashbackAmount = Random.nextInt(5, 75).toDouble()
            val reward = RewardEntity(
                id = "RWD_" + System.currentTimeMillis().toString().takeLast(6),
                title = "Lucky Payment Scratch Card",
                description = "Earned on payment to $recipientTitle",
                rewardAmount = cashbackAmount,
                isScratched = false
            )
            rewardDao.insertReward(reward)
        }

        // 3. Success
        txn = txn.copy(status = "SUCCESS", isScratchCardEarned = earnedReward)
        transactionDao.updateStatus(txnId, "SUCCESS")
        transactionDao.insertTransaction(txn)

        // Insert payment success alert notification
        val formattedAmount = String.format("%.2f", amount)
        addNotification(
            title = "Payment Successful",
            message = "Paid ₹$formattedAmount to $recipientTitle via UPI (UTR: $utr)",
            type = "PAYMENT_SUCCESS",
            amount = amount,
            actionRoute = "DETAIL"
        )

        onStatusChange("SUCCESS", txn)

        return txn
    }
}
