package com.mcis.memoir

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mcis.memoir.ui.components.BottomNavigationBar
import com.mcis.memoir.ui.components.UntitledIcon
import com.mcis.memoir.ui.icons.*
import com.mcis.memoir.ui.memory.reflection.ReflectionEffect
import com.mcis.memoir.ui.memory.reflection.ReflectionIntent
import com.mcis.memoir.ui.memory.reflection.ReflectionViewModel
import com.mcis.memoir.ui.theme.inter

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

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                ReflectionEffect.NavigateToMemoriesList -> onNavigateToMemoriesList()
                is ReflectionEffect.ShowError -> Unit
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

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .shadow(12.dp, RoundedCornerShape(15.dp))
                        .background(Color(0xFFFCE4D9), RoundedCornerShape(15.dp))
                        .clickable { Log.w("memory-creation-flow", "AI polish coming in change #8") }
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
                }

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(83.dp)
                        .shadow(12.dp, RoundedCornerShape(15.dp))
                        .background(DesignTokens.colorLanguageSelectionBackground, RoundedCornerShape(15.dp))
                        .clickable { Log.w("memory-creation-flow", "Add to landmarks deferred") }
                        .padding(horizontal = 20.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        UntitledIcon(UntitledIcons.LandmarkIcon, null, size = 44.dp, tint = Color.Black)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = stringResource(R.string.memory_reflection_add_landmarks),
                            style = TextStyle(
                                fontFamily = inter,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .border(2.dp, Color.Gray, RoundedCornerShape(4.dp))
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
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
