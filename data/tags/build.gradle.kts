// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

plugins {
    alias(libs.plugins.boomlistener.android.library)
}

android {
    namespace = "com.boomlistener.data.tags"
}

dependencies {
    api(project(":core:model"))
    implementation(libs.kotlinx.coroutines.android)
    // Leitura e gravação de tags (TagLib nativo, funciona com o descritor de
    // arquivo do MediaStore, que é o que o Android 10+ permite).
    implementation(libs.taglib)
}
