package com.meninocoiso.beatstarcommunity.presentation.ui.components.updates

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import com.meninocoiso.beatstarcommunity.R
import com.meninocoiso.beatstarcommunity.domain.model.Chart
import com.meninocoiso.beatstarcommunity.presentation.ui.components.layout.CoverArt

@Composable
internal fun UpdateListItem(chart: Chart) {
    ListItem(modifier = Modifier
        .clip(RoundedCornerShape(16.dp)),
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        leadingContent = {
            CoverArt(
                difficulty = null,
                borderRadius = 2.dp,
                url = chart.coverUrl,
                size = 40.dp
            )
        },
        headlineContent = {
            Text(
                text = chart.track,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                lineHeight = TextUnit(1f, TextUnitType.Em)
            )
        },
        supportingContent = {
            Text(
                text = "Update from v${chart.latestVersion.index + 1} → v${chart.availableVersion?.plus(1)}",
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = TextUnit(1f, TextUnitType.Em)
            )
        },
        trailingContent = {
            IconButton(
                onClick = { /*TODO*/ },
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.rounded_download_24),
                    contentDescription = "Update icon"
                )
            }
        })
}