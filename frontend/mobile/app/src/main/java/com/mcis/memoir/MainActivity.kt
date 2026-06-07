package com.mcis.memoir

import android.os.Bundle
//import androidx.activity.enableEdgeToEdge
//import androidx.appcompat.app.AppCompatActivity
//import androidx.core.view.ViewCompat
//import androidx.core.view.WindowInsetsCompat

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.mcis.memoir.ui.theme.AppTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay

/**
 * Navigation destination for the Welcome screen.
 */
data object WelcomeDestination

/**
 * Navigation destination for the Language Selection screen.
 */
data object LanguageSelectionDestination

/**
 * Navigation destination for the Culture Interest screen.
 */
data object CultureInterestDestination

/**
 * Navigation destination for the Home screen.
 */
data object HomeDestination

/**
 * Navigation destination for the Saved screen.
 */
data object SavedDestination

/**
 * Navigation destination for the Memories screen.
 */
data object MemoriesDestination

/**
 * Navigation destination for the Memory Template screen.
 */
data object MemoryTemplateDestination

/**
 * Navigation destination for the Memory Photo Selection screen.
 */
data class MemoryPhotoSelectionDestination(val templateId: String)

/**
 * Navigation destination for the Memory Edit (Preview & Edit) screen.
 */
data class MemoryEditDestination(val templateId: String, val photoResIds: List<Int>)

/**
 * Navigation destination for the Memory Reflection screen.
 */
data object MemoryReflectionDestination

/**
 * Navigation destination for the Route Detail screen.
 */
data class RouteDetailDestination(val routeId: String)

/**
 * Navigation destination for the Spot Detail screen.
 */
data class SpotDetailDestination(val spotId: String)

/**
 * Navigation destination for the Spot Intro (Discovery Mode) screen.
 */
data class SpotIntroDestination(val spotId: String)

/**
 * Navigation destination for the Artifact Discovery (Look Closer) screen.
 */
data class ArtifactDiscoveryDestination(val spotId: String, val artifactId: Int)

/**
 * Navigation destination for the Artifact Detail screen.
 */
data class ArtifactDetailDestination(val spotId: String, val artifactId: Int)

/**
 * Navigation destination for the Camera Preview screen.
 */
data object CameraPreviewDestination

/**
 * Navigation destination for the Spot Explore (Continue Exploring) screen.
 */
data class SpotExploreDestination(val spotId: String)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var showSplash by remember { mutableStateOf(true) }

                    LaunchedEffect(Unit) {
                        delay(2000)
                        showSplash = false
                    }

                    if (showSplash) {
                        SplashScreen()
                    } else {
                        MyAppNavigation()
                    }
                }
            }
        }

//        enableEdgeToEdge()
//        setContentView(R.layout.activity_main)
//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
//            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
//            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
//            insets
//        }
    }
}
