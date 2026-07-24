package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val id: String,
    val title: String,
    val upiId: String,
    val amount: Double,
    val type: String, // "DEBIT" or "CREDIT"
    val status: String, // "PENDING", "PROCESSING", "SUCCESS", "FAILED"
    val category: String, // "Food", "Shopping", "Transfer", "Bills", "Self Transfer", "Recharge"
    val timestamp: Long = System.currentTimeMillis(),
    val bankAccountName: String,
    val utrNumber: String,
    val note: String = "",
    val isScratchCardEarned: Boolean = false
)

@Entity(tableName = "bank_accounts")
data class BankAccountEntity(
    @PrimaryKey val id: String,
    val bankName: String,
    val accountNumberLast4: String,
    val ifsc: String,
    var balance: Double,
    val isDefault: Boolean = false,
    val accountType: String = "Savings",
    val primaryColorHex: String = "#0052FF"
)

@Entity(tableName = "contacts")
data class ContactEntity(
    @PrimaryKey val id: String,
    val name: String,
    val phone: String,
    val upiId: String,
    val avatarInitials: String,
    val avatarBgHex: String,
    val isFavorite: Boolean = false
)

@Entity(tableName = "rewards")
data class RewardEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val rewardAmount: Double,
    var isScratched: Boolean = false,
    val expiryDate: String = "31 Dec 2026"
)

@Entity(tableName = "billers")
data class BillerEntity(
    @PrimaryKey val id: String,
    val name: String,
    val category: String, // "Mobile", "Electricity", "DTH", "Credit Card", "Gas", "Water"
    val accountNumber: String,
    val dueAmount: Double,
    val dueDate: String,
    val iconName: String
)

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val message: String,
    val type: String, // "PAYMENT_SUCCESS", "BILL_REMINDER", "SECURITY_ALERT", "CASHBACK"
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val amount: Double? = null,
    val actionRoute: String? = null
)
