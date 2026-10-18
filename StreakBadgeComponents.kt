package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Badge
import com.example.data.model.BadgeCatalog
import com.example.data.model.DisciplineStats
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun FocusStreakHeroCard(
    stats: DisciplineStats,
    onEditGoal: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "streak_fire")
    val fireScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "fire_anim"
    )

    val goalRatio = if (stats.dailyGoalMinutes > 0) {
        (stats.todayFocusMinutes.toFloat() / stats.dailyGoalMinutes.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val isGoalMet = stats.todayFocusMinutes >= stats.dailyGoalMinutes || stats.todayGoalCompleted

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("focus_streak_hero_card"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(
            1.5.dp,
            if (isGoalMet) NeonEmerald else NeonAmber
        )
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Top Row: Fire Streak + Edit Goal Chip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .scale(fireScale)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        Color(0xFFFF9500).copy(alpha = 0.45f),
                                        Color(0xFFFF3B30).copy(alpha = 0.15f),
                                        Color.Transparent
                                    )
                                )
                            )
                            .border(1.5.dp, Color(0xFFFF9500), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = "Streak flame",
                            tint = Color(0xFFFF9500),
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${stats.streakDays} DAY STREAK",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = TextPrimary,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            if (isGoalMet) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Goal completed",
                                    tint = NeonEmerald,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Text(
                            text = "Best: ${stats.bestStreakDays} Days • Consecutive study",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }

                // Edit Goal Chip
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkSurfaceVariant)
                        .clickable { onEditGoal() }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Goal",
                        tint = NeonCyan,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${stats.dailyGoalMinutes}m Goal",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Daily Study Goal Progress Bar
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isGoalMet) "TODAY'S STUDY GOAL COMPLETED! 🎯" else "TODAY'S STUDY PROGRESS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isGoalMet) NeonEmerald else NeonCyan,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "${stats.todayFocusMinutes} / ${stats.dailyGoalMinutes} min (${(goalRatio * 100).toInt()}%)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                LinearProgressIndicator(
                    progress = { goalRatio },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = if (isGoalMet) NeonEmerald else NeonCyan,
                    trackColor = DarkSurfaceVariant
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = if (isGoalMet)
                        "Discipline streak extended for today! Keep conquering your goals."
                    else {
                        val remaining = (stats.dailyGoalMinutes - stats.todayFocusMinutes).coerceAtLeast(0)
                        "$remaining minutes left today to protect your ${stats.streakDays}-day streak!"
                    },
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }
        }
    }
}

@Composable
fun StreakBadgesShowcase(
    stats: DisciplineStats,
    onBadgeClicked: (Badge) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("streak_badges_showcase")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = NeonAmber,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "STREAK REWARDS & BADGES",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonAmber,
                    letterSpacing = 1.sp
                )
            }

            val unlockedCount = BadgeCatalog.ALL_BADGES.count { stats.isBadgeUnlocked(it.id) }
            Text(
                text = "$unlockedCount of ${BadgeCatalog.ALL_BADGES.size} unlocked",
                fontSize = 11.sp,
                color = TextSecondary
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(BadgeCatalog.ALL_BADGES) { badge ->
                val isUnlocked = stats.isBadgeUnlocked(badge.id)
                BadgeMiniCard(
                    badge = badge,
                    isUnlocked = isUnlocked,
                    currentStreak = stats.streakDays,
                    onClick = { onBadgeClicked(badge) }
                )
            }
        }
    }
}

@Composable
fun BadgeMiniCard(
    badge: Badge,
    isUnlocked: Boolean,
    currentStreak: Int,
    onClick: () -> Unit
) {
    val badgeColor = Color(badge.colorHex)

    Card(
        modifier = Modifier
            .width(130.dp)
            .clickable { onClick() }
            .testTag("badge_item_${badge.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isUnlocked) badgeColor else DarkCardBorder
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Badge Emblem
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(
                        if (isUnlocked) badgeColor.copy(alpha = 0.2f) else DarkSurfaceVariant
                    )
                    .border(
                        1.dp,
                        if (isUnlocked) badgeColor else DarkCardBorder,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isUnlocked) {
                    Text(
                        text = badge.iconEmoji,
                        fontSize = 22.sp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked badge",
                        tint = TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = badge.title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (isUnlocked) TextPrimary else TextMuted,
                textAlign = TextAlign.Center,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "${badge.requiredStreakDays} Days",
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = if (isUnlocked) badgeColor else TextMuted
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Status chip
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isUnlocked) badgeColor.copy(alpha = 0.2f) else Color(0x22FFFFFF)
                    )
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = if (isUnlocked) "UNLOCKED" else "${currentStreak}/${badge.requiredStreakDays}d",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isUnlocked) badgeColor else TextMuted
                )
            }
        }
    }
}

@Composable
fun BadgeDetailDialog(
    badge: Badge,
    stats: DisciplineStats,
    onDismiss: () -> Unit
) {
    val isUnlocked = stats.isBadgeUnlocked(badge.id)
    val badgeColor = Color(badge.colorHex)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = null,
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(badgeColor.copy(alpha = 0.2f))
                        .border(2.dp, badgeColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = badge.iconEmoji,
                        fontSize = 36.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = badge.tierName.uppercase(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    color = badgeColor,
                    letterSpacing = 1.sp
                )

                Text(
                    text = badge.title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Requires ${badge.requiredStreakDays} Consecutive Days of Meeting Study Goal",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(14.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = badge.description,
                            fontSize = 12.sp,
                            color = TextPrimary,
                            lineHeight = 16.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "\"${badge.motto}\"",
                            fontSize = 11.sp,
                            color = NeonCyan,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Progress or Unlocked Banner
                if (isUnlocked) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0x3310B981))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = NeonEmerald,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "BADGE EARNED & ACTIVE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonEmerald
                        )
                    }
                } else {
                    val progress = (stats.streakDays.toFloat() / badge.requiredStreakDays.toFloat()).coerceIn(0f, 1f)
                    val daysRemaining = (badge.requiredStreakDays - stats.streakDays).coerceAtLeast(1)

                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Streak Progress",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                            Text(
                                text = "${stats.streakDays} / ${badge.requiredStreakDays} Days",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = badgeColor,
                            trackColor = DarkSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$daysRemaining consecutive days to unlock this badge",
                            fontSize = 10.sp,
                            color = TextMuted,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = badgeColor)
            ) {
                Text("Understood", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = DarkSurface
    )
}

@Composable
fun DailyGoalDialog(
    currentGoal: Int,
    onSaveGoal: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedGoal by remember { mutableIntStateOf(currentGoal) }
    val options = listOf(25, 45, 50, 60, 90, 120)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Set Daily Study Goal", color = TextPrimary, fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                Text(
                    text = "Hitting this goal every day maintains your Focus Streak and unlocks milestone badges:",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    options.take(3).forEach { mins ->
                        val isSelected = selectedGoal == mins
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) NeonCyan else DarkSurfaceVariant)
                                .clickable { selectedGoal = mins }
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = "$mins m",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.Black else TextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    options.drop(3).forEach { mins ->
                        val isSelected = selectedGoal == mins
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) NeonCyan else DarkSurfaceVariant)
                                .clickable { selectedGoal = mins }
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = "$mins m",
                                fontSize = 13.sp,
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
                    onSaveGoal(selectedGoal)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = NeonEmerald)
            ) {
                Text("Set Goal", color = Color.Black, fontWeight = FontWeight.Bold)
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

@Composable
fun BadgeCelebrationDialog(
    badge: Badge,
    onDismiss: () -> Unit
) {
    val badgeColor = Color(badge.colorHex)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = null,
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🎉 MILESTONE UNLOCKED! 🎉",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = NeonAmber,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(badgeColor.copy(alpha = 0.25f))
                        .border(2.dp, badgeColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = badge.iconEmoji,
                        fontSize = 42.sp
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = badge.title,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "${badge.requiredStreakDays}-Day Streak Achieved!",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = badgeColor
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = badge.description,
                    fontSize = 12.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "\"${badge.motto}\"",
                    fontSize = 11.sp,
                    color = NeonCyan,
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = badgeColor)
            ) {
                Text("Claim Reward", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = DarkSurface
    )
}

