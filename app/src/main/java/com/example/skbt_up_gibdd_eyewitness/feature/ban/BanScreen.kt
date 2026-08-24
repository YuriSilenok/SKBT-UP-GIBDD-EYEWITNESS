package com.example.skbt_up_gibdd_eyewitness.feature.ban

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.skbt_up_gibdd_eyewitness.domain.device.ActiveBan
import com.example.skbt_up_gibdd_eyewitness.domain.device.formattedEnd
import com.example.skbt_up_gibdd_eyewitness.ui.components.AppTopBar

@Composable
fun BanScreen(
    ban: ActiveBan,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        AppTopBar(onBackClick)
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Block,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
            )
            Spacer(Modifier.height(20.dp))
            Text(
                text = "Доступ к чату ограничен",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = if (ban.isPermanent) {
                    "Ваше устройство заблокировано навсегда. Отправка сообщений недоступна."
                } else {
                    "Вы не можете отправлять сообщения до ${ban.formattedEnd()}."
                },
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun BanCheckScreen(
    onBackClick: () -> Unit,
    error: Boolean,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        AppTopBar(onBackClick)
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            if (!error) {
                CircularProgressIndicator()
                Spacer(Modifier.height(16.dp))
                Text("Проверяем доступ к чату…")
            } else {
                Text(
                    "Не удалось проверить статус блокировки. Проверьте подключение к интернету.",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.error,
                )
                Spacer(Modifier.height(20.dp))
                Button(onClick = onRetry) { Text("Повторить") }
            }
        }
    }
}
