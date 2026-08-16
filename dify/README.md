# MemoBrain Dify package

## 推奨DSL

- `MemoBrain_DifyOnly_v0.3.4.yml`: Gemini Google Search GroundingによるKnowledge補完・Geminiモデル自動切替版（推奨）
- `MemoBrain_DifyOnly_v0.3.3.yml`: DuckDuckGo Searchを使う旧版
- `MemoBrain_DifyOnly_v0.3.2.yml`: Geminiモデル自動切替の初期版（正常系接続に不具合があるため非推奨）
- `MemoBrain_DifyOnly_v0.2.2.yml`: Web検索を行わない従来版

AndroidアプリとDify DSLは別バージョンで管理します。Android v1.1.0に更新しても、Dify DSLを必ず変更する必要はありません。

## v0.3.4の動作

1. 利用者の質問で既存Knowledgeを検索
2. 十分な根拠があればKnowledgeから回答
3. 根拠がなければGemini Google Search GroundingでWebを調査
4. 参照URLを残した日本語記事へ整理
5. 既存のMemoBrain Knowledgeへ自動登録
6. 登録した内容を回答

Knowledge候補が返っていても、回答生成が「該当なし」と判断した場合はGroundingへ切り替えます。DuckDuckGoノードとDuckDuckGo Searchプラグインへの依存はありません。

## Geminiモデル自動切替

意図解析、Knowledge回答、Web調査記事生成、画像、動画、文書、Webページ、YouTubeの8種類すべてのLLM処理で、次の順に実行します。

1. `gemini-3.6-flash`
2. `gemini-3.5-flash`
3. `gemini-2.5-flash`

429、503などでLLMノードが失敗すると、Difyの失敗分岐から次のモデルへ進みます。成功した最初のモデル出力だけを変数アグリゲーター経由で既存処理へ渡します。3モデルすべてが失敗した場合は、そのLLM処理をエラー終了します。Web調査記事生成ノードでは3モデルすべてでGoogle Search Groundingを有効にしています。

Gemini公式Difyプラグイン `0.9.5` 以降を使用してください。APIキーは利用者自身のDifyに設定し、公開DSLやGitHubへ含めないでください。

## インポート前の準備

Dify Marketplaceから `Gemini`（`langgenius/gemini`）プラグイン `0.9.5` 以降をインストールまたは更新してください。DuckDuckGo Searchプラグインは不要です。

インポート後に次の環境変数を設定します。

- `DIFY_API_BASE`: 自分のDify API Base（例: `https://your-dify-api.example.com/v1`）
- `KNOWLEDGE_API_KEY`: Dify Knowledge Service API Key
- `DATASET_NAME`: MemoBrainが利用するKnowledge名

公開DSLには実環境URLやAPI Keyを含めていません。

## データ送信と利用枠の注意

ナレッジ補完を使うと、検索語句、取得情報、生成対象が、Difyに設定したGeminiサービスへ送信され、Google Search Groundingで処理されます。生成した記事と参照URLは利用者自身のKnowledgeへ保存します。データの取扱いと料金・利用上限は、利用者のGoogle/Gemini契約およびDify設定に従います。無料利用を保証するものではありません。

外部検索を避けたい場合はv0.2.2を使用するか、v0.3.4のGrounding経路を無効化してください。
