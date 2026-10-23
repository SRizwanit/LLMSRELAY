package com.srizwan.task

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.srizwan.task.task.BookGrid
import com.srizwan.task.task.TaskDashboard

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
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
    ) { pad ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (state.tab) {
                TaskTab.TASK -> TaskPane(state, vm)
                TaskTab.PAY -> PlaceholderPane("পেমেন্ট")
                TaskTab.RANK -> PlaceholderPane("র‍্যাংক")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskTopBar(state: TaskState, nav: NavController, vm: TaskViewModel) {
    if (state.tab != TaskTab.TASK) {
        TopAppBar(
            title = {
                Text(
                    text = when (state.tab) {
                        TaskTab.PAY -> "পেমেন্ট"
                        TaskTab.RANK -> "র‍্যাংক"
                        else -> ""
                    },
                    fontWeight = FontWeight.Bold
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )
        return
    }

    TopAppBar(
        title = {
            TextField(
                value = state.query,
                onValueChange = { vm.setQuery(it) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("কিতাব খুঁজুন…", fontSize = 15.sp) },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                shape = RoundedCornerShape(8.dp)
            )
        },
        actions = {
            IconButton(onClick = { nav.navigate(Routes.PROFILE) }) {
                Icon(
                    painter = painterResource(id = android.R.drawable.ic_menu_myplaces),
                    contentDescription = "প্রোফাইল"
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}

@Composable
private fun TaskBottomNav(tab: TaskTab, vm: TaskViewModel) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp
    ) {
        NavigationBarItem(
            selected = tab == TaskTab.TASK,
            onClick = { vm.switchTab(TaskTab.TASK) },
            icon = {
                Icon(
                    painter = painterResource(id = android.R.drawable.ic_menu_agenda),
                    contentDescription = null
                )
            },
            label = { Text("টাস্ক", fontSize = 12.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
            )
        )
        NavigationBarItem(
            selected = tab == TaskTab.PAY,
            onClick = { vm.switchTab(TaskTab.PAY) },
            icon = {
                Icon(
                    painter = painterResource(id = android.R.drawable.ic_menu_report_image),
                    contentDescription = null
                )
            },
            label = { Text("পেমেন্ট", fontSize = 12.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
            )
        )
        NavigationBarItem(
            selected = tab == TaskTab.RANK,
            onClick = { vm.switchTab(TaskTab.RANK) },
            icon = {
                Icon(
                    painter = painterResource(id = android.R.drawable.ic_menu_sort_by_size),
                    contentDescription = null
                )
            },
            label = { Text("র‍্যাংক", fontSize = 12.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
            )
        )
    }
}

@Composable
private fun TaskPane(state: TaskState, vm: TaskViewModel) {
    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            TaskDashboard(
                ready = state.status == "ready",
                proofs = state.proofs,
                booksById = state.booksById,
                goal = state.goal,
                resume = state.resume,
                onGoal = { /* ধাপ 4e-তে GoalDialog */ },
                onResume = { /* view?book=...&page=... রুটে যাবে, ধাপ 7-এ */ }
            )
        }

        item {
            Box(modifier = Modifier.padding(vertical = 12.dp)) {
                BookGrid(
                    status = state.status,
                    spinning = state.spinning,
                    books = state.books,
                    query = state.query,
                    bookCounts = state.bookCounts,
                    byBook = state.byBook,
                    resume = state.resume,
                    rateOf = { book ->
                        val id = book.optInt("id", 0)
                        state.finData.rates[id] ?: 0
                    },
                    onOpen = { bookId ->
                        /* view?book=$bookId রুটে যাবে, ধাপ 7-এ */
                    }
                )
            }
        }
    }
}

@Composable
private fun PlaceholderPane(label: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "পরের ধাপে বানানো হবে",
                color = AppColors.extra.muted,
                fontSize = 14.sp
            )
        }
    }
}

private fun bn(n: Int): String {
    val bengali = arrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
    return n.toString().map { if (it.isDigit()) bengali[it - '0'] else it }.joinToString("")
}
