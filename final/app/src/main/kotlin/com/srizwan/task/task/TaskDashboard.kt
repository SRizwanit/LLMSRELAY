package com.srizwan.task.task

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.srizwan.task.AppColors
import com.srizwan.task.R
import com.srizwan.task.TaskState
import org.json.JSONObject

data class ResumeTarget(val book: JSONObject, val stem: String)

data class StreakInfo(
    val current: Int,
    val best: Int,
    val doneToday: Boolean
)

@Composable
fun TaskDashboard(
    state: TaskState,
    onGoal: () -> Unit,
    resume: ResumeTarget?,
    onResume: (String, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val pct = if (state.goal > 0) state.todayCount.toFloat() / state.goal else 0f
    val left = (state.goal - state.todayCount).coerceAtLeast(0)
    val streak = streakInfo(state.proofs)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Spacer(Modifier.height(4.dp))
        GoalCard(state.todayCount, state.goal, state.goalHit, pct, left, onGoal)
        StreakCard(streak.current, streak.best, streak.doneToday)
        
        if (resume != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                        RoundedCornerShape(12.dp)
                    )
                    .clickable {
                        val id = resume.book.getString("id")
                        val done = state.proofs.count { it.optString("book_id") == id }
                        onResume(id, done + 1)
                    }
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(R.drawable.rizwantask),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "চালিয়ে যান",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppColors.extra.muted
                        )
                        Text(
                            text = resume.stem,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GoalCard(today: Int, goal: Int, hit: Boolean, pct: Float, left: Int, onGoal: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "আজকের লক্ষ্য",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "${bn(today)}/${bn(goal)}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (hit) AppColors.extra.ok else MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = { pct.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = if (hit) AppColors.extra.ok else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = if (hit) "🎉 আজকের লক্ষ্য পূরণ হয়েছে! মাশাআল্লাহ!" else "আরও ${bn(left)} পেইজ বাকি",
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.extra.muted
            )
        }
    }
}

@Composable
private fun StreakCard(cur: Int, best: Int, doneToday: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
                    )
                )
            )
            .border(
                1.dp,
                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                RoundedCornerShape(12.dp)
            )
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "🔥 টানা দিন",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${bn(cur)} দিন",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (doneToday) "আজ কমপক্ষে ১টি পেইজ করেছেন" else "আজ এখনো কোনো পেইজ করেননি",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.extra.muted
                )
            }

            BadgeRow(best)
        }
    }
}

@Composable
private fun BadgeRow(best: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(7, 30, 100).forEach { m ->
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (best >= m) Brush.linearGradient(
                            listOf(Color(0xFFa8761f), Color(0xFFf2d27a))
                        )
                        else MaterialTheme.colorScheme.surfaceVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when (m) {
                        7 -> "🥉"
                        30 -> "🥈"
                        100 -> "🥇"
                        else -> ""
                    },
                    fontSize = 18.sp
                )
            }
        }
    }
}

fun resumeTarget(resume: JSONObject?, booksById: Map<String, JSONObject>, proofs: List<JSONObject>): ResumeTarget? {
    if (resume == null) return null
    val id = resume.optString("id", "")
    val name = resume.optString("name", "")
    val done = proofs.count { it.optString("book_id") == id }
    val total = resume.optInt("total_pages", 0)
    val stem = "$name · পেইজ ${bn(done + 1)}/$total"
    return ResumeTarget(resume, stem)
}

fun streakInfo(proofs: List<JSONObject>): StreakInfo {
    if (proofs.isEmpty()) return StreakInfo(0, 0, false)

    val dates = proofs.mapNotNull {
        it.optString("created_at", "").substringBefore('T').takeIf { d -> d.isNotBlank() }
    }.distinct().sortedDescending()

    if (dates.isEmpty()) return StreakInfo(0, 0, false)

    val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
    val doneToday = dates.firstOrNull() == today

    var cur = 0
    var best = 0
    var temp = 0
    var prev: String? = null

    for (d in dates) {
        if (prev == null) {
            temp = 1
        } else {
            val gap = daysBetween(d, prev)
            temp = if (gap == 1L) temp + 1 else 1
        }
        if (temp > best) best = temp
        prev = d
    }

    cur = if (doneToday) {
        temp
    } else {
        val yesterday = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(
            java.util.Date(System.currentTimeMillis() - 86400000L)
        )
        if (dates.firstOrNull() == yesterday) temp else 0
    }

    return StreakInfo(cur, best, doneToday)
}

private fun daysBetween(a: String, b: String): Long {
    val fmt = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
    val d1 = fmt.parse(a)?.time ?: 0L
    val d2 = fmt.parse(b)?.time ?: 0L
    return ((d2 - d1) / 86400000L)
}

private fun bn(n: Int): String {
    val digits = "০১২৩৪৫৬৭৮৯"
    return n.toString().map { if (it.isDigit()) digits[it - '0'] else it }.joinToString("")
}
