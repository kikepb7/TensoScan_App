package com.tensoscan.ui.feature.user

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tensoscan.presentation.user.UserUiState
import com.tensoscan.presentation.user.UserViewModel
import com.tensoscan.ui.common.components.TopBarView
import com.tensoscan.ui.model.TopBarModel
import com.tensoscan.ui.theme.BackgroundScreenColor
import com.tensoscan.ui.theme.SizeValues.Size08
import com.tensoscan.ui.theme.SizeValues.Size16
import com.tensoscan.ui.theme.SizeValues.Size24
import com.tensoscan.ui.theme.TensoScanTypography
import com.tensoscan.ui.R.drawable as RDrawable
import com.tensoscan.ui.R.plurals as RPlurals
import com.tensoscan.ui.R.string as RString

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserScreenView(onLogoutNavigate: () -> Unit) {
    val userViewModel = hiltViewModel<UserViewModel>()
    val userState = userViewModel.state.collectAsStateWithLifecycle()
    val logoutState = userViewModel.logoutState.collectAsStateWithLifecycle()
    val logoutTriggered = remember { mutableStateOf(false) }
    val confirmLogout = remember { mutableStateOf(false) }

    LaunchedEffect(logoutState.value) {
        if (logoutState.value && !logoutTriggered.value) {
            logoutTriggered.value = true
            onLogoutNavigate()
        }
    }

    Scaffold(
        topBar = {
            TopBarView(
                topAppBarModel =
                TopBarModel(
                    title = RString.app_name,
                    image = RDrawable.ic_default_user,
                    icon = Icons.Default.Settings,
                ),
            )
        },
    ) { paddingValues ->
        Box(
            modifier =
            Modifier
                .background(BackgroundScreenColor)
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            Column(
                modifier =
                Modifier
                    .background(BackgroundScreenColor)
                    .fillMaxSize()
                    .padding(horizontal = Size24)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Size24),
            ) {
                UserProfileSection(userState.value)
                Spacer(modifier = Modifier.height(Size16))
                AppInfoSection()
                Spacer(modifier = Modifier.height(Size16))
                DisclaimerSection()
            }

            LogoutButton(
                onLogout = {
                    if (userState.value.unsyncedCount > 0) confirmLogout.value = true else userViewModel.logout()
                },
                modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = Size24),
            )
        }
    }

    if (confirmLogout.value) {
        UnsyncedLogoutDialog(
            unsyncedCount = userState.value.unsyncedCount,
            onConfirm = {
                confirmLogout.value = false
                userViewModel.logout()
            },
            onDismiss = { confirmLogout.value = false },
        )
    }
}

@Composable
private fun UnsyncedLogoutDialog(unsyncedCount: Int, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(RString.user_logout_unsynced_title)) },
        text = { Text(pluralStringResource(RPlurals.user_logout_unsynced_message, unsyncedCount, unsyncedCount)) },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(RString.user_logout_unsynced_cancel)) }
        },
        dismissButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(RString.user_logout_unsynced_confirm)) }
        },
    )
}

@Composable
fun UserProfileSection(user: UserUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(Size08)) {
        Text(
            text = stringResource(RString.user_information_text),
            style = TensoScanTypography.titleLarge,
        )
        Spacer(modifier = Modifier.height(Size16))
        ProfileItem(label = stringResource(RString.name_profile_item), value = user.name)
        ProfileItem(label = stringResource(RString.last_name_profile_item), value = user.lastName)
        ProfileItem(label = stringResource(RString.email_profile_item), value = user.email)
    }
}

@Composable
fun ProfileItem(label: String, value: String) {
    Column {
        Text(
            text = "$label:",
            style = TensoScanTypography.labelMedium,
        )
        Text(
            text = value,
            style = TensoScanTypography.bodyLarge,
        )
    }
}

@Composable
fun AppInfoSection() {
    Column(verticalArrangement = Arrangement.spacedBy(Size08)) {
        Text(
            stringResource(RString.about_tensoscan_text),
            style = TensoScanTypography.titleLarge,
        )
        Text(
            text = stringResource(RString.about_tensoscan),
            style = TensoScanTypography.bodyMedium,
        )
    }
}

@Composable
fun DisclaimerSection() {
    Column(verticalArrangement = Arrangement.spacedBy(Size08)) {
        Text(
            stringResource(RString.tensoscan_disclaimer),
            style = TensoScanTypography.titleLarge,
        )
        Text(
            text = stringResource(RString.tensoscan_disclaimer_text),
            style = TensoScanTypography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun LogoutButton(onLogout: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth().padding(horizontal = Size16),
    ) {
        Button(
            onClick = onLogout,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = stringResource(RString.log_out_text),
                color = MaterialTheme.colorScheme.onError,
                style = TensoScanTypography.bodyLarge,
            )
        }
    }
}
