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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.srizwan.task.AppColors
import com.srizwan.task.R
import com.srizwan.task.TaskState
import com.srizwan.task.TaskViewModel
import org.json.JSONObject

@Composable
fun PayPane(
    state: TaskState,
    vm: TaskViewModel,
    proofs: List<JSONObject>,
    pays: List<JSONObject>
) {
    val money = state.money
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(Modifier.height(0.dp)) }

        if (state.moneyLoading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        } else if (state.moneyError.isNotEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "পেমেন্ট তথ্য আনা যায়নি: ${state.moneyError}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.extra.muted,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else if (money != null) {
            val sum = PaySum(
                money.approved,
                money.pending,
                money.corrected,
                money.total,
                money.canReq
            )

            item { PayTiles(sum) }

            if (sum.canReq) {
                item {
                    Button(
                        onClick = { /* vm.requestPayment(context) চাইলে এখানে */ },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.rizwantask),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("পেমেন্টের জন্য অনুরোধ করুন")
                    }
                }
            }

            if (money.corrections.isNotEmpty()) {
                item {
                    Text(
                        text = "✏️ সংশোধনী",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
                items(money.corrections) { p ->
                    CorrectionItem(p, state.booksById)
                }
            }

            if (pays.isNotEmpty()) {
                item {
                    Text(
                        text = "💰 পেমেন্ট ইতিহাস",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
                items(pays) { x ->
                    PayHistoryItem(x)
                }
            }

            if (money.bookSums.isNotEmpty()) {
                item {
                    Text(
                        text = "📚 কিতাব অনুযায়ী",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
                items(money.bookSums) { bs ->
                    BookEarnItem(state.booksById[bs.bookId], bs, vm::rateOf)
                }
            }
        }

        item { Spacer(Modifier.height(0.dp)) }
    }
}

data class PaySum(
    val approved: Int,
    val pending: Int,
    val corrected: Int,
    val total: Int,
    val canReq: Boolean
)

@Composable
private fun PayTiles(s: PaySum?) {
    if (s == null) return
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        PayTile(
            label = "অনুমোদিত",
            value = money(s.approved),
            color = AppColors.extra.ok,
            modifier = Modifier.weight(1f)
        )
        PayTile(
            label = "পেন্ডিং",
            value = money(s.pending),
            color = AppColors.extra.warn,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun PayTile(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.1f))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = color
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = color
            )
        }
    }
}

@Composable
private fun PayHistoryItem(x: JSONObject) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val status = x.optString("status", "pending")
        val color = when (status) {
            "approved" -> AppColors.extra.ok
            "rejected" -> MaterialTheme.colorScheme.error
            else -> AppColors.extra.warn
        }

        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.rizwantask),
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = money(x.optInt("amount", 0)),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )
            Text(
                text = fmtDT(x.optString("created_at", "")),
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.extra.muted
            )
        }

        Spacer(Modifier.width(8.dp))

        Text(
            text = when (status) {
                "approved" -> "✅ পরিশোধিত"
                "rejected" -> "❌ বাতিল"
                else -> "⏳ পেন্ডিং"
            },
            style = MaterialTheme.typography.bodySmall,
            color = color
        )
    }
}

@Composable
private fun BookEarnItem(b: JSONObject?, x: com.srizwan.task.BookPaySum, rateOf: (JSONObject?) -> Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = b?.optString("name", "কিতাব") ?: "কিতাব",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "মোট: ${money(x.total)}",
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.extra.muted
            )
            Spacer(Modifier.height(8.dp))
            SegBar(x.approved, x.pending, x.corrected, x.total)
        }
    }
}

@Composable
private fun SegBar(a: Int, p: Int, c: Int, tot: Int) {
    if (tot == 0) return
    val aPct = a.toFloat() / tot
    val pPct = p.toFloat() / tot

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            if (a > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(aPct)
                        .height(8.dp)
                        .background(AppColors.extra.ok)
                )
            }
            if (p > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(pPct / (1f - aPct))
                        .height(8.dp)
                        .background(AppColors.extra.warn)
                )
            }
        }
    }
}

@Composable
private fun CorrectionItem(p: JSONObject, booksById: Map<String, JSONObject>) {
    val bookId = p.optString("book_id", "")
    val book = booksById[bookId]
    val page = p.optInt("page_number", 0)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f))
            .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = book?.optString("name", "কিতাব") ?: "কিতাব",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
            )
            Text(
                text = "পেইজ ${bn(page)}",
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.extra.muted
            )
            val note = p.optString("admin_note", "")
            if (note.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "নোট: $note",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

private fun money(n: Int): String = "৳${bn(n)}"

private fun bn(n: Int): String {
    val digits = "০১২৩৪৫৬৭৮৯"
    return n.toString().map { if (it.isDigit()) digits[it - '0'] else it }.joinToString("")
}

private fun fmtDT(s: String): String {
    if (s.isBlank()) return ""
    return try {
        val parts = s.substringBefore('T').split('-')
        if (parts.size == 3) "${parts[2]}/${parts[1]}/${parts[0]}" else s
    } catch (e: Exception) {
        s
    }
}
