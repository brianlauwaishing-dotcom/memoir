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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.mcis.memoir.ui.culture.CultureInterestViewModel
import com.mcis.memoir.ui.home.Tag
import com.mcis.memoir.ui.home.TagCatalog
import com.mcis.memoir.ui.theme.AppTheme
import com.mcis.memoir.ui.theme.inter
import com.mcis.memoir.ui.theme.judson

@Composable
fun CultureInterestScreen(
    viewModel: CultureInterestViewModel,
    onStartExploringClick: () -> Unit,
    onSkipClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selected by viewModel.selected.collectAsStateWithLifecycle()

    CultureInterestContent(
        selected = selected,
        onToggle = viewModel::toggle,
        onSkip = {
            viewModel.skip()
            onSkipClick()
        },
        onStart = onStartExploringClick,
        modifier = modifier
    )
}

@Composable
private fun CultureInterestContent(
    selected: Set<String>,
    onToggle: (String) -> Unit,
    onSkip: () -> Unit,
    onStart: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DesignTokens.colorLanguageSelectionBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 29.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                SkipButton(onClick = onSkip)
            }

            Spacer(modifier = Modifier.height(120.dp))

            CultureSelectionContent()

            Spacer(modifier = Modifier.height(32.dp))

            InterestOptionsList(
                interests = TagCatalog.all,
                selectedInterests = selected,
                onInterestToggle = onToggle
            )

            Spacer(modifier = Modifier.height(32.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 80.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                StartExploringButton(onClick = onStart)
            }
        }
    }
}

@Composable
private fun CultureSelectionContent(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
    ) {
        Text(
            text = stringResource(R.string.culture_interest_subtitle),
            color = DesignTokens.colorBlack,
            style = TextStyle(
                fontSize = DesignTokens.fontSizeMedium,
                fontWeight = FontWeight.Normal,
                fontFamily = inter
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.culture_interest_headline),
            color = DesignTokens.colorBlack,
            style = TextStyle(
                fontSize = DesignTokens.fontSizeHeadline,
                fontWeight = FontWeight.Bold,
                fontFamily = judson,
                lineHeight = 68.sp
            )
        )
    }
}

@Composable
private fun InterestOptionsList(
    interests: List<Tag>,
    selectedInterests: Set<String>,
    onInterestToggle: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        interests.forEach { tag ->
            InterestOptionButton(
                label = stringResource(tag.labelRes),
                isSelected = tag.id in selectedInterests,
                onClick = { onInterestToggle(tag.id) }
            )
        }
    }
}

@Composable
private fun InterestOptionButton(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(DesignTokens.cornerRadiusLarge)
    Box(
        modifier = modifier
            .height(48.dp)
            .fillMaxWidth()
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
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isSelected) DesignTokens.colorWhite else DesignTokens.colorBlack,
            style = TextStyle(
                fontSize = 18.sp,
                fontWeight = FontWeight.Normal,
                fontFamily = inter
            )
        )
    }
}

@Composable
private fun SkipButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp)
    ) {
        Text(
            text = stringResource(R.string.skip_button),
            style = TextStyle(
                fontSize = 18.sp,
                fontWeight = FontWeight.Normal,
                fontFamily = inter,
                color = Color(0xFFA8A8A8)
            )
        )

        Image(
            painter = painterResource(R.drawable.arrow_right),
            contentDescription = stringResource(R.string.arrow_right_content_description),
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(14.dp, 15.dp),
            colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(Color(0xFFA8A8A8))
        )
    }
}

@Composable
private fun StartExploringButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(DesignTokens.cornerRadiusLarge)
    Box(
        modifier = modifier
            .height(66.dp)
            .background(
                color = DesignTokens.colorMaroon,
                shape = shape
            )
            .clip(shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 34.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.start_exploring_button),
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

@Preview(
    name = "Medium Phone API 36.1",
    showBackground = true,
    device = "spec:width=411dp,height=914dp"
)
@Preview(showBackground = true)
@Composable
fun CultureInterestScreenPreview() {
    AppTheme {
        CultureInterestContent(
            selected = setOf("temples", "crafts"),
            onToggle = {},
            onSkip = {},
            onStart = {}
        )
    }
}
