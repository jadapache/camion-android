package com.pronavicola.camion.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.pronavicola.camion.domain.model.AppDestination

@Composable
fun CustomNavBar(
    destinations: List<AppDestination>,
    currentRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(72.dp)
            .background(Color.Black)
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        destinations.forEach { destination ->
            val isSelected = currentRoute == destination.route
            val icon = getIconForDestination(destination)

            NavBarItem(
                icon = icon,
                label = destination.label,
                isSelected = isSelected,
                onClick = { onNavigate(destination.route) }
            )
        }
    }
}

@Composable
private fun NavBarItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) Color.White else Color(0xFF9E9E9E),
        animationSpec = spring(stiffness = Spring.StiffnessLow)
    )

    Box(
        modifier = modifier
            .size(width = 72.dp, height = 56.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = contentColor,
            modifier = Modifier.size(28.dp)
        )

        if (isSelected) {
            // Blue vertical indicator on the right
            Box(
                modifier = Modifier
                    .fillMaxHeight(0.4f)
                    .width(4.dp)
                    .background(Color(0xFF005AC1), RoundedCornerShape(2.dp))
                    .align(Alignment.CenterEnd)
                    .offset(x = (-2).dp)
            )
        }
    }
}

private fun getIconForDestination(destination: AppDestination): ImageVector {
    return when (destination) {
        AppDestination.HMI -> Icons.Default.LocalShipping
        AppDestination.REGISTROS -> Icons.Default.History
        AppDestination.PARAMETROS -> Icons.Default.Tune
        AppDestination.CONFIG -> Icons.Default.Settings
    }
}
