package com.srizwan.task

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.srizwan.task.task.BookGrid
import com.srizwan.task.task.PayPane
import com.srizwan.task.task.RankPane
import com.srizwan.task.task.TaskDashboard
import com.srizwan.task.task.resumeTarget

@Composable
fun TaskScreen(navController: NavController, vm: TaskViewModel = viewModel()) {
    val context = LocalContext.current
    val state by vm.state.collectAsState()

    LaunchedEffect(Unit) {
        vm.load(context)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
    ) {
        TaskTopBar(state = state, nav = navController, vm = vm)

        Box(modifier = Modifier.weight(1f)) {
            if (state.loading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                TaskPane(state = state, vm = vm, navController = navController)
            }
        }

        TaskBottomNav(tab = state.tab, vm = vm)
    }
}

@Composable
private fun TaskTopBar(state: TaskState, nav: NavController, vm: TaskViewModel) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "📑",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "RizwanTask",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = state.user?.optString("full_name") ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.extra.muted
                )
            }
            IconButton(onClick = { nav.navigate(Routes.PROFILE) }) {
                Text(text = "👤", style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}

@Composable
private fun TaskBottomNav(tab: TaskTab, vm: TaskViewModel) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        NavigationBarItem(
            selected = tab == TaskTab.TASK,
            onClick = { vm.switchTab(TaskTab.TASK) },
            icon = { Text("📝", style = MaterialTheme.typography.titleMedium) },
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
    val ctx = LocalContext.current
    val userId = state.user?.optInt("id") ?: 0
    val goal = vm.readGoalFromPrefs(ctx, userId)
    val booksById = state.books.associateBy { it.optString("id") }
    val resumeObj = state.proofs.lastOrNull()?.let { p ->
        booksById[p.optString("book_id", "")]
    }
    val resume = resumeTarget(resumeObj, booksById, state.proofs)

    when (state.tab) {
        TaskTab.TASK -> {
            Column(modifier = Modifier.fillMaxSize()) {
                TaskDashboard(
                    state = state,
                    onGoal = { vm.showGoalDialog(true) },
                    resume = resume,
                    onResume = { bookId, page ->
                        // ধাপ 7-এ view route
                    }
                )
                Spacer(Modifier.height(8.dp))
                BookGrid(
                    books = state.books,
                    proofs = state.proofs,
                    query = state.query,
                    onQueryChange = { vm.setQuery(it) },
                    onOpen = { book, page ->
                        // ধাপ 7
                    }
                )
            }
        }
        TaskTab.PAY -> {
            PayPane(
                state = state,
                vm = vm,
                proofs = state.proofs,
                pays = state.payHistory
            )
        }
        TaskTab.RANK -> {
            RankPane(
                hidden = false,
                period = state.rankPeriod,
                onPeriod = { vm.switchRankPeriod(it) },
                loading = state.rankLoading,
                data = state.rankData,
                error = state.rankError,
                meId = userId
            )
        }
    }
}
