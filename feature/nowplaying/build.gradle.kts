// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

plugins {
    alias(libs.plugins.boomlistener.android.feature)
}

android {
    namespace = "com.boomlistener.feature.nowplaying"
}

dependencies {
    implementation(project(":core:player"))
    implementation(project(":data:library"))
    implementation(project(":data:lyrics"))
    // Letra guardada dentro do arquivo.
    implementation(project(":data:tags"))
    // Link do YouTube Music para compartilhar (via Deezer + song.link).
    implementation(project(":data:deezer"))
    // Arrastar para reordenar a fila.
    implementation(libs.reorderable)
    // Banner para compartilhar: vídeo com o trecho de 30 s (Transformer) e o FileProvider.
    implementation(libs.androidx.media3.transformer)
    // Barrinhas que pulsam no vídeo (desenhadas por cima de cada quadro).
    implementation(libs.androidx.media3.effect)
    // Prévia do trecho no painel do banner (um player só dele).
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.core)

    testImplementation(libs.kotlinx.coroutines.test)
}
