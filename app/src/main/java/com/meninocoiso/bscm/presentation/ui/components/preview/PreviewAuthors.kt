package com.meninocoiso.bscm.presentation.ui.components.preview

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.meninocoiso.bscm.R
import com.meninocoiso.bscm.data.remote.dto.ContributorUserDto
import com.meninocoiso.bscm.domain.model.Contributor
import com.meninocoiso.bscm.presentation.ui.components.layout.Avatar
import java.time.LocalDateTime

@Composable
fun PreviewAuthors(
    contentString: String,
    authors: List<Contributor>,
    avatarSize: Dp = 18.dp,
) {
    if (authors.isEmpty()) return

    BoxWithConstraints {
        val maxWidthFraction = 0.7f // 70% of the parent's width
        val maxWidthDp = this.maxWidth * maxWidthFraction

        Box(
            modifier = Modifier.border(
                BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                RoundedCornerShape(150.dp)
            )
        ) {
            Row(
                modifier = Modifier
                    .padding(start = 6.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy((-4).dp)) {
                    for (author in authors) {
                        Avatar(
                            url = author.user.imageUrl,
                            alt = author.user.username.first().toString(),
                            size = avatarSize
                        )
                    }
                }
                Text(
                    style = MaterialTheme.typography.bodySmall,
                    text = "$contentString ${if (authors.size > 1) stringResource(
                        R.string.and_others
                    ) else ""}",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.sizeIn(maxWidth = maxWidthDp)
                )
            }
        }
    }
}

@Preview
@Composable
fun ChartAuthorsPreview() {
    PreviewAuthors(
        contentString = stringResource(
            R.string.chart_by,
            "user1"
        ),
        authors = listOf(
            Contributor(
                user = ContributorUserDto(
                    id = "1",
                    username = "user1",
                    imageUrl = "https://example.com/image1.jpg",
                    createdAt = LocalDateTime.now(),
                ),
                chartId = "1",
                roles = emptyList(),
                joinedAt = LocalDateTime.now()
            ),
        )
    )
}