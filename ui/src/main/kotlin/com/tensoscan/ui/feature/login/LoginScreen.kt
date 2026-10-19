package com.tensoscan.ui.feature.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tensoscan.domain.account.AccountField
import com.tensoscan.presentation.login.LoginUiAction
import com.tensoscan.presentation.login.LoginUiState
import com.tensoscan.presentation.login.LoginViewModel
import com.tensoscan.ui.R
import com.tensoscan.ui.common.components.TensoScanOutlinedTextField
import com.tensoscan.ui.common.components.TensoScanRoundedButton
import com.tensoscan.ui.feature.account.messageRes
import com.tensoscan.ui.theme.BackgroundAppColor
import com.tensoscan.ui.theme.CardColor
import com.tensoscan.ui.theme.PrimaryTextColor
import com.tensoscan.ui.theme.SizeValues.Size08
import com.tensoscan.ui.theme.SizeValues.Size12
import com.tensoscan.ui.theme.SizeValues.Size16
import com.tensoscan.ui.theme.SizeValues.Size24
import com.tensoscan.ui.theme.TensoScanTypography

/** Leaving the screen after a successful sign-in is the app's job: it follows the session, see `NavigationWrapper`. */
@Composable
internal fun LoginScreen(onNavigateToRegister: () -> Unit, viewModel: LoginViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LoginContent(state = state, onAction = viewModel::onAction, onNavigateToRegister = onNavigateToRegister)
}

@Composable
private fun LoginContent(state: LoginUiState, onAction: (LoginUiAction) -> Unit, onNavigateToRegister: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundAppColor)
            .imePadding()
            .verticalScroll(rememberScrollState()),
        contentAlignment = Alignment.Center,
    ) {
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(Size16),
            shape = RoundedCornerShape(Size24),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = Size08),
            colors = CardDefaults.elevatedCardColors(containerColor = CardColor),
        ) {
            Column(
                modifier = Modifier
                    .padding(Size24)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(Size16),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Header()
                Notice(state)
                LoginFields(state, onAction)
                TensoScanRoundedButton(
                    text = stringResource(R.string.login_text),
                    onClick = { onAction(LoginUiAction.SubmitClicked) },
                    enabled = !state.isLoading,
                )
                TextButton(onClick = onNavigateToRegister, enabled = !state.isLoading) {
                    Text(
                        text = stringResource(R.string.not_account_yet_text),
                        style = TensoScanTypography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = PrimaryTextColor,
                    )
                }
                if (state.isLoading) CircularProgressIndicator()
                state.error?.let {
                    Text(
                        text = stringResource(it.messageRes()),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun Header() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = stringResource(R.string.welcome_text),
            style = TensoScanTypography.headlineSmall,
            color = PrimaryTextColor,
        )
        Spacer(modifier = Modifier.height(Size12))
        Text(
            text = stringResource(R.string.login_to_continue_text),
            style = TensoScanTypography.bodyLarge,
            textAlign = TextAlign.Center,
            color = PrimaryTextColor,
        )
    }
}

@Composable
private fun Notice(state: LoginUiState) {
    val message = when {
        state.sessionExpired -> R.string.login_session_expired
        state.accountCreated -> R.string.login_account_created
        else -> return
    }
    Text(
        text = stringResource(message),
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun LoginFields(state: LoginUiState, onAction: (LoginUiAction) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Size08)) {
        TensoScanOutlinedTextField(
            value = state.email,
            onValueChange = { onAction(LoginUiAction.EmailChanged(it)) },
            label = stringResource(R.string.email_label_text),
            icon = Icons.Default.Email,
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Next,
            errorText = state.fieldErrors[AccountField.EMAIL]?.let { stringResource(it.messageRes()) },
        )
        TensoScanOutlinedTextField(
            value = state.password,
            onValueChange = { onAction(LoginUiAction.PasswordChanged(it)) },
            label = stringResource(R.string.password_label_text),
            icon = Icons.Default.Lock,
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done,
            onDone = { onAction(LoginUiAction.SubmitClicked) },
            isPassword = true,
            errorText = state.fieldErrors[AccountField.PASSWORD]?.let { stringResource(it.messageRes()) },
        )
    }
}
