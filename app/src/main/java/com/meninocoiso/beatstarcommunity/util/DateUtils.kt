package com.meninocoiso.beatstarcommunity.util

import java.time.LocalDate

class DateUtils {
	companion object {
		fun toRelativeString(date: LocalDate): String {
			val now = LocalDate.now()
			val diff = now.toEpochDay() - date.toEpochDay()

			// seconds
			if (diff < 60 * 1000) { // 1 minute
				return "${diff / 1000} seconds ago"
			}

			// minutes
			if (diff < 60 * 60 * 1000) { // 1 hour (60 minutes)
				return "${diff / (60 * 1000)} minutes ago"
			}

			// hours
			if (diff < 24 * 60 * 60 * 1000) { // 1 day (24 hours)
				return "${diff / (60 * 60 * 1000)} hours ago"
			}

			// days
			if (diff < 7 * 24 * 60 * 60 * 1000) { // 7 days
				return "${diff / (24 * 60 * 60 * 1000)} days ago"
			}

			// weeks
			return "${diff / (7 * 24 * 60 * 60 * 1000)} weeks ago"
		}
	}
}