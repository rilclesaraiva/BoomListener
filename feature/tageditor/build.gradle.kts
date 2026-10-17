// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

plugins {
    alias(libs.plugins.boomlistener.android.feature)
}

android {
    namespace = "com.boomlistener.feature.tageditor"
}

dependencies {
    implementation(project(":data:tags"))
    implementation(project(":data:library"))
    implementation(project(":data:deezer"))
    implementation(project(":data:lastfm"))
    implementation(project(":core:player"))
    // Photo Picker (capa sem permissão de fotos) e pedido de gravação do Android.
    implementation(libs.androidx.activity.compose)

    testImplementation(libs.kotlinx.coroutines.test)
}
