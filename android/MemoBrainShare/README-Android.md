# MemoBrain Android v1.0.0

MemoBrain Share は、Android の共有メニューからテキスト・URL・画像・動画・PDF/文書を Dify に送信するためのクライアントです。

> **重要: Dify が必須です。**
> このアプリ単体ではメモ保存機能は動作しません。利用者自身が用意した Dify 環境、対応 Chatflow、Dify App API Base、App API Key が必要です。

## v1.0.0 のプライバシー設計

- Dify API Base / API Key は APK に含めない
- Dify接続情報は Android Keystore を利用して端末内で暗号化
- 共有テキスト・添付ファイルはバックグラウンド送信待ちの間だけアプリ専用領域へ AES-GCM 暗号化して保存
- 送信時のみ `cacheDir` に復号し、アップロード直後に削除
- 送信成功または最終失敗時に暗号化キューを削除
- ネットワークが戻らない場合も最大24時間で送信待ちデータを削除する Cleanup Worker を登録
- Androidバックアップ / 端末移行バックアップを無効化
- HTTP平文通信を禁止し、Dify API Base は HTTPS のみ許可
- `FLAG_SECURE` により共有内容・API設定画面のスクリーンショット/タスクスナップショットを抑止
- 通知本文にはメモ内容や Dify のレスポンス本文を表示しない
- Difyエラー時もサーバーレスポンス本文を通知/ログへ出さない
- 画面プレビューは共有テキストの先頭500文字まで
- Google Mobile Ads SDK が追加する Android Advertising ID (`AD_ID`) 権限を manifest merge で削除
- 広告IDのユーザー変更機能は廃止。正式版の広告IDはビルド時のみ組み込み

### 重要な境界

端末内の一時データを残しにくくする設計ですが、MemoBrain の目的上、ユーザーが保存した内容は **設定した Dify に送信されます**。
Dify の Knowledge、利用するLLM/プラグイン、ログ、バックアップ等にどの程度データが残るかは、利用者自身の Dify 構成と各サービスのポリシーに依存します。

また、AdMob SDK 自体によるデータ処理はゼロにはできません。ストア申告時は、使用中の Google Mobile Ads SDK / UMP SDK の最新公式開示を確認してください。

## Debug APK のビルド

ZIPは好きな場所へ展開できます。

```text
MemoBrainShare\Build-MemoBrainApk.cmd
```

をダブルクリックするか、PowerShell 5.1で以下を実行します。

```powershell
.\Build-MemoBrainApk.ps1
```

Debugビルドは必ずGoogle公式テスト広告IDを使用します。

生成先:

```text
MemoBrainShare\output\MemoBrain-v1.0.0-debug.apk
```

## 署名済み Release APK

正式配布版では、AdMob ID と署名情報をローカルファイルから読み込みます。利用者が広告IDや署名情報を変更する画面はありません。

### 1. AdMob

`release-secrets.properties.example` を `release-secrets.properties` にコピーします。

```properties
MEMOBRAIN_ADMOB_APP_ID=ca-app-pub-xxxxxxxxxxxxxxxx~xxxxxxxxxx
MEMOBRAIN_ADMOB_BANNER_ID=ca-app-pub-xxxxxxxxxxxxxxxx/xxxxxxxxxx
```

### 2. 署名情報

`signing-secrets.properties.example` を `signing-secrets.properties` にコピーします。

```properties
MEMOBRAIN_KEYSTORE_PATH=C:/Users/yourname/Documents/AndroidKeys/MemoBrain-upload.jks
MEMOBRAIN_KEYSTORE_PASSWORD=your-keystore-password
MEMOBRAIN_KEY_ALIAS=memobrain-upload
MEMOBRAIN_KEY_PASSWORD=your-key-password
```

Windowsではkeystore pathを `/` 区切りにすると `.properties` のエスケープ問題を避けられます。

`release-secrets.properties`、`signing-secrets.properties`、`.jks`、`.keystore` はGit管理対象外です。

### 3. ビルド

```text
MemoBrainShare\Build-MemoBrainRelease.cmd
```

または:

```powershell
.\Build-MemoBrainRelease.ps1
```

成功すると:

```text
MemoBrainShare\output\MemoBrain-v1.0.0-release.apk
MemoBrainShare\output\SHA256SUMS.txt
```

を生成します。Android SDK の `apksigner.bat` を検出できた場合は署名検証も自動実行します。

GitHub Releases と Samsung Galaxy Store には、この同じ Release APK を使用します。更新時も同じ keystore / alias を使い、`versionCode` を必ず増やしてください。

詳しくは `docs/RELEASE_SIGNING.md` を参照してください。

## Release ビルドの安全チェック

Releaseビルドでは以下の場合にビルドを停止します。

- AdMob App IDが未設定
- Banner Ad Unit IDが未設定
- Google公式テスト広告IDが設定されている
- AdMob ID形式が不正
- keystore path / password / alias / key password が不足している
- keystoreファイルが存在しない

これにより、テスト広告や未署名APKを正式版として誤配布しにくくしています。

## AABを作る場合

Android Studioの **Build > Generate Signed App Bundle / APK** を使う方法に加え、同じ `signing-secrets.properties` を設定済みならGradleの `bundleRelease` でも同じ署名構成を利用できます。

署名鍵、keystore、パスワードはこのプロジェクト/ZIPへ保存しないでください。

## 初回利用

1. アプリを起動
2. `Dify接続設定（必須）`
3. HTTPS の Dify API Base（例 `https://your-dify.example/v1`）を入力
4. Dify App API Key を入力
5. Androidの共有メニューから MemoBrain を選択
6. `MemoBrainに保存` を押す

接続設定は端末内で暗号化され、削除ボタンから消去できます。

## Gradle / JDK

Android Studio では `.idea/gradle.xml` の `#GRADLE_LOCAL_JAVA_HOME` を利用します。
固定のJDK絶対パスはコミットしていません。

Windowsの `Build-MemoBrainApk.ps1` は Gradle 9.5.1 を利用し、JDK 17以上を自動検出します。
