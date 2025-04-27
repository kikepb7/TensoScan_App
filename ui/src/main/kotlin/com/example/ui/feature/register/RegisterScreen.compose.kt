package com.example.ui.feature.register

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.ui.theme.SizeValues.Size08
import com.example.ui.theme.SizeValues.Size12
import com.example.ui.theme.SizeValues.Size16
import com.example.ui.theme.SizeValues.Size24
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.annotation.KoinExperimentalAPI

@OptIn(KoinExperimentalAPI::class)
@Composable
fun RegisterScreen(navController: NavController) {
    val registerViewModel = koinViewModel<RegisterViewModel>()
    val registerState = registerViewModel.state.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current

    LaunchedEffect(registerState.value.isSuccess) {
        if (registerState.value.isSuccess) navController.popBackStack()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        RegisterCard(
            state = registerState.value,
            onEvent = registerViewModel::onEvent,
            onRegister = { registerViewModel.registerUser() },
            onBack = { navController.popBackStack() },
            focusManager = focusManager
        )
    }
}

@Composable
private fun RegisterCard(
    state: RegisterUserState,
    onEvent: (RegisterUserEvent) -> Unit,
    onRegister: () -> Unit,
    onBack: () -> Unit,
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
            RegisterHeader()
            RegisterFields(state, onEvent, onRegister, focusManager)
            RegisterErrorAndLoading(state)
            RegisterActions(onBack, onRegister)
        }
    }
}

@Composable
private fun RegisterHeader() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Create Account",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Please fill in the details below",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun RegisterFields(
    state: RegisterUserState,
    onEvent: (RegisterUserEvent) -> Unit,
    onRegister: () -> Unit,
    focusManager: FocusManager
) {
    Column(verticalArrangement = Arrangement.spacedBy(Size12)) {
        OutlinedTextField(
            value = state.name,
            onValueChange = { onEvent(RegisterUserEvent.NameChanged(it)) },
            label = { Text("First Name") },
            leadingIcon = { Icon(Icons.Default.Person, null) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        OutlinedTextField(
            value = state.lastName,
            onValueChange = { onEvent(RegisterUserEvent.LastNameChanged(it)) },
            label = { Text("Last Name") },
            leadingIcon = { Icon(Icons.Default.PersonOutline, null) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        OutlinedTextField(
            value = state.email,
            onValueChange = { onEvent(RegisterUserEvent.EmailChanged(it)) },
            label = { Text("Email") },
            leadingIcon = { Icon(Icons.Default.Email, null) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions.Default.copy(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            )
        )
        OutlinedTextField(
            value = state.password,
            onValueChange = { onEvent(RegisterUserEvent.PasswordChanged(it)) },
            label = { Text("Password") },
            leadingIcon = { Icon(Icons.Default.Lock, null) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions.Default.copy(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Next
            )
        )
        OutlinedTextField(
            value = state.confirmPassword,
            onValueChange = { onEvent(RegisterUserEvent.ConfirmPasswordChanged(it)) },
            label = { Text("Confirm Password") },
            leadingIcon = { Icon(Icons.Default.Lock, null) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions.Default.copy(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = {
                focusManager.clearFocus()
                onRegister()
            })
        )
    }
}

@Composable
private fun RegisterErrorAndLoading(registerState: RegisterUserState) {
    if (registerState.error != null) {
        Text(
            text = registerState.error,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = Size08)
        )
    }

    if (registerState.isLoading) {
        CircularProgressIndicator(
            modifier = Modifier
                .padding(top = Size08)
        )
    }
}

@Composable
private fun RegisterActions(
    onBack: () -> Unit,
    onRegister: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Size16),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.weight(1f)
        ) {
            Text("Back")
        }
        Spacer(modifier = Modifier.width(Size12))
        Button(
            onClick = onRegister,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(Size12)
        ) {
            Text("Register", style = MaterialTheme.typography.labelLarge)
        }
    }
}