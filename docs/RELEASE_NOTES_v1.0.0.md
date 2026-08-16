# MemoBrain v1.0.0

MemoBrain の初回正式リリースです。

## 主な内容

- Android 共有メニューから URL / YouTube / テキスト / 画像 / PDF / 文書 / 動画を Dify へ送信
- WorkManager によるバックグラウンド保存
- Dify 接続情報の Android Keystore 暗号化保存
- 送信待ちデータの AES-GCM 暗号化
- 最大24時間の一時データ保持と自動削除
- Release ビルドの署名設定を外部化
- 本番 AdMob ID をビルド時に注入
- GitHub Releases と Samsung Galaxy Store で同一署名 APK を配布する構成

## 必要条件

MemoBrain 単体では利用できません。利用者自身が用意した Dify 環境と MemoBrain 用 Dify DSL、Dify App API Key が必要です。

## 配布ファイル

- `MemoBrain-v1.0.0-release.apk`
- `MemoBrain_DifyOnly_v0.2.2.yml`
- `SHA256SUMS.txt`

## Android

- versionName: `1.0.0`
- versionCode: `7`
- applicationId: `net.automationse.memobrainshare`

## APK SHA-256

`802e1047c2e48ce0d639ab1c6c01eac0e0729b8c686a5a532ae71b509413f953`

Galaxy Store には GitHub Releases と同じ署名済み APK を使用します。
