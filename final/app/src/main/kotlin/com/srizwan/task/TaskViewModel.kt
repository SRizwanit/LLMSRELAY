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
import org.json.JSONObject
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

enum class TaskTab { TASK, PAY, RANK }

data class TaskState(
    val tab: TaskTab = TaskTab.TASK,
    val loading: Boolean = true,
    val user: JSONObject? = null,
    val books: List<JSONObject> = emptyList(),
    val proofs: List<JSONObject> = emptyList(),
    val query: String = "",
    val todayCount: Int = 0,
    val totalCount: Int = 0,
    val goal: Int = 5,
    val goalHit: Boolean = false,
    val goalDialogOpen: Boolean = false,
    val money: FinData? = null,
    val booksById: Map<String, JSONObject> = emptyMap(),
    val corrections: List<JSONObject> = emptyList(),
    val payHistory: List<JSONObject> = emptyList(),
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
    val corrected: Int = 0,
    val canReq: Boolean = false,
    val bookSums: List<BookPaySum> = emptyList(),
    val corrections: List<JSONObject> = emptyList()
)

data class BookPaySum(
    val bookId: String,
    val approved: Int,
    val pending: Int,
    val corrected: Int,
    val cancelled: Int,
    val total: Int
)

class TaskViewModel : ViewModel() {
    private val _state = MutableStateFlow(TaskState())
    val state = _state.asStateFlow()

    fun load(context: Context) {
        viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            val session = Session.get(context)
            if (session == null) {
                _state.update { it.copy(loading = false) }
                return@launch
            }
            val userId = session.optInt("id")
            val api = Api(context)

            try {
                val user = api.get("app_users?id=eq.$userId&limit=1").optJSONObject(0)
                if (user != null) {
                    _state.update { it.copy(user = user) }
                    loadBooks(context)
                    loadCount(context, userId)
                    loadMoney(context, userId)
                    loadRank(context, userId, _state.value.rankPeriod)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _state.update { it.copy(loading = false) }
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
        _state.update { it.copy(goalDialogOpen = show) }
    }

    private suspend fun loadBooks(context: Context) {
        val api = Api(context)
        try {
            val arr = api.get("books?select=*&order=id.asc")
            val list = (0 until arr.length()).map { arr.getJSONObject(it) }
            val map = list.associateBy { it.optString("id") }
            _state.update { it.copy(books = list, booksById = map) }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun loadCount(context: Context, userId: Int) {
        val api = Api(context)
        try {
            val today = iso(System.currentTimeMillis()).substring(0, 10)
            val proofsArr = api.get("page_proofs?user_id=eq.$userId&select=*&order=created_at.desc")
            val proofs = (0 until proofsArr.length()).map { proofsArr.getJSONObject(it) }
            val todayCount = proofs.count { it.optString("created_at", "").startsWith(today) }
            val totalCount = proofs.size
            val goal = readGoalFromPrefs(context, userId)
            val goalHit = todayCount >= goal
            _state.update {
                it.copy(
                    proofs = proofs,
                    todayCount = todayCount,
                    totalCount = totalCount,
                    goal = goal,
                    goalHit = goalHit
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun loadMoney(context: Context, userId: Int) {
        _state.update { it.copy(moneyLoading = true, moneyError = "") }
        val api = Api(context)
        try {
            val proofsArr = api.get("page_proofs?user_id=eq.$userId&select=book_id,page,status,rate")
            val proofs = (0 until proofsArr.length()).map { proofsArr.getJSONObject(it) }

            var approved = 0
            var pending = 0
            var corrected = 0

            val bookMap = mutableMapOf<String, BookPaySum>()

            for (p in proofs) {
                val rate = p.optInt("rate", 0)
                val status = p.optString("status", "pending")
                val bookId = p.optString("book_id", "")

                val key = bookId.ifEmpty { "unknown" }
                val sum = bookMap.getOrPut(key) { BookPaySum(key, 0, 0, 0, 0, 0) }

                when (status) {
                    "approved" -> {
                        approved += rate
                        bookMap[key] = sum.copy(
                            approved = sum.approved + rate,
                            total = sum.total + rate
                        )
                    }
                    "pending" -> {
                        pending += rate
                        bookMap[key] = sum.copy(
                            pending = sum.pending + rate,
                            total = sum.total + rate
                        )
                    }
                    "corrected" -> {
                        corrected += rate
                        bookMap[key] = sum.copy(
                            corrected = sum.corrected + rate,
                            total = sum.total + rate
                        )
                    }
                    "cancelled" -> {
                        bookMap[key] = sum.copy(
                            cancelled = sum.cancelled + 1,
                            total = sum.total + rate
                        )
                    }
                }
            }

            val corrArr = api.get("payment_corrections?user_id=eq.$userId&select=*")
            val corrections = (0 until corrArr.length()).map { corrArr.getJSONObject(it) }
            for (c in corrections) {
                corrected += c.optInt("amount", 0)
            }

            val payArr = api.get("payments?user_id=eq.$userId&select=*&order=created_at.desc")
            val payHistory = (0 until payArr.length()).map { payArr.getJSONObject(it) }

            val canReq = approved >= 200

            val money = FinData(
                approved = approved,
                pending = pending,
                corrected = corrected,
                canReq = canReq,
                bookSums = bookMap.values.toList(),
                corrections = corrections
            )

            _state.update {
                it.copy(
                    money = money,
                    corrections = corrections,
                    payHistory = payHistory,
                    moneyLoading = false
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            _state.update {
                it.copy(
                    moneyLoading = false,
                    moneyError = e.message ?: "অজানা ত্রুটি"
                )
            }
        }
    }

    fun rateOf(book: JSONObject?): Int = book?.optInt("rate_per_page", 1) ?: 1

    fun requestPayment(context: Context) {
        viewModelScope.launch {
            val session = Session.get(context) ?: return@launch
            val userId = session.optInt("id")
            val api = Api(context)
            try {
                val body = JSONObject().apply {
                    put("user_id", userId)
                    put("created_at", iso(System.currentTimeMillis()))
                }
                api.post("payment_requests", body)
                loadMoney(context, userId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun readGoalFromPrefs(context: Context, userId: Int): Int {
        val prefs = context.getSharedPreferences("task_prefs", Context.MODE_PRIVATE)
        return prefs.getInt("goal_$userId", 5)
    }

    private fun iso(ms: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date(ms))
    }

    fun switchRankPeriod(p: RankPeriod) {
        if (_state.value.rankPeriod == p) return
        _state.update { it.copy(rankPeriod = p, rankError = "") }
        val session = _state.value.user
        if (session != null) {
            val userId = session.optInt("id")
            viewModelScope.launch {
                loadRank(App.instance, userId, p)
            }
        }
    }

    private suspend fun loadRank(context: Context, userId: Int, period: RankPeriod) {
        _state.update { it.copy(rankLoading = true, rankError = "") }
        val api = Api(context)
        try {
            val where = when (period) {
                RankPeriod.DAY -> {
                    val today = iso(System.currentTimeMillis()).substring(0, 10)
                    "created_at=gte.$today"
                }
                RankPeriod.WEEK -> {
                    val weekAgo = System.currentTimeMillis() - 7L * 24 * 3600 * 1000
                    "created_at=gte.${iso(weekAgo)}"
                }
                RankPeriod.ALL -> ""
            }

            val path = if (where.isNotEmpty()) {
                "page_proofs?$where&select=user_id&order=user_id"
            } else {
                "page_proofs?select=user_id&order=user_id"
            }

            val arr = api.get(path)
            val counts = mutableMapOf<Int, Int>()
            for (i in 0 until arr.length()) {
                val uid = arr.getJSONObject(i).optInt("user_id")
                counts[uid] = counts.getOrDefault(uid, 0) + 1
            }

            val rows = counts.map { RankRow(it.key, it.value) }.sortedByDescending { it.n }

            val userIds = rows.map { it.id }.distinct().joinToString(",")
            val names = mutableMapOf<Int, String>()
            if (userIds.isNotEmpty()) {
                val usersArr = api.get("app_users?id=in.($userIds)&select=id,full_name")
                for (i in 0 until usersArr.length()) {
                    val u = usersArr.getJSONObject(i)
                    names[u.optInt("id")] = u.optString("full_name", "ইউজার")
                }
            }

            _state.update {
                it.copy(
                    rankData = RankData(period, rows, names),
                    rankLoading = false
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            _state.update {
                it.copy(
                    rankLoading = false,
                    rankError = e.message ?: "অজানা ত্রুটি"
                )
            }
        }
    }

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")
}
