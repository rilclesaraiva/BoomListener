// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

plugins {
    alias(libs.plugins.boomlistener.android.feature)
}

android {
    namespace = "com.boomlistener.feature.nowplaying"
}

dependencies {
    implementation(project(":core:player"))
    implementation(project(":data:library"))
    implementation(project(":data:lyrics"))
    // Letra guardada dentro do arquivo.
    implementation(project(":data:tags"))
    // Arrastar para reordenar a fila.
    implementation(libs.reorderable)

    testImplementation(libs.kotlinx.coroutines.test)
}
