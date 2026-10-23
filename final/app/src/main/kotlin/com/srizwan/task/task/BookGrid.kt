package com.srizwan.task.task

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.srizwan.task.AppColors
import org.json.JSONObject

data class BookUserCount(val n: Int)

@Composable
fun BookGrid(
    ready: Boolean,
    books: List<JSONObject>,
    proofs: List<JSONObject>,
    booksById: Map<String, JSONObject>,
    query: String,
    onQueryChange: (String) -> Unit,
    resume: JSONObject?,
    rateOf: (JSONObject?) -> Int,
    onOpen: (String, String) -> Unit
) {
    val countByBook = proofs.groupBy { it.optInt("book_id", 0).toString() }
        .mapValues { (_, list) -> BookUserCount(list.size) }

    val filtered = if (query.isEmpty()) {
        books
    } else {
        books.filter {
            it.optString("name", "").contains(query, ignoreCase = true) ||
                    it.optString("author", "").contains(query, ignoreCase = true)
        }
    }

    Column {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            placeholder = { Text("কিতাব বা লেখক খুঁজুন…") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = AppColors.extra.line
            )
        )

        when {
            !ready -> SkeletonGrid()
            filtered.isEmpty() -> EmptyView(failed = books.isEmpty())
            else -> LazyVerticalGrid(
                columns = GridCells.Adaptive(140.dp),
                contentPadding = PaddingValues(0.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filtered) { b ->
                    val bookId = b.optInt("id", 0).toString()
                    val count = countByBook[bookId]?.n ?: 0
                    val total = b.optInt("total_pages", 0)
                    val resumeStem = if (resume != null && resume.optInt("book_id", 0).toString() == bookId) {
                        resume.optString("stem", "")
                    } else ""

                    BookCard(
                        book = b,
                        done = count,
                        total = total,
                        rate = rateOf(b),
                        isResume = resumeStem.isNotEmpty(),
                        onClick = {
                            val stem = resumeStem.ifEmpty {
                                if (count < total) "page_${count + 1}" else "page_1"
                            }
                            onOpen(bookId, stem)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun BookCard(
    book: JSONObject,
    done: Int,
    total: Int,
    rate: Int,
    isResume: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(140.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column {
            Cover(url = book.optString("cover_url", ""))

            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = book.optString("name", "কিতাব"),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    lineHeight = 16.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = book.optString("author", "লেখক"),
                    fontSize = 11.sp,
                    color = AppColors.extra.muted,
                    maxLines = 1
                )
                Spacer(Modifier.height(8.dp))

                Progress(done, total)

                Spacer(Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = rateTxt(rate),
                        fontSize = 11.sp,
                        color = AppColors.extra.gold,
                        fontWeight = FontWeight.Medium
                    )
                    if (isResume) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Text(
                                text = "চালিয়ে",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize = 10.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Cover(url: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
            .background(AppColors.extra.canvas),
        contentAlignment = Alignment.Center
    ) {
        if (url.isNotEmpty()) {
            AsyncImage(
                model = url,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Text(
                text = "📖",
                fontSize = 48.sp,
                color = AppColors.extra.muted
            )
        }
    }
}

@Composable
private fun Progress(done: Int, total: Int) {
    val pct = if (total > 0) (done.toFloat() / total) else 0f
    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(AppColors.extra.line)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(pct.coerceIn(0f, 1f))
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = "${bn(done)} / ${bn(total)} পেইজ",
            fontSize = 10.sp,
            color = AppColors.extra.muted
        )
    }
}

@Composable
private fun SkeletonGrid() {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(140.dp),
        contentPadding = PaddingValues(0.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(6) {
            SkeletonCard()
        }
    }
}

@Composable
private fun SkeletonCard() {
    Card(
        modifier = Modifier.width(140.dp).height(280.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(10.dp)
    ) {}
}

@Composable
private fun EmptyView(failed: Boolean) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (failed) "কিতাব লোড হয়নি" else "কিছু পাওয়া যায়নি",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = AppColors.extra.muted
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = if (failed) "ইন্টারনেট সংযোগ চেক করুন" else "অন্য শব্দ দিয়ে খুঁজুন",
                fontSize = 12.sp,
                color = AppColors.extra.muted
            )
        }
    }
}

fun rateTxt(rate: Int): String {
    return "৳${bn(rate)}/পেইজ"
}

private fun bn(n: Int): String {
    val map = mapOf('0' to '০', '1' to '১', '2' to '২', '3' to '৩', '4' to '৪', '5' to '৫', '6' to '৬', '7' to '৭', '8' to '৮', '9' to '৯')
    return n.toString().map { map[it] ?: it }.joinToString("")
}
