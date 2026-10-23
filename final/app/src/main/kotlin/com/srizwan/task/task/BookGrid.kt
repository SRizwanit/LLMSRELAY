package com.srizwan.task.task

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.srizwan.task.AppColors
import com.srizwan.task.R
import org.json.JSONObject

@Composable
fun BookGrid(
    ready: Boolean,
    me: Int,
    books: List<JSONObject>,
    proofs: List<JSONObject>,
    booksById: Map<String, JSONObject>,
    query: String,
    onQueryChange: (String) -> Unit,
    goal: Int,
    resume: ResumeTarget?,
    rateOf: (JSONObject?) -> Int,
    onResume: (String, Int) -> Unit,
    onOpen: (String, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val myProofs = proofs.filter { it.optInt("user_id") == me }
    val filtered = if (query.isBlank()) books else books.filter {
        it.optString("name", "").contains(query, ignoreCase = true)
    }

    Column(modifier = modifier.padding(horizontal = 16.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text("কিতাব খুঁজুন...") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = {
                Icon(
                    painter = painterResource(R.drawable.rizwantask),
                    contentDescription = null
                )
            }
        )

        Spacer(Modifier.height(12.dp))

        if (!ready) {
            SkeletonGrid()
        } else if (filtered.isEmpty()) {
            EmptyView(failed = false)
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filtered) { book ->
                    val id = book.optString("id", "")
                    val done = myProofs.count { it.optString("book_id") == id }
                    val total = book.optInt("total_pages", 0)
                    val next = done + 1
                    BookCard(
                        book = book,
                        done = done,
                        total = total,
                        rate = rateOf(book),
                        onClick = { onOpen(id, next) }
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
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Cover(url = book.optString("cover_url", ""))
        Spacer(Modifier.height(8.dp))
        Text(
            text = book.optString("name", ""),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            maxLines = 2,
            minLines = 2,
            lineHeight = 18.sp
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = rateTxt(rate),
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.extra.muted
        )
        Spacer(Modifier.height(8.dp))
        Progress(done, total)
    }
}

@Composable
private fun Cover(url: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.7f)
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
            Icon(
                painter = painterResource(R.drawable.rizwantask),
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = AppColors.extra.muted
            )
        }
    }
}

@Composable
private fun Progress(done: Int, total: Int) {
    val pct = if (total > 0) done.toFloat() / total else 0f
    Column {
        LinearProgressIndicator(
            progress = { pct },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "${bn(done)}/${bn(total)} পেইজ (${bn((pct * 100).toInt())}%)",
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.extra.muted
        )
    }
}

@Composable
private fun SkeletonGrid() {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(bottom = 16.dp),
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
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.7f)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha))
        )
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(16.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha))
        )
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth(0.5f)
                .height(14.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha))
        )
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
            Icon(
                painter = painterResource(R.drawable.rizwantask),
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = AppColors.extra.muted
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (failed) "কিতাব লোড করা যায়নি" else "কোনো কিতাব পাওয়া যায়নি",
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.extra.muted,
                textAlign = TextAlign.Center
            )
        }
    }
}

fun rateTxt(rate: Int): String = "৳${bn(rate)}/পেইজ"

private fun bn(n: Int): String {
    val digits = "০১২৩৪৫৬৭৮৯"
    return n.toString().map { if (it.isDigit()) digits[it - '0'] else it }.joinToString("")
}
