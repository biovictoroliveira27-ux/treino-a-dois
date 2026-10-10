package com.treinoadois.app;

import android.app.Activity;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.Looper;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Build;
import android.view.View;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.credentials.ClearCredentialStateRequest;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.ClearCredentialException;
import androidx.credentials.exceptions.GetCredentialCancellationException;
import androidx.credentials.exceptions.GetCredentialException;
import androidx.webkit.WebViewAssetLoader;

import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;

import org.json.JSONObject;

import java.util.concurrent.Executor;

public class MainActivity extends Activity {
    // ID do cliente da Web do projeto Firebase "treino-a-dois-7d8cd"
    private static final String WEB_CLIENT_ID =
            "697042315294-aok3v80lcuijkfecs2tb1v8c91vdlba1.apps.googleusercontent.com";
    private static final String START_URL =
            "https://appassets.androidplatform.net/assets/index.html";

    private WebView web;
    private CredentialManager credentials;
    private Executor mainExecutor;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        applySystemBars();
        credentials = CredentialManager.create(this);
        final Handler main = new Handler(Looper.getMainLooper());
        mainExecutor = main::post;

        final WebViewAssetLoader loader = new WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
                .build();

        web = new WebView(this);
        web.setBackgroundColor(isNight() ? Color.parseColor("#121615") : Color.parseColor("#F3F4F2"));
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setTextZoom(100);
        web.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                return loader.shouldInterceptRequest(request.getUrl());
            }
        });
        web.addJavascriptInterface(new Bridge(), "AndroidApp");
        setContentView(web);

        if (state != null) web.restoreState(state);
        else web.loadUrl(START_URL);
    }

    private void callJs(String fn, String arg) {
        final String js = "window." + fn + " && window." + fn + "(" + JSONObject.quote(arg) + ")";
        runOnUiThread(() -> web.evaluateJavascript(js, null));
    }

    private void signIn() {
        GetSignInWithGoogleOption option = new GetSignInWithGoogleOption.Builder(WEB_CLIENT_ID).build();
        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(option)
                .build();
        credentials.getCredentialAsync(this, request, new CancellationSignal(), mainExecutor,
                new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                    @Override
                    public void onResult(GetCredentialResponse result) {
                        Credential c = result.getCredential();
                        if (c instanceof CustomCredential
                                && GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL.equals(c.getType())) {
                            try {
                                GoogleIdTokenCredential g = GoogleIdTokenCredential.createFrom(c.getData());
                                callJs("onNativeSignIn", g.getIdToken());
                            } catch (Exception e) {
                                callJs("onNativeSignInError", "token: " + e.getMessage());
                            }
                        } else {
                            callJs("onNativeSignInError", "Tipo de login inesperado");
                        }
                    }

                    @Override
                    public void onError(GetCredentialException e) {
                        if (e instanceof GetCredentialCancellationException) callJs("onNativeSignInError", "cancelado");
                        else callJs("onNativeSignInError", e.getClass().getSimpleName() + ": " + e.getMessage());
                    }
                });
    }

    private void signOut() {
        credentials.clearCredentialStateAsync(new ClearCredentialStateRequest(), new CancellationSignal(), mainExecutor,
                new CredentialManagerCallback<Void, ClearCredentialException>() {
                    @Override public void onResult(Void v) { }
                    @Override public void onError(ClearCredentialException e) { }
                });
    }

    private class Bridge {
        @JavascriptInterface
        public void signIn() { runOnUiThread(MainActivity.this::signIn); }

        @JavascriptInterface
        public void signOut() { runOnUiThread(MainActivity.this::signOut); }

        @JavascriptInterface
        public String version() { return "3.0"; }
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        web.saveState(out);
    }

    private boolean isNight() {
        return (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;
    }

    // Barra de status e de navegação nas cores do app (claro/escuro)
    private void applySystemBars() {
        boolean night = isNight();
        int bar = night ? Color.parseColor("#121615") : Color.parseColor("#F3F4F2");
        int nav = night ? Color.parseColor("#1B201F") : Color.WHITE;
        getWindow().setStatusBarColor(bar);
        getWindow().setNavigationBarColor(nav);
        View decor = getWindow().getDecorView();
        int flags = decor.getSystemUiVisibility();
        if (night) {
            flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            if (Build.VERSION.SDK_INT >= 26) flags &= ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        } else {
            flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            if (Build.VERSION.SDK_INT >= 26) flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        }
        decor.setSystemUiVisibility(flags);
    }

    @Override
    public void onConfigurationChanged(Configuration c) {
        super.onConfigurationChanged(c);
        applySystemBars();
    }

    // Voltar: o app decide (fecha o resumo, volta para Hoje); só sai se estiver no início
    @Override
    public void onBackPressed() {
        web.evaluateJavascript("window.onBack ? String(window.onBack()) : 'false'", value -> {
            if (!"\"true\"".equals(value)) MainActivity.super.onBackPressed();
        });
    }
}
