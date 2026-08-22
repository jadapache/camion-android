package com.pronavicola.camion.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pronavicola.camion.domain.model.AppDestination

@Composable
fun CustomNavBar(
    destinations: List<AppDestination>,
    currentRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 16.dp, start = 24.dp, end = 24.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Row(
            modifier = Modifier
                .wrapContentWidth()
                .height(64.dp)
                .background(
                    color = Color.Black,
                    shape = RoundedCornerShape(32.dp)
                )
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            destinations.forEach { destination ->
                val isSelected = currentRoute == destination.route
                val icon = getIconForDestination(destination)
                
                NavBarItem(
                    icon = icon,
                    label = destination.label,
                    isSelected = isSelected,
                    onClick = { onNavigate(destination.route) },
                    modifier = Modifier.weight(if (isSelected) 1.5f else 1f, fill = false)
                )
            }
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
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) Color(0xFF2E2E2E) else Color.Transparent,
        animationSpec = spring(stiffness = Spring.StiffnessLow)
    )
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) Color.White else Color(0xFF9E9E9E),
        animationSpec = spring(stiffness = Spring.StiffnessLow)
    )

    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(backgroundColor)
            .animateContentSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(24.dp)
            )
            
            AnimatedVisibility(visible = isSelected) {
                Row {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = label,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }
            }
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
