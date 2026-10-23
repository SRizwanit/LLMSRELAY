package com.srizwan.task

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.srizwan.task.data.Api
import com.srizwan.task.task.RankData
import com.srizwan.task.task.RankPeriod
import com.srizwan.task.task.RankRow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder

enum class TaskTab { TASK, PAY, RANK }

data class TaskState(
    val loading: Boolean = true,
    val tab: TaskTab = TaskTab.TASK,
    val userId: Int = 0,
    val userName: String = "",
    val books: List<JSONObject> = emptyList(),
    val proofs: List<JSONObject> = emptyList(),
    val booksById: Map<String, JSONObject> = emptyMap(),
    val userCounts: Map<String, Int> = emptyMap(),
    val todayCount: Int = 0,
    val goal: Int = 10,
    val goalHit: Boolean = false,
    val money: FinData? = null,
    val moneyLoading: Boolean = false,
    val moneyError: String = "",
    val query: String = "",
    val showGoalDialog: Boolean = false,
    val rankPeriod: RankPeriod = RankPeriod.DAY,
    val rankLoading: Boolean = false,
    val rankData: RankData? = null,
    val rankError: String = ""
)

data class FinData(
    val approved: Int,
    val pending: Int,
    val corrected: Int,
    val total: Int,
    val canReq: Boolean,
    val payments: List<JSONObject>,
    val bookSums: List<BookPaySum>,
    val corrections: List<JSONObject>
)

data class BookPaySum(
    val bookId: String,
    val approved: Int,
    val pending: Int,
    val corrected: Int,
    val total: Int
)

class TaskViewModel : ViewModel() {
    private val _state = MutableStateFlow(TaskState())
    val state = _state.asStateFlow()

    fun load(context: Context) {
        viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            val s = Session.get(context)
            if (s == null) {
                _state.update { it.copy(loading = false) }
                return@launch
            }
            val userId = s.optInt("id")
            val userName = s.optString("name", "")
            _state.update { it.copy(userId = userId, userName = userName) }

            val goal = readGoalFromPrefs(context, userId)
            _state.update { it.copy(goal = goal) }

            loadBooks(context)
            loadCount(context, userId)
            loadMoney(context, userId)
            loadRank(context, userId, RankPeriod.DAY)
        }
    }

    fun switchTab(t: TaskTab) {
        _state.update { it.copy(tab = t) }
    }

    fun setQuery(q: String) {
        _state.update { it.copy(query = q) }
    }

    fun showGoalDialog(show: Boolean) {
        _state.update { it.copy(showGoalDialog = show) }
    }

    private suspend fun loadBooks(context: Context) {
        try {
            val api = Api(context)
            val arr = api.get("page_books?select=*&order=name.asc")
            val books = (0 until arr.length()).map { arr.getJSONObject(it) }
            val booksById = books.associateBy { it.getString("id") }

            val counts = mutableMapOf<String, Int>()
            books.forEach { b ->
                val id = b.getString("id")
                val c = api.get("page_proofs?select=user_id&book_id=eq.${enc(id)}")
                counts[id] = c.length()
            }

            _state.update {
                it.copy(
                    books = books,
                    booksById = booksById,
                    userCounts = counts,
                    loading = false
                )
            }
        } catch (e: Exception) {
            _state.update { it.copy(loading = false) }
        }
    }

    private suspend fun loadCount(context: Context, userId: Int) {
        try {
            val api = Api(context)
            val arr = api.get("page_proofs?select=*&user_id=eq.$userId&order=created_at.asc")
            val proofs = (0 until arr.length()).map { arr.getJSONObject(it) }

            val today = iso(System.currentTimeMillis()).substringBefore('T')
            val todayCount = proofs.count { it.optString("created_at", "").startsWith(today) }
            val goalHit = todayCount >= _state.value.goal

            _state.update {
                it.copy(
                    proofs = proofs,
                    todayCount = todayCount,
                    goalHit = goalHit
                )
            }
        } catch (e: Exception) {
        }
    }

    private suspend fun loadMoney(context: Context, userId: Int) {
        _state.update { it.copy(moneyLoading = true, moneyError = "") }
        try {
            val api = Api(context)
            val proofs = api.get("page_proofs?select=*&user_id=eq.$userId&order=created_at.desc")
            val payments = api.get("payments?select=*&user_id=eq.$userId&order=created_at.desc")

            var approved = 0
            var pending = 0
            var corrected = 0
            val bookSums = mutableMapOf<String, Triple<Int, Int, Int>>()

            for (i in 0 until proofs.length()) {
                val p = proofs.getJSONObject(i)
                val st = p.optString("status", "pending")
                val rate = rateOf(_state.value.booksById[p.optString("book_id")])
                val earn = if (st == "approved") rate else 0
                val bookId = p.optString("book_id", "")

                val old = bookSums[bookId] ?: Triple(0, 0, 0)
                when (st) {
                    "approved" -> {
                        approved += earn
                        bookSums[bookId] = old.copy(first = old.first + earn)
                    }
                    "pending" -> {
                        pending += rate
                        bookSums[bookId] = old.copy(second = old.second + rate)
                    }
                    "corrected" -> {
                        corrected += rate
                        bookSums[bookId] = old.copy(third = old.third + rate)
                    }
                }
            }

            val payList = (0 until payments.length()).map { payments.getJSONObject(it) }
            val corrections = (0 until proofs.length()).map { proofs.getJSONObject(it) }
                .filter { it.optString("status") == "corrected" }

            val bookPaySums = bookSums.map { (id, t) ->
                BookPaySum(id, t.first, t.second, t.third, t.first + t.second + t.third)
            }.sortedByDescending { it.total }

            val settings = api.get("app_settings?limit=1")
            val minPayout = if (settings.length() > 0) settings.getJSONObject(0)
                .optInt("min_payout", 100) else 100
            val canReq = approved >= minPayout

            val fin = FinData(
                approved, pending, corrected, approved + pending + corrected,
                canReq, payList, bookPaySums, corrections
            )

            _state.update { it.copy(money = fin, moneyLoading = false) }
        } catch (e: Exception) {
            _state.update {
                it.copy(
                    moneyError = e.message ?: "এরর",
                    moneyLoading = false
                )
            }
        }
    }

    fun rateOf(book: JSONObject?): Int = book?.optInt("rate_per_page", 1) ?: 1

    fun requestPayment(context: Context) {
        viewModelScope.launch {
            try {
                val api = Api(context)
                val body = JSONObject().apply {
                    put("user_id", _state.value.userId)
                    put("amount", _state.value.money?.approved ?: 0)
                    put("status", "pending")
                }
                api.post("payments", body)
                loadMoney(context, _state.value.userId)
            } catch (e: Exception) {
            }
        }
    }

    fun readGoalFromPrefs(context: Context, userId: Int): Int {
        val prefs = context.getSharedPreferences("kp_task", Context.MODE_PRIVATE)
        return prefs.getInt("goal_$userId", 10)
    }

    fun findResume(proofs: List<JSONObject>, booksById: Map<String, JSONObject>): JSONObject? {
        val byBook = proofs.groupBy { it.optString("book_id", "") }
        for (b in booksById.values) {
            val id = b.getString("id")
            val total = b.optInt("total_pages", 0)
            val done = byBook[id]?.size ?: 0
            if (done > 0 && done < total) return b
        }
        return null
    }

    private fun iso(ms: Long): String {
        return java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US)
            .apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }
            .format(java.util.Date(ms))
    }

    fun switchRankPeriod(p: RankPeriod) {
        val userId = _state.value.userId
        if (userId == 0) return
        _state.update { it.copy(rankPeriod = p) }
        viewModelScope.launch {
            loadRank(android.app.Application(), userId, p)
        }
    }

    private suspend fun loadRank(context: Context, userId: Int, period: RankPeriod) {
        _state.update { it.copy(rankLoading = true, rankError = "") }
        try {
            val api = Api(context)
            val now = System.currentTimeMillis()
            val today = iso(now).substringBefore('T')

            val filter = when (period) {
                RankPeriod.DAY -> "created_at=gte.$today"
                RankPeriod.WEEK -> {
                    val weekAgo = now - 7L * 86400000L
                    val date = iso(weekAgo).substringBefore('T')
                    "created_at=gte.$date"
                }
                RankPeriod.ALL -> ""
            }

            val path = if (filter.isEmpty()) "page_proofs?select=user_id,book_id"
            else "page_proofs?select=user_id,book_id&$filter"

            val arr = api.get(path)
            val counts = mutableMapOf<Int, Int>()
            for (i in 0 until arr.length()) {
                val uid = arr.getJSONObject(i).optInt("user_id", 0)
                counts[uid] = (counts[uid] ?: 0) + 1
            }

            val sorted = counts.entries.sortedByDescending { it.value }
                .map { RankRow(it.key, it.value) }

            val userIds = sorted.map { it.id }.distinct()
            val names = mutableMapOf<Int, String>()
            if (userIds.isNotEmpty()) {
                val ids = userIds.joinToString(",")
                val users = api.get("app_users?select=id,full_name&id=in.($ids)")
                for (i in 0 until users.length()) {
                    val u = users.getJSONObject(i)
                    names[u.getInt("id")] = u.optString("full_name", "ইউজার")
                }
            }

            val data = RankData(sorted, names)
            _state.update { it.copy(rankData = data, rankLoading = false) }
        } catch (e: Exception) {
            _state.update {
                it.copy(
                    rankError = e.message ?: "এরর",
                    rankLoading = false
                )
            }
        }
    }

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")
}

private fun JSONArray.optJSONObject(index: Int): JSONObject? = try {
    getJSONObject(index)
} catch (e: Exception) {
    null
}
