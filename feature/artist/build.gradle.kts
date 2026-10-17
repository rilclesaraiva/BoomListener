// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

plugins {
    alias(libs.plugins.boomlistener.android.feature)
}

android {
    namespace = "com.boomlistener.feature.artist"
}

dependencies {
    implementation(project(":data:library"))
    implementation(project(":data:deezer"))
    implementation(project(":data:releases"))
    implementation(project(":data:lastfm"))
    implementation(project(":core:player"))

    testImplementation(libs.kotlinx.coroutines.test)
}
