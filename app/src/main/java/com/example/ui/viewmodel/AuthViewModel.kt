package com.example.ui.viewmodel

import androidx.lifecycle.viewModelScope
import com.example.data.local.PreferencesManager
import com.example.data.local.entity.UserProfileEntity
import com.example.domain.repository.EventRepository
import com.example.domain.repository.UserRepository
import com.example.util.Constants
import com.example.util.OcrCardScanner
import com.example.util.OcrLine
import com.example.util.ScannedStaffCardResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.time.Duration.Companion.milliseconds

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val eventRepository: EventRepository,
    private val preferencesManager: PreferencesManager,
) : androidx.lifecycle.ViewModel() {

    val userProfile: StateFlow<UserProfileEntity?> = userRepository.userProfile
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _isAuthenticated = MutableStateFlow(value = false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    private val _authErrorMessage = MutableStateFlow<String?>(value = null)
    val authErrorMessage: StateFlow<String?> = _authErrorMessage.asStateFlow()

    private val _ocrAuthError = MutableStateFlow<String?>(value = null)
    val ocrAuthError: StateFlow<String?> = _ocrAuthError.asStateFlow()

    private val _ocrScanningState = MutableStateFlow<ScannedStaffCardResult?>(value = null)
    val ocrScanningState: StateFlow<ScannedStaffCardResult?> = _ocrScanningState.asStateFlow()

    private val _ocrIsLoading = MutableStateFlow(value = false)
    val ocrIsLoading: StateFlow<Boolean> = _ocrIsLoading.asStateFlow()

    private val _ocrStability = MutableStateFlow(value = 0f)
    val ocrStability: StateFlow<Float> = _ocrStability.asStateFlow()

    private val _detectedLines = MutableStateFlow<List<OcrLine>>(value = emptyList())
    val detectedLines: StateFlow<List<OcrLine>> = _detectedLines.asStateFlow()

    private val _ocrImageWidth = MutableStateFlow(value = 0)
    val ocrImageWidth: StateFlow<Int> = _ocrImageWidth.asStateFlow()

    private val _ocrImageHeight = MutableStateFlow(value = 0)
    val ocrImageHeight: StateFlow<Int> = _ocrImageHeight.asStateFlow()

    private val ocrResultBuffer = mutableListOf<String>()
    private var lastOcrTop = -1
    private var lastOcrLeft = -1
    private val stabilitityThreshold = 5

    private val _ocrScanSuggested = MutableStateFlow(value = false)
    val ocrScanSuggested: StateFlow<Boolean> = _ocrScanSuggested.asStateFlow()

    fun markOcrScanSuggested() {
        _ocrScanSuggested.value = true
    }

    fun startIdCardOcrScan(preset: com.example.util.StaffCardPreset? = null) {
        viewModelScope.launch {
            _ocrIsLoading.value = true
            
            val existingResult = _ocrScanningState.value
            val result = if ((preset == null) && (existingResult != null)) {
                delay(500.milliseconds)
                existingResult
            } else {
                OcrCardScanner.processStaffCardScan(preset = preset)
            }
            
            _ocrScanningState.value = result
            _ocrIsLoading.value = false

            eventRepository.addEventLog(
                type = "OCR_SCAN_SUCCESS",
                title = "Personel Kartı OCR Taraması Yapıldı",
                detail = "Personel: ${result.fullName} (ID: ${result.staffId}) - Doğruluk Skoru: %${(result.confidenceScore * 100).toInt()}",
                status = Constants.STATUS_SUCCESS,
            )
        }
    }

    fun onRealOcrDetected(ocrLines: List<OcrLine>, imgWidth: Int, imgHeight: Int) {
        _detectedLines.value = ocrLines
        _ocrImageWidth.value = imgWidth
        _ocrImageHeight.value = imgHeight

        val result = OcrCardScanner.parseStaffCardText(ocrLines)
        if (result != null) {
            val idLine = ocrLines.find { it.text.contains(result.staffId) }
            
            var spatialWeight = 1.0f
            if (idLine != null && (lastOcrTop != -1)) {
                val dist = sqrt(
                    (idLine.top - lastOcrTop).toDouble().pow(2.0) +
                            (idLine.left - lastOcrLeft).toDouble().pow(2.0),
                )
                if (dist > 40) spatialWeight = 0.4f
            }
            
            if (idLine != null) {
                lastOcrTop = idLine.top
                lastOcrLeft = idLine.left
            }

            ocrResultBuffer.add(result.staffId)
            if (ocrResultBuffer.size > 15) ocrResultBuffer.removeAt(0)

            val frequency = ocrResultBuffer.count { it == result.staffId }
            val stability = ((frequency.toFloat() / stabilitityThreshold) * spatialWeight).coerceAtMost(1f)
            _ocrStability.value = stability

            if (stability >= 0.8f) {
                val current = _ocrScanningState.value
                val isMoreComplete = current == null || 
                        (result.fullName.length > current.fullName.length && result.staffId == current.staffId) ||
                        (result.department.length > current.department.length && result.staffId == current.staffId)
                
                if (isMoreComplete || (result.staffId != current.staffId)) {
                    _ocrScanningState.value = result
                    viewModelScope.launch {
                        eventRepository.addEventLog(
                            type = "REAL_OCR_DETECTION",
                            title = "Canlı Personel Kartı Tespiti",
                            detail = "Kamera üzerinden kararlı bir şekilde kart tespiti yapıldı: ${result.fullName}",
                            status = Constants.STATUS_INFO,
                        )
                    }
                }
            }
        } else {
            _ocrStability.value = (_ocrStability.value - 0.1f).coerceAtLeast(0f)
        }
    }

    fun clearOcrResult() {
        _ocrScanningState.value = null
        _ocrStability.value = 0f
        _detectedLines.value = emptyList()
        ocrResultBuffer.clear()
        lastOcrTop = -1
        lastOcrLeft = -1
    }

    fun activateWithCode(code: String): Boolean {
        val dynamicCode = preferencesManager.dynamicActivationCode.value
        val isValid = if (dynamicCode != null) {
            code.trim() == dynamicCode
        } else {
            code.trim() == PreferencesManager.DEFAULT_ACTIVATION_CODE
        }

        if (isValid) {
            viewModelScope.launch {
                val ocrResult = _ocrScanningState.value
                val scannedFirstName = ocrResult?.firstName ?: "AHMET CAN"
                val scannedLastName = ocrResult?.lastName ?: "YILMAZ"
                val scannedStaffId = ocrResult?.staffId ?: "ID-2026-999"
                val scannedDept = ocrResult?.department?.takeIf { it.isNotBlank() } ?: "SAHA"

                userRepository.insertOrUpdateUser(
                    UserProfileEntity(
                        id = 1,
                        firstName = scannedFirstName,
                        lastName = scannedLastName,
                        fullName = "$scannedFirstName $scannedLastName",
                        staffId = scannedStaffId,
                        department = scannedDept,
                        activationCode = code.trim(),
                        isActivated = true,
                        isBiometricEnabled = true,
                        lastLoginAt = System.currentTimeMillis(),
                    )
                )
                _isAuthenticated.value = true
                _authErrorMessage.value = null
            }
            return true
        } else {
            _authErrorMessage.value = "Geçersiz aktivasyon kodu! Lütfen doğru şifre kodunu giriniz."
            return false
        }
    }

    fun authenticateWithBiometrics(): Boolean {
        _isAuthenticated.value = true
        viewModelScope.launch {
            userRepository.updateLastLogin()
        }
        return true
    }

    fun authenticateWithOcr(scannedStaffId: String, ocrResult: ScannedStaffCardResult? = null): Boolean {
        val currentProfile = userProfile.value
        val registeredStaffId = currentProfile?.staffId
        return if (scannedStaffId == registeredStaffId) {
            _isAuthenticated.value = true
            _ocrAuthError.value = null
            viewModelScope.launch {
                userRepository.updateLastLogin()
                
                if (ocrResult != null) {
                    currentProfile.let { profile ->
                        userRepository.insertOrUpdateUser(
                            profile.copy(
                                firstName = ocrResult.firstName,
                                lastName = ocrResult.lastName,
                                fullName = ocrResult.fullName,
                                department = ocrResult.department.takeIf { it.isNotBlank() } ?: profile.department
                            )
                        )
                    }
                }

                eventRepository.addEventLog(
                    type = "OCR_AUTH_SUCCESS",
                    title = "Personel Doğrulama Başarılı",
                    detail = "Personel kartı OCR ile doğrulandı ve giriş yapıldı.",
                    status = Constants.STATUS_SUCCESS
                )
            }
            true
        } else {
            _ocrAuthError.value = "Personel kartı kayıtlı personel ile eşleşmiyor!"
            viewModelScope.launch {
                eventRepository.addEventLog(
                    type = "OCR_AUTH_FAILED",
                    title = "Personel Doğrulama Başarısız",
                    detail = "Farklı bir personel kartı ile giriş denemesi yapıldı (Tespit edilen ID: $scannedStaffId).",
                    status = Constants.STATUS_DANGER
                )
            }
            false
        }
    }

    fun logout() {
        viewModelScope.launch {
            userRepository.deactivateUser()
            clearOcrResult()
            _isAuthenticated.value = false
        }
    }
}
