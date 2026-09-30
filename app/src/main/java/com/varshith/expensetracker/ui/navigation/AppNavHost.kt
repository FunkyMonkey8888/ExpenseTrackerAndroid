package com.varshith.expensetracker.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.varshith.expensetracker.data.ExpenseRepository
import com.varshith.expensetracker.data.RecurringExpenseDao
import com.varshith.expensetracker.ui.AddExpenseScreen
import com.varshith.expensetracker.ui.home.HomeScreen
import com.varshith.expensetracker.ui.recurring.RecurringScreen
import com.varshith.expensetracker.ui.search.SearchScreen

private const val ARG_EXPENSE_ID = "expenseId"

object Routes {
    const val HOME = "home"
    const val ADD_EXPENSE = "addExpense?expenseId={expenseId}"
    const val SEARCH = "search"
    const val RECURRING = "recurring"
    fun addExpense(expenseId: Long? = null) =
        if (expenseId != null) "addExpense?expenseId=$expenseId" else "addExpense"
}

@Composable
fun AppNavHost(repository: ExpenseRepository, recurringExpenseDao: RecurringExpenseDao) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                repository = repository,
                onAddExpense = { navController.navigate(Routes.addExpense()) },
                onEditExpense = { id -> navController.navigate(Routes.addExpense(id)) },
                onSearch = { navController.navigate(Routes.SEARCH) },
                onRecurring = { navController.navigate(Routes.RECURRING) }
            )
        }
        composable(
            route = Routes.ADD_EXPENSE,
            arguments = listOf(navArgument(ARG_EXPENSE_ID) {
                type = NavType.LongType
                defaultValue = -1L
            })
        ) { backStackEntry ->
            val idArg = backStackEntry.arguments?.getLong(ARG_EXPENSE_ID) ?: -1L
            AddExpenseScreen(
                repository = repository,
                expenseId = if (idArg == -1L) null else idArg,
                onDone = { navController.popBackStack() }
            )
        }
        composable(Routes.SEARCH) {
            SearchScreen(
                repository = repository,
                onBack = { navController.popBackStack() },
                onExpenseClick = { id -> navController.navigate(Routes.addExpense(id)) }
            )
        }
        composable(Routes.RECURRING) {
            RecurringScreen(
                recurringExpenseDao = recurringExpenseDao,
                onBack = { navController.popBackStack() }
            )
        }
    }
}