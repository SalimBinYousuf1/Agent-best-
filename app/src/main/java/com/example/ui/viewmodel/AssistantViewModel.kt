package com.example.ui.viewmodel

import android.app.Application
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.assistant.actions.ActionExecutor
import com.example.assistant.voice.SpeechManager
import com.example.assistant.voice.SpeechState
import com.example.assistant.voice.TextToSpeechManager
import com.example.data.database.ActionHistoryEntity
import com.example.data.database.AppDatabase
import com.example.data.database.AssistantRepository
import com.example.data.database.MessageEntity
import com.example.data.groq.ChatMessage
import com.example.data.groq.GroqApiClient
import com.example.data.groq.GroqResult
import com.example.data.permissions.PermissionManager
import com.example.data.permissions.PermissionStatus
import com.example.data.permissions.RoleStatus
import com.example.data.preferences.EncryptedPreferencesManager
import com.example.data.database.ConversationEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AssistantViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    val prefsManager = EncryptedPreferencesManager(context)
    val permissionManager = PermissionManager(context)

    private val database = AppDatabase.getInstance(context)
    val repository = AssistantRepository(database.assistantDao())

    val groqClient = GroqApiClient(prefsManager)
    val actionExecutor = ActionExecutor(context, prefsManager, repository)

    private val ttsManager = TextToSpeechManager(context)
    val speechManager = SpeechManager(context) { recognizedText ->
        sendUserPrompt(recognizedText)
    }

    // UI States
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _lastUserPrompt = MutableStateFlow<String?>(null)
    val lastUserPrompt: StateFlow<String?> = _lastUserPrompt.asStateFlow()

    private val _userFeedbackError = MutableStateFlow<String?>(null)
    val userFeedbackError: StateFlow<String?> = _userFeedbackError.asStateFlow()

    private val _accessibleModels = MutableStateFlow<List<String>>(emptyList())
    val accessibleModels: StateFlow<List<String>> = _accessibleModels.asStateFlow()

    private val _permissionStatuses = MutableStateFlow<List<PermissionStatus>>(emptyList())
    val permissionStatuses: StateFlow<List<PermissionStatus>> = _permissionStatuses.asStateFlow()

    private val _roleStatuses = MutableStateFlow<List<RoleStatus>>(emptyList())
    val roleStatuses: StateFlow<List<RoleStatus>> = _roleStatuses.asStateFlow()

    private val _isOnline = MutableStateFlow(checkOnlineStatus())
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _selectedModel = MutableStateFlow(prefsManager.selectedModel)
    val selectedModel: StateFlow<String> = _selectedModel.asStateFlow()

    private val _isAutonomousMode = MutableStateFlow(prefsManager.isAutonomousModeEnabled)
    val isAutonomousMode: StateFlow<Boolean> = _isAutonomousMode.asStateFlow()

    private val _isSetupCompleted = MutableStateFlow(prefsManager.isSetupCompleted)
    val isSetupCompleted: StateFlow<Boolean> = _isSetupCompleted.asStateFlow()

    private val _glassTransparency = MutableStateFlow(prefsManager.glassTransparency)
    val glassTransparency: StateFlow<Float> = _glassTransparency.asStateFlow()

    private val _isReducedTransparency = MutableStateFlow(prefsManager.isReducedTransparency)
    val isReducedTransparency: StateFlow<Boolean> = _isReducedTransparency.asStateFlow()

    private val _currentConversationId = MutableStateFlow(1L)
    val currentConversationId: StateFlow<Long> = _currentConversationId.asStateFlow()

    val conversations: StateFlow<List<ConversationEntity>> = repository.conversations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val messages: StateFlow<List<MessageEntity>> = _currentConversationId
        .flatMapLatest { convId -> repository.getMessages(convId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val actionHistory: StateFlow<List<ActionHistoryEntity>> = repository.actionHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val speechState = speechManager.speechState
    val soundLevel = speechManager.soundLevel

    private var activeJob: Job? = null

    init {
        viewModelScope.launch {
            repository.getOrCreateDefaultConversation()
            refreshSystemStatuses()
            if (prefsManager.hasGroqApiKey()) {
                refreshGroqModels()
            }
        }
    }

    fun checkOnlineStatus(): Boolean {
        val cm = context.getSystemService(ConnectivityManager::class.java) ?: return false
        val activeNetwork = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun refreshSystemStatuses() {
        _isOnline.value = checkOnlineStatus()

        // Permissions
        val permissions = listOf(
            PermissionStatus(
                name = "Microphone",
                permission = android.Manifest.permission.RECORD_AUDIO,
                isGranted = permissionManager.isPermissionGranted(android.Manifest.permission.RECORD_AUDIO),
                rationale = "Required for hands-free voice commands."
            ),
            PermissionStatus(
                name = "Contacts",
                permission = android.Manifest.permission.READ_CONTACTS,
                isGranted = permissionManager.isPermissionGranted(android.Manifest.permission.READ_CONTACTS),
                rationale = "Required to resolve contact names for calls and messages."
            ),
            PermissionStatus(
                name = "Read Calendar",
                permission = android.Manifest.permission.READ_CALENDAR,
                isGranted = permissionManager.isPermissionGranted(android.Manifest.permission.READ_CALENDAR),
                rationale = "Enables schedule conflict checks."
            ),
            PermissionStatus(
                name = "Write Calendar",
                permission = android.Manifest.permission.WRITE_CALENDAR,
                isGranted = permissionManager.isPermissionGranted(android.Manifest.permission.WRITE_CALENDAR),
                rationale = "Enables direct autonomous event creation."
            ),
            PermissionStatus(
                name = "Direct SMS",
                permission = android.Manifest.permission.SEND_SMS,
                isGranted = permissionManager.isPermissionGranted(android.Manifest.permission.SEND_SMS),
                rationale = "Allows sending texts without leaving the assistant."
            ),
            PermissionStatus(
                name = "Direct Phone Call",
                permission = android.Manifest.permission.CALL_PHONE,
                isGranted = permissionManager.isPermissionGranted(android.Manifest.permission.CALL_PHONE),
                rationale = "Allows placing phone calls without opening the dialer."
            ),
            PermissionStatus(
                name = "Notifications",
                permission = android.Manifest.permission.POST_NOTIFICATIONS,
                isGranted = permissionManager.isNotificationPermissionGranted(),
                rationale = "Delivers background execution feedback."
            )
        )
        _permissionStatuses.value = permissions

        // Roles
        _roleStatuses.value = listOf(
            permissionManager.getAssistantRoleStatus(),
            permissionManager.getSmsRoleStatus(),
            permissionManager.getDialerRoleStatus()
        )
    }

    fun refreshGroqModels() {
        viewModelScope.launch {
            when (val result = groqClient.fetchAccessibleModels()) {
                is GroqResult.Success -> {
                    _accessibleModels.value = result.data
                    _selectedModel.value = prefsManager.selectedModel
                }
                is GroqResult.Error -> {
                    _userFeedbackError.value = result.userMessage
                }
            }
        }
    }

    fun sendUserPrompt(prompt: String) {
        val trimmed = prompt.trim()
        if (trimmed.isEmpty()) return

        _lastUserPrompt.value = trimmed
        _userFeedbackError.value = null
        _isLoading.value = true

        activeJob?.cancel()
        val convId = _currentConversationId.value
        activeJob = viewModelScope.launch {
            try {
                // Update title if needed
                if (messages.value.isEmpty()) {
                    val previewTitle = if (trimmed.length > 30) trimmed.take(30) + "…" else trimmed
                    repository.updateConversationTitle(convId, previewTitle)
                }

                // 1. Save user message to database
                repository.addMessage(
                    MessageEntity(
                        conversationId = convId,
                        sender = "USER",
                        content = trimmed,
                        timestamp = System.currentTimeMillis()
                    )
                )

                // 2. Query Groq API
                val recentMessages = messages.value.takeLast(6).map {
                    ChatMessage(
                        role = if (it.sender == "USER") "user" else "assistant",
                        content = it.content
                    )
                }

                when (val result = groqClient.processAssistantRequest(trimmed, recentMessages)) {
                    is GroqResult.Success -> {
                        val decision = result.data
                        // 3. Execute action based on autonomous policy
                        val actionResult = actionExecutor.execute(decision, trimmed)

                        // 4. Save assistant reply to database
                        repository.addMessage(
                            MessageEntity(
                                conversationId = convId,
                                sender = "ASSISTANT",
                                content = actionResult.replyText,
                                timestamp = System.currentTimeMillis(),
                                kind = decision.kind.name,
                                actionType = decision.action.name,
                                actionStatus = actionResult.status,
                                actionDetails = actionResult.details
                            )
                        )

                        // 5. Speak reply if TTS enabled
                        if (prefsManager.isTtsEnabled) {
                            ttsManager.speak(actionResult.replyText)
                        }
                    }
                    is GroqResult.Error -> {
                        val errorText = result.userMessage
                        repository.addMessage(
                            MessageEntity(
                                conversationId = convId,
                                sender = "ASSISTANT",
                                content = errorText,
                                timestamp = System.currentTimeMillis(),
                                kind = "UNSUPPORTED",
                                actionStatus = "FAILED"
                            )
                        )
                        _userFeedbackError.value = errorText
                    }
                }
            } catch (e: Exception) {
                val fallbackMsg = "Could not process request. Please try again."
                repository.addMessage(
                    MessageEntity(
                        conversationId = convId,
                        sender = "ASSISTANT",
                        content = fallbackMsg,
                        timestamp = System.currentTimeMillis(),
                        actionStatus = "FAILED"
                    )
                )
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun startListening() {
        _userFeedbackError.value = null
        speechManager.startListening()
    }

    fun stopListening() {
        speechManager.stopListening()
    }

    fun cancelCurrentRequest() {
        activeJob?.cancel()
        activeJob = null
        speechManager.stopListening()
        _isLoading.value = false
    }

    fun retryLastRequest() {
        val last = _lastUserPrompt.value
        if (!last.isNullOrBlank()) {
            sendUserPrompt(last)
        }
    }

    fun startNewConversation() {
        viewModelScope.launch {
            val title = "Chat ${SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date())}"
            val newId = repository.createConversation(title)
            _currentConversationId.value = newId
            _userFeedbackError.value = null
        }
    }

    fun selectConversation(conversationId: Long) {
        _currentConversationId.value = conversationId
        _userFeedbackError.value = null
    }

    fun deleteConversation(conversationId: Long) {
        viewModelScope.launch {
            repository.deleteConversation(conversationId)
            if (_currentConversationId.value == conversationId) {
                val defaultId = repository.getOrCreateDefaultConversation()
                _currentConversationId.value = defaultId
            }
        }
    }

    fun clearCurrentConversation() {
        viewModelScope.launch {
            repository.clearCurrentConversation(_currentConversationId.value)
        }
    }

    fun clearAllActionHistory() {
        viewModelScope.launch {
            repository.clearActionHistory()
        }
    }

    fun saveGroqApiKey(apiKey: String) {
        prefsManager.saveGroqApiKey(apiKey)
        refreshGroqModels()
    }

    fun deleteGroqApiKey() {
        prefsManager.deleteGroqApiKey()
        _accessibleModels.value = emptyList()
    }

    fun selectModel(model: String) {
        prefsManager.selectedModel = model
        _selectedModel.value = model
    }

    fun setAutonomousMode(enabled: Boolean) {
        prefsManager.isAutonomousModeEnabled = enabled
        _isAutonomousMode.value = enabled
    }

    fun completeSetup() {
        setSetupCompleted(true)
    }

    fun setSetupCompleted(completed: Boolean) {
        prefsManager.isSetupCompleted = completed
        _isSetupCompleted.value = completed
    }

    fun setDirectSms(enabled: Boolean) {
        prefsManager.isDirectSmsEnabled = enabled
    }

    fun setDirectCall(enabled: Boolean) {
        prefsManager.isDirectCallEnabled = enabled
    }

    fun setTts(enabled: Boolean) {
        prefsManager.isTtsEnabled = enabled
    }

    fun setGlassTransparency(value: Float) {
        prefsManager.glassTransparency = value
        _glassTransparency.value = value
    }

    fun setReducedTransparency(value: Boolean) {
        prefsManager.isReducedTransparency = value
        _isReducedTransparency.value = value
    }

    fun dismissError() {
        _userFeedbackError.value = null
    }

    override fun onCleared() {
        super.onCleared()
        speechManager.stopListening()
        ttsManager.shutdown()
    }
}
