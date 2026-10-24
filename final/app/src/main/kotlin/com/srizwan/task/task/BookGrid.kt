package com.srizwan.task.task

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.srizwan.task.AppColors
import org.json.JSONObject

@Composable
fun BookGrid(
    books: List<JSONObject>,
    proofs: List<JSONObject>,
    query: String,
    onQueryChange: (String) -> Unit,
    onOpen: (JSONObject, Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { inner ->
                    if (query.isEmpty()) {
                        Text(
                            text = "🔍 কিতাব খুঁজুন...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppColors.extra.muted
                        )
                    }
                    inner()
                }
            )
        }

        Spacer(Modifier.height(12.dp))

        val filtered = if (query.isBlank()) books else {
            books.filter { it.optString("name", "").contains(query, ignoreCase = true) }
        }

        if (filtered.isEmpty()) {
            EmptyView(failed = false)
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filtered) { book ->
                    val bookId = book.optString("id", "")
                    val done = proofs.count {
                        it.optString("book_id") == bookId && it.optString("status") == "approved"
                    }
                    val total = book.optInt("total_pages", 0)
                    BookCard(book, done, total, onOpen)
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
    onOpen: (JSONObject, Int) -> Unit
) {
    val nextPage = (done + 1).coerceAtMost(total)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable { onOpen(book, nextPage) }
            .padding(12.dp)
    ) {
        Column {
            Cover(book.optString("cover_url", ""))
            Spacer(Modifier.height(8.dp))
            Text(
                text = book.optString("name", "কিতাব"),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = rateTxt(book.optInt("rate_per_page", 1)),
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.extra.ok
            )
            Spacer(Modifier.height(8.dp))
            Progress(done, total)
        }
    }
}

@Composable
private fun Cover(url: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.75f)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (url.isNotBlank()) {
            AsyncImage(
                model = url,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Text(
                text = "📖",
                fontSize = 48.sp
            )
        }
    }
}

@Composable
private fun Progress(done: Int, total: Int) {
    val pct = if (total > 0) done.toFloat() / total else 0f

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${bn(done)}/${bn(total)}",
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.extra.muted
            )
            Text(
                text = "${(pct * 100).toInt()}%",
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.extra.muted
            )
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { pct.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

@Composable
private fun SkeletonGrid() {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
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
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(12.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.75f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(16.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
            Spacer(Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.4f)
                    .height(12.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
        }
    }
}

@Composable
private fun EmptyView(failed: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (failed) "❌" else "📭",
                fontSize = 48.sp
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (failed) "কিতাব লোড করা যায়নি" else "কোনো কিতাব পাওয়া যায়নি",
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.extra.muted
            )
        }
    }
}

fun rateTxt(rate: Int): String = "৳${bn(rate)}/পেইজ"

private fun bn(n: Int): String {
    val digits = "০১২৩৪৫৬৭৮৯"
    return n.toString().map { if (it.isDigit()) digits[it - '0'] else it }.joinToString("")
}
