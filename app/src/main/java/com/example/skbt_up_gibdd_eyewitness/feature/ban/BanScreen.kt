package com.example.skbt_up_gibdd_eyewitness.feature.ban

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.skbt_up_gibdd_eyewitness.domain.device.ActiveBan
import com.example.skbt_up_gibdd_eyewitness.domain.device.formattedEnd
import com.example.skbt_up_gibdd_eyewitness.ui.components.AppTopBar
import com.example.skbt_up_gibdd_eyewitness.ui.theme.AppBackground
import com.example.skbt_up_gibdd_eyewitness.ui.theme.Muted
import com.example.skbt_up_gibdd_eyewitness.ui.theme.Navy
import com.example.skbt_up_gibdd_eyewitness.ui.theme.SKBTUPGIBDDEYEWITNESSTheme
import java.time.Instant

@Suppress("UNUSED_PARAMETER")
@Composable
fun BanScreen(
    ban: ActiveBan,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize().background(AppBackground)) {
        AppTopBar()
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp).padding(bottom = 72.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Block,
                contentDescription = null,
                tint = Color.Red,
                modifier = Modifier.size(96.dp),
            )
            Spacer(Modifier.height(28.dp))
            Text(
                text = if (ban.isPermanent) {
                    "Отправка сообщений недоступна"
                } else {
                    "Отправка сообщений временно\nнедоступна"
                },
                style = MaterialTheme.typography.titleLarge,
                color = Navy,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(32.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            ) {
                Column(Modifier.padding(horizontal = 24.dp, vertical = 20.dp)) {
                    Text(
                        text = if (ban.isPermanent || ban.endsAt == null) "БЛОКИРОВКА" else "БЛОКИРОВКА ДЕЙСТВУЕТ ДО",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF9AAEB8),
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = when {
                            ban.isPermanent -> "Бессрочно"
                            ban.endsAt == null -> "Временно"
                            else -> ban.formattedEnd().orEmpty()
                        },
                        style = MaterialTheme.typography.titleLarge,
                        color = Navy,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            if (!ban.isPermanent) {
                Spacer(Modifier.height(28.dp))
                Text(
                    text = "После окончания срока вы снова сможете\nотправлять сообщения.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Muted,
                    textAlign = TextAlign.Center,
                )
            }
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

@Preview(showBackground = true, widthDp = 403, heightDp = 874)
@Composable
private fun TemporaryBanPreview() = SKBTUPGIBDDEYEWITNESSTheme {
    BanScreen(
        ActiveBan(
            "temporary",
            Instant.parse("2026-08-18T11:30:00Z"),
            Instant.parse("2026-08-19T11:30:00Z"),
            1,
        ),
        {},
    )
}

@Preview(showBackground = true, widthDp = 403, heightDp = 874)
@Composable
private fun PermanentBanPreview() = SKBTUPGIBDDEYEWITNESSTheme {
    BanScreen(
        ActiveBan("permanent", Instant.parse("2026-08-18T11:30:00Z"), null, 3),
        {},
    )
}
