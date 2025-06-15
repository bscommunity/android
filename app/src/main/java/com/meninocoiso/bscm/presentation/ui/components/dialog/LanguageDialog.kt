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
import java.util.Locale

@Composable
fun LanguageDialog() {
    val (isOpened, setIsOpened) = remember { mutableStateOf(false) }

    val localeList = LocaleListCompat.getAdjustedDefault()
    
    println("Current Locale: ${localeList[0]}")
    println("All Locales: ${localeList.toLanguageTags()}")
    println("Available Locales: ${Locale.getAvailableLocales().joinToString { it.toLanguageTag() }}")
    println("Default Locale: ${Locale.getDefault().toLanguageTag()}")
    
    val supportedLocales = listOf(Locale.getDefault(), Locale("en", "US"), Locale("ru", "RU"), Locale("pt", "BR"))
        .distinctBy { it.toLanguageTag() }
    val languageTags = supportedLocales.map { it.toLanguageTag() }

    val currentLocaleTag =
        AppCompatDelegate.getApplicationLocales()[0]?.toLanguageTag() ?: Locale.getDefault()
            .toLanguageTag()
    val selectedLocaleTag = remember { mutableStateOf(currentLocaleTag) }

    val languageStrings = mapOf(
        Locale.getDefault().toLanguageTag() to stringResource(R.string.system_default_language),
        "en-US" to "English",
        "pt-BR" to "Português (Brasil)",
        "ru-RU" to "Русский",
    )

    Button(onClick = { setIsOpened(true) }) {
        Text(text = languageStrings[selectedLocaleTag.value] ?: selectedLocaleTag.value)
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
                    initialSelected = languageStrings[selectedLocaleTag.value]
                        ?: selectedLocaleTag.value,
                    radioOptions = languageTags.map { languageStrings[it] ?: it },
                    onOptionSelected = { index, _ ->
                        val tag = languageTags[index]
                        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
                        selectedLocaleTag.value = tag
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