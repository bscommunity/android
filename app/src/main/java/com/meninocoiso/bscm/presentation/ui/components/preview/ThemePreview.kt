package com.meninocoiso.bscm.presentation.ui.components.preview

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.meninocoiso.bscm.R
import com.meninocoiso.bscm.domain.model.Theme
import com.meninocoiso.bscm.presentation.ui.components.layout.CoverArt
import com.meninocoiso.bscm.presentation.ui.modifiers.debouncedClickable
import com.meninocoiso.bscm.util.PreviewUtils.titleContent

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ThemePreview(
    theme: Theme,
    modifier: Modifier = Modifier,
    isLocal: Boolean = false,
    isDisabled: Boolean = false,
    isSecondary: Boolean = false,
    onDisabled: () -> Unit = {},
    onPress: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = if ((theme.isInstalled == true || isDisabled) && !isLocal) 0.5f else 1f
            }
            .debouncedClickable(onClick = {
                if (isDisabled) {
                    onDisabled()
                    return@debouncedClickable
                }
                onPress()
            })
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .height(210.dp)
                    .fillMaxWidth()
            ) {
                CoverArt(
                    modifier = Modifier
                        .border(
                            width = 3.dp,
                            color = MaterialTheme.colorScheme.onSurface,
                            shape = RoundedCornerShape(4.dp)
                        )
                        .align(Alignment.TopStart),
                    url = theme.previewUrl,
                    borderRadius = 4.dp,
                    width = 110.dp,
                    height = 200.dp
                )

                CoverArt(
                    modifier = Modifier
                        .border(
                            width = 3.dp,
                            color = MaterialTheme.colorScheme.onSurface,
                            shape = RoundedCornerShape(4.dp)
                        )
                        .align(Alignment.BottomEnd),
                    url = theme.coverUrl,
                    borderRadius = 4.dp,
                    width = 75.dp,
                    height = 75.dp
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Column {
                    Row { titleContent(theme.name, false, false) }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.rounded_autorenew_24),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(style = MaterialTheme.typography.labelMedium, text = theme.replaces)
                    }
                }
                PreviewAuthors(
                    contentString = "Theme by ${theme.contributors[0].user.username}",
                    /*contentString = stringResource(
                        R.string.chart_by,
                        theme.contributors[0].user.username
                    ),*/
                    authors = theme.contributors
                )
                if (!isLocal && theme.isInstalled == true) PreviewInstalledTag(false)
            }
        }
    }
}