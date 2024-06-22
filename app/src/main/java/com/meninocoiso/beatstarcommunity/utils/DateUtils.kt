package com.meninocoiso.beatstarcommunity.utils

import java.util.Calendar
import java.util.Date
import kotlin.random.Random

class DateUtils {
	companion object {
		fun toRelativeString(date: Date): String {
			val now = Date()
			val diff = now.time - date.time

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


		fun getRandomDateInYear(year: Int): Date {
			// Note: Implementation with new Java API (not used because of min SDK version to match Beatstar)
			/*val isLeapYear = LocalDate.of(year, 1, 1).isLeapYear
			val daysInYear = if (isLeapYear) 366 else 365
			val randomDayOfYear =
				Random.nextInt(1, daysInYear + 1)  // Random day between 1 and daysInYear
			return LocalDate.ofYearDay(year, randomDayOfYear)*/

			val calendar = Calendar.getInstance()

			// Set the calendar to the start of the year
			calendar.set(year, Calendar.JANUARY, 1, 0, 0, 0)
			calendar.set(Calendar.MILLISECOND, 0)
			val startOfYear = calendar.time

			// Determine if the year is a leap year
			val isLeapYear = calendar.getActualMaximum(Calendar.DAY_OF_YEAR) > 365

			// Calculate the number of days in the year
			val daysInYear = if (isLeapYear) 366 else 365

			// Generate a random day of the year
			val randomDayOfYear = Random.nextInt(daysInYear)

			// Add the random number of days to the start of the year
			calendar.time = startOfYear
			calendar.add(Calendar.DAY_OF_YEAR, randomDayOfYear)

			return calendar.time
		}
	}
}