package com.meninocoiso.bscm.util

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meninocoiso.bscm.R

object PreviewUtils {
    @Composable
    fun Modifier.localContainer(isEnabled: Boolean): Modifier {
        return if (isEnabled) {
            this
                .padding(start = 16.dp, end = 16.dp, top = 8.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
        } else {
            this
        }
    }

    val titleContent: @Composable RowScope.(title: String, isExplicit: Boolean, isDeluxe: Boolean) -> Unit =
        { title, isExplicit, isDeluxe ->
            Text(
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                lineHeight = 20.sp,
                text = getTitle(
                    title = title,
                    isExplicit = isExplicit,
                    isDeluxe = isDeluxe
                ),
                inlineContent = getTitleInlineContent(
                    isExplicit = isExplicit,
                    isDeluxe = isDeluxe
                ),
            )
        }

    private fun getTitle(
        title: String,
        isExplicit: Boolean,
        isDeluxe: Boolean
    ): AnnotatedString {
        return buildAnnotatedString {
            append(title)

            if (isExplicit) {
                // Add spacing before the icon
                append("  ") // Two spaces for approximate spacing
                appendInlineContent("explicit", "[E]")
            }
            if (isDeluxe) {
                // Add spacing before the icon
                append("  ") // Two spaces for approximate spacing
                appendInlineContent("deluxe", "[D]")
            }
        }
    }

    private fun getTitleInlineContent(
        isExplicit: Boolean,
        isDeluxe: Boolean
    ): Map<String, InlineTextContent> {
        val inlineContent = mutableMapOf<String, InlineTextContent>()
        if (isExplicit) {
            inlineContent["explicit"] = InlineTextContent(
                Placeholder(
                    width = 14.sp,
                    height = 14.sp,
                    placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter
                )
            ) {
                Icon(
                    modifier = Modifier.size(14.dp),
                    painter = painterResource(R.drawable.explicit),
                    contentDescription = stringResource(R.string.explicit)
                )
            }
        }
        if (isDeluxe) {
            inlineContent["deluxe"] = InlineTextContent(
                Placeholder(
                    width = 14.sp,
                    height = 14.sp,
                    placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter
                )
            ) {
                Icon(
                    modifier = Modifier.size(14.dp),
                    painter = painterResource(R.drawable.deluxe),
                    contentDescription = stringResource(R.string.deluxe)
                )
            }
        }
        return inlineContent
    }
}