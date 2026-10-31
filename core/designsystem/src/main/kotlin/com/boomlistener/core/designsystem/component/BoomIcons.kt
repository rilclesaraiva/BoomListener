// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.core.designsystem.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * Todos os ícones do app passam por aqui, para trocar o conjunto inteiro
 * mexendo num arquivo só.
 *
 * Os ícones de player não existem no pacote básico do Material, e o pacote
 * "extended" pesa vários MB. Por isso eles são desenhados aqui a partir dos
 * caminhos dos Material Icons (licença Apache 2.0).
 */
object BoomIcons {
    val Library: ImageVector = Icons.Filled.Home
    val Releases: ImageVector = Icons.Filled.Notifications
    val Wellbeing: ImageVector = Icons.Filled.Favorite
    val Settings: ImageVector = Icons.Filled.Settings
    val Search: ImageVector = Icons.Filled.Search
    val Back: ImageVector = Icons.AutoMirrored.Filled.ArrowBack
    val Close: ImageVector = Icons.Filled.Close
    val More: ImageVector = Icons.Filled.MoreVert
    val Collapse: ImageVector = Icons.Filled.KeyboardArrowDown
    val Info: ImageVector = Icons.Filled.Info
    val Privacy: ImageVector = Icons.Filled.Lock

    val Check: ImageVector = Icons.Filled.Check
    val Owned: ImageVector = Icons.Filled.CheckCircle
    val OpenExternal: ImageVector = materialIcon(
        "OpenInNew",
        "M19,19H5V5h7V3H5c-1.11,0 -2,0.9 -2,2v14c0,1.1 0.89,2 2,2h14c1.1,0 2,-0.9 2,-2v-7h-2v7zM14,3v2h3.59l-9.83,9.83 1.41,1.41L19,6.41V10h2V3h-7z",
    )
    val Play: ImageVector = Icons.Filled.PlayArrow
    val Pause: ImageVector = materialIcon("Pause", "M6,19h4V5H6v14zM14,5v14h4V5h-4z")
    val SkipNext: ImageVector = materialIcon("SkipNext", "M6,18l8.5,-6L6,6v12zM16,6v12h2V6h-2z")
    val SkipPrevious: ImageVector = materialIcon("SkipPrevious", "M6,6h2v12H6zM9.5,12l8.5,6V6z")
    val Shuffle: ImageVector = materialIcon(
        "Shuffle",
        "M10.59,9.17L5.41,4 4,5.41l5.17,5.17 1.42,-1.41zM14.5,4l2.04,2.04L4,18.59 5.41,20 17.96,7.46 20,9.5V4h-5.5zM14.83,13.41l-1.41,1.41 3.13,3.13L14.5,20H20v-5.5l-2.04,2.04 -3.13,-3.13z",
    )
    val Repeat: ImageVector = materialIcon(
        "Repeat",
        "M7,7h10v3l4,-4 -4,-4v3H5v6h2V7zM17,17H7v-3l-4,4 4,4v-3h12v-6h-2v4z",
    )
    val RepeatOne: ImageVector = materialIcon(
        "RepeatOne",
        "M7,7h10v3l4,-4 -4,-4v3H5v6h2V7zM17,17H7v-3l-4,4 4,4v-3h12v-6h-2v4zM13,15V9h-1l-2,1v1h1.5v4H13z",
    )
    val MusicNote: ImageVector = materialIcon(
        "MusicNote",
        "M12,3v10.55c-0.59,-0.34 -1.27,-0.55 -2,-0.55 -2.21,0 -4,1.79 -4,4s1.79,4 4,4 4,-1.79 4,-4V7h4V3h-6z",
    )
    val Palette: ImageVector = materialIcon(
        "Palette",
        "M12,3c-4.97,0 -9,4.03 -9,9s4.03,9 9,9c0.83,0 1.5,-0.67 1.5,-1.5 0,-0.39 -0.15,-0.74 -0.39,-1.01 -0.23,-0.26 -0.38,-0.61 -0.38,-0.99 0,-0.83 0.67,-1.5 1.5,-1.5H16c2.76,0 5,-2.24 5,-5 0,-4.42 -4.03,-8 -9,-8zM6.5,12c-0.83,0 -1.5,-0.67 -1.5,-1.5S5.67,9 6.5,9 8,9.67 8,10.5 7.33,12 6.5,12zM9.5,8C8.67,8 8,7.33 8,6.5S8.67,5 9.5,5s1.5,0.67 1.5,1.5S10.33,8 9.5,8zM14.5,8c-0.83,0 -1.5,-0.67 -1.5,-1.5S13.67,5 14.5,5s1.5,0.67 1.5,1.5S15.33,8 14.5,8zM17.5,12c-0.83,0 -1.5,-0.67 -1.5,-1.5S16.67,9 17.5,9s1.5,0.67 1.5,1.5 -0.67,1.5 -1.5,1.5z",
    )
    val Code: ImageVector = materialIcon(
        "Code",
        "M9.4,16.6L4.8,12l4.6,-4.6L8,6l-6,6 6,6 1.4,-1.4zM14.6,16.6l4.6,-4.6 -4.6,-4.6L16,6l6,6 -6,6 -1.4,-1.4z",
    )
    val Update: ImageVector = materialIcon(
        "SystemUpdate",
        "M17,1.01L7,1C5.9,1 5,1.9 5,3v18c0,1.1 0.9,2 2,2h10c1.1,0 2,-0.9 2,-2V3C19,1.9 18.1,1.01 17,1.01zM17,19H7V5h10V19zM16,13h-3V8h-2v5H8l4,4L16,13z",
    )
    val GraphicEq: ImageVector = materialIcon(
        "GraphicEq",
        "M7,18h2V6H7v12zM11,22h2V2h-2v20zM3,14h2v-4H3v4zM15,18h2V6h-2v12zM19,10v4h2v-4h-2z",
    )
    val DragHandle: ImageVector = materialIcon("DragHandle", "M20,9H4v2h16V9zM4,15h16v-2H4v2z")
    val Article: ImageVector = materialIcon(
        "Article",
        "M19,3H5C3.9,3 3,3.9 3,5v14c0,1.1 0.9,2 2,2h14c1.1,0 2,-0.9 2,-2V5C21,3.9 20.1,3 19,3zM14,17H7v-2h7V17zM17,13H7v-2h10V13zM17,9H7V7h10V9z",
    )
    val Edit: ImageVector = Icons.Filled.Edit
    val AutoFix: ImageVector = materialIcon(
        "AutoFixHigh",
        "M7.5,5.6L10,7 8.6,4.5 10,2 7.5,3.4 5,2l1.4,2.5L5,7zM19.5,15.4L17,14l1.4,2.5L17,19l2.5,-1.4L22,19l-1.4,-2.5L22,14zM22,2l-2.5,1.4L17,2l1.4,2.5L17,7l2.5,-1.4L22,7l-1.4,-2.5zM14.37,7.29c-0.39,-0.39 -1.02,-0.39 -1.41,0L1.29,18.96c-0.39,0.39 -0.39,1.02 0,1.41l2.34,2.34c0.39,0.39 1.02,0.39 1.41,0L16.7,11.05c0.39,-0.39 0.39,-1.02 0,-1.41l-2.33,-2.35zM13.34,12.78l-2.12,-2.12 2.44,-2.44 2.12,2.12 -2.44,2.44z",
    )
    val Add: ImageVector = Icons.Filled.Add
    val Person: ImageVector = Icons.Filled.Person
    val Collection: ImageVector = materialIcon(
        "LibraryMusic",
        "M20,2H8c-1.1,0 -2,0.9 -2,2v12c0,1.1 0.9,2 2,2h12c1.1,0 2,-0.9 2,-2V4c0,-1.1 -0.9,-2 -2,-2zM18,7h-3v5.5c0,1.38 -1.12,2.5 -2.5,2.5S10,13.88 10,12.5s1.12,-2.5 2.5,-2.5c0.57,0 1.08,0.19 1.5,0.51V5h4v2zM4,6H2v14c0,1.1 0.9,2 2,2h14v-2H4V6z",
    )
    val History: ImageVector = materialIcon(
        "History",
        "M13,3c-4.97,0 -9,4.03 -9,9L1,12l3.89,3.89 0.07,0.14L9,12L6,12c0,-3.87 3.13,-7 7,-7s7,3.13 7,7 -3.13,7 -7,7c-1.93,0 -3.68,-0.79 -4.94,-2.06l-1.42,1.42C8.27,19.99 10.51,21 13,21c4.97,0 9,-4.03 9,-9s-4.03,-9 -9,-9zM12,8v5l4.28,2.54 0.72,-1.21 -3.5,-2.08L13.5,8L12,8z",
    )
    val Playlist: ImageVector = materialIcon(
        "QueueMusic",
        "M15,6H3v2h12V6zM15,10H3v2h12v-2zM3,16h8v-2H3v2zM17,6v8.18c-0.31,-0.11 -0.65,-0.18 -1,-0.18 -1.66,0 -3,1.34 -3,3s1.34,3 3,3 3,-1.34 3,-3V8h3V6h-5z",
    )
    val Send: ImageVector = materialIcon("Send", "M2.01,21L23,12 2.01,3 2,10l15,2 -15,2z")
    val Bluetooth: ImageVector = materialIcon(
        "Bluetooth",
        "M17.71,7.71L12,2h-1v7.59L6.41,5 5,6.41 10.59,12 5,17.59 6.41,19 11,14.41V22h1l5.71,-5.71 -4.3,-4.29 4.3,-4.29zM13,5.83l1.88,1.88L13,9.59V5.83zM14.88,16.29L13,18.17v-3.76l1.88,1.88z",
    )
    val Headphones: ImageVector = materialIcon(
        "Headphones",
        "M12,1c-4.97,0 -9,4.03 -9,9v7c0,1.66 1.34,3 3,3h3v-8H5v-2c0,-3.87 3.13,-7 7,-7s7,3.13 7,7v2h-4v8h3c1.66,0 3,-1.34 3,-3v-7c0,-4.97 -4.03,-9 -9,-9z",
    )
    val Smartphone: ImageVector = materialIcon(
        "Smartphone",
        "M17,1.01L7,1c-1.1,0 -2,0.9 -2,2v18c0,1.1 0.9,2 2,2h10c1.1,0 2,-0.9 2,-2V3c0,-1.1 -0.9,-1.99 -2,-1.99zM17,19H7V5h10v14z",
    )
    val Tv: ImageVector = materialIcon(
        "Tv",
        "M21,3H3c-1.1,0 -2,0.9 -2,2v12c0,1.1 0.9,2 2,2h5v2h8v-2h5c1.1,0 1.99,-0.9 1.99,-2L23,5c0,-1.1 -0.9,-2 -2,-2zM21,17H3V5h18v12z",
    )
    val Devices: ImageVector = materialIcon(
        "Devices",
        "M4,6h18V4H4c-1.1,0 -2,0.9 -2,2v11H0v3h14v-3H4V6zM23,8h-6c-0.55,0 -1,0.45 -1,1v10c0,0.55 0.45,1 1,1h6c0.55,0 1,-0.45 1,-1V9c0,-0.55 -0.45,-1 -1,-1zM22,17h-4v-7h4v7z",
    )
    val NotificationsOff: ImageVector = materialIcon(
        "NotificationsOff",
        "M20,18.69L7.84,6.14 5.27,3.49 4,4.76l2.8,2.8v0.01c-0.52,0.99 -0.8,2.16 -0.8,3.42v5l-2,2v1h13.73l2,2L21,19.72l-1,-1.03zM12,22c1.11,0 2,-0.89 2,-2h-4c0,1.11 0.89,2 2,2zM18,14.68V11c0,-3.08 -1.64,-5.64 -4.5,-6.32V4c0,-0.83 -0.67,-1.5 -1.5,-1.5s-1.5,0.67 -1.5,1.5v0.68c-0.15,0.03 -0.29,0.08 -0.42,0.12 -0.1,0.03 -0.2,0.07 -0.3,0.11h-0.01c-0.01,0 -0.01,0 -0.02,0.01 -0.23,0.09 -0.46,0.2 -0.68,0.31 0,0 -0.01,0 -0.01,0.01L18,14.68z",
    )
    val NewReleases: ImageVector = materialIcon(
        "NewReleases",
        "M23,12l-2.44,-2.78 0.34,-3.68 -3.61,-0.82 -1.89,-3.18L12,3 8.6,1.54 6.71,4.72l-3.61,0.81 0.34,3.68L1,12l2.44,2.78 -0.34,3.69 3.61,0.82 1.89,3.18L12,21l3.4,1.46 1.89,-3.18 3.61,-0.82 -0.34,-3.68L23,12zM10.09,16.72l-3.8,-3.81 1.48,-1.48 2.32,2.33 5.85,-5.87 1.48,1.48 -7.33,7.35z",
    )
    val BugReport: ImageVector = materialIcon(
        "BugReport",
        "M20,8h-2.81c-0.45,-0.78 -1.07,-1.45 -1.82,-1.96L17,4.41 15.59,3l-2.17,2.17C12.96,5.06 12.49,5 12,5c-0.49,0 -0.96,0.06 -1.41,0.17L8.41,3 7,4.41l1.62,1.63C7.88,6.55 7.26,7.22 6.81,8H4v2h2.09c-0.05,0.33 -0.09,0.66 -0.09,1v1H4v2h2v1c0,0.34 0.04,0.67 0.09,1H4v2h2.81c1.04,1.79 2.97,3 5.19,3s4.15,-1.21 5.19,-3H20v-2h-2.09c0.05,-0.33 0.09,-0.66 0.09,-1v-1h2v-2h-2v-1c0,-0.34 -0.04,-0.67 -0.09,-1H20V8zM14,16h-4v-2h4v2zM14,12h-4v-2h4v2z",
    )
    val ChevronRight: ImageVector = materialIcon("ChevronRight", "M10,6L8.59,7.41 13.17,12l-4.58,4.59L10,18l6,-6z")
    /** Coração cheio: música favorita. */
    val Favorite: ImageVector = Icons.Filled.Favorite
    /** Coração vazio: ainda não é favorita. */
    val FavoriteBorder: ImageVector = materialIcon(
        "FavoriteBorder",
        "M16.5,3c-1.74,0 -3.41,0.81 -4.5,2.09C10.91,3.81 9.24,3 7.5,3 4.42,3 2,5.42 2,8.5c0,3.78 3.4,6.86 8.55,11.54L12,21.35l1.45,-1.32C18.6,15.36 22,12.28 22,8.5 22,5.42 19.58,3 16.5,3zM12.1,18.55l-0.1,0.1 -0.1,-0.1C7.14,14.24 4,11.39 4,8.5 4,6.5 5.5,5 7.5,5c1.54,0 3.04,0.99 3.57,2.36h1.87C13.46,5.99 14.96,5 16.5,5c2,0 3.5,1.5 3.5,3.5 0,2.89 -3.14,5.74 -7.9,10.05z",
    )
    val Image: ImageVector = materialIcon(
        "Image",
        "M21,19V5c0,-1.1 -0.9,-2 -2,-2H5c-1.1,0 -2,0.9 -2,2v14c0,1.1 0.9,2 2,2h14c1.1,0 2,-0.9 2,-2zM8.5,13.5l2.5,3.01L14.5,12l4.5,6H5l3.5,-4.5z",
    )

    // Grade / lista (Biblioteca).
    val GridView: ImageVector = materialIcon(
        "GridView",
        "M3,3v8h8V3H3zM9,9H5V5h4V9zM3,13v8h8v-8H3zM9,19H5v-4h4V19zM13,3v8h8V3H13zM19,9h-4V5h4V9zM13,13v8h8v-8H13zM19,19h-4v-4h4V19z",
    )
    val ListView: ImageVector = Icons.AutoMirrored.Filled.List

    // Ícones do menu das músicas (caminhos dos Material Icons).
    val Share: ImageVector = Icons.Filled.Share
    val Delete: ImageVector = Icons.Filled.Delete
    val PlaylistAdd: ImageVector = materialIcon(
        "PlaylistAdd",
        "M14,10H2v2h12v-2zM14,6H2v2h12V6zM18,14v-4h-2v4h-4v2h4v4h2v-4h4v-2h-4zM2,16h8v-2H2v2z",
    )
    val RemoveCircle: ImageVector = materialIcon(
        "RemoveCircleOutline",
        "M7,11v2h10v-2H7zM12,2C6.48,2 2,6.48 2,12s4.48,10 10,10 10,-4.48 10,-10S17.52,2 12,2zM12,20c-4.41,0 -8,-3.59 -8,-8s3.59,-8 8,-8 8,3.59 8,8 -3.59,8 -8,8z",
    )
    val Album: ImageVector = materialIcon(
        "Album",
        "M12,2C6.48,2 2,6.48 2,12s4.48,10 10,10 10,-4.48 10,-10S17.52,2 12,2zM12,16.5c-2.49,0 -4.5,-2.01 -4.5,-4.5S9.51,7.5 12,7.5s4.5,2.01 4.5,4.5 -2.01,4.5 -4.5,4.5zM12,11c-0.55,0 -1,0.45 -1,1s0.45,1 1,1 1,-0.45 1,-1 -0.45,-1 -1,-1z",
    )
    val Timer: ImageVector = materialIcon(
        "Timer",
        "M15,1H9v2h6V1zM11,14h2V8h-2v6zM19.03,7.39l1.42,-1.42c-0.43,-0.51 -0.9,-0.99 -1.41,-1.41l-1.42,1.42C16.07,4.74 14.12,4 12,4c-4.97,0 -9,4.03 -9,9s4.02,9 9,9 9,-4.03 9,-9c0,-2.12 -0.74,-4.07 -1.97,-5.61zM12,20c-3.87,0 -7,-3.13 -7,-7s3.13,-7 7,-7 7,3.13 7,7 -3.13,7 -7,7z",
    )
    val Speed: ImageVector = materialIcon(
        "Speed",
        "M20.38,8.57l-1.23,1.85a8,8 0,0 1,-0.22 7.58L5.07,18A8,8 0,0 1,15.58 6.85l1.85,-1.23A10,10 0,0 0,3.35 19a2,2 0,0 0,1.72 1h13.85a2,2 0,0 0,1.74 -1,10 10,0 0,0 -0.27,-10.44zM10.59,15.41a2,2 0,0 0,2.83 0l5.66,-8.49 -8.49,5.66a2,2 0,0 0,0 2.83z",
    )
    val QueuePlayNext: ImageVector = materialIcon(
        "QueuePlayNext",
        "M21,3H3C1.89,3 1,3.89 1,5v12c0,1.1 0.89,2 2,2h5v2h8v-2h2v-2H3V5h18v8h2V5c0,-1.11 -0.9,-2 -2,-2zM13,10V7h-2v3H8v2h3v3h2v-3h3v-2h-3zM24,18l-4.5,4.5L18,21l3,-3 -3,-3 1.5,-1.5L24,18z",
    )
    val Backup: ImageVector = materialIcon(
        "Backup",
        "M19.35,10.04C18.67,6.59 15.64,4 12,4 9.11,4 6.6,5.64 5.35,8.04 2.34,8.36 0,10.91 0,14c0,3.31 2.69,6 6,6h13c2.76,0 5,-2.24 5,-5 0,-2.64 -2.05,-4.78 -4.65,-4.96zM14,13v4h-4v-4H7l5,-5 5,5h-3z",
    )
    val Restore: ImageVector = materialIcon(
        "Restore",
        "M13,3c-4.97,0 -9,4.03 -9,9L1,12l3.89,3.89 0.07,0.14L9,12L6,12c0,-3.87 3.13,-7 7,-7s7,3.13 7,7 -3.13,7 -7,7c-1.93,0 -3.68,-0.79 -4.94,-2.06l-1.42,1.42C8.27,19.99 10.51,21 13,21c4.97,0 9,-4.03 9,-9s-4.03,-9 -9,-9zM12,8v5l4.28,2.54 0.72,-1.21 -3.5,-2.08L13.5,8L12,8z",
    )
    /** Uma onda em mola (o visualizador "Onda"), desenhada só com traço. */
    val Waveform: ImageVector = strokeIcon(
        "Waveform",
        "M2,12c1.2,0 1.6,-5 3,-5s1.8,10 3.3,10 1.8,-12 3.4,-12 1.9,14 3.4,14 1.8,-10 3.3,-10 1.6,5 3.6,3",
    )
    val QueueMusic: ImageVector = materialIcon(
        "QueueMusic",
        "M15,6H3v2h12V6zM15,10H3v2h12v-2zM3,16h8v-2H3v2zM17,6v8.18c-0.31,-0.11 -0.65,-0.18 -1,-0.18 -1.66,0 -3,1.34 -3,3s1.34,3 3,3 3,-1.34 3,-3V8h3V6h-5z",
    )
}

private fun materialIcon(name: String, pathData: String): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).addPath(
        pathData = addPathNodes(pathData),
        fill = SolidColor(Color.Black), // a cor real vem do tint do Icon
    ).build()

private fun strokeIcon(name: String, pathData: String): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).addPath(
        pathData = addPathNodes(pathData),
        fill = null,
        stroke = SolidColor(Color.Black), // a cor real vem do tint do Icon
        strokeLineWidth = 2f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
    ).build()
