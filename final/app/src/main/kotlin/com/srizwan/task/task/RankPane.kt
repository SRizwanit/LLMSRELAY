package com.srizwan.task.task

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.srizwan.task.AppColors

enum class RankPeriod(val key: String, val label: String) {
    DAY("day", "আজ"),
    WEEK("week", "এই সপ্তাহ"),
    ALL("all", "সব সময়")
}

data class RankRow(val id: Int, val n: Int)

data class RankData(
    val period: RankPeriod,
    val rows: List<RankRow>,
    val names: Map<Int, String>
)

@Composable
fun RankPane(
    hidden: Boolean,
    period: RankPeriod,
    onPeriod: (RankPeriod) -> Unit,
    loading: Boolean,
    data: RankData?,
    error: String,
    meId: Int
) {
    if (hidden) return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
            .widthIn(max = 520.dp)
            .padding(top = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // হেডার কার্ড
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFFa8761f),
                                    Color(0xFFf2d27a)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🏆",
                        fontSize = 32.sp
                    )
                }

                Spacer(Modifier.height(12.dp))

                Text(
                    text = "লিডারবোর্ড",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(Modifier.height(4.dp))

                // নিজের অবস্থান
                val myRank = if (data != null) {
                    val idx = data.rows.indexOfFirst { it.id == meId }
                    val lab = when (data.period) {
                        RankPeriod.DAY -> "আজ"
                        RankPeriod.WEEK -> "এই সপ্তাহে"
                        RankPeriod.ALL -> "সব মিলিয়ে"
                    }
                    when {
                        idx < 0 -> "$lab আপনার কোনো পেইজ নেই — একটি পেইজ প্রুফ করলেই তালিকায় আসবেন!"
                        idx == 0 -> "$lab আপনি ১ নম্বরে! মাশাআল্লাহ, এগিয়ে থাকুন।"
                        else -> {
                            val behind = data.rows[idx - 1].n - data.rows[idx].n
                            "$lab আপনার অবস্থান ${bn(idx + 1)} নম্বর • পরের জনের থেকে ${bn(behind)} পেইজ পিছনে"
                        }
                    }
                } else ""

                Text(
                    text = myRank,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.extra.muted,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        // পিরিয়ড চিপস
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            RankPeriod.entries.forEach { p ->
                val sel = period == p
                FilterChip(
                    selected = sel,
                    onClick = { onPeriod(p) },
                    label = {
                        Text(
                            text = p.label,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    modifier = Modifier.padding(horizontal = 4.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // লোডিং
        if (loading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(32.dp),
                    strokeWidth = 3.dp
                )
            }
        }

        // তালিকা বা এরর
        when {
            error.isNotEmpty() -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Text(
                        text = "লিডারবোর্ড আনা যায়নি: $error",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.extra.muted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(20.dp)
                    )
                }
            }
            data != null -> {
                if (data.rows.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Text(
                            text = "এই সময়ে এখনো কেউ কোনো পেইজ প্রুফড করেননি — প্রথম হওয়ার সুযোগ আপনার!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppColors.extra.muted,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(20.dp)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // টপ ২০
                        itemsIndexed(data.rows.take(20)) { i, r ->
                            RankRowItem(r, i, meId, data.names)
                        }

                        // নিজের সারি যদি ২০-এর পরে
                        val idx = data.rows.indexOfFirst { it.id == meId }
                        if (idx >= 20) {
                            item {
                                Text(
                                    text = "⋮",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = AppColors.extra.muted,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp)
                                )
                            }
                            item {
                                RankRowItem(data.rows[idx], idx, meId, data.names)
                            }
                        }

                        item { Spacer(Modifier.height(80.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun RankRowItem(r: RankRow, i: Int, meId: Int, names: Map<Int, String>) {
    val mine = r.id == meId
    val topThree = i < 3

    val bg = when {
        mine -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        topThree -> when (i) {
            0 -> Color(0xFFFFD700).copy(alpha = 0.15f)
            1 -> Color(0xFFC0C0C0).copy(alpha = 0.15f)
            2 -> Color(0xFFCD7F32).copy(alpha = 0.15f)
            else -> MaterialTheme.colorScheme.surface
        }
        else -> MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = bg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // র‍্যাংক
            Text(
                text = bn(i + 1),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (topThree) FontWeight.Bold else FontWeight.Normal,
                color = if (topThree) {
                    when (i) {
                        0 -> Color(0xFFB8860B)
                        1 -> Color(0xFF808080)
                        2 -> Color(0xFF8B4513)
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                } else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.width(40.dp)
            )

            // নাম
            Text(
                text = (names[r.id] ?: "ইউজার") + if (mine) " (আপনি)" else "",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (mine) FontWeight.SemiBold else FontWeight.Normal,
                color = if (mine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            )

            // পেইজ কাউন্ট
            Text(
                text = "${bn(r.n)} পেইজ",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = AppColors.extra.muted
            )
        }
    }
}

private fun bn(n: Int): String {
    val bengaliDigits = arrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
    return n.toString().map { c ->
        if (c.isDigit()) bengaliDigits[c - '0'] else c
    }.joinToString("")
}
