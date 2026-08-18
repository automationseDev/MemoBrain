# MemoBrain 公開チェックリスト

## 1. アプリ

- [ ] applicationIdが `net.automationse.memobrainshare` のまま
- [ ] versionNameが `1.3.1`、versionCodeが `12` 以上
- [ ] v1.0.0と同じ正式署名でAPKを生成
- [ ] Dify接続先、API Key、広告IDがAPKに入っていない
- [ ] テキスト/URL/画像/PDF/動画のバックグラウンド保存を確認
- [ ] 送信履歴とDify返信が暗号化され、共有本文・URL・ファイル名が履歴に保存されない
- [ ] 最終失敗した送信を24時間以内に履歴から再送できる
- [ ] 同一URL（追跡パラメータ違いを含む）と同一ファイルが重複登録されない
- [ ] 完全な空入力がエラーになりKnowledgeへ登録されない
- [ ] Difyチャット、ファイル添付、Android戻る操作を確認
- [ ] 広告WebViewと利用者Dify WebViewが分離されている
- [ ] 送信成功時と24時間経過時の一時データ削除を確認

## 2. Dify DSL v0.3.7

- [ ] `MemoBrain_DifyOnly_v0.3.7.yml` をテスト環境へインポート
- [ ] Gemini公式Difyプラグイン `0.9.5` 以降をインストール
- [ ] DuckDuckGoノード・DuckDuckGo Searchプラグインへの依存がない
- [ ] Knowledgeにある質問がGroundingなしで回答される
- [ ] Knowledgeにない質問では確認メッセージを表示し、Groundingを実行しない
- [ ] `Web調査: 調べたい内容` と送信した場合だけGemini Groundingで調査・記事化・自動登録される
- [ ] Gemini 3.6 Flash成功時に後続処理へ進む
- [ ] 失敗時に3.6 Flash → 3.5 Flash → 2.5 Flashの順で切り替わる
- [ ] 記事に参照URLが含まれる
- [ ] 検索失敗時に秘密情報やサーバーレスポンス本文が表示されない

## 3. Web広告

- [ ] `web/memobrain-ad/` をHTTPSで配備
- [ ] 本番 `config.js` はGit管理外
- [ ] AdSense Publisher / Slot IDはWebサーバー側だけに設定
- [ ] Dify URLを広告ページや `config.js` に設定していない
- [ ] Web側の同意管理/CMPを対象地域に合わせて設定
- [ ] 広告だけのページにせず、案内・プライバシー等の有用な内容を掲載

## 4. Releaseビルド

- [ ] `signing-secrets.properties` の署名情報をローカルだけに設定
- [ ] keystore / password / API Key / token / 本番config.jsがGitや成果物に含まれない
- [ ] `Build-MemoBrainRelease.cmd` で署名済みAPKを生成
- [ ] apksignerで署名を検証
- [ ] SHA256SUMSを生成
- [ ] Android Debug Buildが成功
- [ ] Android Release Smoke Buildが成功

## 5. 配布

- [ ] GitHub Releaseへ署名APK、Dify DSL、SHA256SUMSを添付
- [ ] リリースノートにDify必須・Web検索時の外部送信を明記
- [ ] プライバシーポリシーをHTTPSで公開
- [ ] mainへのマージ・正式Release公開は最終確認後に実施
