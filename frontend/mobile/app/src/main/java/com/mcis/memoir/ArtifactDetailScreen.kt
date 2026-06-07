package com.mcis.memoir

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import com.mcis.memoir.ui.components.BottomNavigationBar
import com.mcis.memoir.ui.components.UntitledIcon
import com.mcis.memoir.ui.icons.*
import com.mcis.memoir.ui.theme.AppTheme
import com.mcis.memoir.ui.theme.inter

/**
 * Screen showing details and storytelling for a specific artifact.
 * Based on Figma node 677:254.
 */
@Composable
fun ArtifactDetailScreen(
    selectedLanguage: String = "en",
    spotId: String = "grand_mazu",
    artifactId: Int = 1,
    onBackClick: () -> Unit = {},
    onInfoClick: (String) -> Unit = {},
    onCameraClick: () -> Unit = {},
    onNavigateToHome: () -> Unit = {},
    onNavigateToSaved: () -> Unit = {},
    onNavigateToMemories: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isChinese = selectedLanguage == "zh"
    val spot = remember(spotId) { MockData.spots.find { it.id == spotId } }
    val artifact = remember(artifactId) { spot?.discoveryItems?.find { it.id == artifactId } }

    if (spot == null || artifact == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.spot_not_found))
        }
        return
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // 1. Full Screen Artifact Image
        Image(
            painter = painterResource(artifact.imageRes),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // 2. Dark Gradient Overlay (Fade to black at bottom)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black),
                        startY = 0f,
                        endY = 2200f // Deep black at bottom
                    )
                )
        )

        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 42.dp)
            ) {
                Spacer(modifier = Modifier.height(57.dp))

                // 3. Header Row (Back, "You Discovered" label and Info Button)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().offset(x = (-24).dp)
                ) {
                    Box(
                        modifier = Modifier
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
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = if (isChinese) stringResource(R.string.discovery_you_discovered_zh) else stringResource(R.string.discovery_you_discovered),
                        style = TextStyle(
                            fontFamily = inter,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color.White
                        )
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    // Info Button (Link to Spot Detail)
                    UntitledIcon(
                        imageVector = UntitledIcons.InfoIcon,
                        contentDescription = if (isChinese) stringResource(R.string.spot_explore_info_content_description_zh) else stringResource(R.string.spot_explore_info_content_description),
                        tint = Color.White,
                        size = 38.dp,
                        modifier = Modifier
                            .offset(x = 26.dp, y = (-7).dp)
                            .clip(CircleShape)
                            .clickable { onInfoClick(spotId) }
                    )
                }

                Spacer(modifier = Modifier.height(54.dp))

                // 4. Artifact Title
                Text(
                    text = if (isChinese) artifact.labelZh else artifact.labelEn,
                    style = TextStyle(
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(150.dp))

                // 5. Long Description / Story
                Text(
                    text = if (isChinese) artifact.moreInfoZh else artifact.moreInfoEn,
                    style = TextStyle(
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color.White,
                        lineHeight = 32.sp
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 6. Camera Action Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 34.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(DesignTokens.colorMaroon, CircleShape)
                        .clip(CircleShape)
                        .clickable { onCameraClick() },
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

            // 7. Bottom Navigation Bar
            BottomNavigationBar(
                isChinese = isChinese,
                onHomeClick = onNavigateToHome,
                onSavedClick = onNavigateToSaved,
                onMemoriesClick = onNavigateToMemories,
                currentDestination = ""
            )
        }
    }
}

@Preview(showBackground = true, device = "spec:width=412dp,height=915dp")
@Composable
fun ArtifactDetailScreenPreview() {
    AppTheme {
        ArtifactDetailScreen()
    }
}
