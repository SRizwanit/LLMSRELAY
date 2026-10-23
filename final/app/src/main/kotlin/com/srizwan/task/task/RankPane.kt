package com.srizwan.task.task

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.srizwan.task.AppColors
import com.srizwan.task.R

enum class RankPeriod(val key: String, val label: String) {
    DAY("day", "আজ"),
    WEEK("week", "এই সপ্তাহ"),
    ALL("all", "সর্বকালীন")
}

data class RankRow(val id: Int, val n: Int)

data class RankData(
    val all: List<RankRow>,
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
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            RankPeriod.entries.forEach { p ->
                Button(
                    onClick = { onPeriod(p) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (period == p) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (period == p) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text(p.label, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        when {
            loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            error.isNotEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "তথ্য লোড করা যায়নি: $error",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.extra.muted,
                        textAlign = TextAlign.Center
                    )
                }
            }
            data == null || data.all.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            painter = painterResource(R.drawable.rizwantask),
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = AppColors.extra.muted
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "কোনো ডেটা নেই",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppColors.extra.muted
                        )
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(data.all) { index, row ->
                        RankRowItem(row, index, meId, data.names)
                    }
                    item { Spacer(Modifier.height(0.dp)) }
                }
            }
        }
    }
}

@Composable
private fun RankRowItem(r: RankRow, i: Int, meId: Int, names: Map<Int, String>) {
    val isMe = r.id == meId
    val bg = if (isMe) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
    else MaterialTheme.colorScheme.surface

    val borderColor = if (isMe) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
    else Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .then(
                if (isMe) Modifier.border(1.dp, borderColor, RoundedCornerShape(12.dp))
                else Modifier
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val rankColor = when (i) {
            0 -> Color(0xFFFFD700)
            1 -> Color(0xFFC0C0C0)
            2 -> Color(0xFFCD7F32)
            else -> MaterialTheme.colorScheme.primary
        }

        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(rankColor.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = when (i) {
                    0 -> "🥇"
                    1 -> "🥈"
                    2 -> "🥉"
                    else -> bn(i + 1)
                },
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = if (i < 3) rankColor else MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = names[r.id] ?: "ইউজার ${r.id}",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            if (isMe) {
                Text(
                    text = "আপনি",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(Modifier.width(8.dp))

        Text(
            text = "${bn(r.n)} পেইজ",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )
    }
}

private fun bn(n: Int): String {
    val digits = "০১২৩৪৫৬৭৮৯"
    return n.toString().map { if (it.isDigit()) digits[it - '0'] else it }.joinToString("")
}
