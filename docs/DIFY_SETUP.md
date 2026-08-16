# Dify セットアップ

MemoBrain は Dify が必須です。Android アプリだけをインストールしても保存機能は利用できません。

## 1. DSL をインポート

Dify の Studio から `dify/MemoBrain_DifyOnly_v0.2.2.yml` をインポートします。

YouTube の字幕取得には `langgenius/transcript` の Transcript ツールを使用します。インポート時またはノード設定時に要求された場合は Dify のプラグイン画面から導入してください。

## 2. Dify アプリ側の環境変数

以下を設定します。

- `DIFY_API_BASE`: 自分の Dify API ベース URL。例 `https://dify-api.example.com/v1`
- `KNOWLEDGE_API_KEY`: Dify Knowledge Service API Key
- `DATASET_NAME`: MemoBrain が利用する Knowledge 名。既定値 `MemoBrain`

公開リポジトリの DSL に実キーは含まれていません。

## 3. Android アプリ用 App API Key

Dify で MemoBrain アプリの App API Key を発行します。Android 側には以下を設定します。

- API Base URL: `https://dify-api.example.com/v1` のような HTTPS URL
- App API Key: Dify アプリの API Key

MemoBrain は HTTP URLを拒否します。

## 4. 疎通確認

Windows PowerShell 5.1 では `scripts/Test-MemoBrain.ps1` を利用できます。

```powershell
.\scripts\Test-MemoBrain.ps1 `
  -ApiBase "https://dify-api.example.com/v1" `
  -AppApiKey "YOUR_APP_API_KEY"
```

API Key をコマンド履歴やスクリーンショットへ残したくない場合は、実行後に履歴の取り扱いにも注意してください。
