package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AttendanceLog
import com.example.data.model.Staff
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

// Navigation tabs
enum class BioClockTab(val label: String, val icon: ImageVector) {
    SCANNER("Scan Portal", Icons.Default.Face),
    STAFF("Personnel", Icons.Default.People),
    ANALYTICS("Logs & Stats", Icons.Default.Assessment),
    CONFIG("Settings", Icons.Default.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BioClockApp(viewModel: WorkforceViewModel) {
    val allStaff by viewModel.allStaff.collectAsStateWithLifecycle()
    val allLogs by viewModel.allLogs.collectAsStateWithLifecycle()
    val todayLogs by viewModel.todayLogs.collectAsStateWithLifecycle()

    val shiftTime by viewModel.shiftTime.collectAsStateWithLifecycle()
    val gracePeriod by viewModel.gracePeriod.collectAsStateWithLifecycle()

    var currentTab by remember { mutableStateOf(BioClockTab.SCANNER) }
    var selectedStaffForConfigDetail by remember { mutableStateOf<Staff?>(null) }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                BioClockTab.values().forEach { tab ->
                    NavigationBarItem(
                        selected = currentTab == tab,
                        onClick = { currentTab = tab },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label, fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // High Density custom borderless dashboard header
            HighDensityHeader(
                onSettingsClick = { currentTab = BioClockTab.CONFIG }
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when (currentTab) {
                    BioClockTab.SCANNER -> ScannerScreen(
                        viewModel = viewModel,
                        allStaff = allStaff,
                        todayLogs = todayLogs
                    )
                    BioClockTab.STAFF -> StaffListScreen(
                        allStaff = allStaff,
                        allLogs = allLogs,
                        todayLogs = todayLogs,
                        shiftTime = shiftTime,
                        gracePeriod = gracePeriod,
                        onEditStaffShift = { staff -> selectedStaffForConfigDetail = staff },
                        onToggleAbsence = { staffId -> viewModel.toggleAbsenceOverride(staffId) }
                    )
                    BioClockTab.ANALYTICS -> AnalyticsScreen(
                        allLogs = allLogs,
                        allStaff = allStaff,
                        todayLogs = todayLogs,
                        onClearHistory = { viewModel.clearLogs() },
                        onTriggerAbsentCheck = { viewModel.checkAbsentees() }
                    )
                    BioClockTab.CONFIG -> ConfigScreen(
                        viewModel = viewModel,
                        shiftTime = shiftTime,
                        gracePeriod = gracePeriod
                    )
                }
            }
        }

        // Edit individual arrival time overlay
        selectedStaffForConfigDetail?.let { staff ->
            EditStaffArrivalDialog(
                staff = staff,
                onDismiss = { selectedStaffForConfigDetail = null },
                onConfirm = { hour, min ->
                    // Upgrade specific staff's desired arrival time
                    viewModel.updateShiftConfiguration(hour, min, gracePeriod)
                    selectedStaffForConfigDetail = null
                }
            )
        }
    }
}

@Composable
fun HighDensityHeader(onSettingsClick: () -> Unit) {
    val todayDateStr = remember {
        val sdf = SimpleDateFormat("EEEE, MMM dd '•' 'Shift A'", Locale.getDefault())
        sdf.format(Date())
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 20.dp, bottom = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = "Logo",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "BIOCLOCK TERMINAL",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.5).sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .clickable { onSettingsClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = todayDateStr,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold
        )
    }
}

// ======================== TABS IMPLEMENTATION ========================

@Composable
fun QuickStatsGrid(presentCount: Int, lateCount: Int, absentCount: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Present Card
        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isSystemInDarkTheme()) Color(0xFF003354) else Color(0xFFD3E4FF)
            )
        ) {
            Column(
                modifier = Modifier.padding(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "PRESENT",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isSystemInDarkTheme()) Color(0xFFD3E4FF) else Color(0xFF001D36),
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = presentCount.toString(),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isSystemInDarkTheme()) Color(0xFFD3E4FF) else Color(0xFF001D36)
                )
            }
        }

        // Late Card
        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isSystemInDarkTheme()) Color(0xFF690005) else Color(0xFFFFDAD6)
            )
        ) {
            Column(
                modifier = Modifier.padding(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "LATE",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isSystemInDarkTheme()) Color(0xFFFFDAD6) else Color(0xFF410002),
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = lateCount.toString(),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isSystemInDarkTheme()) Color(0xFFFFDAD6) else Color(0xFF410002)
                )
            }
        }

        // Absent Card
        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isSystemInDarkTheme()) Color(0xFF334155) else Color(0xFFF1F5F9)
            )
        ) {
            Column(
                modifier = Modifier.padding(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "ABSENT",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isSystemInDarkTheme()) Color(0xFF94A3B8) else Color(0xFF475569),
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = absentCount.toString(),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isSystemInDarkTheme()) Color(0xFFF1F5F9) else Color(0xFF475569)
                )
            }
        }
    }
}

@Composable
fun ScannerScreen(
    viewModel: WorkforceViewModel,
    allStaff: List<Staff>,
    todayLogs: Map<Int, AttendanceLog>
) {
    val scanState by viewModel.scanState.collectAsStateWithLifecycle()
    val scanProgress by viewModel.scanProgress.collectAsStateWithLifecycle()
    val scanStatusMessage by viewModel.scanStatusMessage.collectAsStateWithLifecycle()
    val scannedStaff by viewModel.scannedStaff.collectAsStateWithLifecycle()
    val scanMatchConfidence by viewModel.scanMatchConfidence.collectAsStateWithLifecycle()

    var scanningForClockIn by remember { mutableStateOf(true) }

    val clockInCount = todayLogs.values.count { it.clockInTime != null }
    val lateCount = todayLogs.values.count { it.isLate }
    val absentCount = todayLogs.values.count { it.isAbsent }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // High Density Stats Panel at the local top Level
        QuickStatsGrid(
            presentCount = clockInCount,
            lateCount = lateCount,
            absentCount = absentCount
        )

        // Toggle Clock-In or Clock-Out
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Button(
                onClick = { scanningForClockIn = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (scanningForClockIn) MaterialTheme.colorScheme.primary else Color.Transparent,
                    contentColor = if (scanningForClockIn) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Login, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Clock In", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }

            Button(
                onClick = { scanningForClockIn = false },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (!scanningForClockIn) MaterialTheme.colorScheme.primary else Color.Transparent,
                    contentColor = if (!scanningForClockIn) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Clock Out", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }

        // Biometric scanning hub view
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp) // Maintain tight high-density sizing
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF0F172A)) // High Contrast Obsidian Background
                .border(2.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            if (scanState == "IDLE") {
                // Standing Instruction State
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Face,
                        contentDescription = "Scan",
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        modifier = Modifier.size(64.dp)
                    )
                    Text(
                        text = "Face ID Terminal Ready",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Tap a staff profile below or the Scan button to run dynamic biometric face verification verification scan.",
                        color = Color.LightGray,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            } else {
                // Biometric scanning simulation actively executing
                val infiniteTransition = rememberInfiniteTransition(label = "lasersweep")
                val laserOffsetMultiplier by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1800, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "sweepAnimation"
                )

                // Simulated Cam scanning feed Matrix
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height

                    // Draw dark green grid background lines of biometric scanners
                    val gridCols = 8
                    val gridRows = 8
                    val colWidth = canvasWidth / gridCols
                    val rowHeight = canvasHeight / gridRows
                    for (i in 0..gridCols) {
                        drawLine(
                            color = Color(0xFF14532D).copy(alpha = 0.3f),
                            start = Offset(i * colWidth, 0f),
                            end = Offset(i * colWidth, canvasHeight),
                            strokeWidth = 1f
                        )
                    }
                    for (i in 0..gridRows) {
                        drawLine(
                            color = Color(0xFF14532D).copy(alpha = 0.3f),
                            start = Offset(0f, i * rowHeight),
                            end = Offset(canvasWidth, i * rowHeight),
                            strokeWidth = 1f
                        )
                    }

                    // Draw biometric bounding target oval
                    val cx = canvasWidth / 2
                    val cy = canvasHeight / 2
                    val radiusX = canvasWidth * 0.33f
                    val radiusY = canvasHeight * 0.40f

                    drawOval(
                        color = when (scanState) {
                            "MATCHED" -> Color(0xFF10B981)
                            "ERROR" -> Color(0xFFEF4444)
                            else -> Color(0xFF10B981)
                        },
                        topLeft = Offset(cx - radiusX, cy - radiusY),
                        size = androidx.compose.ui.geometry.Size(radiusX * 2, radiusY * 2),
                        style = Stroke(
                            width = 4f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 10f), 0f)
                        )
                    )

                    // Draw abstract face structure nodes simulating landmark identification
                    val eyeY = cy - radiusY * 0.3f
                    val leftEyeX = cx - radiusX * 0.35f
                    val rightEyeX = cx + radiusX * 0.35f
                    val noseY = cy + radiusY * 0.05f
                    val mouthY = cy + radiusY * 0.45f

                    val nodeIdColor = when (scanState) {
                        "MATCHED" -> Color(0xFF10B981)
                        "ERROR" -> Color(0xFFEF4444)
                        "ANALYZING" -> Color(0xFFFBBF24)
                        else -> Color(0xFF34D399)
                    }

                    // Eyes
                    drawCircle(color = nodeIdColor, radius = 6f, center = Offset(leftEyeX, eyeY))
                    drawCircle(color = nodeIdColor, radius = 6f, center = Offset(rightEyeX, eyeY))
                    // Nose
                    drawCircle(color = nodeIdColor, radius = 6f, center = Offset(cx, noseY))
                    // Mouth
                    drawCircle(color = nodeIdColor, radius = 6f, center = Offset(cx, mouthY))
                    // Cheek boundaries
                    drawCircle(color = nodeIdColor, radius = 5f, center = Offset(cx - radiusX * 0.6f, cy))
                    drawCircle(color = nodeIdColor, radius = 5f, center = Offset(cx + radiusX * 0.6f, cy))

                    // Interconnecting green scan lines
                    drawLine(color = nodeIdColor.copy(alpha = 0.4f), start = Offset(leftEyeX, eyeY), end = Offset(cx, noseY), strokeWidth = 2f)
                    drawLine(color = nodeIdColor.copy(alpha = 0.4f), start = Offset(rightEyeX, eyeY), end = Offset(cx, noseY), strokeWidth = 2f)
                    drawLine(color = nodeIdColor.copy(alpha = 0.4f), start = Offset(cx, noseY), end = Offset(cx, mouthY), strokeWidth = 2f)
                    drawLine(color = nodeIdColor.copy(alpha = 0.4f), start = Offset(cx - radiusX * 0.6f, cy), end = Offset(cx, mouthY), strokeWidth = 2f)
                    drawLine(color = nodeIdColor.copy(alpha = 0.4f), start = Offset(cx + radiusX * 0.6f, cy), end = Offset(cx, mouthY), strokeWidth = 2f)

                    // Laser horizontal sweep lines
                    val sweepY = (canvasHeight * 0.1f) + (canvasHeight * 0.8f * laserOffsetMultiplier)
                    drawLine(
                        color = Color(0xFF00FF66),
                        start = Offset(cx - radiusX * 1.1f, sweepY),
                        end = Offset(cx + radiusX * 1.1f, sweepY),
                        strokeWidth = 6f,
                        cap = StrokeCap.Round
                    )
                }

                // Scan result over-layer text
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.6f))
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "SCANNING: ${scannedStaff?.name?.uppercase() ?: ""}",
                            color = Color.Green,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "MODE: ${if (scanningForClockIn) "CLOCK-IN" else "CLOCK-OUT"}",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // Intermediate scanning values status
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color.Black.copy(alpha = 0.8f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = scanStatusMessage,
                                color = if (scanState == "ERROR") Color.Red else Color.Green,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { scanProgress },
                                color = if (scanState == "ERROR") Color.Red else Color.Green,
                                trackColor = Color.DarkGray,
                                modifier = Modifier
                                    .width(160.dp)
                                    .height(5.dp)
                                    .clip(CircleShape)
                            )
                        }
                    }
                }
            }
        }

        // Personnel terminal select instructions
        Text(
            text = "STAFF STATUS (${allStaff.size})",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            letterSpacing = 1.sp,
            modifier = Modifier.align(Alignment.Start)
        )

        // Roster horizontal list for rapid face verification simulations
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val validTeam = allStaff.take(5) // Limit explicitly to the requested 5 staff of interest
            items(validTeam) { member ->
                val log = todayLogs[member.id]
                val isClockedIn = log?.clockInTime != null
                val isClockedOut = log?.clockOutTime != null

                val statusLabel = when {
                    isClockedOut -> "CLOCKED OUT"
                    isClockedIn -> if (log?.isLate == true) "LATE (${log.lateMinutes}m)" else "ON TIME"
                    log?.isAbsent == true -> "ABSENT"
                    else -> "ABSENT" // Match HTML list showing absent default or pending check in
                }

                val badgeColors = when {
                    isClockedOut -> Triple(Color(0xFFF1F5F9), Color(0xFF64748B), Color(0xFFE2E8F0))
                    isClockedIn -> if (log?.isLate == true) {
                        Triple(Color(0xFFFEF2F2), Color(0xFFB91C1C), Color(0xFFFEE2E2))
                    } else {
                        Triple(Color(0xFFF0FDF4), Color(0xFF15803D), Color(0xFFDCFCE7))
                    }
                    log?.isAbsent == true -> Triple(Color(0xFFFEF2F2).copy(alpha = 0.6f), Color(0xFFB91C1C), Color(0xFFFEE2E2).copy(alpha = 0.6f))
                    else -> Triple(Color(0xFFF8FAFC), Color(0xFF64748B), Color(0xFFE2E8F0))
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .clickable(enabled = scanState == "IDLE") {
                                val needsClockIn = !isClockedIn || log?.isAbsent == true
                                viewModel.startFaceScanning(
                                    staff = member,
                                    isClockIn = needsClockIn,
                                    onComplete = {}
                                )
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Fancy Rounded Initial Profile Avatar
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp)) // Matching index letter block rounded corners `rounded-xl`
                                    .background(Color.parseHtmlHex(member.avatarColorHex).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = member.name.split(" ").map { it.take(1) }.joinToString(""),
                                    color = Color.parseHtmlHex(member.avatarColorHex),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Column {
                                Text(
                                    text = member.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (isClockedIn) "In: ${SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(log?.clockInTime ?: 0))}" else "Pending Check-in",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Face recognition status badges
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = badgeColors.first,
                            border = BorderStroke(1.dp, badgeColors.third),
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = statusLabel,
                                fontSize = 10.sp,
                                color = badgeColors.second,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        // Custom Bottom Face ID Panel
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Button(
                    onClick = {
                        val firstAvailable = allStaff.firstOrNull { member ->
                            val l = todayLogs[member.id]
                            l?.clockInTime == null && l?.isAbsent != true
                        } ?: allStaff.firstOrNull()

                        firstAvailable?.let { member ->
                            val log = todayLogs[member.id]
                            val isClockedIn = log?.clockInTime != null
                            viewModel.startFaceScanning(
                                staff = member,
                                isClockIn = !isClockedIn || log?.isAbsent == true,
                                onComplete = {}
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF005FB0), // Saturated blue
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Face,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Scan Face to Clock In",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
                Text(
                    text = "VERIFIED BY AI BIOMETRICS",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    letterSpacing = 1.2.sp
                )
            }
        }
    }
}

// ---------------------- STAFF ROSTER SCREEN ----------------------
@Composable
fun StaffListScreen(
    allStaff: List<Staff>,
    allLogs: List<AttendanceLog>,
    todayLogs: Map<Int, AttendanceLog>,
    shiftTime: Pair<Int, Int>,
    gracePeriod: Int,
    onEditStaffShift: (Staff) -> Unit,
    onToggleAbsence: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column {
            Text(
                text = "Personnel List & Punctuality",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Operational reports of work schedule and actual punctuality values.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val validTeam = allStaff.take(5) // Max 5 staff members explicitly as requested
            items(validTeam) { staff ->
                val todayLog = todayLogs[staff.id]
                
                // Calculate detailed statistics for this user based on logs
                val logsForThisStaff = allLogs.filter { it.staffId == staff.id }
                val totalPresent = logsForThisStaff.count { it.clockInTime != null }
                val totalAbsent = logsForThisStaff.count { it.isAbsent }
                val totalLate = logsForThisStaff.count { it.isLate }

                // Punctuality rate = On-time present / Total presence
                val punctualityRate = if (totalPresent > 0) {
                    ((totalPresent - totalLate).toFloat() / totalPresent.toFloat() * 100).roundToInt()
                } else {
                    100
                }

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // User Profile Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.parseHtmlHex(staff.avatarColorHex).copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = staff.name.split(" ").map { it.take(1) }.joinToString(""),
                                        color = Color.parseHtmlHex(staff.avatarColorHex),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Column {
                                    Text(
                                        text = staff.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = staff.role,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Core Punctuality Value % Badge
                            val ringColors = when {
                                punctualityRate >= 90 -> Triple(Color(0xFFF0FDF4), Color(0xFF15803D), Color(0xFFDCFCE7))
                                punctualityRate >= 70 -> Triple(Color(0xFFFFFBEB), Color(0xFFB45309), Color(0xFFFEF3C7))
                                else -> Triple(Color(0xFFFEF2F2), Color(0xFFB91C1C), Color(0xFFFEE2E2))
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = ringColors.first,
                                border = BorderStroke(1.dp, ringColors.third),
                                modifier = Modifier.widthIn(min = 60.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "$punctualityRate%",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 12.sp,
                                        color = ringColors.second
                                    )
                                    Text(
                                        text = "On Time",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 8.sp,
                                        color = ringColors.second
                                    )
                                }
                            }
                        }

                        // Target Arrival configuration block
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .clickable { onEditStaffShift(staff) }
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Schedule,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Target Arrival: ${String.format("%02d:%02d", staff.targetArrivalHour, staff.targetArrivalMinute)} AM",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Schedule",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Today's clock actions + status reporting
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "TODAY'S RECORD",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                if (todayLog != null) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
                                        if (todayLog.clockInTime != null) {
                                            Text(
                                                text = "IN: " + sdf.format(Date(todayLog.clockInTime)),
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = if (todayLog.isLate) Color(0xFFEF4444) else Color(0xFF10B981)
                                            )
                                        }
                                        if (todayLog.clockOutTime != null) {
                                            Text(
                                                text = "OUT: " + sdf.format(Date(todayLog.clockOutTime)),
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.Gray
                                            )
                                        }
                                        if (todayLog.isAbsent) {
                                            Text(
                                                text = "ABSENT OVERRIDE",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFEF4444) // Match Absent CSS color style (#EF4444/red)
                                            )
                                        }
                                    }
                                } else {
                                    Text(
                                        text = "No records for today",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }
                            }

                            // Absent toggle switcher action
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Mark Absent",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Switch(
                                    checked = todayLog?.isAbsent == true,
                                    onCheckedChange = { onToggleAbsence(staff.id) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color(0xFFEF4444),
                                        checkedTrackColor = Color(0xFFFEE2E2)
                                    )
                                )
                            }
                        }

                        // Horizontal historical stats counters
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            thickness = 1.dp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "$totalPresent",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Present",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "$totalLate",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 15.sp,
                                    color = Color(0xFFEF4444)
                                )
                                Text(
                                    text = "Late",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "$totalAbsent",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 15.sp,
                                    color = Color(0xFFEF4444) // Consistent absent color
                                )
                                Text(
                                    text = "Absent",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ------------------------ ANALYTICS SCREEN ------------------------

@Composable
fun AnalyticsScreen(
    allLogs: List<AttendanceLog>,
    allStaff: List<Staff>,
    todayLogs: Map<Int, AttendanceLog>,
    onClearHistory: () -> Unit,
    onTriggerAbsentCheck: () -> Unit
) {
    var showResetConfirmation by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Workforce Office Analytics",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Aggregated records for managing absent/late statuses.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Quick Actions menu button
            IconButton(
                onClick = { onTriggerAbsentCheck() },
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                ),
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Cached,
                    contentDescription = "Run Absent Scan",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // --- DASHBOARD RATIO SUMMARY CARD ---
        val totalStaffToCount = 5
        val clockInCount = todayLogs.values.count { it.clockInTime != null }
        val lateCount = todayLogs.values.count { it.isLate }
        val absentCount = todayLogs.values.count { it.isAbsent }
        val notArrivedCount = (totalStaffToCount - clockInCount - absentCount).coerceAtLeast(0)

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "TODAY'S WORKFORCE SUMMARY",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.5.sp
                )

                // Fancy graphic progress distribution bar
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(14.dp)
                            .clip(RoundedCornerShape(7.dp))
                    ) {
                        val presentPct = if (clockInCount > 0) (clockInCount - lateCount).toFloat() / totalStaffToCount else 0f
                        val latePct = if (lateCount > 0) lateCount.toFloat() / totalStaffToCount else 0f
                        val absentPct = if (absentCount > 0) absentCount.toFloat() / totalStaffToCount else 0f
                        val pendingPct = 1.0f - presentPct - latePct - absentPct

                        if (presentPct > 0) Box(modifier = Modifier.weight(presentPct).fillMaxHeight().background(Color(0xFF10B981)))
                        if (latePct > 0) Box(modifier = Modifier.weight(latePct).fillMaxHeight().background(Color(0xFFEF4444)))
                        if (absentPct > 0) Box(modifier = Modifier.weight(absentPct).fillMaxHeight().background(Color(0xFFEF4444))) // Match red Absent color
                        if (pendingPct > 0) Box(modifier = Modifier.weight(pendingPct).fillMaxHeight().background(Color.Gray.copy(alpha = 0.3f)))
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Labels and counter indicators in high density grid flow
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        rowOfIndicators(clockInCount, lateCount, absentCount, notArrivedCount)
                    }
                }
            }
        }

        // Helper trigger button for marking today's left workers absent
        Button(
            onClick = { onTriggerAbsentCheck() },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF005FB0), // High Saturated Blue CTA
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
        ) {
            Icon(Icons.Default.ManageAccounts, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Automate Absentee Checks", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Operational Logs History",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = "Clear Database",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFEF4444),
                modifier = Modifier
                    .clickable { showResetConfirmation = true }
                    .padding(4.dp)
            )
        }

        // Database Logs History list
        if (allLogs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlaylistAddCheck,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "History is empty",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = "Trigger dynamic Face Scans to log operational data.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(allLogs) { log ->
                    // Find corresponding employee
                    val staff = allStaff.find { it.id == log.staffId }
                    if (staff != null) {
                        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
                        val clockInStr = log.clockInTime?.let { sdf.format(Date(it)) } ?: "--"
                        val clockOutStr = log.clockOutTime?.let { sdf.format(Date(it)) } ?: "--"

                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.parseHtmlHex(staff.avatarColorHex).copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = staff.name.split(" ").map { it.take(1) }.joinToString(""),
                                            color = Color.parseHtmlHex(staff.avatarColorHex),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Column {
                                        Text(
                                            text = staff.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = log.dateString,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                // Clock details status badges
                                Column(
                                    horizontalAlignment = Alignment.End
                                ) {
                                    if (log.isAbsent) {
                                        Surface(
                                            color = Color(0xFFFEF2F2),
                                            shape = RoundedCornerShape(6.dp),
                                            border = BorderStroke(1.dp, Color(0xFFFEE2E2))
                                        ) {
                                            Text(
                                                text = "ABSENT",
                                                fontWeight = FontWeight.Black,
                                                fontSize = 9.sp,
                                                color = Color(0xFFB91C1C),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    } else {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(
                                                    text = "In: $clockInStr",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    color = if (log.isLate) Color(0xFFEF4444) else Color(0xFF10B981)
                                                )
                                                if (log.isLate) {
                                                    Text(
                                                        text = "${log.lateMinutes}m Late",
                                                        fontSize = 9.sp,
                                                        color = Color(0xFFEF4444),
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                            Column(horizontalAlignment = Alignment.End) {
                                                if (log.clockOutTime != null) {
                                                    Text(
                                                        text = "Out: $clockOutStr",
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.Gray
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // Match confidence metric reporting
                                    if (log.faceScanMatchPercentage > 0) {
                                        Text(
                                            text = "Face ID: ${String.format("%.1f", log.faceScanMatchPercentage)}%",
                                            fontSize = 9.sp,
                                            color = MaterialTheme.colorScheme.outline,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showResetConfirmation) {
        AlertDialog(
            onDismissRequest = { showResetConfirmation = false },
            title = { Text("Confirm Clear Logs") },
            text = { Text("Are you absolutely sure you want to completely erase all historical workforce check-in and clocking logs?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearHistory()
                        showResetConfirmation = false
                    }
                ) {
                    Text("Clear All", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun rowOfIndicators(clockInCount: Int, lateCount: Int, absentCount: Int, notArrivedCount: Int) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IndicatorLabel(color = Color(0xFF10B981), text = "On-time: ${clockInCount - lateCount}")
            IndicatorLabel(color = Color(0xFFEF4444), text = "Late: $lateCount")
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IndicatorLabel(color = Color(0xFFEF4444), text = "Absent: $absentCount")
            IndicatorLabel(color = Color.Gray, text = "Unclocked: $notArrivedCount")
        }
    }
}

@Composable
fun IndicatorLabel(color: Color, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// ------------------------ CONFIGURATION SETTINGS SCREEN ------------------------

@Composable
fun ConfigScreen(
    viewModel: WorkforceViewModel,
    shiftTime: Pair<Int, Int>,
    gracePeriod: Int
) {
    var editHour by remember(shiftTime) { mutableStateOf(shiftTime.first.toString()) }
    var editMin by remember(shiftTime) { mutableStateOf(shiftTime.second.toString()) }
    var editGrace by remember(gracePeriod) { mutableStateOf(gracePeriod.toString()) }

    var saveToastVisible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column {
            Text(
                text = "Shift Configuration",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Set work start configurations to automatically calculate lateness and absenteeism.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "COMPANY WORKING HOURS",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 0.5.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = editHour,
                        onValueChange = { editHour = it },
                        label = { Text("Hour (24h)", fontSize = 11.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = editMin,
                        onValueChange = { editMin = it },
                        label = { Text("Minute", fontSize = 11.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                OutlinedTextField(
                    value = editGrace,
                    onValueChange = { editGrace = it },
                    label = { Text("Grace Period (Minutes)", fontSize = 11.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Button(
                    onClick = {
                        val hr = editHour.toIntOrNull() ?: 9
                        val mn = editMin.toIntOrNull() ?: 0
                        val gr = editGrace.toIntOrNull() ?: 5
                        viewModel.updateShiftConfiguration(hr, mn, gr)
                        saveToastVisible = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF005FB0),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save Shift parameters", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }

        // Biometric Security Parameters card
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "BIOMETRIC ENGINE PARAMETERS",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 0.5.sp
                )

                DetailRow(label = "Facial Match Threshold", value = "96.0% (High Sec)")
                DetailRow(label = "Spoof Spoiling Shield", value = "Enabled (3D Mesh)")
                DetailRow(label = "Directory Sync", value = "Local Database")
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        if (saveToastVisible) {
            Surface(
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { saveToastVisible = false }
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Working parameters successfully saved!",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}

// ------------------------ DIALOG OVERLAYS ------------------------

@Composable
fun EditStaffArrivalDialog(
    staff: Staff,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit
) {
    var editHour by remember { mutableStateOf(staff.targetArrivalHour.toString()) }
    var editMin by remember { mutableStateOf(staff.targetArrivalMinute.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Edit Arrival: ${staff.name}") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text(
                    text = "Specify custom arrival hour & minute for ${staff.name}.",
                    style = MaterialTheme.typography.bodyMedium
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = editHour,
                        onValueChange = { editHour = it },
                        label = { Text("Hour (24h)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = editMin,
                        onValueChange = { editMin = it },
                        label = { Text("Minute") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val hr = editHour.toIntOrNull() ?: 9
                    val mn = editMin.toIntOrNull() ?: 0
                    onConfirm(hr, mn)
                }
            ) {
                Text("Update", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// ======================== HTML COLOR HELPER ========================

fun Color.Companion.parseHtmlHex(hex: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        Color(0xFF10B981) // fallback emerald
    }
}
