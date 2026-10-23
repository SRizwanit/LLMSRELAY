package com.srizwan.task

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.srizwan.task.data.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject

enum class LoginStep { PHONE, OTP, PIN, DEV }
enum class PinMode { LOGIN, SETUP_EXISTING, SETUP_NEW }
enum class BlockReason { BOUND, USED }

sealed interface LoginEvent {
    data object GoTask : LoginEvent
    data class GoSignup(val phone: String, val pin: String) : LoginEvent
    data class Toast(val msg: String) : LoginEvent
}

class LoginViewModel(app: Application) : AndroidViewModel(app) {
    var step by mutableStateOf(LoginStep.PHONE)
        private set
    var phoneIn by mutableStateOf("")
        private set
    var otp by mutableStateOf("")
        private set
    var pin by mutableStateOf("")
        private set
    var pin2 by mutableStateOf("")
        private set
    var mode by mutableStateOf(PinMode.LOGIN)
        private set
    var blockReason by mutableStateOf(BlockReason.BOUND)
        private set
    var err by mutableStateOf("")
        private set
    var busy by mutableStateOf(false)
        private set
    var otpCode by mutableStateOf("")
        private set
    var showOtpDialog by mutableStateOf(false)
    var left by mutableIntStateOf(0)
        private set
    var focusTick by mutableIntStateOf(0)
        private set
    var event by mutableStateOf<LoginEvent?>(null)
        private set

    var phone: String = ""
        private set
    private var user: AppUser? = null
    private var timerJob: Job? = null

    fun consumeEvent() { event = null }

    fun setOtpInput(v: String) { otp = v.filter(Char::isDigit).take(6) }
    fun setPinInput(v: String) { pin = v.filter(Char::isDigit).take(4) }
    fun setPin2Input(v: String) { pin2 = v.filter(Char::isDigit).take(4) }
    fun setPhoneInput(v: String) { phoneIn = v.take(18) }

    private fun show(s: LoginStep) { step = s; err = ""; focusTick++ }

    private fun pinStep(m: PinMode) { mode = m; pin = ""; pin2 = ""; show(LoginStep.PIN) }
    private fun devStep(r: BlockReason) { blockReason = r; show(LoginStep.DEV) }

    private fun afterUserKnown(u: AppUser) {
        val dev = Session.deviceId(getApplication())
        val stored = u.deviceId
        if (!deviceEmpty(stored) && stored != dev) {
            devStep(BlockReason.BOUND)
            return
        }
        pinStep(if (PinHash.hasPin(u)) PinMode.LOGIN else PinMode.SETUP_EXISTING)
    }

    private fun startResend() {
        timerJob?.cancel()
        var s = 120
        left = s
        timerJob = viewModelScope.launch {
            while (s > 0) {
                delay(1000)
                s -= 1
                left = s
            }
        }
    }

    private suspend fun startOtp() {
        val code = UserRepo.requestOtp(phone)
        show(LoginStep.OTP)
        otp = ""
        otpCode = code
        showOtpDialog = true
        startResend()
    }

    private fun run(block: suspend () -> Unit) {
        viewModelScope.launch {
            busy = true
            err = ""
            try {
                block()
            } catch (e: Exception) {
                err = e.message ?: "কোনো ত্রুটি ঘটেছে"
            } finally {
                busy = false
            }
        }
    }

    fun onPhone() {
        run {
            val v = Phone.validate(phoneIn)
            if (!v.ok) {
                err = v.msg
                return@run
            }
            phone = v.phone
            val u = UserRepo.findUserByPhone(phone)
            user = u
            if (u != null) {
                if (u.isBlocked && !Phone.isAdminPhone(u.phone)) {
                    err = "আপনার অ্যাকাউন্ট ব্লক করা হয়েছে" +
                            (if (!u.blockedReason.isNullOrEmpty()) ": ${u.blockedReason}" else "") +
                            "। অ্যাডমিনের সাথে যোগাযোগ করুন।"
                    return@run
                }
                afterUserKnown(u)
            } else {
                startOtp()
            }
        }
    }

    fun onResend() = run { startOtp() }

    fun onOtp() {
        run {
            val c = otp.trim()
            if (c.length != 6) {
                err = "৬ ডিজিটের কোড দিন"
                return@run
            }
            if (!UserRepo.verifyOtp(phone, c)) {
                err = "দয়া করে সঠিক ওটিপি লিখুন"
                return@run
            }
            val dev = Session.deviceId(getApplication())
            if (UserRepo.findUserByDevice(dev) != null) {
                devStep(BlockReason.USED)
                return@run
            }
            pinStep(PinMode.SETUP_NEW)
        }
    }

    private suspend fun finish(u: AppUser) {
        Session.set(getApplication(), u)
        ActivityLog.log("login", "", u.id, u.fullName)
        event = LoginEvent.GoTask
    }

    fun onPin() {
        run {
            if (pin.length != 4) {
                err = "৪ ডিজিটের PIN দিন"
                return@run
            }
            if (mode != PinMode.LOGIN && pin != pin2) {
                err = "দুটি PIN মিলছে না"
                return@run
            }
            val dev = Session.deviceId(getApplication())
            val u = user ?: throw Exception("একাউন্ট খুঁজে পাওয়া যায়নি")

            when (mode) {
                PinMode.LOGIN -> {
                    if (!PinHash.verifyPin(u, pin)) {
                        pin = ""
                        err = "PIN সঠিক নয়"
                        return@run
                    }
                    if (deviceEmpty(u.deviceId)) {
                        if (UserRepo.findUserByDevice(dev, u.id) != null) {
                            devStep(BlockReason.USED)
                            return@run
                        }
                        UserRepo.patchUser(u.id, JSONObject().put("device_id", dev))
                        u.deviceId = dev
                    }
                    finish(u)
                }
                PinMode.SETUP_EXISTING -> {
                    if (UserRepo.findUserByDevice(dev, u.id) != null) {
                        devStep(BlockReason.USED)
                        return@run
                    }
                    val salt = PinHash.newSalt()
                    val hash = PinHash.hashPin(pin, salt)
                    UserRepo.patchUser(
                        u.id,
                        JSONObject()
                            .put("password_salt", salt)
                            .put("password_hash", hash)
                            .put("device_id", dev)
                    )
                    u.deviceId = dev
                    finish(u)
                }
                PinMode.SETUP_NEW -> {
                    event = LoginEvent.GoSignup(phone, pin)
                }
            }
        }
    }

    fun onRetry() {
        run {
            if (blockReason == BlockReason.BOUND) {
                val u = UserRepo.findUserByPhone(phone)
                    ?: throw Exception("একাউন্ট খুঁজে পাওয়া যায়নি")
                user = u
                if (u.isBlocked && !Phone.isAdminPhone(u.phone)) {
                    err = "আপনার অ্যাকাউন্ট ব্লক করা হয়েছে"
                    return@run
                }
                val st = u.deviceId
                val dev = Session.deviceId(getApplication())
                if (deviceEmpty(st) || st == dev) {
                    event = LoginEvent.Toast("ডিভাইস মিলেছে, এখন লগইন করুন")
                    pinStep(if (PinHash.hasPin(u)) PinMode.LOGIN else PinMode.SETUP_EXISTING)
                } else {
                    err = "এখনো অ্যাডমিন ডিভাইস রিসেট করেননি, একটু পর আবার চেষ্টা করুন"
                }
            } else {
                val dev = Session.deviceId(getApplication())
                if (UserRepo.findUserByDevice(dev) != null) {
                    err = "এই ডিভাইসে এখনো অন্য একাউন্ট বাঁধা আছে"
                } else {
                    event = LoginEvent.Toast("ডিভাইস এখন খালি আছে")
                    val u = user
                    if (u != null) {
                        pinStep(if (PinHash.hasPin(u)) PinMode.LOGIN else PinMode.SETUP_EXISTING)
                    } else {
                        pinStep(PinMode.SETUP_NEW)
                    }
                }
            }
        }
    }

    fun onChange() {
        timerJob?.cancel()
        phone = ""
        user = null
        phoneIn = ""
        show(LoginStep.PHONE)
    }

    fun tick() {
        // (left কাউন্টডাউন timerJob-এ চলে)
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
