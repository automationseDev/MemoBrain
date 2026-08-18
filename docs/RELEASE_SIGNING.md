# 署名済み Release APK の作成

MemoBrain の Release APK は、同じ `applicationId`、同じ署名鍵、増加する `versionCode` を維持して更新します。

ネイティブ AdMob は使用しないため、Release APK の作成に AdMob App ID / Banner Ad Unit ID は不要です。広告を利用する場合の AdSense 設定は Webサーバー側の `web/memobrain-ad/config.js` で行います。

## 1. 署名情報を設定

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

同じ設定はDevelop APKにも適用されます。正式版は `net.automationse.memobrainshare`、Develop版は `net.automationse.memobrainshare.dev` と別の `applicationId` なので共存できますが、それぞれを上書き更新するには、以前と同じkeystore・aliasと、より大きい `versionCode` が必要です。現在の `versionCode` は `12` です。

すでにインストール済みのDevelop版が別のdebug鍵で署名されている場合は、その元の鍵を指定することでアンインストールを避けられます。

```properties
MEMOBRAIN_DEVELOP_KEYSTORE_PATH=C:/Users/yourname/.android/debug.keystore
MEMOBRAIN_DEVELOP_KEYSTORE_PASSWORD=android
MEMOBRAIN_DEVELOP_KEY_ALIAS=androiddebugkey
MEMOBRAIN_DEVELOP_KEY_PASSWORD=android
```

Develop専用設定を省略すると正式版用の固定鍵が使われます。すでに配布されたAPKと異なる鍵へ変更すると、Androidの仕様上、上書き更新できません。

## 2. Release APK をビルド

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

内部では Gradle の `:app:assembleRelease` を実行します。Release ビルド時には次を検証します。

- Keystore path / password / alias / key password がすべて設定されていること
- 指定した keystore ファイルが存在すること

## 3. 出力

現在のブランチでは成功すると以下を生成します。

```text
android/MemoBrainShare/output/MemoBrain-v1.3.0-release.apk
android/MemoBrainShare/output/SHA256SUMS.txt
```

Android SDK の `apksigner.bat` を検出できる環境では、スクリプトが APK の署名検証も自動実行します。

正式リリース時には `versionName` / `versionCode` と出力ファイル名をリリース番号に合わせて更新してください。

## 4. AdSense は Webサーバー側で設定

Android Release APK に広告IDは注入しません。

`web/memobrain-ad/config.example.js` を参考に、実際のHTTPS配備先だけで `config.js` を作成します。

```javascript
window.MEMOBRAIN_AD_CONFIG = {
  adsenseClient: "ca-pub-...",
  adsenseSlot: "...",
  showAdPlaceholder: false
};
```

広告ページへDify URLを設定しません。利用者のDify公開Web App URLはAndroidアプリ内で暗号化保存します。

`config.js` はGit管理対象外です。AdSense値はAPKにもGitHubにも入れる必要がありません。

## 5. 環境変数 / Gradle property を使う場合

署名情報はローカル `.properties` ファイルの代わりに、次の名前を Gradle project property または環境変数として渡せます。

```text
MEMOBRAIN_KEYSTORE_PATH
MEMOBRAIN_KEYSTORE_PASSWORD
MEMOBRAIN_KEY_ALIAS
MEMOBRAIN_KEY_PASSWORD
```

これにより、将来 GitHub Actions Secrets を使った自動署名にも移行できます。

## 6. GitHub Actionsで更新可能なDevelop APKを配布する場合

GitHub Actionsのdebug workflowは、次のRepository secretsがすべて設定されている場合のみ、固定署名済みDevelop APKを成果物として公開します。

```text
MEMOBRAIN_KEYSTORE_BASE64
MEMOBRAIN_KEYSTORE_PASSWORD
MEMOBRAIN_KEY_ALIAS
MEMOBRAIN_KEY_PASSWORD
```

`MEMOBRAIN_KEYSTORE_BASE64` には、ローカルビルドで使用する同じkeystoreをBase64化した値を設定してください。固定署名が設定されていない場合、CIはビルド検証だけを行い、環境ごとに異なる署名のAPKは配布しません。keystoreやパスワードをリポジトリへコミットしないでください。
