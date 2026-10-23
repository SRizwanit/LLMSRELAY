package com.srizwan.task

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.srizwan.task.data.Api
import com.srizwan.task.task.RankData
import com.srizwan.task.task.RankPeriod
import com.srizwan.task.task.RankRow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

enum class TaskTab { TASK, PAY, RANK }

data class TaskState(
    val tab: TaskTab = TaskTab.TASK,
    val userId: Int = 0,
    val userName: String = "",
    val query: String = "",
    val books: List<JSONObject> = emptyList(),
    val proofs: List<JSONObject> = emptyList(),
    val booksById: Map<String, JSONObject> = emptyMap(),
    val loading: Boolean = true,
    val todayCount: Int = 0,
    val goal: Int = 10,
    val goalHit: Boolean = false,
    val money: FinData? = null,
    val moneyLoading: Boolean = true,
    val moneyError: String = "",
    val showGoalDialog: Boolean = false,
    val rankPeriod: RankPeriod = RankPeriod.DAY,
    val rankData: RankData? = null,
    val rankLoading: Boolean = true,
    val rankError: String = ""
)

data class FinData(
    val total: Int,
    val approved: Int,
    val pending: Int,
    val cancelled: Int,
    val corrected: Int,
    val canReq: Boolean,
    val payments: List<JSONObject>,
    val bookSums: List<BookPaySum>,
    val corrections: List<JSONObject>
)

data class BookPaySum(
    val bookId: String,
    val approved: Int,
    val pending: Int,
    val cancelled: Int,
    val total: Int
)

class TaskViewModel : ViewModel() {
    private val _state = MutableStateFlow(TaskState())
    val state: StateFlow<TaskState> = _state.asStateFlow()

    fun load(context: Context) {
        val s = Session.get(context)
        if (s == null) {
            _state.value = _state.value.copy(loading = false, moneyLoading = false)
            return
        }
        val uid = s.optInt("id")
        val name = s.optString("name", "ইউজার")
        _state.value = _state.value.copy(userId = uid, userName = name, goal = readGoalFromPrefs(context, uid))

        viewModelScope.launch {
            try {
                loadBooks(context)
                loadCount(context, uid)
                loadMoney(context, uid)
                loadRank(context, uid, _state.value.rankPeriod)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun switchTab(t: TaskTab) {
        _state.value = _state.value.copy(tab = t)
    }

    fun setQuery(q: String) {
        _state.value = _state.value.copy(query = q)
    }

    fun showGoalDialog(show: Boolean) {
        _state.value = _state.value.copy(showGoalDialog = show)
    }

    private suspend fun loadBooks(context: Context) {
        _state.value = _state.value.copy(loading = true)
        try {
            val api = Api(context)
            val arr = api.get("kitabs?select=*&order=sort_order.asc,title.asc")
            val list = (0 until arr.length()).map { arr.getJSONObject(it) }
            val byId = list.associateBy { it.optString("id", "") }

            val proofArr = api.get("page_proofs?user_id=eq.${_state.value.userId}&select=*&order=created_at.desc")
            val proofs = (0 until proofArr.length()).map { proofArr.getJSONObject(it) }

            _state.value = _state.value.copy(books = list, proofs = proofs, booksById = byId, loading = false)
        } catch (e: Exception) {
            e.printStackTrace()
            _state.value = _state.value.copy(loading = false)
        }
    }

    private suspend fun loadCount(context: Context, userId: Int) {
        try {
            val api = Api(context)
            val today = iso(System.currentTimeMillis()).substringBefore('T')
            val arr = api.get("page_proofs?user_id=eq.$userId&created_at=gte.$today&select=id")
            val count = arr.length()
            val hit = count >= _state.value.goal
            _state.value = _state.value.copy(todayCount = count, goalHit = hit)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun loadMoney(context: Context, userId: Int) {
        _state.value = _state.value.copy(moneyLoading = true, moneyError = "")
        try {
            val api = Api(context)
            val proofsArr = api.get("page_proofs?user_id=eq.$userId&select=*&order=created_at.desc")
            val proofs = (0 until proofsArr.length()).map { proofsArr.getJSONObject(it) }

            val paymentsArr = api.get("payments?user_id=eq.$userId&select=*&order=created_at.desc")
            val payments = (0 until paymentsArr.length()).map { paymentsArr.getJSONObject(it) }

            val correctionsArr = api.get("page_proofs?user_id=eq.$userId&correction_amount=neq.0&select=*&order=created_at.desc")
            val corrections = (0 until correctionsArr.length()).map { correctionsArr.getJSONObject(it) }

            val byBook = mutableMapOf<String, Triple<Int, Int, Int>>()
            for (p in proofs) {
                val bid = p.optString("book_id", "")
                if (bid.isEmpty()) continue
                val old = byBook[bid] ?: Triple(0, 0, 0)
                val st = p.optString("status", "pending")
                val amt = p.optInt("amount", 0)
                byBook[bid] = when (st) {
                    "approved" -> old.copy(first = old.first + amt)
                    "cancelled" -> old.copy(third = old.third + amt)
                    else -> old.copy(second = old.second + amt)
                }
            }

            val bookSums = byBook.map { (bid, triple) ->
                BookPaySum(bid, triple.first, triple.second, triple.third, triple.first + triple.second)
            }

            var totApproved = 0
            var totPending = 0
            var totCancelled = 0
            var totCorrected = 0
            for (p in proofs) {
                val st = p.optString("status", "pending")
                val amt = p.optInt("amount", 0)
                val corr = p.optInt("correction_amount", 0)
                totCorrected += corr
                when (st) {
                    "approved" -> totApproved += amt
                    "cancelled" -> totCancelled += amt
                    else -> totPending += amt
                }
            }

            val canReq = totApproved > 0 && payments.none { it.optString("status") == "pending" }

            _state.value = _state.value.copy(
                money = FinData(
                    total = totApproved + totPending,
                    approved = totApproved,
                    pending = totPending,
                    cancelled = totCancelled,
                    corrected = totCorrected,
                    canReq = canReq,
                    payments = payments,
                    bookSums = bookSums,
                    corrections = corrections
                ),
                moneyLoading = false,
                moneyError = ""
            )
        } catch (e: Exception) {
            e.printStackTrace()
            _state.value = _state.value.copy(moneyLoading = false, moneyError = e.message ?: "অজানা ত্রুটি")
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
                    put("created_at", iso(System.currentTimeMillis()))
                }
                api.post("payments", body)
                loadMoney(context, _state.value.userId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun readGoalFromPrefs(context: Context, userId: Int): Int {
        val prefs = context.getSharedPreferences("task_prefs", Context.MODE_PRIVATE)
        return prefs.getInt("goal_$userId", 10)
    }

    private fun iso(ms: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(ms)
    }

    fun switchRankPeriod(p: RankPeriod) {
        if (_state.value.rankPeriod == p) return
        _state.value = _state.value.copy(rankPeriod = p, rankLoading = true, rankError = "")
        viewModelScope.launch {
            loadRank(Session.get(android.app.Application())?.let { android.app.Application() } ?: return@launch, _state.value.userId, p)
        }
    }

    private suspend fun loadRank(context: Context, userId: Int, period: RankPeriod) {
        _state.value = _state.value.copy(rankLoading = true, rankError = "")
        try {
            val api = Api(context)

            val filter = when (period) {
                RankPeriod.DAY -> {
                    val today = iso(System.currentTimeMillis()).substringBefore('T')
                    "created_at=gte.$today"
                }
                RankPeriod.WEEK -> {
                    val cal = Calendar.getInstance()
                    cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                    cal.set(Calendar.HOUR_OF_DAY, 0)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    val start = iso(cal.timeInMillis).substringBefore('T')
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
            val counts = mutableMapOf<Int, Int>()
            for (i in 0 until arr.length()) {
                val uid = arr.getJSONObject(i).optInt("user_id")
                counts[uid] = counts.getOrDefault(uid, 0) + 1
            }

            val rows = counts.map { RankRow(it.key, it.value) }.sortedByDescending { it.n }

            val userIds = rows.map { it.id }.distinct()
            val names = mutableMapOf<Int, String>()
            if (userIds.isNotEmpty()) {
                val usersPath = "app_users?id=in.(${userIds.joinToString(",") { enc(it.toString()) }})&select=id,full_name"
                val usersArr = api.get(usersPath)
                for (i in 0 until usersArr.length()) {
                    val u = usersArr.getJSONObject(i)
                    names[u.optInt("id")] = u.optString("full_name", "ইউজার")
                }
            }

            _state.value = _state.value.copy(
                rankData = RankData(period, rows, names),
                rankLoading = false,
                rankError = ""
            )
        } catch (e: Exception) {
            e.printStackTrace()
            _state.value = _state.value.copy(
                rankLoading = false,
                rankError = e.message ?: "অজানা ত্রুটি"
            )
        }
    }

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")
}
