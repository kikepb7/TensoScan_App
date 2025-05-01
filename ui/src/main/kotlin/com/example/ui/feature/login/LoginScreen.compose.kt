package com.example.ui.feature.login

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.ui.R
import com.example.ui.common.components.TensoScanOutlinedTextField
import com.example.ui.common.components.TensoScanRoundedButton
import com.example.ui.common.navigation.Routes
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
            .background(BackgroundAppColor),
        contentAlignment = Alignment.Center
    ) {
        LoginCard(
            state = loginState.value,
            onEmailChange = { loginViewModel.onEvent(LoginEvent.EmailChanged(it)) },
            onPasswordChange = { loginViewModel.onEvent(LoginEvent.PasswordChanged(it)) },
            onSubmit = { loginViewModel.onEvent(LoginEvent.Submit) },
            onNavigateToRegister = { navController.navigate(Routes.Register.route) }
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
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .padding(Size16),
        shape = RoundedCornerShape(Size24),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = Size08),
        colors = CardDefaults.elevatedCardColors(containerColor = CardColor)
    ) {
        Column(
            modifier = Modifier
                .background(color = CardColor)
                .padding(Size24)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(Size16),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HeaderSection()
            EmailField(state.email, onEmailChange)
            PasswordField(state.password, onPasswordChange)
            TensoScanRoundedButton(text = stringResource(R.string.login_text), onClick = onSubmit)
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
            text = stringResource(R.string.welcome_text),
            style = TensoScanTypography.headlineSmall,
            color = PrimaryTextColor
        )
        Spacer(modifier = Modifier.height(Size12))
        Text(
            text = stringResource(R.string.login_to_continue_text),
            style = TensoScanTypography.bodyLarge,
            textAlign = TextAlign.Center,
            color = PrimaryTextColor
        )
    }
}

@Composable
private fun EmailField(value: String, onValueChange: (String) -> Unit) {
    TensoScanOutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = stringResource(R.string.email_label_text),
        icon = Icons.Default.Email,
        keyboardType = KeyboardType.Email,
        imeAction = ImeAction.Next
    )
}

@Composable
private fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    onDone: (() -> Unit)? = null
) {
    TensoScanOutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = stringResource(R.string.password_label_text),
        icon = Icons.Default.Lock,
        keyboardType = KeyboardType.Password,
        imeAction = ImeAction.Done,
        onDone = onDone,
        isPassword = true
    )
}


@Composable
private fun RegisterTextButton(onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.padding(top = Size08)
    ) {
        Text(
            text = stringResource(R.string.not_account_yet_text),
            style = TensoScanTypography.bodySmall,
            textAlign = TextAlign.Center,
            color = PrimaryTextColor
        )
    }
}