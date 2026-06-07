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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mcis.memoir.data.MockData
import com.mcis.memoir.ui.components.UntitledIcon
import com.mcis.memoir.ui.icons.*
import com.mcis.memoir.ui.theme.AppTheme
import com.mcis.memoir.ui.theme.inter

/**
 * Screen for the "Look Closer" discovery step of an artifact.
 * Based on Figma node 622:223.
 */
@Composable
fun ArtifactDiscoveryScreen(
    selectedLanguage: String = "en",
    spotId: String = "grand_mazu",
    artifactId: Int = 1,
    onBackClick: () -> Unit = {},
    onInfoClick: (String) -> Unit = {},
    onMoreClick: (String, Int) -> Unit = { _, _ -> },
    onCameraClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isChinese = selectedLanguage == "zh"
    val spot = remember(spotId) { MockData.spots.find { it.id == spotId } }
    val artifact = remember(artifactId) { spot?.discoveryItems?.find { it.id == artifactId } }

    if (spot == null || artifact == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(if (isChinese) stringResource(R.string.spot_not_found_zh) else stringResource(R.string.spot_not_found))
        }
        return
    }

    Box(modifier = modifier.fillMaxSize()) {
        // 1. Full Screen Artifact Image
        Image(
            painter = painterResource(artifact.imageRes),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // 2. Gradient Overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black),
                        startY = 350f
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 42.dp)
        ) {
            Spacer(modifier = Modifier.height(57.dp))

            // 3. Header Row
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
                    text = if (isChinese) stringResource(R.string.discovery_mode_zh) else stringResource(R.string.discovery_mode),
                    style = TextStyle(
                        fontFamily = inter,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color.White
                    )
                )

                Spacer(modifier = Modifier.weight(1f))

                // 4. Info Button
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

            Spacer(modifier = Modifier.height(450.dp))

            // 5. "Look Closer" Label and Index
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = if (isChinese) stringResource(R.string.discovery_look_closer_zh) else stringResource(R.string.discovery_look_closer),
                    style = TextStyle(
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color.White
                    )
                )
                Text(
                    text = "${artifact.id}/${spot.discoveryItems.size}",
                    style = TextStyle(
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color.White
                    ),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // 6. Question with highlighting
            val question = if (isChinese) artifact.questionZh else artifact.questionEn
            val label = if (isChinese) artifact.labelZh else artifact.labelEn
            val annotatedQuestion = buildAnnotatedString {
                val parts = question.split(label)
                if (parts.size > 1) {
                    append(parts[0])
                    withStyle(style = SpanStyle(color = Color(0xFFBF1B20))) {
                        append(label)
                    }
                    append(parts[1])
                } else {
                    append(question)
                }
            }

            Text(
                text = annotatedQuestion,
                style = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Normal, color = Color.White),
                modifier = Modifier.width(242.dp)
            )

            Spacer(modifier = Modifier.height(128.dp))

            // 7. Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // "More" Button -> Now leads to the storytelling ArtifactDetailScreen
                val buttonShape = RoundedCornerShape(15.dp)
                Box(
                    modifier = Modifier
                        .width(145.dp)
                        .height(51.dp)
                        .background(DesignTokens.colorMaroon, buttonShape)
                        .clip(buttonShape)
                        .clickable { onMoreClick(spotId, artifactId) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isChinese) stringResource(R.string.more_button_zh) else stringResource(R.string.more_button),
                        style = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Normal, color = Color.White)
                    )
                }

                // Camera Action Button
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
        }
    }
}

@Preview(showBackground = true, device = "spec:width=412dp,height=915dp")
@Composable
fun ArtifactDiscoveryScreenPreview() {
    AppTheme {
        ArtifactDiscoveryScreen()
    }
}
