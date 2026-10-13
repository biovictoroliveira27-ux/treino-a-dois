package com.treinoadois.wear;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.android.gms.wearable.DataClient;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;

import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Spotter no relógio.
 * Telas: início (hoje + outros dias) → dia (exercícios) → exercício (séries) → série (carga e reps) → descanso.
 * Os dados chegam do celular pelo Data Layer; cada série marcada volta ao celular, que salva na nuvem.
 */
public class WatchActivity extends Activity implements DataClient.OnDataChangedListener, SensorEventListener {
    private static final String PATH_TODAY = "/spotter/today";
    private static final int TEAL = Color.parseColor("#4CC2B4");
    private static final int ORANGE = Color.parseColor("#F0954F");
    private static final int GREEN = Color.parseColor("#5FB56E");
    private static final int CARD = Color.parseColor("#22292A");
    private static final int CARD2 = Color.parseColor("#2E3637");
    private static final int MUTED = Color.parseColor("#9AA5A1");

    private static final int HOME = 0, DAY = 1, EXERCISE = 2, SET = 3, REST = 4;

    private JSONObject snap;
    private int screen = HOME;
    private String who = null;      // "ele" ou "ela"
    private String dayId = null;
    private int exIdx = -1, setIdx = -1;
    private double load = 0;
    private int reps = 0;
    private String unit = "kg";
    private boolean focusLoad = true;
    private boolean showTip = false;
    private long timerEnd = 0;
    private int heartRate = 0;

    private final Handler h = new Handler(Looper.getMainLooper());
    private final Runnable tick = this::tickTimer;
    private ScrollView scroll;
    private TextView clockView, loadView, repsView, hrView;
    private SensorManager sensors;

    // ---------- Ciclo de vida ----------
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.BLACK);
        scroll.setFillViewport(true);
        scroll.setFocusable(true);
        scroll.setFocusableInTouchMode(true);
        setContentView(scroll);
        who = prefs().getString("who", null);
        String cached = prefs().getString("snap", null);
        if (cached != null) { try { snap = new JSONObject(cached); } catch (Exception ignored) { } }
        sensors = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (checkSelfPermission("android.permission.BODY_SENSORS") != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{"android.permission.BODY_SENSORS"}, 3);
        }
        render();
    }

    @Override
    protected void onResume() {
        super.onResume();
        try { Wearable.getDataClient(this).addListener(this); } catch (Exception ignored) { }
        fetch();
        send("/spotter/refresh", "{}");
        startHeart();
        scroll.requestFocus();
    }

    @Override
    protected void onPause() {
        try { Wearable.getDataClient(this).removeListener(this); } catch (Exception ignored) { }
        try { sensors.unregisterListener(this); } catch (Exception ignored) { }
        super.onPause();
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] perms, int[] res) {
        super.onRequestPermissionsResult(code, perms, res);
        startHeart();
    }

    @Override
    public void onBackPressed() {
        switch (screen) {
            case REST: stopTimer(); return;
            case SET: go(EXERCISE); return;
            case EXERCISE: go(DAY); return;
            case DAY: go(HOME); return;
            default: super.onBackPressed();
        }
    }

    private SharedPreferences prefs() { return getSharedPreferences("spotter", MODE_PRIVATE); }

    private void go(int s) { screen = s; showTip = false; render(); }

    // ---------- Frequência cardíaca ----------
    private void startHeart() {
        try {
            if (checkSelfPermission("android.permission.BODY_SENSORS") != PackageManager.PERMISSION_GRANTED) return;
            Sensor s = sensors.getDefaultSensor(Sensor.TYPE_HEART_RATE);
            if (s != null) sensors.registerListener(this, s, SensorManager.SENSOR_DELAY_NORMAL);
        } catch (Exception ignored) { }
    }

    @Override
    public void onSensorChanged(SensorEvent e) {
        if (e.values.length > 0 && e.values[0] > 0) {
            heartRate = Math.round(e.values[0]);
            if (hrView != null) hrView.setText("♥ " + heartRate + " bpm");
        }
    }

    @Override
    public void onAccuracyChanged(Sensor s, int a) { }

    // ---------- Moldura giratória ----------
    @Override
    public boolean onGenericMotionEvent(MotionEvent ev) {
        if (screen == SET && ev.getAction() == MotionEvent.ACTION_SCROLL && ev.isFromSource(InputDevice.SOURCE_ROTARY_ENCODER)) {
            float d = -ev.getAxisValue(MotionEvent.AXIS_SCROLL);
            int step = d > 0 ? 1 : -1;
            if (focusLoad && !nk()) load = Math.max(0, load + step); else reps = Math.max(0, reps + step);
            updateValues();
            buzz(new long[]{0, 8});
            return true;
        }
        return super.onGenericMotionEvent(ev);
    }

    // ---------- Dados ----------
    private void fetch() {
        try {
            Wearable.getDataClient(this).getDataItems(Uri.parse("wear://*" + PATH_TODAY))
                    .addOnSuccessListener(buf -> {
                        String best = null; long bestT = -1;
                        for (DataItem it : buf) {
                            DataMap m = DataMapItem.fromDataItem(it).getDataMap();
                            long t = m.getLong("t", 0);
                            if (t > bestT) { bestT = t; best = m.getString("json"); }
                        }
                        buf.release();
                        if (best != null) applySnap(best);
                    });
        } catch (Exception ignored) { }
    }

    @Override
    public void onDataChanged(DataEventBuffer events) {
        String json = null;
        for (DataEvent e : events) {
            if (e.getType() == DataEvent.TYPE_CHANGED && PATH_TODAY.equals(e.getDataItem().getUri().getPath())) {
                json = DataMapItem.fromDataItem(e.getDataItem()).getDataMap().getString("json");
            }
        }
        if (json != null) { final String j = json; runOnUiThread(() -> applySnap(j)); }
    }

    private void applySnap(String json) {
        try {
            JSONObject s = new JSONObject(json);
            if (s.optInt("v") < 2) return;
            if (snap != null && s.optLong("updatedAt") < snap.optLong("updatedAt")) return;
            snap = s;
            prefs().edit().putString("snap", json).apply();
            if (screen == HOME || screen == DAY || screen == EXERCISE) render();
        } catch (Exception ignored) { }
    }

    private void send(String path, String payload) {
        try {
            final byte[] data = payload.getBytes(StandardCharsets.UTF_8);
            Wearable.getNodeClient(this).getConnectedNodes().addOnSuccessListener(nodes -> {
                for (Node n : nodes) Wearable.getMessageClient(this).sendMessage(n.getId(), path, data);
            });
        } catch (Exception ignored) { }
    }

    private String role() {
        if (who != null) return who;
        return snap != null ? snap.optString("role", "ele") : "ele";
    }

    private JSONArray days() { JSONArray a = snap == null ? null : snap.optJSONArray("days"); return a == null ? new JSONArray() : a; }

    private JSONObject day(String id) {
        JSONArray a = days();
        for (int i = 0; i < a.length(); i++) { JSONObject d = a.optJSONObject(i); if (d != null && id != null && id.equals(d.optString("id"))) return d; }
        return null;
    }

    /** Exercícios do dia que a pessoa escolhida faz, como pares [índice no dia]. */
    private JSONArray myEx(JSONObject d) {
        JSONArray out = new JSONArray();
        JSONArray ex = d == null ? null : d.optJSONArray("ex");
        if (ex != null) for (int i = 0; i < ex.length(); i++) { JSONObject e = ex.optJSONObject(i); if (e != null && e.optJSONObject(role()) != null) out.put(e); }
        return out;
    }

    private JSONObject curEx() { JSONArray a = myEx(day(dayId)); return exIdx >= 0 && exIdx < a.length() ? a.optJSONObject(exIdx) : null; }

    private JSONObject curPerson() { JSONObject e = curEx(); return e == null ? null : e.optJSONObject(role()); }

    private boolean nk() { JSONObject p = curPerson(); return p != null && p.optBoolean("nk"); }

    private static int done(JSONObject p) {
        JSONArray s = p == null ? null : p.optJSONArray("sets"); int n = 0;
        if (s != null) for (int i = 0; i < s.length(); i++) { JSONObject x = s.optJSONObject(i); if (x != null && x.optBoolean("ok")) n++; }
        return n;
    }

    private static int total(JSONObject p) { JSONArray s = p == null ? null : p.optJSONArray("sets"); return s == null ? 0 : s.length(); }

    private static int firstOpen(JSONObject p) {
        JSONArray s = p == null ? null : p.optJSONArray("sets");
        if (s == null) return -1;
        for (int i = 0; i < s.length(); i++) { JSONObject x = s.optJSONObject(i); if (x == null || !x.optBoolean("ok")) return i; }
        return -1;
    }

    private static String fmt(double v) {
        if (v == Math.floor(v)) return String.valueOf((long) v);
        return String.format(Locale.US, "%.1f", v).replace('.', ',');
    }

    private static String unitLabel(String u) { return "br".equals(u) ? "barras" : "kg"; }

    private static String repsUnit(JSONObject p) {
        String mu = p.optString("mu", "reps");
        return "s".equals(mu) ? "seg" : "m".equals(mu) ? "m" : "reps";
    }

    private String setText(JSONObject p, JSONObject s) {
        if (s == null || (s.isNull("kg") && s.isNull("r"))) return "—";
        StringBuilder b = new StringBuilder();
        if (!p.optBoolean("nk") && !s.isNull("kg")) b.append(fmt(s.optDouble("kg"))).append(" ").append(unitLabel(p.optString("u"))).append(" × ");
        b.append(s.isNull("r") ? "–" : String.valueOf(s.optInt("r"))).append(" ").append(repsUnit(p));
        return b.toString();
    }

    // ---------- Telas ----------
    private void render() {
        try {
            hrView = null; loadView = null; repsView = null; clockView = null;
            switch (screen) {
                case DAY: renderDay(); break;
                case EXERCISE: renderExercise(); break;
                case SET: renderSet(); break;
                case REST: renderRest(); break;
                default: renderHome();
            }
            scroll.scrollTo(0, 0);
            scroll.requestFocus();
        } catch (Exception e) {
            LinearLayout col = column();
            col.addView(text("Algo deu errado", 15, true, ORANGE));
            col.addView(text(e.getClass().getSimpleName() + ": " + e.getMessage(), 11, false, MUTED), lp(6, 6));
            col.addView(button("Voltar ao início", CARD, v -> { screen = HOME; render(); }));
        }
    }

    private LinearLayout column() {
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(Gravity.CENTER_HORIZONTAL);
        col.setPadding(dp(24), dp(30), dp(24), dp(52));
        scroll.removeAllViews();
        scroll.addView(col, new ScrollView.LayoutParams(ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.WRAP_CONTENT));
        return col;
    }

    private int roleColor() { return "ela".equals(role()) ? TEAL : ORANGE; }

    private void renderHome() {
        LinearLayout col = column();
        col.addView(text("SPOTTER", 12, true, roleColor()));
        if (snap == null) {
            col.addView(text("Abra o Spotter no celular para enviar os treinos ao relógio.", 14, false, Color.WHITE), lp(10, 10));
            col.addView(button("Atualizar", CARD, v -> { fetch(); send("/spotter/refresh", "{}"); }));
            return;
        }
        Button toggle = button("Treino: " + ("ela".equals(role()) ? "Ela" : "Ele") + "  ⇄", CARD2, v -> {
            who = "ela".equals(role()) ? "ele" : "ela";
            prefs().edit().putString("who", who).apply();
            render();
        });
        toggle.setTextColor(roleColor());
        col.addView(toggle, lp(4, 8));

        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        if (!today.equals(snap.optString("date")))
            col.addView(text("Dados de outro dia. Abra o Spotter no celular para atualizar.", 12, false, MUTED), lp(0, 6));

        String tid = snap.optString("today", "");
        JSONObject td = day(tid);
        if (td != null) {
            col.addView(text("HOJE", 11, true, MUTED), lp(2, 2));
            col.addView(dayButton(td, true), lp(0, 8));
        } else {
            col.addView(text("Hoje é descanso", 15, true, Color.WHITE), lp(2, 6));
        }
        col.addView(text("OUTROS DIAS", 11, true, MUTED), lp(4, 2));
        JSONArray a = days();
        for (int i = 0; i < a.length(); i++) {
            JSONObject d = a.optJSONObject(i);
            if (d == null || d.optString("id").equals(tid)) continue;
            col.addView(dayButton(d, false), lp(0, 6));
        }
        String at = new SimpleDateFormat("HH:mm", Locale.US).format(new Date(snap.optLong("updatedAt")));
        col.addView(text("Sincronizado às " + at, 11, false, MUTED), lp(8, 4));
        col.addView(button("Atualizar", CARD, v -> { fetch(); send("/spotter/refresh", "{}"); }));
    }

    private Button dayButton(JSONObject d, boolean big) {
        JSONArray ex = myEx(d);
        int dn = 0, tt = 0;
        for (int i = 0; i < ex.length(); i++) { JSONObject p = ex.optJSONObject(i).optJSONObject(role()); dn += done(p); tt += total(p); }
        String label = d.optString("chip") + "\n" + d.optString("title") + (dn > 0 ? "\n" + dn + "/" + tt + " séries" : "");
        Button b = button(label, big ? roleColor() : CARD, v -> { dayId = d.optString("id"); go(DAY); });
        if (big) b.setTextColor(Color.BLACK);
        return b;
    }

    private void renderDay() {
        JSONObject d = day(dayId);
        if (d == null) { screen = HOME; renderHome(); return; }
        LinearLayout col = column();
        col.addView(text(d.optString("chip"), 11, true, roleColor()));
        col.addView(text(d.optString("title"), 17, true, Color.WHITE), lp(2, 0));
        col.addView(text(d.optString("focus"), 12, false, MUTED), lp(0, 8));
        JSONArray ex = myEx(d);
        int dn = 0, tt = 0;
        for (int i = 0; i < ex.length(); i++) { JSONObject p = ex.optJSONObject(i).optJSONObject(role()); dn += done(p); tt += total(p); }
        col.addView(text(dn + " de " + tt + " séries", 12, true, dn == tt && tt > 0 ? GREEN : Color.WHITE), lp(0, 8));
        for (int i = 0; i < ex.length(); i++) {
            final int idx = i;
            JSONObject e = ex.optJSONObject(i), p = e.optJSONObject(role());
            int a = done(p), t = total(p);
            String sub = p.optString("s");
            JSONArray last = p.optJSONArray("last");
            if (last != null && last.length() > 0) sub += "\nÚltima: " + setText(p, last.optJSONObject(last.length() - 1));
            Button b = button((i + 1) + ". " + e.optString("n") + "  " + a + "/" + t + "\n" + sub,
                    a == t && t > 0 ? Color.parseColor("#1E3A26") : CARD, v -> { exIdx = idx; go(EXERCISE); });
            if (a == t && t > 0) b.setTextColor(GREEN);
            col.addView(b, lp(0, 6));
        }
        col.addView(button("Voltar", CARD2, v -> go(HOME)), lp(6, 0));
    }

    private void renderExercise() {
        JSONObject e = curEx(), p = curPerson();
        if (e == null || p == null) { screen = DAY; renderDay(); return; }
        LinearLayout col = column();
        col.addView(text(e.optString("n"), 16, true, Color.WHITE));
        if (!p.optString("v").isEmpty()) col.addView(text(p.optString("v"), 12, false, MUTED));
        col.addView(text(p.optString("s") + " · " + p.optString("d"), 12, true, roleColor()), lp(4, 4));
        hrView = text(heartRate > 0 ? "♥ " + heartRate + " bpm" : "", 12, false, Color.parseColor("#E5737A"));
        col.addView(hrView);

        JSONArray sets = p.optJSONArray("sets");
        int open = firstOpen(p);
        for (int i = 0; sets != null && i < sets.length(); i++) {
            final int si = i;
            JSONObject s = sets.optJSONObject(i);
            boolean ok = s != null && s.optBoolean("ok");
            String label = "Série " + (i + 1) + "   " + (ok ? "✓ " + setText(p, s) : (i == open ? "toque para registrar" : "—"));
            Button b = button(label, ok ? Color.parseColor("#1E3A26") : (i == open ? CARD2 : CARD), v -> openSet(si));
            if (ok) b.setTextColor(GREEN);
            col.addView(b, lp(4, 0));
        }

        JSONArray last = p.optJSONArray("last");
        if (last != null && last.length() > 0) {
            StringBuilder t = new StringBuilder("Última vez");
            String ld = p.optString("ld");
            if (ld.length() == 10) t.append(" (").append(ld.substring(8)).append("/").append(ld.substring(5, 7)).append(")");
            t.append(":");
            for (int i = 0; i < last.length(); i++) t.append("\n").append(i + 1).append(". ").append(setText(p, last.optJSONObject(i)));
            col.addView(text(t.toString(), 12, false, MUTED), lp(10, 4));
        }

        String tip = p.optString("t");
        if (!tip.isEmpty()) {
            if (showTip) col.addView(text(tip, 12, false, Color.WHITE), lp(6, 4));
            col.addView(button(showTip ? "Esconder dica" : "Ver dica técnica", CARD, v -> { showTip = !showTip; renderExercise(); }), lp(6, 0));
        }

        JSONArray all = myEx(day(dayId));
        LinearLayout nav = new LinearLayout(this);
        if (exIdx > 0) nav.addView(button("‹ Anterior", CARD, v -> { exIdx--; go(EXERCISE); }), new LinearLayout.LayoutParams(0, dp(46), 1f));
        if (exIdx > 0 && exIdx < all.length() - 1) nav.addView(new View(this), new LinearLayout.LayoutParams(dp(6), 1));
        if (exIdx < all.length() - 1) nav.addView(button("Próximo ›", CARD, v -> { exIdx++; go(EXERCISE); }), new LinearLayout.LayoutParams(0, dp(46), 1f));
        col.addView(nav, lp(8, 0));
        col.addView(button("Voltar ao dia", CARD2, v -> go(DAY)), lp(6, 0));
    }

    private void openSet(int si) {
        JSONObject p = curPerson(); if (p == null) return;
        setIdx = si;
        unit = p.optString("u", "kg"); if (unit.isEmpty()) unit = "kg";
        JSONArray sets = p.optJSONArray("sets"), last = p.optJSONArray("last");
        JSONObject today = sets != null ? sets.optJSONObject(si) : null;
        JSONObject before = si > 0 && sets != null ? sets.optJSONObject(si - 1) : null;
        JSONObject ref = last != null && last.length() > 0 ? last.optJSONObject(Math.min(si, last.length() - 1)) : null;
        load = 0; reps = p.optInt("hi", 10);
        if (ref != null) { if (!ref.isNull("kg")) load = ref.optDouble("kg", 0); if (!ref.isNull("r")) reps = ref.optInt("r", reps); }
        if (before != null && before.optBoolean("ok")) { if (!before.isNull("kg")) load = before.optDouble("kg", load); if (!before.isNull("r")) reps = before.optInt("r", reps); }
        if (today != null) { if (!today.isNull("kg")) load = today.optDouble("kg", load); if (!today.isNull("r")) reps = today.optInt("r", reps); }
        focusLoad = !p.optBoolean("nk");
        go(SET);
    }

    private void renderSet() {
        JSONObject e = curEx(), p = curPerson();
        if (e == null || p == null) { screen = EXERCISE; renderExercise(); return; }
        JSONObject s = p.optJSONArray("sets").optJSONObject(setIdx);
        boolean ok = s != null && s.optBoolean("ok");
        LinearLayout col = column();
        col.addView(text(e.optString("n"), 13, true, Color.WHITE));
        col.addView(text("Série " + (setIdx + 1) + " de " + total(p) + " · meta " + p.optString("s").replaceFirst("^\\s*\\d+\\s*×\\s*", ""), 12, false, roleColor()), lp(2, 6));

        if (!p.optBoolean("nk")) {
            loadView = text("", 24, true, Color.WHITE);
            loadView.setOnClickListener(v -> { focusLoad = true; updateValues(); });
            col.addView(stepper(loadView, -1, true));
            Button ub = button("Unidade: " + unitLabel(unit), CARD, v -> { unit = "br".equals(unit) ? "kg" : "br"; updateValues(); ((Button) v).setText("Unidade: " + unitLabel(unit)); });
            ub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            col.addView(ub, lp(2, 6));
        }
        repsView = text("", 24, true, Color.WHITE);
        repsView.setOnClickListener(v -> { focusLoad = false; updateValues(); });
        col.addView(stepper(repsView, -1, false), lp(0, 4));
        col.addView(text("Gire a moldura para ajustar o valor destacado", 10, false, MUTED), lp(0, 6));
        updateValues();

        Button done = button(ok ? "Salvar" : "✓  Concluir série", GREEN, v -> saveSet(true, !ok));
        done.setTextColor(Color.BLACK);
        col.addView(done, lp(2, 0));
        if (ok) col.addView(button("Desmarcar série", CARD, v -> saveSet(false, false)), lp(6, 0));
        col.addView(button("Voltar", CARD2, v -> go(EXERCISE)), lp(6, 0));
    }

    private void updateValues() {
        JSONObject p = curPerson(); if (p == null) return;
        if (loadView != null) {
            loadView.setText(fmt(load) + " " + unitLabel(unit));
            loadView.setTextColor(focusLoad ? roleColor() : Color.WHITE);
        }
        if (repsView != null) {
            repsView.setText(reps + " " + repsUnit(p));
            repsView.setTextColor(!focusLoad || loadView == null ? roleColor() : Color.WHITE);
        }
    }

    private void saveSet(boolean ok, boolean startRest) {
        JSONObject e = curEx(), p = curPerson(); if (e == null || p == null) return;
        boolean nk = p.optBoolean("nk");
        try {
            JSONObject set = new JSONObject();
            set.put("kg", nk ? JSONObject.NULL : load);
            set.put("r", reps);
            set.put("ok", ok);
            p.getJSONArray("sets").put(setIdx, set);
            if (!nk) p.put("u", unit);
            prefs().edit().putString("snap", snap.toString()).apply();

            JSONObject log = new JSONObject();
            log.put("date", snap.optString("date"));
            log.put("day", dayId);
            log.put("k", e.optString("k"));
            log.put("p", role());
            log.put("s", setIdx);
            log.put("kg", nk ? JSONObject.NULL : load);
            log.put("r", reps);
            log.put("ok", ok);
            if (!nk) log.put("u", unit);
            log.put("at", System.currentTimeMillis());
            send("/spotter/log", log.toString());
        } catch (Exception ignored) { }
        buzz(new long[]{0, 40});
        int rest = p.optInt("rest", 0);
        if (ok && startRest && rest > 0) { timerEnd = System.currentTimeMillis() + rest * 1000L; go(REST); h.removeCallbacks(tick); tickTimer(); }
        else go(EXERCISE);
    }

    // ---------- Descanso ----------
    private void renderRest() {
        LinearLayout col = column();
        col.setGravity(Gravity.CENTER);
        col.setPadding(dp(24), dp(34), dp(24), dp(34));
        col.addView(text("DESCANSO", 12, true, roleColor()));
        clockView = text("", 46, true, Color.WHITE);
        col.addView(clockView, lp(2, 2));
        hrView = text(heartRate > 0 ? "♥ " + heartRate + " bpm" : "", 13, true, Color.parseColor("#E5737A"));
        col.addView(hrView, lp(0, 6));
        JSONObject p = curPerson();
        int nxt = firstOpen(p);
        if (p != null && nxt >= 0) col.addView(text("Próxima: série " + (nxt + 1) + " de " + total(p), 12, false, MUTED), lp(0, 8));
        LinearLayout row = new LinearLayout(this);
        row.addView(button("+15s", CARD, v -> timerEnd += 15000), new LinearLayout.LayoutParams(0, dp(46), 1f));
        row.addView(new View(this), new LinearLayout.LayoutParams(dp(8), 1));
        row.addView(button("Pular", CARD, v -> stopTimer()), new LinearLayout.LayoutParams(0, dp(46), 1f));
        col.addView(row, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
    }

    private void tickTimer() {
        if (screen != REST || clockView == null) return;
        long left = timerEnd - System.currentTimeMillis();
        if (left <= 0) { buzz(new long[]{0, 400, 150, 400, 150, 400}); stopTimer(); return; }
        long sec = (left + 999) / 1000;
        clockView.setText(String.format(Locale.US, "%d:%02d", sec / 60, sec % 60));
        h.postDelayed(tick, 250);
    }

    private void stopTimer() {
        h.removeCallbacks(tick);
        JSONObject p = curPerson();
        int nxt = firstOpen(p);
        if (nxt >= 0) openSet(nxt); else go(EXERCISE);
    }

    private void buzz(long[] pattern) {
        try {
            Vibrator v;
            if (Build.VERSION.SDK_INT >= 31) v = ((VibratorManager) getSystemService(Context.VIBRATOR_MANAGER_SERVICE)).getDefaultVibrator();
            else v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            v.vibrate(VibrationEffect.createWaveform(pattern, -1));
        } catch (Exception ignored) { }
    }

    // ---------- Componentes ----------
    private int dp(int v) { return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics()); }

    private LinearLayout.LayoutParams lp(int topDp, int bottomDp) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        p.topMargin = dp(topDp); p.bottomMargin = dp(bottomDp);
        return p;
    }

    private TextView text(String s, int sp, boolean bold, int color) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
        t.setGravity(Gravity.CENTER);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    private Button button(String label, int bg, View.OnClickListener click) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        b.setTextColor(Color.WHITE);
        b.setMinWidth(0); b.setMinimumWidth(0);
        b.setMinHeight(dp(44)); b.setMinimumHeight(dp(44));
        b.setPadding(dp(10), dp(8), dp(10), dp(8));
        b.setStateListAnimator(null);
        GradientDrawable d = new GradientDrawable();
        d.setColor(bg); d.setCornerRadius(dp(20));
        b.setBackground(d);
        b.setOnClickListener(click);
        return b;
    }

    // [ − ] valor [ + ]   toque: ±1  ·  toque longo: ±5
    private LinearLayout stepper(TextView value, int unused, boolean isLoad) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        Button m = button("−", CARD, v -> step(isLoad, -1));
        m.setOnLongClickListener(v -> { step(isLoad, -5); return true; });
        Button p = button("+", CARD, v -> step(isLoad, 1));
        p.setOnLongClickListener(v -> { step(isLoad, 5); return true; });
        m.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22); p.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        m.setPadding(0, 0, 0, 0); p.setPadding(0, 0, 0, 0);
        row.addView(m, new LinearLayout.LayoutParams(dp(46), dp(46)));
        row.addView(value, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(p, new LinearLayout.LayoutParams(dp(46), dp(46)));
        return row;
    }

    private void step(boolean isLoad, int n) {
        focusLoad = isLoad;
        if (isLoad) load = Math.max(0, load + n); else reps = Math.max(0, reps + n);
        updateValues();
    }
}
