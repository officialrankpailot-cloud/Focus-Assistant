package com.example.ui

import android.app.TimePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.DisciplineApp
import com.example.data.model.StudyScheduleEntity
import com.example.focus.FocusSessionManager
import com.example.schedule.StudyReminderScheduler
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun ScheduleScreen(
    onStartSessionDirectly: (Int) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scheduleDao = DisciplineApp.instance.database.studyScheduleDao()

    val schedules by scheduleDao.getAllSchedules().collectAsState(initial = emptyList())

    var showAddDialog by remember { mutableStateOf(false) }
    var editingSchedule by remember { mutableStateOf<StudyScheduleEntity?>(null) }
    var scheduleToDelete by remember { mutableStateOf<StudyScheduleEntity?>(null) }

    fun toggleSchedule(schedule: StudyScheduleEntity, isEnabled: Boolean) {
        val updated = schedule.copy(isEnabled = isEnabled)
        coroutineScope.launch(Dispatchers.IO) {
            scheduleDao.updateSchedule(updated)
            if (isEnabled) {
                StudyReminderScheduler.scheduleReminder(context, updated)
            } else {
                StudyReminderScheduler.cancelReminder(context, updated.id)
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .testTag("schedule_screen")
    ) {
        item {
            // Header Hero Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Color(0x3306B6D4))
                            .border(1.dp, NeonCyan, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "Schedule",
                            tint = NeonCyan,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "DAILY STUDY SCHEDULE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Automated Reminders",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Set availability slots & get timely alerts before each session.",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Add Schedule Block Button
            Button(
                onClick = {
                    editingSchedule = null
                    showAddDialog = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("add_study_block_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NeonEmerald)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Add Study Session Block",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "YOUR STUDY BLOCKS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "${schedules.count { it.isEnabled }} active reminders",
                    fontSize = 11.sp,
                    color = NeonCyan
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        if (schedules.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No study sessions scheduled yet",
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap 'Add Study Session Block' above to configure your daily study hours and reminders.",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(schedules) { schedule ->
                ScheduleItemCard(
                    schedule = schedule,
                    onToggleEnabled = { isChecked -> toggleSchedule(schedule, isChecked) },
                    onStartFocusNow = {
                        onStartSessionDirectly(schedule.durationMinutes)
                    },
                    onEdit = {
                        editingSchedule = schedule
                        showAddDialog = true
                    },
                    onDelete = {
                        scheduleToDelete = schedule
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        item {
            Spacer(modifier = Modifier.height(30.dp))
        }
    }

    // Add / Edit Dialog
    if (showAddDialog) {
        StudyScheduleEditDialog(
            initialSchedule = editingSchedule,
            onSave = { saved ->
                coroutineScope.launch(Dispatchers.IO) {
                    val id = scheduleDao.insertSchedule(saved)
                    val full = saved.copy(id = if (saved.id != 0L) saved.id else id)
                    if (full.isEnabled) {
                        StudyReminderScheduler.scheduleReminder(context, full)
                    }
                }
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }

    // Delete Confirmation Dialog
    if (scheduleToDelete != null) {
        AlertDialog(
            onDismissRequest = { scheduleToDelete = null },
            title = {
                Text("Delete Study Session?", color = TextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "Are you sure you want to remove \"${scheduleToDelete?.title}\"? Its scheduled reminders will be canceled.",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val item = scheduleToDelete!!
                        coroutineScope.launch(Dispatchers.IO) {
                            scheduleDao.deleteSchedule(item)
                            StudyReminderScheduler.cancelReminder(context, item.id)
                        }
                        scheduleToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF3366))
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { scheduleToDelete = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = DarkSurface
        )
    }
}

@Composable
fun ScheduleItemCard(
    schedule: StudyScheduleEntity,
    onToggleEnabled: (Boolean) -> Unit,
    onStartFocusNow: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val categoryColor = Color(schedule.colorHex)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("schedule_item_${schedule.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (schedule.isEnabled) categoryColor else DarkCardBorder
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Top Row: Category tag + Time Interval + Toggle Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(categoryColor.copy(alpha = 0.2f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = schedule.category.uppercase(),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = categoryColor,
                            letterSpacing = 0.5.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "${schedule.durationMinutes} min",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }

                Switch(
                    checked = schedule.isEnabled,
                    onCheckedChange = onToggleEnabled,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.Black,
                        checkedTrackColor = categoryColor,
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = DarkSurfaceVariant
                    ),
                    modifier = Modifier.testTag("toggle_schedule_${schedule.id}")
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Time & Title
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${schedule.formattedStartTime()} – ${schedule.formattedEndTime()}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = if (schedule.isEnabled) TextPrimary else TextMuted
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = schedule.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = if (schedule.isEnabled) TextSecondary else TextMuted
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Days of week pills & reminder alert info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Days active pills
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    val daysList = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                    val activeSet = schedule.daysOfWeek.split(",").map { it.trim() }.toSet()

                    daysList.forEach { day ->
                        val isActive = activeSet.contains(day)
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(if (isActive) categoryColor.copy(alpha = 0.25f) else DarkSurfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = day.take(1),
                                fontSize = 9.sp,
                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                color = if (isActive) categoryColor else TextMuted
                            )
                        }
                    }
                }

                // Reminder tag
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = NeonAmber,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (schedule.reminderMinutesBefore > 0)
                            "${schedule.reminderMinutesBefore}m before"
                        else
                            "At start",
                        fontSize = 10.sp,
                        color = NeonAmber,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons: Start Focus Now + Edit + Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onStartFocusNow,
                    modifier = Modifier
                        .height(36.dp)
                        .testTag("start_focus_now_${schedule.id}"),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = categoryColor),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Start Focus Now", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Row {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = Color(0xFFFF5252),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StudyScheduleEditDialog(
    initialSchedule: StudyScheduleEntity?,
    onSave: (StudyScheduleEntity) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    var title by remember { mutableStateOf(initialSchedule?.title ?: "Deep Work Session") }
    var startHour by remember { mutableIntStateOf(initialSchedule?.startHour ?: 9) }
    var startMinute by remember { mutableIntStateOf(initialSchedule?.startMinute ?: 0) }
    var durationMinutes by remember { mutableIntStateOf(initialSchedule?.durationMinutes ?: 45) }
    var reminderMinutesBefore by remember { mutableIntStateOf(initialSchedule?.reminderMinutesBefore ?: 10) }
    var selectedCategory by remember { mutableStateOf(initialSchedule?.category ?: "Deep Work") }

    val allDays = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    var selectedDays by remember {
        mutableStateOf(
            initialSchedule?.daysOfWeek?.split(",")?.map { it.trim() }?.toSet()
                ?: setOf("Mon", "Tue", "Wed", "Thu", "Fri")
        )
    }

    val quickTitles = listOf("Deep Work", "Coding Flow", "Exam Revision", "Book Reading", "Problem Solving")
    val durationOptions = listOf(25, 45, 60, 90, 120)
    val reminderOptions = listOf(0, 5, 10, 15, 30)

    val categoryColor = when (selectedCategory) {
        "Reading" -> 0xFF10B981
        "Coding" -> 0xFF8B5CF6
        "Revision" -> 0xFFF59E0B
        else -> 0xFF06B6D4
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialSchedule == null) "New Study Schedule Block" else "Edit Study Block",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                // Title Input
                Text(text = "SESSION TITLE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("e.g. Mathematics Practice", color = TextMuted) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("schedule_title_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = DarkSurfaceVariant,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Quick Title Suggestions
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(quickTitles) { suggestion ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(DarkSurfaceVariant)
                                .clickable { title = suggestion }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(suggestion, fontSize = 10.sp, color = TextSecondary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Start Time & Time Picker
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "START TIME", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
                        val period = if (startHour >= 12) "PM" else "AM"
                        val h = when {
                            startHour == 0 -> 12
                            startHour > 12 -> startHour - 12
                            else -> startHour
                        }
                        Text(
                            text = String.format("%02d:%02d %s", h, startMinute, period),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            color = TextPrimary
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            TimePickerDialog(
                                context,
                                { _, h, m ->
                                    startHour = h
                                    startMinute = m
                                },
                                startHour,
                                startMinute,
                                false
                            ).show()
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan)
                    ) {
                        Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Change Time", fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Duration Selector
                Text(text = "DURATION", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(durationOptions) { mins ->
                        val isSelected = durationMinutes == mins
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) NeonCyan else DarkSurfaceVariant)
                                .clickable { durationMinutes = mins }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "$mins m",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.Black else TextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Days of week selector
                Text(text = "DAYS OF THE WEEK", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    allDays.forEach { day ->
                        val isSelected = selectedDays.contains(day)
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) NeonCyan else DarkSurfaceVariant)
                                .clickable {
                                    val current = selectedDays.toMutableSet()
                                    if (isSelected) {
                                        if (current.size > 1) current.remove(day)
                                    } else {
                                        current.add(day)
                                    }
                                    selectedDays = current
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = day.take(1),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.Black else TextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Reminder notification timing
                Text(text = "REMINDER NOTIFICATION", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NeonAmber)
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(reminderOptions) { mins ->
                        val isSelected = reminderMinutesBefore == mins
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) NeonAmber else DarkSurfaceVariant)
                                .clickable { reminderMinutesBefore = mins }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = if (mins == 0) "At start" else "${mins}m before",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.Black else TextSecondary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val schedule = StudyScheduleEntity(
                            id = initialSchedule?.id ?: 0L,
                            title = title.trim(),
                            startHour = startHour,
                            startMinute = startMinute,
                            durationMinutes = durationMinutes,
                            daysOfWeek = selectedDays.joinToString(","),
                            isEnabled = initialSchedule?.isEnabled ?: true,
                            reminderMinutesBefore = reminderMinutesBefore,
                            category = selectedCategory,
                            colorHex = categoryColor
                        )
                        onSave(schedule)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = NeonEmerald)
            ) {
                Text("Save Schedule", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        },
        containerColor = DarkSurface
    )
}
