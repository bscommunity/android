package com.meninocoiso.bscm.presentation.ui.components.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.meninocoiso.bscm.R
import com.meninocoiso.bscm.domain.model.Collection
import com.meninocoiso.bscm.presentation.ui.components.layout.CoverArt
import com.meninocoiso.bscm.presentation.ui.components.layout.LinearGradient
import com.meninocoiso.bscm.presentation.ui.modifiers.debouncedClickable

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CollectionPreview(
    collection: Collection,
    modifier: Modifier = Modifier,
    onPress: () -> Unit
) {
    Box(
        modifier = modifier
            .height(185.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .debouncedClickable(onClick = { onPress() }),
        contentAlignment = Alignment.BottomStart
    ) {
        if (collection.coverUrl != null) {
            CoverArt(
                modifier = Modifier.matchParentSize(),
                width = Dp.Unspecified,
                height = Dp.Unspecified,
                url = collection.coverUrl,
            )
            LinearGradient(
                modifier = Modifier
                    .matchParentSize(),
                size = null,
                colors = listOf(
                    Color.Black.copy(alpha = 0f),    // 0%
                    Color.Black.copy(alpha = 0.65f)  // 65%
                )
            )
        }
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = collection.name,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onBackground,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )
            if (!collection.isPublic) {
                Icon(
                    modifier = Modifier.padding(start = 16.dp).size(20.dp),
                    tint = MaterialTheme.colorScheme.onBackground,
                    painter = painterResource(R.drawable.rounded_lock_24),
                    contentDescription = null
                )
            }
        }
    }
}