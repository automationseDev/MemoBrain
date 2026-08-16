# MemoBrain

MemoBrain は、Android の共有メニューから URL、YouTube、テキスト、画像、PDF、文書、動画などを **自分で用意した Dify** に送り、個人用ナレッジとして整理・保存するセルフホスト向けアプリです。Dify の公開Web Appを Android アプリ内の WebView から開くAIチャット機能も備えます。

> **Dify は必須です。MemoBrain 単体では保存機能を利用できません。**
> 利用者自身の Dify 環境、MemoBrain 用 Dify DSL、Dify App API Key が必要です。

## 配布方針

- **GitHub Releases**: APK と Dify DSL のメイン配布先
- Android のネイティブ AdMob バナーは使用しない
- 広告は開発者管理の案内用HTTPS WebViewに表示し、利用者のDifyは別WebViewで直接開く
- AdSense Publisher / Slot ID は Android APK へ埋め込まず、Webサーバー側だけで設定できる

## リポジトリ構成

```text
MemoBrain/
├─ android/MemoBrainShare/       Android アプリソース
├─ dify/                         Dify DSL 配布パッケージ
├─ web/memobrain-ad/             案内 / AdSense専用Webページ
├─ docs/                         導入・公開・プライバシー資料
├─ scripts/                      Dify 疎通確認ツール
├─ .gitignore
├─ CHANGELOG.md
└─ README.md
```

## クイックスタート

1. Dify を用意します。
2. `dify/MemoBrain_DifyOnly_v0.3.1.yml` を Dify にインポートします。ナレッジ不足時のWeb検索を使う場合は、Dify Marketplaceから DuckDuckGo Search プラグインを追加します。
3. Dify 側の環境変数 `DIFY_API_BASE`、`KNOWLEDGE_API_KEY`、`DATASET_NAME` を設定します。
4. MemoBrain の APK をインストールします。
5. MemoBrain の「Dify接続設定」に Dify App API Base URL と App API Key を設定します。
6. 利用者自身のDify公開Web App URLを `Dify Web App URL` に設定します。広告ページへDify URLは送信されません。
7. Android の共有メニューから MemoBrain を選択して保存します。AIチャットではまずKnowledgeを検索し、不足時はWeb検索から記事を生成してKnowledgeへ自動登録できます。

詳細は [Difyセットアップ](docs/DIFY_SETUP.md)、[Android導入手順](docs/INSTALL_ANDROID.md)、[Dify Web Chat / AdSense](docs/DIFY_WEB_CHAT.md) を参照してください。

## ナレッジ補完エージェント

Dify DSL v0.3.1では、質問に対して既存Knowledgeを先に検索します。検索結果がないか関連度が低い場合、DuckDuckGo SearchでWebを検索し、参照URL付きの日本語記事へ整理して同じKnowledgeへ登録したうえで回答します。検索語句はDuckDuckGoプラグインへ、検索結果と生成対象は設定したLLMへ送信されます。この外部検索はAIチャットから質問した場合にだけ動作し、Android共有による通常保存の経路は従来どおりです。

## 必要環境

- Android 8.0 (API 26) 以上
- Dify（セルフホストまたは利用者自身が管理する環境）
- Dify App API Key
- Dify Knowledge Service API Key
- HTTPS で到達可能な Dify API URL

## プライバシー設計

- Dify 接続 URL / App API Key / Dify Web App URL は Android Keystore を利用して暗号化保存
- 送信待ちテキスト・共有ファイルはアプリ専用領域で AES-GCM 暗号化
- 送信成功または最終失敗後に送信待ちデータを削除
- 未送信データも最大24時間で削除
- Android バックアップ / データ移行バックアップを無効化
- 平文 HTTP 接続を拒否
- 共有内容をタスクスナップショットへ残しにくくする `FLAG_SECURE` を使用
- 通知にメモ本文や Dify レスポンス本文を表示しない
- Google Mobile Ads SDK由来の Advertising ID (`AD_ID`) 権限を削除

ただし、保存した内容は利用者自身が設定した Dify 環境へ送信・保存されます。Dify 側に保存されたデータは MemoBrain のアンインストールでは削除されません。

詳しくは [プライバシーポリシー](docs/PRIVACY_POLICY_JA.md) を参照してください。

## WebView AdSense

MemoBrain はネイティブ AdMob バナーを使用しません。Google Mobile Ads SDK は `MobileAds.registerWebView()` による WebView API for Ads のためだけに残しています。

広告は `web/memobrain-ad/` をHTTPSで配備した開発者管理WebViewに表示します。利用者自身のDify公開Web Appは別WebViewで直接開くため、Dify URLが広告ページへ送信されることはありません。

```javascript
window.MEMOBRAIN_AD_CONFIG = {
  adsenseClient: "ca-pub-...",
  adsenseSlot: "...",
  showAdPlaceholder: false
};
```

`config.js` は `.gitignore` 対象です。広告IDをAPKへ埋め込む必要はなく、Dify URLを `config.js` に設定してはいけません。

## Android ビルド

Debug APK は Windows で `android/MemoBrainShare/Build-MemoBrainApk.cmd` を実行できます。

署名済み Release APK は署名情報をローカル設定したうえで次を実行します。

```text
android/MemoBrainShare/Build-MemoBrainRelease.cmd
```

Release ビルドでは `signing-secrets.properties` から keystore path / password / alias / key password を読み込みます。秘密鍵やパスワードは GitHub にコミットしません。

詳細は [署名済みRelease APKの作成](docs/RELEASE_SIGNING.md) を参照してください。

Android Studio で開く場合は `android/MemoBrainShare` を直接 Open してください。

## 公開時にコミットしないもの

- Dify App API Key
- Dify Knowledge Service API Key
- `signing-secrets.properties`
- `web/memobrain-ad/config.js`
- `.jks` / `.keystore`
- 署名パスワード
- `local.properties`

## ライセンス

現時点ではオープンソースライセンスを設定していません。正式な利用・改変・再配布条件は公開前に決定予定です。
