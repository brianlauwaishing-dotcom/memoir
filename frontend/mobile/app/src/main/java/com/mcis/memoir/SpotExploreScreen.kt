package com.mcis.memoir

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import com.mcis.memoir.data.MockData
import com.mcis.memoir.data.RouteData
import com.mcis.memoir.ui.components.BottomNavigationBar
import com.mcis.memoir.ui.components.UntitledIcon
import com.mcis.memoir.ui.icons.*
import com.mcis.memoir.ui.theme.AppTheme
import com.mcis.memoir.ui.theme.inter
import com.mcis.memoir.ui.theme.judson

@Composable
fun SpotExploreScreen(
    selectedLanguage: String = "en",
    spotId: String = "grand_mazu",
    onBackClick: () -> Unit = {},
    onNavigateToHome: () -> Unit = {},
    onNavigateToSaved: () -> Unit = {},
    onNavigateToMemories: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isChinese = selectedLanguage == "zh"
    val spot = remember(spotId) { MockData.spots.find { it.id == spotId } }

    if (spot == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(if (isChinese) stringResource(R.string.spot_not_found_zh) else stringResource(R.string.spot_not_found))
        }
        return
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DesignTokens.colorLanguageSelectionBackground)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                // Hero Image Section
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(259.dp)
                ) {
                    Image(
                        painter = painterResource(spot.imageRes),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                Column(
                    modifier = Modifier
                        .padding(horizontal = 29.dp)
                        .padding(top = 24.dp)
                ) {
                    // Title and Info Icon Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (isChinese) spot.titleZh else spot.titleEn,
                            style = TextStyle(
                                fontFamily = judson,
                                fontSize = 40.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        
                        UntitledIcon(
                            imageVector = UntitledIcons.InfoIcon,
                            contentDescription = if (isChinese) stringResource(R.string.spot_explore_info_content_description_zh) else stringResource(R.string.spot_explore_info_content_description),
                            tint = Color.Black,
                            size = 38.dp,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { /* Info logic */ }
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Buttons Row (Podcast & Share)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Podcast Button
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

                        // Share Button - FIXED: Added padding before clipping to prevent cutting the icon
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .clickable { /* Share logic */ },
                            contentAlignment = Alignment.Center
                        ) {
                            UntitledIcon(
                                imageVector = UntitledIcons.ShareIcon,
                                contentDescription = if (isChinese) stringResource(R.string.spot_explore_share_content_description_zh) else stringResource(R.string.spot_explore_share_content_description),
                                tint = Color.Black,
                                size = 28.dp // Smaller than container to avoid edge clipping
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Facts List
                    val facts = if (isChinese) spot.factsZh else spot.factsEn
                    facts.forEach { fact ->
                        Row(modifier = Modifier.padding(bottom = 8.dp)) {
                            Text(text = "• ", style = TextStyle(fontFamily = inter, fontSize = 16.sp))
                            Text(text = fact, style = TextStyle(fontFamily = inter, fontSize = 16.sp))
                        }
                    }

                    Spacer(modifier = Modifier.height(40.dp))

                    // Photography Tips Section
                    Text(
                        text = if (isChinese) stringResource(R.string.spot_explore_photography_tips_zh) else stringResource(R.string.spot_explore_photography_tips),
                        style = TextStyle(
                            fontFamily = judson, 
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color.Black
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Tip 1: Front Panorama (Full Width)
                    val tip1 = spot.photographyTips.find { it.id == 1 }
                    if (tip1 != null) {
                        PhotographyTipCard(tip1, isChinese)
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Tip 2 & 3: Side by Side or Vertical
                    Row(modifier = Modifier.fillMaxWidth()) {
                        val tip2 = spot.photographyTips.find { it.id == 2 }
                        val tip3 = spot.photographyTips.find { it.id == 3 }
                        
                        if (tip2 != null) {
                            Column(modifier = Modifier.weight(1f)) {
                                PhotographyTipCardSmall(tip2, isChinese)
                            }
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        if (tip3 != null) {
                            Column(modifier = Modifier.weight(1f)) {
                                PhotographyTipCardSmall(tip3, isChinese)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(100.dp))
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
fun PhotographyTipCard(tip: com.mcis.memoir.data.PhotographyTip, isChinese: Boolean) {
    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(188.dp)
                .clip(RoundedCornerShape(8.dp))
        ) {
            if (tip.imageRes != null) {
                Image(
                    painter = painterResource(tip.imageRes),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            
            // Camera Button Overlay
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(80.dp)
                    .background(DesignTokens.colorMaroon, CircleShape)
                    .clip(CircleShape)
                    .clickable { /* Camera logic */ },
                contentAlignment = Alignment.Center
            ) {
                UntitledIcon(
                    imageVector = UntitledIcons.CameraIcon,
                    contentDescription = if (isChinese) stringResource(R.string.spot_explore_take_photo_zh) else stringResource(R.string.spot_explore_take_photo),
                    tint = Color.White,
                    size = 24.dp
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "${tip.id}. ${if (isChinese) tip.descriptionZh else tip.descriptionEn}",
            style = TextStyle(fontFamily = inter, fontSize = 14.sp, color = Color(0xFF2B2B2B))
        )
    }
}

@Composable
fun PhotographyTipCardSmall(tip: com.mcis.memoir.data.PhotographyTip, isChinese: Boolean) {
    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(231.dp)
                .clip(RoundedCornerShape(8.dp))
        ) {
            if (tip.imageRes != null) {
                Image(
                    painter = painterResource(tip.imageRes),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "${tip.id}. ${if (isChinese) tip.descriptionZh else tip.descriptionEn}",
            style = TextStyle(fontFamily = inter, fontSize = 14.sp, color = Color(0xFF2B2B2B))
        )
    }
}

@Preview(showBackground = true, device = "spec:width=412dp,height=915dp")
@Composable
fun SpotExploreScreenPreview() {
    AppTheme {
        SpotExploreScreen()
    }
}
