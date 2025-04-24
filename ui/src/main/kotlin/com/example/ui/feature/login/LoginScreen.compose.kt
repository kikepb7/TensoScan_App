package com.example.ui.feature.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.ui.common.components.PulseLoadingView
import com.example.ui.common.navigation.Routes
import com.example.ui.theme.SizeValues.Size16
import com.example.ui.theme.SizeValues.Size24
import com.example.ui.theme.SizeValues.Size32
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.annotation.KoinExperimentalAPI

@OptIn(KoinExperimentalAPI::class)
@Composable
fun LoginScreen(navController: NavController) {
    val loginViewModel = koinViewModel<LoginViewModel>()
    val loginState = loginViewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(key1 = loginState.value.isLoading, key2 = loginState.value.error) {
        if (!loginState.value.isLoading && loginState.value.error == null && loginState.value.email.isNotBlank()) {
            navController.navigate(Routes.Summary.route) {
                popUpTo(Routes.Login.route) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Size32),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedTextField(
            value = loginState.value.email,
            onValueChange = { loginViewModel.onEvent(LoginEvent.EmailChanged(it)) },
            label = { Text("Email") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(Size16))
        OutlinedTextField(
            value = loginState.value.password,
            onValueChange = { loginViewModel.onEvent(LoginEvent.PasswordChanged(it)) },
            label = { Text("Password") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation()
        )
        Spacer(modifier = Modifier.height(Size24))
        Button(
            onClick = { loginViewModel.onEvent(LoginEvent.Submit) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Login")
        }

        Spacer(modifier = Modifier.height(Size16))

        TextButton(
            onClick = { navController.navigate(Routes.Register.route) }
        ) {
            Text(text = "Don't have an account? Sign up")
        }

        if (loginState.value.isLoading) {
            Spacer(modifier = Modifier.height(Size16))
            PulseLoadingView()
        }

        loginState.value.error?.let {
            Spacer(modifier = Modifier.height(Size16))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
    }
}