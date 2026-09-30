package com.varshith.expensetracker.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

object CategoryStyle {
    fun colorFor(category: String): Color = when (category) {
        "Food" -> CategoryFood
        "Travel" -> CategoryTravel
        "Shopping" -> CategoryShopping
        "Bills" -> CategoryBills
        "Health" -> CategoryHealth
        else -> CategoryOther
    }

    fun iconFor(category: String): ImageVector = when (category) {
        "Food" -> Icons.Filled.Restaurant
        "Travel" -> Icons.Filled.Flight
        "Shopping" -> Icons.Filled.ShoppingBag
        "Bills" -> Icons.Filled.ReceiptLong
        "Health" -> Icons.Filled.LocalHospital
        else -> Icons.Filled.Category
    }
}