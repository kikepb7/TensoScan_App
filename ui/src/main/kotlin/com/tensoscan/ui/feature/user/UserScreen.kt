package com.tensoscan.ui.feature.user

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.tensoscan.presentation.user.DeleteAccountUiState
import com.tensoscan.presentation.user.UserUiAction
import com.tensoscan.presentation.user.UserUiState
import com.tensoscan.presentation.user.UserViewModel
import com.tensoscan.ui.common.components.BottomBarNavigation
import com.tensoscan.ui.common.components.TensoScanOutlinedTextField
import com.tensoscan.ui.common.components.TopBar
import com.tensoscan.ui.common.navigation.bottomnavigation.BottomBarItem.Assistant
import com.tensoscan.ui.common.navigation.bottomnavigation.BottomBarItem.Camera
import com.tensoscan.ui.common.navigation.bottomnavigation.BottomBarItem.Summary
import com.tensoscan.ui.common.navigation.bottomnavigation.BottomBarItem.User
import com.tensoscan.ui.feature.account.messageRes
import com.tensoscan.ui.model.TopBarModel
import com.tensoscan.ui.theme.BackgroundScreenColor
import com.tensoscan.ui.theme.SizeValues.Size08
import com.tensoscan.ui.theme.SizeValues.Size16
import com.tensoscan.ui.theme.SizeValues.Size24
import com.tensoscan.ui.theme.TensoScanTypography
import com.tensoscan.ui.R.drawable as RDrawable
import com.tensoscan.ui.R.plurals as RPlurals
import com.tensoscan.ui.R.string as RString

/** Signing out and deleting the account end with the session gone; the app then leaves this screen by itself. */
@Composable
internal fun UserScreen(mainNavController: NavHostController, viewModel: UserViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    UserScreenContent(state = state, mainNavController = mainNavController, onAction = viewModel::onAction)
}

@Composable
private fun UserScreenContent(
    state: UserUiState,
    mainNavController: NavHostController,
    onAction: (UserUiAction) -> Unit,
) {
    Scaffold(
        topBar = {
            TopBar(
                topAppBarModel = TopBarModel(
                    title = RString.app_name,
                    image = RDrawable.ic_default_user,
                ),
            )
        },
        bottomBar = {
            BottomBarNavigation(
                items = listOf(User(), Camera(), Summary(), Assistant()),
                navController = mainNavController,
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .background(BackgroundScreenColor)
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = Size24)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Size24),
        ) {
            UserProfileSection(state, onRetry = { onAction(UserUiAction.RetryProfileClicked) })
            AppInfoSection()
            DisclaimerSection()
            AccountActions(state, onAction)
        }
    }

    if (state.confirmLogout) {
        UnsyncedLogoutDialog(
            unsyncedCount = state.unsyncedCount,
            onConfirm = { onAction(UserUiAction.LogoutConfirmed) },
            onDismiss = { onAction(UserUiAction.LogoutDismissed) },
        )
    }
    state.deleteAccount?.let { dialog ->
        DeleteAccountDialog(
            state = dialog,
            onPasswordChange = { onAction(UserUiAction.DeletePasswordChanged(it)) },
            onConfirm = { onAction(UserUiAction.DeleteAccountConfirmed) },
            onDismiss = { onAction(UserUiAction.DeleteAccountDismissed) },
        )
    }
}

@Composable
private fun UnsyncedLogoutDialog(unsyncedCount: Int, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(RString.user_logout_unsynced_title)) },
        text = { Text(pluralStringResource(RPlurals.user_logout_unsynced_message, unsyncedCount, unsyncedCount)) },
        // The safe choice is the prominent one: keep the session and the measurements.
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(RString.user_logout_unsynced_cancel)) }
        },
        dismissButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(RString.user_logout_unsynced_confirm)) }
        },
    )
}

@Composable
private fun DeleteAccountDialog(
    state: DeleteAccountUiState,
    onPasswordChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(RString.user_delete_account_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Size16)) {
                Text(stringResource(RString.user_delete_account_message))
                TensoScanOutlinedTextField(
                    value = state.password,
                    onValueChange = onPasswordChange,
                    label = stringResource(RString.password_label_text),
                    icon = Icons.Default.Lock,
                    isPassword = true,
                    errorText = state.error?.let { stringResource(it.messageRes()) },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = state.password.isNotEmpty() && !state.isDeleting) {
                if (state.isDeleting) {
                    CircularProgressIndicator(modifier = Modifier.padding(end = Size08))
                }
                Text(
                    text = stringResource(RString.user_delete_account_confirm),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !state.isDeleting) {
                Text(stringResource(RString.user_delete_account_cancel))
            }
        },
    )
}

@Composable
private fun UserProfileSection(state: UserUiState, onRetry: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Size08)) {
        Text(
            text = stringResource(RString.user_information_text),
            style = TensoScanTypography.titleLarge,
        )
        when {
            state.isLoading -> CircularProgressIndicator()

            state.loadFailed -> {
                Text(
                    text = stringResource(RString.user_profile_load_failed),
                    color = MaterialTheme.colorScheme.error,
                    style = TensoScanTypography.bodyMedium,
                )
                TextButton(onClick = onRetry) { Text(stringResource(RString.common_retry)) }
            }

            else -> {
                ProfileItem(label = stringResource(RString.name_profile_item), value = state.name)
                ProfileItem(label = stringResource(RString.last_name_profile_item), value = state.lastName)
                ProfileItem(label = stringResource(RString.email_profile_item), value = state.email)
            }
        }
    }
}

@Composable
private fun ProfileItem(label: String, value: String) {
    Column {
        Text(text = "$label:", style = TensoScanTypography.labelMedium)
        Text(text = value, style = TensoScanTypography.bodyLarge)
    }
}

@Composable
private fun AppInfoSection() {
    Column(verticalArrangement = Arrangement.spacedBy(Size08)) {
        Text(stringResource(RString.about_tensoscan_text), style = TensoScanTypography.titleLarge)
        Text(text = stringResource(RString.about_tensoscan), style = TensoScanTypography.bodyMedium)
    }
}

@Composable
private fun DisclaimerSection() {
    Column(verticalArrangement = Arrangement.spacedBy(Size08)) {
        Text(stringResource(RString.tensoscan_disclaimer), style = TensoScanTypography.titleLarge)
        Text(
            text = stringResource(RString.tensoscan_disclaimer_text),
            style = TensoScanTypography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AccountActions(state: UserUiState, onAction: (UserUiAction) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = Size24),
        verticalArrangement = Arrangement.spacedBy(Size08),
    ) {
        Spacer(modifier = Modifier.height(Size08))
        Button(
            onClick = { onAction(UserUiAction.LogoutClicked) },
            enabled = !state.isLoggingOut,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = stringResource(RString.log_out_text),
                color = MaterialTheme.colorScheme.onError,
                style = TensoScanTypography.bodyLarge,
            )
        }
        TextButton(
            onClick = { onAction(UserUiAction.DeleteAccountClicked) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = stringResource(RString.user_delete_account),
                color = MaterialTheme.colorScheme.error,
                style = TensoScanTypography.bodyMedium,
            )
        }
    }
}
