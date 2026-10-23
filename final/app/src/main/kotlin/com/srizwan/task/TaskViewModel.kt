package com.srizwan.task

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.srizwan.task.data.Api
import com.srizwan.task.task.BookUserCount
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

enum class TaskTab { TASK, PAY, RANK }

data class TaskState(
    val tab: TaskTab = TaskTab.TASK,
    val status: String = "loading",
    val spinning: Boolean = false,
    val query: String = "",
    val books: List<JSONObject> = emptyList(),
    val booksById: Map<String, JSONObject> = emptyMap(),
    val proofs: List<JSONObject> = emptyList(),
    val bookCounts: Map<String, Int> = emptyMap(),
    val byBook: Map<String, BookUserCount> = emptyMap(),
    val goal: Int = 5,
    val resume: JSONObject? = null,
    val finData: FinData = FinData(),
    val rankData: RankData = RankData()
)

data class FinData(
    val rates: Map<Int, Int> = emptyMap(),
    val balance: Int = 0,
    val pending: Int = 0,
    val paid: Int = 0
)

data class RankData(
    val list: List<JSONObject> = emptyList()
)

class TaskViewModel : ViewModel() {
    private val _state = MutableStateFlow(TaskState())
    val state = _state.asStateFlow()

    fun load(context: Context) {
        val s = Session.get(context) ?: return
        val userId = s.optInt("id", 0)
        if (userId == 0) return

        viewModelScope.launch {
            _state.value = _state.value.copy(status = "loading", spinning = true)
            try {
                loadBooks(context)
                loadCount(context, userId)
                val goal = readGoalFromPrefs(context, userId)
                _state.value = _state.value.copy(status = "ready", spinning = false, goal = goal)
                loadMoney(context, userId)
            } catch (e: Exception) {
                _state.value = _state.value.copy(status = "error", spinning = false)
            }
        }
    }

    fun switchTab(t: TaskTab) {
        _state.value = _state.value.copy(tab = t)
    }

    fun setQuery(q: String) {
        _state.value = _state.value.copy(query = q)
    }

    private suspend fun loadBooks(context: Context) {
        val api = Api(context)
        val arr = api.get("app_books?select=*&order=id.asc")
        val list = (0 until arr.length()).map { arr.getJSONObject(it) }
        val map = list.associateBy { it.optInt("id", 0).toString() }
        _state.value = _state.value.copy(books = list, booksById = map)
    }

    private suspend fun loadCount(context: Context, userId: Int) {
        val api = Api(context)
        val s = Session.get(context) ?: return

        val proofsArr = api.get("page_proofs?user_id=eq.$userId&select=*&order=created_at.desc")
        val proofs = (0 until proofsArr.length()).map { proofsArr.getJSONObject(it) }

        val countMap = mutableMapOf<String, Int>()
        val byBookMap = mutableMapOf<String, BookUserCount>()

        proofs.forEach { p ->
            val bid = p.optInt("book_id", 0).toString()
            countMap[bid] = (countMap[bid] ?: 0) + 1
            byBookMap[bid] = BookUserCount((byBookMap[bid]?.n ?: 0) + 1)
        }

        val resume = if (proofs.isNotEmpty()) {
            val last = proofs.first()
            JSONObject().apply {
                put("b", last.optInt("book_id", 0).toString())
                put("p", last.optInt("page_num", 0))
            }
        } else null

        _state.value = _state.value.copy(
            proofs = proofs,
            bookCounts = countMap,
            byBook = byBookMap,
            resume = resume
        )
    }

    private suspend fun loadMoney(context: Context, userId: Int) {
        val api = Api(context)
        val books = _state.value.books

        val ratesMap = books.associate {
            val id = it.optInt("id", 0)
            val rate = it.optInt("base_rate", 0)
            id to rate
        }

        val payArr = api.get("user_payments?user_id=eq.$userId&select=*")
        var balance = 0
        var pending = 0
        var paid = 0

        for (i in 0 until payArr.length()) {
            val p = payArr.getJSONObject(i)
            val amt = p.optInt("amount", 0)
            val st = p.optString("status", "")
            when (st) {
                "pending" -> pending += amt
                "paid" -> paid += amt
            }
        }

        balance = pending

        _state.value = _state.value.copy(finData = FinData(ratesMap, balance, pending, paid))
    }

    private fun readGoalFromPrefs(context: Context, userId: Int): Int {
        val prefs = context.getSharedPreferences("kp_task", Context.MODE_PRIVATE)
        return prefs.getInt("goal_u$userId", 5)
    }
}
