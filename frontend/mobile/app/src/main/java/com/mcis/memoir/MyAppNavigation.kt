package com.mcis.memoir

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import androidx.activity.compose.BackHandler
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mcis.memoir.i18n.LocaleController
import com.mcis.memoir.ui.culture.CultureInterestViewModel
import com.mcis.memoir.ui.culture.CultureInterestViewModelFactory
import com.mcis.memoir.ui.home.HomeEffect
import com.mcis.memoir.ui.home.HomeViewModel
import com.mcis.memoir.ui.home.HomeViewModelFactory
import com.mcis.memoir.ui.language.LanguageSelectionViewModel
import com.mcis.memoir.ui.artifact.ArtifactDetailViewModel
import com.mcis.memoir.ui.artifact.ArtifactDetailViewModelFactory
import com.mcis.memoir.ui.artifact.ArtifactDiscoveryViewModel
import com.mcis.memoir.ui.artifact.ArtifactDiscoveryViewModelFactory
import com.mcis.memoir.ui.route.RouteDetailViewModel
import com.mcis.memoir.ui.route.RouteDetailViewModelFactory
import com.mcis.memoir.ui.saved.SavedViewModel
import com.mcis.memoir.ui.saved.SavedViewModelFactory
import com.mcis.memoir.ui.spot.SpotIntroViewModel
import com.mcis.memoir.ui.spot.SpotIntroViewModelFactory
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@Composable
fun MyAppNavigation() {
    val prefsRepo = remember { MemoirApplication.prefs }
    val coroutineScope = rememberCoroutineScope()

    val selectedLanguage by prefsRepo.language.collectAsStateWithLifecycle(initialValue = "en")
    val onboardingCompleted by prefsRepo.onboardingDone
        .map { it as Boolean? }
        .collectAsStateWithLifecycle(initialValue = null)

    // Flow State: Memory Creation
    var selectedPhotos by rememberSaveable { mutableStateOf(listOf<Int>()) }

    // Decide starting destination
    if (onboardingCompleted == null) {
        SplashScreen()
        return
    }
    val initialDestination = if (onboardingCompleted == true) HomeDestination else WelcomeDestination
    val backStack = rememberSaveable(
        onboardingCompleted,
        saver = destinationStackSaver
    ) {
        mutableStateListOf<Any>(initialDestination)
    }

    BackHandler(enabled = backStack.size > 1) {
        backStack.removeLastOrNull()
    }
    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        transitionSpec = {
            slideInHorizontally(
                initialOffsetX = { fullWidth -> fullWidth },
                animationSpec = tween(400)
            ) togetherWith ExitTransition.None
        },
        popTransitionSpec = {
            EnterTransition.None
                .togetherWith (
                    slideOutHorizontally(
                        targetOffsetX = { fullWidth -> fullWidth },
                        animationSpec = tween(400)
                    )
                )
        },
        entryProvider = { key ->
            when (key) {
                is WelcomeDestination -> NavEntry(key) {
                    WelcomeScreen(
                        selectedLanguage = selectedLanguage,
                        onBeginTourClick = {
                            backStack.add(LanguageSelectionDestination)
                        }
                    )
                }
                is LanguageSelectionDestination -> NavEntry(key) {
                    val vm: LanguageSelectionViewModel = viewModel {
                        LanguageSelectionViewModel(prefsRepo)
                    }
                    LanguageSelectionScreen(
                        viewModel = vm,
                        onNavigateNext = {
                            backStack.add(CultureInterestDestination)
                        }
                    )
                }
                is CultureInterestDestination -> NavEntry(key) {
                    val vm: CultureInterestViewModel = viewModel(
                        factory = CultureInterestViewModelFactory(MemoirApplication.prefs)
                    )
                    CultureInterestScreen(
                        viewModel = vm,
                        onStartExploringClick = {
                            coroutineScope.launch {
                                prefsRepo.markOnboardingDone()
                            }
                            backStack.add(HomeDestination)
                        },
                        onSkipClick = {
                            coroutineScope.launch {
                                prefsRepo.markOnboardingDone()
                            }
                            backStack.add(HomeDestination)
                        }
                    )
                }
                is HomeDestination -> NavEntry(key) {
                    val ctx = LocalContext.current
                    val currentLocale = LocaleController.currentLocale()
                    val vm: HomeViewModel = viewModel(
                        factory = HomeViewModelFactory(
                            content = MemoirApplication.content,
                            prefs = MemoirApplication.prefs,
                            resources = ctx.resources,
                            localeProvider = { currentLocale }
                        )
                    )
                    HomeScreen(
                        viewModel = vm,
                        onNavigateToSaved = {
                            backStack.add(SavedDestination)
                        },
                        onNavigateToMemories = {
                            backStack.add(MemoriesDestination)
                        }
                    )
                    LaunchedEffect(vm) {
                        vm.effects.collect { effect ->
                            when (effect) {
                                is HomeEffect.NavigateToRoute -> backStack.add(RouteDetailDestination(effect.routeId))
                            }
                        }
                    }
                }
                is SavedDestination -> NavEntry(key) {
                    val ctx = LocalContext.current
                    val currentLocale = LocaleController.currentLocale()
                    val vm: SavedViewModel = viewModel(
                        factory = SavedViewModelFactory(
                            content = MemoirApplication.content,
                            prefs = MemoirApplication.prefs,
                            resources = ctx.resources,
                            localeProvider = { currentLocale }
                        )
                    )
                    SavedScreen(
                        viewModel = vm,
                        onNavigateToHome = {
                            backStack.clear()
                            backStack.add(HomeDestination)
                        },
                        onNavigateToMemories = {
                            backStack.add(MemoriesDestination)
                        },
                        onMoreClick = { routeId ->
                            backStack.add(RouteDetailDestination(routeId))
                        }
                    )
                }
                is MemoriesDestination -> NavEntry(key) {
                    MemoriesScreen(
                        selectedLanguage = selectedLanguage,
                        onNavigateToHome = {
                            backStack.clear()
                            backStack.add(HomeDestination)
                        },
                        onNavigateToSaved = {
                            backStack.add(SavedDestination)
                        },
                        onNavigateToMemories = {
                            if (backStack.last() != MemoriesDestination) {
                                backStack.add(MemoriesDestination)
                            }
                        },
                        onCreateMemoryClick = {
                            selectedPhotos = emptyList() // Reset for new creation
                            backStack.add(MemoryTemplateDestination)
                        }
                    )
                }
                is MemoryTemplateDestination -> NavEntry(key) {
                    MemoryTemplateScreen(
                        selectedLanguage = selectedLanguage,
                        onBackClick = {
                            backStack.removeLastOrNull()
                        },
                        onNavigateToHome = {
                            backStack.clear()
                            backStack.add(HomeDestination)
                        },
                        onNavigateToSaved = {
                            backStack.add(SavedDestination)
                        },
                        onNavigateToMemories = {
                            while (backStack.lastOrNull() != MemoriesDestination && backStack.size > 1) {
                                backStack.removeLastOrNull()
                            }
                            if (backStack.isEmpty()) {
                                backStack.add(MemoriesDestination)
                            }
                        },
                        onTemplateSelect = { templateId ->
                            selectedPhotos = emptyList() // Clear selection when choosing a new template
                            backStack.add(MemoryPhotoSelectionDestination(templateId))
                        }
                    )
                }
                is MemoryPhotoSelectionDestination -> NavEntry(key) {
                    MemoryPhotoSelectionScreen(
                        selectedLanguage = selectedLanguage,
                        templateId = key.templateId,
                        initialPhotos = selectedPhotos,
                        onPhotosChange = { selectedPhotos = it },
                        onBackClick = {
                            backStack.removeLastOrNull()
                        },
                        onNextClick = { photos ->
                            selectedPhotos = photos
                            backStack.add(MemoryEditDestination(key.templateId, photos))
                        },
                        onNavigateToHome = {
                            backStack.clear()
                            backStack.add(HomeDestination)
                        },
                        onNavigateToSaved = {
                            backStack.add(SavedDestination)
                        },
                        onNavigateToMemories = {
                            while (backStack.lastOrNull() != MemoriesDestination && backStack.size > 1) {
                                backStack.removeLastOrNull()
                            }
                            if (backStack.isEmpty()) {
                                backStack.add(MemoriesDestination)
                            }
                        }
                    )
                }
                is MemoryEditDestination -> NavEntry(key) {
                    MemoryEditScreen(
                        selectedLanguage = selectedLanguage,
                        templateId = key.templateId,
                        photoResIds = selectedPhotos,
                        onBackClick = {
                            backStack.removeLastOrNull()
                        },
                        onSaveClick = {
                            backStack.add(MemoryReflectionDestination)
                        },
                        onNavigateToHome = {
                            backStack.clear()
                            backStack.add(HomeDestination)
                        },
                        onNavigateToSaved = {
                            backStack.add(SavedDestination)
                        },
                        onNavigateToMemories = {
                            while (backStack.lastOrNull() != MemoriesDestination && backStack.size > 1) {
                                backStack.removeLastOrNull()
                            }
                            if (backStack.isEmpty()) {
                                backStack.add(MemoriesDestination)
                            }
                        }
                    )
                }
                is MemoryReflectionDestination -> NavEntry(key) {
                    MemoryReflectionScreen(
                        selectedLanguage = selectedLanguage,
                        onBackClick = {
                            backStack.removeLastOrNull()
                        },
                        onNextClick = {
                            // Clear backstack to Memories
                            while (backStack.lastOrNull() != MemoriesDestination && backStack.size > 1) {
                                backStack.removeLastOrNull()
                            }
                            if (backStack.isEmpty()) {
                                backStack.add(MemoriesDestination)
                            }
                        },
                        onNavigateToHome = {
                            backStack.clear()
                            backStack.add(HomeDestination)
                        },
                        onNavigateToSaved = {
                            backStack.add(SavedDestination)
                        },
                        onNavigateToMemories = {
                            while (backStack.lastOrNull() != MemoriesDestination && backStack.size > 1) {
                                backStack.removeLastOrNull()
                            }
                            if (backStack.isEmpty()) {
                                backStack.add(MemoriesDestination)
                            }
                        }
                    )
                }
                is RouteDetailDestination -> NavEntry(key) {
                    val ctx = LocalContext.current
                    val currentLocale = LocaleController.currentLocale()
                    val vm: RouteDetailViewModel = viewModel(
                        key = key.routeId,
                        factory = RouteDetailViewModelFactory(
                            routeId = key.routeId,
                            content = MemoirApplication.content,
                            prefs = MemoirApplication.prefs,
                            resources = ctx.resources,
                            localeProvider = { currentLocale }
                        )
                    )
                    RouteDetailScreen(
                        viewModel = vm,
                        onBackClick = {
                            backStack.removeLastOrNull()
                        },
                        onNavigateToSaved = {
                            backStack.add(SavedDestination)
                        },
                        onNavigateToMemories = {
                            backStack.add(MemoriesDestination)
                        },
                        onSpotClick = { spotId ->
                            backStack.add(SpotIntroDestination(spotId))
                        }
                    )
                }
                is SpotIntroDestination -> NavEntry(key) {
                    val ctx = LocalContext.current
                    val currentLocale = LocaleController.currentLocale()
                    val vm: SpotIntroViewModel = viewModel(
                        key = key.spotId,
                        factory = SpotIntroViewModelFactory(
                            spotId = key.spotId,
                            content = MemoirApplication.content,
                            resources = ctx.resources,
                            localeProvider = { currentLocale }
                        )
                    )
                    SpotIntroScreen(
                        viewModel = vm,
                        onBackClick = {
                            backStack.removeLastOrNull()
                        },
                        onInfoClick = { spotId ->
                            backStack.add(SpotDetailDestination(spotId))
                        },
                        onDiscoveryItemClick = { artifactId ->
                            backStack.add(ArtifactDiscoveryDestination(key.spotId, artifactId))
                        }
                    )
                }
                is ArtifactDiscoveryDestination -> NavEntry(key) {
                    val ctx = LocalContext.current
                    val currentLocale = LocaleController.currentLocale()
                    val vm: ArtifactDiscoveryViewModel = viewModel(
                        key = "${key.spotId}/${key.artifactId}",
                        factory = ArtifactDiscoveryViewModelFactory(
                            spotId = key.spotId,
                            artifactId = key.artifactId,
                            content = MemoirApplication.content,
                            resources = ctx.resources,
                            localeProvider = { currentLocale }
                        )
                    )
                    ArtifactDiscoveryScreen(
                        viewModel = vm,
                        onBackClick = {
                            // Ensure we go back to SpotIntro, not just previous screen
                            while (backStack.lastOrNull() != SpotIntroDestination(key.spotId) && backStack.size > 1) {
                                backStack.removeLastOrNull()
                            }
                            if (backStack.isEmpty()) {
                                backStack.add(SpotIntroDestination(key.spotId))
                            }
                        },
                        onInfoClick = { spotId ->
                            backStack.add(SpotDetailDestination(spotId))
                        },
                        onMoreClick = { spotId, artifactId ->
                            backStack.add(ArtifactDetailDestination(spotId, artifactId))
                        },
                        onCameraClick = {
                            backStack.add(CameraPreviewDestination)
                        }
                    )
                }
                is ArtifactDetailDestination -> NavEntry(key) {
                    val ctx = LocalContext.current
                    val currentLocale = LocaleController.currentLocale()
                    val vm: ArtifactDetailViewModel = viewModel(
                        key = "${key.spotId}/${key.artifactId}",
                        factory = ArtifactDetailViewModelFactory(
                            spotId = key.spotId,
                            artifactId = key.artifactId,
                            content = MemoirApplication.content,
                            resources = ctx.resources,
                            localeProvider = { currentLocale }
                        )
                    )
                    ArtifactDetailScreen(
                        viewModel = vm,
                        onBackClick = {
                            // Ensure we go back to SpotIntro, not just previous screen
                            while (backStack.lastOrNull() != SpotIntroDestination(key.spotId) && backStack.size > 1) {
                                backStack.removeLastOrNull()
                            }
                            if (backStack.isEmpty()) {
                                backStack.add(SpotIntroDestination(key.spotId))
                            }
                        },
                        onInfoClick = {
                            backStack.add(SpotDetailDestination(key.spotId))
                        },
                        onCameraClick = {
                            backStack.add(CameraPreviewDestination)
                        },
                        onNavigateToHome = {
                            backStack.clear()
                            backStack.add(HomeDestination)
                        },
                        onNavigateToSaved = {
                            backStack.add(SavedDestination)
                        },
                        onNavigateToMemories = {
                            backStack.add(MemoriesDestination)
                        }
                    )
                }
                is CameraPreviewDestination -> NavEntry(key) {
                    CameraPreviewScreen(
                        onBackClick = {
                            backStack.removeLastOrNull()
                        },
                        onCaptureClick = {
                            backStack.removeLastOrNull()
                        }
                    )
                }
                is SpotDetailDestination -> NavEntry(key) {
                    SpotDetailScreen(
                        selectedLanguage = selectedLanguage,
                        spotId = key.spotId,
                        onBackClick = {
                            backStack.removeLastOrNull()
                        },
                        onNavigateToHome = {
                            backStack.clear()
                            backStack.add(HomeDestination)
                        },
                        onNavigateToSaved = {
                            backStack.add(SavedDestination)
                        },
                        onNavigateToMemories = {
                            backStack.add(MemoriesDestination)
                        },
                        onDiscoveryItemClick = { artifactId ->
                            backStack.add(ArtifactDiscoveryDestination(key.spotId, artifactId))
                        }
                    )
                }
                else -> NavEntry(Unit) { Text("Unknown destination") }
            }
        }
    )

}

private val destinationStackSaver = Saver<SnapshotStateList<Any>, List<String>>(
    save = { stack -> stack.map(::destinationToToken) },
    restore = { tokens ->
        mutableStateListOf<Any>().apply {
            addAll(tokens.map(::destinationFromToken))
        }
    }
)

private fun destinationToToken(destination: Any): String = when (destination) {
    WelcomeDestination -> "welcome"
    LanguageSelectionDestination -> "language"
    CultureInterestDestination -> "culture"
    HomeDestination -> "home"
    SavedDestination -> "saved"
    MemoriesDestination -> "memories"
    MemoryTemplateDestination -> "memory-template"
    MemoryReflectionDestination -> "memory-reflection"
    CameraPreviewDestination -> "camera-preview"
    is MemoryPhotoSelectionDestination -> "memory-photo:${destination.templateId}"
    is MemoryEditDestination -> "memory-edit:${destination.templateId}:${destination.photoResIds.joinToString(",")}"
    is RouteDetailDestination -> "route-detail:${destination.routeId}"
    is SpotDetailDestination -> "spot-detail:${destination.spotId}"
    is SpotIntroDestination -> "spot-intro:${destination.spotId}"
    is ArtifactDiscoveryDestination -> "artifact-discovery:${destination.spotId}:${destination.artifactId}"
    is ArtifactDetailDestination -> "artifact-detail:${destination.spotId}:${destination.artifactId}"
    is SpotExploreDestination -> "spot-explore:${destination.spotId}"
    else -> "welcome"
}

private fun destinationFromToken(token: String): Any {
    val parts = token.split(":")
    return when (parts.firstOrNull()) {
        "welcome" -> WelcomeDestination
        "language" -> LanguageSelectionDestination
        "culture" -> CultureInterestDestination
        "home" -> HomeDestination
        "saved" -> SavedDestination
        "memories" -> MemoriesDestination
        "memory-template" -> MemoryTemplateDestination
        "memory-reflection" -> MemoryReflectionDestination
        "camera-preview" -> CameraPreviewDestination
        "memory-photo" -> MemoryPhotoSelectionDestination(parts.getOrElse(1) { "" })
        "memory-edit" -> MemoryEditDestination(
            templateId = parts.getOrElse(1) { "" },
            photoResIds = parts.getOrNull(2)
                ?.split(",")
                ?.filter { it.isNotBlank() }
                ?.mapNotNull { it.toIntOrNull() }
                .orEmpty()
        )
        "route-detail" -> RouteDetailDestination(parts.getOrElse(1) { "" })
        "spot-detail" -> SpotDetailDestination(parts.getOrElse(1) { "" })
        "spot-intro" -> SpotIntroDestination(parts.getOrElse(1) { "" })
        "artifact-discovery" -> ArtifactDiscoveryDestination(
            spotId = parts.getOrElse(1) { "" },
            artifactId = parts.getOrNull(2)?.toIntOrNull() ?: 0
        )
        "artifact-detail" -> ArtifactDetailDestination(
            spotId = parts.getOrElse(1) { "" },
            artifactId = parts.getOrNull(2)?.toIntOrNull() ?: 0
        )
        "spot-explore" -> SpotExploreDestination(parts.getOrElse(1) { "" })
        else -> WelcomeDestination
    }
}
