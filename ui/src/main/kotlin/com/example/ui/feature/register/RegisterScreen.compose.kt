package com.example.ui.feature.register

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.ui.R
import com.example.ui.common.components.TensoScanOutlinedTextField
import com.example.ui.common.components.TensoScanOutlinedRoundedButton
import com.example.ui.common.components.TensoScanRoundedButton
import com.example.ui.theme.BackgroundAppColor
import com.example.ui.theme.CardColor
import com.example.ui.theme.PrimaryTextColor
import com.example.ui.theme.SizeValues.Size08
import com.example.ui.theme.SizeValues.Size12
import com.example.ui.theme.SizeValues.Size16
import com.example.ui.theme.SizeValues.Size24
import com.example.ui.theme.TensoScanTypography
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
        modifier = Modifier.fillMaxSize().background(BackgroundAppColor),
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
        modifier = Modifier.fillMaxWidth(0.9f).padding(Size16),
        shape = RoundedCornerShape(Size24),
        colors = CardDefaults.elevatedCardColors(containerColor = CardColor),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = Size08)
    ) {
        Column(
            modifier = Modifier.padding(Size24).navigationBarsPadding(),
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
            text = stringResource(R.string.create_account_text),
            style = TensoScanTypography.headlineSmall,
            color = PrimaryTextColor
        )
        Spacer(modifier = Modifier.height(Size12))
        Text(
            text = stringResource(R.string.fill_fields_text),
            style = TensoScanTypography.bodySmall,
            textAlign = TextAlign.Center,
            color = PrimaryTextColor
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
        TensoScanOutlinedTextField(
            value = state.name,
            onValueChange = { onEvent(RegisterUserEvent.NameChanged(it)) },
            label = stringResource(R.string.name_register_text),
            icon = Icons.Default.Person,
            imeAction = ImeAction.Next
        )

        TensoScanOutlinedTextField(
            value = state.lastName,
            onValueChange = { onEvent(RegisterUserEvent.LastNameChanged(it)) },
            label = stringResource(R.string.lastName_register_text),
            icon = Icons.Default.PersonOutline,
            imeAction = ImeAction.Next
        )

        TensoScanOutlinedTextField(
            value = state.email,
            onValueChange = { onEvent(RegisterUserEvent.EmailChanged(it)) },
            label = stringResource(R.string.email_label_text),
            icon = Icons.Default.Email,
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Next
        )

        TensoScanOutlinedTextField(
            value = state.password,
            onValueChange = { onEvent(RegisterUserEvent.PasswordChanged(it)) },
            label = stringResource(R.string.password_label_text),
            icon = Icons.Default.Lock,
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Next,
            isPassword = true
        )

        TensoScanOutlinedTextField(
            value = state.confirmPassword,
            onValueChange = { onEvent(RegisterUserEvent.ConfirmPasswordChanged(it)) },
            label = stringResource(R.string.confirm_password_text),
            icon = Icons.Default.Lock,
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done,
            isPassword = true,
            onDone = {
                focusManager.clearFocus()
                onRegister()
            }
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
        CircularProgressIndicator(modifier = Modifier.padding(top = Size08))
    }
}

@Composable
private fun RegisterActions(onBack: () -> Unit, onRegister: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = Size16),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        TensoScanOutlinedRoundedButton(text = stringResource(R.string.back_button_text), onClick = onBack, modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.width(Size12))
        TensoScanRoundedButton(text = stringResource(R.string.register_button_text), onClick = onRegister, modifier = Modifier.weight(1f))

    }
}