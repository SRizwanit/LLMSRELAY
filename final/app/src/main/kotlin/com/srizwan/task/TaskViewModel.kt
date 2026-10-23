package com.srizwan.task

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.srizwan.task.data.Api
import com.srizwan.task.task.PaySum
import com.srizwan.task.task.BookPaySum
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.URLEncoder

enum class TaskTab { TASK, PAY, RANK }

data class TaskState(
    val tab: TaskTab = TaskTab.TASK,
    val me: JSONObject? = null,
    val settings: JSONObject? = null,
    val books: List<JSONObject> = emptyList(),
    val proofs: List<JSONObject> = emptyList(),
    val pays: List<JSONObject> = emptyList(),
    val fin: FinData? = null,
    val rank: RankData? = null,
    val query: String = "",
    val goal: Int = 0,
    val resume: JSONObject? = null,
    val reqBusy: Boolean = false,
    val error: String? = null
)

data class FinData(
    val sum: PaySum,
    val booksById: Map<String, JSONObject>
)

data class RankData(
    val list: List<JSONObject> = emptyList()
)

class TaskViewModel : ViewModel() {
    var state by mutableStateOf(TaskState())
        private set

    fun load(context: Context) {
        viewModelScope.launch {
            try {
                val session = Session.get(context) ?: return@launch
                val userId = session.optInt("id")
                val api = Api(context)

                val settingsArr = api.get("app_settings?select=*&limit=1")
                val settings = if (settingsArr.length() > 0) settingsArr.getJSONObject(0) else null

                val meArr = api.get("app_users?id=eq.$userId&select=*&limit=1")
                val me = if (meArr.length() > 0) meArr.getJSONObject(0) else null

                val goal = readGoalFromPrefs(context, userId)
                val resumeArr = api.get("user_resume?user_id=eq.$userId&select=*&limit=1")
                val resume = if (resumeArr.length() > 0) resumeArr.getJSONObject(0) else null

                loadBooks(context)
                loadCount(context, userId)
                loadMoney(context, userId)

                state = state.copy(me = me, settings = settings, goal = goal, resume = resume, error = null)
            } catch (e: Exception) {
                state = state.copy(error = e.message ?: "লোড ব্যর্থ")
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
        // TODO: goal dialog state
    }

    private suspend fun loadBooks(context: Context) {
        val api = Api(context)
        val arr = api.get("app_books?select=*&order=position.asc")
        val books = (0 until arr.length()).map { arr.getJSONObject(it) }
        state = state.copy(books = books)
    }

    private suspend fun loadCount(context: Context, userId: Int) {
        val api = Api(context)
        val arr = api.get("page_proofs?user_id=eq.$userId&select=*&order=created_at.desc")
        val proofs = (0 until arr.length()).map { arr.getJSONObject(it) }
        state = state.copy(proofs = proofs)
    }

    private suspend fun loadMoney(context: Context, userId: Int) {
        val api = Api(context)
        val proofsArr = api.get("page_proofs?user_id=eq.$userId&select=book_id,review_status,amount&order=created_at.desc")
        val proofs = (0 until proofsArr.length()).map { proofsArr.getJSONObject(it) }

        val paysArr = api.get("payments?user_id=eq.$userId&select=*&order=created_at.desc")
        val pays = (0 until paysArr.length()).map { paysArr.getJSONObject(it) }

        var earned = 0
        var paid = 0
        var requested = 0
        var pending = 0
        var pendingAmt = 0
        var approved = 0
        var correction = 0
        val byBook = mutableMapOf<String, MutableMap<String, Int>>()

        proofs.forEach { p ->
            val st = p.optString("review_status", "")
            val amt = p.optInt("amount", 0)
            val bookId = p.optInt("book_id", 0).toString()
            byBook.getOrPut(bookId) {
                mutableMapOf("n" to 0, "approved" to 0, "pending" to 0, "correction" to 0, "earned" to 0)
            }.apply {
                this["n"] = (this["n"] ?: 0) + 1
            }

            when (st) {
                "approved" -> {
                    earned += amt
                    approved++
                    byBook[bookId]!!["approved"] = (byBook[bookId]!!["approved"] ?: 0) + 1
                    byBook[bookId]!!["earned"] = (byBook[bookId]!!["earned"] ?: 0) + amt
                }
                "pending" -> {
                    pending++
                    pendingAmt += amt
                    byBook[bookId]!!["pending"] = (byBook[bookId]!!["pending"] ?: 0) + 1
                }
                "correction" -> {
                    correction++
                    byBook[bookId]!!["correction"] = (byBook[bookId]!!["correction"] ?: 0) + 1
                }
            }
        }

        pays.forEach { x ->
            val st = x.optString("status", "")
            val amt = x.optInt("amount", 0)
            when (st) {
                "paid" -> paid += amt
                "requested" -> requested += amt
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
            byBook = byBook.mapValues { (_, v) ->
                BookPaySum(
                    n = v["n"] ?: 0,
                    approved = v["approved"] ?: 0,
                    pending = v["pending"] ?: 0,
                    correction = v["correction"] ?: 0,
                    earned = v["earned"] ?: 0
                )
            }
        )

        val booksById = state.books.associateBy { it.optInt("id", 0).toString() }
        state = state.copy(
            fin = FinData(sum = sum, booksById = booksById),
            pays = pays
        )
    }

    fun rateOf(book: JSONObject?): Int {
        val settings = state.settings ?: return 0
        return if (book != null) {
            book.optInt("rate_per_page", settings.optInt("default_rate", 0))
        } else {
            settings.optInt("default_rate", 0)
        }
    }

    fun requestPayment(context: Context) {
        if (state.reqBusy) return
        state = state.copy(reqBusy = true, error = null)
        viewModelScope.launch {
            try {
                val me = state.me ?: return@launch
                val userId = me.optInt("id")
                val fin = state.fin ?: return@launch
                val available = fin.sum.available

                if (available < (state.settings?.optInt("min_payout", 100) ?: 100)) {
                    state = state.copy(reqBusy = false, error = "পর্যাপ্ত টাকা নেই")
                    return@launch
                }
                if (me.optString("payment_number", "").isEmpty()) {
                    state = state.copy(reqBusy = false, error = "পেমেন্ট নাম্বার নেই")
                    return@launch
                }

                val api = Api(context)
                val body = JSONObject().apply {
                    put("user_id", userId)
                    put("amount", available)
                    put("status", "requested")
                    put("method", me.optString("payment_method"))
                    put("number", me.optString("payment_number"))
                }
                api.post("payments", body)

                loadMoney(context, userId)
                state = state.copy(reqBusy = false)
            } catch (e: Exception) {
                state = state.copy(reqBusy = false, error = e.message ?: "রিকোয়েস্ট ব্যর্থ")
            }
        }
    }

    private fun readGoalFromPrefs(context: Context, userId: Int): Int {
        val prefs = context.getSharedPreferences("kp_session", Context.MODE_PRIVATE)
        return prefs.getInt("goal_$userId", 0)
    }
}
