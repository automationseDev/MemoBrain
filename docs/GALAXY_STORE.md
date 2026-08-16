# Galaxy Store 公開方針

Galaxy Store には GitHub Releases と**同一の署名済み Release APK**を公開します。

## 維持するもの

- applicationId: `net.automationse.memobrainshare`
- 同一のリリース署名鍵
- 同一 versionCode / versionName の APK
- Release ビルド時に組み込んだ開発者管理の AdMob ID

正式 APK は `android/MemoBrainShare/Build-MemoBrainRelease.cmd` で作成し、生成された `output/MemoBrain-v1.0.0-release.apk` を GitHub Releases と Galaxy Store の両方へ使用します。

署名鍵 (`.jks`) とパスワード、`signing-secrets.properties`、本番 AdMob ID を含む `release-secrets.properties` はリポジトリへコミットしません。

詳しい作成手順は [RELEASE_SIGNING.md](RELEASE_SIGNING.md) を参照してください。

## ストア説明で必ず明示する事項

- Dify が必須
- MemoBrain 単体では利用不可
- 利用者自身の Dify 環境と App API Key が必要
- 共有内容は利用者が設定した Dify へ送信される
- 広告を表示する
