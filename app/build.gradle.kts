// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

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

// Assinatura de release: a Action decodifica a keystore dos Secrets e passa o
// caminho em ANDROID_KEYSTORE_PATH (com a senha e o alias). Nada disso vai para
// o Git; sem a variável, o release sai sem assinatura.
val releaseKeystorePath: String? = System.getenv("ANDROID_KEYSTORE_PATH")

android {
    namespace = "com.boomlistener.app"

    defaultConfig {
        // NUNCA mude o applicationId depois do primeiro APK publicado.
        applicationId = "com.boomlistener.app"
        versionCode = appVersionCode
        versionName = appVersion
    }

    // Idiomas do app (inglês, português, espanhol): gera a lista para o Android 13+
    // mostrar o app em Configurações → Idioma do app (o padrão está em res/resources.properties).
    androidResources {
        generateLocaleConfig = true
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
        if (releaseKeystorePath != null) {
            create("release") {
                storeFile = file(releaseKeystorePath)
                storePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("ANDROID_KEY_ALIAS")
                keyPassword = System.getenv("ANDROID_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            // R8: tira do APK o que o app não usa e otimiza o código (o Compose
            // roda bem mais leve assim). Regras próprias em proguard-rules.pro.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            if (releaseKeystorePath != null) {
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

    implementation(project(":feature:library"))
    implementation(project(":feature:nowplaying"))
    implementation(project(":feature:artist"))
    implementation(project(":feature:tageditor"))
    implementation(project(":feature:releases"))
    implementation(project(":feature:settings"))
    implementation(project(":feature:wellbeing"))
    implementation(project(":feature:profile"))

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
}
