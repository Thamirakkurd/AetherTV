package com.aethertv.v2

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tv
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aethertv.v2.ui.components.FloatingGlassNavBar
import com.aethertv.v2.ui.components.GlassMediaCard
import com.aethertv.v2.ui.components.GlassTopHeader
import com.aethertv.v2.ui.components.NavTabItem
import com.aethertv.v2.ui.screens.HomeScreen
import com.aethertv.v2.ui.theme.MatteGlassTheme
import com.aethertv.v2.ui.theme.MatteGlassTokens

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
        )
        setContent {
            MatteGlassTheme {
                AetherV2Shell()
            }
        }
    }
}

@Composable
private fun AetherV2Shell() {
    val tabs = remember {
        listOf(
            NavTabItem("Home", Icons.Default.Home),
            NavTabItem("Live", Icons.Default.LiveTv),
            NavTabItem("Movies", Icons.Default.Movie),
            NavTabItem("Series", Icons.Default.Tv),
            NavTabItem("Settings", Icons.Default.Settings)
        )
    }
    var selectedTab by remember { mutableIntStateOf(0) }

    val demoItems = remember(selectedTab) {
        val prefix = tabs[selectedTab].label
        (1..12).map { index -> "$prefix $index" }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MatteGlassTokens.CanvasWhite)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            GlassTopHeader(
                title = tabs[selectedTab].label,
                onSearchClick = {},
                onFilterClick = {},
                onAddClick = {}
            )

            if (selectedTab == 0) {
                HomeScreen()
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(start = 6.dp, end = 6.dp, top = 4.dp, bottom = 104.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(demoItems) { idx, title ->
                        GlassMediaCard(
                            title = title,
                            subtitle = if (selectedTab == 1) "HD Stream • Sports & Entertainment" else "2026 • Action / Drama",
                            badgeText = if (selectedTab == 1) "LIVE" else "4K UHD",
                            showLiveDot = selectedTab == 1,
                            toneIndex = idx,
                            onClick = {}
                        )
                    }
                }
            }
        }

        FloatingGlassNavBar(
            items = tabs,
            selectedIndex = selectedTab,
            onSelect = { selectedTab = it },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
