package com.meninocoiso.bscm.presentation.ui.components.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.meninocoiso.bscm.R

@Composable
fun BoxScope.ProfileIndicator(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(36.dp)
            .align(Alignment.BottomCenter)
            .zIndex(2f)
            .offset(y = 8.dp)
            .border(
                width = 2.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(100)
            )
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(100))
            .background(MaterialTheme.colorScheme.surfaceContainerLowest),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            modifier = Modifier.size(22.dp),
            painter = painterResource(R.drawable.rounded_person_24px),
            contentDescription = null
        )
    }
}