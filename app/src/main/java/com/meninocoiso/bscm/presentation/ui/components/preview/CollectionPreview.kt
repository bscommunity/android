package com.meninocoiso.bscm.presentation.ui.components.preview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
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
    Box(modifier = modifier.debouncedClickable(onClick = { onPress() })) {
        if (collection.coverUrl != null) {
            CoverArt(
                modifier = Modifier.fillMaxSize(),
                url = collection.coverUrl,
            )
        } else {
            LinearGradient(
                modifier = Modifier.fillMaxSize(),
                colors = listOf(
                    Color.Black.copy(alpha = 0f),    // 0%
                    Color.Black.copy(alpha = 0.65f)  // 65%
                )
            )
        }
        Column(
            modifier = modifier
                .debouncedClickable(onClick = { onPress() })
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.Bottom,
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = collection.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                if (!collection.isPublic) {
                    Icon(painter = painterResource(R.drawable.rounded_lock_24), contentDescription = null)
                }
            }
        }
    }
}