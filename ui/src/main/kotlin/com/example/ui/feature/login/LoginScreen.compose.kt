package com.example.ui.feature.login

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.ui.common.navigation.Routes
import com.example.ui.theme.SizeValues.Size08
import com.example.ui.theme.SizeValues.Size12
import com.example.ui.theme.SizeValues.Size16
import com.example.ui.theme.SizeValues.Size24
import com.example.ui.theme.SizeValues.Size48
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.annotation.KoinExperimentalAPI

@OptIn(KoinExperimentalAPI::class)
@Composable
fun LoginScreen(navController: NavController) {
    val loginViewModel = koinViewModel<LoginViewModel>()
    val loginState = loginViewModel.state.collectAsStateWithLifecycle()
    val localContext = LocalContext.current

    BackHandler { (localContext as? Activity)?.finish() }

    LaunchedEffect(loginState.value.isLoading, loginState.value.error) {
        if (!loginState.value.isLoading && loginState.value.error == null && loginState.value.email.isNotBlank()) {
            navController.navigate(Routes.Summary.route) {
                popUpTo(Routes.Login.route) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {

        LoginCard(
            state = loginState.value,
            onEmailChange = { loginViewModel.onEvent(LoginEvent.EmailChanged(it)) },
            onPasswordChange = { loginViewModel.onEvent(LoginEvent.PasswordChanged(it)) },
            onSubmit = { loginViewModel.onEvent(LoginEvent.Submit) },
            onNavigateToRegister = { navController.navigate(Routes.Register.route) },
            focusManager = LocalFocusManager.current
        )
    }
}

@Composable
private fun LoginCard(
    state: LoginState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onNavigateToRegister: () -> Unit,
    focusManager: FocusManager
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .padding(Size16),
        shape = RoundedCornerShape(Size24),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = Size08)
    ) {
        Column(
            modifier = Modifier
                .padding(Size24)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(Size16),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HeaderSection()
            EmailField(state.email, onEmailChange)
            PasswordField(state.password, onPasswordChange, focusManager, onSubmit)
            LoginButton(onSubmit)
            RegisterTextButton(onNavigateToRegister)

            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.padding(top = Size08))
            }

            state.error?.let {
                Text(
                    text = it.toString(),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = Size08)
                )
            }
        }
    }
}

@Composable
private fun HeaderSection() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "¡Bienvenido/a!",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Por favor, inicie sesión para continuar",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EmailField(value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text("Email") },
        leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Email") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions.Default.copy(
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Next
        )
    )
}

@Composable
private fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    focusManager: FocusManager,
    onSubmit: () -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text("Contraseña") },
        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Contraseña") },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions.Default.copy(
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(onDone = {
            focusManager.clearFocus()
            onSubmit()
        })
    )
}

@Composable
private fun LoginButton(onSubmit: () -> Unit) {
    Button(
        onClick = onSubmit,
        modifier = Modifier
            .fillMaxWidth()
            .height(Size48),
        shape = RoundedCornerShape(Size12)
    ) {
        Text("Acceder", style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun RegisterTextButton(onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.padding(top = Size08)
    ) {
        Text("¿Aún no tienes una cuenta? Regístrate")
    }
}