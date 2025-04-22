package com.example.tensoscan.ui.feature.register

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.tensoscan.ui.common.components.PulseLoadingView
import com.example.tensoscan.ui.theme.SizeValues.Size16
import com.example.tensoscan.ui.theme.SizeValues.Size24
import com.example.tensoscan.ui.theme.SizeValues.Size32
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.annotation.KoinExperimentalAPI

@OptIn(KoinExperimentalAPI::class)
@Composable
fun RegisterScreen(navController: NavController) {
    val registerViewModel = koinViewModel<RegisterViewModel>()
    val registerState = registerViewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(registerState.value.isSuccess) {
        if (registerState.value.isSuccess) navController.popBackStack()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Size32),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedTextField(
            value = registerState.value.name,
            onValueChange = { registerViewModel.onEvent(RegisterUserEvent.NameChanged(it)) },
            label = { Text("Nombre") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(Size16))
        OutlinedTextField(
            value = registerState.value.lastName,
            onValueChange = { registerViewModel.onEvent(RegisterUserEvent.LastNameChanged(it)) },
            label = { Text("Apellidos") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(Size16))
        OutlinedTextField(
            value = registerState.value.email,
            onValueChange = { registerViewModel.onEvent(RegisterUserEvent.EmailChanged(it)) },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(Size16))
        OutlinedTextField(
            value = registerState.value.password,
            onValueChange = { registerViewModel.onEvent(RegisterUserEvent.PasswordChanged(it)) },
            label = { Text("Contraseña") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation()
        )
        Spacer(modifier = Modifier.height(Size16))
        OutlinedTextField(
            value = registerState.value.confirmPassword,
            onValueChange = { registerViewModel.onEvent(RegisterUserEvent.ConfirmPasswordChanged(it)) },
            label = { Text("Repetir Contraseña") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation()
        )

        registerState.value.error?.let {
            Spacer(modifier = Modifier.height(Size16))
            Text(text = it, color = MaterialTheme.colorScheme.error)
        }

        Spacer(modifier = Modifier.height(Size24))
        Row {
            Button(onClick = { navController.popBackStack() }) {
                Text(text = "Back")
            }
            Spacer(modifier = Modifier.width(Size16))
            Button(onClick = { registerViewModel.registerUser() }) {
                Text(text = "Register")
            }
        }

        if (registerState.value.isLoading) {
            Spacer(modifier = Modifier.height(Size16))
            PulseLoadingView()
        }
    }
}