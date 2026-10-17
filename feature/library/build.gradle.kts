// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

plugins {
    alias(libs.plugins.boomlistener.android.feature)
}

android {
    namespace = "com.boomlistener.feature.library"
}

dependencies {
    implementation(project(":data:library"))
    // Link do YouTube Music para compartilhar (via Deezer + song.link).
    implementation(project(":data:deezer"))
    implementation(project(":core:player"))
    // Pedido de permissão (rememberLauncherForActivityResult).
    implementation(libs.androidx.activity.compose)
    // Segurar e arrastar para reordenar a playlist.
    implementation(libs.reorderable)

    testImplementation(libs.kotlinx.coroutines.test)
}
