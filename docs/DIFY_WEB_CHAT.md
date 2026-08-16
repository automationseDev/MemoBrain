# Dify Web Chat (experimental)

This branch adds an in-app WebView screen for the Dify published Web App.

## Android setup

1. Open MemoBrain.
2. Open `Dify接続設定（必須）`.
3. Keep the existing Dify API Base and App API Key settings.
4. Set `AIチャット Web URL（任意・HTTPS）` to one of the following:
   - the HTTPS URL of the Dify published Web App; or
   - an HTTPS wrapper page that embeds the Dify Web App.
5. Tap `Dify AIチャットを開く`.

The Web URL is stored with the same Android Keystore backed AES-GCM mechanism used by the other connection settings. It is not baked into the APK.

## WebView behavior

- HTTPS only.
- JavaScript and DOM storage are enabled because the Dify Web App needs them.
- Mixed HTTP content is blocked.
- Third-party cookies are enabled so an HTTPS wrapper page can embed a Dify app on another host.
- File chooser support is enabled for Dify chat attachments.
- Main-frame links to another host are opened in the external browser.
- `FLAG_SECURE` is enabled on the chat screen.
- The WebView is registered with Google Mobile Ads SDK via `MobileAds.registerWebView()` so a publisher-owned HTTPS wrapper page can use the WebView API for Ads.

## AdSense note

Do not use an ad-only wrapper page. If AdSense is used, the HTTPS page must contain meaningful publisher-owned content and must comply with the Google Publisher Policies. Consent collected in the native Android app is not automatically propagated to AdSense tags inside WebView; the web page must handle its own web consent requirements.

The sample in `web/memobrain-chat/` is intentionally configuration-free. Copy `config.example.js` to `config.js` only in the deployment environment and fill in the Dify Web App URL. AdSense values are optional and should only be enabled after the site/page is approved and policy requirements are satisfied.

## Current scope

This is an experimental feature branch. The existing native AdMob banner remains unchanged so the WebView chat can be tested independently before deciding whether to replace native AdMob in a later release.
