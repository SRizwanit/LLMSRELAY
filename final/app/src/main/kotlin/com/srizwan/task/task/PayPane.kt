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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.srizwan.task.AppColors
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

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
    hidden: Boolean,
    me: JSONObject,
    settings: JSONObject?,
    sum: PaySum?,
    booksById: Map<String, JSONObject>,
    proofs: List<JSONObject>,
    pays: List<JSONObject>,
    rateOf: (JSONObject?) -> Int,
    reqBusy: Boolean,
    onRequest: () -> Unit
) {
    if (hidden) return

    val s = sum
    val min = minPayout(settings)
    val corr = s?.let { proofs.filter { it.optString("review_status") == "correction" } } ?: emptyList()
    val hasReq = pays.any { it.optString("status") == "requested" }
    val ok = s != null && s.available >= min && s.available > 0

    val hint = if (s != null) {
        when {
            me.optString("payment_number", "").isEmpty() -> "আগে প্রোফাইল থেকে পেমেন্ট নাম্বার দিন।"
            ok -> "উত্তোলনযোগ্য ${money(s.available)} — রিকোয়েস্ট পাঠালে অ্যাডমিন অনুমোদন দিয়ে ${pmName(me.optString("payment_method"))} নাম্বার ${me.optString("payment_number")}-এ পাঠাবেন।"
            else -> "ন্যূনতম ${money(min)} হলে রিকোয়েস্ট করা যাবে। আরও ${money(maxOf(0, min - s.available))} আয় দরকার।${if (hasReq) " (একটি রিকোয়েস্ট অপেক্ষমাণ আছে)" else ""}"
        }
    } else ""

    val warn = s?.let {
        val reviewed = it.approved + it.correction
        val wp = settings?.optInt("warn_pct", 20) ?: 20
        val wm = settings?.optInt("warn_min", 5) ?: 5
        val pct = if (reviewed > 0) (it.correction * 100) / reviewed else 0
        if (reviewed >= wm && pct >= wp) WarnInfo(reviewed, pct, it.correction) else null
    }

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
            if (warn != null) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEE2E2)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "সতর্কতা:",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFDC2626),
                            fontSize = 14.sp
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "রিভিউ হওয়া ${bn(warn.reviewed)}টি পেইজের মধ্যে ${bn(warn.correction)}টিতে ভুল ধরা পড়েছে (${bn(warn.pct)}%)। দয়া করে মনোযোগ দিয়ে প্রুফ করুন — ভুল পেইজে আয় গোনা হয় না।",
                            fontSize = 13.sp,
                            color = Color(0xFF991B1B),
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            if (s == null) {
                PayTilesSkeleton()
            } else {
                PayTiles(s)
            }

            Spacer(Modifier.height(12.dp))

            if (s != null) {
                ForecastCard(s = s, min = min, rateOf = rateOf)
                Spacer(Modifier.height(14.dp))
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "পেমেন্ট রিকোয়েস্ট",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            modifier = Modifier.weight(1f)
                        )
                        if (s != null) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = AppColors.extra.gold
                            ) {
                                Text(
                                    text = "ডিফল্ট রেট ${rateTxt(rateOf(null))} / পেইজ",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    fontSize = 12.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = hint,
                        fontSize = 13.sp,
                        color = AppColors.extra.muted,
                        lineHeight = 18.sp
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = onRequest,
                        enabled = !reqBusy && s != null && ok && me.optString("payment_number", "").isNotEmpty(),
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("পেমেন্ট রিকোয়েস্ট করুন", fontSize = 15.sp)
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "পেমেন্ট ইতিহাস",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(Modifier.height(10.dp))
                    if (s != null) {
                        if (pays.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "এখনো কোনো পেমেন্ট ইতিহাস নেই",
                                    fontSize = 13.sp,
                                    color = AppColors.extra.muted
                                )
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                pays.forEach { x ->
                                    PayHistoryItem(x)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "কিতাব অনুযায়ী আয়",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "আয় গোনা হয় শুধু অ্যাডমিন যে পেইজ অনুমোদিত করেছেন সেগুলোর।",
                        fontSize = 12.sp,
                        color = AppColors.extra.muted,
                        modifier = Modifier.padding(top = 4.dp, bottom = 10.dp),
                        lineHeight = 16.sp
                    )
                    if (s != null) {
                        val ids = s.byBook.keys.toList()
                        if (ids.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "এখনো কোনো পেইজ প্রুফড করেননি",
                                    fontSize = 13.sp,
                                    color = AppColors.extra.muted
                                )
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                ids.forEach { id ->
                                    val b = booksById[id]
                                    val x = s.byBook[id]!!
                                    BookEarnItem(b, x, rateOf)
                                }
                            }
                        }
                    }
                }
            }

            if (corr.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "সংশোধন দরকার",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "অ্যাডমিন এই পেইজগুলো সংশোধন করতে বলেছেন। ঠিক করে আবার প্রুফড চাপলে পুনরায় রিভিউতে যাবে।",
                            fontSize = 12.sp,
                            color = AppColors.extra.muted,
                            modifier = Modifier.padding(top = 4.dp, bottom = 10.dp),
                            lineHeight = 16.sp
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            corr.take(50).forEach { p ->
                                CorrectionItem(p, booksById)
                            }
                        }
                        if (corr.size > 50) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "আরও ${bn(corr.size - 50)}টি আছে…",
                                fontSize = 12.sp,
                                color = AppColors.extra.muted
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun PayTiles(s: PaySum) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PayTile(
            amount = money(s.earned),
            label = "মোট আয় (অনুমোদিত ${bn(s.approved)} পেইজ)",
            highlight = true
        )
        PayTile(
            amount = money(s.available),
            label = "উত্তোলনযোগ্য (বাকি)"
        )
        PayTile(
            amount = money(s.paid),
            label = "এ পর্যন্ত পেয়েছেন"
        )
        PayTile(
            amount = money(s.requested),
            label = "রিকোয়েস্টে আছে (অপেক্ষমাণ)"
        )
        PayTile(
            amount = bn(s.pending),
            label = "রিভিউ বাকি পেইজ (${money(s.pendingAmt)})"
        )
        PayTile(
            amount = bn(s.correction),
            label = "সংশোধন দরকার"
        )
    }
}

@Composable
private fun PayTile(amount: String, label: String, highlight: Boolean = false) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (highlight) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = amount,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = label,
                fontSize = 13.sp,
                color = AppColors.extra.muted
            )
        }
    }
}

@Composable
private fun PayTilesSkeleton() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(4) {
            Card(
                modifier = Modifier.fillMaxWidth().height(70.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(10.dp)
            ) {}
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
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "আয়ের পূর্বাভাস",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Spacer(Modifier.height(10.dp))

            if (s.pending > 0) {
                ForecastRow("রিভিউ বাকি ${bn(s.pending)} পেইজ সব অনুমোদিত হলে যোগ হবে", "≈ +${money(s.pendingAmt)}", up = true)
                Spacer(Modifier.height(6.dp))
                ForecastRow("তখন মোট আয় হবে", "≈ ${money(allEarn)}")
                Spacer(Modifier.height(6.dp))
                ForecastRow("তখন উত্তোলনযোগ্য হবে", "≈ ${money(allAvail)}")
                if (apr != null && apr < 1f) {
                    Spacer(Modifier.height(6.dp))
                    ForecastRow(
                        "আপনার অনুমোদনের হার ${bn((apr * 100).toInt())}% — সেই হিসাবে যোগ হবে",
                        "≈ +${money((s.pendingAmt * apr).toInt())}"
                    )
                }
                Spacer(Modifier.height(8.dp))
                val tip = when {
                    s.available >= min -> "এখনই রিকোয়েস্ট করার মতো টাকা আছে।"
                    allAvail >= min -> "রিভিউ হয়ে গেলে ন্যূনতম পেমেন্ট (${money(min)}) ছুঁয়ে রিকোয়েস্ট করতে পারবেন।"
                    else -> "ন্যূনতম পেমেন্ট (${money(min)}) ছুঁতে আরও ≈ ${bn((min - allAvail + dr - 1) / dr)} পেইজ প্রুফ করতে হবে।"
                }
                Text(
                    text = "$tip অ্যাডমিন অনুমোদন না দেওয়া পর্যন্ত এটি শুধু আনুমানিক হিসাব।",
                    fontSize = 12.sp,
                    color = AppColors.extra.muted,
                    lineHeight = 17.sp
                )
            } else {
                Text(
                    text = "এখন রিভিউ বাকি কোনো পেইজ নেই। আরও পেইজ প্রুফড করলে অনুমোদিত হওয়ার পর প্রতি পেইজে ≈ ${rateTxt(dr)} যোগ হবে" +
                            (if (s.available < min) "; ন্যূনতম পেমেন্ট (${money(min)}) ছুঁতে আরও ≈ ${bn((min - s.available + dr - 1) / dr)} পেইজ দরকার" else "") + "।",
                    fontSize = 13.sp,
                    color = AppColors.extra.muted,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
private fun ForecastRow(label: String, value: String, up: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = if (up) AppColors.extra.ok else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun PayHistoryItem(x: JSONObject) {
    val st = when (x.optString("status")) {
        "paid" -> Pair("পরিশোধিত", AppColors.extra.ok)
        "rejected" -> Pair("বাতিল", MaterialTheme.colorScheme.error)
        else -> Pair("অপেক্ষমাণ (বাকি)", AppColors.extra.warn)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = money(x.optInt("amount", 0)),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = st.second
            ) {
                Text(
                    text = st.first,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    fontSize = 11.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = buildString {
                append("রিকোয়েস্ট: ${fmtDT(x.optString("created_at"))}")
                val processedAt = x.optString("processed_at", "")
                if (processedAt.isNotEmpty()) {
                    append("\n")
                    append(if (x.optString("status") == "paid") "পরিশোধ" else "প্রক্রিয়া")
                    append(": ${fmtDT(processedAt)}")
                }
                append("\n")
                append(pmName(x.optString("method", "")))
                append(" ")
                append(x.optString("number", ""))
                val pages = x.optInt("pages", 0)
                if (pages > 0) {
                    append(" • ${bn(pages)} পেইজ")
                }
                val note = x.optString("note", "")
                if (note.isNotEmpty()) {
                    append("\nনোট: $note")
                }
            },
            fontSize = 12.sp,
            color = AppColors.extra.muted,
            lineHeight = 16.sp
        )
    }
}

@Composable
private fun BookEarnItem(b: JSONObject?, x: BookPaySum, rateOf: (JSONObject?) -> Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = b?.optString("name") ?: "কিতাব",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = money(x.earned),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
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
                fontSize = 11.sp,
                color = AppColors.extra.muted
            )
            Text(
                text = "রেট ${rateTxt(rateOf(b))}",
                fontSize = 11.sp,
                color = AppColors.extra.muted
            )
        }
    }
}

@Composable
private fun SegBar(a: Int, p: Int, c: Int, tot: Int) {
    val t = maxOf(tot, a + p + c, 1)
    val aPct = a.toFloat() / t
    val pPct = p.toFloat() / t
    val cPct = c.toFloat() / t

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(AppColors.extra.line)
    ) {
        if (a > 0) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(aPct)
                    .background(AppColors.extra.ok)
            )
        }
        if (p > 0) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(pPct)
                    .background(AppColors.extra.warn)
            )
        }
        if (c > 0) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(cPct)
                    .background(MaterialTheme.colorScheme.error)
            )
        }
    }
}

@Composable
private fun CorrectionItem(p: JSONObject, booksById: Map<String, JSONObject>) {
    val bookId = p.optInt("book_id", 0).toString()
    val b = booksById[bookId]
    val stem = p.optString("stem", "")
    val note = p.optString("review_note", "")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${b?.optString("name") ?: "কিতাব"} — $stem",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.error
            ) {
                Text(
                    text = "খুলুন",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    fontSize = 12.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        if (note.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = "অ্যাডমিন: $note",
                fontSize = 12.sp,
                color = AppColors.extra.muted
            )
        }
    }
}

private data class WarnInfo(val reviewed: Int, val pct: Int, val correction: Int)

fun minPayout(s: JSONObject?): Int = s?.optInt("min_payout", 100) ?: 100

private fun money(n: Int): String = "৳${bn(n)}"

private fun bn(n: Int): String {
    val map = mapOf('0' to '০', '1' to '১', '2' to '২', '3' to '৩', '4' to '৪', '5' to '৫', '6' to '৬', '7' to '৭', '8' to '৮', '9' to '৯')
    return n.toString().map { map[it] ?: it }.joinToString("")
}

private fun r2(n: Int): Int = ((n + 0.5).toInt())

private fun pmName(m: String): String = when (m) {
    "bkash" -> "বিকাশ"
    "nagad" -> "নগদ"
    "rocket" -> "রকেট"
    "upay" -> "উপায়"
    else -> m
}

private fun fmtDT(s: String): String {
    if (s.isEmpty()) return ""
    return try {
        val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val date = fmt.parse(s) ?: return s
        val local = SimpleDateFormat("d MMM yyyy, h:mm a", Locale("bn", "BD"))
        local.format(date)
    } catch (e: Exception) {
        s
    }
}
