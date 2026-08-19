# MemoBrain v1.4.0 開発版

## 非AI Knowledge操作

Knowledge画面から行う次の操作は、Dify DSL v0.3.8の構造化action経路を利用します。

- ナレッジ一覧と詳細表示
- キーワード検索
- カテゴリとタグによる絞り込み
- 未完了TODOと「あとで読む」の一覧
- TODO完了と読了更新

これらの経路にはLLMノードがなく、Geminiの生成トークンを消費しません。Dify Knowledge Service APIによる検索・更新は実行されます。

## 必要な組み合わせ

- Android: `1.4.0-develop`（applicationId `net.automationse.memobrainshare.dev`）
- Dify DSL: `dify/MemoBrain_DifyOnly_v0.3.8.yml`
- Dify環境変数: `DIFY_API_BASE`、`KNOWLEDGE_API_KEY`、`DATASET_NAME`

既存のDify App API Base URL、App API Key、Web App URLはそのまま利用できます。v0.3.8を別アプリとしてインポートした場合のみ、新しいApp API KeyとWeb App URLをAndroidの接続プロファイルへ設定してください。
