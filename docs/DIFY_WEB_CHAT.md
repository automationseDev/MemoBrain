# Dify Web Chat / WebView AdSense

This branch adds an in-app WebView screen for the Dify published Web App and moves advertising to web-hosted AdSense only.

## Android setup

1. Open MemoBrain.
2. Open `Dify接続設定（必須）`.
3. Keep the existing Dify API Base and App API Key settings.
4. Set `AIチャット Web URL（任意・HTTPS）` to one of the following:
   - the HTTPS URL of the Dify published Web App; or
   - preferably, an HTTPS wrapper page that embeds the Dify Web App and contains the publisher-owned content/AdSense area.
5. Tap `Dify AIチャットを開く`.

The Web URL is stored with the same Android Keystore backed AES-GCM mechanism used by the other connection settings. It is not baked into the APK.

## Layout / system bars

The chat header now applies Android system bar insets, so `戻る` / title / `再読込` stay below the status bar on edge-to-edge devices. The bottom navigation inset is also applied to the WebView container.

The sample wrapper page places the ad area explicitly between the MemoBrain guide content and the Dify chat. Set `showAdPlaceholder: true` in `config.js` during layout testing to see the exact future ad position without a live AdSense unit.

## WebView behavior

- HTTPS only.
- JavaScript and DOM storage are enabled because the Dify Web App needs them.
- Mixed HTTP content is blocked.
- Third-party cookies are enabled so an HTTPS wrapper page can embed a Dify app on another host.
- File chooser support is enabled for Dify chat attachments.
- Main-frame links to another host are opened in the external browser.
- `FLAG_SECURE` is enabled on the chat screen.
- The WebView is registered with Google Mobile Ads SDK via `MobileAds.registerWebView()` for Google's WebView API for Ads.

## AdSense-only design

MemoBrain no longer uses a native AdMob banner and no longer embeds an AdMob application ID or banner unit ID in the APK.

The Android app keeps `play-services-ads` only because `MobileAds.registerWebView()` is required for the WebView API for Ads. `AndroidManifest.xml` uses the `com.google.android.gms.ads.INTEGRATION_MANAGER=webview` metadata flag instead of an AdMob application ID.

AdSense values belong to the deployed web page:

```javascript
window.MEMOBRAIN_CHAT_CONFIG = {
  difyWebAppUrl: "https://your-dify.example.com/chat/replace-me",
  adsenseClient: "ca-pub-...",
  adsenseSlot: "...",
  showAdPlaceholder: false
};
```

`web/memobrain-chat/config.js` is intentionally excluded from Git. Copy `config.example.js` to `config.js` only in the deployment environment. The publisher/slot values do not need to be shared with contributors or embedded in the Android build.

A public AdSense publisher ID is not a password/API secret and can be visible in delivered HTML, but this layout keeps it out of the Git repository and APK.

## Consent / policy boundary

The WebView API for Ads does not propagate consent collected in the native Android app into AdSense tags in the WebView. Web advertising consent must therefore be handled in the web context using the site's applicable consent/CMP configuration.

Do not use an ad-only wrapper page. Keep useful publisher-owned content on the page and follow current Google Publisher Policies. This repository cannot guarantee AdSense approval; the deployed site's content and configuration are what Google evaluates.

## Web deployment

Deploy the contents of `web/memobrain-chat/` to an HTTPS location, create a local `config.js`, then put that wrapper page URL into MemoBrain's `AIチャット Web URL` setting.

If the direct Dify Web App URL is configured instead, Dify chat still works, but there is no MemoBrain wrapper and therefore no MemoBrain AdSense area.
