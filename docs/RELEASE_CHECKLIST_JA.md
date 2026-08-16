# MemoBrain Google Play 公開チェックリスト

## 1. アプリ

- [ ] Dify接続先が初期値としてAPKに入っていない
- [ ] Dify API KeyがAPKに入っていない
- [ ] Debug版でテスト広告が表示できる
- [ ] Release版に正式AdMob App ID / Banner IDを設定
- [ ] アプリ内に広告ID編集UIが存在しない
- [ ] UMP同意フォームを実機確認
- [ ] 必要な場合「広告のプライバシー設定」ボタンが表示される
- [ ] テキスト/URL/画像/PDF/動画のバックグラウンド保存を確認
- [ ] 保存後に端末内 `pending_jobs` が削除されることを確認
- [ ] オフライン保存後、再接続で送信されることを確認
- [ ] 24時間Cleanup Workerの設計を確認

## 2. Dify必須の表示

- [ ] Google Playの詳細説明に「Dify必須」「アプリ単体では利用不可」を明記
- [ ] アプリ起動画面にもDify必須を表示
- [ ] 初回セットアップ手順を掲載
- [ ] 対応Dify DSL/Chatflowの導入方法を公開ページに用意

## 3. プライバシー

- [ ] `PRIVACY_POLICY_JA.md` の `[開発者名]` を置換
- [ ] `[連絡先メールアドレス]` を置換
- [ ] プライバシーポリシーをHTTPSでWeb公開
- [ ] Google Play ConsoleへプライバシーポリシーURLを登録
- [ ] Data safetyを現在のAdMob/Dify構成に合わせて回答
- [ ] Difyで利用している外部LLM/プラグインのデータ送信先も確認

## 4. AdMob

- [ ] `https://automationse.net/app-ads.txt` がHTTP 200で取得できる
- [ ] AdMobでapp-ads.txt確認済み
- [ ] AdMobの「プライバシーとメッセージ」を設定
- [ ] 正式版ではテスト広告IDを使っていない

## 5. Releaseビルド

- [ ] `release-secrets.properties.example` をコピーして `release-secrets.properties` を作成
- [ ] 正式AdMob App IDを設定
- [ ] 正式Banner Ad Unit IDを設定
- [ ] `release-secrets.properties` がGit管理外であることを確認
- [ ] keystore / パスワードをGitや配布ZIPへ入れていない
- [ ] Android StudioでSigned Android App Bundle (AAB)を生成
- [ ] versionCodeが過去リリースより大きい

## 6. Play Console

- [ ] ストア掲載情報
- [ ] アプリアイコン
- [ ] スクリーンショット
- [ ] コンテンツのレーティング
- [ ] 広告ありの申告
- [ ] Data safety
- [ ] プライバシーポリシー
- [ ] 対象ユーザー/コンテンツ
- [ ] テストトラックで実機確認後に製品版へ

## GitHub + Galaxy Store

- [ ] GitHub Release と Galaxy Store に同じ署名 APK を使用
- [ ] `versionCode` を前回より増加
- [ ] GitHub Release に Dify DSL を添付
- [ ] GitHub Release の SHA256SUMS を作成
- [ ] `.jks` / パスワード / API Key / `release-secrets.properties` が成果物に含まれていない
