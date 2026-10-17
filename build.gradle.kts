// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
//
// Só declara os plugins (apply false) para que todos os módulos usem as mesmas
// versões. A configuração de verdade fica nos plugins de convenção (build-logic).
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
