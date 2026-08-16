# MemoBrain

MemoBrain は、Android の共有メニューから URL、YouTube、テキスト、画像、PDF、文書、動画などを **自分で用意した Dify** に送り、個人用ナレッジとして整理・保存するセルフホスト向けアプリです。

> **Dify は必須です。MemoBrain 単体では保存機能を利用できません。**
> 利用者自身の Dify 環境、MemoBrain 用 Dify DSL、Dify App API Key が必要です。

## 配布方針

- **GitHub Releases**: APK と Dify DSL のメイン配布先
- **Samsung Galaxy Store**: 同じ applicationId / 同じ署名の正式 APK を掲載する予定
- Release ビルドの AdMob ID は利用者が変更できず、開発者のビルド時設定のみを使用

## リポジトリ構成

```text
MemoBrain/
├─ android/MemoBrainShare/       Android アプリソース
├─ dify/                         Dify DSL
├─ docs/                         導入・公開・プライバシー資料
├─ scripts/                      Dify 疎通確認ツール
├─ .gitignore
├─ CHANGELOG.md
└─ README.md
```

## クイックスタート

1. Dify を用意します。
2. `dify/MemoBrain_DifyOnly_v0.2.2.yml` を Dify にインポートします。
3. Dify 側の環境変数 `DIFY_API_BASE`、`KNOWLEDGE_API_KEY`、`DATASET_NAME` を設定します。
4. MemoBrain の APK をインストールします。
5. MemoBrain の「Dify接続設定」に Dify App API Base URL と App API Key を設定します。
6. Android の共有メニューから MemoBrain を選択して保存します。

詳細は [Difyセットアップ](docs/DIFY_SETUP.md) と [Android導入手順](docs/INSTALL_ANDROID.md) を参照してください。

## 必要環境

- Android 8.0 (API 26) 以上
- Dify（セルフホストまたは利用者自身が管理する環境）
- Dify App API Key
- Dify Knowledge Service API Key
- HTTPS で到達可能な Dify API URL

## プライバシー設計

- Dify 接続 URL / App API Key は Android Keystore を利用して暗号化保存
- 送信待ちテキスト・共有ファイルはアプリ専用領域で AES-GCM 暗号化
- 送信成功または最終失敗後に送信待ちデータを削除
- 未送信データも最大24時間で削除
- Android バックアップ / データ移行バックアップを無効化
- 平文 HTTP 接続を拒否
- 共有内容をタスクスナップショットへ残しにくくする `FLAG_SECURE` を使用
- 通知にメモ本文や Dify レスポンス本文を表示しない
- AdMob の Advertising ID (`AD_ID`) 権限を削除

ただし、保存した内容は利用者自身が設定した Dify 環境へ送信・保存されます。Dify 側に保存されたデータは MemoBrain のアンインストールでは削除されません。

詳しくは [プライバシーポリシー](docs/PRIVACY_POLICY_JA.md) を参照してください。

## AdMob

Debug ビルドは Google 公式テスト広告 ID を固定使用します。Release ビルドは `release-secrets.properties`、Gradle project property、または環境変数からビルド時に正式 ID を受け取ります。正式 ID は GitHub へコミットしません。

```properties
MEMOBRAIN_ADMOB_APP_ID=ca-app-pub-xxxxxxxxxxxxxxxx~xxxxxxxxxx
MEMOBRAIN_ADMOB_BANNER_ID=ca-app-pub-xxxxxxxxxxxxxxxx/xxxxxxxxxx
```

`release-secrets.properties.example` をテンプレートとして利用してください。

## Android ビルド

Windows では `android/MemoBrainShare/Build-MemoBrainApk.cmd` を実行できます。展開場所に依存せず、JDK / Android SDK を自動探索し、必要なら Gradle 9.5.1 Wrapper を生成します。

Android Studio で開く場合は `android/MemoBrainShare` を直接 Open してください。

## 公開時にコミットしないもの

- Dify App API Key
- Dify Knowledge Service API Key
- `release-secrets.properties`
- AdMob 本番 ID を含むローカル設定
- `.jks` / `.keystore`
- 署名パスワード
- `local.properties`

## ライセンス

現時点ではオープンソースライセンスを設定していません。正式な利用・改変・再配布条件は公開前に決定予定です。
