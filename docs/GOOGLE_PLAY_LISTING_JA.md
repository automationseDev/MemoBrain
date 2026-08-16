# Google Play ストア掲載文案

## アプリ名

MemoBrain

## 短い説明

Androidの共有メニューから、あなたのDifyへメモ・URL・画像・文書をすばやく保存。

## 詳細説明

MemoBrainは、Androidで見つけた情報を自分のDify環境へ素早く送るための共有クライアントです。

ブラウザ、YouTube、SNS、ギャラリー、ファイルアプリなどから「共有」→「MemoBrain」を選ぶだけ。テキスト、URL、画像、動画、PDF、文書をバックグラウンドでDifyへ送信できます。

### 重要: Difyが必須です

**MemoBrainは単体では利用できません。**
利用者自身が用意した Dify 環境と、MemoBrain対応Chatflow / Dify App API Key が必要です。

インストール後、アプリ内の「Dify接続設定」で以下を設定してください。

- HTTPSのDify API Base URL
- Dify App API Key

開発者のDifyアカウントを共有するサービスではありません。利用者自身のDify環境へ接続するクライアントです。

### 主な機能

- Android共有メニューから保存
- テキスト / URL / YouTube URL
- 画像
- 動画
- PDF / 文書
- 任意の補足メモ
- WorkManagerによるバックグラウンド送信
- 送信結果通知

### プライバシーを考慮した設計

- Dify接続情報をAndroid Keystoreで暗号化
- 送信待ちデータを端末内で暗号化
- 送信完了・失敗後に一時データを削除
- 未送信データも最大24時間で削除
- HTTPS接続のみ許可
- Androidバックアップを無効化
- 通知にメモ本文を表示しない
- 広告ID設定をユーザーへ公開せず、正式版の広告は開発者がビルド時に設定

### 広告

本アプリにはGoogle AdMobのバナー広告が表示されます。必要な地域ではGoogle User Messaging Platformによるプライバシー/同意設定を表示します。

### ご利用前に

Difyの構築・運用、LLMプロバイダー、Knowledge、プラグイン等に関する設定は利用者自身で行う必要があります。
