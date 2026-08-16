# MemoBrain Dify workflow

MemoBrain Android アプリの保存先として利用する Dify DSL です。

## 配布ファイル

`MemoBrain-v0.2.2-DifyPatch.zip` に次を収録しています。

- `MemoBrain_DifyOnly_v0.2.2.yml` — Dify にインポートする DSL
- `MemoBrain-v0.2.2-README.txt` — 補足説明

ZIPを展開し、Dify Studio から `MemoBrain_DifyOnly_v0.2.2.yml` をインポートしてください。

## インポート後に設定する値

DSLには実運用の API Key は含めていません。Dify側で次を設定してください。

- `DIFY_API_BASE`: 自分の Dify API Base URL
- `KNOWLEDGE_API_KEY`: 自分の Dify Knowledge Service API Key
- `DATASET_NAME`: 保存先 Knowledge 名（例: `MemoBrain`）

Android側には別途、Dify App API Base URL と App API Key を設定します。

## YouTube

YouTube字幕取得ノードは `langgenius/transcript` の Transcript ツールを利用します。Difyから要求された場合はプラグインを導入してください。

## セキュリティ

API Key、実運用URL、認証情報をこのリポジトリへコミットしないでください。
