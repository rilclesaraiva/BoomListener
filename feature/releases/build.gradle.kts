// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

plugins {
    alias(libs.plugins.boomlistener.android.feature)
}

android {
    namespace = "com.boomlistener.feature.releases"
}

dependencies {
    implementation(project(":data:releases"))
    implementation(project(":data:deezer"))

    testImplementation(libs.kotlinx.coroutines.test)
}
