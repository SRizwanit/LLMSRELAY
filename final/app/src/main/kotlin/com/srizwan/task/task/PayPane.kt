package com.srizwan.task.task

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.srizwan.task.AppColors
import com.srizwan.task.TaskState
import com.srizwan.task.TaskViewModel
import org.json.JSONObject
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.round

data class PaySum(
    val earned: Int,
    val available: Int,
    val paid: Int,
    val requested: Int,
    val pending: Int,
    val pendingAmt: Int,
    val approved: Int,
    val correction: Int,
    val byBook: Map<String, BookPaySum>
)

data class BookPaySum(
    val n: Int,
    val approved: Int,
    val pending: Int,
    val correction: Int,
    val earned: Int
)

@Composable
fun PayPane(
    state: TaskState,
    vm: TaskViewModel,
    proofs: List<JSONObject>,
    pays: List<JSONObject>
) {
    val s = state.finData?.sum
    val booksById = state.booksById
    val me = state.me ?: return
    val S = state.settings

    val min = minPayout(S)
    val hasReq = pays.any { it.optString("status") == "requested" }
    val ok = s != null && s.available >= min && s.available > 0
    val corr = proofs.filter { it.optString("review_status") == "correction" }

    var hint = ""
    if (s != null) {
        hint = if (me.optString("payment_number").isEmpty()) {
            "আগে প্রোফাইল থেকে পেমেন্ট নাম্বার দিন।"
        } else if (ok) {
            "উত্তোলনযোগ্য ${money(s.available)} — রিকোয়েস্ট পাঠালে অ্যাডমিন অনুমোদন দিয়ে ${pmName(me.optString("payment_method"))} নাম্বার ${me.optString("payment_number")}-এ পাঠাবেন।"
        } else {
            "ন্যূনতম ${money(min)} হলে রিকোয়েস্ট করা যাবে। আরও ${money(max(0, min - s.available))} আয় দরকার।${if (hasReq) " (একটি রিকোয়েস্ট অপেক্ষমাণ আছে)" else ""}"
        }
    }

    val warn = if (s != null) {
        val reviewed = s.approved + s.correction
        val wp = S?.optInt("warn_pct", 20) ?: 20
        val wm = S?.optInt("warn_min", 5) ?: 5
        val pct = if (reviewed > 0) round(s.correction.toFloat() / reviewed * 100).toInt() else 0
        if (reviewed >= wm && pct >= wp) WarnInfo(reviewed, pct, s.correction) else null
    } else null

    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item { Spacer(Modifier.height(16.dp)) }

        if (warn != null) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(
                            text = buildAnnotatedString {
                                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("সতর্কতা:") }
                                append(" রিভিউ হওয়া ${bn(warn.reviewed)}টি পেইজের মধ্যে ${bn(warn.correction)}টিতে ভুল ধরা পড়েছে (${bn(warn.pct)}%)। দয়া করে মনোযোগ দিয়ে প্রুফ করুন — ভুল পেইজে আয় গোনা হয় না।")
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }

        item { PayTiles(s) }
        item { Spacer(Modifier.height(12.dp)) }

        if (s != null) {
            item { ForecastCard(s, min, vm::rateOf) }
            item { Spacer(Modifier.height(14.dp)) }
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "পেমেন্ট রিকোয়েস্ট",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Surface(
                            color = AppColors.extra.gold,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = if (s != null) "ডিফল্ট রেট ${rateTxt(vm.rateOf(null))} / পেইজ" else "",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }

                    Text(
                        text = hint,
                        modifier = Modifier.padding(top = 8.dp, bottom = 12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.extra.muted
                    )

                    Button(
                        onClick = { vm.requestPayment(context) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = s != null && ok && me.optString("payment_number").isNotEmpty() && !state.reqBusy,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("পেমেন্ট রিকোয়েস্ট করুন")
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        text = "পেমেন্ট ইতিহাস",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(Modifier.height(10.dp))
                    if (pays.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 18.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "এখনো কোনো পেমেন্ট ইতিহাস নেই",
                                color = AppColors.extra.muted
                            )
                        }
                    } else {
                        pays.forEach { x -> PayHistoryItem(x) }
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        text = "কিতাব অনুযায়ী আয়",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "আয় গোনা হয় শুধু অ্যাডমিন যে পেইজ অনুমোদিত করেছেন সেগুলোর।",
                        modifier = Modifier.padding(top = 4.dp, bottom = 10.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.extra.muted
                    )
                    if (s == null || s.byBook.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 18.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "এখনো কোনো পেইজ প্রুফড করেননি",
                                color = AppColors.extra.muted
                            )
                        }
                    } else {
                        s.byBook.forEach { (id, x) ->
                            val b = booksById[id]
                            BookEarnItem(b, x, vm::rateOf)
                        }
                    }
                }
            }
        }

        if (corr.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            text = "সংশোধন দরকার",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = buildAnnotatedString {
                                append("অ্যাডমিন এই পেইজগুলো সংশোধন করতে বলেছেন। ঠিক করে আবার ")
                                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("প্রুফড") }
                                append(" চাপলে পুনরায় রিভিউতে যাবে।")
                            },
                            modifier = Modifier.padding(top = 4.dp, bottom = 10.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = AppColors.extra.muted
                        )
                        corr.take(50).forEach { p ->
                            CorrectionItem(p, booksById)
                        }
                        if (corr.size > 50) {
                            Text(
                                text = "আরও ${bn(corr.size - 50)}টি আছে…",
                                modifier = Modifier.padding(top = 8.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = AppColors.extra.muted
                            )
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun PayTiles(s: PaySum?) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PayTile(
                amount = if (s != null) money(s.earned) else "—",
                label = if (s != null) "মোট আয় (অনুমোদিত ${bn(s.approved)} পেইজ)" else "মোট আয়",
                highlight = true,
                modifier = Modifier.weight(1f)
            )
            PayTile(
                amount = if (s != null) money(s.available) else "—",
                label = "উত্তোলনযোগ্য (বাকি)",
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PayTile(
                amount = if (s != null) money(s.paid) else "—",
                label = "এ পর্যন্ত পেয়েছেন",
                modifier = Modifier.weight(1f)
            )
            PayTile(
                amount = if (s != null) money(s.requested) else "—",
                label = "রিকোয়েস্টে আছে (অপেক্ষমাণ)",
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PayTile(
                amount = if (s != null) bn(s.pending) else "—",
                label = if (s != null) "রিভিউ বাকি পেইজ (${money(s.pendingAmt)})" else "রিভিউ বাকি পেইজ",
                modifier = Modifier.weight(1f)
            )
            PayTile(
                amount = if (s != null) bn(s.correction) else "—",
                label = "সংশোধন দরকার",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun PayTile(
    amount: String,
    label: String,
    highlight: Boolean = false,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (highlight) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = amount,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = if (highlight) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = if (highlight) MaterialTheme.colorScheme.onPrimaryContainer else AppColors.extra.muted
            )
        }
    }
}

@Composable
private fun ForecastCard(s: PaySum, min: Int, rateOf: (JSONObject?) -> Int) {
    val reviewedN = s.approved + s.correction
    val apr = if (reviewedN >= 3) s.approved.toFloat() / reviewedN else null
    val dr = rateOf(null)
    val allEarn = r2(s.earned + s.pendingAmt)
    val allAvail = r2(s.available + s.pendingAmt)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = "আয়ের পূর্বাভাস",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(Modifier.height(8.dp))

            if (s.pending > 0) {
                ForecastRow(
                    label = "রিভিউ বাকি ${bn(s.pending)} পেইজ সব অনুমোদিত হলে যোগ হবে",
                    value = "≈ +${money(s.pendingAmt)}",
                    up = true
                )
                ForecastRow(label = "তখন মোট আয় হবে", value = "≈ ${money(allEarn)}")
                ForecastRow(label = "তখন উত্তোলনযোগ্য হবে", value = "≈ ${money(allAvail)}")
                if (apr != null && apr < 1) {
                    ForecastRow(
                        label = "আপনার অনুমোদনের হার ${bn(round(apr * 100).toInt())}% — সেই হিসাবে যোগ হবে",
                        value = "≈ +${money((s.pendingAmt * apr).toInt())}"
                    )
                }

                val tip = when {
                    s.available >= min -> "এখনই রিকোয়েস্ট করার মতো টাকা আছে।"
                    allAvail >= min -> "রিভিউ হয়ে গেলে ন্যূনতম পেমেন্ট (${money(min)}) ছুঁয়ে রিকোয়েস্ট করতে পারবেন।"
                    else -> "ন্যূনতম পেমেন্ট (${money(min)}) ছুঁতে আরও ≈ ${bn(ceil((min - allAvail).toFloat() / dr).toInt())} পেইজ প্রুফ করতে হবে।"
                }

                Text(
                    text = "$tip অ্যাডমিন অনুমোদন না দেওয়া পর্যন্ত এটি শুধু আনুমানিক হিসাব।",
                    modifier = Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.extra.muted
                )
            } else {
                val needMore = if (s.available < min) {
                    "; ন্যূনতম পেমেন্ট (${money(min)}) ছুঁতে আরও ≈ ${bn(ceil((min - s.available).toFloat() / dr).toInt())} পেইজ দরকার"
                } else ""
                Text(
                    text = "এখন রিভিউ বাকি কোনো পেইজ নেই। আরও পেইজ প্রুফড করলে অনুমোদিত হওয়ার পর প্রতি পেইজে ≈ ${rateTxt(dr)} যোগ হবে$needMore।",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.extra.muted
                )
            }
        }
    }
}

@Composable
private fun ForecastRow(label: String, value: String, up: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = if (up) AppColors.extra.ok else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun PayHistoryItem(x: JSONObject) {
    val status = x.optString("status")
    val (st, color) = when (status) {
        "paid" -> "পরিশোধিত" to AppColors.extra.ok
        "rejected" -> "বাতিল" to MaterialTheme.colorScheme.error
        else -> "অপেক্ষমাণ (বাকি)" to AppColors.extra.warn
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = money(x.optInt("amount")),
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
            )
            Surface(
                color = color.copy(alpha = 0.2f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = st,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = color
                )
            }
        }

        Text(
            text = buildAnnotatedString {
                append("রিকোয়েস্ট: ${fmtDT(x.optString("created_at"))}")
                val proc = x.optString("processed_at", "")
                if (proc.isNotEmpty()) {
                    append("\n")
                    append("${if (status == "paid") "পরিশোধ" else "প্রক্রিয়া"}: ${fmtDT(proc)}")
                }
                append("\n")
                append("${pmName(x.optString("method"))} ${x.optString("number")}")
                val pages = x.optInt("pages", 0)
                if (pages > 0) {
                    append(" • ${bn(pages)} পেইজ")
                }
                val note = x.optString("note", "")
                if (note.isNotEmpty()) {
                    append("\nনোট: $note")
                }
            },
            modifier = Modifier.padding(top = 4.dp),
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.extra.muted
        )
    }
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 4.dp),
        color = MaterialTheme.colorScheme.outline
    )
}

@Composable
private fun BookEarnItem(b: JSONObject?, x: BookPaySum, rateOf: (JSONObject?) -> Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = b?.optString("name") ?: "কিতাব",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = money(x.earned),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
            )
        }
        Spacer(Modifier.height(6.dp))
        SegBar(a = x.approved, p = x.pending, c = x.correction, tot = x.n)
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "অনুমোদিত ${bn(x.approved)} • বাকি ${bn(x.pending)} • সংশোধন ${bn(x.correction)}",
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.extra.muted
            )
            Text(
                text = "রেট ${rateTxt(rateOf(b))}",
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.extra.muted
            )
        }
    }
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 4.dp),
        color = MaterialTheme.colorScheme.outline
    )
}

@Composable
private fun SegBar(a: Int, p: Int, c: Int, tot: Int) {
    val t = max(tot, a + p + c).coerceAtLeast(1)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        if (a > 0) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(a.toFloat() / t)
                    .background(AppColors.extra.ok)
            )
        }
        if (p > 0) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(p.toFloat() / t)
                    .background(AppColors.extra.warn)
            )
        }
        if (c > 0) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(c.toFloat() / t)
                    .background(MaterialTheme.colorScheme.error)
            )
        }
    }
}

@Composable
private fun CorrectionItem(p: JSONObject, booksById: Map<String, JSONObject>) {
    val bookId = p.optInt("book_id").toString()
    val b = booksById[bookId]
    val stem = p.optString("stem")
    val note = p.optString("review_note", "")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${b?.optString("name") ?: "কিতাব"} — $stem",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
            )
            Surface(
                modifier = Modifier.clickable {
                    // Navigate to view screen (placeholder — ধাপ 7-এ হবে)
                },
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "খুলুন",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
        if (note.isNotEmpty()) {
            Text(
                text = "অ্যাডমিন: $note",
                modifier = Modifier.padding(top = 4.dp),
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.extra.muted
            )
        }
    }
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 4.dp),
        color = MaterialTheme.colorScheme.outline
    )
}

private data class WarnInfo(val reviewed: Int, val pct: Int, val correction: Int)

fun minPayout(s: JSONObject?): Int = s?.optInt("min_payout", 100) ?: 100

private fun money(n: Int): String = "৳${bn(n)}"

private fun bn(n: Int): String {
    val s = n.toString()
    val bn = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
    return s.map { if (it.isDigit()) bn[it - '0'] else it }.joinToString("")
}

private fun r2(n: Int): Int = ((n + 0.5).toInt())

private fun pmName(m: String): String = when (m) {
    "bkash" -> "বিকাশ"
    "nagad" -> "নগদ"
    "rocket" -> "রকেট"
    else -> m
}

private fun fmtDT(s: String): String {
    if (s.isEmpty()) return ""
    return try {
        val parts = s.split("T")
        if (parts.size != 2) return s
        val date = parts[0]
        val time = parts[1].substringBefore('.')
        "$date $time"
    } catch (e: Exception) {
        s
    }
}
