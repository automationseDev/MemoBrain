# GitHub Release 運用

MemoBrainの正式版はGitHub Releasesだけで配布します。アプリストア向けAPK/AABは配布しません。

## v1.2.0のRelease assets

```text
MemoBrain-v1.2.0-release.apk
MemoBrain_DifyOnly_v0.3.5.yml
SHA256SUMS.txt
```

一般利用者向け配布物は、v1.0.0と同じ正式署名鍵で署名したAPKです。Release Smoke Buildの一時署名APKは正式配布に使用しません。

## 作成手順

1. `android/MemoBrainShare/Build-MemoBrainRelease.cmd` で正式署名APKを生成
2. `apksigner`の検証成功を確認
3. `SHA256SUMS.txt`とAPKのハッシュが一致することを確認
4. mainの対象コミットへ `v1.2.0` タグを作成
5. APK、推奨Dify DSL、SHA256SUMSをReleaseへ添付
6. Releaseを公開後、実機でv1.0.0から更新インストールできることを確認

## 署名情報

署名情報はローカルの `signing-secrets.properties` から読み込みます。このファイル、keystore、password、API Key、tokenはGitHubへコミットしません。

詳しくは [RELEASE_SIGNING.md](RELEASE_SIGNING.md) を参照してください。
