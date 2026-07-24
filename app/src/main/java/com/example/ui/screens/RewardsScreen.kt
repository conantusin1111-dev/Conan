package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.RewardEntity
import com.example.ui.PaymentUiState
import com.example.ui.theme.PayBluePrimary
import com.example.ui.theme.PayGoldReward
import com.example.ui.theme.PayTealAccent

data class MerchantOffer(
    val id: String,
    val brandName: String,
    val offerTitle: String,
    val promoCode: String,
    val category: String,
    val icon: ImageVector,
    val bgColor: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RewardsScreen(
    uiState: PaymentUiState,
    onBackClick: () -> Unit,
    onScratchReward: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    var copiedCodeId by remember { mutableStateOf<String?>(null) }
    var selectedOfferCategory by remember { mutableStateOf("All") }

    val merchantOffers = listOf(
        MerchantOffer("off_1", "Starbucks", "Get Flat 20% Cashback on UPI", "STARBUCKS20", "Food", Icons.Default.Fastfood, Color(0xFF006241)),
        MerchantOffer("off_2", "Zomato Gourmet", "Flat ₹100 Off on orders above ₹399", "ZOMATO100", "Food", Icons.Default.Fastfood, Color(0xFFE23744)),
        MerchantOffer("off_3", "Swiggy Instamart", "Flat ₹75 Cashback on Grocery", "INSTA75", "Shopping", Icons.Default.ShoppingBag, Color(0xFFFC8019)),
        MerchantOffer("off_4", "Uber Rides", "Win up to ₹150 Cashback on 3 Rides", "UBERUPI", "Travel", Icons.Default.TwoWheeler, Color(0xFF000000)),
        MerchantOffer("off_5", "Amazon Pay", "Flat ₹50 Off on Utility Bill Payments", "AMZBILL50", "Bills", Icons.Default.MonetizationOn, Color(0xFFFF9900))
    )

    val filteredOffers = if (selectedOfferCategory == "All") {
        merchantOffers
    } else {
        merchantOffers.filter { it.category.equals(selectedOfferCategory, ignoreCase = true) }
    }

    val unscratchedRewards = uiState.rewards.filter { !it.isScratched }
    val scratchedRewards = uiState.rewards.filter { it.isScratched }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Rewards & Offers Dashboard",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 19.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Surface(
                        color = PayGoldReward.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = "Points",
                                tint = PayGoldReward,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "1,850 PTS",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Summary Dashboard Hero Card
            item {
                RewardsHeaderHeroCard(
                    totalCashback = uiState.totalCashbackEarned,
                    availableScratchCards = unscratchedRewards.size
                )
            }

            // Scratch Cards Section
            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Your Scratch Cards",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${unscratchedRewards.size} Available",
                            fontSize = 12.sp,
                            color = PayBluePrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (uiState.rewards.isEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Make transactions to earn scratch cards!", color = Color.Gray, fontSize = 13.sp)
                            }
                        }
                    } else {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(uiState.rewards, key = { it.id }) { reward ->
                                ScratchCardItem(
                                    reward = reward,
                                    onScratch = { onScratchReward(reward.id) }
                                )
                            }
                        }
                    }
                }
            }

            // Cashback History Section
            item {
                Column {
                    Text(
                        text = "Cashback History",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (scratchedRewards.isEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No cashback claimed yet. Tap on scratch cards above to unlock rewards!",
                                    color = Color.Gray,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    } else {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                scratchedRewards.forEachIndexed { index, reward ->
                                    CashbackHistoryRow(reward = reward)
                                    if (index < scratchedRewards.size - 1) {
                                        androidx.compose.material3.Divider(
                                            modifier = Modifier.padding(vertical = 8.dp),
                                            color = Color(0xFFF1F5F9)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Exclusive Partner Offers Section
            item {
                Column {
                    Text(
                        text = "Exclusive Partner Offers",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    // Offer Filter Pills
                    val offerCategories = listOf("All", "Food", "Shopping", "Travel", "Bills")
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        items(offerCategories) { cat ->
                            val isSelected = cat == selectedOfferCategory
                            Surface(
                                color = if (isSelected) PayBluePrimary else Color(0xFFE2E8F0),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.clickable { selectedOfferCategory = cat }
                            ) {
                                Text(
                                    text = cat,
                                    color = if (isSelected) Color.White else Color.DarkGray,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        filteredOffers.forEach { offer ->
                            val isCopied = copiedCodeId == offer.id
                            MerchantOfferCard(
                                offer = offer,
                                isCopied = isCopied,
                                onCopyCode = {
                                    clipboardManager.setText(AnnotatedString(offer.promoCode))
                                    copiedCodeId = offer.id
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RewardsHeaderHeroCard(
    totalCashback: Double,
    availableScratchCards: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .testTag("rewards_hero_card"),
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
                            Color(0xFF2563EB)
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Surface(
                            color = PayGoldReward,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "PAYPULSE PERKS",
                                color = Color.Black,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Total Cashback Won",
                            color = Color.LightGray,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "₹${totalCashback.toInt()}",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 32.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .background(PayGoldReward, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CardGiftcard,
                            contentDescription = "Rewards",
                            tint = Color.Black,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(PayTealAccent, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "$availableScratchCards Scratch Cards Unlocked",
                            color = PayTealAccent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Surface(
                        color = Color.White.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.clickable { }
                    ) {
                        Text(
                            text = "Redeem Points",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ScratchCardItem(
    reward: RewardEntity,
    onScratch: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(140.dp)
            .height(140.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable { if (!reward.isScratched) onScratch() }
            .testTag("scratch_card_${reward.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (reward.isScratched) Color(0xFF1E293B) else PayGoldReward
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            if (reward.isScratched) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(PayTealAccent.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = "Won",
                            tint = PayGoldReward,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "WON ₹${reward.rewardAmount.toInt()}",
                        color = PayTealAccent,
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = reward.title,
                        color = Color.LightGray,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = "Scratch",
                        tint = Color.Black,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "TAP TO SCRATCH",
                        color = Color.Black,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "Win up to ₹500",
                        color = Color(0xFF334155),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun CashbackHistoryRow(reward: RewardEntity) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xFFDCFCE7), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.MonetizationOn,
                    contentDescription = "Cashback",
                    tint = Color(0xFF15803D),
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = reward.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Claimed • ${reward.expiryDate}",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }
        }

        Text(
            text = "+ ₹${reward.rewardAmount.toInt()}",
            fontWeight = FontWeight.Black,
            fontSize = 16.sp,
            color = Color(0xFF15803D)
        )
    }
}

@Composable
fun MerchantOfferCard(
    offer: MerchantOffer,
    isCopied: Boolean,
    onCopyCode: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(offer.bgColor, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = offer.icon,
                        contentDescription = offer.brandName,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = offer.brandName,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = offer.offerTitle,
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
                color = if (isCopied) Color(0xFFDCFCE7) else Color(0xFFF1F5F9),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .clickable { onCopyCode() }
                    .testTag("claim_offer_${offer.id}")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                        contentDescription = "Code",
                        tint = if (isCopied) Color(0xFF15803D) else PayBluePrimary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isCopied) "Copied!" else offer.promoCode,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isCopied) Color(0xFF15803D) else PayBluePrimary
                    )
                }
            }
        }
    }
}
