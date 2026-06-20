package com.mcis.memoir

import androidx.compose.foundation.Image
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.mcis.memoir.data.MemoryData
import com.mcis.memoir.data.MemoryStatus
import com.mcis.memoir.data.MockData
import com.mcis.memoir.ui.components.BottomNavigationBar
import com.mcis.memoir.ui.components.UntitledIcon
import com.mcis.memoir.ui.icons.*
import com.mcis.memoir.ui.theme.AppTheme
import com.mcis.memoir.ui.theme.inter

/**
 * Screen displaying the user's past journey memories with "In Progress" and "Completed" sections.
 * Optimized with a fixed action button that doesn't block content.
 */
@Composable
fun MemoriesScreen(
    selectedLanguage: String = "en",
    onNavigateToHome: () -> Unit = {},
    onNavigateToSaved: () -> Unit = {},
    onNavigateToMemories: () -> Unit = {},
    onCreateMemoryClick: () -> Unit = {},
    onRouteClick: (String) -> Unit = {},
    onSpotClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isChinese = selectedLanguage == "zh"
    val allMemories = remember { MockData.memories }
    val inProgressMemories = allMemories.filter { it.status == MemoryStatus.IN_PROGRESS }
    val completedMemories = allMemories.filter { it.status == MemoryStatus.COMPLETED }

    var activeMenuMemoryId by remember { mutableStateOf<String?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf("route") }

    // 儲存的書籤 ID (Mock)
    val savedSpotIds = remember { mutableStateListOf("grand_mazu_temple_datianhougong", "grand_wumiao_temple_sidian_wumiao") }

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
                    text = if (isChinese) stringResource(R.string.memories_headline_zh) else stringResource(R.string.memories_headline),
                    style = TextStyle(
                        fontFamily = inter,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = DesignTokens.colorMaroon
                    )
                )
            }

            // Route / Bookmark selector (更新為新版底線設計)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.Bottom
            ) {
                // Route Tab
                Column(
                    modifier = Modifier
                        .width(IntrinsicSize.Min)
                        .clickable { selectedTab = "route" },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isChinese) "行程" else "Route",
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
                            .background(if (selectedTab == "route") DesignTokens.colorMaroon else Color.Transparent)
                    )
                }

                Spacer(modifier = Modifier.width(24.dp))

                // Bookmark Tab
                Column(
                    modifier = Modifier
                        .width(IntrinsicSize.Min)
                        .clickable { selectedTab = "bookmark" },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isChinese) "書籤" else "Bookmark",
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
                            .background(if (selectedTab == "bookmark") DesignTokens.colorMaroon else Color.Transparent)
                    )
                }
            }

            if (selectedTab == "route") {
                // Scrollable Content for Route
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // In Progress Section
                    if (inProgressMemories.isNotEmpty()) {
                        item {
                            Text(
                                text = if (isChinese) stringResource(R.string.memories_in_progress_zh) else stringResource(R.string.memories_in_progress),
                                style = TextStyle(
                                    fontFamily = inter,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                ),
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                        items(inProgressMemories) { memory ->
                            InProgressMemoryCard(
                                memory = memory,
                                isChinese = isChinese,
                                onMoreClick = { activeMenuMemoryId = it.id }
                            )
                        }
                    }

                    // Completed Section
                    if (completedMemories.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = if (isChinese) stringResource(R.string.memories_completed_zh) else stringResource(R.string.memories_completed),
                                style = TextStyle(
                                    fontFamily = inter,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                ),
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                        items(completedMemories) { memory ->
                            CompletedMemoryCard(
                                memory = memory,
                                isChinese = isChinese,
                                onMoreClick = { activeMenuMemoryId = it.id }
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
                            .clickable { onCreateMemoryClick() },
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
                                text = if (isChinese) stringResource(R.string.memories_create_button_zh) else stringResource(R.string.memories_create_button),
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
                // Bookmark Content (全新設計)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                ) {
                    // Search Bar
                    var searchText by remember { mutableStateOf("") }
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
                            value = searchText,
                            onValueChange = { searchText = it },
                            textStyle = TextStyle(
                                fontFamily = inter,
                                fontSize = 16.sp,
                                color = Color.Black
                            ),
                            decorationBox = { innerTextField ->
                                if (searchText.isEmpty()) {
                                    Text(
                                        text = if (isChinese) "搜尋書籤" else "Search Bookmarks",
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
                        text = if (isChinese) "選擇書籤" else "Choose Bookmarks",
                        style = TextStyle(
                            fontFamily = inter,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.Black
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Bookmark List
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        val spotsToRender = MockData.spots.filter { it.id in savedSpotIds }
                        // 搜尋過濾邏輯
                        val filteredSpots = if (searchText.isEmpty()) {
                            spotsToRender
                        } else {
                            spotsToRender.filter {
                                it.titleEn.contains(searchText, ignoreCase = true) ||
                                        it.titleZh.contains(searchText, ignoreCase = true)
                            }
                        }

                        items(filteredSpots, key = { it.id }) { spot ->
                            SimpleBookmarkCard(
                                spot = spot,
                                isChinese = isChinese,
                                onClick = { onSpotClick(spot.id) }
                            )
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
        if (activeMenuMemoryId != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { activeMenuMemoryId = null }
            ) {
                Box(
                    modifier = Modifier
                        .padding(end = 20.dp, top = 200.dp)
                        .align(Alignment.TopEnd)
                ) {
                    MemoryActionMenu(
                        isChinese = isChinese,
                        onDeleteClick = {
                            showDeleteDialog = true
                        }
                    )
                }
            }
        }

        // Delete Confirmation Dialog
        if (showDeleteDialog) {
            DeleteConfirmationDialog(
                isChinese = isChinese,
                onCancel = { showDeleteDialog = false },
                onDelete = {
                    showDeleteDialog = false
                    activeMenuMemoryId = null
                }
            )
        }
    }
}

// -------------------------------------------------------------
// 全新的書籤卡片設計 (對應 Figma)
// -------------------------------------------------------------
@Composable
fun SimpleBookmarkCard(
    spot: com.mcis.memoir.data.SpotData,
    isChinese: Boolean,
    onClick: () -> Unit
) {
    val title = if (isChinese) spot.titleZh else spot.titleEn
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
            // Image
            Image(
                painter = painterResource(spot.imageRes),
                contentDescription = title,
                modifier = Modifier
                    .width(100.dp)
                    .fillMaxHeight(),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(24.dp))

            // Title
            Text(
                text = title,
                style = TextStyle(
                    fontFamily = inter,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                ),
                modifier = Modifier.weight(1f)
            )

            // Bookmark Icon
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


// -------------------------------------------------------------
// 原有的 UI 元件 (無更動)
// -------------------------------------------------------------

@Composable
fun MemoryActionMenu(isChinese: Boolean, onDeleteClick: () -> Unit) {
    val menuShape = RoundedCornerShape(15.dp)
    Column(
        modifier = Modifier
            .width(197.dp)
            .shadow(12.dp, menuShape)
            .background(DesignTokens.colorLanguageSelectionBackground, menuShape)
            .padding(vertical = 8.dp)
    ) {
        MenuItem(UntitledIcons.EditIcon, if (isChinese) stringResource(R.string.memories_menu_edit_zh) else stringResource(R.string.memories_menu_edit))
        DividerLine()
        MenuItem(UntitledIcons.DeleteIcon, if (isChinese) stringResource(R.string.memories_menu_delete_zh) else stringResource(R.string.memories_menu_delete), onClick = onDeleteClick)
        DividerLine()
        MenuItem(UntitledIcons.DuplicateIcon, if (isChinese) stringResource(R.string.memories_menu_duplicate_zh) else stringResource(R.string.memories_menu_duplicate))
        DividerLine()
        MenuItem(UntitledIcons.ShareIcon, if (isChinese) stringResource(R.string.memories_menu_share_zh) else stringResource(R.string.memories_menu_share))
    }
}

@Composable
fun MenuItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(38.dp)
            .clickable { onClick() }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        UntitledIcon(imageVector = icon, contentDescription = null, size = 20.dp, tint = Color.Black)
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = label, style = TextStyle(fontFamily = inter, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Black))
    }
}

@Composable
fun DividerLine() {
    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFE0E0E0)))
}

@Composable
fun DeleteConfirmationDialog(isChinese: Boolean, onCancel: () -> Unit, onDelete: () -> Unit) {
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
                text = if (isChinese) stringResource(R.string.memories_delete_confirm_zh) else stringResource(R.string.memories_delete_confirm),
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
                    Text(text = if (isChinese) stringResource(R.string.cancel_button_zh) else stringResource(R.string.cancel_button), style = TextStyle(fontFamily = inter, fontSize = 14.sp, color = Color.Black))
                }
                Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(Color(0xFFE0E0E0)))
                Box(
                    modifier = Modifier.weight(1f).fillMaxHeight().clickable { onDelete() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = if (isChinese) stringResource(R.string.delete_button_zh) else stringResource(R.string.delete_button), style = TextStyle(fontFamily = inter, fontSize = 14.sp, color = DesignTokens.colorMaroon))
                }
            }
        }
    }
}

@Composable
fun InProgressMemoryCard(memory: MemoryData, isChinese: Boolean, onMoreClick: (MemoryData) -> Unit) {
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
            Image(
                painter = painterResource(memory.imageRes),
                contentDescription = null,
                modifier = Modifier
                    .width(132.dp)
                    .fillMaxHeight(),
                contentScale = ContentScale.Crop
            )
            Column(
                modifier = Modifier
                    .padding(start = 16.dp, top = 12.dp, end = 12.dp)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.Top
            ) {
                Text(
                    text = if (isChinese) memory.titleZh else memory.titleEn,
                    style = TextStyle(
                        fontFamily = inter,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = if (isChinese) stringResource(R.string.memories_continue_editing_zh) else stringResource(R.string.memories_continue_editing),
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
                        val progressWidth = (160 * (memory.currentProgress.toFloat() / memory.totalProgress)).dp
                        Box(
                            modifier = Modifier
                                .width(progressWidth)
                                .fillMaxHeight()
                                .background(Color(0xFFA32B2B), CircleShape)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${memory.currentProgress}/${memory.totalProgress}",
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

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
                .size(24.dp)
                .clip(CircleShape)
                .clickable { onMoreClick(memory) },
            contentAlignment = Alignment.Center
        ) {
            UntitledIcon(imageVector = UntitledIcons.MoreIcon, contentDescription = "More", size = 18.dp, tint = Color.Black)
        }
    }
}

@Composable
fun CompletedMemoryCard(memory: MemoryData, isChinese: Boolean, onMoreClick: (MemoryData) -> Unit) {
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
            Image(
                painter = painterResource(memory.imageRes),
                contentDescription = null,
                modifier = Modifier
                    .width(132.dp)
                    .fillMaxHeight(),
                contentScale = ContentScale.Crop
            )
            Column(
                modifier = Modifier
                    .padding(start = 16.dp, top = 12.dp, end = 12.dp)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.Top
            ) {
                Text(
                    text = if (isChinese) memory.titleZh else memory.titleEn,
                    style = TextStyle(
                        fontFamily = inter,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (isChinese) stringResource(R.string.memories_updated_on_zh, memory.date) else stringResource(R.string.memories_updated_on, memory.date),
                    style = TextStyle(
                        fontFamily = inter,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                )
                Spacer(modifier = Modifier.weight(1f))
                Row(
                    modifier = Modifier.padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    UntitledIcon(
                        imageVector = UntitledIcons.SavedFilled,
                        contentDescription = null,
                        tint = Color.Black,
                        size = 20.dp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = memory.likes.toString(),
                        style = TextStyle(fontFamily = inter, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    UntitledIcon(
                        imageVector = UntitledIcons.CommentIcon,
                        contentDescription = null,
                        tint = Color.Black,
                        size = 20.dp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = memory.comments.toString(),
                        style = TextStyle(fontFamily = inter, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 12.dp, end = 12.dp)
                .size(24.dp)
                .clip(CircleShape)
                .clickable { onMoreClick(memory) },
            contentAlignment = Alignment.Center
        ) {
            UntitledIcon(imageVector = UntitledIcons.MoreIcon, contentDescription = "More", size = 18.dp, tint = Color.Black)
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 12.dp)
                .size(24.dp)
                .clip(CircleShape)
                .clickable { /* Detail */ },
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(R.drawable.arrow_right),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(Color.Black)
            )
        }
    }
}

@Preview(showBackground = true, device = "spec:width=412dp,height=915dp")
@Composable
fun MemoriesScreenPreview() {
    AppTheme {
        MemoriesScreen()
    }
}