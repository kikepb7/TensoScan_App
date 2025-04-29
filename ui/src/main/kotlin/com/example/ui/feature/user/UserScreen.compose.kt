package com.example.ui.feature.user

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.SizeValues.Size08
import com.example.ui.theme.SizeValues.Size24
import com.example.ui.theme.SizeValues.Size32
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.annotation.KoinExperimentalAPI

@OptIn(KoinExperimentalAPI::class)
@Composable
fun UserScreenView(
    onLogoutNavigate: () -> Unit
) {
    val userViewModel = koinViewModel<UserViewModel>()
    val userState = userViewModel.state.collectAsStateWithLifecycle()
    val logoutState = userViewModel.logoutState.collectAsStateWithLifecycle()
    val logoutTriggered = remember { mutableStateOf(false) }

    LaunchedEffect(logoutState.value) {
        if (logoutState.value && !logoutTriggered.value) {
            logoutTriggered.value = true
            onLogoutNavigate()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Size24)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(text = "Perfil de Usuario", style = MaterialTheme.typography.titleLarge)

            ProfileItem(label = "Nombre", value = userState.value.name)
            ProfileItem(label = "Apellidos", value = userState.value.lastName)
            ProfileItem(label = "Email", value = userState.value.email)

            Spacer(modifier = Modifier.height(Size32))

            Text(text = "Acerca de la app", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "TensoScan ayuda a registrar y analizar tus mediciones de presión arterial.",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Button(
            onClick = userViewModel::logout,
            colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Cerrar sesión", color = Color.White)
        }
    }
}

@Composable
fun ProfileItem(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = Size08)) {
        Text(text = label, style = MaterialTheme.typography.labelMedium)
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
    }
}