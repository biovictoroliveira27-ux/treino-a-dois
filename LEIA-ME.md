# Treino a Dois — app Android

Projeto Android pronto. O GitHub compila o APK de graça na nuvem; você não precisa instalar nada no computador.

## Gerar o APK (uma vez, ~10 min)

1. Crie uma conta grátis em github.com (se ainda não tiver).
2. Clique em **+ → New repository**. Nome: `treino-a-dois`. Marque **Public** ou **Private** e clique em **Create repository**.
3. Na página do repositório, clique em **uploading an existing file**. Descompacte o .zip no computador e arraste **todo o conteúdo da pasta `treino-a-dois`** (as pastas `app` e os arquivos soltos) para a janela. Clique em **Commit changes**.
4. Confira se a pasta `.github` subiu. Se ela não aparecer na lista de arquivos (no Mac ela fica oculta), crie à mão:
   **Add file → Create new file**, nome `.github/workflows/build.yml`, cole o conteúdo do arquivo `build.yml` (está no fim deste LEIA-ME) e clique em **Commit changes**.
5. Abra a aba **Actions**. Vai aparecer “Gerar APK” rodando (bolinha amarela). Em 3–5 minutos fica verde.
6. Na página inicial do repositório, à direita, clique em **Releases** e baixe **TreinoADois.apk**.
   Dica: abra esse link de Releases direto no celular para baixar lá.

## Instalar no celular

1. Abra o arquivo `TreinoADois.apk` baixado.
2. O Android vai pedir para **permitir instalar apps desta fonte** (Chrome ou Arquivos). Permita e toque em **Instalar**.
3. Se o Play Protect avisar que o app é desconhecido, toque em **Mais detalhes → Instalar assim mesmo**. O aviso aparece porque o app não veio da Play Store.

Mande o mesmo arquivo para o celular dela (WhatsApp, Drive) e instale do mesmo jeito.

## Bom saber

- Os registros ficam salvos **no próprio celular**. Cada um registra o seu treino no seu aparelho.
- Desinstalar o app ou limpar os dados dele apaga o histórico.
- A tela fica acesa enquanto o app está aberto.
- Para mudar o treino, edite `app/src/main/assets/index.html` no GitHub (ícone de lápis). Ao salvar, um APK novo é gerado em Releases. Instale por cima: o histórico continua.

## Conteúdo de `.github/workflows/build.yml`

```yaml
name: Gerar APK

on:
  push:
  workflow_dispatch:

permissions:
  contents: write

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: "17"

      - uses: gradle/actions/setup-gradle@v4
        with:
          gradle-version: "8.9"

      - name: Compilar
        run: gradle assembleDebug --no-daemon

      - name: Renomear
        run: cp app/build/outputs/apk/debug/app-debug.apk TreinoADois.apk

      - uses: actions/upload-artifact@v4
        with:
          name: TreinoADois-apk
          path: TreinoADois.apk

      - name: Publicar para download
        uses: softprops/action-gh-release@v2
        with:
          tag_name: v1.${{ github.run_number }}
          name: Treino a Dois ${{ github.run_number }}
          files: TreinoADois.apk
```
