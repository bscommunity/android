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
	}
}