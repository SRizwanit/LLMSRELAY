package com.srizwan.task

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController

@Composable
fun TaskScreen(navController: NavController, vm: TaskViewModel = viewModel()) {
    val context = LocalContext.current
    val state by vm.state.collectAsState()

    LaunchedEffect(Unit) {
        vm.load(context)
    }

    Scaffold(
        topBar = { TaskTopBar(state, navController, vm) },
        bottomBar = { TaskBottomNav(state.tab, vm) },
        containerColor = MaterialTheme.colorScheme.background
    ) { pad ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            when (state.tab) {
                TaskTab.TASK -> PlaceholderPane("টাস্কের তালিকা")
                TaskTab.PAY -> PlaceholderPane("পেমেন্ট")
                TaskTab.RANK -> PlaceholderPane("লিডারবোর্ড")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskTopBar(state: TaskState, nav: NavController, vm: TaskViewModel) {
    val context = LocalContext.current
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .background(MaterialTheme.colorScheme.primary, shape = MaterialTheme.shapes.small),
                    contentAlignment = Alignment.Center
                ) {
                    Text("📑", fontSize = 16.sp)
                }
                Text(
                    text = when (state.tab) {
                        TaskTab.TASK -> "টাস্কের তালিকা"
                        TaskTab.PAY -> "পেমেন্ট"
                        TaskTab.RANK -> "লিডারবোর্ড"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        actions = {
            if (state.myCount != null) {
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = AppColors.extra.gold.copy(alpha = 0.16f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AppColors.extra.gold.copy(alpha = 0.55f)),
                    modifier = Modifier.padding(end = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text("✓", fontSize = 14.sp, color = AppColors.extra.gold)
                        Text(
                            text = bn(state.myCount),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.extra.gold
                        )
                        Text("টি সম্পন্ন", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f))
                    }
                }
            }

            IconButton(onClick = { nav.navigate("profile") }) {
                Text("👤", fontSize = 18.sp)
            }

            if (state.isAdmin) {
                TextButton(onClick = { nav.navigate("admin") }) {
                    Text("🛡", fontSize = 14.sp)
                    Spacer(Modifier.width(4.dp))
                    Text("অ্যাডমিন", fontSize = 12.sp)
                }
                IconButton(onClick = {
                    Session.clear(context)
                    nav.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }) {
                    Text("🚪", fontSize = 18.sp)
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
    )
}

@Composable
private fun TaskBottomNav(tab: TaskTab, vm: TaskViewModel) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        NavigationBarItem(
            selected = tab == TaskTab.TASK,
            onClick = { vm.switchTab(TaskTab.TASK) },
            icon = { Text("📋", fontSize = 20.sp) },
            label = { Text("টাস্ক", fontSize = 11.sp) }
        )
        NavigationBarItem(
            selected = tab == TaskTab.PAY,
            onClick = { vm.switchTab(TaskTab.PAY) },
            icon = { Text("💰", fontSize = 20.sp) },
            label = { Text("পেমেন্ট", fontSize = 11.sp) }
        )
        NavigationBarItem(
            selected = tab == TaskTab.RANK,
            onClick = { vm.switchTab(TaskTab.RANK) },
            icon = { Text("🏆", fontSize = 20.sp) },
            label = { Text("র‍্যাংক", fontSize = 11.sp) }
        )
    }
}

@Composable
private fun PlaceholderPane(label: String) {
    Box(
        modifier = Modifier.fillMaxWidth().height(300.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(label, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
            Text("পরের উপ-ধাপে বানানো হবে", color = AppColors.extra.muted, fontSize = 13.sp)
        }
    }
}

private fun bn(n: Int): String {
    val bangla = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
    return n.toString().map { if (it.isDigit()) bangla[it - '0'] else it }.joinToString("")
}
