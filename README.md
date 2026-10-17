# Boom, listener!

Reprodutor de música **local** para Android, gratuito, sem anúncios e sem conta.

> 🚧 Em construção. A versão atual é só o esqueleto do app.

## O que ele vai fazer

- Tocar as músicas que estão no seu aparelho, com visual **Material 3 Expressive**
- **Editar as tags** completas: título, artista, álbum, ano, gênero, capa e letra
- **Página de artista** com foto, biografia e discografia, marcando o que você já tem e o que falta
- **Radar de lançamentos:** siga artistas e receba aviso quando sair música nova
- **Bem-estar:** estatísticas de escuta e avisos leves de pausa

O app **nunca toca** nada de serviços externos. Deezer e Last.fm servem só para trazer informações, e os lançamentos têm um botão para abrir no serviço que você preferir.

## Privacidade

Sua biblioteca, seus artistas seguidos e suas estatísticas ficam **só no seu aparelho**. O app consulta o Deezer e o Last.fm apenas para buscar informações públicas de artistas e álbuns, e só envia o que você ouve ao Last.fm (scrobble) se você ativar.

## Tecnologia

Kotlin, Jetpack Compose e Material 3, divididos em módulos (`core`, `data`, `feature`). Veja [`docs/ARQUITETURA.md`](docs/ARQUITETURA.md). O APK é gerado automaticamente pelo GitHub Actions a cada atualização.

## Licença

[GNU GPL v3](LICENSE). Código escrito com auxílio de IA, sob direção e revisão do autor.
