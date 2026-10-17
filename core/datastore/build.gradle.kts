// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/boomlistener/blob/main/LICENSE

plugins {
    alias(libs.plugins.boomlistener.android.library)
}

android {
    namespace = "com.boomlistener.core.datastore"
}

dependencies {
    implementation(project(":core:model"))
}
