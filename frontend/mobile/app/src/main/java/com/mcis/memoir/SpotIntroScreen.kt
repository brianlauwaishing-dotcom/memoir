package com.mcis.memoir

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.mcis.memoir.data.DiscoveryItem
import com.mcis.memoir.data.MockData
import com.mcis.memoir.data.SpotData
import com.mcis.memoir.ui.components.UntitledIcon
import com.mcis.memoir.ui.icons.*
import com.mcis.memoir.ui.theme.AppTheme
import com.mcis.memoir.ui.theme.inter

/**
 * New Intro screen for a cultural spot.
 * Based on Figma node 622:224.
 * Leads to ArtifactDetailScreen or SpotDetailScreen.
 */
@Composable
fun SpotIntroScreen(
    selectedLanguage: String = "en",
    spotId: String = "grand_mazu",
    onBackClick: () -> Unit = {},
    onInfoClick: (String) -> Unit = {},
    onDiscoveryItemClick: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isChinese = selectedLanguage == "zh"
    val spot = remember(spotId) { MockData.spots.find { it.id == spotId } }

    if (spot == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.spot_not_found))
        }
        return
    }

    Box(modifier = modifier.fillMaxSize()) {
        // 1. Full Screen Background
        Image(
            painter = painterResource(spot.imageRes),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // 2. Semi-transparent Overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 30.dp)
        ) {
            Spacer(modifier = Modifier.height(57.dp))

            // 3. Header Row (Back and Title)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { onBackClick() },
                    contentAlignment = Alignment.Center
                ) {
                    UntitledIcon(
                        imageVector = UntitledIcons.BackIcon,
                        contentDescription = stringResource(R.string.back_button),
                        tint = Color.White,
                        size = 24.dp
                    )
                }
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = if (isChinese) spot.titleZh else spot.titleEn,
                    style = TextStyle(
                        fontFamily = inter,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color.White
                    ),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(26.dp))

            // 4. Discovery Mode Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = if (isChinese) stringResource(R.string.discovery_mode_zh) else stringResource(R.string.discovery_mode),
                        style = TextStyle(
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color.White
                        )
                    )
                    Spacer(modifier = Modifier.height(5.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${spot.discoveryItems.size}/${spot.discoveryItems.size}",
                            style = TextStyle(
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Normal,
                                color = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.width(20.dp))
                        Text(
                            text = if (isChinese) stringResource(R.string.discovery_found_zh) else stringResource(R.string.discovery_found),
                            style = TextStyle(
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Normal,
                                color = Color.White
                            )
                        )
                    }
                }

                // 5. Info Button (Link to Spot Detail)
                UntitledIcon(
                    imageVector = UntitledIcons.InfoIcon,
                    contentDescription = "Info",
                    tint = Color.White,
                    size = 38.dp,
                    modifier = Modifier
                        .padding(top = 66.dp) // Adjusted based on Figma
                        .clip(CircleShape)
                        .clickable { onInfoClick(spotId) }
                )
            }

            Spacer(modifier = Modifier.height(33.dp))

            // 6. Discovery Items List
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(25.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(spot.discoveryItems) { item ->
                    DiscoveryItemCard(
                        item = item, 
                        isChinese = isChinese,
                        onClick = { onDiscoveryItemClick(item.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun DiscoveryItemCard(item: DiscoveryItem, isChinese: Boolean, onClick: () -> Unit = {}) {
    val cardShape = RoundedCornerShape(15.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(116.dp)
            .background(Color.Transparent, cardShape)
            .border(1.dp, Color.White.copy(alpha = 0.5f), cardShape)
            .clip(cardShape)
            .clickable { onClick() }
            .padding(start = 9.dp, top = 7.dp, bottom = 6.dp, end = 30.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(33.dp)
    ) {
        // Artifact Mask/Image
        Box(
            modifier = Modifier
                .size(103.dp)
                .clip(CircleShape)
        ) {
            Image(
                painter = painterResource(item.imageRes),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        Text(
            text = if (isChinese) item.labelZh else item.labelEn,
            style = TextStyle(
                fontSize = 24.sp,
                fontWeight = FontWeight.Normal,
                color = Color.White
            )
        )
    }
}

@Preview(showBackground = true, device = "spec:width=412dp,height=915dp")
@Composable
fun SpotIntroScreenPreview() {
    AppTheme {
        SpotIntroScreen()
    }
}
