package com.meninocoiso.beatstarcommunity.util

import java.time.LocalDate

class DateUtils {
	companion object {
		fun toRelativeString(date: LocalDate): String {
			val now = LocalDate.now()
			val diff = now.toEpochDay() - date.toEpochDay() // days

			// days
			if (diff < 7) { // 7 days
				return "$diff days ago"
			}

			// weeks
			return "$diff weeks ago"
		}

		fun toDurationString(seconds: Float): String {
			val minutes = (seconds % 3600) / 60
			val formattedSeconds = seconds % 60

			return "${minutes.toInt()}m${formattedSeconds.toInt()}s"
		}
	}
}