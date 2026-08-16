# Changelog

## v0.4.1
- Android StudioのGradle JVM 11問題を対策
- `.idea/gradle.xml` に `#GRADLE_LOCAL_JAVA_HOME` を設定
- JDKの絶対パスを埋め込まず、展開場所非依存を維持
- Java sourceCompatibility / targetCompatibility は17のまま
- Android app version 0.4.1 / versionCode 6

## v0.4.0

### Privacy / security
- Dify API Base / API Key をAndroid Keystoreで暗号化
- 既存v0.3.xの平文Dify Baseを初回読込時に暗号化へ移行
- 共有テキスト/ファイルをAES-GCM暗号化した送信キューへ保存
- 送信処理中のみcacheDirへ復号し即時削除
- 成功/最終失敗で送信キュー削除
- 24時間のCleanup Workerを追加
- 起動時にも期限切れキューを掃除
- Android backup / device transferを無効化
- HTTPSのDify接続のみ許可
- HTTPリダイレクトを自動追従しない
- Difyエラーレスポンス本文を通知/独自ログへ露出しない
- 通知本文を汎用メッセージ化
- FLAG_SECUREを追加
- 共有内容の画面プレビューを500文字に制限
- IME personalized learningを抑止

### Ads
- アプリ内のAdMob ID編集機能を削除
- 広告ON/OFF設定を削除し、正式版はビルド時に組み込んだ広告のみ使用
- DebugはGoogle公式テスト広告ID固定
- Releaseは正式App ID/Banner ID必須
- Releaseで未設定/テストID/形式不正の場合はビルド失敗
- release-secrets.propertiesをgitignore
- AD_ID permissionをmanifest mergeから削除
- UMP privacy options入口はREQUIRED時のみ表示

### Publication
- versionName 0.4.0 / versionCode 5
- Dify必須である旨をアプリ画面・README・Google Play掲載文案へ明記
- PRIVACY_POLICY_JA.mdを追加
- PLAY_STORE_LISTING_JA.mdを追加
- DATA_SAFETY_JA.mdを追加

## Public repository preparation
- GitHub + Galaxy Store 配布構成へ整理
- Dify DSL の公開既定URLを example.com に変更
- Gradle bootstrap を 9.5.1 に更新
- ビルドスクリプト内のバージョン表示を 0.4.1 に統一
- 公開用 README / Dify / Galaxy Store / GitHub Release / Security 文書を追加
- 実API Key・署名鍵・本番AdMob IDは非コミット方針
