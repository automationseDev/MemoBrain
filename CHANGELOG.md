# Changelog

## v1.3.0 - 2026-08-17

- Android app version 1.3.0 / versionCode 10（v1.2.0から同一署名で更新可能）
- 共有時の補足メモ、カテゴリ、タグ、重要度、あとで読む、TODO指定を追加
- 複数のDify接続プロファイルとKnowledge切り替えを追加
- 接続プロファイルをAndroid Keystore + AES-GCMで暗号化
- バックグラウンド再送を送信時の接続プロファイルへ固定
- ライト／ダークテーマ対応のモダンなカードUIへ刷新
- 読み込み不具合があったホーム画面ウィジェットを削除
- Dify DSL v0.3.6でAndroid共有メタデータの手動指定を優先
- 正式版と共存できる `net.automationse.memobrainshare.dev` のDebugビルドを維持

## v1.2.0 - 2026-08-17

- Android app version 1.2.0 / versionCode 9（同一applicationId・同一署名で更新可能）
- 暗号化した送信履歴、最終失敗時の24時間以内の手動再送を追加
- 正規化URLとファイルSHA-256による重複登録防止を追加
- Dify DSL v0.3.5でKnowledge不足時のWeb調査を明示確認式へ変更

- Dify DSL v0.3.4でDuckDuckGoを廃止し、Knowledge不足時のWeb検索・記事生成をGemini Google Search Groundingへ統合
- Dify DSL v0.3.3でGemini成功時の接続ハンドルを`success-branch`から`source`へ修正
- Dify DSL v0.3.2でGemini 3.6 Flash → 3.5 Flash → 2.5 Flashの自動フェイルオーバーを全LLM処理へ追加
- Dify DSL v0.3.1でDuckDuckGo Queryの空入力とKnowledge該当なし時のWeb検索未実行を修正
- Android app version 1.1.0 / versionCode 8（同一applicationId・同一署名でv1.0.0から更新可能）
- Dify DSL v0.3.0にKnowledge優先・DuckDuckGo Web検索フォールバック・記事化・Knowledge自動登録を追加
- 完全な空入力でKnowledgeへ投稿されない入力チェックを追加
- AIチャット画面を広告用WebViewと利用者Dify用WebViewへ分離
- 利用者のDify Web App URLを広告ページへ送信しない構成へ変更
- `web/memobrain-chat/` を広告・案内専用の `web/memobrain-ad/` へ置換
- AdSense設定はGit管理外の `web/memobrain-ad/config.js` に限定

## v1.0.0
- 初回正式リリース版へ移行
- Android app version 1.0.0 / versionCode 7
- GitHub / Galaxy Store 配布を見据えた公開構成を採用
- Dify 必須構成、暗号化保存、バックグラウンド送信、AdMob Release設定を維持
- `signing-secrets.properties` によるローカル署名設定を追加
- Release APK / AAB の Gradle signingConfig を追加
- Release時に AdMob設定と署名設定を検証し、不備がある場合はビルド停止
- `Build-MemoBrainRelease.ps1/.cmd` を追加
- Release APK生成時に `SHA256SUMS.txt` を作成
- Android SDKの `apksigner` を検出できる場合は署名を自動検証
- GitHub Releases と Samsung Galaxy Store へ同一署名APKを配布する手順を追加

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
