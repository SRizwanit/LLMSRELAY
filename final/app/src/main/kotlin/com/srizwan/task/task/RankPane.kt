package com.srizwan.task.task

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Spacer(Modifier.height(16.dp))

            // Header card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFFa8761f), Color(0xFFf2d27a))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "🏆",
                            fontSize = 32.sp,
                            color = Color(0xFF4a3200)
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "লিডারবোর্ড",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(8.dp))

                    val myRank = if (data != null) {
                        val idx = data.rows.indexOfFirst { it.id == meId }
                        val lab = data.period.label
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
            Spacer(Modifier.height(16.dp))
        }

        item {
            // Period chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                RankPeriod.entries.forEach { p ->
                    val selected = period == p
                    Surface(
                        onClick = { onPeriod(p) },
                        shape = RoundedCornerShape(20.dp),
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        Text(
                            text = p.label,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        if (loading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        if (error.isNotEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "লিডারবোর্ড আনা যায়নি: $error",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.extra.muted,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        if (data != null && !loading && error.isEmpty()) {
            if (data.rows.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "এই সময়ে এখনো কেউ কোনো পেইজ প্রুফড করেননি — প্রথম হওয়ার সুযোগ আপনার!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppColors.extra.muted,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                val myIdx = data.rows.indexOfFirst { it.id == meId }
                val top20 = data.rows.take(20)

                itemsIndexed(top20) { i, r ->
                    RankRowItem(r = r, i = i, meId = meId, names = data.names)
                }

                if (myIdx >= 20) {
                    item {
                        Text(
                            text = "⋮",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodySmall,
                            color = AppColors.extra.muted
                        )
                    }
                    item {
                        RankRowItem(
                            r = data.rows[myIdx],
                            i = myIdx,
                            meId = meId,
                            names = data.names
                        )
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun RankRowItem(r: RankRow, i: Int, meId: Int, names: Map<Int, String>) {
    val mine = r.id == meId
    val rankColor = when (i) {
        0 -> Color(0xFFFFD700) // Gold
        1 -> Color(0xFFC0C0C0) // Silver
        2 -> Color(0xFFCD7F32) // Bronze
        else -> MaterialTheme.colorScheme.onSurface
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        color = if (mine) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        tonalElevation = if (mine) 4.dp else 1.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = bn(i + 1),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (i < 3) FontWeight.Bold else FontWeight.Normal,
                color = rankColor,
                modifier = Modifier.width(40.dp)
            )
            Text(
                text = (names[r.id] ?: "ইউজার") + if (mine) " (আপনি)" else "",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (mine) FontWeight.SemiBold else FontWeight.Normal,
                modifier = Modifier.weight(1f),
                color = if (mine) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${bn(r.n)} পেইজ",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = if (mine) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun bn(n: Int): String {
    val bengali = arrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
    return n.toString().map { if (it.isDigit()) bengali[it - '0'] else it }.joinToString("")
}
