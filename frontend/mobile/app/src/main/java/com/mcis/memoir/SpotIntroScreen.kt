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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.mcis.memoir.ui.components.UntitledIcon
import com.mcis.memoir.ui.icons.*
import com.mcis.memoir.ui.spot.DiscoveryItemCard
import com.mcis.memoir.ui.spot.SpotIntroState
import com.mcis.memoir.ui.spot.SpotIntroViewModel
import com.mcis.memoir.ui.theme.AppTheme
import com.mcis.memoir.ui.theme.inter

@Composable
fun SpotIntroScreen(
    viewModel: SpotIntroViewModel,
    onBackClick: () -> Unit,
    onInfoClick: (String) -> Unit,
    onDiscoveryItemClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle(initialValue = SpotIntroState())
    SpotIntroContent(
        state = state,
        onBackClick = onBackClick,
        onInfoClick = onInfoClick,
        onDiscoveryItemClick = onDiscoveryItemClick,
        modifier = modifier
    )
}

@Composable
private fun SpotIntroContent(
    state: SpotIntroState,
    onBackClick: () -> Unit,
    onInfoClick: (String) -> Unit,
    onDiscoveryItemClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        if (state.heroDrawableRes == 0) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.DarkGray)
            )
        } else {
            Image(
                painter = painterResource(state.heroDrawableRes),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
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
                    .padding(horizontal = 30.dp)
            ) {
                Spacer(modifier = Modifier.height(57.dp))

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
                        text = state.title,
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.discovery_mode),
                            style = TextStyle(
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Normal,
                                color = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.height(5.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = state.foundLabel,
                                style = TextStyle(
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = Color.White
                                )
                            )
                            Spacer(modifier = Modifier.width(20.dp))
                            Text(
                                text = stringResource(R.string.discovery_found),
                                style = TextStyle(
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = Color.White
                                )
                            )
                        }
                    }

                    UntitledIcon(
                        imageVector = UntitledIcons.InfoIcon,
                        contentDescription = stringResource(R.string.spot_explore_info_content_description),
                        tint = Color.White,
                        size = 38.dp,
                        modifier = Modifier
                            .padding(top = 66.dp)
                            .clip(CircleShape)
                            .clickable { state.spotId?.let(onInfoClick) }
                    )
                }

                Spacer(modifier = Modifier.height(33.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(25.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(state.discoveryItems, key = { it.id }) { item ->
                        DiscoveryItemRow(
                            item = item,
                            onClick = { onDiscoveryItemClick(item.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DiscoveryItemRow(item: DiscoveryItemCard, onClick: () -> Unit = {}) {
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
        Box(
            modifier = Modifier
                .size(103.dp)
                .clip(CircleShape)
        ) {
            if (item.imageDrawableRes == 0) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.DarkGray)
                )
            } else {
                Image(
                    painter = painterResource(item.imageDrawableRes),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }

        Text(
            text = item.label,
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
        SpotIntroContent(
            state = SpotIntroState(
                isLoading = false,
                spotId = "demo",
                title = "Demo Spot",
                heroDrawableRes = R.drawable.grand_mazu_temple,
                foundLabel = "3/3",
                discoveryItems = listOf(
                    DiscoveryItemCard(1, "Dragon Pillar", R.drawable.dragon_pillar)
                )
            ),
            onBackClick = {},
            onInfoClick = {},
            onDiscoveryItemClick = {}
        )
    }
}
