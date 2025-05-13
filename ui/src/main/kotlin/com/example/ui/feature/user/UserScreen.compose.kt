package com.example.ui.feature.user

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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.R.string as RString
import com.example.ui.R.drawable as RDrawable
import com.example.ui.common.components.TopBarView
import com.example.ui.model.TopBarModel
import com.example.ui.theme.BackgroundScreenColor
import com.example.ui.theme.SizeValues.Size08
import com.example.ui.theme.SizeValues.Size16
import com.example.ui.theme.SizeValues.Size24
import com.example.ui.theme.TensoScanTypography
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.annotation.KoinExperimentalAPI

@OptIn(KoinExperimentalAPI::class, ExperimentalMaterial3Api::class)
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

    Scaffold(
        topBar = {
            TopBarView(
                topAppBarModel = TopBarModel(
                    title = RString.app_name,
                    image = RDrawable.ic_default_user,
                    icon = Icons.Default.Settings
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .background(BackgroundScreenColor)
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .background(BackgroundScreenColor)
                    .fillMaxSize()
                    .padding(horizontal = Size24)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Size24)
            ) {
                UserProfileSection(userState.value)
                Spacer(modifier = Modifier.height(Size16))
                AppInfoSection()
                Spacer(modifier = Modifier.height(Size16))
                DisclaimerSection()
            }

            LogoutButton(
                onLogout = userViewModel::logout,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = Size24)
            )
        }
    }
}

@Composable
fun UserProfileSection(user: UserUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(Size08)) {
        Text(
            text = stringResource(RString.user_information_text),
            style = TensoScanTypography.titleLarge
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
            style = TensoScanTypography.labelMedium
        )
        Text(
            text = value,
            style = TensoScanTypography.bodyLarge
        )
    }
}

@Composable
fun AppInfoSection() {
    Column(verticalArrangement = Arrangement.spacedBy(Size08)) {
        Text(
            stringResource(RString.about_tensoscan_text),
            style = TensoScanTypography.titleLarge
        )
        Text(
            text = stringResource(RString.about_tensoscan),
            style = TensoScanTypography.bodyMedium
        )
    }
}

@Composable
fun DisclaimerSection() {
    Column(verticalArrangement = Arrangement.spacedBy(Size08)) {
        Text(
            stringResource(RString.tensoscan_disclaimer),
            style = TensoScanTypography.titleLarge
        )
        Text(
            text = stringResource(RString.tensoscan_disclaimer_text),
            style = TensoScanTypography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun LogoutButton(
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxWidth().padding(horizontal = Size16)
    ) {
        Button(
            onClick = onLogout,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(RString.log_out_text),
                color = MaterialTheme.colorScheme.onError,
                style = TensoScanTypography.bodyLarge
            )
        }
    }
}
