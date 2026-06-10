package dev.liquidglass.sample

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.liquidglass.compose.GlassShape
import dev.liquidglass.compose.GlassStyle
import dev.liquidglass.compose.LiquidGlassProviderState
import dev.liquidglass.compose.components.GlassBottomBar
import dev.liquidglass.compose.components.GlassIconButton
import dev.liquidglass.compose.components.GlassSurface
import dev.liquidglass.compose.container.LiquidGlassContainer
import dev.liquidglass.compose.container.LiquidGlassContainerState
import dev.liquidglass.compose.container.glassEffect
import dev.liquidglass.compose.container.rememberLiquidGlassContainerState
import dev.liquidglass.compose.liquidGlass
import kotlin.math.roundToInt

/** Everything made of glass, layered above the provider as siblings. */
@Composable
internal fun BoxScope.GlassOverlay(glassState: LiquidGlassProviderState) {
    TitlePill(glassState)
    DraggableLens(glassState)
    FabCluster(glassState)
    BottomBar(glassState)
}

@Composable
private fun BoxScope.TitlePill(glassState: LiquidGlassProviderState) {
    GlassSurface(
        state = glassState,
        modifier = Modifier
            .align(Alignment.TopCenter)
            .statusBarsPadding()
            .padding(top = 16.dp),
        style = GlassStyle.Regular,
    ) {
        Text(
            text = "Liquid Glass",
            color = Color.White,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
        )
    }
}

/** A free-floating lens with strong refraction and visible dispersion. */
@Composable
private fun BoxScope.DraggableLens(glassState: LiquidGlassProviderState) {
    var lensOffset by remember { mutableStateOf(Offset.Zero) }
    val lensStyle = remember {
        GlassStyle.Clear.copy(
            shape = GlassShape.Circle,
            refraction = dev.liquidglass.compose.GlassRefraction(height = 28.dp, amount = 36.dp),
            chromaticAberration = 0.5f,
        )
    }
    Box(
        modifier = Modifier
            .align(Alignment.Center)
            .offset { IntOffset(lensOffset.x.roundToInt(), lensOffset.y.roundToInt()) }
            .size(150.dp)
            .liquidGlass(glassState, lensStyle)
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    lensOffset += dragAmount
                }
            },
    )
}

/**
 * The signature demo: a container whose action buttons morph out of the main
 * button and merge while they travel — Apple's GlassEffectContainer behavior.
 */
@Composable
private fun BoxScope.FabCluster(glassState: LiquidGlassProviderState) {
    var expanded by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 45f else 0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 380f),
        label = "fabRotation",
    )

    val container = rememberLiquidGlassContainerState(glassState)
    LiquidGlassContainer(
        state = container,
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .navigationBarsPadding()
            .padding(end = 20.dp, bottom = 112.dp),
        spacing = 40.dp,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (expanded) {
                ClusterAction(container, Icons.Filled.Edit, "Edit", "edit")
                ClusterAction(container, Icons.Filled.Share, "Share", "share")
                ClusterAction(container, Icons.Filled.Favorite, "Favorite", "favorite")
            }
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .glassEffect(container, id = "fab", shape = GlassShape.Circle, interactive = true)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        role = Role.Button,
                    ) { expanded = !expanded },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = if (expanded) "Close actions" else "Open actions",
                    tint = Color.White,
                    modifier = Modifier.rotate(rotation),
                )
            }
        }
    }
}

@Composable
private fun ClusterAction(
    container: LiquidGlassContainerState,
    icon: ImageVector,
    label: String,
    id: String,
) {
    Box(
        modifier = Modifier
            .size(52.dp)
            .glassEffect(container, id = id, shape = GlassShape.Circle, interactive = true)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
            ) { },
        contentAlignment = Alignment.Center,
    ) {
        Icon(imageVector = icon, contentDescription = label, tint = Color.White)
    }
}

@Composable
private fun BoxScope.BottomBar(glassState: LiquidGlassProviderState) {
    GlassBottomBar(
        state = glassState,
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .navigationBarsPadding()
            .padding(bottom = 20.dp),
    ) {
        BarItem(glassState, Icons.Filled.Home, "Home")
        BarItem(glassState, Icons.Filled.Search, "Search")
        BarItem(glassState, Icons.Filled.Favorite, "Favorites")
        BarItem(glassState, Icons.Filled.Settings, "Settings")
    }
}

@Composable
private fun BarItem(
    glassState: LiquidGlassProviderState,
    icon: ImageVector,
    label: String,
) {
    GlassIconButton(
        onClick = { },
        state = glassState,
        style = GlassStyle.Clear.copy(shape = GlassShape.Circle).interactive(),
    ) {
        Icon(imageVector = icon, contentDescription = label, tint = Color.White)
    }
}
