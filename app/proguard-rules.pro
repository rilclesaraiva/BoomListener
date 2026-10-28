# Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
# Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

# Regras do R8 (encolher e otimizar o APK de release). As bibliotecas
# (Compose, Media3, WorkManager, DataStore…) já trazem as regras delas.

# Os nomes do código não são embaralhados: o APK encolhe e fica otimizado do
# mesmo jeito, mas um erro no celular mostra classe e linha de verdade (daqui
# não dá para baixar o mapping.txt da Action e desembaralhar).
-dontobfuscate
-keepattributes SourceFile,LineNumberTable

# TagLib (leitura e gravação de tags): o código nativo (JNI) cria e chama
# estas classes pelo nome, coisa que o R8 não enxerga.
-keep class com.kyant.taglib.** { *; }
