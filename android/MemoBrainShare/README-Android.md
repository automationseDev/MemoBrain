# MemoBrain Android

MemoBrain Share は、Android の共有メニューからテキスト・URL・画像・動画・PDF/文書を Dify に送信し、アプリ内 WebView から Dify の公開Web Appを利用できるクライアントです。

> **重要: Dify が必須です。**
> このアプリ単体ではメモ保存機能は動作しません。利用者自身が用意した Dify 環境、対応 Chatflow、Dify App API Base、App API Key が必要です。

## プライバシー設計

- Dify API Base / API Key / Dify Web App URL は APK に含めない
- Dify接続情報は Android Keystore を利用して端末内で暗号化
- 共有テキスト・添付ファイルはバックグラウンド送信待ちの間だけアプリ専用領域へ AES-GCM 暗号化して保存
- 送信時のみ `cacheDir` に復号し、アップロード直後に削除
- 送信成功時に暗号化キューを削除し、最終失敗時は手動再送のため最大24時間だけ保持
- ネットワークが戻らない場合も最大24時間で送信待ちデータを削除する Cleanup Worker を登録
- Androidバックアップ / 端末移行バックアップを無効化
- HTTP平文通信を禁止し、Dify API Base / Dify Web App URL は HTTPS のみ許可
- `FLAG_SECURE` により共有内容・API設定・チャット画面のスクリーンショット/タスクスナップショットを抑止
- 通知本文にはメモ内容や Dify のレスポンス本文を表示しない
- Difyの返信は送信結果画面と暗号化された送信履歴で確認できる
- Difyエラー時もサーバーレスポンス本文を通知/ログへ出さない
- 画面プレビューは共有テキストの先頭500文字まで
- Google Mobile Ads SDK が追加する Android Advertising ID (`AD_ID`) 権限を manifest merge で削除

### 広告設計

MemoBrain はネイティブ AdMob バナーを使用しません。広告は開発者管理の案内用HTTPS Webページを表示する専用WebViewに配置し、利用者のDify公開Web Appは別のWebViewで直接表示します。

Android側では `MobileAds.registerWebView()` のために Google Mobile Ads SDK を残しています。これは Google の WebView API for Ads 連携用で、APK に AdMob App ID / Banner Ad Unit ID を埋め込むためではありません。

AdSenseのPublisher ID / Slot IDはAndroidビルド時に不要です。`web/memobrain-ad/config.js` をWebサーバー上だけで設定できます。このファイルはGit管理対象外です。

WebView API for Ads はネイティブアプリ側の同意状態をWeb広告へ自動伝搬しないため、Web広告の同意/CMPはWebページ側で処理してください。

### 重要な境界

端末内の一時データを残しにくくする設計ですが、MemoBrain の目的上、ユーザーが保存した内容は **設定した Dify に送信されます**。
Dify の Knowledge、利用するLLM/プラグイン、ログ、バックアップ等にどの程度データが残るかは、利用者自身の Dify 構成と各サービスのポリシーに依存します。

## Develop版

Debugビルドは `net.automationse.memobrainshare.dev`、表示名「MemoBrain Develop」として生成されます。正式版とは別アプリとして同時にインストールでき、接続設定・履歴も分離されます。ホーム画面ウィジェットは提供していません。

Develop版をアンインストールせず更新するため、`signing-secrets.properties` に設定した固定keystoreとaliasで署名します。正式版とDevelop版は `applicationId` が異なるため、同じ署名鍵を使用しても共存できます。

## Debug APK のビルド

ZIPは好きな場所へ展開できます。

```text
MemoBrainShare\Build-MemoBrainApk.cmd
```

をダブルクリックするか、PowerShell 5.1で以下を実行します。

```powershell
.\Build-MemoBrainApk.ps1
```

DebugビルドにもネイティブAdMob IDは不要です。
`Build-MemoBrainApk.ps1` では、既存Develop版を継続更新できるように署名設定が必須です。

USBデバッグを許可したAndroid端末へ、ビルド後にそのまま上書きインストールする場合:

```powershell
.\Build-MemoBrainApk.ps1 -Clean -Install
```

生成先:

```text
MemoBrainShare\output\MemoBrain-v1.4.0-develop-debug.apk
```

## 署名済み Release APK

正式配布版でローカル設定が必要なのは署名情報だけです。

`signing-secrets.properties.example` を `signing-secrets.properties` にコピーします。

```properties
MEMOBRAIN_KEYSTORE_PATH=C:/Users/yourname/Documents/AndroidKeys/MemoBrain-upload.jks
MEMOBRAIN_KEYSTORE_PASSWORD=your-keystore-password
MEMOBRAIN_KEY_ALIAS=memobrain-upload
MEMOBRAIN_KEY_PASSWORD=your-key-password
```

Windowsではkeystore pathを `/` 区切りにすると `.properties` のエスケープ問題を避けられます。

`signing-secrets.properties`、`.jks`、`.keystore` はGit管理対象外です。

ビルド:

```text
MemoBrainShare\Build-MemoBrainRelease.cmd
```

または:

```powershell
.\Build-MemoBrainRelease.ps1
```

成功すると:

```text
MemoBrainShare\output\MemoBrain-v1.4.0-release.apk
MemoBrainShare\output\SHA256SUMS.txt
```

を生成します。Android SDK の `apksigner.bat` を検出できた場合は署名検証も自動実行します。

詳しくは `docs/RELEASE_SIGNING.md` を参照してください。

## Release ビルドの安全チェック

Releaseビルドでは以下の場合にビルドを停止します。

- keystore path / password / alias / key password が不足している
- keystoreファイルが存在しない

ネイティブAdMobを廃止したため、AdMob App ID / Banner Ad Unit ID のチェックはありません。

## AIチャット / AdSense Webページ

1. `web/memobrain-ad/` を開発者管理のHTTPS Webサーバーへ配置します。
2. `config.example.js` をサーバー上で `config.js` としてコピーします。
3. レイアウト確認中は `showAdPlaceholder: true` にすると広告位置が見えます。
4. AdSenseを利用するときだけ `adsenseClient` / `adsenseSlot` をサーバー上の `config.js` に設定します。
5. MemoBrainの `Dify Web App URL` には、利用者自身のDify公開Web App URLを直接設定します。

広告ページとDifyは別WebViewです。広告ページの `config.js` にDify URLを設定せず、利用者のDify URLが広告ページへ送信されることもありません。

## 初回利用

1. アプリを起動
2. `Dify接続設定（必須）`
3. HTTPS の Dify API Base（例 `https://your-dify.example/v1`）を入力
4. Dify App API Key を入力
5. 必要に応じて Dify Web App URL を入力
6. Androidの共有メニューから MemoBrain を選択
7. `MemoBrainに保存` を押す

接続設定は端末内で暗号化され、削除ボタンから消去できます。

## 送信履歴・再送・重複防止

- 送信履歴画面で、送信待ち・送信中・成功・失敗・期限切れを確認できます。
- 最終失敗した送信は、暗号化済み送信データが残っている24時間以内に再送できます。
- 履歴はAndroid Keystoreを利用して暗号化し、Difyの返信を送信状態と合わせて確認できます。共有本文、URL、ファイル名は保存しません。
- 正規化したURLとファイル内容のSHA-256を使い、履歴にある同一URL・同一ファイルの重複登録を止めます。
- 履歴は最大100件・30日間です。履歴を消去すると重複判定情報も消去されます。

## ネイティブKnowledge管理

- `検索`: Dify App APIでKnowledgeを直接検索します。
- 情報不足時は `Webで調査してKnowledgeへ保存` を押し、確認後にGemini調査を実行できます。
- `ナレッジ一覧`: 保存したメモを一覧表示し、各メモの詳細を確認します。
- `TODO`: 未完了TODOを確認し、完了として更新できます。
- `あとで読む`: 未読メモを確認し、読了として更新できます。
- 画面上部でDify接続プロファイルを切り替えられます。
- ホーム画面および `アプリ情報` からアプリのバージョン・versionCode・アプリIDを確認できます。

## Gradle / JDK

Android Studio では `.idea/gradle.xml` の `#GRADLE_LOCAL_JAVA_HOME` を利用します。
固定のJDK絶対パスはコミットしていません。

Windowsの `Build-MemoBrainApk.ps1` は Gradle 9.5.1 を利用し、JDK 17以上を自動検出します。
