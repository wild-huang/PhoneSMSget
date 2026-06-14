package com.wild_huang.phonesmsget

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.telephony.SmsManager
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

enum class ColorSchemeOption {
    DYNAMIC, PURPLE, BLUE, GREEN, ORANGE
}

enum class DarkModeOption {
    SYSTEM, LIGHT, DARK, OLED
}

data class SettingsState(
    val colorScheme: ColorSchemeOption = ColorSchemeOption.DYNAMIC,
    val darkMode: DarkModeOption = DarkModeOption.SYSTEM,
    val smsText: String = "dx1091"
)

data class MainUiState(
    val phoneNumber: String = "",
    val carrier: String = "",
    val selectedSimIndex: Int = 0,
    val simCount: Int = 1,
    val isLoading: Boolean = false,
    val queryResult: QueryResult? = null,
    val error: String? = null
)

data class QueryResult(
    val generalData: String,
    val generalDataTotal: String,
    val directedData: String,
    val directedDataTotal: String,
    val callDuration: String,
    val callDurationTotal: String
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _settingsState = MutableStateFlow(SettingsState())
    val settingsState: StateFlow<SettingsState> = _settingsState.asStateFlow()

    private val subscriptionManager = application.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
    private val prefs = application.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    init {
        loadSimInfo()
        loadSettings()
    }

    private fun loadSettings() {
        val colorSchemeName = prefs.getString("color_scheme", ColorSchemeOption.DYNAMIC.name)
        val colorScheme = try {
            ColorSchemeOption.valueOf(colorSchemeName ?: ColorSchemeOption.DYNAMIC.name)
        } catch (_: Exception) {
            ColorSchemeOption.DYNAMIC
        }
        val darkModeName = prefs.getString("dark_mode", DarkModeOption.SYSTEM.name)
        val darkMode = try {
            DarkModeOption.valueOf(darkModeName ?: DarkModeOption.SYSTEM.name)
        } catch (_: Exception) {
            DarkModeOption.SYSTEM
        }
        val smsText = prefs.getString("sms_text", "dx1091") ?: "dx1091"
        _settingsState.value = SettingsState(colorScheme = colorScheme, darkMode = darkMode, smsText = smsText)
    }

    fun updateColorScheme(colorScheme: ColorSchemeOption) {
        _settingsState.value = _settingsState.value.copy(colorScheme = colorScheme)
        prefs.edit().putString("color_scheme", colorScheme.name).apply()
    }

    fun updateDarkMode(darkMode: DarkModeOption) {
        _settingsState.value = _settingsState.value.copy(darkMode = darkMode)
        prefs.edit().putString("dark_mode", darkMode.name).apply()
    }

    fun updateSmsText(smsText: String) {
        val sanitized = smsText.take(100).filter { it.isLetterOrDigit() || it == ' ' }
        _settingsState.value = _settingsState.value.copy(smsText = sanitized)
        prefs.edit().putString("sms_text", sanitized).apply()
    }

    @Suppress("DEPRECATION")
    private fun loadSimInfo() {
        try {
            val subscriptionInfoList: List<SubscriptionInfo> = subscriptionManager.activeSubscriptionInfoList ?: emptyList()
            val simCount = subscriptionInfoList.size
            val phoneNumber = if (simCount > 0) {
                subscriptionInfoList[0].number ?: "未知号码"
            } else {
                "无SIM卡"
            }
            val carrier = if (simCount > 0) {
                subscriptionInfoList[0].carrierName?.toString() ?: "未知运营商"
            } else {
                "未知运营商"
            }
            _uiState.value = _uiState.value.copy(
                phoneNumber = phoneNumber,
                carrier = carrier,
                simCount = simCount,
                selectedSimIndex = 0
            )
        } catch (_: SecurityException) {
            _uiState.value = _uiState.value.copy(
                phoneNumber = "需要权限",
                carrier = "需要权限",
                error = "需要电话权限才能获取SIM卡信息"
            )
        }
    }

    fun selectSim(index: Int) {
        if (index in 0 until _uiState.value.simCount) {
            _uiState.value = _uiState.value.copy(selectedSimIndex = index)
            updatePhoneNumberAndCarrier()
        }
    }

    @Suppress("DEPRECATION")
    private fun updatePhoneNumberAndCarrier() {
        try {
            val subscriptionInfoList: List<SubscriptionInfo> = subscriptionManager.activeSubscriptionInfoList ?: emptyList()
            val selectedSim = subscriptionInfoList.getOrNull(_uiState.value.selectedSimIndex)
            val phoneNumber = selectedSim?.number ?: "未知号码"
            val carrier = selectedSim?.carrierName?.toString() ?: "未知运营商"
            _uiState.value = _uiState.value.copy(
                phoneNumber = phoneNumber,
                carrier = carrier
            )
        } catch (_: SecurityException) {
            // Handle permission error
        }
    }

    fun startQuery() {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null, queryResult = null)
        viewModelScope.launch {
            try {
                val message = _settingsState.value.smsText
                val destinationAddress = "10086"
                sendSms(destinationAddress, message)
                
                val response = suspendCancellableCoroutine<String> { continuation ->
                    val messageBuilder = StringBuilder()
                    var lastMessageTime = 0L
                    val handler = android.os.Handler(android.os.Looper.getMainLooper())
                    
                    val receiver = object : BroadcastReceiver() {
                        override fun onReceive(context: Context, intent: android.content.Intent) {
                            if (intent.action == android.provider.Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
                                val messages = android.provider.Telephony.Sms.Intents.getMessagesFromIntent(intent)
                                for (msg in messages) {
                                    val originatingAddress = msg.originatingAddress
                                    if (originatingAddress == "10086") {
                                        messageBuilder.append(msg.messageBody)
                                        lastMessageTime = System.currentTimeMillis()
                                        
                                        handler.removeCallbacksAndMessages(null)
                                        handler.postDelayed({
                                            if (!continuation.isCompleted) {
                                                try {
                                                    getApplication<Application>().unregisterReceiver(this)
                                                } catch (_: Exception) {
                                                    // Ignore
                                                }
                                                continuation.resume(messageBuilder.toString())
                                            }
                                        }, 2000)
                                    }
                                }
                            }
                        }
                    }
                    
                    val filter = android.content.IntentFilter(android.provider.Telephony.Sms.Intents.SMS_RECEIVED_ACTION)
                    getApplication<Application>().registerReceiver(receiver, filter)
                    
                    continuation.invokeOnCancellation {
                        handler.removeCallbacksAndMessages(null)
                        try {
                            getApplication<Application>().unregisterReceiver(receiver)
                        } catch (_: Exception) {
                            // Ignore
                        }
                    }
                    
                    viewModelScope.launch {
                        delay(30000)
                        if (!continuation.isCompleted) {
                            handler.removeCallbacksAndMessages(null)
                            try {
                                getApplication<Application>().unregisterReceiver(receiver)
                            } catch (_: Exception) {
                                // Ignore
                            }
                            continuation.resumeWithException(Exception("等待回复超时"))
                        }
                    }
                }
                
                val queryResult = parseSmsResponse(response)
                _uiState.value = _uiState.value.copy(isLoading = false, queryResult = queryResult)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    @Suppress("DEPRECATION")
    fun sendSms(phoneNumber: String, message: String) {
        try {
            val subscriptionInfoList: List<SubscriptionInfo> = subscriptionManager.activeSubscriptionInfoList ?: emptyList()
            val selectedSim = subscriptionInfoList.getOrNull(_uiState.value.selectedSimIndex)
            val subscriptionId = selectedSim?.subscriptionId ?: SubscriptionManager.getDefaultSubscriptionId()
            
            val smsManager = SmsManager.getDefault()
            
            smsManager.sendTextMessage(phoneNumber, null, message, null, null)
        } catch (_: SecurityException) {
            throw Exception("没有发送短信的权限")
        } catch (e: Exception) {
            throw Exception("发送短信失败: ${e.message}")
        }
    }

    fun parseSmsResponse(message: String): QueryResult {
        val generalDataRegex = Regex("国内通用流量共([\\d.]+)M.*?还剩余([\\d.]+)M")
        val directedDataRegex = Regex("互联网卡品定向流量共([\\d.]+)M.*?还剩余([\\d.]+)M")
        val callDurationRegex = Regex("国内通话主叫时长共([\\d]+)分钟.*?还剩余([\\d]+)分钟")
        
        val generalDataMatch = generalDataRegex.find(message)
        val directedDataMatch = directedDataRegex.find(message)
        val callDurationMatch = callDurationRegex.find(message)
        
        val generalDataTotalMB = generalDataMatch?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0
        val generalDataMB = generalDataMatch?.groupValues?.get(2)?.toDoubleOrNull() ?: 0.0
        val directedDataTotalMB = directedDataMatch?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0
        val directedDataMB = directedDataMatch?.groupValues?.get(2)?.toDoubleOrNull() ?: 0.0
        val callDurationTotalMinutes = callDurationMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val callDurationMinutes = callDurationMatch?.groupValues?.get(2)?.toIntOrNull() ?: 0
        
        val generalData = if (generalDataMB >= 1024) {
            String.format(Locale.getDefault(), "%.2f", generalDataMB / 1024)
        } else {
            String.format(Locale.getDefault(), "%.2f", generalDataMB)
        }
        val generalDataUnit = if (generalDataMB >= 1024) "GB" else "MB"
        val generalDataTotal = if (generalDataTotalMB >= 1024) {
            String.format(Locale.getDefault(), "%.2f", generalDataTotalMB / 1024)
        } else {
            String.format(Locale.getDefault(), "%.2f", generalDataTotalMB)
        }
        
        val directedData = if (directedDataMB >= 1024) {
            String.format(Locale.getDefault(), "%.2f", directedDataMB / 1024)
        } else {
            String.format(Locale.getDefault(), "%.2f", directedDataMB)
        }
        val directedDataUnit = if (directedDataMB >= 1024) "GB" else "MB"
        val directedDataTotal = if (directedDataTotalMB >= 1024) {
            String.format(Locale.getDefault(), "%.2f", directedDataTotalMB / 1024)
        } else {
            String.format(Locale.getDefault(), "%.2f", directedDataTotalMB)
        }
        
        return QueryResult(
            generalData = "$generalData $generalDataUnit",
            generalDataTotal = "$generalDataTotal $generalDataUnit",
            directedData = "$directedData $directedDataUnit",
            directedDataTotal = "$directedDataTotal $directedDataUnit",
            callDuration = "$callDurationMinutes 分钟",
            callDurationTotal = "$callDurationTotalMinutes 分钟"
        )
    }
}
