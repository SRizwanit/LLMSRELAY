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
    period: RankPeriod,
    onPeriod: (RankPeriod) -> Unit,
    loading: Boolean,
    data: RankData?,
    error: String,
    meId: Int
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(16.dp))

        // Header Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 2.dp
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
                        fontSize = 32.sp
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "লিডারবোর্ড",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))

                val myRank = if (data != null) {
                    val idx = data.rows.indexOfFirst { it.id == meId }
                    val lab = data.period.label
                    when {
                        idx < 0 -> "$lab আপনার কোনো পেইজ নেই — একটি পেইজ প্রুফ করলেই তালিকায় আসবেন!"
                        idx == 0 -> "$lab আপনি ১ নম্বরে! মাশাআল্লাহ, এগিয়ে থাকুন।"
                        else -> {
                            val gap = data.rows[idx - 1].n - data.rows[idx].n
                            "$lab আপনার অবস্থান ${bn(idx + 1)} নম্বর • পরের জনের থেকে ${bn(gap)} পেইজ পিছনে"
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

        // Period Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            RankPeriod.entries.forEach { p ->
                val selected = p == period
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

        // Loading / Error / List
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.TopCenter
        ) {
            when {
                loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(40.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                error.isNotEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
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

                data != null -> {
                    if (data.rows.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "এই সময়ে এখনো কেউ কোনো পেইজ প্রুফড করেননি — প্রথম হওয়ার সুযোগ আপনার!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = AppColors.extra.muted,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    } else {
                        val idx = data.rows.indexOfFirst { it.id == meId }
                        val topRows = data.rows.take(20)
                        val needEllipsis = idx >= 20

                        LazyColumn(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            itemsIndexed(topRows) { i, r ->
                                RankRowItem(r, i, meId, data.names)
                            }

                            if (needEllipsis && idx >= 20) {
                                item {
                                    Text(
                                        text = "⋮",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp),
                                        textAlign = TextAlign.Center,
                                        color = AppColors.extra.muted,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                                item {
                                    RankRowItem(data.rows[idx], idx, meId, data.names)
                                }
                            }

                            item { Spacer(Modifier.height(16.dp)) }
                        }
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

    val bgColor = when {
        mine -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        topThree -> when (i) {
            0 -> Color(0xFFfef3c7)
            1 -> Color(0xFFe5e5e5)
            2 -> Color(0xFFfed7aa)
            else -> MaterialTheme.colorScheme.surfaceVariant
        }
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = bgColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rank
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        if (topThree) {
                            when (i) {
                                0 -> Color(0xFFf59e0b)
                                1 -> Color(0xFF9ca3af)
                                2 -> Color(0xFFf97316)
                                else -> MaterialTheme.colorScheme.surface
                            }
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = bn(i + 1),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (topThree) Color.White else MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(Modifier.width(12.dp))

            // Name
            Text(
                text = (names[r.id] ?: "ইউজার") + if (mine) " (আপনি)" else "",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (mine) FontWeight.Bold else FontWeight.Normal,
                color = if (mine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )

            // Count
            Text(
                text = "${bn(r.n)} পেইজ",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (mine) FontWeight.Bold else FontWeight.Normal,
                color = if (mine) MaterialTheme.colorScheme.primary else AppColors.extra.muted
            )
        }
    }
}

private fun bn(n: Int): String {
    val bengali = arrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
    return n.toString().map { if (it.isDigit()) bengali[it - '0'] else it }.joinToString("")
}
