package com.cosmere.archives;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.Context;
import android.os.Environment;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import androidx.webkit.WebViewAssetLoader;
import androidx.webkit.WebViewClientCompat;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;

public class MainActivity extends Activity {
    private static final int FILE_REQUEST = 1301;
    private WebView webView;
    private ValueCallback<Uri[]> fileCallback;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        webView = new WebView(this);
        setContentView(webView);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(true);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        final WebViewAssetLoader loader = new WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
            .build();
        webView.setWebViewClient(new WebViewClientCompat() {
            @Override public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                return loader.shouldInterceptRequest(request.getUrl());
            }
        });
        webView.setWebChromeClient(new WebChromeClient() {
            @Override public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (fileCallback != null) fileCallback.onReceiveValue(null);
                fileCallback = callback;
                try {
                    Intent intent = params.createIntent();
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    startActivityForResult(intent, FILE_REQUEST);
                    return true;
                } catch (Exception e) {
                    fileCallback = null;
                    callback.onReceiveValue(null);
                    return true;
                }
            }
        });
        checkForUpdates();
        if (savedInstanceState == null) {
            webView.loadUrl("https://appassets.androidplatform.net/assets/index.html");
        } else {
            webView.restoreState(savedInstanceState);
        }
    }

    // The public APK repository has its own build numbers; no secret token is embedded.
    private void checkForUpdates() {
        new Thread(() -> {
            try {
                URL u = new URL("https://api.github.com/repos/cosmerearchives-creator/eBook-research/releases?per_page=10");
                HttpURLConnection c = (HttpURLConnection) u.openConnection();
                c.setConnectTimeout(8000);
                c.setReadTimeout(8000);
                c.setRequestProperty("Accept", "application/vnd.github+json");
                c.setRequestProperty("User-Agent", "Cosmere-Archives-Android");
                try {
                    if (c.getResponseCode() != 200) return;
                    byte[] bytes;
                    try (InputStream stream = c.getInputStream(); ByteArrayOutputStream buf = new ByteArrayOutputStream()) { byte[] part = new byte[8192]; int n; while ((n = stream.read(part)) != -1) buf.write(part, 0, n); bytes = buf.toByteArray(); }
                    JSONArray releases = new JSONArray(new String(bytes, StandardCharsets.UTF_8));
                    int installed = (int) getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
                    int newest = installed;
                    String link = null;
                    for (int i = 0; i < releases.length(); i++) {
                        JSONObject release = releases.getJSONObject(i);
                        if (release.optBoolean("draft")) continue;
                        String tag = release.optString("tag_name", "");
                        if (!tag.startsWith("android-build-")) continue;
                        int number;
                        try { number = Integer.parseInt(tag.substring("android-build-".length())); } catch (NumberFormatException ex) { continue; }
                        if (number <= newest) continue;
                        JSONArray assets = release.optJSONArray("assets");
                        if (assets == null) continue;
                        for (int j=0; j<assets.length(); j++) {
                            JSONObject asset=assets.getJSONObject(j);
                            if ("The-Cosmere-Archives.apk".equals(asset.optString("name"))) {
                                newest=number;
                                link=asset.optString("browser_download_url");
                                break;
                            }
                        }
                    }
                    if (link != null && link.startsWith("https://github.com/")) {
                        final String downloadUrl = link;
                        final int build = newest;
                        runOnUiThread(() -> new AlertDialog.Builder(this)
                            .setTitle("Update available")
                            .setMessage("The Cosmere Archives build " + build + " is available. Download the APK now? Android will ask you to approve installation. Back up your research before changing test builds.")
                            .setNegativeButton("Later", (dialog, which) -> {})
                            .setPositiveButton("Download", (dialog, which) -> downloadUpdate(downloadUrl, build))
                            .show());
                    }
                } finally { c.disconnect(); }
            } catch (Exception ignored) { /* Offline use works without updates. */ }
        }).start();
    }

    private void downloadUpdate(String url, int build) {
        try {
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            request.setTitle("The Cosmere Archives update " + build);
            request.setDescription("Tap the completed download to install the update");
            request.setMimeType("application/vnd.android.package-archive");
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,
                "The-Cosmere-Archives-" + build + ".apk");
            DownloadManager dm = (DownloadManager)getSystemService(Context.DOWNLOAD_SERVICE);
            dm.enqueue(request);
            Toast.makeText(this, "Downloading APK. Tap the completed download to install.", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Download unavailable. Visit GitHub Releases.", Toast.LENGTH_LONG).show();
        }
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_REQUEST && fileCallback != null) {
            fileCallback.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, data));
            fileCallback = null;
        }
    }

    @Override protected void onSaveInstanceState(Bundle outState) {
        webView.saveState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override public void onBackPressed() {
        if (webView.canGoBack()) webView.goBack(); else super.onBackPressed();
    }

    @Override protected void onDestroy() {
        if (fileCallback != null) {fileCallback.onReceiveValue(null); fileCallback = null;}
        webView.destroy();
        super.onDestroy();
    }
}
