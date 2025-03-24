package com.example.tensoscan.ui.common.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.example.tensoscan.R.string as RString
import com.example.tensoscan.R.drawable as RDrawable
import com.example.tensoscan.ui.theme.SizeValues.Size16
import com.example.tensoscan.ui.theme.SizeValues.Size24
import com.example.tensoscan.ui.theme.SizeValues.Size76
import com.example.tensoscan.ui.theme.TensoScanDefaultButtonColor
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummaryErrorBottomSheet(
    message: String,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    val coroutineScope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = {
            coroutineScope.launch { sheetState.hide() }
            onDismiss
        },
        containerColor = Color.White,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Size24),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = RDrawable.ic_error),
                contentDescription = "Error image",
                modifier = Modifier.size(Size76)
            )
            Spacer(modifier = Modifier.height(Size16))
            Text(
                text = message,
                style = MaterialTheme.typography.titleMedium,
                color = Color.Black,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(Size24))
            TensoScanButtonView(
                text = stringResource(RString.text_accept_default),
                color = TensoScanDefaultButtonColor,
                onClick = onDismiss
            )
        }
    }
}

@Composable
@Preview(showBackground = true)
fun SummaryErrorBottomSheetPreview() {
    SummaryErrorBottomSheet(message = stringResource(id = RString.upload_state_timeout_error), onDismiss = {})
}