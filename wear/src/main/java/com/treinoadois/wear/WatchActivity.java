package com.treinoadois.wear;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
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
 * Spotter no relógio: treino do dia, registro de séries e descanso.
 * Os dados vêm do celular (Data Layer) e as séries voltam para o celular, que salva na nuvem.
 */
public class WatchActivity extends Activity implements DataClient.OnDataChangedListener {
    private static final String PATH_TODAY = "/spotter/today";
    private static final int TEAL = Color.parseColor("#4CC2B4");
    private static final int ORANGE = Color.parseColor("#F0954F");
    private static final int GREEN = Color.parseColor("#5FB56E");
    private static final int CARD = Color.parseColor("#22292A");
    private static final int MUTED = Color.parseColor("#9AA5A1");

    private JSONObject snap;
    private int screen = 0;          // 0 = lista, 1 = exercício, 2 = descanso
    private int cur = -1;            // exercício aberto
    private double load = 0;         // carga em edição
    private int reps = 0;            // repetições em edição
    private boolean edited = false;
    private long timerEnd = 0;
    private final Handler h = new Handler(Looper.getMainLooper());
    private final Runnable tick = this::tickTimer;
    private ScrollView scroll;
    private TextView clockView;

    // ---------- Ciclo de vida ----------
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.BLACK);
        scroll.setFillViewport(true);
        setContentView(scroll);
        String cached = prefs().getString("snap", null);
        if (cached != null) { try { snap = new JSONObject(cached); } catch (Exception ignored) { } }
        render();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Wearable.getDataClient(this).addListener(this);
        fetch();
        send("/spotter/refresh", "{}");
    }

    @Override
    protected void onPause() {
        Wearable.getDataClient(this).removeListener(this);
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        if (screen == 2) { stopTimer(); return; }
        if (screen == 1) { screen = 0; render(); return; }
        super.onBackPressed();
    }

    private SharedPreferences prefs() { return getSharedPreferences("spotter", MODE_PRIVATE); }

    // ---------- Dados ----------
    private void fetch() {
        Wearable.getDataClient(this).getDataItems(Uri.parse("wear://*" + PATH_TODAY))
                .addOnSuccessListener(buf -> {
                    String best = null; long bestT = -1;
                    for (DataItem it : buf) {
                        com.google.android.gms.wearable.DataMap m = DataMapItem.fromDataItem(it).getDataMap();
                        long t = m.getLong("t", 0);
                        if (t > bestT) { bestT = t; best = m.getString("json"); }
                    }
                    buf.release();
                    if (best != null) applySnap(best);
                });
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
            if (snap != null && s.optLong("updatedAt") < snap.optLong("updatedAt")) return;
            snap = s;
            prefs().edit().putString("snap", json).apply();
            if (screen != 2 && !(screen == 1 && edited)) render();
        } catch (Exception ignored) { }
    }

    private void send(String path, String payload) {
        final byte[] data = payload.getBytes(StandardCharsets.UTF_8);
        Wearable.getNodeClient(this).getConnectedNodes().addOnSuccessListener(nodes -> {
            for (Node n : nodes) Wearable.getMessageClient(this).sendMessage(n.getId(), path, data);
        });
    }

    private JSONArray exercises() { return snap == null ? new JSONArray() : snap.optJSONArray("ex") == null ? new JSONArray() : snap.optJSONArray("ex"); }

    private static int doneCount(JSONObject ex) {
        JSONArray sets = ex.optJSONArray("sets"); int n = 0;
        if (sets != null) for (int i = 0; i < sets.length(); i++) { JSONObject s = sets.optJSONObject(i); if (s != null && s.optBoolean("ok")) n++; }
        return n;
    }

    private static int nextSet(JSONObject ex) {
        JSONArray sets = ex.optJSONArray("sets");
        if (sets == null) return 0;
        for (int i = 0; i < sets.length(); i++) { JSONObject s = sets.optJSONObject(i); if (s == null || !s.optBoolean("ok")) return i; }
        return -1;
    }

    private static String fmt(double v) {
        if (v == Math.floor(v)) return String.valueOf((long) v);
        return String.format(Locale.US, "%.1f", v).replace('.', ',');
    }

    private String loadUnit(JSONObject ex) { return "br".equals(ex.optString("u")) ? "barras" : "kg"; }

    private String repsUnit(JSONObject ex) {
        String mu = ex.optString("mu", "reps");
        return "s".equals(mu) ? "seg" : "m".equals(mu) ? "m" : "reps";
    }

    // Valores sugeridos para a próxima série: hoje > última vez > meta
    private void prefill(JSONObject ex) {
        int s = nextSet(ex); if (s < 0) s = 0;
        JSONArray sets = ex.optJSONArray("sets"), last = ex.optJSONArray("last");
        JSONObject today = sets != null ? sets.optJSONObject(s) : null;
        JSONObject prevSet = null;
        if (s > 0 && sets != null) prevSet = sets.optJSONObject(s - 1);
        JSONObject ref = null;
        if (last != null && last.length() > 0) ref = last.optJSONObject(Math.min(s, last.length() - 1));
        load = 0; reps = ex.optInt("hi", 10);
        if (ref != null) { if (!ref.isNull("kg")) load = ref.optDouble("kg", 0); if (!ref.isNull("r")) reps = ref.optInt("r", reps); }
        if (prevSet != null && prevSet.optBoolean("ok")) { if (!prevSet.isNull("kg")) load = prevSet.optDouble("kg", load); if (!prevSet.isNull("r")) reps = prevSet.optInt("r", reps); }
        if (today != null) { if (!today.isNull("kg")) load = today.optDouble("kg", load); if (!today.isNull("r")) reps = today.optInt("r", reps); }
        edited = false;
    }

    // ---------- Telas ----------
    private void render() {
        if (screen == 1) renderExercise(); else renderList();
        scroll.scrollTo(0, 0);
    }

    private LinearLayout column() {
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(Gravity.CENTER_HORIZONTAL);
        int side = dp(22);
        col.setPadding(side, dp(26), side, dp(46));
        scroll.removeAllViews();
        scroll.addView(col, new ScrollView.LayoutParams(ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.WRAP_CONTENT));
        return col;
    }

    private int roleColor() { return snap != null && "ela".equals(snap.optString("role")) ? TEAL : ORANGE; }

    private void renderList() {
        LinearLayout col = column();
        col.addView(text("SPOTTER", 12, true, roleColor(), Gravity.CENTER));
        if (snap == null) {
            col.addView(text("Abra o Spotter no celular para enviar o treino ao relógio.", 14, false, Color.WHITE, Gravity.CENTER), lp(0, dp(10)));
            col.addView(button("Atualizar", CARD, v -> { fetch(); send("/spotter/refresh", "{}"); }), lp(dp(8), 0));
            return;
        }
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        boolean stale = !today.equals(snap.optString("date"));
        JSONObject day = snap.optJSONObject("day");
        if (stale) {
            col.addView(text("Dados de outro dia. Abra o Spotter no celular para atualizar.", 13, false, MUTED, Gravity.CENTER), lp(dp(4), dp(6)));
        }
        if (day == null) {
            col.addView(text("Hoje é descanso", 18, true, Color.WHITE, Gravity.CENTER), lp(dp(6), dp(4)));
            col.addView(text("Uma caminhada leve ajuda na recuperação.", 13, false, MUTED, Gravity.CENTER));
        } else {
            col.addView(text(day.optString("title"), 18, true, Color.WHITE, Gravity.CENTER), lp(dp(4), 0));
            col.addView(text(day.optString("focus"), 12, false, MUTED, Gravity.CENTER), lp(0, dp(8)));
            JSONArray exs = exercises();
            for (int i = 0; i < exs.length(); i++) {
                final int idx = i;
                JSONObject ex = exs.optJSONObject(i);
                int done = doneCount(ex), total = ex.optJSONArray("sets") != null ? ex.optJSONArray("sets").length() : 0;
                boolean complete = total > 0 && done == total;
                Button b = button((i + 1) + ". " + ex.optString("n") + "\n" + done + "/" + total + " séries",
                        complete ? Color.parseColor("#1E3A26") : CARD, v -> openExercise(idx));
                b.setTextColor(complete ? GREEN : Color.WHITE);
                col.addView(b, lp(dp(4), dp(4)));
            }
        }
        String at = new SimpleDateFormat("HH:mm", Locale.US).format(new Date(snap.optLong("updatedAt")));
        col.addView(text("Sincronizado às " + at, 11, false, MUTED, Gravity.CENTER), lp(dp(10), dp(4)));
        col.addView(button("Atualizar", CARD, v -> { fetch(); send("/spotter/refresh", "{}"); }), lp(dp(2), 0));
    }

    private void openExercise(int idx) {
        cur = idx; screen = 1;
        prefill(exercises().optJSONObject(idx));
        render();
    }

    private TextView loadLabel, repsLabel;

    private void renderExercise() {
        JSONObject ex = exercises().optJSONObject(cur);
        if (ex == null) { screen = 0; renderList(); return; }
        LinearLayout col = column();
        col.addView(text(ex.optString("n"), 16, true, Color.WHITE, Gravity.CENTER));
        if (!ex.optString("v").isEmpty()) col.addView(text(ex.optString("v"), 12, false, MUTED, Gravity.CENTER));
        col.addView(text(ex.optString("s") + " · " + ex.optString("d"), 12, true, roleColor(), Gravity.CENTER), lp(dp(2), dp(6)));

        int s = nextSet(ex);
        int total = ex.optJSONArray("sets") != null ? ex.optJSONArray("sets").length() : 0;
        if (s < 0) {
            col.addView(text("Exercício concluído", 16, true, GREEN, Gravity.CENTER), lp(dp(8), dp(8)));
            col.addView(button("Próximo exercício", CARD, v -> {
                if (cur + 1 < exercises().length()) openExercise(cur + 1); else { screen = 0; render(); }
            }), lp(dp(4), 0));
        } else {
            col.addView(text("Série " + (s + 1) + " de " + total, 13, true, Color.WHITE, Gravity.CENTER), lp(dp(2), dp(4)));
            boolean nk = ex.optBoolean("nk");
            if (!nk) {
                loadLabel = text(fmt(load) + " " + loadUnit(ex), 22, true, Color.WHITE, Gravity.CENTER);
                col.addView(stepper(loadLabel, v -> { load = Math.max(0, load - 1); edited = true; updateLabels(ex); },
                        v -> { load = load + 1; edited = true; updateLabels(ex); },
                        v -> { load = Math.max(0, load - 5); edited = true; updateLabels(ex); },
                        v -> { load = load + 5; edited = true; updateLabels(ex); }), lp(dp(2), dp(2)));
            } else loadLabel = null;
            repsLabel = text(reps + " " + repsUnit(ex), 22, true, Color.WHITE, Gravity.CENTER);
            col.addView(stepper(repsLabel, v -> { reps = Math.max(0, reps - 1); edited = true; updateLabels(ex); },
                    v -> { reps = reps + 1; edited = true; updateLabels(ex); },
                    v -> { reps = Math.max(0, reps - 5); edited = true; updateLabels(ex); },
                    v -> { reps = reps + 5; edited = true; updateLabels(ex); }), lp(dp(2), dp(6)));
            Button ok = button("✓  Concluir série", GREEN, v -> completeSet(s));
            ok.setTextColor(Color.BLACK);
            col.addView(ok, lp(dp(2), dp(2)));
        }
        String hist = historyText(ex);
        if (!hist.isEmpty()) col.addView(text(hist, 11, false, MUTED, Gravity.CENTER), lp(dp(8), dp(2)));
        col.addView(button("Voltar", CARD, v -> { screen = 0; render(); }), lp(dp(6), 0));
    }

    private void updateLabels(JSONObject ex) {
        if (loadLabel != null) loadLabel.setText(fmt(load) + " " + loadUnit(ex));
        if (repsLabel != null) repsLabel.setText(reps + " " + repsUnit(ex));
    }

    private String historyText(JSONObject ex) {
        StringBuilder sb = new StringBuilder();
        JSONArray sets = ex.optJSONArray("sets");
        if (sets != null) {
            StringBuilder t = new StringBuilder();
            for (int i = 0; i < sets.length(); i++) {
                JSONObject s = sets.optJSONObject(i);
                if (s != null && s.optBoolean("ok")) {
                    if (t.length() > 0) t.append(" · ");
                    if (!ex.optBoolean("nk") && !s.isNull("kg")) t.append(fmt(s.optDouble("kg"))).append("×");
                    t.append(s.isNull("r") ? "–" : String.valueOf(s.optInt("r")));
                }
            }
            if (t.length() > 0) sb.append("Hoje: ").append(t);
        }
        JSONArray last = ex.optJSONArray("last");
        if (last != null && last.length() > 0) {
            StringBuilder t = new StringBuilder();
            for (int i = 0; i < last.length(); i++) {
                JSONObject s = last.optJSONObject(i);
                if (t.length() > 0) t.append(" · ");
                if (!ex.optBoolean("nk") && !s.isNull("kg")) t.append(fmt(s.optDouble("kg"))).append("×");
                t.append(s.isNull("r") ? "–" : String.valueOf(s.optInt("r")));
            }
            if (sb.length() > 0) sb.append("\n");
            sb.append("Última vez: ").append(t);
        }
        return sb.toString();
    }

    private void completeSet(int s) {
        JSONObject ex = exercises().optJSONObject(cur);
        if (ex == null) return;
        try {
            JSONObject set = new JSONObject();
            set.put("kg", ex.optBoolean("nk") ? JSONObject.NULL : load);
            set.put("r", reps);
            set.put("ok", true);
            ex.getJSONArray("sets").put(s, set);
            prefs().edit().putString("snap", snap.toString()).apply();

            JSONObject log = new JSONObject();
            log.put("date", snap.optString("date"));
            log.put("day", snap.getJSONObject("day").optString("id"));
            log.put("k", ex.optString("k"));
            log.put("p", snap.optString("role"));
            log.put("s", s);
            log.put("kg", ex.optBoolean("nk") ? JSONObject.NULL : load);
            log.put("r", reps);
            log.put("ok", true);
            log.put("at", System.currentTimeMillis());
            send("/spotter/log", log.toString());
        } catch (Exception ignored) { }
        buzz(new long[]{0, 40});
        edited = false;
        int rest = ex.optInt("rest", 0);
        if (rest > 0) startTimer(rest); else render();
    }

    // ---------- Descanso ----------
    private void startTimer(int seconds) {
        timerEnd = System.currentTimeMillis() + seconds * 1000L;
        screen = 2;
        LinearLayout col = column();
        col.setGravity(Gravity.CENTER);
        col.setPadding(dp(22), dp(40), dp(22), dp(30));
        col.addView(text("DESCANSO", 12, true, roleColor(), Gravity.CENTER));
        clockView = text("", 46, true, Color.WHITE, Gravity.CENTER);
        col.addView(clockView, lp(dp(4), dp(10)));
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER);
        row.addView(button("+15s", CARD, v -> timerEnd += 15000), new LinearLayout.LayoutParams(0, dp(48), 1f));
        View gap = new View(this); row.addView(gap, new LinearLayout.LayoutParams(dp(8), 1));
        row.addView(button("Pular", CARD, v -> stopTimer()), new LinearLayout.LayoutParams(0, dp(48), 1f));
        col.addView(row, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        h.removeCallbacks(tick);
        tickTimer();
    }

    private void tickTimer() {
        if (screen != 2) return;
        long left = timerEnd - System.currentTimeMillis();
        if (left <= 0) {
            buzz(new long[]{0, 400, 150, 400, 150, 400});
            stopTimer();
            return;
        }
        long sec = (left + 999) / 1000;
        clockView.setText(String.format(Locale.US, "%d:%02d", sec / 60, sec % 60));
        h.postDelayed(tick, 250);
    }

    private void stopTimer() {
        h.removeCallbacks(tick);
        screen = 1;
        JSONObject ex = exercises().optJSONObject(cur);
        if (ex != null) prefill(ex);
        render();
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

    private LinearLayout.LayoutParams lp(int top, int bottom) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        p.topMargin = top; p.bottomMargin = bottom;
        return p;
    }

    private TextView text(String s, int sp, boolean bold, int color, int gravity) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
        t.setGravity(gravity);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    private Button button(String label, int bg, View.OnClickListener click) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        b.setTextColor(Color.WHITE);
        b.setMinHeight(dp(44));
        b.setPadding(dp(10), dp(6), dp(10), dp(6));
        GradientDrawable d = new GradientDrawable();
        d.setColor(bg); d.setCornerRadius(dp(22));
        b.setBackground(d);
        b.setOnClickListener(click);
        return b;
    }

    // [ − ]  valor  [ + ]  (toque longo: ±5)
    private LinearLayout stepper(TextView value, View.OnClickListener minus, View.OnClickListener plus,
                                 View.OnClickListener minus5, View.OnClickListener plus5) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        Button m = button("−", CARD, minus); m.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        m.setOnLongClickListener(v -> { minus5.onClick(v); return true; });
        Button p = button("+", CARD, plus); p.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        p.setOnLongClickListener(v -> { plus5.onClick(v); return true; });
        row.addView(m, new LinearLayout.LayoutParams(dp(48), dp(48)));
        row.addView(value, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(p, new LinearLayout.LayoutParams(dp(48), dp(48)));
        return row;
    }
}
