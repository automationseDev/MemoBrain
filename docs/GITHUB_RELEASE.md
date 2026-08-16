# GitHub Release 運用

GitHub Releases を MemoBrain のメイン配布先とします。

## Release assets

v1.0.0 の推奨構成:

```text
MemoBrain-v1.0.0-release.apk
MemoBrain_DifyOnly_v0.2.2.yml
SHA256SUMS.txt
```

Android APK は `android/MemoBrainShare/Build-MemoBrainRelease.cmd` で生成します。生成される `MemoBrain-v1.0.0-release.apk` は Gradle の release signingConfig で署名され、同時に `SHA256SUMS.txt` も生成されます。

Galaxy Store へ提出する APK と GitHub Releases の APK は、**同じファイルを使用**します。

`.aab` は GitHub 直接インストール用ではないため、GitHub Releases の一般利用者向け配布物は APK とします。

## 署名情報

署名情報は `signing-secrets.properties` からローカルで読み込みます。このファイルと keystore は GitHub へコミットしません。

詳しくは [RELEASE_SIGNING.md](RELEASE_SIGNING.md) を参照してください。

## versionCode

更新ごとに Android の `versionCode` を必ず増加させます。GitHub 版と Galaxy Store 版で同一バージョンを配布する場合は、同じ APK を利用します。
