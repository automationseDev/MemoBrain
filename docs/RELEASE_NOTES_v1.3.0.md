# MemoBrain v1.3.0

共有画面のモダンUI、送信前のカテゴリ・タグ・重要度・あとで読む・TODO指定、複数Dify接続プロファイルを追加しました。

## 主な変更

- ライト／ダークテーマ対応のカード型UI
- 「保存する」を中心に、履歴・再送とAIチャットを整理
- 詳細指定は折りたたみ式で、従来のクイック保存を維持
- 複数Knowledge用の暗号化接続プロファイル
- 送信時に選択したプロファイルをバックグラウンド再送でも維持
- ホーム画面ウィジェットを削除
- Dify DSL v0.3.6でAndroid共有メタデータを優先
- WebView不要のネイティブKnowledge検索・一覧・TODO・あとで読む管理
- 情報不足時に確認付きWeb調査ボタンからGemini調査とKnowledge登録
- ホーム画面とアプリ情報でバージョン・versionCode・アプリIDを表示

## Dify更新

カテゴリ、タグ、重要度、あとで読む、TODOの手動指定を利用する場合は、Dify DSL `MemoBrain_DifyOnly_v0.3.6.yml` への更新を推奨します。従来の保存・検索のみであればv0.3.5も動作します。

複数Knowledgeを利用する場合は、KnowledgeごとにDSLからDify Appを作成し、各Appの `DATASET_NAME` とApp API KeyをAndroidの接続プロファイルへ登録してください。

## Android

- versionName: `1.3.0`
- versionCode: `12`
- applicationId: `net.automationse.memobrainshare`
- minSdk: 26
- targetSdk: 36

同一署名のv1.2.0から上書き更新できます。
