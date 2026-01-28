package com.meninocoiso.bscm.presentation.ui.components.details

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.meninocoiso.bscm.R
import com.meninocoiso.bscm.presentation.ui.components.SwitchUI
import com.meninocoiso.bscm.presentation.ui.components.layout.CoverArt
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionBottomSheet(
    sheetState: SheetState,
    onDismissRequest: () -> Unit,
    onClose: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val horizontalPagerState = rememberPagerState { 2 }

    val currentTitle = when (horizontalPagerState.currentPage) {
        0 -> "Add collection"
        1 -> "Create collection"
        else -> ""
    }

    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = onDismissRequest,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (horizontalPagerState.currentPage > 0) {
                IconButton(onClick = {
                    coroutineScope.launch {
                        horizontalPagerState.scrollToPage(
                            horizontalPagerState.currentPage - 1
                        )
                    }
                }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = stringResource(R.string.back)
                    )
                }
            } else {
                Spacer(modifier = Modifier.size(48.dp))
            }
            Text(
                text = currentTitle,
                style = MaterialTheme.typography.titleMedium
            )
            if (horizontalPagerState.currentPage == 0) {
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.close_bottomsheet)
                    )
                }
            } else {
                Spacer(modifier = Modifier.size(48.dp))
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize()
        ) {
            HorizontalPager(
                state = horizontalPagerState,
                key = { it }, // Recompose the pager when the page changes
                beyondViewportPageCount = 0,
                userScrollEnabled = false,
                modifier = Modifier
                    .wrapContentHeight()
            ) { index ->
                when (index) {
                    0 -> CollectionsListSection(
                        onCreateNewCollectionClick = {
                            coroutineScope.launch {
                                horizontalPagerState.scrollToPage(
                                    1
                                )
                            }
                        }
                    )

                    1 -> CreateCollectionSection(
                        onBackClick = {
                            coroutineScope.launch {
                                horizontalPagerState.scrollToPage(0)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun CollectionsListSection(
    modifier: Modifier = Modifier,
    onCollectionClick: (String) -> Unit = { },
    onCreateNewCollectionClick: () -> Unit = { }
) {
    LazyColumn(modifier = modifier) {
        item {
            val interactionSource = remember { MutableInteractionSource() }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        onClick = onCreateNewCollectionClick,
                        interactionSource = interactionSource,
                        indication = ripple()
                    )
                    .padding(vertical = 12.dp, horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .size(56.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.rounded_add_2_24),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Text("Criar nova coleção", style = MaterialTheme.typography.titleMedium)
            }
        }
        items(12) {
            CollectionItem(
                name = "bonito.",
                coverUrl = "https://i.imgur.com/5Hsj4tJ.jpeg",
                isPublic = false,
                contentCounts = Triple(24, 16, 7),
                onClick = { onCollectionClick("bonito.") }
            )
        }
    }
}

@Composable
fun CreateCollectionSection(
    onBackClick: () -> Unit = { }
) {
    var isPublic by rememberSaveable { mutableStateOf(false) }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.Start,
        modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
    ) {
        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            state = rememberTextFieldState(initialText = ""),
            label = { Text("Name") },
            lineLimits = TextFieldLineLimits.SingleLine,
            supportingText = { Text("0/30") },
            placeholder = { Text("Enter collection name") }
        )
        ListItem(
            colors = ListItemDefaults.colors(
                containerColor = Color.Transparent
            ),
            headlineContent = { Text("Make public") },
            supportingContent = { Text("Public collections will be showed in your profile and can be shared with friends") },
            trailingContent = {
                SwitchUI(checked = isPublic, onCheckedChange = { isPublic = !isPublic })
            },
        )
        Button(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            onClick = { println("Teste") }) {
            Text("Save")
        }
    }
}

@Composable
fun CollectionItem(
    name: String,
    coverUrl: String,
    isPublic: Boolean = false,
    contentCounts: Triple<Int, Int, Int>,
    onClick: () -> Unit = { }
) {
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                onClick = onClick,
                interactionSource = interactionSource,
                indication = ripple()
            )
            .padding(vertical = 12.dp, horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CoverArt(url = coverUrl, size = 56.dp, borderRadius = 8.dp)
        Column(
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(name, style = MaterialTheme.typography.titleMedium)
                if (!isPublic) {
                    Icon(
                        modifier = Modifier.size(14.dp),
                        painter = painterResource(R.drawable.rounded_lock_24),
                        contentDescription = null
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "${contentCounts.first} charts",
                    style = MaterialTheme.typography.labelLarge
                )
                Text(
                    text = "${contentCounts.second} tour passes",
                    style = MaterialTheme.typography.labelLarge
                )
                Text(
                    text = "${contentCounts.third} themes",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}