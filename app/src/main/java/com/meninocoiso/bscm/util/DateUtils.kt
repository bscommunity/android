package com.meninocoiso.bscm.util

import android.content.Context
import com.meninocoiso.bscm.R
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Locale

object DateUtils {
    enum class DateFormat {
        DAY, WEEK, LAST_MONTH, YEAR
    }

    /**
     * Groups items by date using one or more specified format strategies.
     * Each item belongs to exactly one group based on the chosen format.
     * If multiple formats are provided, they are applied in a hierarchical order:
     * DAY > WEEK > LAST_MONTH > YEAR
     *
     * @param context Android context for accessing string resources
     * @param items List of items to group
     * @param getDate Lambda to extract date string from item (format: yyyy-MM-dd)
     * @param formats The grouping strategies (can be multiple: DAY, WEEK, LAST_MONTH, YEAR)
     * @return Map with localized group labels as keys and lists of items as values
     */
    fun <T> groupItemsByDate(
        context: Context,
        items: List<T>,
        getDate: (item: T) -> String,
        vararg formats: DateFormat = arrayOf(DateFormat.DAY)
    ): Map<String, List<T>> {
        val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val today = LocalDate.now()
        val formatList = if (formats.isEmpty()) listOf(DateFormat.DAY) else formats.toList()

        return items.groupBy { item ->
            try {
                val itemDate = LocalDate.parse(getDate(item), dateFormatter)
                getGroupLabel(context, itemDate, today, formatList)
            } catch (e: Exception) {
                context.getString(R.string.date_unknown)
            }
        }
    }

    /**
     * Determines the group label for a given date based on the format strategies.
     * Applies formats in hierarchical order to ensure an item belongs to exactly one group.
     */
    private fun getGroupLabel(
        context: Context,
        itemDate: LocalDate,
        today: LocalDate,
        formats: List<DateFormat>
    ): String {
        // Apply formats in hierarchical order (primary to secondary)
        for (format in formats) {
            val label = when (format) {
                DateFormat.DAY -> getDayLabel(context, itemDate, today)
                DateFormat.WEEK -> getWeekLabel(context, itemDate, today)
                DateFormat.LAST_MONTH -> getMonthLabel(context, itemDate, today)
                DateFormat.YEAR -> getYearLabel(itemDate)
            }
            // If we get a non-default label, use it
            if (label != itemDate.toString() && label != itemDate.year.toString()) {
                return label
            }
        }
        // Fallback to first format if all return defaults
        return when (formats.firstOrNull()) {
            DateFormat.DAY -> getDayLabel(context, itemDate, today)
            DateFormat.WEEK -> getWeekLabel(context, itemDate, today)
            DateFormat.LAST_MONTH -> getMonthLabel(context, itemDate, today)
            DateFormat.YEAR -> getYearLabel(itemDate)
            else -> getDayLabel(context, itemDate, today)
        }
    }

    /**
     * Gets a day label (e.g., "today", "yesterday", or "2026-02-09")
     */
    private fun getDayLabel(context: Context, itemDate: LocalDate, today: LocalDate): String {
        return when {
            itemDate == today -> context.getString(R.string.today)
            itemDate == today.minusDays(1) -> context.getString(R.string.yesterday)
            else -> itemDate.toString()
        }
    }

    /**
     * Gets a week label (e.g., "Week 1", "Week 1, 2026", "This week", or "Last week")
     */
    private fun getWeekLabel(context: Context, itemDate: LocalDate, today: LocalDate): String {
        val weekFields = WeekFields.of(Locale.getDefault())
        val itemWeek = itemDate.get(weekFields.weekOfYear())
        val itemYear = itemDate.year

        return when {
            itemDate.year != today.year -> {
                context.getString(R.string.week_year_format, itemWeek, itemYear)
            }
            else -> {
                val todayWeek = today.get(weekFields.weekOfYear())
                when (itemWeek) {
                    todayWeek -> context.getString(R.string.this_week)
                    todayWeek - 1 -> context.getString(R.string.last_week)
                    else -> context.getString(R.string.week_format, itemWeek)
                }
            }
        }
    }

    /**
     * Gets a month label (e.g., "This month", "Last month", or "February 2026")
     */
    private fun getMonthLabel(context: Context, itemDate: LocalDate, today: LocalDate): String {
        val monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())

        return when {
            itemDate.year != today.year -> itemDate.format(monthFormatter)
            itemDate.monthValue == today.monthValue -> context.getString(R.string.this_month)
            itemDate.monthValue == today.monthValue - 1 -> context.getString(R.string.last_month)
            else -> itemDate.format(monthFormatter)
        }
    }

    /**
     * Gets a year label (e.g., "2026")
     */
    private fun getYearLabel(itemDate: LocalDate): String {
        return itemDate.year.toString()
    }
}