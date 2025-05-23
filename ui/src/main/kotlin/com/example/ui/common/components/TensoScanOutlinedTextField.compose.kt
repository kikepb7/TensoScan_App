package com.example.ui.common.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.example.ui.R
import com.example.ui.theme.IconColor
import com.example.ui.theme.SecondaryTextColor
import com.example.ui.theme.SizeValues.Size16
import com.example.ui.theme.TensoScanTypography
import com.example.ui.theme.TextFieldBorderColor
import com.example.ui.theme.TextFieldTextColor

@Composable
fun TensoScanOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    onDone: (() -> Unit)? = null,
    isPassword: Boolean = false,
) {
    var passwordVisible by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = {
            Text(text = label, style = TensoScanTypography.bodySmall, color = TextFieldTextColor)
        },
        leadingIcon = { Icon(imageVector = icon, contentDescription = label, tint = IconColor) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
        trailingIcon = {
            if (isPassword) {
                val visibilityIcon = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(imageVector = visibilityIcon, contentDescription = if (passwordVisible) stringResource(
                        R.string.hide_password_content_description
                    ) else stringResource(R.string.show_password_content_description),
                        tint = IconColor
                    )
                }
            }
        },
        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = keyboardType, imeAction = imeAction),
        keyboardActions = if (onDone != null) KeyboardActions(onDone = { onDone() }) else KeyboardActions.Default,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = TextFieldBorderColor,
            unfocusedBorderColor = TextFieldBorderColor,
            cursorColor = IconColor,
            focusedLabelColor = TextFieldTextColor,
            unfocusedLabelColor = TextFieldTextColor,
        )
    )
}

@Composable
fun TensoScanChatInputField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    onClear: () -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        placeholder = { Text(text = placeholder, style = TensoScanTypography.bodySmall) },
        shape = RoundedCornerShape(Size16),
        maxLines = 5,
        minLines = 1,
        singleLine = true,
        trailingIcon = {
            if (value.isNotBlank()) {
                IconButton(onClick = onClear) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.clear_text_content_description),
                        tint = IconColor
                    )
                }
            }
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = TextFieldBorderColor,
            unfocusedBorderColor = TextFieldBorderColor.copy(alpha = 0.5f),
            cursorColor = TextFieldTextColor,
            focusedTextColor = TextFieldTextColor,
            unfocusedTextColor = TextFieldTextColor,
            focusedLabelColor = TextFieldTextColor,
            unfocusedLabelColor = TextFieldTextColor,
            focusedPlaceholderColor = SecondaryTextColor,
            unfocusedPlaceholderColor = SecondaryTextColor
        )
    )
}