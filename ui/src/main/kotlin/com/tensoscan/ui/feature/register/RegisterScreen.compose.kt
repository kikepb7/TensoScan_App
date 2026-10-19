package com.tensoscan.ui.feature.register

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tensoscan.domain.account.AccountField
import com.tensoscan.presentation.register.RegisterUiAction
import com.tensoscan.presentation.register.RegisterUiEvent
import com.tensoscan.presentation.register.RegisterUiState
import com.tensoscan.presentation.register.RegisterViewModel
import com.tensoscan.ui.R
import com.tensoscan.ui.common.components.TensoScanOutlinedRoundedButton
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

/**
 * [onGoToLogin] is only used when the account was created but the automatic sign-in failed; otherwise the app leaves
 * this screen by itself when the session appears.
 */
@Composable
fun RegisterScreen(
    onBack: () -> Unit,
    onGoToLogin: (email: String) -> Unit,
    viewModel: RegisterViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is RegisterUiEvent.GoToLogin -> onGoToLogin(event.email)
            }
        }
    }

    RegisterContent(state = state, onAction = viewModel::onAction, onBack = onBack)
}

@Composable
private fun RegisterContent(state: RegisterUiState, onAction: (RegisterUiAction) -> Unit, onBack: () -> Unit) {
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
            colors = CardDefaults.elevatedCardColors(containerColor = CardColor),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = Size08),
        ) {
            Column(
                modifier = Modifier
                    .padding(Size24)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(Size16),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Header()
                RegisterFields(state, onAction)
                PrivacyConsent(state, onAction)
                if (state.isLoading) CircularProgressIndicator()
                state.error?.let {
                    Text(
                        text = stringResource(it.messageRes()),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                    )
                }
                Actions(isLoading = state.isLoading, onBack = onBack, onRegister = {
                    onAction(RegisterUiAction.SubmitClicked)
                })
            }
        }
    }
}

@Composable
private fun Header() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = stringResource(R.string.create_account_text),
            style = TensoScanTypography.headlineSmall,
            color = PrimaryTextColor,
        )
        Spacer(modifier = Modifier.height(Size12))
        Text(
            text = stringResource(R.string.fill_fields_text),
            style = TensoScanTypography.bodySmall,
            textAlign = TextAlign.Center,
            color = PrimaryTextColor,
        )
    }
}

@Composable
private fun RegisterFields(state: RegisterUiState, onAction: (RegisterUiAction) -> Unit) {
    @Composable
    fun errorOf(field: AccountField): String? = state.fieldErrors[field]?.let { stringResource(it.messageRes()) }

    Column(verticalArrangement = Arrangement.spacedBy(Size08)) {
        TensoScanOutlinedTextField(
            value = state.name,
            onValueChange = { onAction(RegisterUiAction.NameChanged(it)) },
            label = stringResource(R.string.name_register_text),
            icon = Icons.Default.Person,
            imeAction = ImeAction.Next,
            errorText = errorOf(AccountField.NAME),
        )
        TensoScanOutlinedTextField(
            value = state.lastName,
            onValueChange = { onAction(RegisterUiAction.LastNameChanged(it)) },
            label = stringResource(R.string.lastName_register_text),
            icon = Icons.Default.PersonOutline,
            imeAction = ImeAction.Next,
            errorText = errorOf(AccountField.LAST_NAME),
        )
        TensoScanOutlinedTextField(
            value = state.email,
            onValueChange = { onAction(RegisterUiAction.EmailChanged(it)) },
            label = stringResource(R.string.email_label_text),
            icon = Icons.Default.Email,
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Next,
            errorText = errorOf(AccountField.EMAIL),
        )
        TensoScanOutlinedTextField(
            value = state.password,
            onValueChange = { onAction(RegisterUiAction.PasswordChanged(it)) },
            label = stringResource(R.string.password_label_text),
            icon = Icons.Default.Lock,
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Next,
            isPassword = true,
            errorText = errorOf(AccountField.PASSWORD),
        )
        TensoScanOutlinedTextField(
            value = state.confirmPassword,
            onValueChange = { onAction(RegisterUiAction.ConfirmPasswordChanged(it)) },
            label = stringResource(R.string.confirm_password_text),
            icon = Icons.Default.Lock,
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done,
            isPassword = true,
            errorText = errorOf(AccountField.CONFIRM_PASSWORD),
        )
    }
}

@Composable
private fun PrivacyConsent(state: RegisterUiState, onAction: (RegisterUiAction) -> Unit) {
    var showDetails by rememberSaveable { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(
                    value = state.acceptedPrivacy,
                    role = Role.Checkbox,
                    onValueChange = { onAction(RegisterUiAction.PrivacyChanged(it)) },
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // The whole row is the control, so the checkbox itself must not be a second focus stop for TalkBack.
            Checkbox(checked = state.acceptedPrivacy, onCheckedChange = null)
            Spacer(modifier = Modifier.width(Size08))
            Text(
                text = stringResource(R.string.register_privacy_label),
                style = TensoScanTypography.bodySmall,
                color = PrimaryTextColor,
            )
        }
        state.fieldErrors[AccountField.PRIVACY]?.let {
            Text(
                text = stringResource(it.messageRes()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        TextButton(onClick = { showDetails = true }) {
            Text(text = stringResource(R.string.register_privacy_details), style = TensoScanTypography.bodySmall)
        }
    }

    if (showDetails) {
        AlertDialog(
            onDismissRequest = { showDetails = false },
            title = { Text(stringResource(R.string.privacy_summary_title)) },
            text = { Text(stringResource(R.string.privacy_summary_text)) },
            confirmButton = {
                TextButton(onClick = { showDetails = false }) { Text(stringResource(R.string.privacy_summary_close)) }
            },
        )
    }
}

@Composable
private fun Actions(isLoading: Boolean, onBack: () -> Unit, onRegister: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Size12),
    ) {
        TensoScanOutlinedRoundedButton(
            text = stringResource(R.string.back_button_text),
            onClick = onBack,
            modifier = Modifier.weight(1f),
        )
        TensoScanRoundedButton(
            text = stringResource(R.string.register_button_text),
            onClick = onRegister,
            modifier = Modifier.weight(1f),
            enabled = !isLoading,
        )
    }
}
