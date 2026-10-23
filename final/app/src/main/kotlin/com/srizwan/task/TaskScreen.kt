package com.srizwan.task

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.srizwan.task.task.BookGrid
import com.srizwan.task.task.PayPane
import com.srizwan.task.task.RankPane
import com.srizwan.task.task.TaskDashboard
import com.srizwan.task.task.resumeTarget

@OptIn(ExperimentalMaterial3Api::class)
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
        ) {
            if (state.loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                TaskPane(state, vm, navController)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskTopBar(state: TaskState, nav: NavController, vm: TaskViewModel) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = "RizwanTask 📑",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        navigationIcon = {
            Box(
                modifier = Modifier
                    .padding(start = 12.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = state.userName.firstOrNull()?.uppercase() ?: "U",
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        },
        actions = {
            IconButton(onClick = { nav.navigate(Routes.PROFILE) }) {
                Icon(
                    painter = painterResource(R.drawable.rizwantask),
                    contentDescription = "Profile",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier.statusBarsPadding()
    )
}

@Composable
private fun TaskBottomNav(tab: TaskTab, vm: TaskViewModel) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        NavigationBarItem(
            selected = tab == TaskTab.TASK,
            onClick = { vm.switchTab(TaskTab.TASK) },
            icon = {
                Icon(
                    painter = painterResource(R.drawable.rizwantask),
                    contentDescription = "টাস্ক"
                )
            },
            label = { Text("টাস্ক") }
        )
        NavigationBarItem(
            selected = tab == TaskTab.PAY,
            onClick = { vm.switchTab(TaskTab.PAY) },
            icon = {
                Icon(
                    painter = painterResource(R.drawable.rizwantask),
                    contentDescription = "পেমেন্ট"
                )
            },
            label = { Text("পেমেন্ট") }
        )
        NavigationBarItem(
            selected = tab == TaskTab.RANK,
            onClick = { vm.switchTab(TaskTab.RANK) },
            icon = {
                Icon(
                    painter = painterResource(R.drawable.rizwantask),
                    contentDescription = "র‍্যাংক"
                )
            },
            label = { Text("র‍্যাংক") }
        )
    }
}

@Composable
private fun TaskPane(state: TaskState, vm: TaskViewModel, navController: NavController) {
    when (state.tab) {
        TaskTab.TASK -> {
            Column(modifier = Modifier.fillMaxSize()) {
                val resume = resumeTarget(
                    state.books.firstOrNull { b ->
                        val id = b.optString("id", "")
                        val total = b.optInt("total_pages", 0)
                        val done = state.proofs.count { it.optString("book_id") == id }
                        done > 0 && done < total
                    },
                    state.booksById,
                    state.proofs
                )
                
                TaskDashboard(
                    state = state,
                    onGoal = { vm.showGoalDialog(true) },
                    resume = resume,
                    onResume = { book: String, page: Int ->
                        // ধাপ ৭-এ View রুট বানানো হবে
                    },
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(Modifier.height(12.dp))
                BookGrid(
                    ready = !state.loading,
                    me = state.userId,
                    books = state.books,
                    proofs = state.proofs,
                    booksById = state.booksById,
                    query = state.query,
                    onQueryChange = { vm.setQuery(it) },
                    goal = state.goal,
                    resume = resume,
                    rateOf = { vm.rateOf(it) },
                    onResume = { book: String, page: Int ->
                        // ধাপ ৭-এ View রুট বানানো হবে
                    },
                    onOpen = { book: String, page: Int ->
                        // ধাপ ৭-এ View রুট বানানো হবে
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        TaskTab.PAY -> {
            PayPane(
                state = state,
                vm = vm,
                proofs = state.proofs,
                pays = state.money?.payments ?: emptyList()
            )
        }
        TaskTab.RANK -> {
            RankPane(
                period = state.rankPeriod,
                onPeriod = { vm.switchRankPeriod(it) },
                loading = state.rankLoading,
                data = state.rankData,
                error = state.rankError,
                meId = state.userId
            )
        }
    }
}
