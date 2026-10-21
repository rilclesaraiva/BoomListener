// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

import java.util.Properties

plugins {
    alias(libs.plugins.boomlistener.android.application)
    alias(libs.plugins.boomlistener.android.compose)
}

// Fonte única da versão: APP_VERSION em gradle.properties (ver CLAUDE.md).
// Nunca edite versionCode/versionName aqui.
val appVersion: String = providers.gradleProperty("APP_VERSION").get()
val appVersionCode: Int = appVersion.split(".").map { it.toInt() }.let { (major, minor, patch) ->
    major * 10000 + minor * 100 + patch
}

// Assinatura: "keystore.properties" NUNCA vai para o Git. Ele é criado na hora
// pela Action a partir dos Secrets do repositório. Sem ele, o build de release
// sai sem assinatura (normal para quem só compila para testar).
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) keystorePropertiesFile.inputStream().use { load(it) }
}

android {
    namespace = "com.boomlistener.app"

    defaultConfig {
        // NUNCA mude o applicationId depois do primeiro APK publicado.
        applicationId = "com.boomlistener.app"
        versionCode = appVersionCode
        versionName = appVersion
    }

    signingConfigs {
        // Chave de debug fixa, versionada no repositório: sem ela, cada build da
        // Action gerava uma chave nova e o APK de debug não atualizava por cima.
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
        if (keystorePropertiesFile.exists()) {
            create("release") {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (keystorePropertiesFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }
}

dependencies {
    implementation(project(":core:designsystem"))
    implementation(project(":core:common"))
    implementation(project(":core:model"))
    implementation(project(":core:datastore"))
    implementation(project(":core:player"))
    implementation(project(":data:wellbeing"))
    implementation(project(":data:profile"))
    // Tipo dos artistas seguidos na barra lateral (tela larga).
    implementation(project(":data:releases"))

    implementation(project(":feature:library"))
    implementation(project(":feature:nowplaying"))
    implementation(project(":feature:artist"))
    implementation(project(":feature:tageditor"))
    implementation(project(":feature:releases"))
    implementation(project(":feature:settings"))
    implementation(project(":feature:wellbeing"))
    implementation(project(":feature:profile"))

    implementation(libs.androidx.core)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
}
