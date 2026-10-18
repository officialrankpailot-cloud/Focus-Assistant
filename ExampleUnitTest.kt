package com.example

import org.junit.Assert.*
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testBadgeCatalogHasFiveMilestones() {
    val badges = com.example.data.model.BadgeCatalog.ALL_BADGES
    assertEquals(5, badges.size)
    assertEquals(1, badges[0].requiredStreakDays)
    assertEquals(3, badges[1].requiredStreakDays)
    assertEquals(7, badges[2].requiredStreakDays)
    assertEquals(14, badges[3].requiredStreakDays)
    assertEquals(30, badges[4].requiredStreakDays)
  }

  @Test
  fun testStatsBadgeUnlockCheck() {
    val stats = com.example.data.model.DisciplineStats(
      streakDays = 5,
      unlockedBadgeIds = "badge_1,badge_3"
    )
    assertTrue(stats.isBadgeUnlocked("badge_1"))
    assertTrue(stats.isBadgeUnlocked("badge_3"))
    assertFalse(stats.isBadgeUnlocked("badge_7"))
  }

  @Test
  fun testSoundscapeCatalog() {
    val soundscapes = com.example.focus.Soundscape.values()
    assertEquals(5, soundscapes.size)
    assertNotNull(soundscapes.find { it.id == "lofi" })
    assertNotNull(soundscapes.find { it.id == "white_noise" })
    assertNotNull(soundscapes.find { it.id == "brown_noise" })
    assertNotNull(soundscapes.find { it.id == "rain" })
    assertNotNull(soundscapes.find { it.id == "stream" })
  }

  @Test
  fun testVolumeClamping() {
    com.example.focus.FocusAmbientAudioPlayer.setVolume(1.5f)
    assertEquals(1.0f, com.example.focus.FocusAmbientAudioPlayer.volume.value, 0.001f)
    com.example.focus.FocusAmbientAudioPlayer.setVolume(-0.5f)
    assertEquals(0.0f, com.example.focus.FocusAmbientAudioPlayer.volume.value, 0.001f)
    com.example.focus.FocusAmbientAudioPlayer.setVolume(0.8f)
    assertEquals(0.8f, com.example.focus.FocusAmbientAudioPlayer.volume.value, 0.001f)
  }

  @Test
  fun testStudyScheduleFormatting() {
    val scheduleMorning = com.example.data.model.StudyScheduleEntity(
      title = "Deep Study",
      startHour = 9,
      startMinute = 15,
      durationMinutes = 45
    )
    assertEquals("09:15 AM", scheduleMorning.formattedStartTime())
    assertEquals("10:00 AM", scheduleMorning.formattedEndTime())

    val scheduleEvening = com.example.data.model.StudyScheduleEntity(
      title = "Night Reading",
      startHour = 19,
      startMinute = 30,
      durationMinutes = 60
    )
    assertEquals("07:30 PM", scheduleEvening.formattedStartTime())
    assertEquals("08:30 PM", scheduleEvening.formattedEndTime())
  }
}
