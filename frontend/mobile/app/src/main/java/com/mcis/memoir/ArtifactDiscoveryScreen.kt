package com.mcis.memoir

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mcis.memoir.ui.artifact.ArtifactDiscoveryState
import com.mcis.memoir.ui.artifact.ArtifactDiscoveryViewModel
import com.mcis.memoir.ui.artifact.QuestionHighlight
import com.mcis.memoir.ui.components.UntitledIcon
import com.mcis.memoir.ui.icons.*
import com.mcis.memoir.ui.theme.AppTheme
import com.mcis.memoir.ui.theme.inter

@Composable
fun ArtifactDiscoveryScreen(
    viewModel: ArtifactDiscoveryViewModel,
    onBackClick: () -> Unit,
    onInfoClick: (String) -> Unit,
    onMoreClick: (String, Int) -> Unit,
    onCameraClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle(initialValue = ArtifactDiscoveryState())
    ArtifactDiscoveryContent(
        state = state,
        onBackClick = onBackClick,
        onInfoClick = onInfoClick,
        onBookmarkClick = viewModel::onBookmarkClick,
        onMoreClick = onMoreClick,
        onCameraClick = onCameraClick,
        modifier = modifier
    )
}

@Composable
private fun ArtifactDiscoveryContent(
    state: ArtifactDiscoveryState,
    onBackClick: () -> Unit,
    onInfoClick: (String) -> Unit,
    onBookmarkClick: () -> Unit,
    onMoreClick: (String, Int) -> Unit,
    onCameraClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        if (state.imageDrawableRes == 0) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.DarkGray)
            )
        } else {
            Image(
                painter = painterResource(state.imageDrawableRes),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

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

        when {
            state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color.White)
            }

            state.error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = stringResource(R.string.spot_not_found), color = Color.White)
            }

            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(top = 16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color.Black.copy(alpha = 0.38f), CircleShape)
                            .clip(CircleShape)
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
                    Spacer(modifier = Modifier.width(10.dp))
                    Box(
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.38f), RoundedCornerShape(18.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.discovery_mode),
                            style = TextStyle(
                                fontFamily = inter,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Normal,
                                color = Color.White
                            )
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(Color.Black.copy(alpha = 0.38f), CircleShape)
                                .clip(CircleShape)
                                .clickable { onBookmarkClick() },
                            contentAlignment = Alignment.Center
                        ) {
                            UntitledIcon(
                                imageVector = if (state.isBookmarked) {
                                    UntitledIcons.BookmarkFilled
                                } else {
                                    UntitledIcons.BookmarkIcon
                                },
                                contentDescription = stringResource(R.string.artifact_bookmark_content_description),
                                tint = Color.White,
                                size = 26.dp
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(Color.Black.copy(alpha = 0.38f), CircleShape)
                                .clip(CircleShape)
                                .clickable { state.spotId?.let(onInfoClick) },
                            contentAlignment = Alignment.Center
                        ) {
                            UntitledIcon(
                                imageVector = UntitledIcons.InfoIcon,
                                contentDescription = stringResource(R.string.spot_explore_info_content_description),
                                tint = Color.White,
                                size = 30.dp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = stringResource(R.string.discovery_look_closer),
                        style = TextStyle(
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color.White
                        )
                    )
                    Text(
                        text = "${state.capturedArtifactsCount}/${state.totalArtifacts}",
                        style = TextStyle(
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color.White
                        ),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                Text(
                    text = highlightedQuestion(state.highlight),
                    style = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Normal, color = Color.White),
                    modifier = Modifier.width(242.dp)
                )

                Spacer(modifier = Modifier.height(28.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val buttonShape = RoundedCornerShape(15.dp)
                    Box(
                        modifier = Modifier
                            .width(145.dp)
                            .height(51.dp)
                            .background(DesignTokens.colorMaroon, buttonShape)
                            .clip(buttonShape)
                            .clickable {
                                state.spotId?.let { spotId ->
                                    onMoreClick(spotId, state.artifactId)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.more_button),
                            style = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Normal, color = Color.White)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .background(DesignTokens.colorMaroon, CircleShape)
                            .border(2.dp, Color.White.copy(alpha = 0.88f), CircleShape)
                            .clip(CircleShape)
                            .clickable { onCameraClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        UntitledIcon(
                            imageVector = UntitledIcons.CameraIcon,
                            contentDescription = stringResource(R.string.spot_explore_take_photo),
                            tint = Color.White,
                            size = 24.dp
                        )
                    }
                }
            }
        }
    }
}

private fun highlightedQuestion(highlight: QuestionHighlight) = buildAnnotatedString {
    append(highlight.prefix)
    if (highlight.label.isNotEmpty()) {
        withStyle(style = SpanStyle(color = Color(0xFFBF1B20))) {
            append(highlight.label)
        }
        append(highlight.suffix)
    }
}

@Preview(showBackground = true, device = "spec:width=412dp,height=915dp")
@Composable
fun ArtifactDiscoveryScreenPreview() {
    AppTheme {
        ArtifactDiscoveryContent(
            state = ArtifactDiscoveryState(
                isLoading = false,
                spotId = "demo",
                artifactId = 1,
                displayPosition = 1,
                totalArtifacts = 3,
                label = "Dragon Pillar",
                highlight = QuestionHighlight("How many ", "Dragon Pillar", " carvings do you see?"),
                imageDrawableRes = R.drawable.dragon_pillar
            ),
            onBackClick = {},
            onInfoClick = {},
            onBookmarkClick = {},
            onMoreClick = { _, _ -> },
            onCameraClick = {}
        )
    }
}
