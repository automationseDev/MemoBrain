# MemoBrain

MemoBrain は、Android の共有メニューから URL、YouTube、テキスト、画像、PDF、文書、動画などを **自分で用意した Dify** に送り、個人用ナレッジとして整理・保存するセルフホスト向けアプリです。Dify の公開Web Appを Android アプリ内の WebView から開くAIチャット機能も備えます。

> **Dify は必須です。MemoBrain 単体では保存機能を利用できません。**
> 利用者自身の Dify 環境、MemoBrain 用 Dify DSL、Dify App API Key が必要です。

## ダウンロード

正式版は **[GitHub Releases](https://github.com/automationseDev/MemoBrain/releases/latest)** だけで配布します。

- AndroidにはReleaseページの署名済みAPKをインストールします
- Difyには同じReleaseページの推奨DSLをインポートします
- GitHub以外のアプリストアでは配布していません
- APKのSHA-256はReleaseに添付する `SHA256SUMS.txt` で確認できます

## 配布・広告方針

- AndroidのネイティブAdMobバナーは使用しない
- 広告は開発者管理の案内用HTTPS WebViewに表示し、利用者のDifyは別WebViewで直接開く
- AdSense Publisher / Slot IDはAndroid APKへ埋め込まず、Webサーバー側だけで設定できる

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
2. `dify/MemoBrain_DifyOnly_v0.3.8.yml` を Dify にインポートします。Gemini公式Difyプラグイン `0.9.5` 以降が必要です。DuckDuckGo Searchプラグインは不要です。
3. Dify 側の環境変数 `DIFY_API_BASE`、`KNOWLEDGE_API_KEY`、`DATASET_NAME` を設定します。
4. MemoBrain の APK をインストールします。
5. MemoBrain の「Dify接続設定」に Dify App API Base URL と App API Key を設定します。
6. 利用者自身のDify公開Web App URLを `Dify Web App URL` に設定します。広告ページへDify URLは送信されません。
7. Android の共有メニューから MemoBrain を選択して保存します。AIチャットではまずKnowledgeを検索し、不足時はWeb検索から記事を生成してKnowledgeへ自動登録できます。

詳細は [Difyセットアップ](docs/DIFY_SETUP.md)、[Android導入手順](docs/INSTALL_ANDROID.md)、[Dify Web Chat / AdSense](docs/DIFY_WEB_CHAT.md) を参照してください。

## Androidの送信管理

- 共有画面は「すぐ保存」を維持し、必要な場合だけ詳細欄を展開
- 補足メモ、カテゴリ、タグ、重要度、あとで読む、TODOを送信前に指定可能（空欄はAI自動分類）
- 複数のDify接続プロファイルを登録し、共有ごとに保存先Knowledgeを選択
- プロファイルのAPI URL / API Key / Web App URLはAndroid Keystore + AES-GCMで暗号化
- 各プロファイルは対象Knowledgeを設定したDify App API Keyと組にして登録（Knowledge名は識別名として送信メタデータにも付加）
- バックグラウンドジョブは送信時のプロファイルIDに固定され、後から選択先を変えても誤送信しない
- 送信履歴で送信待ち・送信中・成功・失敗・期限切れを確認
- 失敗した送信は、暗号化済みデータが残る24時間以内に手動再送
- URL正規化とファイルSHA-256による重複登録防止
- 履歴とDify返信は端末内で暗号化し、共有本文・URL・ファイル名は履歴に保存しない
- WebViewを開かずにKnowledge検索・ナレッジ一覧・TODO・あとで読むを利用可能
- 閲覧、詳細、キーワード/カテゴリ/タグ検索、TODO完了、読了は構造化actionで分岐し、Geminiを呼び出さずに実行
- Knowledgeに情報がない場合、確認付きボタンからWeb調査とKnowledge保存を実行
- ホーム画面とアプリ情報からバージョン、versionCode、ビルド種別を確認可能

## ナレッジ補完エージェント

Dify DSL v0.3.8では、共有時の手動メタデータを優先しつつ、質問に対して既存Knowledgeを先に検索します。Androidから `action` が指定された閲覧・管理操作は非AI経路で処理します。通常の質問で検索結果がないか関連度が低い場合は一度停止して確認を表示し、利用者が `Web調査: 調べたい内容` と送信した場合だけGeminiのGoogle Search GroundingでWebを調査します。

### Geminiモデル自動切替

LLM処理は `Gemini 3.6 Flash → Gemini 3.5 Flash → Gemini 2.5 Flash` の順で実行します。429、503などで上位モデルのノードが失敗した場合だけ次のモデルへ進み、成功した最初の出力を既存処理へ渡します。3モデルを利用するにはGemini公式Difyプラグイン `0.9.5` 以降が必要です。

## 必要環境

- Android 8.0 (API 26) 以上
- Dify（セルフホストまたは利用者自身が管理する環境）
- Dify App API Key
- Dify Knowledge Service API Key
- HTTPS で到達可能な Dify API URL

## プライバシー設計

- Dify 接続 URL / App API Key / Dify Web App URL は Android Keystore を利用して暗号化保存
- 送信待ちテキスト・共有ファイルはアプリ専用領域で AES-GCM 暗号化
- 送信成功後に送信待ちデータを削除し、最終失敗時は再送用として最大24時間だけ暗号化保持
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

## Develop版

Debug APKは正式版と競合しない開発専用パッケージです。

- applicationId: `net.automationse.memobrainshare.dev`
- アプリ名: `MemoBrain Develop`
- versionName: `1.4.0-develop`
- versionCode: `13`
- 正式版と同時インストール可能
- `signing-secrets.properties` の固定署名鍵を使用してアンインストールせず更新可能
- カード型のモダンな共有画面UIを利用可能

ReleaseビルドのapplicationIdは従来どおり `net.automationse.memobrainshare` です。

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
