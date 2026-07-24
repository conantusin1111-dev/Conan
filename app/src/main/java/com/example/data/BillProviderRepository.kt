package com.example.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.ui.graphics.vector.ImageVector

data class UtilityCategory(
    val id: String,
    val name: String,
    val icon: ImageVector,
    val description: String,
    val colorHex: String
)

data class BillProvider(
    val id: String,
    val name: String,
    val category: String,
    val upiId: String,
    val accountIdLabel: String,
    val sampleConsumerNo: String,
    val defaultAmount: Double,
    val dueDate: String,
    val bgHex: String
)

data class FetchedBillDetails(
    val providerId: String,
    val providerName: String,
    val category: String,
    val upiId: String,
    val consumerName: String,
    val consumerNumber: String,
    val billAmount: Double,
    val dueDate: String,
    val billNumber: String,
    val billPeriod: String,
    val status: String = "UNPAID"
)

class BillProviderRepository {

    fun getCategories(): List<UtilityCategory> {
        return listOf(
            UtilityCategory("cat_elec", "Electricity", Icons.Default.ElectricBolt, "Pay power & electricity bills", "#3B82F6"),
            UtilityCategory("cat_water", "Water", Icons.Default.WaterDrop, "Municipal & Jal Board water bills", "#06B6D4"),
            UtilityCategory("cat_broadband", "Broadband", Icons.Default.Router, "Fiber & Landline bill payments", "#8B5CF6"),
            UtilityCategory("cat_mobile", "Mobile", Icons.Default.PhoneAndroid, "Recharge & Postpaid mobile bills", "#10B981"),
            UtilityCategory("cat_gas", "LPG Gas", Icons.Default.LocalGasStation, "Piped gas & cylinder booking", "#F59E0B"),
            UtilityCategory("cat_dth", "DTH & TV", Icons.Default.Tv, "Direct-to-home TV recharges", "#EC4899")
        )
    }

    private val allProviders = listOf(
        // Electricity
        BillProvider("prov_1", "Tata Power Electricity", "Electricity", "tatapower@icici", "CA Number (12 digits)", "102938475612", 1280.0, "15 Aug 2026", "#0284C7"),
        BillProvider("prov_2", "Adani Electricity Mumbai", "Electricity", "adani.mumbai@axis", "Consumer Account No", "9012384712", 2450.0, "12 Aug 2026", "#0D9488"),
        BillProvider("prov_3", "BESCOM Bengaluru", "Electricity", "bescom@sbi", "Account ID / K-Number", "K8849201", 940.0, "18 Aug 2026", "#2563EB"),
        BillProvider("prov_4", "BSES Rajdhani Delhi", "Electricity", "bses.rajdhani@hdfc", "CA Number", "100293841", 1620.0, "20 Aug 2026", "#4F46E5"),

        // Water
        BillProvider("prov_5", "Delhi Jal Board (DJB)", "Water", "djb.water@sbi", "K-No / Water Connection No", "DJB9948102", 420.0, "25 Aug 2026", "#0284C7"),
        BillProvider("prov_6", "Bangalore Water Supply (BWSSB)", "Water", "bwssb@canara", "RR Number / Consumer No", "BWSSB77402", 580.0, "22 Aug 2026", "#0D9488"),
        BillProvider("prov_7", "BMC Water Department Mumbai", "Water", "bmc.water@icici", "CCN Number", "BMC8829102", 350.0, "30 Aug 2026", "#2563EB"),

        // Broadband
        BillProvider("prov_8", "Airtel Xstream Fiber", "Broadband", "airtel.broadband@icici", "Landline / Account Number", "08049281726", 943.0, "10 Aug 2026", "#DC2626"),
        BillProvider("prov_9", "JioFiber", "Broadband", "jio.fiber@jio", "JioFiber Service ID", "309482710293", 1179.0, "14 Aug 2026", "#2563EB"),
        BillProvider("prov_10", "ACT Fibernet", "Broadband", "act.fibernet@axis", "Account No / User ID", "ACT1092837", 825.0, "16 Aug 2026", "#7C3AED"),

        // Mobile
        BillProvider("prov_11", "Jio Prepaid & Postpaid", "Mobile", "jio.recharge@jio", "Mobile Number", "9876543210", 299.0, "Instant", "#2563EB"),
        BillProvider("prov_12", "Airtel Mobile", "Mobile", "airtel.pay@icici", "Mobile Number", "9812345678", 349.0, "Instant", "#DC2626"),
        BillProvider("prov_13", "Vi (Vodafone Idea)", "Mobile", "vi.recharge@paytm", "Mobile Number", "9711223344", 269.0, "Instant", "#E11D48"),

        // Gas
        BillProvider("prov_14", "Indane Gas (LPG)", "LPG Gas", "indane.lpg@sbi", "Consumer ID / Mobile No", "7092817263", 850.0, "On Delivery", "#EA580C"),
        BillProvider("prov_15", "Mahanagar Gas Mumbai (MGL)", "LPG Gas", "mgl.piped@icici", "BP Number", "102938475", 620.0, "28 Aug 2026", "#D97706"),

        // DTH
        BillProvider("prov_16", "Tata Play (Tata Sky)", "DTH & TV", "tataplay@sbi", "Subscriber ID", "1092837461", 450.0, "Instant", "#7C3AED"),
        BillProvider("prov_17", "Airtel Digital TV", "DTH & TV", "airtel.dth@icici", "Customer ID", "301928374", 380.0, "Instant", "#DC2626")
    )

    fun getAllProviders(): List<BillProvider> = allProviders

    fun getProvidersByCategory(category: String): List<BillProvider> {
        if (category.equals("All", ignoreCase = true)) return allProviders
        return allProviders.filter { it.category.equals(category, ignoreCase = true) }
    }

    fun searchProviders(query: String): List<BillProvider> {
        if (query.isBlank()) return allProviders
        val q = query.lowercase().trim()
        return allProviders.filter {
            it.name.lowercase().contains(q) ||
                    it.category.lowercase().contains(q) ||
                    it.accountIdLabel.lowercase().contains(q)
        }
    }

    fun fetchBill(provider: BillProvider, consumerNo: String): FetchedBillDetails {
        val num = if (consumerNo.isNotBlank()) consumerNo else provider.sampleConsumerNo
        return FetchedBillDetails(
            providerId = provider.id,
            providerName = provider.name,
            category = provider.category,
            upiId = provider.upiId,
            consumerName = "Alex Morgan",
            consumerNumber = num,
            billAmount = provider.defaultAmount,
            dueDate = provider.dueDate,
            billNumber = "BILL-${(100000..999999).random()}",
            billPeriod = "July 2026",
            status = "UNPAID"
        )
    }
}
