package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

@Database(
    entities = [
        TransactionEntity::class,
        BankAccountEntity::class,
        ContactEntity::class,
        RewardEntity::class,
        BillerEntity::class,
        NotificationEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun bankAccountDao(): BankAccountDao
    abstract fun contactDao(): ContactDao
    abstract fun rewardDao(): RewardDao
    abstract fun billerDao(): BillerDao
    abstract fun notificationDao(): NotificationDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "paypulse_database"
                )
                .fallbackToDestructiveMigration()
                .addCallback(DatabaseCallback())
                .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        populateDatabase(database)
                    }
                }
            }
        }

        suspend fun populateDatabase(db: AppDatabase) {
            // Seed Initial Bank Accounts
            val banks = listOf(
                BankAccountEntity("bank_hdfc", "HDFC Bank", "4821", "HDFC0001234", 48500.50, isDefault = true, primaryColorHex = "#004B87"),
                BankAccountEntity("bank_sbi", "State Bank of India", "9012", "SBIN0004567", 12350.00, isDefault = false, primaryColorHex = "#280071"),
                BankAccountEntity("bank_icici", "ICICI Bank", "3341", "ICIC0008910", 8740.25, isDefault = false, primaryColorHex = "#F37021")
            )
            db.bankAccountDao().insertBankAccounts(banks)

            // Seed Initial Contacts
            val contacts = listOf(
                ContactEntity("c1", "Rahul Sharma", "+91 98765 43210", "rahul.sharma@okaxis", "RS", "#0052FF", isFavorite = true),
                ContactEntity("c2", "Priya Verma", "+91 98123 45678", "priya.verma@paytm", "PV", "#00D084", isFavorite = true),
                ContactEntity("c3", "Aniket Das", "+91 99887 76655", "aniket.das@ybl", "AD", "#FFB703", isFavorite = true),
                ContactEntity("c4", "Sneha Patel", "+91 97112 23344", "sneha.p@icici", "SP", "#9C27B0", isFavorite = false),
                ContactEntity("c5", "Vikram Malhotra", "+91 96543 21098", "vikram.m@okicici", "VM", "#FF5722", isFavorite = false)
            )
            db.contactDao().insertContacts(contacts)

            // Seed Initial Billers
            val billers = listOf(
                BillerEntity("b1", "Jio Prepaid", "Mobile", "+91 98765 43210", 299.00, "Due in 3 days", "phone"),
                BillerEntity("b2", "Airtel Fiber", "Mobile", "ACC-891023", 999.00, "Due in 5 days", "wifi"),
                BillerEntity("b3", "Tata Play DTH", "DTH", "1029384756", 450.00, "Overdue", "tv"),
                BillerEntity("b4", "BESCOM Electricity", "Electricity", "584930201", 1420.00, "Due tomorrow", "flash")
            )
            db.billerDao().insertBillers(billers)

            // Seed Initial Rewards
            val rewards = listOf(
                RewardEntity("r1", "Flat ₹50 Cashback", "On your next mobile recharge", 50.0, isScratched = false),
                RewardEntity("r2", "Google Pay Scratch Card", "Won ₹25 for paying Swiggy", 25.0, isScratched = true),
                RewardEntity("r3", "Mystery UPI Gift", "Scratch to unlock rewards up to ₹500", 100.0, isScratched = false)
            )
            db.rewardDao().insertReward(rewards[0])
            db.rewardDao().insertReward(rewards[1])
            db.rewardDao().insertReward(rewards[2])

            // Seed Initial Transactions
            val currentTime = System.currentTimeMillis()
            val initialTxns = listOf(
                TransactionEntity(
                    id = "TXN_101",
                    title = "Rahul Sharma",
                    upiId = "rahul.sharma@okaxis",
                    amount = 1200.0,
                    type = "DEBIT",
                    status = "SUCCESS",
                    category = "Transfer",
                    timestamp = currentTime - 3600000,
                    bankAccountName = "HDFC Bank - 4821",
                    utrNumber = "420194821039",
                    note = "Dinner split",
                    isScratchCardEarned = true
                ),
                TransactionEntity(
                    id = "TXN_102",
                    title = "Swiggy Food",
                    upiId = "swiggy@icici",
                    amount = 385.0,
                    type = "DEBIT",
                    status = "SUCCESS",
                    category = "Food",
                    timestamp = currentTime - 86400000,
                    bankAccountName = "UPI Lite Wallet",
                    utrNumber = "420183719204",
                    note = "Lunch order via UPI Lite",
                    isScratchCardEarned = true
                ),
                TransactionEntity(
                    id = "TXN_103",
                    title = "Z Pay Wallet Top-up",
                    upiId = "wallet.topup@zpay",
                    amount = 500.0,
                    type = "CREDIT",
                    status = "SUCCESS",
                    category = "Wallet",
                    timestamp = currentTime - 120000000,
                    bankAccountName = "Z Pay Wallet",
                    utrNumber = "420177721092",
                    note = "Auto top-up via HDFC Bank",
                    isScratchCardEarned = false
                ),
                TransactionEntity(
                    id = "TXN_104",
                    title = "Priya Verma",
                    upiId = "priya.verma@paytm",
                    amount = 2500.0,
                    type = "CREDIT",
                    status = "SUCCESS",
                    category = "Transfer",
                    timestamp = currentTime - 172800000,
                    bankAccountName = "HDFC Bank - 4821",
                    utrNumber = "420172619401",
                    note = "Concert ticket refund",
                    isScratchCardEarned = false
                ),
                TransactionEntity(
                    id = "TXN_105",
                    title = "Airtel Fiber",
                    upiId = "airtel.fiber@icici",
                    amount = 999.0,
                    type = "DEBIT",
                    status = "SUCCESS",
                    category = "Bills",
                    timestamp = currentTime - 259200000,
                    bankAccountName = "HDFC Bank - 4821",
                    utrNumber = "420161829304",
                    note = "Monthly Broadband Bill",
                    isScratchCardEarned = true
                )
            )
            for (txn in initialTxns) {
                db.transactionDao().insertTransaction(txn)
            }

            // Seed Initial Notifications
            val initialNotifications = listOf(
                NotificationEntity(
                    id = "NOTIF_1",
                    title = "Payment Successful",
                    message = "Paid ₹1,200.00 to Rahul Sharma via UPI (UTR: 420194821039)",
                    type = "PAYMENT_SUCCESS",
                    timestamp = currentTime - 3600000,
                    isRead = false,
                    amount = 1200.00,
                    actionRoute = "DETAIL"
                ),
                NotificationEntity(
                    id = "NOTIF_2",
                    title = "⚡ Bill Payment Reminder",
                    message = "BESCOM Electricity bill of ₹1,420.00 is due tomorrow! Pay with 1-click via Z Pay.",
                    type = "BILL_REMINDER",
                    timestamp = currentTime - 7200000,
                    isRead = false,
                    amount = 1420.00,
                    actionRoute = "BILLS"
                ),
                NotificationEntity(
                    id = "NOTIF_3",
                    title = "📶 Broadband Renewal Due Soon",
                    message = "Airtel Fiber bill of ₹999.00 due in 5 days to avoid service interruption.",
                    type = "BILL_REMINDER",
                    timestamp = currentTime - 18000000,
                    isRead = true,
                    amount = 999.00,
                    actionRoute = "BILLS"
                )
            )
            for (notif in initialNotifications) {
                db.notificationDao().insertNotification(notif)
            }
        }
    }
}
