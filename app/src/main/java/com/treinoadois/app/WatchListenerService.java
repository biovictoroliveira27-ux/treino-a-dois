package com.treinoadois.app;

import android.content.SharedPreferences;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;

/** Recebe séries marcadas no relógio quando o app do celular está fechado e guarda para aplicar depois. */
public class WatchListenerService extends WearableListenerService {
    static final String PREFS = "spotter_watch";

    @Override
    public void onMessageReceived(MessageEvent e) {
        if (!"/spotter/log".equals(e.getPath()) || MainActivity.alive) return;
        try {
            SharedPreferences p = getSharedPreferences(PREFS, MODE_PRIVATE);
            JSONArray q = new JSONArray(p.getString("queue", "[]"));
            q.put(new JSONObject(new String(e.getData(), StandardCharsets.UTF_8)));
            p.edit().putString("queue", q.toString()).apply();
        } catch (Exception ignored) { }
    }
}
