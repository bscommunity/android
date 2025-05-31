package com.meninocoiso.bscm.presentation.ui.components.workshop

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.meninocoiso.bscm.domain.enums.SortOption

data class WorkshopChip(
    val id: SortOption,
    val title: String,
    val enabled: Boolean = true,
)

val chipItems = listOf(
    WorkshopChip(
        id = SortOption.LAST_UPDATED,
        title = "Last updated",
    ),
    WorkshopChip(
        id = SortOption.MOST_DOWNLOADED,
        title = "Most downloaded",
    ),
    WorkshopChip(
        id = SortOption.WEEKLY_RANK,
        title = "Weekly Rank",
        enabled = false
    ),
    WorkshopChip(
        id = SortOption.MOST_LIKED,
        title = "Most liked",
        enabled = false
    ),
)

@Composable
fun WorkshopChips(
    currentSortOption: SortOption,
    onSortOptionChange: (SortOption) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Spacer(modifier = Modifier.width(8.dp)) // Leading padding

        chipItems.forEach { chip ->
            FilterChip(
                onClick = { onSortOptionChange(chip.id) },
                label = {
                    Text(chip.title)
                },
                enabled = chip.enabled,
                selected = currentSortOption == chip.id,
                leadingIcon = if (currentSortOption == chip.id) {
                    {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = "Check icon",
                            modifier = Modifier.size(FilterChipDefaults.IconSize)
                        )
                    }
                } else {
                    null
                },
            )
        }

        Spacer(modifier = Modifier.width(8.dp)) // Leading padding
    }
}