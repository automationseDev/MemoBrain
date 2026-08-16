# MemoBrain Android v1.0.0

MemoBrain Share は、Android の共有メニューからテキスト・URL・画像・動画・PDF/文書を Dify に送信するためのクライアントです。

> **重要: Dify が必須です。**
> このアプリ単体ではメモ保存機能は動作しません。利用者自身が用意した Dify 環境、対応 Chatflow、Dify App API Base、App API Key が必要です。

## v1.0.0 のプライバシー設計

- Dify API Base / API Key は APK に含めない
- Dify接続情報は Android Keystore を利用して端末内で暗号化
- 共有テキスト・添付ファイルはバックグラウンド送信待ちの間だけアプリ専用領域へ AES-GCM 暗号化して保存
- 送信時のみ `cacheDir` に復号し、アップロード直後に削除
- 送信成功または最終失敗時に暗号化キューを削除
- ネットワークが戻らない場合も最大24時間で送信待ちデータを削除する Cleanup Worker を登録
- Androidバックアップ / 端末移行バックアップを無効化
- HTTP平文通信を禁止し、Dify API Base は HTTPS のみ許可
- `FLAG_SECURE` により共有内容・API設定画面のスクリーンショット/タスクスナップショットを抑止
- 通知本文にはメモ内容や Dify のレスポンス本文を表示しない
- Difyエラー時もサーバーレスポンス本文を通知/ログへ出さない
- 画面プレビューは共有テキストの先頭500文字まで
- Google Mobile Ads SDK が追加する Android Advertising ID (`AD_ID`) 権限を manifest merge で削除
- 広告IDのユーザー変更機能は廃止。正式版の広告IDはビルド時のみ組み込み

### 重要な境界

端末内の一時データを残しにくくする設計ですが、MemoBrain の目的上、ユーザーが保存した内容は **設定した Dify に送信されます**。
Dify の Knowledge、利用するLLM/プラグイン、ログ、バックアップ等にどの程度データが残るかは、利用者自身の Dify 構成と各サービスのポリシーに依存します。

また、AdMob SDK 自体によるデータ処理はゼロにはできません。Google Play の Data safety 申告時は、使用中の Google Mobile Ads SDK / UMP SDK の最新公式開示を必ず確認してください。

## Debug APK のビルド

ZIPは好きな場所へ展開できます。

```text
MemoBrainShare\Build-MemoBrainApk.cmd
```

をダブルクリックするか、PowerShell 5.1で以下を実行します。

```powershell
.\Build-MemoBrainApk.ps1
```

Debugビルドは必ずGoogle公式テスト広告IDを使用します。

生成先:

```text
MemoBrainShare\output\MemoBrain-v1.0.0-debug.apk
```

## 正式版 AdMob ID

正式版にはユーザーが広告IDを変更する画面はありません。

`release-secrets.properties.example` をコピーして、ローカルだけに次のファイルを作成します。

```text
release-secrets.properties
```

中身:

```properties
MEMOBRAIN_ADMOB_APP_ID=ca-app-pub-xxxxxxxxxxxxxxxx~xxxxxxxxxx
MEMOBRAIN_ADMOB_BANNER_ID=ca-app-pub-xxxxxxxxxxxxxxxx/xxxxxxxxxx
```

`release-secrets.properties` は `.gitignore` 対象です。

Releaseビルドでは以下の場合にビルドを停止します。

- App IDが未設定
- Banner Ad Unit IDが未設定
- Google公式テストIDが設定されている
- ID形式が不正

これにより、正式版がテスト広告や他人が入力した広告IDで配布されることを防ぎます。

## Google Play 用ビルド

新規Google Playアプリは Android App Bundle (AAB) で公開します。
Android Studioから `MemoBrainShare` を開き、正式な AdMob ID をローカル設定後、
**Build > Generate Signed App Bundle / APK > Android App Bundle** で署名済みAABを作成してください。

署名鍵、keystore、パスワードはこのプロジェクト/ZIPへ保存しないでください。

## 初回利用

1. アプリを起動
2. `Dify接続設定（必須）`
3. HTTPS の Dify API Base（例 `https://your-dify.example/v1`）を入力
4. Dify App API Key を入力
5. Androidの共有メニューから MemoBrain を選択
6. `MemoBrainに保存` を押す

接続設定は端末内で暗号化され、削除ボタンから消去できます。

## Gradle / JDK

Android Studio では `.idea/gradle.xml` の `#GRADLE_LOCAL_JAVA_HOME` を利用します。
固定のJDK絶対パスはコミットしていません。

Windowsの `Build-MemoBrainApk.ps1` は Gradle 9.5.1 を利用し、JDK 17以上を自動検出します。
