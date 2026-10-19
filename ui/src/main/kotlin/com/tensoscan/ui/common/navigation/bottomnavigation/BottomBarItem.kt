package com.tensoscan.ui.common.navigation.bottomnavigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector
import com.tensoscan.ui.R
import com.tensoscan.ui.common.navigation.Routes

/** A tab of the bottom bar. The title is the only description of the icon, so the icon itself is decorative. */
internal sealed class BottomBarItem(val route: String, @StringRes val titleRes: Int, val icon: ImageVector) {
    class User : BottomBarItem(Routes.User.route, R.string.nav_user, Icons.Default.Person)

    class Camera : BottomBarItem(Routes.Camera.route, R.string.nav_camera, Icons.Default.CameraAlt)

    class Summary : BottomBarItem(Routes.Summary.route, R.string.nav_history, Icons.Default.History)

    class Assistant : BottomBarItem(Routes.Assistant.route, R.string.nav_assistant, Icons.AutoMirrored.Filled.Chat)
}
