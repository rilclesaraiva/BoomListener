// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.

pluginManagement {
    // Plugins de convenção: a configuração compartilhada entre módulos mora aqui.
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "BoomListener"

include(":app")

include(":core:model")
include(":core:common")
include(":core:designsystem")
include(":core:database")
include(":core:datastore")
include(":core:network")
include(":core:player")

include(":data:library")
include(":data:tags")
include(":data:deezer")
include(":data:lastfm")
include(":data:releases")
include(":data:wellbeing")

include(":feature:library")
include(":feature:nowplaying")
include(":feature:artist")
include(":feature:tageditor")
include(":feature:releases")
include(":feature:settings")
include(":feature:wellbeing")
