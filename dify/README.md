# MemoBrain Dify package

## 推奨DSL

- `MemoBrain_DifyOnly_v0.3.8.yml`: Androidの閲覧・検索・完了操作を構造化actionで非AI処理する推奨版
- `MemoBrain_DifyOnly_v0.3.6.yml`: Android共有メタデータ・Web調査・Geminiモデル自動切替版（旧版）
- `MemoBrain_DifyOnly_v0.3.5.yml`: Web調査の明示確認・Gemini Grounding・モデル自動切替版
- `MemoBrain_DifyOnly_v0.3.4.yml`: Knowledge不足時に自動でGemini Groundingを実行する旧版
- `MemoBrain_DifyOnly_v0.3.3.yml`: DuckDuckGo Searchを使う旧版
- `MemoBrain_DifyOnly_v0.2.2.yml`: Web検索を行わない従来版

AndroidアプリとDify DSLは別バージョンで管理します。

## v0.3.8 非AI action

AndroidのKnowledge画面は、Dify App APIの `inputs` に `action`、`query`、`category`、`tag` を指定します。次のactionは意図解析・回答生成のLLMノードを通らず、Knowledge Service APIとコードノードだけで処理されます。

- `knowledge_list` / `knowledge_detail` / `knowledge_search`
- `todo_list` / `read_later_list`
- `todo_complete` / `read_later_complete`

`action` が空の従来リクエストは、これまでどおりGeminiを利用する保存・質問・Web調査フローへ進みます。

## v0.3.6の動作

1. 利用者の質問で既存Knowledgeを検索
2. 十分な根拠があればKnowledgeから回答
3. 根拠がなければWeb調査の確認メッセージを表示して停止
4. 利用者が `Web調査: 調べたい内容` と送信した場合だけGemini Google Search Groundingを実行
5. 参照URLを残した日本語記事へ整理
6. 既存のMemoBrain Knowledgeへ自動登録して回答

確認なしに検索語句を外部Web検索へ送信しません。DuckDuckGo Searchプラグインは不要です。

## Geminiモデル自動切替

意図解析、Knowledge回答、Web調査記事生成、画像、動画、文書、Webページ、YouTubeのLLM処理で次の順に実行します。

1. `gemini-3.6-flash`
2. `gemini-3.5-flash`
3. `gemini-2.5-flash`

429、503などでノードが失敗した場合だけ次のモデルへ進みます。Web調査記事生成では3モデルすべてでGoogle Search Groundingを有効にしています。

## インポート前の準備

Dify Marketplaceから `Gemini`（`langgenius/gemini`）プラグイン `0.9.5` 以降をインストールまたは更新してください。

インポート後に次の環境変数を設定します。

- `DIFY_API_BASE`: 自分のDify API Base（例: `https://your-dify-api.example.com/v1`）
- `KNOWLEDGE_API_KEY`: Dify Knowledge Service API Key
- `DATASET_NAME`: MemoBrainが利用するKnowledge名

複数KnowledgeをAndroidから切り替える場合は、Knowledgeごとに本DSLからDify Appを作成し、それぞれの `DATASET_NAME` を設定してください。Androidの接続プロファイルには、そのDify AppのAPI Base / App API Key / Web App URLとKnowledge名を組にして登録します。これによりAPI Key単位で保存先を分離し、再送時も送信時のプロファイルへ固定できます。

Androidから送られる `[MB:META]` JSONには手動指定したカテゴリ、タグ、重要度、あとで読む、TODO、補足メモが含まれます。空欄または `auto` の項目は従来どおりAIが分類します。

公開DSLには実環境URLやAPI Keyを含めていません。

## データ送信と利用枠の注意

`Web調査:`を付けて明示的に実行すると、検索語句、取得情報、生成対象がDifyに設定したGeminiサービスへ送信され、Google Search Groundingで処理されます。生成した記事と参照URLは利用者自身のKnowledgeへ保存します。料金・利用上限は利用者のGoogle/Gemini契約とDify設定に従い、無料利用を保証するものではありません。
