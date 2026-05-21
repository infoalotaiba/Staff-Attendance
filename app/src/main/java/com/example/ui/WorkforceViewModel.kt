package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.data.local.AppDatabase
import com.example.data.model.AttendanceLog
import com.example.data.model.Staff
import com.example.data.repository.WorkforceRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class WorkforceViewModel(application: Application) : AndroidViewModel(application) {

    private val db = Room.databaseBuilder(
        application,
        AppDatabase::class.java,
        "bioclock_workforce.db"
    ).build()

    private val repository = WorkforceRepository(
        db.staffDao(),
        db.attendanceLogDao(),
        db.workConfigDao()
    )

    // Flows from DB
    val allStaff: StateFlow<List<Staff>> = repository.allStaff
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allLogs: StateFlow<List<AttendanceLog>> = repository.allLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Map of today's attendance logs (StaffId -> AttendanceLog) for quick UI lookups
    private val _todayLogs = MutableStateFlow<Map<Int, AttendanceLog>>(emptyMap())
    val todayLogs: StateFlow<Map<Int, AttendanceLog>> = _todayLogs.asStateFlow()

    // Config details
    private val _shiftTime = MutableStateFlow(Pair(9, 0))
    val shiftTime: StateFlow<Pair<Int, Int>> = _shiftTime.asStateFlow()

    private val _gracePeriod = MutableStateFlow(5)
    val gracePeriod: StateFlow<Int> = _gracePeriod.asStateFlow()

    // Interactive Biometric Scanning States
    private val _scanState = MutableStateFlow("IDLE") // IDLE, DETECTING, ANALYZING, MATCHED, ERROR
    val scanState: StateFlow<String> = _scanState.asStateFlow()

    private val _scanProgress = MutableStateFlow(0f) // 0.0 to 1.0f
    val scanProgress: StateFlow<Float> = _scanProgress.asStateFlow()

    private val _scanStatusMessage = MutableStateFlow("Position face in reader area")
    val scanStatusMessage: StateFlow<String> = _scanStatusMessage.asStateFlow()

    private val _scannedStaff = MutableStateFlow<Staff?>(null)
    val scannedStaff: StateFlow<Staff?> = _scannedStaff.asStateFlow()

    private val _scanMatchConfidence = MutableStateFlow(0f)
    val scanMatchConfidence: StateFlow<Float> = _scanMatchConfidence.asStateFlow()

    init {
        viewModelScope.launch {
            // Check & populate 5 default team members if empty
            repository.checkAndPrepopulate()
            // Pull initial configurations
            refreshConfig()
            // Observe log updates periodically to map today's logs
            combine(allLogs, repository.allStaff) { logs, staff ->
                val todayDate = repository.getTodayDateString()
                logs.filter { it.dateString == todayDate }
                    .associateBy { it.staffId }
            }.collect { mappedLogs ->
                _todayLogs.value = mappedLogs
            }
        }
    }

    suspend fun refreshConfig() {
        _shiftTime.value = repository.getShiftStartTime()
        _gracePeriod.value = repository.getGracePeriodMins()
    }

    fun updateShiftConfiguration(hour: Int, minute: Int, graceMins: Int) {
        viewModelScope.launch {
            repository.saveShiftConfig(hour, minute, graceMins)
            refreshConfig()
        }
    }

    // Mark as absent if not clocked in today
    fun checkAbsentees() {
        viewModelScope.launch {
            repository.triggerAbsenteeCheck()
        }
    }

    // Clear logs for resetting or troubleshooting
    fun clearLogs() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    // Toggle manual absence/presence override directly
    fun toggleAbsenceOverride(staffId: Int) {
        viewModelScope.launch {
            val today = repository.getTodayDateString()
            val logs = allLogs.value
            val existing = logs.find { it.staffId == staffId && it.dateString == today }
            if (existing != null) {
                if (existing.isAbsent) {
                    // Turn off absence (remove log entirely, letting them clock in)
                    db.attendanceLogDao().deleteLog(existing)
                } else {
                    // Turn on absence (if they clocked in, delete clock-in info and set absent)
                    db.attendanceLogDao().insertLog(
                        existing.copy(
                            clockInTime = null,
                            clockOutTime = null,
                            isLate = false,
                            isAbsent = true,
                            lateMinutes = 0,
                            faceScanMatchPercentage = 0f
                        )
                    )
                }
            } else {
                // Set directly as absent
                repository.markAsAbsent(staffId)
            }
        }
    }

    /**
     * Start a beautiful simulated biometric scanning cycle for a selected staff member.
     * Transitions through realistic biometric scanning states.
     */
    fun startFaceScanning(staff: Staff, isClockIn: Boolean, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            _scannedStaff.value = staff
            _scanState.value = "DETECTING"
            _scanProgress.value = 0f
            _scanMatchConfidence.value = 0f

            val steps = listOf(
                "Aligning facial contour mesh..." to 0.25f,
                "Analyzing iris & eye spacing landmarks..." to 0.50f,
                "Validating 3D depth structural spoofing check..." to 0.75f,
                "Matching biometrics with personnel directory..." to 1.0f
            )

            for (step in steps) {
                _scanStatusMessage.value = step.first
                var innerProg = _scanProgress.value
                while (innerProg < step.second) {
                    innerProg += 0.05f
                    _scanProgress.value = innerProg.coerceAtMost(1f)
                    delay(40) // speed of biometric scan ticks
                }
                delay(150) // brief processing stop at milestones
            }

            // Biometric comparison matching success parameters
            _scanState.value = "ANALYZING"
            delay(400)

            // Generate high-fidelity randomized match percentages (e.g. 96.5% - 99.8%)
            val generatedMatch = 95f + (Math.random().toFloat() * 4.9f)
            _scanMatchConfidence.value = generatedMatch

            if (generatedMatch >= 96.0f) {
                _scanState.value = "MATCHED"
                _scanStatusMessage.value = "Face Identity Confirmed: Match Rate ${String.format("%.1f", generatedMatch)}%"
                delay(800)

                // Execute database action (clock in or clock out)
                if (isClockIn) {
                    repository.clockInStaff(staff.id, generatedMatch)
                } else {
                    repository.clockOutStaff(staff.id)
                }

                delay(800)
                _scanState.value = "IDLE"
                onComplete()
            } else {
                _scanState.value = "ERROR"
                _scanStatusMessage.value = "Recognition Timeout: High-Sec anti-spoof threshold failed."
                delay(1500)
                _scanState.value = "IDLE"
            }
        }
    }

    fun resetScannerState() {
        _scanState.value = "IDLE"
        _scanProgress.value = 0f
        _scannedStaff.value = null
        _scanMatchConfidence.value = 0f
        _scanStatusMessage.value = "Position face in reader area"
    }

    override fun onCleared() {
        super.onCleared()
        db.close()
    }
}
