// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.core.common

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings

/**
 * Permissões de mídia do app. Só pedimos ÁUDIO: as capas vêm de dentro dos
 * arquivos de música e do próprio MediaStore de áudio, que essa permissão já
 * cobre. Fotos e vídeos ficam de fora de propósito (ver CLAUDE.md).
 */
object MediaPermissions {

    /** A permissão de leitura de música certa para a versão do Android. */
    val readAudio: String
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

    fun hasReadAudio(context: Context): Boolean =
        context.checkSelfPermission(readAudio) == PackageManager.PERMISSION_GRANTED
}

/** Abre a tela "Informações do app" do Android, onde o usuário libera permissões. */
fun Context.openAppSettings() {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", packageName, null),
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    startActivity(intent)
}
