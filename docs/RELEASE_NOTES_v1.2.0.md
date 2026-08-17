# MemoBrain v1.2.0

GitHub Releases専用の正式更新版です。MemoBrainの利用には、利用者自身が用意したDify環境が必要です。

## 主な新機能

- Dify公開Web Appを表示するアプリ内AIチャット
- 広告用WebViewと利用者Dify WebViewの分離
- 暗号化した送信履歴
- 最終失敗した送信の24時間以内の手動再送
- 正規化URLとファイルSHA-256による重複登録防止
- Knowledge不足時のGemini Grounding実行前確認
- Gemini 3.6 Flash → 3.5 Flash → 2.5 Flashのフェイルオーバー

## Dify

推奨DSLは `MemoBrain_DifyOnly_v0.3.5.yml` です。

Knowledgeに十分な情報がない場合、自動ではWeb調査しません。次の形式で送信した場合だけGemini Google Search Groundingを実行し、記事をKnowledgeへ登録します。

```text
Web調査: 調べたい内容
```

Gemini公式Difyプラグイン `0.9.5` 以降が必要です。DuckDuckGo Searchプラグインは不要です。

## Android

- versionName: `1.2.0`
- versionCode: `9`
- applicationId: `net.automationse.memobrainshare`
- Android 8.0（API 26）以上

v1.0.0と同じ正式署名APKのため、アプリを削除せず更新できます。

## 配布ファイル

- `MemoBrain-v1.2.0-release.apk`
- `MemoBrain_DifyOnly_v0.3.5.yml`
- `SHA256SUMS.txt`

## セキュリティ上の注意

API Key、keystore、password、token、本番 `config.js` は配布ファイルやリポジトリに含めていません。Dify接続先とAPI Keyは利用者がアプリへ設定します。
