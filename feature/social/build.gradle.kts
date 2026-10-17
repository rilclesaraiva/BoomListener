// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

plugins {
    alias(libs.plugins.boomlistener.android.feature)
}

android {
    namespace = "com.boomlistener.feature.social"
}

dependencies {
    implementation(project(":data:social"))
    implementation(project(":data:library"))
    implementation(project(":data:deezer"))
    implementation(project(":core:player"))

    testImplementation(libs.kotlinx.coroutines.test)
}
