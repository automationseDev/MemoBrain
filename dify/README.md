# MemoBrain Dify package

## 推奨DSL

- `MemoBrain_DifyOnly_v0.3.1.yml`: Knowledge優先検索、Web検索フォールバック、記事生成、Knowledge自動登録を備えたAdvanced Chatflow
- `MemoBrain_DifyOnly_v0.2.2.yml`: Web検索を行わない従来版

AndroidアプリとDify DSLは別バージョンで管理します。Android v1.1.0に更新しても、Dify DSLを必ず変更する必要はありません。

## v0.3.1の動作

1. 利用者の質問で既存Knowledgeを検索
2. 検索結果が存在し、最高関連度スコアが0.35以上ならKnowledgeだけで回答
3. 不足時はDuckDuckGo SearchでWeb検索（最大5件）
4. 参照URLを残した日本語記事へ整理
5. 既存のMemoBrain Knowledgeへ自動登録
6. 登録した内容を回答

共有メニューからの通常保存、一覧、詳細、更新、削除の既存機能は維持しています。\n\n### v0.3.1での修正\n\n- DuckDuckGo QueryをDifyの`mixed`入力形式で明示的にバインド\n- 意図解析後のQueryが空ならユーザー入力を使用\n- Knowledge候補があっても回答生成結果が「該当なし」ならWeb検索へ再分岐\n- Knowledge APIレスポンスのスコアが`segment.score`にある場合にも対応

## インポート前の準備

Dify Marketplaceから `DuckDuckGo Search`（`langgenius/duckduckgo`）プラグインをインストールしてください。API Keyは不要です。セルフホストDifyから外部Webへ到達できる必要があります。

インポート後に次の環境変数を設定します。

- `DIFY_API_BASE`: 自分のDify API Base（例: `https://your-dify-api.example.com/v1`）
- `KNOWLEDGE_API_KEY`: Dify Knowledge Service API Key
- `DATASET_NAME`: MemoBrainが利用するKnowledge名

公開DSLには実環境URLやAPI Keyを含めていません。

## データ送信上の注意

ナレッジ補完を使うと、質問の検索語句はDuckDuckGo Searchプラグインへ送信されます。検索結果は、Difyに設定したLLMへ渡して記事化し、利用者自身のKnowledgeへ保存します。外部検索を避けたい場合はv0.2.2を使用するか、v0.3.1のWeb検索経路を無効化してください。
