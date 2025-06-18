package com.meninocoiso.bscm.presentation.ui.components.dialog

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.core.os.LocaleListCompat
import com.meninocoiso.bscm.R
import com.meninocoiso.bscm.presentation.ui.components.RadioGroupUI

data class SupportedLanguage(val tag: String?, val displayName: String)

@Composable
fun LanguageDialog() {
    val (isOpened, setIsOpened) = remember { mutableStateOf(false) }

    val systemDefault = SupportedLanguage(null, stringResource(R.string.system_default_language))
    val supportedLanguages = listOf(
        systemDefault,
        SupportedLanguage("en-US", "English"),
        SupportedLanguage("pt-BR", "Português (Brasil)"),
        SupportedLanguage("es-ES", "Español"),
        SupportedLanguage("ru-RU", "Русский")
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
                    painter = painterResource(id = R.drawable.baseline_palette_24),
                    contentDescription = null
                )
            },
            text = {
                RadioGroupUI(
                    initialSelected = selectedLanguage.value.displayName,
                    radioOptions = supportedLanguages.map { it.displayName },
                    onOptionSelected = { index, _ ->
                        val lang = supportedLanguages[index]
                        if (lang.tag == null) {
                            AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
                        } else {
                            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(lang.tag))
                        }
                        selectedLanguage.value = lang
                    }
                )
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