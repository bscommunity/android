package com.meninocoiso.beatstarcommunity.util

import java.time.LocalDate

class DateUtils {
	companion object {
		fun toRelativeString(date: LocalDate): String {
			val now = LocalDate.now()
			val diff = now.toEpochDay() - date.toEpochDay() // days

			if (diff < 1) { // today
				return "today"
			}

			if (diff < 2) { // yesterday
				return "yesterday"
			}

			// days
			if (diff < 7) { // 7 days
				return "$diff days ago"
			}

			// weeks
			if (diff < 30) { // 30 days
				if (diff.toInt() == 7) {
					return "1 week ago"
				}
				
				return "${diff / 7} weeks ago"
			}
			
			// months
			if (diff < 365) { // 365 days
				if (diff.toInt() == 30) {
					return "1 month ago"
				}
				
				return "${diff / 30} months ago"
			}

			if (diff.toInt() == 365) {
				return "1 year ago"
			}
			
			return "${diff / 365} years ago"
		}

		fun toDurationString(seconds: Float): String {
			val minutes = (seconds % 3600) / 60
			val formattedSeconds = seconds % 60

			return "${minutes.toInt()}m${formattedSeconds.toInt()}s"
		}
	}
}