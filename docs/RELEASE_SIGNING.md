# 署名済み Release APK の作成

MemoBrain の GitHub Releases と Samsung Galaxy Store には、同じ `applicationId`、同じ `versionCode`、同じ署名鍵で作成した同一 Release APK を使用します。

## 1. 正式 AdMob ID を設定

`android/MemoBrainShare/release-secrets.properties.example` をコピーして、次のローカルファイルを作成します。

```text
android/MemoBrainShare/release-secrets.properties
```

内容:

```properties
MEMOBRAIN_ADMOB_APP_ID=ca-app-pub-xxxxxxxxxxxxxxxx~xxxxxxxxxx
MEMOBRAIN_ADMOB_BANNER_ID=ca-app-pub-xxxxxxxxxxxxxxxx/xxxxxxxxxx
```

このファイルは `.gitignore` 対象です。

## 2. 署名情報を設定

`android/MemoBrainShare/signing-secrets.properties.example` をコピーして、次のローカルファイルを作成します。

```text
android/MemoBrainShare/signing-secrets.properties
```

例:

```properties
MEMOBRAIN_KEYSTORE_PATH=C:/Users/yourname/Documents/AndroidKeys/MemoBrain-upload.jks
MEMOBRAIN_KEYSTORE_PASSWORD=your-keystore-password
MEMOBRAIN_KEY_ALIAS=memobrain-upload
MEMOBRAIN_KEY_PASSWORD=your-key-password
```

Windows のパスは `/` を使うと `.properties` のバックスラッシュエスケープを避けられます。相対パスも使用できますが、秘密鍵そのものはプロジェクト配下へ置かない運用を推奨します。

`signing-secrets.properties`、`.jks`、`.keystore` はすべて Git の除外対象です。

## 3. Release APK をビルド

Windows では以下を実行します。

```text
android/MemoBrainShare/Build-MemoBrainRelease.cmd
```

または PowerShell 5.1 で:

```powershell
cd android\MemoBrainShare
.\Build-MemoBrainRelease.ps1
```

クリーンビルドする場合:

```powershell
.\Build-MemoBrainRelease.ps1 -Clean
```

内部では Gradle の `:app:assembleRelease` を実行します。Release ビルド時には次の両方を検証します。

- 正式 AdMob App ID / Banner Ad Unit ID が設定されていること
- Keystore path / password / alias / key password がすべて設定されていること

未設定、Google テスト広告 ID、ダミー広告 ID、存在しない keystore の場合はビルドを停止します。

## 4. 出力

成功すると以下を生成します。

```text
android/MemoBrainShare/output/MemoBrain-v1.0.0-release.apk
android/MemoBrainShare/output/SHA256SUMS.txt
```

Android SDK の `apksigner.bat` を検出できる環境では、スクリプトが APK の署名検証も自動実行します。

## 5. 配布

同じ `MemoBrain-v1.0.0-release.apk` を次の両方へ使用します。

- GitHub Releases
- Samsung Galaxy Store

異なる署名鍵で再ビルドした APK を片方だけへ出さないでください。更新時も同じ keystore / alias を使い、`versionCode` を必ず増やします。

## 環境変数 / Gradle property を使う場合

ローカル `.properties` ファイルの代わりに、次の名前を Gradle project property または環境変数として渡すこともできます。

```text
MEMOBRAIN_ADMOB_APP_ID
MEMOBRAIN_ADMOB_BANNER_ID
MEMOBRAIN_KEYSTORE_PATH
MEMOBRAIN_KEYSTORE_PASSWORD
MEMOBRAIN_KEY_ALIAS
MEMOBRAIN_KEY_PASSWORD
```

これにより、将来 GitHub Actions の Secrets を使った自動署名にも移行できます。
