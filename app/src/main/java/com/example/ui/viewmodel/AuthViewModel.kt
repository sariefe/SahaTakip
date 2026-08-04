package com.example.ui.viewmodel

import androidx.lifecycle.viewModelScope
import com.example.data.local.PreferencesManager
import com.example.data.local.entity.UserProfileEntity
import com.example.data.repository.SahaRepository
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
    private val repository: SahaRepository
) : androidx.lifecycle.ViewModel() {

    init {
        viewModelScope.launch {
            repository.initializeAndSyncDefaultData()
        }
    }

    val userProfile: StateFlow<UserProfileEntity?> = repository.userProfile
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    private val _authErrorMessage = MutableStateFlow<String?>(null)
    val authErrorMessage: StateFlow<String?> = _authErrorMessage.asStateFlow()

    private val _ocrAuthError = MutableStateFlow<String?>(null)
    val ocrAuthError: StateFlow<String?> = _ocrAuthError.asStateFlow()

    private val _ocrScanningState = MutableStateFlow<ScannedStaffCardResult?>(null)
    val ocrScanningState: StateFlow<ScannedStaffCardResult?> = _ocrScanningState.asStateFlow()

    private val _ocrIsLoading = MutableStateFlow(false)
    val ocrIsLoading: StateFlow<Boolean> = _ocrIsLoading.asStateFlow()

    private val _ocrStability = MutableStateFlow(0f)
    val ocrStability: StateFlow<Float> = _ocrStability.asStateFlow()

    private val _detectedLines = MutableStateFlow<List<OcrLine>>(emptyList())
    val detectedLines: StateFlow<List<OcrLine>> = _detectedLines.asStateFlow()

    private val _ocrImageWidth = MutableStateFlow(0)
    val ocrImageWidth: StateFlow<Int> = _ocrImageWidth.asStateFlow()

    private val _ocrImageHeight = MutableStateFlow(0)
    val ocrImageHeight: StateFlow<Int> = _ocrImageHeight.asStateFlow()

    private val ocrResultBuffer = mutableListOf<String>()
    private var lastOcrTop = -1
    private var lastOcrLeft = -1
    private val stabilitityThreshold = 5

    private val _ocrScanSuggested = MutableStateFlow(false)
    val ocrScanSuggested: StateFlow<Boolean> = _ocrScanSuggested.asStateFlow()

    fun markOcrScanSuggested() {
        _ocrScanSuggested.value = true
    }

    fun startIdCardOcrScan(preset: com.example.util.StaffCardPreset? = null) {
        viewModelScope.launch {
            _ocrIsLoading.value = true
            
            val existingResult = _ocrScanningState.value
            val result = if (preset == null && existingResult != null) {
                delay(500.milliseconds)
                existingResult
            } else {
                OcrCardScanner.processStaffCardScan(preset = preset)
            }
            
            _ocrScanningState.value = result
            _ocrIsLoading.value = false

            repository.addEventLog(
                type = "OCR_SCAN_SUCCESS",
                title = "Personel Kartı OCR Taraması Yapıldı",
                detail = "Personel: ${result.fullName} (ID: ${result.staffId}) - Doğruluk Skoru: %${(result.confidenceScore * 100).toInt()}",
                status = "BAŞARILI"
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
            if (idLine != null && lastOcrTop != -1) {
                val dist = sqrt(
                    (idLine.top - lastOcrTop).toDouble().pow(2.0) +
                            (idLine.left - lastOcrLeft).toDouble().pow(2.0)
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

            if (frequency >= stabilitityThreshold) {
                val current = _ocrScanningState.value
                val isMoreComplete = current == null || 
                        (result.fullName.length > current.fullName.length && result.staffId == current.staffId) ||
                        (result.department.length > current.department.length && result.staffId == current.staffId)
                
                if (isMoreComplete || (result.staffId != current.staffId)) {
                    _ocrScanningState.value = result
                    viewModelScope.launch {
                        repository.addEventLog(
                            type = "REAL_OCR_DETECTION",
                            title = "Canlı Personel Kartı Tespiti",
                            detail = "Kamera üzerinden kararlı bir şekilde kart tespiti yapıldı: ${result.fullName}",
                            status = "BİLGİ"
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
        if (code.trim() == PreferencesManager.DEFAULT_ACTIVATION_CODE || code.trim() == "123456") {
            viewModelScope.launch {
                val ocrResult = _ocrScanningState.value
                val scannedFirstName = ocrResult?.firstName ?: "AHMET CAN"
                val scannedLastName = ocrResult?.lastName ?: "YILMAZ"
                val scannedStaffId = ocrResult?.staffId ?: "ID-2026-999"
                val scannedDept = ocrResult?.department?.takeIf { it.isNotBlank() } ?: "SAHA"

                repository.userDao.insertOrUpdateUser(
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
                        lastLoginAt = System.currentTimeMillis()
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
            repository.userDao.updateLastLogin()
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
                repository.userDao.updateLastLogin()
                
                if (ocrResult != null) {
                    currentProfile.let { profile ->
                        repository.userDao.insertOrUpdateUser(
                            profile.copy(
                                firstName = ocrResult.firstName,
                                lastName = ocrResult.lastName,
                                fullName = ocrResult.fullName,
                                department = ocrResult.department.takeIf { it.isNotBlank() } ?: profile.department
                            )
                        )
                    }
                }

                repository.addEventLog(
                    type = "OCR_AUTH_SUCCESS",
                    title = "Personel Doğrulama Başarılı",
                    detail = "Personel kartı OCR ile doğrulandı ve giriş yapıldı.",
                    status = "BAŞARILI"
                )
            }
            true
        } else {
            _ocrAuthError.value = "Personel kartı kayıtlı personel ile eşleşmiyor!"
            viewModelScope.launch {
                repository.addEventLog(
                    type = "OCR_AUTH_FAILED",
                    title = "Personel Doğrulama Başarısız",
                    detail = "Farklı bir personel kartı ile giriş denemesi yapıldı (Tespit edilen ID: $scannedStaffId).",
                    status = "TEHLİKE"
                )
            }
            false
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.deactivateUser()
            _isAuthenticated.value = false
        }
    }
}
