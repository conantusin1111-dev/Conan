package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.ContactEntity
import com.example.data.model.TransactionEntity
import com.example.ui.PaymentUiState
import com.example.ui.components.QuickActionsGrid
import com.example.ui.components.RecentContactsCarousel
import com.example.ui.components.TransactionItemCard
import com.example.ui.components.UpiHeaderBar
import com.example.ui.theme.PayBluePrimary
import com.example.ui.theme.PayGoldReward
import com.example.ui.theme.PayTealAccent

@Composable
fun HomeScreen(
    uiState: PaymentUiState,
    onSearchChange: (String) -> Unit,
    onScanQrClick: () -> Unit,
    onPayContactClick: (ContactEntity?) -> Unit,
    onPayUpiIdClick: () -> Unit,
    onCheckBalanceClick: () -> Unit,
    onMobileRechargeClick: () -> Unit,
    onBillPaymentsClick: () -> Unit = {},
    onNotificationClick: () -> Unit = {},
    onLockAppClick: () -> Unit = {},
    onRewardsClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onTxnClick: (TransactionEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            item {
                UpiHeaderBar(
                    userName = "Alex Morgan",
                    upiId = "alex.morgan@okaxis",
                    upiLiteBalance = uiState.upiLiteBalance,
                    unreadNotificationCount = uiState.unreadNotificationCount,
                    onProfileClick = onCheckBalanceClick,
                    onUpiLiteClick = onCheckBalanceClick,
                    onNotificationClick = onNotificationClick,
                    onLockClick = onLockAppClick,
                    onSearchChange = onSearchChange,
                    searchQuery = uiState.searchQuery
                )
            }

            // Quick Balance View
            item {
                com.example.ui.components.QuickBalanceCard(
                    bankAccounts = uiState.bankAccounts,
                    onCheckBalanceClick = onCheckBalanceClick
                )
            }

            // Quick Actions Grid
            item {
                QuickActionsGrid(
                    onScanQr = onScanQrClick,
                    onPayContact = { onPayContactClick(null) },
                    onPayUpiId = onPayUpiIdClick,
                    onSelfTransfer = onCheckBalanceClick,
                    onCheckBalance = onCheckBalanceClick,
                    onMobileRecharge = onMobileRechargeClick
                )
            }

            // Recent Contacts
            item {
                RecentContactsCarousel(
                    contacts = uiState.contacts,
                    onSelectContact = { onPayContactClick(it) }
                )
            }

            // Promo Cashback Hero Banner
            item {
                PromoCashbackBanner(
                    cashbackEarned = uiState.totalCashbackEarned,
                    onRewardsClick = onRewardsClick
                )
            }

            // Bills & Utilities Grid
            item {
                BillUtilitiesGrid(
                    onRechargeClick = onMobileRechargeClick,
                    onBillsClick = onBillPaymentsClick
                )
            }

            // Recent Transactions Section Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Activity",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onHistoryClick() }
                    ) {
                        Text(
                            text = "Full History",
                            style = MaterialTheme.typography.bodyMedium,
                            color = PayBluePrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            Icons.Default.History,
                            contentDescription = "History",
                            tint = PayBluePrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Transaction items
            val recentTxns = uiState.transactions.take(5)
            if (recentTxns.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No transactions found matching search.",
                                color = Color.Gray,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            } else {
                items(recentTxns, key = { it.id }) { txn ->
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        TransactionItemCard(
                            transaction = txn,
                            onClick = { onTxnClick(txn) }
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun PromoCashbackBanner(
    cashbackEarned: Double,
    onRewardsClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable { onRewardsClick() }
            .testTag("banner_rewards"),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF0F172A),
                            Color(0xFF1E293B),
                            Color(0xFF0052FF)
                        )
                    )
                )
        ) {
            // Draw promo banner image if available
            Image(
                painter = painterResource(id = R.drawable.img_promo_banner_1784899564548),
                contentDescription = "Promo Banner",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                contentScale = ContentScale.Crop,
                alpha = 0.35f
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Surface(
                        color = PayGoldReward,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "REWARDS & CASHBACK",
                            color = Color.Black,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Total Earned: ₹${cashbackEarned.toInt()}",
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Scratch cards available! Pay with UPI to win more.",
                        color = Color.LightGray,
                        fontSize = 12.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(PayGoldReward, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Rewards",
                        tint = Color.Black,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun BillUtilitiesGrid(
    onRechargeClick: () -> Unit,
    onBillsClick: () -> Unit = onRechargeClick
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Bills & Recharge",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "View All",
                    fontSize = 12.sp,
                    color = PayBluePrimary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onBillsClick() }
                )
            }
            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                BillCategoryItem(
                    icon = Icons.Default.PhoneAndroid,
                    label = "Mobile",
                    tag = "bill_cat_mobile",
                    onClick = onBillsClick
                )
                BillCategoryItem(
                    icon = Icons.Default.ElectricBolt,
                    label = "Electricity",
                    tag = "bill_cat_electricity",
                    onClick = onBillsClick
                )
                BillCategoryItem(
                    icon = Icons.Default.Tv,
                    label = "DTH",
                    tag = "bill_cat_dth",
                    onClick = onBillsClick
                )
                BillCategoryItem(
                    icon = Icons.Default.WaterDrop,
                    label = "Water",
                    tag = "bill_cat_water",
                    onClick = onBillsClick
                )
            }
        }
    }
}

@Composable
fun BillCategoryItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tag: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(72.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 4.dp)
            .testTag(tag)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(Color(0xFFF1F5F9), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = PayBluePrimary,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
