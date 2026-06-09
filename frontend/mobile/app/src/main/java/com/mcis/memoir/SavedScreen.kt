package com.mcis.memoir

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mcis.memoir.i18n.LocaleController
import com.mcis.memoir.ui.components.BottomNavigationBar
import com.mcis.memoir.ui.home.RouteCard
import com.mcis.memoir.ui.home.RouteCardItem
import com.mcis.memoir.ui.saved.SavedState
import com.mcis.memoir.ui.saved.SavedViewModel
import com.mcis.memoir.ui.theme.AppTheme
import com.mcis.memoir.ui.theme.judson

@Composable
fun SavedScreen(
    viewModel: SavedViewModel,
    onNavigateToHome: () -> Unit,
    onNavigateToMemories: () -> Unit,
    onMoreClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    SavedContent(
        state = state,
        onMoreClick = onMoreClick,
        onNavigateToHome = onNavigateToHome,
        onNavigateToMemories = onNavigateToMemories,
        modifier = modifier
    )
}

@Composable
internal fun SavedContent(
    state: SavedState,
    onMoreClick: (String) -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToMemories: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isChinese = LocaleController.currentLocale().language == "zh"

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DesignTokens.colorLanguageSelectionBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 29.dp)
                    .padding(top = 62.dp)
            ) {
                Text(
                    text = stringResource(R.string.saved_headline),
                    style = TextStyle(
                        fontFamily = judson,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color.Black
                    )
                )
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
                    state.error != null -> {
                        CenterMessage(text = state.error)
                    }
                    state.isLoading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 100.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = DesignTokens.colorMaroon)
                        }
                    }
                    state.cards.isEmpty() -> {
                        CenterMessage(text = stringResource(R.string.saved_empty_message))
                    }
                    else -> {
                        state.cards.forEach { card ->
                            RouteCardItem(
                                card = card,
                                onClick = { onMoreClick(card.id) }
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
            onHomeClick = onNavigateToHome,
            onMemoriesClick = onNavigateToMemories,
            currentDestination = "saved"
        )
    }
}

@Composable
private fun CenterMessage(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 100.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = TextStyle(
                fontSize = 16.sp,
                color = Color.Gray
            )
        )
    }
}

@Preview(showBackground = true, device = "spec:width=412dp,height=915dp")
@Composable
fun SavedScreenPreview() {
    AppTheme {
        SavedContent(
            state = SavedState(
                isLoading = false,
                cards = listOf(
                    RouteCard(
                        id = "demo",
                        title = "Demo",
                        category = "Demo",
                        heroDrawableRes = R.drawable.sounds_of_temple,
                        description = "..."
                    )
                )
            ),
            onMoreClick = {},
            onNavigateToHome = {},
            onNavigateToMemories = {}
        )
    }
}
