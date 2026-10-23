package com.srizwan.task.task

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.srizwan.task.AppColors
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

data class ResumeTarget(val book: JSONObject, val stem: String)

data class StreakInfo(
    val current: Int,
    val best: Int,
    val doneToday: Boolean
)

@Composable
fun TaskDashboard(
    ready: Boolean,
    me: JSONObject?,
    books: List<JSONObject>,
    proofs: List<JSONObject>,
    booksById: Map<String, JSONObject>,
    query: String,
    onQueryChange: (String) -> Unit,
    goal: Int,
    resume: ResumeTarget?,
    rateOf: (JSONObject?) -> Int,
    onGoal: () -> Unit,
    onResume: (String, String) -> Unit
) {
    val today = proofs.count {
        val d = it.optString("created_at", "").take(10)
        d == SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date())
    }

    val streak = streakInfo(proofs)
    val hit = goal > 0 && today >= goal
    val pct = if (goal > 0) (today.toFloat() / goal).coerceIn(0f, 1f) else 0f
    val left = maxOf(0, goal - today)

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .widthIn(max = 560.dp)
        ) {
            GoalCard(today, goal, hit, pct, left, onGoal)
            Spacer(Modifier.height(10.dp))
            StreakCard(streak.current, streak.best, streak.doneToday)
            Spacer(Modifier.height(14.dp))

            if (resume != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "চালিয়ে যান",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "${resume.book.optString("name")} — ${resume.stem}",
                            fontSize = 13.sp,
                            color = AppColors.extra.muted
                        )
                        Spacer(Modifier.height(10.dp))
                        Button(
                            onClick = {
                                val bookId = resume.book.optInt("id", 0).toString()
                                onResume(bookId, resume.stem)
                            },
                            modifier = Modifier.fillMaxWidth().height(44.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("খুলুন", fontSize = 15.sp)
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
            }

            BookGrid(
                ready = ready,
                books = books,
                proofs = proofs,
                booksById = booksById,
                query = query,
                onQueryChange = onQueryChange,
                resume = resume?.let {
                    JSONObject().apply {
                        put("book_id", it.book.optInt("id"))
                        put("stem", it.stem)
                    }
                },
                rateOf = rateOf,
                onOpen = onResume
            )

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun GoalCard(today: Int, goal: Int, hit: Boolean, pct: Float, left: Int, onGoal: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "আজকের লক্ষ্য",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                TextButton(onClick = onGoal) {
                    Text("পরিবর্তন", fontSize = 13.sp)
                }
            }
            Spacer(Modifier.height(8.dp))

            if (goal == 0) {
                Text(
                    text = "আপনি এখনো কোনো লক্ষ্য ঠিক করেননি। দৈনিক কতটি পেইজ প্রুফড করবেন তা ঠিক করলে কাজে মনোযোগ ও প্রেরণা বজায় থাকবে।",
                    fontSize = 13.sp,
                    color = AppColors.extra.muted,
                    lineHeight = 18.sp
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = "${bn(today)} / ${bn(goal)} পেইজ",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (hit) AppColors.extra.ok else MaterialTheme.colorScheme.onSurface
                    )
                    if (!hit && left > 0) {
                        Text(
                            text = "আরও ${bn(left)} পেইজ",
                            fontSize = 13.sp,
                            color = AppColors.extra.muted
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .background(AppColors.extra.line, RoundedCornerShape(4.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(pct)
                            .background(
                                if (hit) AppColors.extra.ok else MaterialTheme.colorScheme.primary,
                                RoundedCornerShape(4.dp)
                            )
                    )
                }

                if (hit) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "🎉 আজকের লক্ষ্য অর্জন করেছেন!",
                        fontSize = 13.sp,
                        color = AppColors.extra.ok,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun StreakCard(cur: Int, best: Int, doneToday: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🔥 টানা দিন",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "${bn(cur)} দিন",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (cur > 0) AppColors.extra.gold else AppColors.extra.muted
                    )
                    Text(
                        text = "বর্তমান",
                        fontSize = 12.sp,
                        color = AppColors.extra.muted
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${bn(best)} দিন",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.extra.muted
                    )
                    Text(
                        text = "সেরা রেকর্ড",
                        fontSize = 12.sp,
                        color = AppColors.extra.muted
                    )
                }
            }

            if (cur > 0) {
                Spacer(Modifier.height(8.dp))
                BadgeRow(best)
            }

            if (!doneToday && cur > 0) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "⚠️ আজ এখনো কোনো পেইজ প্রুফড করেননি — রেকর্ড বজায় রাখতে আজই কাজ করুন।",
                    fontSize = 12.sp,
                    color = AppColors.extra.warn,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
private fun BadgeRow(best: Int) {
    val milestones = listOf(3, 7, 14, 30, 50, 100)
    val reached = milestones.filter { it <= best }

    if (reached.isNotEmpty()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            reached.forEach { m ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = AppColors.extra.gold
                ) {
                    Text(
                        text = "${bn(m)}+ 🏆",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 11.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

fun resumeTarget(resume: JSONObject?, booksById: Map<String, JSONObject>, proofs: List<JSONObject>): ResumeTarget? {
    if (resume == null) return null
    val bookId = resume.optInt("book_id", 0).toString()
    val stem = resume.optString("stem", "")
    if (bookId == "0" || stem.isEmpty()) return null

    val book = booksById[bookId] ?: return null
    val total = book.optInt("total_pages", 0)
    if (total == 0) return null

    val done = proofs.filter { it.optInt("book_id", 0).toString() == bookId }
    if (done.size >= total) return null

    return ResumeTarget(book = book, stem = stem)
}

fun streakInfo(proofs: List<JSONObject>): StreakInfo {
    if (proofs.isEmpty()) return StreakInfo(0, 0, false)

    val dates = proofs.mapNotNull { it.optString("created_at", "").take(10).ifEmpty { null } }
        .distinct()
        .sorted()

    if (dates.isEmpty()) return StreakInfo(0, 0, false)

    val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }.format(Date())

    val doneToday = dates.contains(today)

    var cur = 0
    var best = 0
    var streak = 0
    var prevDate: String? = null

    dates.forEach { d ->
        if (prevDate == null) {
            streak = 1
        } else {
            val gap = daysBetween(prevDate!!, d)
            if (gap == 1L) {
                streak++
            } else {
                streak = 1
            }
        }
        best = maxOf(best, streak)
        prevDate = d
    }

    if (prevDate != null) {
        val gap = daysBetween(prevDate!!, today)
        cur = if (gap == 0L) streak else if (gap == 1L && !doneToday) streak else 0
    }

    return StreakInfo(cur, best, doneToday)
}

private fun daysBetween(a: String, b: String): Long {
    return try {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val d1 = fmt.parse(a)!!.time
        val d2 = fmt.parse(b)!!.time
        (d2 - d1) / 86400000L
    } catch (e: Exception) {
        0L
    }
}

private fun bn(n: Int): String {
    val map = mapOf('0' to '০', '1' to '১', '2' to '২', '3' to '৩', '4' to '৪', '5' to '৫', '6' to '৬', '7' to '৭', '8' to '৮', '9' to '৯')
    return n.toString().map { map[it] ?: it }.joinToString("")
}
