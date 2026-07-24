package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.BankAccountEntity
import com.example.data.model.BillerEntity
import com.example.data.model.ContactEntity
import com.example.data.model.NotificationEntity
import com.example.data.model.RewardEntity
import com.example.data.model.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    fun getTransactionById(id: String): Flow<TransactionEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)

    @Query("UPDATE transactions SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'DEBIT' AND status = 'SUCCESS'")
    fun getTotalDebits(): Flow<Double?>
}

@Dao
interface BankAccountDao {
    @Query("SELECT * FROM bank_accounts")
    fun getAllBankAccounts(): Flow<List<BankAccountEntity>>

    @Query("SELECT * FROM bank_accounts WHERE isDefault = 1 LIMIT 1")
    fun getDefaultBankAccount(): Flow<BankAccountEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBankAccounts(accounts: List<BankAccountEntity>)

    @Query("UPDATE bank_accounts SET balance = :newBalance WHERE id = :accountId")
    suspend fun updateBalance(accountId: String, newBalance: Double)

    @Query("UPDATE bank_accounts SET isDefault = CASE WHEN id = :accountId THEN 1 ELSE 0 END")
    suspend fun setDefaultAccount(accountId: String)
}

@Dao
interface ContactDao {
    @Query("SELECT * FROM contacts ORDER BY isFavorite DESC, name ASC")
    fun getAllContacts(): Flow<List<ContactEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContacts(contacts: List<ContactEntity>)
}

@Dao
interface RewardDao {
    @Query("SELECT * FROM rewards ORDER BY isScratched ASC")
    fun getAllRewards(): Flow<List<RewardEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReward(reward: RewardEntity)

    @Query("UPDATE rewards SET isScratched = 1 WHERE id = :id")
    suspend fun markScratched(id: String)
}

@Dao
interface BillerDao {
    @Query("SELECT * FROM billers")
    fun getAllBillers(): Flow<List<BillerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBillers(billers: List<BillerEntity>)
}

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications ORDER BY timestamp DESC")
    fun getAllNotifications(): Flow<List<NotificationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity)

    @Query("UPDATE notifications SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: String)

    @Query("UPDATE notifications SET isRead = 1")
    suspend fun markAllAsRead()

    @Query("DELETE FROM notifications")
    suspend fun clearAll()

    @Query("DELETE FROM notifications WHERE id = :id")
    suspend fun deleteById(id: String)
}
