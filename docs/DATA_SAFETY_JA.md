# Google Play Data safety 記入メモ（v0.4.0）

これは申告のたたき台です。最終申告は、公開時点のGoogle公式情報、実際に利用するAdMob SDK、Dify構成、LLM/プラグイン構成を確認して決定してください。

## MemoBrain本体

ユーザーが明示的に共有した以下の情報を、ユーザー自身が設定したDifyへ送信します。

- テキスト / URL / 補足メモ
- 写真・画像
- 動画
- ファイル・文書

用途: アプリの主要機能（ユーザーが指定したDifyへの保存/処理）

DifyでKnowledge保存する構成では、送信先Difyにデータが残るため「一時的な処理だけ」とは扱わない前提で検討してください。

Difyがさらに外部LLM、Transcript、Web取得、ストレージ等へデータを送信する場合、公開者はその実構成も確認してください。

## Dify接続情報

- Dify API Base: 端末内暗号化保存
- Dify App API Key: 端末内暗号化保存

これらをMemoBrain開発者のサーバーへ送信する処理はありません。API Keyはユーザー指定DifyへのAuthorizationとしてのみ使用します。

## Google Mobile Ads SDK / UMP

Google Mobile Ads SDKは、広告、分析、不正防止等の目的で次の情報を自動的に収集・共有することがあるとGoogleが案内しています。

- IPアドレス（おおまかな位置の推定に利用される場合あり）
- アプリ/広告とのインタラクション
- 診断情報
- デバイス・アカウント関連識別情報

v0.4.0では `com.google.android.gms.permission.AD_ID` を manifest merge から削除しており、Android Advertising IDへのアクセスを無効化しています。ただしApp Set ID等を含む他のSDKデータ処理まで無くなるわけではありません。

Google Play ConsoleのData safetyでは、公開時点で使用しているGoogle Mobile Ads SDKの公式「Google Play data disclosure」を再確認してください。

## セキュリティ

- Dify通信: HTTPSのみ
- 端末内Dify設定: Android Keystoreで暗号化
- 送信待ちユーザーコンテンツ: AES-GCM暗号化
- 一時復号ファイル: upload試行中のみcacheDirへ作成、終了時削除
- 保持期限: 最大24時間
- Androidバックアップ: 無効
- 広告同意: UMP

## 「データを収集していない」とは申告しない

MemoBrainはユーザーコンテンツをDifyへ送信し、AdMob SDKもデータ処理を行うため、「アプリがデータを収集・共有しない」という申告は適切ではありません。
