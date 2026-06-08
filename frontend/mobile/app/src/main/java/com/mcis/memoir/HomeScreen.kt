package com.mcis.memoir

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
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
import com.mcis.memoir.ui.home.HomeIntent
import com.mcis.memoir.ui.home.HomeState
import com.mcis.memoir.ui.home.HomeViewModel
import com.mcis.memoir.ui.home.RouteCard
import com.mcis.memoir.ui.home.RouteCardItem
import com.mcis.memoir.ui.home.TagCatalog
import com.mcis.memoir.ui.icons.SearchIcon
import com.mcis.memoir.ui.icons.UntitledIcons
import com.mcis.memoir.ui.theme.AppTheme
import com.mcis.memoir.ui.theme.inter
import com.mcis.memoir.ui.theme.judson

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToSaved: () -> Unit,
    onNavigateToMemories: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    HomeContent(
        state = state,
        onIntent = viewModel::onIntent,
        onNavigateToSaved = onNavigateToSaved,
        onNavigateToMemories = onNavigateToMemories,
        modifier = modifier
    )
}

@Composable
private fun HomeContent(
    state: HomeState,
    onIntent: (HomeIntent) -> Unit,
    onNavigateToSaved: () -> Unit = {},
    onNavigateToMemories: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isChinese = LocaleController.currentLocale().language == "zh"

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
                    .padding(horizontal = 29.dp)
                    .padding(top = 60.dp)
            ) {
                Text(
                    text = stringResource(R.string.home_subtitle),
                    style = TextStyle(
                        fontFamily = inter,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color.Black
                    )
                )
                Text(
                    text = stringResource(R.string.home_headline),
                    style = TextStyle(
                        fontFamily = judson,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color.Black
                    )
                )

                Spacer(modifier = Modifier.height(11.dp))

                SearchBar(
                    query = state.query,
                    onQueryChange = { onIntent(HomeIntent.SearchChanged(it)) },
                    placeholder = stringResource(R.string.home_search_placeholder)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item("all") {
                    CategoryChip(
                        label = stringResource(R.string.home_category_all),
                        isSelected = state.activeTags.isEmpty() || "all" in state.activeTags,
                        onClick = { onIntent(HomeIntent.FilterTagToggled("all")) }
                    )
                }
                items(TagCatalog.all, key = { it.id }) { tag ->
                    CategoryChip(
                        label = stringResource(tag.labelRes),
                        isSelected = tag.id in state.activeTags,
                        onClick = { onIntent(HomeIntent.FilterTagToggled(tag.id)) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 29.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                when {
                    state.isLoading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = DesignTokens.colorMaroon)
                        }
                    }
                    state.error != null -> {
                        Text(
                            text = state.error,
                            style = TextStyle(
                                fontFamily = inter,
                                fontSize = 14.sp,
                                color = Color.Black
                            )
                        )
                    }
                    state.cards.isEmpty() && state.query.isNotBlank() -> {
                        Text(
                            text = stringResource(R.string.home_no_results, state.query),
                            style = TextStyle(
                                fontFamily = inter,
                                fontSize = 14.sp,
                                color = Color.Black
                            )
                        )
                    }
                    else -> {
                        state.cards.forEach { card ->
                            RouteCardItem(
                                card = card,
                                onClick = { onIntent(HomeIntent.CardClicked(card.id)) }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(80.dp))
            }
        }

        BottomNavigationBar(
            modifier = Modifier.align(Alignment.BottomCenter),
            isChinese = isChinese,
            onHomeClick = {},
            onSavedClick = onNavigateToSaved,
            onMemoriesClick = onNavigateToMemories,
            currentDestination = "home"
        )
    }
}

@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .background(Color(0xFFD7D7D7), RoundedCornerShape(50.dp))
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            UntitledIcon(
                imageVector = UntitledIcons.SearchIcon,
                contentDescription = null,
                tint = Color(0xFF7B7B7B),
                size = 24.dp
            )
            Spacer(modifier = Modifier.width(10.dp))

            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                textStyle = TextStyle(
                    fontFamily = inter,
                    fontSize = 16.sp,
                    color = Color.Black
                ),
                cursorBrush = SolidColor(Color.Black),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { innerTextField ->
                    if (query.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = TextStyle(
                                fontFamily = inter,
                                fontSize = 16.sp,
                                color = Color(0xFF7B7B7B)
                            )
                        )
                    }
                    innerTextField()
                }
            )
        }
    }
}

@Composable
fun CategoryChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(50.dp)
    Box(
        modifier = Modifier
            .height(30.dp)
            .background(
                color = if (isSelected) DesignTokens.colorMaroon else Color.Transparent,
                shape = shape
            )
            .border(
                width = if (isSelected) 0.dp else 1.dp,
                color = if (isSelected) Color.Transparent else DesignTokens.colorBorderGray,
                shape = shape
            )
            .clip(shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = TextStyle(
                fontFamily = inter,
                fontSize = 12.sp,
                color = if (isSelected) Color.White else Color.Black
            )
        )
    }
}

@Preview(showBackground = true, device = "spec:width=412dp,height=915dp")
@Composable
fun HomeScreenPreview() {
    AppTheme {
        HomeContent(
            state = HomeState(
                cards = listOf(
                    RouteCard(
                        id = "demo",
                        title = "Demo Route",
                        category = "Demo",
                        heroDrawableRes = R.drawable.sounds_of_temple,
                        description = "A short route preview."
                    )
                ),
                isLoading = false
            ),
            onIntent = {}
        )
    }
}
