package com.mcis.memoir

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Log
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mcis.memoir.ui.components.BottomNavigationBar
import com.mcis.memoir.ui.components.UntitledIcon
import com.mcis.memoir.ui.icons.*
import com.mcis.memoir.ui.memory.reflection.AiState
import com.mcis.memoir.ui.memory.reflection.ReflectionEffect
import com.mcis.memoir.ui.memory.reflection.ReflectionIntent
import com.mcis.memoir.ui.memory.reflection.ReflectionViewModel
import com.mcis.memoir.ui.theme.inter
import kotlinx.coroutines.delay

@Composable
fun MemoryReflectionScreen(
    viewModel: ReflectionViewModel,
    onBackClick: () -> Unit = {},
    onNavigateToMemoriesList: () -> Unit = {},
    onNavigateToHome: () -> Unit = {},
    onNavigateToSaved: () -> Unit = {},
    onNavigateToMemories: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isChinese = LocalConfiguration.current.locales[0].language == "zh"
    val context = LocalContext.current

    var copiedAt by remember { mutableLongStateOf(0L) }
    var showCopied by remember { mutableStateOf(false) }
    LaunchedEffect(copiedAt) {
        if (copiedAt > 0L) {
            showCopied = true
            delay(1500)
            showCopied = false
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                ReflectionEffect.NavigateToMemoriesList -> onNavigateToMemoriesList()
                is ReflectionEffect.ShowError -> Unit
                is ReflectionEffect.CopyToClipboard -> {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("memoir-reflection", effect.text))
                    copiedAt = System.currentTimeMillis()
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DesignTokens.colorLanguageSelectionBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 54.dp, bottom = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.memory_reflection_headline),
                    style = TextStyle(
                        fontFamily = inter,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = DesignTokens.colorMaroon
                    )
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 17.dp)
                        .size(24.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { onBackClick() },
                    contentAlignment = Alignment.Center
                ) {
                    UntitledIcon(
                        imageVector = UntitledIcons.BackIcon,
                        contentDescription = stringResource(R.string.back_button),
                        size = 24.dp,
                        tint = DesignTokens.colorMaroon
                    )
                }

                Text(
                    text = stringResource(R.string.save_button),
                    style = TextStyle(
                        fontFamily = inter,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = DesignTokens.colorMaroon
                    ),
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 17.dp)
                        .clickable { viewModel.onIntent(ReflectionIntent.SaveClicked) }
                )
            }

            Column(
                modifier = Modifier
                    .padding(horizontal = 35.dp)
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                ReflectionField(
                    label = stringResource(R.string.memory_reflection_mood_label),
                    value = state.overallMood,
                    onValueChange = { viewModel.onIntent(ReflectionIntent.MoodChanged(it)) },
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(16.dp))
                ReflectionField(
                    label = stringResource(R.string.memory_reflection_insights_label),
                    value = state.userInsights,
                    onValueChange = { viewModel.onIntent(ReflectionIntent.InsightsChanged(it)) }
                )
                Spacer(modifier = Modifier.height(16.dp))
                ReflectionField(
                    label = stringResource(R.string.memory_reflection_feedback_label),
                    value = state.postTripFeedback,
                    onValueChange = { viewModel.onIntent(ReflectionIntent.FeedbackChanged(it)) }
                )

                Spacer(modifier = Modifier.height(24.dp))

                AiReflectionSection(
                    aiState = state.aiState,
                    showCopied = showCopied,
                    onPolish = { viewModel.onIntent(ReflectionIntent.PolishClicked) },
                    onRegenerate = { viewModel.onIntent(ReflectionIntent.RegenerateClicked) },
                    onCopy = { viewModel.onIntent(ReflectionIntent.CopyClicked) },
                    onDismissError = { viewModel.onIntent(ReflectionIntent.DismissAiError) }
                )

                Spacer(modifier = Modifier.height(16.dp))


            }

            BottomNavigationBar(
                isChinese = isChinese,
                onHomeClick = onNavigateToHome,
                onSavedClick = onNavigateToSaved,
                onMemoriesClick = onNavigateToMemories,
                currentDestination = "memories"
            )
        }
    }
}

@Composable
private fun ReflectionField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    singleLine: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier
            .fillMaxWidth()
            .height(if (singleLine) 76.dp else 148.dp),
        singleLine = singleLine,
        textStyle = TextStyle(fontFamily = inter, fontSize = 14.sp, color = Color.Black)
    )
}

@Composable
private fun AiReflectionSection(
    aiState: AiState,
    showCopied: Boolean,
    onPolish: () -> Unit,
    onRegenerate: () -> Unit,
    onCopy: () -> Unit,
    onDismissError: () -> Unit
) {
    when (aiState) {
        is AiState.Idle -> PolishButton(enabled = true, showSpinner = false, onClick = onPolish)
        is AiState.Generating -> PolishButton(enabled = false, showSpinner = true, onClick = {})
        is AiState.Ready -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(12.dp, RoundedCornerShape(15.dp))
                    .background(Color.White, RoundedCornerShape(15.dp))
                    .padding(16.dp)
            ) {
                Text(
                    text = aiState.text,
                    style = TextStyle(fontFamily = inter, fontSize = 15.sp, color = Color.Black)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .background(Color(0xFFFCE4D9), RoundedCornerShape(12.dp))
                        .clickable { onCopy() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.memory_reflection_copy),
                        style = TextStyle(fontFamily = inter, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .background(DesignTokens.colorMaroon, RoundedCornerShape(12.dp))
                        .clickable { onRegenerate() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.memory_reflection_regenerate),
                        style = TextStyle(
                            fontFamily = inter,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }
            }
            if (showCopied) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.memory_reflection_copied),
                    style = TextStyle(fontFamily = inter, fontSize = 13.sp, color = DesignTokens.colorMaroon)
                )
            }
        }
        is AiState.Error -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFDECEA), RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = aiState.message,
                    modifier = Modifier.weight(1f),
                    style = TextStyle(fontFamily = inter, fontSize = 14.sp, color = Color(0xFFB00020))
                )
                Text(
                    text = stringResource(R.string.memory_reflection_regenerate),
                    modifier = Modifier
                        .clickable { onPolish() }
                        .padding(horizontal = 8.dp),
                    style = TextStyle(
                        fontFamily = inter,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = DesignTokens.colorMaroon
                    )
                )
                Text(
                    text = "✕",
                    modifier = Modifier
                        .clickable { onDismissError() }
                        .padding(start = 8.dp),
                    style = TextStyle(fontFamily = inter, fontSize = 14.sp, color = Color.Gray)
                )
            }
        }
    }
}

@Composable
private fun PolishButton(
    enabled: Boolean,
    showSpinner: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .shadow(12.dp, RoundedCornerShape(15.dp))
            .background(Color(0xFFFCE4D9), RoundedCornerShape(15.dp))
            .then(if (enabled) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = stringResource(R.string.memory_reflection_polish_ai),
            style = TextStyle(
                fontFamily = inter,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        )
        if (showSpinner) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(24.dp),
                color = DesignTokens.colorMaroon,
                strokeWidth = 2.dp
            )
        }
    }
}
