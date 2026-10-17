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
    implementation(project(":core:player"))
    // Pedido de permissão (rememberLauncherForActivityResult).
    implementation(libs.androidx.activity.compose)

    testImplementation(libs.kotlinx.coroutines.test)
}
