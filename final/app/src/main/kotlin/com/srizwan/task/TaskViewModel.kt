package com.srizwan.task

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.srizwan.task.data.Api
import com.srizwan.task.data.Phone
import com.srizwan.task.data.UserRepo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

enum class TaskTab { TASK, PAY, RANK }

data class TaskState(
    val me: JSONObject? = null,
    val isAdmin: Boolean = false,
    val tab: TaskTab = TaskTab.TASK,
    val myCount: Int? = null,
    val books: List<JSONObject> = emptyList(),
    val booksStatus: String = "loading",
    val spinning: Boolean = false,
    val query: String = "",
    val bookCounts: Map<String, Int> = emptyMap(),
    val fin: FinData = FinData(),
    val goal: Int = 20,
    val rankPeriod: String = "day",
    val rank: RankData = RankData(),
    val badge: Int = 0
)

data class FinData(
    val S: JSONObject = JSONObject(),
    val proofs: List<JSONObject> = emptyList(),
    val pays: List<JSONObject> = emptyList(),
    val ready: Boolean = false
)

data class RankData(
    val loading: Boolean = false,
    val data: JSONObject? = null,
    val error: String = ""
)

class TaskViewModel : ViewModel() {
    private val _state = MutableStateFlow(TaskState())
    val state = _state.asStateFlow()

    private var api: Api? = null
    private var repo: UserRepo? = null
    private var busyCnt = false
    private var busyMoney = false

    fun load(context: Context) {
        api = Api(context)
        repo = UserRepo(api!!)

        val session = Session.get(context)
        if (session == null) {
            _state.value = _state.value.copy(booksStatus = "error")
            return
        }

        val userId = session.optInt("id")
        val phone = session.optString("phone", "")
        val isAdmin = Phone.isAdminPhone(phone)

        _state.value = _state.value.copy(
            me = session,
            isAdmin = isAdmin,
            goal = readGoalFromPrefs(context, userId)
        )

        viewModelScope.launch {
            loadBooks(context)
            loadCount(context, userId)
            loadMoney(context, userId)
        }
    }

    fun switchTab(t: TaskTab) {
        _state.value = _state.value.copy(tab = t)
    }

    private suspend fun loadBooks(context: Context) {
        _state.value = _state.value.copy(booksStatus = "loading")
        try {
            val arr = api!!.get("books?select=*&order=created_at.desc")
            val list = (0 until arr.length()).map { arr.getJSONObject(it) }
            _state.value = _state.value.copy(books = list, booksStatus = "ready")
        } catch (e: Exception) {
            _state.value = _state.value.copy(booksStatus = "error")
        }
    }

    private suspend fun loadCount(context: Context, userId: Int) {
        if (busyCnt) return
        busyCnt = true
        try {
            val arr = api!!.get("page_proofs?user_id=eq.$userId&select=stem&limit=5000")
            val cnt = arr.length()
            _state.value = _state.value.copy(myCount = cnt)
        } catch (e: Exception) {
            // ignore
        } finally {
            busyCnt = false
        }
    }

    private suspend fun loadMoney(context: Context, userId: Int) {
        if (busyMoney) return
        busyMoney = true
        try {
            val sArr = api!!.get("app_settings?id=eq.1&select=page_rate,min_payout,warn_pct,warn_min&limit=1")
            val S = if (sArr.length() > 0) sArr.getJSONObject(0) else JSONObject()

            val proofsArr = api!!.get("page_proofs?user_id=eq.$userId&select=book_id,stem,review_status,review_note,rate,created_at,reviewed_at&order=created_at.desc,book_id.asc,stem.asc&limit=5000")
            val proofs = (0 until proofsArr.length()).map { proofsArr.getJSONObject(it) }

            val paysArr = api!!.get("payments?user_id=eq.$userId&order=created_at.desc&limit=100")
            val pays = (0 until paysArr.length()).map { paysArr.getJSONObject(it) }

            _state.value = _state.value.copy(
                fin = FinData(S = S, proofs = proofs, pays = pays, ready = true)
            )
        } catch (e: Exception) {
            // ignore
        } finally {
            busyMoney = false
        }
    }

    private fun readGoalFromPrefs(context: Context, userId: Int): Int {
        val prefs = context.getSharedPreferences("kp_task", Context.MODE_PRIVATE)
        val g = prefs.getInt("goal_$userId", 20)
        return if (g in 1..1000) g else 20
    }
}
