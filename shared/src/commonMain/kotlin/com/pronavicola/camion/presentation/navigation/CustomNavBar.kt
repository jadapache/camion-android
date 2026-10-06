package com.pronavicola.camion.presentation.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Doorbell
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pronavicola.camion.domain.model.AppDestination
import com.pronavicola.camion.domain.model.SessionUser

@Composable
fun CustomNavBar(
    destinations: List<AppDestination>,
    currentRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
    currentUser: SessionUser? = null,
    onUserClick: () -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(72.dp)
            .background(Color.Black)
            .padding(vertical = 15.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            destinations.forEach { destination ->
                val isSelected = currentRoute == destination.route
                val icon = getIconForDestination(destination)

                NavBarItem(
                    icon = icon,
                    label = destination.label,
                    isSelected = isSelected,
                    onClick = { onNavigate(destination.route) },
                )
            }
        }

        UserItem(
            user = currentUser,
            onClick = onUserClick,
        )
    }
}

@Composable
private fun UserItem(
    user: SessionUser?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(72.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (user != null) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xFF005AC1), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                val initials = user.usuario.take(2).uppercase()
                Text(
                    text = initials,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        } else {
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = "Iniciar Sesión",
                tint = Color(0xFF9E9E9E),
                modifier = Modifier.size(36.dp),
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
    modifier: Modifier = Modifier,
) {
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) Color.White else Color(0xFF9E9E9E),
        animationSpec = spring(stiffness = Spring.StiffnessLow),
    )

    Box(
        modifier = modifier
            .size(width = 72.dp, height = 48.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = contentColor,
            modifier = Modifier.size(28.dp),
        )

        if (isSelected) {
            // Blue vertical indicator on the right
            Box(
                modifier = Modifier
                    .fillMaxHeight(0.4f)
                    .width(4.dp)
                    .background(Color(0xFF005AC1), RoundedCornerShape(2.dp))
                    .align(Alignment.CenterEnd)
                    .offset(x = (-2).dp),
            )
        }
    }
}

private fun getIconForDestination(destination: AppDestination): ImageVector {
    return when (destination) {
        AppDestination.HMI -> Icons.Default.LocalShipping
        AppDestination.REGISTROS -> Icons.Default.History
        AppDestination.ALARMAS_REGISTROS -> Icons.Default.Notifications
        AppDestination.PARAMETROS -> Icons.Default.Tune
        AppDestination.CONFIG -> Icons.Default.Settings
        AppDestination.SIMULACION -> Icons.Default.Build
    }
}
