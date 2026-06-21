package com.mcis.memoir

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mcis.memoir.i18n.LocaleController
import com.mcis.memoir.ui.components.BottomNavigationBar
import com.mcis.memoir.ui.components.UntitledIcon
import com.mcis.memoir.ui.icons.*
import com.mcis.memoir.ui.memory.components.FilePhoto
import com.mcis.memoir.ui.memory.library.BookmarkSpotCard
import com.mcis.memoir.ui.memory.library.MemoriesEffect
import com.mcis.memoir.ui.memory.library.MemoriesIntent
import com.mcis.memoir.ui.memory.library.MemoriesState
import com.mcis.memoir.ui.memory.library.MemoriesTab
import com.mcis.memoir.ui.memory.library.MemoriesViewModel
import com.mcis.memoir.ui.memory.library.MemoryCard
import com.mcis.memoir.ui.memory.library.WizardEntry
import com.mcis.memoir.ui.theme.inter
import java.io.File

/**
 * Screen displaying the user's memory library (Route tab) and saved spots (Bookmark tab),
 * backed live by [MemoriesViewModel].
 */
@Composable
fun MemoriesScreen(
    viewModel: MemoriesViewModel,
    onNavigateToHome: () -> Unit = {},
    onNavigateToSaved: () -> Unit = {},
    onNavigateToMemories: () -> Unit = {},
    onCreateMemoryClick: () -> Unit = {},
    onNavigateToWizard: (String, WizardEntry) -> Unit = { _, _ -> },
    onNavigateToSpot: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is MemoriesEffect.NavigateToWizard -> onNavigateToWizard(effect.memoryId, effect.entry)
                MemoriesEffect.NavigateToCreate -> onCreateMemoryClick()
                is MemoriesEffect.NavigateToSpot -> onNavigateToSpot(effect.spotId)
                is MemoriesEffect.ShareMemory -> shareMemoryPhotos(context, effect.relativePaths, effect.title)
            }
        }
    }

    MemoriesContent(
        state = state,
        onIntent = viewModel::onIntent,
        onNavigateToHome = onNavigateToHome,
        onNavigateToSaved = onNavigateToSaved,
        onNavigateToMemories = onNavigateToMemories,
        modifier = modifier
    )
}

@Composable
private fun MemoriesContent(
    state: MemoriesState,
    onIntent: (MemoriesIntent) -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToSaved: () -> Unit,
    onNavigateToMemories: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isChinese = LocaleController.currentLocale().language == "zh"
    val context = LocalContext.current
    val filesDir = remember(context) { context.filesDir }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DesignTokens.colorLanguageSelectionBackground)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 54.dp, bottom = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.memories_headline),
                    style = TextStyle(
                        fontFamily = inter,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = DesignTokens.colorMaroon
                    )
                )
            }

            // Route / Bookmark selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.Bottom
            ) {
                TabLabel(
                    label = stringResource(R.string.memories_route_tab),
                    selected = state.selectedTab == MemoriesTab.ROUTE,
                    onClick = { onIntent(MemoriesIntent.TabSelected(MemoriesTab.ROUTE)) }
                )
                Spacer(modifier = Modifier.width(24.dp))
                TabLabel(
                    label = stringResource(R.string.memories_bookmark_tab),
                    selected = state.selectedTab == MemoriesTab.BOOKMARK,
                    onClick = { onIntent(MemoriesIntent.TabSelected(MemoriesTab.BOOKMARK)) }
                )
            }

            if (state.selectedTab == MemoriesTab.ROUTE) {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (state.inProgress.isNotEmpty()) {
                        item {
                            SectionHeader(stringResource(R.string.memories_in_progress))
                        }
                        items(state.inProgress, key = { it.id }) { card ->
                            InProgressMemoryCard(
                                card = card,
                                filesDir = filesDir,
                                onBodyClick = { onIntent(MemoriesIntent.ContinueEditingClicked(card.id)) },
                                onMoreClick = { onIntent(MemoriesIntent.MoreClicked(card.id)) }
                            )
                        }
                    }

                    if (state.completed.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            SectionHeader(stringResource(R.string.memories_completed))
                        }
                        items(state.completed, key = { it.id }) { card ->
                            CompletedMemoryCard(
                                card = card,
                                filesDir = filesDir,
                                onMoreClick = { onIntent(MemoriesIntent.MoreClicked(card.id)) }
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }

                // Fixed Bottom Action Area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val buttonShape = RoundedCornerShape(50.dp)
                    Box(
                        modifier = Modifier
                            .width(335.dp)
                            .height(76.dp)
                            .background(DesignTokens.colorMaroon, buttonShape)
                            .clip(buttonShape)
                            .clickable { onIntent(MemoriesIntent.CreateMemoryClicked) },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            UntitledIcon(
                                imageVector = UntitledIcons.AddIcon,
                                contentDescription = null,
                                tint = Color.White,
                                size = 40.dp
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = stringResource(R.string.memories_create_button),
                                style = TextStyle(
                                    fontFamily = inter,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = Color.White
                                )
                            )
                        }
                    }
                }
            } else {
                // Bookmark Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                ) {
                    // Search Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .background(Color(0xFFE2E0D8), RoundedCornerShape(24.dp))
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = Color.Gray
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        BasicTextField(
                            value = state.bookmarkSearchQuery,
                            onValueChange = { onIntent(MemoriesIntent.BookmarkSearchChanged(it)) },
                            textStyle = TextStyle(
                                fontFamily = inter,
                                fontSize = 16.sp,
                                color = Color.Black
                            ),
                            decorationBox = { innerTextField ->
                                if (state.bookmarkSearchQuery.isEmpty()) {
                                    Text(
                                        text = stringResource(R.string.memories_search_bookmarks),
                                        style = TextStyle(
                                            fontFamily = inter,
                                            fontSize = 16.sp,
                                            color = Color.Gray
                                        )
                                    )
                                }
                                innerTextField()
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Text(
                        text = stringResource(R.string.memories_choose_bookmarks),
                        style = TextStyle(
                            fontFamily = inter,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.Black
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (state.bookmarkedSpots.isEmpty()) {
                        Text(
                            text = stringResource(R.string.memories_empty_bookmarks),
                            style = TextStyle(
                                fontFamily = inter,
                                fontSize = 14.sp,
                                color = Color.Gray
                            )
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(bottom = 24.dp)
                        ) {
                            items(state.bookmarkedSpots, key = { it.id }) { spot ->
                                BookmarkSpotCardItem(
                                    card = spot,
                                    onClick = { onIntent(MemoriesIntent.BookmarkSpotClicked(spot.id)) }
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Navigation Bar
            BottomNavigationBar(
                isChinese = isChinese,
                onHomeClick = onNavigateToHome,
                onSavedClick = onNavigateToSaved,
                onMemoriesClick = onNavigateToMemories,
                currentDestination = "memories"
            )
        }

        // Action Menu Dropdown (Overlay)
        val activeId = state.activeMenuMemoryId
        if (activeId != null && !state.showDeleteDialog) {
            val activeCard = (state.inProgress + state.completed).firstOrNull { it.id == activeId }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { onIntent(MemoriesIntent.MenuDismissed) }
            ) {
                Box(
                    modifier = Modifier
                        .padding(end = 20.dp, top = 200.dp)
                        .align(Alignment.TopEnd)
                ) {
                    MemoryActionMenu(
                        shareEnabled = activeCard?.coverRelativePath != null,
                        onEditClick = { onIntent(MemoriesIntent.EditClicked(activeId)) },
                        onDeleteClick = { onIntent(MemoriesIntent.DeleteClicked(activeId)) },
                        onDuplicateClick = { onIntent(MemoriesIntent.DuplicateClicked(activeId)) },
                        onShareClick = { onIntent(MemoriesIntent.ShareClicked(activeId)) }
                    )
                }
            }
        }

        // Delete Confirmation Dialog
        if (state.showDeleteDialog) {
            DeleteConfirmationDialog(
                onCancel = { onIntent(MemoriesIntent.DeleteCancelled) },
                onDelete = { onIntent(MemoriesIntent.DeleteConfirmed) }
            )
        }
    }
}

private fun shareMemoryPhotos(context: Context, relativePaths: List<String>, title: String) {
    if (relativePaths.isEmpty()) return
    val authority = "${context.packageName}.fileprovider"
    val uris = relativePaths.map { path ->
        FileProvider.getUriForFile(context, authority, File(context.filesDir, path))
    }
    val intent = if (uris.size == 1) {
        Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, uris.first())
            putExtra(Intent.EXTRA_TEXT, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    } else {
        Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "image/jpeg"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
            putExtra(Intent.EXTRA_TEXT, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
    context.startActivity(Intent.createChooser(intent, title).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

@Composable
private fun TabLabel(label: String, selected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(IntrinsicSize.Min)
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = TextStyle(
                fontFamily = inter,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .height(2.dp)
                .fillMaxWidth()
                .background(if (selected) DesignTokens.colorMaroon else Color.Transparent)
        )
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = TextStyle(
            fontFamily = inter,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        ),
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
fun BookmarkSpotCardItem(
    card: BookmarkSpotCard,
    onClick: () -> Unit
) {
    val cardShape = RoundedCornerShape(12.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .shadow(elevation = 4.dp, shape = cardShape, spotColor = Color(0x20000000))
            .background(Color(0xFFFBFBF6), cardShape)
            .clip(cardShape)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (card.heroDrawableRes != 0) {
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(card.heroDrawableRes),
                    contentDescription = card.title,
                    modifier = Modifier
                        .width(100.dp)
                        .fillMaxHeight(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .width(100.dp)
                        .fillMaxHeight()
                        .background(Color.LightGray)
                )
            }

            Spacer(modifier = Modifier.width(24.dp))

            Text(
                text = card.title,
                style = TextStyle(
                    fontFamily = inter,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                ),
                modifier = Modifier.weight(1f)
            )

            UntitledIcon(
                imageVector = UntitledIcons.SavedFilled,
                contentDescription = "Bookmark",
                tint = DesignTokens.colorMaroon,
                size = 28.dp,
                modifier = Modifier.padding(end = 16.dp)
            )
        }
    }
}

@Composable
fun MemoryActionMenu(
    shareEnabled: Boolean,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onDuplicateClick: () -> Unit,
    onShareClick: () -> Unit
) {
    val menuShape = RoundedCornerShape(15.dp)
    Column(
        modifier = Modifier
            .width(197.dp)
            .shadow(12.dp, menuShape)
            .background(DesignTokens.colorLanguageSelectionBackground, menuShape)
            .padding(vertical = 8.dp)
    ) {
        MenuItem(UntitledIcons.EditIcon, stringResource(R.string.memories_menu_edit), onClick = onEditClick)
        DividerLine()
        MenuItem(UntitledIcons.DeleteIcon, stringResource(R.string.memories_menu_delete), onClick = onDeleteClick)
        DividerLine()
        MenuItem(UntitledIcons.DuplicateIcon, stringResource(R.string.memories_menu_duplicate), onClick = onDuplicateClick)
        DividerLine()
        MenuItem(
            UntitledIcons.ShareIcon,
            stringResource(R.string.memories_menu_share),
            enabled = shareEnabled,
            onClick = onShareClick
        )
    }
}

@Composable
fun MenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    enabled: Boolean = true,
    onClick: () -> Unit = {}
) {
    val tint = if (enabled) Color.Black else Color.Gray
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(38.dp)
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        UntitledIcon(imageVector = icon, contentDescription = null, size = 20.dp, tint = tint)
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = label, style = TextStyle(fontFamily = inter, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = tint))
    }
}

@Composable
fun DividerLine() {
    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFE0E0E0)))
}

@Composable
fun DeleteConfirmationDialog(onCancel: () -> Unit, onDelete: () -> Unit) {
    Dialog(onDismissRequest = onCancel) {
        val shape = RoundedCornerShape(15.dp)
        Column(
            modifier = Modifier
                .width(243.dp)
                .background(DesignTokens.colorLanguageSelectionBackground, shape)
                .padding(top = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            UntitledIcon(imageVector = UntitledIcons.DeleteIcon, contentDescription = null, size = 30.dp, tint = DesignTokens.colorMaroon)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.memories_delete_confirm),
                style = TextStyle(fontFamily = inter, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Black),
                modifier = Modifier.padding(horizontal = 24.dp),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(18.dp))
            Row(modifier = Modifier.fillMaxWidth().height(44.dp)) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxHeight().clickable { onCancel() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = stringResource(R.string.cancel_button), style = TextStyle(fontFamily = inter, fontSize = 14.sp, color = Color.Black))
                }
                Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(Color(0xFFE0E0E0)))
                Box(
                    modifier = Modifier.weight(1f).fillMaxHeight().clickable { onDelete() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = stringResource(R.string.delete_button), style = TextStyle(fontFamily = inter, fontSize = 14.sp, color = DesignTokens.colorMaroon))
                }
            }
        }
    }
}

@Composable
fun InProgressMemoryCard(
    card: MemoryCard,
    filesDir: File,
    onBodyClick: () -> Unit,
    onMoreClick: () -> Unit
) {
    val cardShape = RoundedCornerShape(15.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(127.dp)
            .shadow(elevation = 15.dp, shape = cardShape, spotColor = Color(0x40000000))
            .background(DesignTokens.colorLanguageSelectionBackground, cardShape)
            .clip(cardShape)
            .clickable { onBodyClick() }
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            MemoryCover(card = card, filesDir = filesDir)
            Column(
                modifier = Modifier
                    .padding(start = 16.dp, top = 12.dp, end = 12.dp)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.Top
            ) {
                Text(
                    text = card.title,
                    style = TextStyle(
                        fontFamily = inter,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.memories_continue_editing),
                    style = TextStyle(
                        fontFamily = inter,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .width(160.dp)
                            .height(11.dp)
                            .background(Color(0xFFD9D9D9), CircleShape)
                    ) {
                        val fraction = card.draftProgress.current.toFloat() / card.draftProgress.total
                        Box(
                            modifier = Modifier
                                .width((160 * fraction).dp)
                                .fillMaxHeight()
                                .background(Color(0xFFA32B2B), CircleShape)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${card.draftProgress.current}/${card.draftProgress.total}",
                        style = TextStyle(
                            fontFamily = inter,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF979797)
                        )
                    )
                }
            }
        }

        MoreButton(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp),
            onClick = onMoreClick
        )
    }
}

@Composable
fun CompletedMemoryCard(
    card: MemoryCard,
    filesDir: File,
    onMoreClick: () -> Unit
) {
    val cardShape = RoundedCornerShape(15.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(127.dp)
            .shadow(elevation = 15.dp, shape = cardShape, spotColor = Color(0x40000000))
            .background(DesignTokens.colorLanguageSelectionBackground, cardShape)
            .clip(cardShape)
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            MemoryCover(card = card, filesDir = filesDir)
            Column(
                modifier = Modifier
                    .padding(start = 16.dp, top = 12.dp, end = 12.dp)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.Top
            ) {
                Text(
                    text = card.title,
                    style = TextStyle(
                        fontFamily = inter,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.memories_updated_on, card.dateLabel),
                    style = TextStyle(
                        fontFamily = inter,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                )
            }
        }

        MoreButton(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 12.dp, end = 12.dp),
            onClick = onMoreClick
        )
    }
}

@Composable
private fun MemoryCover(card: MemoryCard, filesDir: File) {
    val cover = card.coverRelativePath
    if (cover != null) {
        FilePhoto(
            relativePath = cover,
            filesDir = filesDir,
            modifier = Modifier
                .width(132.dp)
                .fillMaxHeight()
        )
    } else {
        Box(
            modifier = Modifier
                .width(132.dp)
                .fillMaxHeight()
                .background(Color.LightGray)
        )
    }
}

@Composable
private fun MoreButton(modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .size(24.dp)
            .clip(CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        UntitledIcon(imageVector = UntitledIcons.MoreIcon, contentDescription = "More", size = 18.dp, tint = Color.Black)
    }
}
