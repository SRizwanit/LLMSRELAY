package com.srizwan.task

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

private fun mmss(s: Int) = "%02d:%02d".format(s / 60, s % 60)

@Composable
fun LoginScreen(
    onLoggedIn: () -> Unit,
    onSignup: (phone: String, pin: String) -> Unit,
    vm: LoginViewModel = viewModel()
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val phoneFocus = remember { FocusRequester() }
    val otpFocus = remember { FocusRequester() }
    val pinFocus = remember { FocusRequester() }

    LaunchedEffect(vm.focusTick) {
        if (vm.focusTick > 0) {
            when (vm.step) {
                LoginStep.OTP -> otpFocus.requestFocus()
                LoginStep.PIN -> pinFocus.requestFocus()
                else -> {}
            }
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(100)
            vm.tick()
        }
    }

    LaunchedEffect(vm.event) {
        when (val e = vm.event) {
            is LoginEvent.GoTask -> {
                vm.consumeEvent()
                onLoggedIn()
            }
            is LoginEvent.GoSignup -> {
                vm.consumeEvent()
                onSignup(e.phone, e.pin)
            }
            is LoginEvent.Toast -> {
                vm.consumeEvent()
                android.widget.Toast.makeText(context, e.msg, android.widget.Toast.LENGTH_SHORT).show()
            }
            null -> {}
        }
    }

    fun wa(msg: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/${BuildConfig.ADMIN_WA}?text=${Uri.encode(msg)}"))
        context.startActivity(intent)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(AppColors.extra.canvas, MaterialTheme.colorScheme.background)
                )
            )
            .systemBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 18.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(18.dp))

            // Brand: লোগো + শিরোনাম
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF0A4F40), Color(0xFF12896D))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = androidx.compose.ui.res.painterResource(R.drawable.rizwantask),
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = Color.White
                )
            }

            Spacer(Modifier.height(12.dp))
            Text(
                text = "কিতাব প্রুফিং",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "মোবাইল নাম্বার দিয়ে লগইন করুন",
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.extra.muted
            )

            Spacer(Modifier.height(22.dp))

            // কার্ড
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Box(modifier = Modifier.padding(20.dp)) {
                    Column {
                        // STEP: phone
                        if (vm.step == LoginStep.PHONE) {
                            FieldLabel("ফোন নাম্বার")
                            Spacer(Modifier.height(6.dp))
                            OutlinedTextField(
                                value = vm.phoneIn,
                                onValueChange = vm::setPhoneInput,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(phoneFocus),
                                placeholder = { Text("017XXXXXXXX") },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Phone,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(onDone = { vm.onPhone() }),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp)
                            )
                            Spacer(Modifier.height(16.dp))
                            PrimaryButton(
                                text = "পরবর্তী",
                                enabled = !vm.busy,
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { vm.onPhone() }
                            )
                        }

                        // STEP: otp
                        if (vm.step == LoginStep.OTP) {
                            FieldLabel("৬ ডিজিটের OTP কোড দিন")
                            Spacer(Modifier.height(6.dp))
                            OutlinedTextField(
                                value = vm.otp,
                                onValueChange = vm::setOtpInput,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(otpFocus),
                                placeholder = { Text("------", textAlign = TextAlign.Center) },
                                textStyle = LocalTextStyle.current.copy(
                                    textAlign = TextAlign.Center,
                                    letterSpacing = 0.6.sp,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(onDone = { vm.onOtp() }),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp)
                            )
                            Spacer(Modifier.height(16.dp))
                            PrimaryButton(
                                text = "ভেরিফাই করুন",
                                enabled = !vm.busy,
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { vm.onOtp() }
                            )
                            Spacer(Modifier.height(10.dp))
                            LinkButton(
                                text = if (vm.left > 0) "OTP আবার পাঠান (${mmss(vm.left)})" else "OTP আবার পাঠান",
                                enabled = vm.left == 0,
                                onClick = { vm.onResend() }
                            )
                        }

                        // STEP: pin
                        if (vm.step == LoginStep.PIN) {
                            val pinLabel = when (vm.mode) {
                                PinMode.LOGIN -> "৪ ডিজিটের PIN দিন"
                                PinMode.SETUP_EXISTING -> "আপনার অ্যাকাউন্টের জন্য নতুন ৪ ডিজিটের PIN সেট করুন"
                                PinMode.SETUP_NEW -> "আপনার নতুন একাউন্টের জন্য ৪ ডিজিটের PIN সেট করুন"
                            }
                            val pinBtn = when (vm.mode) {
                                PinMode.LOGIN -> "লগইন করুন"
                                PinMode.SETUP_EXISTING -> "PIN সেট করুন"
                                PinMode.SETUP_NEW -> "পরবর্তী ধাপ"
                            }
                            val setup = vm.mode != PinMode.LOGIN

                            FieldLabel(pinLabel)
                            Spacer(Modifier.height(6.dp))
                            PinField(
                                value = vm.pin,
                                onValueChange = vm::setPinInput,
                                focusRequester = pinFocus,
                                onDone = { vm.onPin() }
                            )

                            if (setup) {
                                Spacer(Modifier.height(14.dp))
                                FieldLabel("PIN আবার দিন")
                                Spacer(Modifier.height(6.dp))
                                PinField(
                                    value = vm.pin2,
                                    onValueChange = vm::setPin2Input,
                                    onDone = { vm.onPin() }
                                )
                            }

                            Spacer(Modifier.height(16.dp))
                            PrimaryButton(
                                text = pinBtn,
                                enabled = !vm.busy,
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { vm.onPin() }
                            )

                            if (!setup) {
                                Spacer(Modifier.height(10.dp))
                                LinkButton(
                                    text = "PIN ভুলে গেছেন? অ্যাডমিনকে WhatsApp করুন",
                                    onClick = { wa("আসসালামু আলাইকুম, আমি কিতাব প্রুফিং অ্যাপে আমার PIN ভুলে গিয়েছি। আমার ফোন নাম্বার: ${vm.phone}। দয়া করে সাহায্য করুন।") }
                                )
                            }
                        }

                        // STEP: device blocked
                        if (vm.step == LoginStep.DEV) {
                            val devMsg = if (vm.blockReason == BlockReason.BOUND) {
                                "এই একাউন্টটি অন্য একটি ডিভাইসে যুক্ত আছে। অ্যাডমিন ডিভাইস রিসেট করে দিলে নিচের 'আবার চেষ্টা করুন' বাটনে চাপ দিন।"
                            } else {
                                "এই মোবাইলে ইতিমধ্যে অন্য একটি একাউন্ট ব্যবহার করা হচ্ছে। একটি ডিভাইসে একটি মাত্র একাউন্ট চালানো যায়।"
                            }
                            Text(
                                text = devMsg,
                                style = MaterialTheme.typography.bodyMedium,
                                lineHeight = 24.sp,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(Modifier.height(16.dp))
                            OutlinedButton(
                                onClick = { vm.onRetry() },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !vm.busy,
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("আবার চেষ্টা করুন")
                            }
                            Spacer(Modifier.height(10.dp))
                            PrimaryButton(
                                text = "অ্যাডমিনকে WhatsApp এ জানান",
                                enabled = !vm.busy,
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { wa("আসসালামু আলাইকুম, আমি কিতাব প্রুফিং অ্যাপে অন্য একটি ডিভাইস ব্যবহার করতে চাই। আমার ফোন নাম্বার: ${vm.phone}। দয়া করে আমার একাউন্টের ডিভাইস রিসেট করে দিন।") }
                            )
                        }

                        // spinner
                        if (vm.busy) {
                            Spacer(Modifier.height(14.dp))
                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp))
                            }
                        }

                        // error
                        if (vm.err.isNotEmpty()) {
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = vm.err,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // change number
                        if (vm.step != LoginStep.PHONE) {
                            Spacer(Modifier.height(10.dp))
                            LinkButton(
                                text = "অন্য নাম্বার ব্যবহার করবেন?",
                                muted = true,
                                onClick = { vm.onChange() }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Text(
                text = "নতুন ইউজার? OTP ভেরিফাই করার পর প্রোফাইল তৈরি করুন",
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.extra.muted,
                textAlign = TextAlign.Center
            )
        }

        // OTP কোড দেখানোর ডায়ালগ
        if (vm.showOtpDialog) {
            AlertDialog(
                onDismissRequest = { vm.showOtpDialog = false },
                title = { Text("আপনার OTP কোড") },
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = vm.otpCode,
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.6.sp
                            ),
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "২ মিনিটের মধ্যে কোডটি লিখুন",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppColors.extra.muted,
                            textAlign = TextAlign.Center
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { vm.showOtpDialog = false }) {
                        Text("ঠিক আছে")
                    }
                }
            )
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = AppColors.extra.muted
    )
}

@Composable
private fun PinField(
    value: String,
    onValueChange: (String) -> Unit,
    focusRequester: FocusRequester = remember { FocusRequester() },
    onDone: () -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester),
        placeholder = { Text("----", textAlign = TextAlign.Center) },
        textStyle = LocalTextStyle.current.copy(
            textAlign = TextAlign.Center,
            letterSpacing = 0.6.sp,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        ),
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        singleLine = true,
        shape = RoundedCornerShape(10.dp)
    )
}

@Composable
private fun PrimaryButton(text: String, enabled: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    ) {
        Text(text, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun LinkButton(text: String, enabled: Boolean = true, muted: Boolean = false, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (muted) FontWeight.Normal else FontWeight.SemiBold,
            color = if (muted) AppColors.extra.muted else MaterialTheme.colorScheme.primary
        )
    }
}
