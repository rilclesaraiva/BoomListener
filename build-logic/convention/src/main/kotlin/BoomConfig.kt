// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/boomlistener/blob/main/LICENSE

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

/** Valores de SDK compartilhados por todos os módulos Android. */
internal object BoomConfig {
    const val COMPILE_SDK = 37
    const val TARGET_SDK = 36
    // Android 8.0. Cobre praticamente todos os aparelhos em uso, inclusive os
    // mais simples, que é o público que mais importa (mesma filosofia do BusBus).
    const val MIN_SDK = 26
    val JAVA_VERSION = JavaVersion.VERSION_17
}

internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

/**
 * Configuração comum a app e bibliotecas. Com o Kotlin embutido do AGP 9, o
 * jvmTarget do Kotlin segue automaticamente o targetCompatibility abaixo.
 */
internal fun configureAndroidCommon(extension: CommonExtension) {
    extension.compileSdk = BoomConfig.COMPILE_SDK
    extension.defaultConfig.minSdk = BoomConfig.MIN_SDK
    extension.compileOptions.sourceCompatibility = BoomConfig.JAVA_VERSION
    extension.compileOptions.targetCompatibility = BoomConfig.JAVA_VERSION
}
