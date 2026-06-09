package com.mcis.memoir

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mcis.memoir.i18n.LocaleController
import com.mcis.memoir.ui.artifact.ArtifactDetailState
import com.mcis.memoir.ui.artifact.ArtifactDetailViewModel
import com.mcis.memoir.ui.components.BottomNavigationBar
import com.mcis.memoir.ui.components.UntitledIcon
import com.mcis.memoir.ui.icons.*
import com.mcis.memoir.ui.theme.AppTheme
import com.mcis.memoir.ui.theme.inter

@Composable
fun ArtifactDetailScreen(
    viewModel: ArtifactDetailViewModel,
    onBackClick: () -> Unit,
    onInfoClick: () -> Unit,
    onCameraClick: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToSaved: () -> Unit,
    onNavigateToMemories: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle(initialValue = ArtifactDetailState())
    ArtifactDetailContent(
        state = state,
        onBackClick = onBackClick,
        onInfoClick = onInfoClick,
        onCameraClick = onCameraClick,
        onNavigateToHome = onNavigateToHome,
        onNavigateToSaved = onNavigateToSaved,
        onNavigateToMemories = onNavigateToMemories,
        modifier = modifier
    )
}

@Composable
private fun ArtifactDetailContent(
    state: ArtifactDetailState,
    onBackClick: () -> Unit,
    onInfoClick: () -> Unit,
    onCameraClick: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToSaved: () -> Unit,
    onNavigateToMemories: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isChinese = LocaleController.currentLocale().language == "zh"

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
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
                        startY = 0f,
                        endY = 2200f
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

            else -> Column(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 42.dp)
                ) {
                    Spacer(modifier = Modifier.height(57.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset(x = (-24).dp)
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
                            text = stringResource(R.string.discovery_you_discovered),
                            style = TextStyle(
                                fontFamily = inter,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Normal,
                                color = Color.White
                            )
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        UntitledIcon(
                            imageVector = UntitledIcons.InfoIcon,
                            contentDescription = stringResource(R.string.spot_explore_info_content_description),
                            tint = Color.White,
                            size = 38.dp,
                            modifier = Modifier
                                .offset(x = 26.dp, y = (-7).dp)
                                .clip(CircleShape)
                                .clickable { onInfoClick() }
                        )
                    }

                    Spacer(modifier = Modifier.height(54.dp))

                    Text(
                        text = state.label,
                        style = TextStyle(
                            fontSize = 40.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color.White
                        )
                    )

                    Spacer(modifier = Modifier.height(150.dp))

                    Text(
                        text = state.description,
                        style = TextStyle(
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color.White,
                            lineHeight = 32.sp
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

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
                            contentDescription = stringResource(R.string.spot_explore_take_photo),
                            tint = Color.White,
                            size = 24.dp
                        )
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
    }
}

@Preview(showBackground = true, device = "spec:width=412dp,height=915dp")
@Composable
fun ArtifactDetailScreenPreview() {
    AppTheme {
        ArtifactDetailContent(
            state = ArtifactDetailState(
                isLoading = false,
                label = "Dragon Pillar",
                description = "The coiling dragon pillar symbolizes protection and strength.",
                imageDrawableRes = R.drawable.dragon_pillar
            ),
            onBackClick = {},
            onInfoClick = {},
            onCameraClick = {},
            onNavigateToHome = {},
            onNavigateToSaved = {},
            onNavigateToMemories = {}
        )
    }
}
