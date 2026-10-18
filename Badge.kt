package com.example.data.model

data class Badge(
    val id: String,
    val title: String,
    val subtitle: String,
    val requiredStreakDays: Int,
    val tierName: String,
    val colorHex: Long,
    val iconEmoji: String,
    val description: String,
    val motto: String
)

object BadgeCatalog {
    val ALL_BADGES = listOf(
        Badge(
            id = "badge_1",
            title = "Novice Disciple",
            subtitle = "1-Day Streak",
            requiredStreakDays = 1,
            tierName = "Bronze Tier",
            colorHex = 0xFF10B981,
            iconEmoji = "🥉",
            description = "Completed your daily study goal for 1 full day. The foundational step to self-mastery.",
            motto = "A journey of a thousand miles begins with a single step."
        ),
        Badge(
            id = "badge_3",
            title = "Iron Will",
            subtitle = "3-Day Streak",
            requiredStreakDays = 3,
            tierName = "Silver Tier",
            colorHex = 0xFF06B6D4,
            iconEmoji = "🥈",
            description = "Held the line for 3 consecutive days. Resistance is beginning to break.",
            motto = "Discipline is choosing between what you want now and what you want most."
        ),
        Badge(
            id = "badge_7",
            title = "Monk Mode",
            subtitle = "7-Day Streak",
            requiredStreakDays = 7,
            tierName = "Gold Tier",
            colorHex = 0xFFF59E0B,
            iconEmoji = "🥇",
            description = "One full week of unbreakable discipline. No social distractions breached your fortress.",
            motto = "Consistency is what transforms average into excellence."
        ),
        Badge(
            id = "badge_14",
            title = "Titanium Focus",
            subtitle = "14-Day Streak",
            requiredStreakDays = 14,
            tierName = "Platinum Tier",
            colorHex = 0xFF8B5CF6,
            iconEmoji = "💎",
            description = "Two consecutive weeks of meeting study goals. Focus is now an involuntary habit.",
            motto = "We are what we repeatedly do. Excellence, then, is not an act, but a habit."
        ),
        Badge(
            id = "badge_30",
            title = "Grandmaster Mind",
            subtitle = "30-Day Streak",
            requiredStreakDays = 30,
            tierName = "Mythic Tier",
            colorHex = 0xFFFF3366,
            iconEmoji = "👑",
            description = "A full month of absolute self-control. You have mastered your mobile impulses.",
            motto = "He who conquers others is strong; he who conquers himself is mighty."
        )
    )

    fun getBadge(id: String): Badge? = ALL_BADGES.find { it.id == id }
}
