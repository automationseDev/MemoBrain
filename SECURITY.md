# Security Policy

MemoBrain は利用者自身の Dify 環境へデータを送信するクライアントです。

## 秘密情報

次の情報は Issue、Pull Request、Discussion、コミットへ投稿しないでください。

- Dify App API Key
- Dify Knowledge Service API Key
- AdMob のローカル Release 設定
- Android 署名鍵 (`.jks` / `.keystore`) とパスワード
- 実運用環境の認証情報

漏えいした可能性がある API Key は、対象サービス側で直ちに無効化・再発行してください。

## 脆弱性報告

公開 Issue に秘密情報や再現用の実データを貼らないでください。報告時は、個人データと認証情報を除去した再現手順を使用してください。
