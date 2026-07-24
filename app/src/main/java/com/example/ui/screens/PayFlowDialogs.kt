package com.example.ui.screens

import android.Manifest
import android.net.Uri
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.model.BankAccountEntity
import com.example.data.model.ContactEntity
import com.example.data.model.RewardEntity
import com.example.ui.theme.PayBluePrimary
import com.example.ui.theme.PayGoldReward
import com.example.ui.theme.PayTealAccent
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.text.NumberFormat
import java.util.Locale

fun parseUpiQrUrl(rawQr: String): Pair<String, String> {
    val trimmed = rawQr.trim()
    if (trimmed.startsWith("upi://pay", ignoreCase = true)) {
        val uri = Uri.parse(trimmed)
        val pa = uri.getQueryParameter("pa") ?: ""
        val pn = uri.getQueryParameter("pn") ?: if (pa.contains("@")) pa.substringBefore("@").replace(".", " ") else "Merchant"
        return Pair(pn.ifBlank { "UPI Merchant" }, pa.ifBlank { trimmed })
    }
    if (trimmed.contains("@")) {
        val name = trimmed.substringBefore("@").replace(".", " ").replace("_", " ")
        val formattedName = name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        return Pair(formattedName.ifBlank { "UPI Recipient" }, trimmed)
    }
    return Pair("Scanned Merchant", trimmed)
}

@Composable
fun CameraViewfinder(
    modifier: Modifier = Modifier,
    isTorchOn: Boolean = false
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var hasError by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { previewView ->
                cameraProviderFuture.addListener({
                    try {
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }
                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                        cameraProvider.unbindAll()
                        val camera = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview
                        )
                        if (camera.cameraInfo.hasFlashUnit()) {
                            camera.cameraControl.enableTorch(isTorchOn)
                        }
                    } catch (e: Exception) {
                        hasError = true
                    }
                }, ContextCompat.getMainExecutor(context))
            }
        )

        if (hasError) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF1E293B)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Camera feed initializing...",
                    color = Color.LightGray,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun ScanQrDialog(
    onDismiss: () -> Unit,
    onQrScanned: (String, String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    var isTorchOn by remember { mutableStateOf(false) }
    var manualUpiInput by remember { mutableStateOf("") }

    // Laser scanning line animation
    val infiniteTransition = rememberInfiniteTransition(label = "scanner_laser")
    val laserYRatio by infiniteTransition.animateFloat(
        initialValue = 0.05f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_y"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF0F172A)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.QrCodeScanner,
                        contentDescription = "QR Scanner",
                        tint = PayTealAccent,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Scan Any UPI QR Code",
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.LightGray)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Scanner Area or Camera Permission Prompt
            if (cameraPermissionState.status.isGranted) {
                Box(
                    modifier = Modifier
                        .size(260.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFF1E293B))
                        .border(2.5.dp, PayTealAccent, RoundedCornerShape(24.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    // Live Camera Feed
                    CameraViewfinder(
                        modifier = Modifier.fillMaxSize(),
                        isTorchOn = isTorchOn
                    )

                    // Laser beam overlay
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .align(Alignment.TopCenter)
                            .padding(horizontal = 12.dp)
                            .graphicsLayer {
                                translationY = laserYRatio * 250.dp.toPx()
                            }
                            .background(PayTealAccent)
                    )

                    // Torch toggle button at top right
                    IconButton(
                        onClick = { isTorchOn = !isTorchOn },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            .size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                            contentDescription = "Toggle Torch",
                            tint = PayGoldReward,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(Color(0xFF00D084), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Camera Active • Point camera at merchant QR code",
                        color = Color.LightGray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                // Permission Request Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, PayTealAccent)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(PayBluePrimary.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.CameraAlt,
                                contentDescription = "Camera",
                                tint = PayTealAccent,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Camera Access Required",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "To scan merchant QR codes at stores and shops, allow camera access.",
                            color = Color.LightGray,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { cameraPermissionState.launchPermissionRequest() },
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .height(44.dp)
                                .testTag("grant_camera_permission_btn"),
                            shape = RoundedCornerShape(22.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PayTealAccent, contentColor = Color.Black)
                        ) {
                            Text("GRANT CAMERA PERMISSION", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Manual UPI ID input
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = manualUpiInput,
                    onValueChange = { manualUpiInput = it },
                    placeholder = { Text("Enter UPI ID or paste URL", color = Color.Gray, fontSize = 13.sp) },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("manual_upi_input"),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0xFF1E293B),
                        unfocusedContainerColor = Color(0xFF1E293B),
                        focusedBorderColor = PayTealAccent,
                        unfocusedBorderColor = Color(0xFF334155)
                    )
                )

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {
                        if (manualUpiInput.isNotBlank()) {
                            val (title, upiId) = parseUpiQrUrl(manualUpiInput)
                            onQrScanned(title, upiId)
                        }
                    },
                    enabled = manualUpiInput.isNotBlank(),
                    modifier = Modifier
                        .height(52.dp)
                        .testTag("manual_upi_submit"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PayBluePrimary)
                ) {
                    Text("PAY", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Or Quick Scan Sample Merchants:",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onQrScanned("Starbucks Coffee", "starbucks@icici") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("qr_sample_starbucks"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B), contentColor = Color.White)
                ) {
                    Text("Starbucks", fontSize = 11.sp, maxLines = 1)
                }

                Button(
                    onClick = { onQrScanned("Supermarket", "mart@okaxis") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("qr_sample_mart"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B), contentColor = Color.White)
                ) {
                    Text("Supermart", fontSize = 11.sp, maxLines = 1)
                }

                Button(
                    onClick = { onQrScanned("Fuel Station", "petrol@sbi") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("qr_sample_fuel"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B), contentColor = Color.White)
                ) {
                    Text("Fuel Station", fontSize = 11.sp, maxLines = 1)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentEntrySheet(
    recipientTitle: String,
    upiId: String,
    bankAccounts: List<BankAccountEntity>,
    selectedBankId: String,
    onBankSelect: (String) -> Unit,
    onDismiss: () -> Unit,
    onProceedToPin: (Double, String, String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var amountText by remember { mutableStateOf("") }
    var noteText by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Transfer") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Pay $recipientTitle",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp
                    )
                    Text(
                        text = upiId,
                        color = Color.Gray,
                        fontSize = 13.sp
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Amount Input
            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = { Text("Enter Amount (₹)") },
                prefix = { Text("₹ ", fontWeight = FontWeight.Bold) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("amount_input"),
                shape = RoundedCornerShape(16.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Note Input
            OutlinedTextField(
                value = noteText,
                onValueChange = { noteText = it },
                label = { Text("Add Note (e.g., Dinner, Rent)") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("note_input"),
                shape = RoundedCornerShape(16.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Select Bank Account",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            for (bank in bankAccounts) {
                val isSelected = bank.id == selectedBankId
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onBankSelect(bank.id) }
                        .testTag("bank_select_${bank.id}"),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) PayBluePrimary.copy(alpha = 0.1f) else Color(0xFFF8FAFC)
                    ),
                    border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, PayBluePrimary) else null
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AccountBalance, contentDescription = "Bank", tint = PayBluePrimary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(text = bank.bankName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(text = "A/c •••• ${bank.accountNumberLast4} | Bal: ₹${bank.balance.toInt()}", fontSize = 12.sp, color = Color.Gray)
                            }
                        }
                        if (isSelected) {
                            Icon(Icons.Default.CheckCircle, contentDescription = "Selected", tint = PayBluePrimary)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull() ?: 0.0
                    if (amt > 0) {
                        onProceedToPin(amt, noteText, category)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("proceed_to_pin_btn"),
                enabled = (amountText.toDoubleOrNull() ?: 0.0) > 0,
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PayBluePrimary)
            ) {
                Text("PROCEED TO PAY", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BankBalanceSheet(
    bankAccounts: List<BankAccountEntity>,
    upiLiteBalance: Double,
    onDismiss: () -> Unit,
    onTopUpUpiLite: (Double) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Bank Accounts & Balance",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // UPI Lite Balance Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Bolt, contentDescription = "Lite", tint = PayTealAccent)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("UPI LITE WALLET", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("₹${upiLiteBalance.toInt()}", color = PayTealAccent, fontWeight = FontWeight.Black, fontSize = 28.sp)
                        Text("Instant payments without UPI PIN", color = Color.LightGray, fontSize = 11.sp)
                    }

                    Button(
                        onClick = { onTopUpUpiLite(500.0) },
                        colors = ButtonDefaults.buttonColors(containerColor = PayTealAccent, contentColor = Color.Black),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.testTag("topup_lite_btn")
                    ) {
                        Text("+ Add ₹500", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text("Linked Bank Accounts", fontWeight = FontWeight.Bold, fontSize = 16.sp)

            Spacer(modifier = Modifier.height(12.dp))

            for (bank in bankAccounts) {
                val formattedBal = NumberFormat.getNumberInstance(Locale("en", "IN")).format(bank.balance)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = bank.bankName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(text = "${bank.accountType} A/c •••• ${bank.accountNumberLast4}", fontSize = 12.sp, color = Color.Gray)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(text = "₹$formattedBal", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = PayBluePrimary)
                            Text(text = if (bank.isDefault) "Default" else "", fontSize = 10.sp, color = PayTealAccent, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RewardsScratchSheet(
    rewards: List<RewardEntity>,
    totalCashback: Double,
    onScratch: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF0F172A)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Rewards & Cashback", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                    Text("Total Cashback Won: ₹${totalCashback.toInt()}", color = PayGoldReward, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.height(320.dp)
            ) {
                items(rewards, key = { it.id }) { reward ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { if (!reward.isScratched) onScratch(reward.id) }
                            .testTag("reward_card_${reward.id}"),
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
                                    Icon(Icons.Default.Star, contentDescription = "Won", tint = PayGoldReward, modifier = Modifier.size(32.dp))
                                    Text("WON ₹${reward.rewardAmount.toInt()}", color = PayTealAccent, fontWeight = FontWeight.Black, fontSize = 18.sp)
                                    Text(reward.title, color = Color.LightGray, fontSize = 10.sp)
                                }
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Star, contentDescription = "Scratch", tint = Color.Black, modifier = Modifier.size(36.dp))
                                    Text("TAP TO SCRATCH", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 12.sp)
                                    Text("Win up to ₹500", color = Color.DarkGray, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
