package com.varshith.expensetracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.varshith.expensetracker.ui.navigation.AppNavHost
import com.varshith.expensetracker.ui.theme.ExpenseTrackerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as ExpenseTrackerApp
        setContent {
            ExpenseTrackerTheme {
                AppNavHost(
                    repository = app.repository,
                    recurringExpenseDao = app.database.recurringExpenseDao()
                )
            }
        }
    }
}