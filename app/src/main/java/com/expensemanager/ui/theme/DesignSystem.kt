package com.expensemanager.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

object DsSpacing {
    val xxs = 4.dp
    val xs = 8.dp
    val sm = 12.dp
    val md = 16.dp
    val lg = 20.dp
    val xl = 24.dp
}

object DsRadius {
    val sm = RoundedCornerShape(10.dp)
    val md = RoundedCornerShape(14.dp)
    val lg = RoundedCornerShape(18.dp)
}

object DsAnim {
    const val fast = 220
    const val medium = 360
    const val slow = 900
}

@Composable
fun flatCardElevation() = CardDefaults.cardElevation(defaultElevation = 0.dp)
