package com.srizwan.task.task

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.srizwan.task.AppColors
import com.srizwan.task.R
import org.json.JSONObject

data class ResumeTarget(val book: JSONObject, val stem: String)

data class StreakInfo(
    val today: Int,
    val cur: Int,
    val best: Int,
    val doneToday: Boolean
)

val BADGES = listOf(
    3 to "🥉 ব্রোঞ্জ",
    7 to "🥈 সিলভার",
    14 to "🥇 গোল্ড",
    30 to "💎 ডায়মন্ড",
    60 to "👑 প্ল্যাটিনাম",
    100 to "🏆 লিজেন্ড"
)

@Composable
fun TaskDashboard(
    ready: Boolean,
    proofs: List<JSONObject>,
    booksById: Map<String, JSONObject>,
    goal: Int,
    resume: JSONObject?,
    onGoal: () -> Unit,
    onResume: () -> Unit
) {
    val rt = resumeTarget(resume, booksById, proofs)
    val hasStat = ready && (proofs.isEmpty() || proofs[0].optString("created_at", "").isNotEmpty())

    if (!hasStat && rt == null) {
        return Box(modifier = Modifier.fillMaxWidth())
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 12.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        if (hasStat) {
            val s = streakInfo(proofs)
            val left = maxOf(0, goal - s.today)
            val hit = s.today >= goal
            val pct = minOf(100f, s.today.toFloat() / goal * 100f)
            val next = BADGES.find { it.first > s.best }

            GoalCard(
                today = s.today,
                goal = goal,
                hit = hit,
                pct = pct,
                left = left,
                onGoal = onGoal
            )

            Spacer(Modifier.height(12.dp))

            StreakCard(
                cur = s.cur,
                best = s.best,
                doneToday = s.doneToday
            )

            Spacer(Modifier.height(12.dp))

            BadgeRow(best = s.best)

            if (next != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "পরের ব্যাজ \"${next.second}\": টানা ${bn(next.first)} দিন (সেরা রেকর্ড থেকে আরও ${bn(next.first - s.best)} দিন)।",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.extra.muted,
                    fontSize = 13.sp
                )
            }
        }

        if (rt != null) {
            if (hasStat) Spacer(Modifier.height(16.dp))

            Button(
                onClick = onResume,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    painter = painterResource(id = android.R.drawable.ic_media_play),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "চালিয়ে যান — ${rt.book.optString("name", "")}${if (rt.stem.isNotEmpty()) " • পেইজ $rt.stem" else ""}",
                    fontSize = 15.sp
                )
            }
        }
    }
}

@Composable
private fun GoalCard(today: Int, goal: Int, hit: Boolean, pct: Float, left: Int, onGoal: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (hit) AppColors.extra.ok.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                role = Role.Button,
                onClick = onGoal
            )
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "🎯", fontSize = 16.sp)
            Spacer(Modifier.width(6.dp))
            Text(
                text = "আজকের লক্ষ্য",
                style = MaterialTheme.typography.labelMedium,
                color = AppColors.extra.muted,
                fontSize = 13.sp
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = "${bn(today)} / ${bn(goal)} পেইজ",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(MaterialTheme.colorScheme.outline)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(pct / 100f)
                    .height(6.dp)
                    .background(if (hit) AppColors.extra.ok else MaterialTheme.colorScheme.primary)
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = if (hit) "লক্ষ্য পূরণ হয়েছে ✓ • বদলাতে চাপুন" else "আর ${bn(left)} পেইজ বাকি • বদলাতে চাপুন",
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.extra.muted,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun StreakCard(cur: Int, best: Int, doneToday: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "⚡", fontSize = 16.sp)
            Spacer(Modifier.width(6.dp))
            Text(
                text = "টানা কাজ",
                style = MaterialTheme.typography.labelMedium,
                color = AppColors.extra.muted,
                fontSize = 13.sp
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = "${bn(cur)} দিন",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = when {
                doneToday -> "আজকের দিন গোনা হয়েছে • সেরা ${bn(best)} দিন"
                cur > 0 -> "আজ কাজ করলে ${bn(cur + 1)} দিন হবে • সেরা ${bn(best)} দিন"
                else -> "আজ একটি পেইজ করলেই শুরু • সেরা ${bn(best)} দিন"
            },
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.extra.muted,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun BadgeRow(best: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        BADGES.forEach { (threshold, label) ->
            val on = best >= threshold
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (on) AppColors.extra.gold.copy(alpha = 0.15f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    .padding(vertical = 6.dp, horizontal = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label.split(" ").first(),
                    fontSize = 16.sp,
                    color = if (on) AppColors.extra.gold else AppColors.extra.muted.copy(alpha = 0.5f)
                )
            }
        }
    }
}

fun resumeTarget(resume: JSONObject?, booksById: Map<String, JSONObject>, proofs: List<JSONObject>): ResumeTarget? {
    if (resume == null) return null
    val bid = resume.optString("b", "")
    if (bid.isEmpty()) return null
    val book = booksById[bid] ?: return null
    var stem = ""
    val p = resume.optInt("p", 0)
    if (p > 0) {
        val done = proofs.filter { it.optInt("book_id", 0).toString() == bid && it.optInt("page_num", 0) == p }
        if (done.isEmpty()) stem = bn(p)
    }
    return ResumeTarget(book, stem)
}

fun streakInfo(proofs: List<JSONObject>): StreakInfo {
    val dates = proofs.mapNotNull {
        val t = it.optString("created_at", "")
        if (t.isEmpty()) null else t.substring(0, 10)
    }.distinct().sorted()

    var today = 0
    val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).apply {
        timeZone = java.util.TimeZone.getTimeZone("UTC")
    }.format(java.util.Date())

    if (dates.isNotEmpty() && dates.last() == todayStr) {
        today = proofs.count { it.optString("created_at", "").startsWith(todayStr) }
    }

    val doneToday = today > 0

    var cur = 0
    var best = 0
    var run = 0
    var prev = ""

    for (d in dates) {
        if (prev.isEmpty()) {
            run = 1
        } else {
            val gap = daysBetween(prev, d)
            run = if (gap == 1L) run + 1 else 1
        }
        if (run > best) best = run
        prev = d
    }

    if (dates.isNotEmpty()) {
        val gap = daysBetween(dates.last(), todayStr)
        cur = if (gap == 0L) run else if (gap == 1L && run > 0) run else 0
    }

    return StreakInfo(today, cur, best, doneToday)
}

private fun daysBetween(a: String, b: String): Long {
    val fmt = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).apply {
        timeZone = java.util.TimeZone.getTimeZone("UTC")
    }
    val d1 = fmt.parse(a)?.time ?: 0L
    val d2 = fmt.parse(b)?.time ?: 0L
    return (d2 - d1) / 86400000L
}

private fun bn(n: Int): String {
    val bengali = arrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
    return n.toString().map { if (it.isDigit()) bengali[it - '0'] else it }.joinToString("")
}
