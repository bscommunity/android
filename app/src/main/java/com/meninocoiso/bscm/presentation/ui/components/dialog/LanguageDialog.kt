package com.meninocoiso.bscm.presentation.ui.components.dialog

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.LocaleListCompat
import com.meninocoiso.bscm.R
import com.meninocoiso.bscm.presentation.ui.components.RadioGroupUI

data class SupportedLanguage(
    val tag: String?,
    val displayName: String,
    val contributor: String? = null
)

private const val CONTRIBUTORS_LINK =
    "https://docs.google.com/spreadsheets/d/1pPn-XXC_2ivgPXWlPcgxU2jun8Kq9u3nKlpBFSoljmM/edit?usp=sharing"

@Composable
fun LanguageDialog() {
    val context = LocalContext.current
    val (isOpened, setIsOpened) = remember { mutableStateOf(false) }

    val systemDefault = SupportedLanguage(null, stringResource(R.string.system_default_language))
    val supportedLanguages = listOf(
        systemDefault,
        SupportedLanguage("en-US", "English"),
        SupportedLanguage("pt-BR", "Português (Brasil)"),
        SupportedLanguage("es-ES", "Español", contributor = "Farfu"),
        SupportedLanguage("ru-RU", "Русский", contributor = "MusicCat")
    )

    val currentLocaleTag = AppCompatDelegate.getApplicationLocales()[0]?.toLanguageTag()
    val selectedLanguage = remember {
        mutableStateOf(
            supportedLanguages.find { it.tag == currentLocaleTag } ?: systemDefault
        )
    }

    Button(onClick = { setIsOpened(true) }) {
        Text(text = selectedLanguage.value.displayName)
    }
    if (isOpened) {
        AlertDialog(
            onDismissRequest = { setIsOpened(false) },
            title = { Text(text = stringResource(R.string.app_language)) },
            icon = {
                Icon(
                    painter = painterResource(id = R.drawable.round_translate_24),
                    contentDescription = null
                )
            },
            text = {
                Column {
                    RadioGroupUI(
                        initialSelected = selectedLanguage.value.displayName,
                        radioOptions = supportedLanguages.map { it.displayName },
                        trailingElements = supportedLanguages.map { lang ->
                            lang.contributor?.let { contributor ->
                                @Composable {
                                    Text(
                                        text = "by $contributor",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            } ?: @Composable {}
                        },
                        onOptionSelected = { index, _ ->
                            val lang = supportedLanguages[index]
                            if (lang.tag == null) {
                                AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
                            } else {
                                AppCompatDelegate.setApplicationLocales(
                                    LocaleListCompat.forLanguageTags(
                                        lang.tag
                                    )
                                )
                            }
                            selectedLanguage.value = lang
                        },
                    )
                    /*Text(
                        text = buildAnnotatedString {
                            append(stringResource(R.string.language_dialog_info))
                            appendInlineContent(inlineContentId, "[icon]")
                        },
                        modifier = Modifier
                            .padding(top = 16.dp)
                            .clickable(onClick = {
                                LinkingUtils.openLink(context, CONTRIBUTORS_LINK)
                            }),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        lineHeight = 20.sp,
                        inlineContent = inlineContent
                    )*/
                }
            },
            dismissButton = {
                TextButton(onClick = { setIsOpened(false) }) {
                    Text(text = stringResource(R.string.cancel))
                }
            },
            confirmButton = {
                Button(onClick = { setIsOpened(false) }) {
                    Text(text = stringResource(R.string.confirm))
                }
            }
        )
    }
}

val inlineContentId = "open_in_new_icon"

val inlineContent = mapOf(
    inlineContentId to InlineTextContent(
        Placeholder(
            width = 18.sp,
            height = 12.sp,
            placeholderVerticalAlign = PlaceholderVerticalAlign.Center
        )
    ) {
        Icon(
            painter = painterResource(R.drawable.open_in_new_24px),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 6.dp)
        )
    }
)