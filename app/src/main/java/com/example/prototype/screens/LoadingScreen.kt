package com.example.prototype.screens



import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.prototype.R
import kotlinx.coroutines.delay


@Composable
fun LoadingScreen(onLoadingComplete: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.loading_icon),
                contentDescription = "Loading image",
                modifier = Modifier.size(50.dp),
                colorFilter = ColorFilter.tint(Color(0xFF1B588C))

            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "EKG",
                fontSize = 24.sp,
                fontWeight = FontWeight.W900,
                color = Color(0xFF1B588C)
            )
        }
    }
    LaunchedEffect(Unit) {
        delay(3000)
        onLoadingComplete()
    }
}