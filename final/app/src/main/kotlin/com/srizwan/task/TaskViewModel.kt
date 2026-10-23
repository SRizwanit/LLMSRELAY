package com.srizwan.task

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.srizwan.task.data.Api
import com.srizwan.task.task.BookPaySum
import com.srizwan.task.task.PaySum
import com.srizwan.task.task.minPayout
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder

enum class TaskTab { TASK, PAY, RANK }

data class TaskState(
    val loading: Boolean = false,
    val failed: Boolean = false,
    val me: JSONObject? = null,
    val settings: JSONObject? = null,
    val tab: TaskTab = TaskTab.TASK,
    val query: String = "",
    val books: List<JSONObject> = emptyList(),
    val booksById: Map<String, JSONObject> = emptyMap(),
    val proofs: List<JSONObject> = emptyList(),
    val pays: List<JSONObject> = emptyList(),
    val counts: Map<String, Int> = emptyMap(),
    val finData: FinData? = null,
    val rankData: RankData? = null,
    val showGoalDialog: Boolean = false,
    val reqBusy: Boolean = false,
    val error: String? = null
)

data class FinData(
    val sum: PaySum?,
    val pays: List<JSONObject>
)

data class RankData(
    val ranks: List<JSONObject> = emptyList()
)

class TaskViewModel : ViewModel() {
    var state by mutableStateOf(TaskState())
        private set

    fun load(context: Context) {
        if (state.loading || state.me != null) return
        state = state.copy(loading = true, failed = false)
        viewModelScope.launch {
            try {
                val s = Session.get(context)
                if (s == null) {
                    state = state.copy(loading = false, failed = true)
                    return@launch
                }
                val uid = s.optInt("id")

                val api = Api(context)
                val u = api.get("app_users?id=eq.$uid").optJSONObject(0)
                    ?: throw Exception("User not found")

                val settingsArr = api.get("app_settings?limit=1")
                val settings = if (settingsArr.length() > 0) settingsArr.getJSONObject(0) else null

                loadBooks(context)
                loadCount(context, uid)
                loadMoney(context, uid)

                state = state.copy(loading = false, me = u, settings = settings)
            } catch (e: Exception) {
                state = state.copy(loading = false, failed = true)
            }
        }
    }

    fun switchTab(t: TaskTab) {
        state = state.copy(tab = t)
    }

    fun setQuery(q: String) {
        state = state.copy(query = q)
    }

    fun showGoalDialog(show: Boolean) {
        state = state.copy(showGoalDialog = show)
    }

    private suspend fun loadBooks(context: Context) {
        val api = Api(context)
        val arr = api.get("books?select=*&order=id.asc")
        val list = (0 until arr.length()).map { arr.getJSONObject(it) }
        val map = list.associateBy { it.optInt("id").toString() }
        state = state.copy(books = list, booksById = map)
    }

    private suspend fun loadCount(context: Context, userId: Int) {
        val api = Api(context)
        val arr = api.get("page_proofs?select=book_id,review_status&user_id=eq.$userId")
        val proofs = (0 until arr.length()).map { arr.getJSONObject(it) }
        state = state.copy(proofs = proofs)

        val counts = mutableMapOf<String, Int>()
        proofs.forEach { p ->
            val bid = p.optInt("book_id").toString()
            counts[bid] = (counts[bid] ?: 0) + 1
        }
        state = state.copy(counts = counts)
    }

    private suspend fun loadMoney(context: Context, userId: Int) {
        val api = Api(context)

        val proofsArr = api.get("page_proofs?select=*&user_id=eq.$userId&order=created_at.desc")
        val proofs = (0 until proofsArr.length()).map { proofsArr.getJSONObject(it) }

        val paysArr = api.get("payments?select=*&user_id=eq.$userId&order=created_at.desc")
        val pays = (0 until paysArr.length()).map { paysArr.getJSONObject(it) }

        var earned = 0
        var paid = 0
        var requested = 0
        var pending = 0
        var pendingAmt = 0
        var approved = 0
        var correction = 0
        val byBook = mutableMapOf<String, BookPaySum>()

        proofs.forEach { p ->
            val st = p.optString("review_status")
            val bid = p.optInt("book_id").toString()
            val rate = rateOf(state.booksById[bid])
            val amt = rate

            val cur = byBook[bid] ?: BookPaySum(0, 0, 0, 0, 0)
            byBook[bid] = cur.copy(n = cur.n + 1)

            when (st) {
                "approved" -> {
                    earned += amt
                    approved++
                    byBook[bid] = byBook[bid]!!.copy(approved = cur.approved + 1, earned = cur.earned + amt)
                }
                "pending" -> {
                    pending++
                    pendingAmt += amt
                    byBook[bid] = byBook[bid]!!.copy(pending = cur.pending + 1)
                }
                "correction" -> {
                    correction++
                    byBook[bid] = byBook[bid]!!.copy(correction = cur.correction + 1)
                }
            }
        }

        pays.forEach { x ->
            val st = x.optString("status")
            val a = x.optInt("amount", 0)
            when (st) {
                "paid" -> paid += a
                "requested" -> requested += a
            }
        }

        val available = earned - paid - requested

        val sum = PaySum(
            earned = earned,
            available = available,
            paid = paid,
            requested = requested,
            pending = pending,
            pendingAmt = pendingAmt,
            approved = approved,
            correction = correction,
            byBook = byBook
        )

        state = state.copy(
            finData = FinData(sum, pays),
            proofs = proofs,
            pays = pays
        )
    }

    fun rateOf(book: JSONObject?): Int {
        val br = book?.optInt("rate", 0) ?: 0
        val sr = state.settings?.optInt("default_rate", 0) ?: 0
        return if (br > 0) br else if (sr > 0) sr else 2
    }

    fun requestPayment(context: Context) {
        val me = state.me ?: return
        val s = state.finData?.sum ?: return
        val min = minPayout(state.settings)

        if (s.available < min) return
        if (me.optString("payment_number").isEmpty()) return

        state = state.copy(reqBusy = true)
        viewModelScope.launch {
            try {
                val api = Api(context)
                val body = JSONObject().apply {
                    put("user_id", me.optInt("id"))
                    put("amount", s.available)
                    put("method", me.optString("payment_method"))
                    put("number", me.optString("payment_number"))
                    put("status", "requested")
                    put("pages", s.approved)
                    put("created_at", iso(System.currentTimeMillis()))
                }
                api.post("payments", body)
                loadMoney(context, me.optInt("id"))
                state = state.copy(reqBusy = false)
            } catch (e: Exception) {
                state = state.copy(reqBusy = false, error = e.message)
            }
        }
    }

    fun readGoalFromPrefs(context: Context, userId: Int): Int {
        val prefs = context.getSharedPreferences("kp_task", Context.MODE_PRIVATE)
        return prefs.getInt("goal_$userId", 5)
    }

    fun findResume(proofs: List<JSONObject>, booksById: Map<String, JSONObject>): JSONObject? {
        val approvedOrPending = proofs.filter {
            val st = it.optString("review_status")
            st == "approved" || st == "pending"
        }
        if (approvedOrPending.isEmpty()) return null

        val byBook = approvedOrPending.groupBy { it.optInt("book_id").toString() }
        for ((bid, plist) in byBook) {
            val b = booksById[bid] ?: continue
            val total = b.optInt("total_pages", 0)
            if (plist.size < total) {
                val next = plist.size + 1
                return JSONObject().apply {
                    put("book_id", bid.toInt())
                    put("stem", "page_$next")
                }
            }
        }
        return null
    }

    private fun iso(ms: Long): String {
        return java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).apply {
            timeZone = java.util.TimeZone.getTimeZone("UTC")
        }.format(java.util.Date(ms))
    }
}

private fun JSONArray.optJSONObject(index: Int): JSONObject? = try {
    getJSONObject(index)
} catch (e: Exception) {
    null
}
