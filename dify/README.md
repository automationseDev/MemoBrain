# MemoBrain Dify package

## 推奨DSL

- `MemoBrain_DifyOnly_v0.3.3.yml`: 正常系接続を修正したKnowledge補完・Geminiモデル自動切替版（推奨）
- `MemoBrain_DifyOnly_v0.3.2.yml`: Geminiモデル自動切替の初期版（正常系接続に不具合があるため非推奨）
- `MemoBrain_DifyOnly_v0.3.1.yml`: DuckDuckGo QueryとKnowledge該当なし分岐を修正した従来モデル版
- `MemoBrain_DifyOnly_v0.2.2.yml`: Web検索を行わない従来版

AndroidアプリとDify DSLは別バージョンで管理します。Android v1.1.0に更新しても、Dify DSLを必ず変更する必要はありません。

## v0.3.3の動作

1. 利用者の質問で既存Knowledgeを検索
2. 十分な根拠があればKnowledgeから回答
3. 根拠がなければDuckDuckGo SearchでWeb検索（最大5件）
4. 参照URLを残した日本語記事へ整理
5. 既存のMemoBrain Knowledgeへ自動登録
6. 登録した内容を回答

Knowledge候補が返っていても、回答生成が「該当なし」と判断した場合はWeb検索へ切り替えます。

## v0.3.3での修正

Difyの失敗分岐を有効にしたLLMノードでも、正常系の接続ハンドルは`source`です。v0.3.2で使用していた`success-branch`を`source`へ修正し、各モデルの成功後に出力アグリゲーターを経由して既存フローが継続するようにしました。

## Geminiモデル自動切替

意図解析、Knowledge回答、Web調査記事生成、画像、動画、文書、Webページ、YouTubeの8種類すべてのLLM処理で、次の順に実行します。

1. `gemini-3.6-flash`
2. `gemini-3.5-flash`
3. `gemini-2.5-flash`

429、503などでLLMノードが失敗すると、Difyの失敗分岐から次のモデルへ進みます。成功した最初のモデル出力だけを変数アグリゲーター経由で既存処理へ渡します。3モデルすべてが失敗した場合は、そのLLM処理をエラー終了します。

Gemini公式Difyプラグイン `0.9.5` 以降を使用してください。APIキーは利用者自身のDifyに設定し、公開DSLやGitHubへ含めないでください。

## インポート前の準備

Dify Marketplaceから次のプラグインをインストールまたは更新してください。

- `Gemini`（`langgenius/gemini`）: バージョン `0.9.5` 以降
- `DuckDuckGo Search`（`langgenius/duckduckgo`）

DuckDuckGo SearchにはAPI Keyは不要ですが、セルフホストDifyから外部Webへ到達できる必要があります。

インポート後に次の環境変数を設定します。

- `DIFY_API_BASE`: 自分のDify API Base（例: `https://your-dify-api.example.com/v1`）
- `KNOWLEDGE_API_KEY`: Dify Knowledge Service API Key
- `DATASET_NAME`: MemoBrainが利用するKnowledge名

公開DSLには実環境URLやAPI Keyを含めていません。

## データ送信上の注意

ナレッジ補完を使うと、質問の検索語句はDuckDuckGo Searchプラグインへ送信されます。検索結果は、Difyに設定したGeminiへ渡して記事化し、利用者自身のKnowledgeへ保存します。外部検索を避けたい場合はv0.2.2を使用するか、v0.3.3のWeb検索経路を無効化してください。
