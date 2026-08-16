# GitHub Release 運用

GitHub Releases を MemoBrain のメイン配布先とします。

## Release assets

推奨構成:

```text
MemoBrain-vX.Y.Z.apk
MemoBrain_DifyOnly-vX.Y.Z.yml
SHA256SUMS.txt
```

APK は Android Studio で **release / signed APK** として生成し、Galaxy Store へ提出する APK と同じ署名を使用します。

`.aab` は GitHub 直接インストール用ではないため、GitHub Releases の一般利用者向け配布物は APK とします。

## versionCode

更新ごとに Android の `versionCode` を必ず増加させます。GitHub 版と Galaxy Store 版で同一バージョンを配布する場合は、同じ APK を利用します。
