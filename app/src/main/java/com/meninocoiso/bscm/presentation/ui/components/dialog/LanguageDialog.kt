package com.meninocoiso.bscm.presentation.ui.components.dialog

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.verticalScroll
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
)

@Composable
fun LanguageDialog() {
    val (isOpened, setIsOpened) = remember { mutableStateOf(false) }

    val systemDefault = SupportedLanguage(null, stringResource(R.string.system_default_language))
    val supportedLanguages = listOf(
        systemDefault,
        SupportedLanguage("en-US", "English"),
        SupportedLanguage("pt-BR", "Português (Brasil)"),
        SupportedLanguage("es-ES", "Español"),
        SupportedLanguage("ru-RU", "Русский"),
        SupportedLanguage("fr-FR", "Français"),
        SupportedLanguage("de-DE", "Deutsch"),
        SupportedLanguage("ro-RO", "Română" ),
        SupportedLanguage("id-ID", "Bahasa Indonesia"),
        SupportedLanguage("hu-HU", "Magyar"),
        SupportedLanguage("zh-CN", "中文 (中国)"),
        SupportedLanguage("el-GR", "Ελληνικά"),
        SupportedLanguage("vi-VN", "Tiếng Việt")
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
            modifier = Modifier.heightIn(max = 650.dp),
            onDismissRequest = { setIsOpened(false) },
            title = { Text(text = stringResource(R.string.app_language)) },
            icon = {
                Icon(
                    painter = painterResource(id = R.drawable.round_translate_24),
                    contentDescription = null
                )
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    RadioGroupUI(
                        initialSelected = selectedLanguage.value.displayName,
                        radioOptions = supportedLanguages.map { it.displayName },
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