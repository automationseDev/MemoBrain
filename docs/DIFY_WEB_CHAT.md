# Dify Web Chat / WebView AdSense

MemoBrain uses two independent WebViews on the chat screen:

1. a small publisher-owned information/AdSense WebView; and
2. the user's own Dify published Web App WebView.

The user's Dify URL is loaded directly from the Android Keystore-backed encrypted setting. It is never embedded in, appended to, or sent to the publisher-owned advertising page.

## Android setup

1. Open MemoBrain.
2. Open `Dify接続設定（必須）`.
3. Set Dify API Base and App API Key.
4. Set `Dify Web App URL（任意・HTTPS）` to the user's own published Dify Web App URL.
5. Tap `Dify AIチャットを開く`.

The chat screen loads `https://automationse.net/memobrain-ad/` in the advertising WebView and the configured Dify URL in a separate WebView. AdSense identifiers and the user's Dify URL are never combined.

## WebView behavior

- HTTPS only.
- Mixed HTTP content is blocked.
- JavaScript and DOM storage are enabled.
- Third-party cookies are enabled for web compatibility.
- Dify file chooser support is retained.
- Dify main-frame navigation is limited to the configured Dify host; other hosts open externally.
- The advertising WebView is limited to the publisher-owned host.
- `FLAG_SECURE` remains enabled.
- Only the advertising WebView is registered with `MobileAds.registerWebView()`.

## Advertising page

Deploy `web/memobrain-ad/` to:

```text
https://automationse.net/memobrain-ad/
```

Copy `config.example.js` to `config.js` only in the deployment environment:

```javascript
window.MEMOBRAIN_AD_CONFIG = {
  adsenseClient: "ca-pub-...",
  adsenseSlot: "...",
  showAdPlaceholder: false
};
```

`config.js` is excluded from Git. It must contain advertising configuration only. Do not add a Dify URL, API key, token, signing secret, or personal endpoint.

The advertising page includes publisher-owned MemoBrain guidance so it is not an ad-only document. Web consent/CMP configuration remains the responsibility of the deployed site. The repository cannot guarantee AdSense approval.

## Privacy boundary

Visiting the public advertising page reveals only the publisher's MemoBrain guidance and advertising configuration. It does not expose or provide access to a user's Dify instance. The Dify Web App URL remains encrypted on the device and is loaded only by the Dify WebView.
