package com.example.skbt_up_gibdd_eyewitness.feature.welcome

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.skbt_up_gibdd_eyewitness.R
import com.example.skbt_up_gibdd_eyewitness.ui.components.AppTopBar
import com.example.skbt_up_gibdd_eyewitness.ui.theme.SKBTUPGIBDDEYEWITNESSTheme
import kotlinx.coroutines.launch

@Composable
fun WelcomeScreen(onStartClick: suspend () -> Result<Unit>, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    Column(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        AppTopBar()
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 27.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(painterResource(R.drawable.app_logo), contentDescription = null, modifier = Modifier.size(width = 112.dp, height = 123.dp))
            Spacer(Modifier.height(20.dp))
            Text(
                "Уважаемые участники дорожного движения!",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))
            Card(
                shape = RoundedCornerShape(15.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(1.dp),
            ) {
                Column(Modifier.padding(horizontal = 20.dp, vertical = 15.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
                    WelcomeParagraph("Госавтоинспекция Костромской области информирует, что приложение создано для предупреждения ДТП с участием нетрезвых водителей.")
                    WelcomeParagraph("С его помощью можно анонимно сообщать о водителях с признаками опьянения, которые управляют транспортом.")
                    WelcomeParagraph("В сообщении можно указать номер, марку, цвет автомобиля, направление движения, отправить геолокацию, фото или видео.")
                    WelcomeParagraph("Вся поступившая информация обрабатывается роботом.")
                }
            }
            Spacer(Modifier.height(20.dp))
            errorMessage?.let {
                Text(
                    "Не удалось подключиться к серверу. Проверьте интернет и повторите попытку.",
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 10.dp),
                )
            }
            Button(
                onClick = {
                    scope.launch {
                        isLoading = true
                        errorMessage = null
                        onStartClick().onFailure { errorMessage = it.message ?: "Ошибка подключения" }
                        isLoading = false
                    }
                },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth().height(55.dp),
                shape = RoundedCornerShape(15.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            ) { Text(if (isLoading) "Подключение…" else "Начать", style = MaterialTheme.typography.labelLarge) }
        }
    }
}

@Composable
private fun WelcomeParagraph(text: String) = Text(text, style = MaterialTheme.typography.bodyMedium)

@Preview(showBackground = true, widthDp = 403, heightDp = 874)
@Composable
private fun WelcomePreview() = SKBTUPGIBDDEYEWITNESSTheme { WelcomeScreen({ Result.success(Unit) }) }
