// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

plugins {
    alias(libs.plugins.boomlistener.android.library)
}

android {
    namespace = "com.boomlistener.data.profile"
}

dependencies {
    api(project(":core:model"))
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.org.json)
}
