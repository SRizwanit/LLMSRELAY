package com.srizwan.task

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.srizwan.task.task.PayPane
import com.srizwan.task.task.TaskDashboard
import com.srizwan.task.task.resumeTarget

@Composable
fun TaskScreen(navController: NavController, vm: TaskViewModel = viewModel()) {
    val state = vm.state
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        vm.load(context)
    }

    Scaffold(
        topBar = { TaskTopBar(state, navController, vm) },
        bottomBar = { TaskBottomNav(state.tab, vm) }
    ) { pad ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .background(MaterialTheme.colorScheme.background)
        ) {
            TaskPane(state, vm, navController)
        }
    }
}

@Composable
private fun TaskTopBar(state: TaskState, nav: NavController, vm: TaskViewModel) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "RizwanTask 📑",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { nav.navigate(Routes.PROFILE) }) {
                    Text("প্রোফাইল")
                }
            }
        }
    }
}

@Composable
private fun TaskBottomNav(tab: TaskTab, vm: TaskViewModel) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        NavigationBarItem(
            selected = tab == TaskTab.TASK,
            onClick = { vm.switchTab(TaskTab.TASK) },
            icon = { Text("📋", fontSize = 20.sp) },
            label = { Text("টাস্ক", fontSize = 12.sp) }
        )
        NavigationBarItem(
            selected = tab == TaskTab.PAY,
            onClick = { vm.switchTab(TaskTab.PAY) },
            icon = { Text("💰", fontSize = 20.sp) },
            label = { Text("পেমেন্ট", fontSize = 12.sp) }
        )
        NavigationBarItem(
            selected = tab == TaskTab.RANK,
            onClick = { vm.switchTab(TaskTab.RANK) },
            icon = { Text("🏆", fontSize = 20.sp) },
            label = { Text("র‍্যাংক", fontSize = 12.sp) }
        )
    }
}

@Composable
private fun TaskPane(state: TaskState, vm: TaskViewModel, navController: NavController) {
    val context = LocalContext.current

    when (state.tab) {
        TaskTab.TASK -> {
            val booksById = state.books.associateBy { it.optInt("id", 0).toString() }
            val goal = vm.readGoalFromPrefs(context, state.me?.optInt("id") ?: 0)
            val resume = vm.findResume(state.proofs, booksById)
            val resumeT = resumeTarget(resume, booksById, state.proofs)

            TaskDashboard(
                ready = state.me != null && state.books.isNotEmpty(),
                me = state.me,
                books = state.books,
                proofs = state.proofs,
                booksById = booksById,
                query = state.query,
                onQueryChange = { vm.setQuery(it) },
                goal = goal,
                resume = resumeT,
                rateOf = vm::rateOf,
                onGoal = { vm.showGoalDialog(true) },
                onResume = { book, stem ->
                    // TODO: navigate to view?book=...&page=...
                }
            )
        }
        TaskTab.PAY -> {
            PayPane(
                state = state,
                vm = vm,
                proofs = state.proofs,
                pays = state.pays
            )
        }
        TaskTab.RANK -> PlaceholderPane("র‍্যাংক")
    }

    state.error?.let { err ->
        LaunchedEffect(err) {
            // TODO: show Snackbar
        }
    }
}

@Composable
private fun PlaceholderPane(label: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "পরের ধাপে বানানো হবে",
                color = AppColors.extra.muted
            )
        }
    }
}

private fun bn(n: Int): String {
    val map = mapOf('0' to '০', '1' to '১', '2' to '২', '3' to '৩', '4' to '৪', '5' to '৫', '6' to '৬', '7' to '৭', '8' to '৮', '9' to '৯')
    return n.toString().map { map[it] ?: it }.joinToString("")
}
