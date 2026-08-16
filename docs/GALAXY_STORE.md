# Galaxy Store 公開方針

Galaxy Store には GitHub Releases と同一系統の正式署名 APK を公開します。

## 維持するもの

- applicationId: `net.automationse.memobrainshare`
- 同一のリリース署名鍵
- 同一 versionCode / versionName の APK
- Release ビルド時に組み込んだ開発者管理の AdMob ID

署名鍵 (`.jks`) とパスワードはリポジトリへコミットしません。

## ストア説明で必ず明示する事項

- Dify が必須
- MemoBrain 単体では利用不可
- 利用者自身の Dify 環境と App API Key が必要
- 共有内容は利用者が設定した Dify へ送信される
- 広告を表示する
