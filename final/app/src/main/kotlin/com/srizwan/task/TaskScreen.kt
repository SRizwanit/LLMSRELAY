package com.srizwan.task

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.srizwan.task.task.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskScreen(navController: NavController, vm: TaskViewModel = viewModel()) {
    val context = LocalContext.current
    val state by vm.state.collectAsState()

    LaunchedEffect(Unit) {
        vm.load(context)
    }

    Scaffold(
        topBar = {
            TaskTopBar(state, navController, vm)
        },
        bottomBar = {
            TaskBottomNav(state.tab, vm)
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { pad ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(pad)
        ) {
            TaskPane(state, vm, navController)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskTopBar(state: TaskState, nav: NavController, vm: TaskViewModel) {
    val context = LocalContext.current
    var menuExpanded by remember { mutableStateOf(false) }

    TopAppBar(
        title = {
            Text(
                text = "RizwanTask",
                fontWeight = FontWeight.Bold
            )
        },
        actions = {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(Icons.Default.Menu, contentDescription = "মেনু")
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("প্রোফাইল") },
                    onClick = {
                        menuExpanded = false
                        nav.navigate(Routes.PROFILE)
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Person, contentDescription = null)
                    }
                )
                DropdownMenuItem(
                    text = { Text("লগআউট") },
                    onClick = {
                        menuExpanded = false
                        Session.clear(context)
                        nav.navigate(Routes.LOGIN) {
                            popUpTo(Routes.TASK) { inclusive = true }
                        }
                    },
                    leadingIcon = {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null)
                    }
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

@Composable
private fun TaskBottomNav(tab: TaskTab, vm: TaskViewModel) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        NavigationBarItem(
            selected = tab == TaskTab.TASK,
            onClick = { vm.switchTab(TaskTab.TASK) },
            icon = { Text("📋", style = MaterialTheme.typography.titleMedium) },
            label = { Text("টাস্ক") }
        )
        NavigationBarItem(
            selected = tab == TaskTab.PAY,
            onClick = { vm.switchTab(TaskTab.PAY) },
            icon = { Text("💰", style = MaterialTheme.typography.titleMedium) },
            label = { Text("পেমেন্ট") }
        )
        NavigationBarItem(
            selected = tab == TaskTab.RANK,
            onClick = { vm.switchTab(TaskTab.RANK) },
            icon = { Text("🏆", style = MaterialTheme.typography.titleMedium) },
            label = { Text("র‍্যাংক") }
        )
    }
}

@Composable
private fun TaskPane(state: TaskState, vm: TaskViewModel, navController: NavController) {
    when (state.tab) {
        TaskTab.TASK -> TaskDashboard(
            state = state,
            vm = vm,
            navController = navController
        )
        TaskTab.PAY -> PayPane(
            state = state,
            vm = vm,
            proofs = state.proofs,
            pays = state.money.history
        )
        TaskTab.RANK -> RankPane(
            hidden = false,
            period = state.rankPeriod,
            onPeriod = vm::switchRankPeriod,
            loading = state.rankLoading,
            data = state.rankData,
            error = state.rankError,
            meId = state.userId
        )
    }
}
