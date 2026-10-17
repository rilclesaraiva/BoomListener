// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

plugins {
    alias(libs.plugins.boomlistener.android.library)
    alias(libs.plugins.boomlistener.android.compose)
}

android {
    namespace = "com.boomlistener.core.designsystem"
}

dependencies {
    implementation(project(":core:model"))
    // "api": quem usa o design system recebe os componentes do Material 3 junto.
    api(libs.androidx.compose.material3)
    api(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.palette)
}
