package com.mcis.memoir

import android.util.Log
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mcis.memoir.ui.language.LanguageEffect
import com.mcis.memoir.ui.language.LanguageIntent
import com.mcis.memoir.ui.language.LanguageSelectionViewModel
import com.mcis.memoir.ui.language.LanguageState
import com.mcis.memoir.ui.theme.AppTheme
import com.mcis.memoir.ui.theme.inter
import com.mcis.memoir.ui.theme.judson

@Composable
fun LanguageSelectionScreen(
    onNavigateNext: () -> Unit,
    viewModel: LanguageSelectionViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                LanguageEffect.NavigateNext -> onNavigateNext()
                is LanguageEffect.ShowError -> Log.w("LanguageSelection", effect.msg)
            }
        }
    }

    LanguageSelectionContent(
        state = state,
        onSelect = { tag -> viewModel.onIntent(LanguageIntent.Select(tag)) },
        onConfirm = { viewModel.onIntent(LanguageIntent.Confirm) },
        modifier = modifier
    )
}

@Composable
fun LanguageSelectionContent(
    state: LanguageState,
    onSelect: (String) -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier
) {
    val canConfirm = state.selected != null && !state.applying

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DesignTokens.colorLanguageSelectionBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 29.dp)
        ) {
            Spacer(modifier = Modifier.height(120.dp))

            LanguageSelectionCopy()

            Spacer(modifier = Modifier.height(32.dp))

            LanguageOptionButtons(
                selectedLanguage = state.selected,
                onLanguageSelect = onSelect
            )

            Spacer(modifier = Modifier.weight(1f))

            NextButton(
                enabled = canConfirm,
                onClick = onConfirm,
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(bottom = 80.dp)
            )
        }
    }
}

@Composable
private fun LanguageSelectionCopy(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
    ) {
        Text(
            text = stringResource(R.string.language_selection_subtitle),
            color = DesignTokens.colorBlack,
            style = TextStyle(
                fontSize = DesignTokens.fontSizeMedium,
                fontWeight = FontWeight.Normal,
                fontFamily = inter
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.language_selection_headline),
            color = DesignTokens.colorBlack,
            style = TextStyle(
                fontSize = DesignTokens.fontSizeHeadline,
                fontWeight = FontWeight.Bold,
                fontFamily = judson,
                lineHeight = 68.sp
            )
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = stringResource(R.string.language_selection_description),
            color = DesignTokens.colorBlack,
            style = TextStyle(
                fontSize = DesignTokens.fontSizeBody,
                fontWeight = FontWeight.Normal,
                fontFamily = inter
            )
        )
    }
}

@Composable
private fun LanguageOptionButtons(
    selectedLanguage: String?,
    onLanguageSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        LanguageOptionButton(
            label = stringResource(R.string.language_english),
            isSelected = selectedLanguage == "en",
            onClick = { onLanguageSelect("en") },
            modifier = Modifier.weight(1f)
        )

        LanguageOptionButton(
            label = stringResource(R.string.language_chinese),
            isSelected = selectedLanguage == "zh",
            onClick = { onLanguageSelect("zh") },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun LanguageOptionButton(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(DesignTokens.cornerRadiusLarge)

    Box(
        modifier = modifier
            .height(56.dp)
            .background(
                color = if (isSelected) DesignTokens.colorMaroon else DesignTokens.colorWhite,
                shape = shape
            )
            .then(
                if (!isSelected) {
                    Modifier.border(
                        width = 1.dp,
                        color = DesignTokens.colorBorderGray,
                        shape = shape
                    )
                } else {
                    Modifier
                }
            )
            .clip(shape)
            .semantics { selected = isSelected }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isSelected) DesignTokens.colorWhite else DesignTokens.colorBlack,
            style = TextStyle(
                fontSize = DesignTokens.fontSizeMedium,
                fontWeight = FontWeight.Normal,
                fontFamily = inter
            )
        )
    }
}

@Composable
private fun NextButton(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(DesignTokens.cornerRadiusLarge)
    val backgroundColor = if (enabled) DesignTokens.colorMaroon else DesignTokens.colorBorderGray

    Box(
        modifier = modifier
            .height(66.dp)
            .background(
                color = backgroundColor,
                shape = shape
            )
            .clip(shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 34.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.next_button),
                style = TextStyle(
                    fontSize = DesignTokens.fontSizeMedium,
                    fontWeight = FontWeight.Normal,
                    fontFamily = inter,
                    color = DesignTokens.colorWhite
                )
            )

            Image(
                painter = painterResource(R.drawable.arrow_right),
                contentDescription = stringResource(R.string.arrow_right_content_description),
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LanguageSelectionScreenPreview() {
    AppTheme {
        LanguageSelectionContent(
            state = LanguageState(selected = "en"),
            onSelect = {},
            onConfirm = {}
        )
    }
}
