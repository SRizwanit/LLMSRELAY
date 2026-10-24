package com.srizwan.task

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.srizwan.task.data.Api
import com.srizwan.task.task.RankData
import com.srizwan.task.task.RankPeriod
import com.srizwan.task.task.RankRow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

enum class TaskTab { TASK, PAY, RANK }

data class TaskState(
    val loading: Boolean = true,
    val error: String = "",
    val tab: TaskTab = TaskTab.TASK,
    val userId: Int = 0,
    val books: List<JSONObject> = emptyList(),
    val proofs: List<JSONObject> = emptyList(),
    val query: String = "",
    val todayCount: Int = 0,
    val goal: Int = 10,
    val goalHit: Boolean = false,
    val showGoalDialog: Boolean = false,
    val money: FinData = FinData(),
    val moneyLoading: Boolean = false,
    val moneyError: String = "",
    val rankPeriod: RankPeriod = RankPeriod.DAY,
    val rankLoading: Boolean = false,
    val rankData: RankData? = null,
    val rankError: String = ""
)

data class FinData(
    val approved: Int = 0,
    val pending: Int = 0,
    val correction: Int = 0,
    val paid: Int = 0,
    val wallet: Int = 0,
    val history: List<JSONObject> = emptyList(),
    val bookSums: List<BookPaySum> = emptyList(),
    val corrections: List<JSONObject> = emptyList(),
    val canReq: Boolean = false
)

data class BookPaySum(
    val bookId: String,
    val approved: Int,
    val pending: Int,
    val correction: Int,
    val total: Int,
    val cancelled: Int = 0
)

class TaskViewModel : ViewModel() {
    private val _state = MutableStateFlow(TaskState())
    val state = _state.asStateFlow()

    fun load(context: Context) {
        val s = Session.get(context) ?: return
        val uid = s.optInt("id", 0)
        if (uid == 0) return

        _state.update { it.copy(loading = true, userId = uid, goal = readGoalFromPrefs(context, uid)) }

        viewModelScope.launch {
            try {
                loadBooks(context)
                loadCount(context, uid)
                loadMoney(context, uid)
                loadRank(context, uid, _state.value.rankPeriod)
                _state.update { it.copy(loading = false) }
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = e.message ?: "লোড ব্যর্থ") }
            }
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
        val api = Api(context)
        val arr = api.get("app_books?select=*&order=id.asc")
        val list = (0 until arr.length()).map { arr.getJSONObject(it) }
        _state.update { it.copy(books = list) }
    }

    private suspend fun loadCount(context: Context, userId: Int) {
        val api = Api(context)
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val arr = api.get("page_proofs?user_id=eq.$userId&select=*&order=created_at.desc")
        val all = (0 until arr.length()).map { arr.getJSONObject(it) }
        val todayCount = all.count { it.optString("created_at", "").startsWith(today) }
        val goalHit = todayCount >= _state.value.goal
        _state.update { it.copy(proofs = all, todayCount = todayCount, goalHit = goalHit) }
    }

    private suspend fun loadMoney(context: Context, userId: Int) {
        _state.update { it.copy(moneyLoading = true, moneyError = "") }
        try {
            val api = Api(context)
            val proofsArr = api.get("page_proofs?user_id=eq.$userId&select=*")
            val proofs = (0 until proofsArr.length()).map { proofsArr.getJSONObject(it) }

            val booksById = _state.value.books.associateBy { it.optString("id", "") }

            var approved = 0
            var pending = 0
            var correction = 0
            val bookMap = mutableMapOf<String, Triple<Int, Int, Int>>()

            proofs.forEach { p ->
                val st = p.optString("status", "pending")
                val bid = p.optString("book_id", "")
                val rate = booksById[bid]?.optInt("rate_per_page", 1) ?: 1
                val old = bookMap[bid] ?: Triple(0, 0, 0)
                when (st) {
                    "approved" -> {
                        approved += rate
                        bookMap[bid] = old.copy(first = old.first + rate)
                    }
                    "pending" -> {
                        pending += rate
                        bookMap[bid] = old.copy(second = old.second + rate)
                    }
                    "correction" -> {
                        correction += rate
                        bookMap[bid] = old.copy(third = old.third + rate)
                    }
                }
            }

            val bookSums = bookMap.map { (bid, t) ->
                val tot = t.first + t.second + t.third
                BookPaySum(bid, t.first, t.second, t.third, tot, 0)
            }

            val payArr = api.get("payments?user_id=eq.$userId&select=*&order=created_at.desc")
            val history = (0 until payArr.length()).map { payArr.getJSONObject(it) }
            val paid = history.filter { it.optString("status") == "completed" }
                .sumOf { it.optInt("amount", 0) }

            val corrArr = api.get("page_proofs?user_id=eq.$userId&status=eq.correction&select=*&order=created_at.desc")
            val corrections = (0 until corrArr.length()).map { corrArr.getJSONObject(it) }

            val wallet = approved - paid
            val canReq = wallet >= 100 && history.none { it.optString("status") == "pending" }

            _state.update {
                it.copy(
                    money = FinData(
                        approved, pending, correction, paid, wallet, history, bookSums, corrections, canReq
                    ),
                    moneyLoading = false
                )
            }
        } catch (e: Exception) {
            _state.update { it.copy(moneyLoading = false, moneyError = e.message ?: "লোড ব্যর্থ") }
        }
    }

    fun rateOf(book: JSONObject?): Int = book?.optInt("rate_per_page", 1) ?: 1

    fun requestPayment(context: Context) {
        viewModelScope.launch {
            try {
                val api = Api(context)
                val body = JSONObject().apply {
                    put("user_id", _state.value.userId)
                    put("amount", _state.value.money.wallet)
                    put("status", "pending")
                    put("created_at", iso(System.currentTimeMillis()))
                }
                api.post("payments", body)
                delay(500)
                loadMoney(context, _state.value.userId)
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message ?: "অনুরোধ ব্যর্থ") }
            }
        }
    }

    fun readGoalFromPrefs(context: Context, userId: Int): Int {
        val prefs = context.getSharedPreferences("task_prefs", Context.MODE_PRIVATE)
        return prefs.getInt("goal_$userId", 10)
    }

    private fun iso(ms: Long): String {
        return SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date(ms))
    }

    fun switchRankPeriod(p: RankPeriod) {
        _state.update { it.copy(rankPeriod = p, rankLoading = true) }
        viewModelScope.launch {
            try {
                loadRank(App.instance, _state.value.userId, p)
            } catch (e: Exception) {
                _state.update { it.copy(rankLoading = false, rankError = e.message ?: "লোড ব্যর্থ") }
            }
        }
    }

    private suspend fun loadRank(context: Context, userId: Int, period: RankPeriod) {
        val api = Api(context)

        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()
        val filter = when (period) {
            RankPeriod.DAY -> {
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(cal.time)
                "created_at=gte.$start"
            }
            RankPeriod.WEEK -> {
                cal.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                if (cal.timeInMillis > now) cal.add(Calendar.WEEK_OF_YEAR, -1)
                val start = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(cal.time)
                "created_at=gte.$start"
            }
            RankPeriod.ALL -> ""
        }

        val path = if (filter.isEmpty()) {
            "page_proofs?status=eq.approved&select=user_id"
        } else {
            "page_proofs?status=eq.approved&$filter&select=user_id"
        }

        val arr = api.get(path)
        val userCounts = mutableMapOf<Int, Int>()
        for (i in 0 until arr.length()) {
            val uid = arr.getJSONObject(i).optInt("user_id", 0)
            if (uid > 0) userCounts[uid] = (userCounts[uid] ?: 0) + 1
        }

        val rows = userCounts.map { (id, n) -> RankRow(id, n) }
            .sortedByDescending { it.n }

        val userIds = rows.map { it.id }.distinct()
        val names = mutableMapOf<Int, String>()
        if (userIds.isNotEmpty()) {
            val usersPath = "app_users?id=in.(${userIds.joinToString(",")})&select=id,full_name"
            val usersArr = api.get(usersPath)
            for (i in 0 until usersArr.length()) {
                val u = usersArr.getJSONObject(i)
                names[u.optInt("id", 0)] = u.optString("full_name", "ইউজার")
            }
        }

        _state.update {
            it.copy(
                rankLoading = false,
                rankData = RankData(period, rows, names),
                rankError = ""
            )
        }
    }
}
