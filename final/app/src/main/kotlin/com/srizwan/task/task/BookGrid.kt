package com.srizwan.task.task

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.SubcomposeAsyncImage
import com.srizwan.task.AppColors
import org.json.JSONObject

@Composable
fun BookGrid(
    status: String,
    spinning: Boolean,
    books: List<JSONObject>,
    query: String,
    bookCounts: Map<String, Int>,
    byBook: Map<String, BookUserCount>,
    resume: JSONObject?,
    rateOf: (JSONObject) -> Int,
    onOpen: (Int) -> Unit
) {
    val q = query.trim().lowercase()
    val list = books.filter {
        if (q.isEmpty()) true
        else {
            val name = it.optString("name", "").lowercase()
            val author = it.optString("author", "").lowercase()
            name.contains(q) || author.contains(q)
        }
    }

    val skeleton = books.isEmpty() && status == "loading"
    val failed = books.isEmpty() && status == "error"
    val showEmpty = !skeleton && (failed || list.isEmpty())

    Box(modifier = Modifier.fillMaxSize()) {
        if (spinning) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                    .align(Alignment.TopCenter)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.3f)
                        .height(3.dp)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }

        if (skeleton) {
            SkeletonGrid()
        } else if (showEmpty) {
            EmptyView(failed)
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(list, key = { it.optInt("id", 0) }) { b ->
                    BookCard(
                        book = b,
                        bookCounts = bookCounts,
                        byBook = byBook,
                        resume = resume,
                        rateOf = rateOf,
                        onOpen = onOpen
                    )
                }
            }
        }
    }
}

@Composable
private fun BookCard(
    book: JSONObject,
    bookCounts: Map<String, Int>,
    byBook: Map<String, BookUserCount>,
    resume: JSONObject?,
    rateOf: (JSONObject) -> Int,
    onOpen: (Int) -> Unit
) {
    val id = book.optInt("id", 0).toString()
    val done = bookCounts[id] ?: 0
    val total = book.optInt("total_pages", 0)
    val mine = byBook[id]?.n ?: 0
    val last = resume != null && resume.optString("b", "") == id

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                role = Role.Button,
                onClick = { onOpen(book.optInt("id", 0)) }
            )
            .padding(10.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.7f)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Cover(url = book.optString("cover_url", ""))
            if (last) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "শেষ কাজ",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Text(
            text = book.optString("name", ""),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        val author = book.optString("author", "")
        if (author.isNotEmpty()) {
            Spacer(Modifier.height(2.dp))
            Text(
                text = author,
                style = MaterialTheme.typography.bodySmall,
                fontSize = 12.sp,
                color = AppColors.extra.muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(Modifier.height(8.dp))

        Progress(done = done, total = total)

        Spacer(Modifier.height(6.dp))

        Text(
            text = "রেট ${rateTxt(rateOf(book))}/পেইজ${if (mine > 0) " • আমার ${bn(mine)}" else ""}",
            style = MaterialTheme.typography.bodySmall,
            fontSize = 11.sp,
            color = AppColors.extra.muted
        )
    }
}

@Composable
private fun Cover(url: String) {
    var bad by remember { mutableStateOf(false) }

    if (url.isEmpty() || bad) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = android.R.drawable.ic_menu_gallery),
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = AppColors.extra.muted.copy(alpha = 0.5f)
            )
        }
    } else {
        SubcomposeAsyncImage(
            model = url,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            loading = {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
            },
            onError = { bad = true }
        )
    }
}

@Composable
private fun Progress(done: Int, total: Int) {
    if (total > 0) {
        val rem = maxOf(0, total - done)
        val pct = minOf(100f, done.toFloat() / total * 100f)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.outline)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(pct / 100f)
                    .height(4.dp)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }

        Spacer(Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${bn(done)} সম্পন্ন",
                fontSize = 11.sp,
                color = AppColors.extra.muted
            )
            Text(
                text = "${bn(rem)} বাকি",
                fontSize = 11.sp,
                color = AppColors.extra.muted
            )
        }
    } else if (done > 0) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${bn(done)} সম্পন্ন",
                fontSize = 11.sp,
                color = AppColors.extra.muted
            )
            Text(
                text = "বাকি অজানা",
                fontSize = 11.sp,
                color = AppColors.extra.muted,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun SkeletonGrid() {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(4) {
            SkeletonCard()
        }
    }
}

@Composable
private fun SkeletonCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(10.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.7f)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(14.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth(0.5f)
                .height(12.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .height(11.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
    }
}

@Composable
private fun EmptyView(failed: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (failed) "কিতাব লোড হয়নি — নেট দেখে রিফ্রেশ চাপুন" else "কোনো কিতাব পাওয়া যায়নি",
            style = MaterialTheme.typography.bodyMedium,
            color = AppColors.extra.muted,
            fontSize = 14.sp
        )
    }
}

data class BookUserCount(val n: Int)

fun rateTxt(rate: Int): String {
    return if (rate <= 0) "—" else bn(rate)
}

private fun bn(n: Int): String {
    val bengali = arrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
    return n.toString().map { if (it.isDigit()) bengali[it - '0'] else it }.joinToString("")
}
