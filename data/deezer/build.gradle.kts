// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

plugins {
    alias(libs.plugins.boomlistener.android.library)
}

android {
    namespace = "com.boomlistener.data.deezer"
}

dependencies {
    api(project(":core:model"))
    implementation(project(":core:common"))
    implementation(libs.kotlinx.coroutines.android)

    // org.json de verdade nos testes (resposta da busca da Apple).
    testImplementation(libs.org.json)
}
