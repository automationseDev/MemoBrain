# MemoBrain v1.3.1

既存の正式版を上書き更新できる署名済みリリースです。

## 変更内容

- Dify DSL v0.3.7を同梱
- `Web調査:` で始まる明示的な調査依頼を通常メモ保存より優先
- Gemini GroundingによるWeb検索、記事生成、MemoBrain Knowledge登録を確認
- Gemini 3.6 Flash、3.5 Flash、2.5 Flashのフォールバックを維持
- Dify応答とWeb調査の確認メッセージをアプリ内に表示
- ネイティブKnowledge検索、ナレッジ一覧、TODO、あとで読む管理
- 情報不足時に確認付きボタンからWeb調査とKnowledge登録
- ホーム画面とアプリ情報にバージョン、versionCode、アプリIDを表示
- 脳とノートを組み合わせた新しいアプリアイコン
- Release / Developの固定署名とUSB接続端末への上書きインストール

## Android

- versionName: `1.3.1`
- versionCode: `12`
- applicationId: `net.automationse.memobrainshare`
- minSdk: 26
- targetSdk: 36

v1.3.0と同じ正式署名鍵・同じapplicationIdを使用するため、アプリデータとDify接続設定を保持したまま更新インストールできます。

## Release assets

- `MemoBrain-v1.3.1-release.apk`
- `MemoBrain_DifyOnly_v0.3.7.yml`
- `SHA256SUMS.txt`
