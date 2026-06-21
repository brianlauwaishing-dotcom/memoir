package com.mcis.memoir

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mcis.memoir.i18n.LocaleController
import com.mcis.memoir.ui.components.BottomNavigationBar
import com.mcis.memoir.ui.components.UntitledIcon
import com.mcis.memoir.ui.icons.*
import com.mcis.memoir.ui.spot.SpotDetailGalleryItem
import com.mcis.memoir.ui.spot.SpotDetailState
import com.mcis.memoir.ui.spot.SpotDetailViewModel
import com.mcis.memoir.ui.theme.AppTheme
import com.mcis.memoir.ui.theme.inter
import com.mcis.memoir.ui.theme.judson

@Composable
fun SpotDetailScreen(
    viewModel: SpotDetailViewModel,
    onBackClick: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToSaved: () -> Unit,
    onNavigateToMemories: () -> Unit,
    onDiscoveryItemClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle(initialValue = SpotDetailState())
    SpotDetailContent(
        state = state,
        onBackClick = onBackClick,
        onNavigateToHome = onNavigateToHome,
        onNavigateToSaved = onNavigateToSaved,
        onNavigateToMemories = onNavigateToMemories,
        onDiscoveryItemClick = onDiscoveryItemClick,
        modifier = modifier
    )
}

@Composable
private fun SpotDetailContent(
    state: SpotDetailState,
    onBackClick: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToSaved: () -> Unit,
    onNavigateToMemories: () -> Unit,
    onDiscoveryItemClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val isChinese = LocaleController.currentLocale().language == "zh"

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF7F3EA)) // New Beige Background
    ) {
        when {
            state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = DesignTokens.colorMaroon)
            }

            state.error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.spot_not_found))
            }

            else -> Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Main Content Area - Scrollable
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Hero Image
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(259.dp)
                    ) {
                        if (state.heroDrawableRes != 0) {
                            Image(
                                painter = painterResource(state.heroDrawableRes),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .padding(horizontal = 29.dp)
                            .padding(top = 24.dp)
                    ) {
                        // Title
                        Text(
                            text = state.title,
                            style = TextStyle(
                                fontFamily = judson,
                                fontSize = 40.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Info Row (Duration, Map and Podcast)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    UntitledIcon(
                                        imageVector = UntitledIcons.ClockIcon,
                                        contentDescription = null,
                                        tint = DesignTokens.colorMaroon,
                                        size = 18.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = state.duration,
                                        style = TextStyle(fontFamily = inter, fontSize = 16.sp, color = DesignTokens.colorMaroon)
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    UntitledIcon(
                                        imageVector = UntitledIcons.MapIcon,
                                        contentDescription = null,
                                        tint = DesignTokens.colorMaroon,
                                        size = 18.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isChinese) stringResource(R.string.spot_detail_map_zh) else stringResource(R.string.spot_detail_map),
                                        style = TextStyle(fontFamily = inter, fontSize = 16.sp, color = DesignTokens.colorMaroon),
                                        modifier = Modifier.clickable { /* Map logic */ }
                                    )
                                }
                            }

                            // Podcast Button (Moved from Explore Screen)
                            val podcastShape = RoundedCornerShape(50.dp)
                            Box(
                                modifier = Modifier
                                    .width(148.dp)
                                    .height(38.dp)
                                    .background(DesignTokens.colorMaroon, podcastShape)
                                    .clip(podcastShape)
                                    .clickable { /* Podcast logic */ },
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    UntitledIcon(
                                        imageVector = UntitledIcons.PodcastIcon,
                                        contentDescription = null,
                                        tint = Color.White,
                                        size = 20.dp
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isChinese) stringResource(R.string.spot_explore_podcast_zh) else stringResource(R.string.spot_explore_podcast),
                                        style = TextStyle(fontFamily = inter, fontSize = 16.sp, color = Color.White)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Section: Why It Matters
                        SpotSection(
                            title = if (isChinese) "/ ${stringResource(R.string.spot_detail_why_matters_zh)}" else "/ ${stringResource(R.string.spot_detail_why_matters)}",
                            content = state.whyItMatters
                        )

                        // Discovery Gallery (Artifacts)
                        DiscoveryGallery(state.discoveryItems, onDiscoveryItemClick)

                        // Section: Historical Context
                        SpotSection(
                            title = if (isChinese) "/ ${stringResource(R.string.spot_detail_historical_context_zh)}" else "/ ${stringResource(R.string.spot_detail_historical_context)}",
                            content = state.historicalContext
                        )

                        // Section: Architectural Features
                        SpotSection(
                            title = if (isChinese) "/ ${stringResource(R.string.spot_detail_architectural_features_zh)}" else "/ ${stringResource(R.string.spot_detail_architectural_features)}",
                            content = state.architecturalFeatures
                        )

                        // Section: Modern Use & Preservation
                        SpotSection(
                            title = if (isChinese) "/ ${stringResource(R.string.spot_detail_modern_use_preservation_zh)}" else "/ ${stringResource(R.string.spot_detail_modern_use_preservation)}",
                            content = state.modernUse
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Feelings Section
                        Text(
                            text = if (isChinese) stringResource(R.string.spot_detail_how_make_feel_zh) else stringResource(R.string.spot_detail_how_make_feel),
                            style = TextStyle(fontFamily = inter, fontSize = 18.sp, color = Color.Black)
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            state.feelings.forEach { feeling ->
                                FeelingChip(feeling)
                            }
                        }

                        Spacer(modifier = Modifier.height(120.dp))
                    }
                }

                BottomNavigationBar(
                    isChinese = isChinese,
                    onHomeClick = onNavigateToHome,
                    onSavedClick = onNavigateToSaved,
                    onMemoriesClick = onNavigateToMemories,
                    currentDestination = ""
                )
            }
        }

        // Fixed Back Button
        Box(
            modifier = Modifier
                .padding(top = 57.dp, start = 18.dp)
                .size(24.dp)
                .clickable { onBackClick() },
            contentAlignment = Alignment.Center
        ) {
            UntitledIcon(
                imageVector = UntitledIcons.BackIcon,
                contentDescription = if (isChinese) stringResource(R.string.back_button_zh) else stringResource(R.string.back_button),
                tint = Color.White,
                size = 24.dp
            )
        }
    }
}

@Composable
fun DiscoveryGallery(items: List<SpotDetailGalleryItem>, onMoreClick: (Int) -> Unit) {
    if (items.isEmpty()) return

    Column(modifier = Modifier.padding(bottom = 32.dp)) {
        items.forEach { item ->
            DiscoveryEntry(item, onMoreClick)
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun DiscoveryEntry(item: SpotDetailGalleryItem, onMoreClick: (Int) -> Unit) {
    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(188.dp)
                .clip(RoundedCornerShape(8.dp))
        ) {
            if (item.imageDrawableRes != 0) {
                Image(
                    painter = painterResource(item.imageDrawableRes),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = item.label,
                style = TextStyle(
                    fontFamily = inter, // Fallback for specialized font
                    fontSize = 20.sp,
                    color = Color(0xFF2B2B2B)
                )
            )

            val buttonShape = RoundedCornerShape(15.dp)
            Box(
                modifier = Modifier
                    .width(90.dp)
                    .height(34.dp)
                    .background(DesignTokens.colorMaroon, buttonShape)
                    .clip(buttonShape)
                    .clickable { onMoreClick(item.id) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (LocaleController.currentLocale().language == "zh") stringResource(R.string.more_button_zh) else stringResource(R.string.more_button),
                    style = TextStyle(fontSize = 15.sp, color = Color.White)
                )
            }
        }
    }
}

@Composable
fun SpotSection(title: String, content: String) {
    if (content.isBlank()) return
    Column(modifier = Modifier.padding(bottom = 24.dp)) {
        Text(
            text = title,
            style = TextStyle(fontFamily = inter, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = content,
            style = TextStyle(fontFamily = inter, fontSize = 16.sp, color = Color.Black, lineHeight = 22.sp)
        )
    }
}

@Composable
fun FeelingChip(label: String) {
    Box(
        modifier = Modifier
            .border(1.dp, Color(0xFF8B8B8B), RoundedCornerShape(50.dp))
            .padding(horizontal = 16.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = label, style = TextStyle(fontFamily = inter, fontSize = 16.sp, color = Color.Black))
    }
}

@Preview(showBackground = true, device = "spec:width=412dp,height=915dp")
@Composable
fun SpotDetailScreenPreview() {
    AppTheme {
        SpotDetailContent(
            state = SpotDetailState(
                isLoading = false,
                title = "Grand Mazu Temple",
                heroDrawableRes = R.drawable.grand_mazu_temple,
                duration = "20–30 mins",
                whyItMatters = "The first officially built Mazu temple in Taiwan.",
                historicalContext = "Converted from the Zheng Kingdom's royal palace in 1684.",
                feelings = listOf("Awe", "Peaceful", "Inspired"),
                discoveryItems = listOf(
                    SpotDetailGalleryItem(1, "Dragon Pillar", R.drawable.dragon_pillar)
                )
            ),
            onBackClick = {},
            onNavigateToHome = {},
            onNavigateToSaved = {},
            onNavigateToMemories = {},
            onDiscoveryItemClick = {}
        )
    }
}
