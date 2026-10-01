package com.example.projectlauncher;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.view.*;
import android.view.animation.*;
import android.widget.*;
import org.json.*;
import java.io.InputStream;

public class MainActivity extends Activity {

    static final int PICK_IMAGE = 700;
    static final int PICK_BUTTON_IMAGE = 701;

    LinearLayout root, content, bottom;
    ScrollView scroll;
    SharedPreferences prefs;

    JSONArray projects;
    JSONArray homeButtons;

    String pendingImage = "";
    String pendingButtonImage = "";

    ImageView editorPreview;
    ImageView buttonEditorPreview;
    TextView editorPickLabel;

    float swipeDownX, swipeDownY;
    boolean animating = false;
    int enterDir = 0;

    // Set by sub-screens (project details, error screen) so Back returns to the right place.
    Runnable backAction = null;

    int accent = Color.rgb(139, 92, 246);

    static final int BUTTON_BORDER_COLOR = Color.rgb(34, 211, 238);
    static final int BUTTON_LABEL_COLOR = Color.WHITE;

    final int cyan = Color.rgb(34, 211, 238);
    final int green = Color.rgb(52, 211, 153);
    final int red = Color.rgb(251, 113, 133);
    final int lightRed = Color.rgb(255, 150, 150);
    final int darkRedText = Color.rgb(120, 20, 30);

    // theme colours (set in applyPalette)
    int bg = Color.rgb(4, 7, 20);
    int panel = Color.rgb(10, 17, 39);
    int panel2 = Color.rgb(14, 25, 55);
    int text = Color.rgb(245, 247, 255);
    int muted = Color.rgb(145, 158, 194);
    int line = Color.rgb(31, 55, 102);
    int link = Color.rgb(34, 211, 238);
    int descColor = Color.rgb(251, 191, 36);
    int dialogFill = Color.rgb(9, 15, 34);
    int dialogLine = Color.rgb(42, 66, 120);

    float den;

    int dp(float v) {
        return (int) (v * den + 0.5f);
    }

    boolean light() {
        return prefs.getBoolean("light_mode", false);
    }

    // ============================================================
    // TOUCH / SWIPE NAVIGATION
    // ============================================================

    @Override
    public boolean dispatchTouchEvent(MotionEvent e) {

        if (e.getAction() == MotionEvent.ACTION_DOWN) {
            swipeDownX = e.getX();
            swipeDownY = e.getY();
        } else if (e.getAction() == MotionEvent.ACTION_UP) {
            float dx = e.getX() - swipeDownX;
            float dy = e.getY() - swipeDownY;
            // horizontal swipes only (ignore normal vertical scrolling)
            if (Math.abs(dx) > dp(110) && Math.abs(dx) > Math.abs(dy) * 2) {
                swipePage(currentPage(), dx < 0 ? 1 : -1);
            }
        }

        return super.dispatchTouchEvent(e);
    }

    int currentPage() {
        return prefs.getInt("current_page", 0);
    }

    // ============================================================
    // BACK BUTTON
    // Details / error screen -> go back one level
    // Other tabs -> Home
    // Home -> exit app
    // ============================================================

    @Override
    public void onBackPressed() {

        if (backAction != null) {
            Runnable r = backAction;
            backAction = null;
            r.run();
            return;
        }

        if (currentPage() != 0) {
            showHome();
            return;
        }

        super.onBackPressed();
    }

    // ============================================================
    // CREATE
    // ============================================================

    @Override
    public void onCreate(Bundle b) {

        super.onCreate(b);

        den = getResources().getDisplayMetrics().density;

        prefs = getSharedPreferences("project_launcher", 0);

        load();

        accent = prefs.getInt("accent", accent);

        applyPalette();
        applySystemBars();

        showHome();
    }

    // ============================================================
    // DATA
    // ============================================================

    void load() {

        try {
            projects = new JSONArray(prefs.getString("projects", "[]"));
        } catch (Exception e) {
            projects = new JSONArray();
        }

        try {
            homeButtons = new JSONArray(prefs.getString("home_buttons", "[]"));
        } catch (Exception e) {
            homeButtons = new JSONArray();
        }
    }

    void save() {
        prefs.edit().putString("projects", projects.toString()).apply();
    }

    void saveHomeButtons() {
        prefs.edit().putString("home_buttons", homeButtons.toString()).apply();
    }

    // ============================================================
    // COLORS / THEME
    // ============================================================

    void applyPalette() {

        if (light()) {
            // glassmorphism palette: translucent white panels over a soft gradient
            bg = Color.rgb(224, 231, 255);
            panel = Color.argb(150, 255, 255, 255);
            panel2 = Color.argb(140, 255, 255, 255);
            text = Color.rgb(25, 31, 48);
            muted = Color.rgb(84, 97, 128);
            line = Color.argb(220, 255, 255, 255);
            link = Color.rgb(8, 145, 178);
            descColor = Color.rgb(180, 83, 9);
            dialogFill = Color.argb(225, 255, 255, 255);
            dialogLine = Color.argb(255, 255, 255, 255);
        } else {
            bg = Color.rgb(4, 7, 20);
            panel = Color.rgb(10, 17, 39);
            panel2 = Color.rgb(14, 25, 55);
            text = Color.rgb(245, 247, 255);
            muted = Color.rgb(145, 158, 194);
            line = Color.rgb(31, 55, 102);
            link = Color.rgb(34, 211, 238);
            descColor = Color.rgb(251, 191, 36);
            dialogFill = Color.rgb(9, 15, 34);
            dialogLine = Color.rgb(42, 66, 120);
        }
    }

    void applySystemBars() {

        boolean light = light();

        getWindow().setStatusBarColor(bg);
        getWindow().setNavigationBarColor(light ? Color.rgb(226, 236, 252) : Color.rgb(5, 9, 23));

        int flags = 0;

        if (light) {
            if (Build.VERSION.SDK_INT >= 23) flags |= 0x2000; // light status bar icons
            if (Build.VERSION.SDK_INT >= 26) flags |= 0x10;   // light navigation bar icons
        }

        if (Build.VERSION.SDK_INT >= 23) {
            getWindow().getDecorView().setSystemUiVisibility(flags);
        }
    }

    int darker(int c) {
        return Color.rgb(
                (int) (Color.red(c) * 0.55f),
                (int) (Color.green(c) * 0.55f),
                (int) (Color.blue(c) * 0.55f)
        );
    }

    // ============================================================
    // DRAWABLES / UI HELPERS
    // ============================================================

    GradientDrawable bg(int color, float r) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(r));
        return g;
    }

    GradientDrawable outline(int fill, int line, float r) {
        GradientDrawable g = bg(fill, r);
        g.setStroke(dp(1), line);
        return g;
    }

    GradientDrawable gradient(int c1, int c2, float r) {
        GradientDrawable g = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{c1, c2}
        );
        g.setCornerRadius(dp(r));
        return g;
    }

    TextView tv(String s, float size, int color) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setFontFeatureSettings("kern");
        return t;
    }

    Button button(String label) {

        Button b = new Button(this);

        b.setText(label);
        b.setTextColor(Color.WHITE);
        b.setTextSize(13);
        b.setAllCaps(false);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setGravity(Gravity.CENTER);
        b.setBackground(gradient(accent, darker(accent), 28));
        b.setPadding(dp(12), 0, dp(12), 0);

        return b;
    }

    Button smallButton(String s) {
        Button b = button(s);
        b.setTextSize(11);
        b.setPadding(dp(4), 0, dp(4), 0);
        return b;
    }

    // Light red delete button
    Button deleteButton(String label) {
        Button b = smallButton(label);
        b.setTextSize(12);
        b.setTextColor(darkRedText);
        b.setBackground(bg(lightRed, 22));
        return b;
    }

    Button flatButton(String label, int color) {
        Button b = button(label);
        b.setTextColor(color);
        b.setBackground(ColorDrawableCompat.transparent());
        return b;
    }

    Space gap(int h) {
        Space s = new Space(this);
        s.setLayoutParams(new LinearLayout.LayoutParams(1, dp(h)));
        return s;
    }

    // Rounded corners for any view (used for all images)
    void round(View v, float r) {
        final float rad = dp(r);
        v.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, android.graphics.Outline o) {
                o.setRoundRect(0, 0, view.getWidth(), view.getHeight(), rad);
            }
        });
        v.setClipToOutline(true);
    }

    // ============================================================
    // BASE LAYOUT
    // ============================================================

    void base(final int active) {

        backAction = null;

        prefs.edit().putInt("current_page", active).apply();

        boolean light = light();

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);

        if (light) {
            root.setBackground(new GlassBgDrawable(accent));
        } else {
            root.setBackgroundColor(bg);
        }

        setContentView(root);

        root.setOnApplyWindowInsetsListener(
                (v, insets) -> {
                    int top = insets.getSystemWindowInsetTop();
                    int bottomInset = insets.getSystemWindowInsetBottom();
                    v.setPadding(0, top, 0, bottomInset);
                    return insets;
                }
        );

        root.requestApplyInsets();

        scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);

        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(18), dp(18), dp(18), dp(18));

        scroll.addView(content);

        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        // BOTTOM NAVIGATION (theme aware, soft wave)
        bottom = new WaveNavLayout(this, light, accent);
        bottom.setGravity(Gravity.CENTER);
        bottom.setPadding(dp(10), dp(8), dp(10), dp(6));

        GradientDrawable nb;

        if (light) {
            nb = bg(Color.argb(175, 255, 255, 255), 0);
            nb.setStroke(dp(1), Color.argb(235, 255, 255, 255));
        } else {
            nb = gradient(Color.rgb(9, 15, 36), Color.rgb(5, 9, 23), 0);
            nb.setStroke(dp(1), Color.rgb(24, 40, 80));
        }

        float r = dp(24);
        nb.setCornerRadii(new float[]{r, r, r, r, 0, 0, 0, 0});
        bottom.setBackground(nb);

        root.addView(bottom, new LinearLayout.LayoutParams(-1, dp(78)));

        navItem("\uD83C\uDFE0", "Home", active == 0, () -> showHome());
        navItem("\uD83D\uDCC1", "Projects", active == 1, () -> showProjects());
        navItem("\u26A1", "Buttons", active == 2, () -> showButtons());
        navItem("\u2699\uFE0F", "Settings", active == 3, () -> showManage());

        if (enterDir != 0) {
            playEnter(enterDir);
            enterDir = 0;
        }
    }

    // ============================================================
    // PAGE SLIDE TRANSITION (smooth horizontal slide + fade)
    // ============================================================

    void swipePage(int current, final int delta) {

        if (animating || scroll == null) return;

        animating = true;

        int w = getResources().getDisplayMetrics().widthPixels;
        final int next = (current + delta + 4) % 4;

        // current page slides out in the swipe direction
        scroll.animate()
                .translationX(delta > 0 ? -w * 0.35f : w * 0.35f)
                .alpha(0f)
                .setDuration(150)
                .setInterpolator(new AccelerateInterpolator())
                .withEndAction(() -> {
                    enterDir = delta;
                    showPage(next);
                    animating = false;
                })
                .start();
    }

    void playEnter(int dir) {

        int w = getResources().getDisplayMetrics().widthPixels;

        // new page slides in from the opposite side
        scroll.setTranslationX(dir > 0 ? w * 0.35f : -w * 0.35f);
        scroll.setAlpha(0f);

        scroll.animate()
                .translationX(0f)
                .alpha(1f)
                .setDuration(230)
                .setInterpolator(new DecelerateInterpolator(1.6f))
                .start();
    }

    // ============================================================
    // NAV ITEM
    // ============================================================

    void navItem(String icon, String label, boolean active, final Runnable action) {

        boolean light = light();

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setClickable(true);

        int inactive = light ? Color.rgb(94, 106, 138) : Color.rgb(117, 130, 166);
        int activeLabel = light ? darker(accent) : Color.WHITE;

        TextView i = tv(icon, 18, Color.WHITE);
        i.setGravity(Gravity.CENTER);

        if (active) {
            i.setBackground(gradient(accent, darker(accent), 16));
        } else {
            i.setAlpha(0.75f);
        }

        TextView l = tv(label, 11, active ? activeLabel : inactive);
        l.setGravity(Gravity.CENTER);
        l.setTypeface(Typeface.DEFAULT, active ? Typeface.BOLD : Typeface.NORMAL);

        box.addView(i, new LinearLayout.LayoutParams(dp(54), dp(32)));
        box.addView(l, new LinearLayout.LayoutParams(-2, dp(20)));

        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, -1, 1);
        p.setMargins(dp(3), 0, dp(3), 0);

        bottom.addView(box, p);

        box.setOnClickListener(v -> action.run());
    }

    // ============================================================
    // HEADER
    // ============================================================

    LinearLayout header(String title, String sub) {

        LinearLayout h = new LinearLayout(this);
        h.setGravity(Gravity.CENTER_VERTICAL);
        h.setPadding(0, 0, 0, dp(10));

        TextView logo = tv("\u2726", 21, Color.WHITE);
        logo.setGravity(Gravity.CENTER);
        logo.setBackground(gradient(accent, Color.rgb(33, 211, 238), 15));

        h.addView(logo, new LinearLayout.LayoutParams(dp(42), dp(42)));

        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        titles.setPadding(dp(12), 0, 0, 0);

        TextView a = tv(title, 21, text);
        a.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        titles.addView(a);

        if (sub != null) {
            titles.addView(tv(sub, 12, muted));
        }

        h.addView(titles, new LinearLayout.LayoutParams(0, -2, 1));

        content.addView(h);

        return h;
    }

    // Title on the left, "Add  +" on the top right (used by Projects and Buttons)
    void topBar(String title, String subtitle, final Runnable onAdd) {

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);

        TextView h = tv(title, 25, text);
        h.setTypeface(Typeface.DEFAULT, Typeface.BOLD);

        top.addView(h, new LinearLayout.LayoutParams(0, -2, 1));

        Button add = button("Add  +");
        top.addView(add, new LinearLayout.LayoutParams(dp(96), dp(42)));

        add.setOnClickListener(v -> onAdd.run());

        content.addView(top);

        TextView sub = tv(subtitle, 12, muted);
        sub.setPadding(0, dp(2), 0, dp(10));
        content.addView(sub);
    }

    // ============================================================
    // SECTIONS / CARDS
    // ============================================================

    TextView section(String s) {

        TextView t = tv(s, 17, text);
        t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        t.setPadding(0, dp(14), 0, dp(7));

        content.addView(t);

        return t;
    }

    LinearLayout card() {

        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(15), dp(14), dp(15), dp(14));
        c.setBackground(outline(panel, line, 20));
        c.setElevation(light() ? 0 : dp(2));

        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.setMargins(0, dp(5), 0, dp(8));

        content.addView(c, p);

        return c;
    }

    // ============================================================
    // PAGE SWITCHING
    // ============================================================

    void showPage(int page) {
        if (page == 0) {
            showHome();
        } else if (page == 1) {
            showProjects();
        } else if (page == 2) {
            showButtons();
        } else {
            showManage();
        }
    }

    // ============================================================
    // HOME  (buttons first, then projects)
    // ============================================================

    void showHome() {

        base(0);

        header("Welcome Elvin \uD83D\uDC4B", "Your projects, ready when you are.");

        content.addView(gap(8));

        // ---------- BUTTONS FIRST ----------
        section("Quick Buttons");

        int shown = 0;

        for (int i = 0; i < homeButtons.length() && shown < 2; i++) {
            JSONObject o = homeButtons.optJSONObject(i);
            if (o != null && o.optBoolean("show", true)) {
                buttonCard(i, false);
                shown++;
            }
        }

        if (shown == 0) {
            LinearLayout e = card();
            TextView empty = tv(
                    "No buttons on Home yet\n" +
                            "Add one in the Buttons tab and tick \"Display this button on Home\".",
                    14,
                    muted
            );
            empty.setLineSpacing(2, 1);
            e.addView(empty);
        }

        // ---------- PROJECTS ----------
        section("My Projects");

        if (projects.length() == 0) {

            LinearLayout e = card();

            TextView empty = tv(
                    "No projects yet\n" +
                            "Tap Add in the Projects section to add your first project.",
                    14,
                    muted
            );
            empty.setLineSpacing(2, 1);
            e.addView(empty);

        } else {

            int n = Math.min(3, projects.length());

            for (int i = 0; i < n; i++) {
                JSONObject o = projects.optJSONObject(i);
                if (o != null) {
                    content.addView(projectRow(o));
                }
            }

            if (projects.length() > 3) {
                TextView more = tv("View all projects  \u2192", 13, link);
                more.setGravity(Gravity.RIGHT);
                more.setPadding(0, dp(6), 0, dp(6));
                content.addView(more);
                more.setOnClickListener(v -> showProjects());
            }
        }
    }

    // ============================================================
    // IMAGES (default fallback + rounded corners + border)
    // ============================================================

    void applyImage(ImageView iv, String u) {

        Bitmap bm = null;

        if (u != null && !u.isEmpty()) {
            try {
                Uri uri = Uri.parse(u);

                BitmapFactory.Options op = new BitmapFactory.Options();
                op.inJustDecodeBounds = true;

                InputStream in = getContentResolver().openInputStream(uri);
                BitmapFactory.decodeStream(in, null, op);
                in.close();

                int s = 1;
                while (op.outWidth / s > 800) s *= 2;

                op = new BitmapFactory.Options();
                op.inSampleSize = s;

                in = getContentResolver().openInputStream(uri);
                bm = BitmapFactory.decodeStream(in, null, op);
                in.close();

            } catch (Exception e) {
                bm = null;
            }
        }

        if (bm != null) {
            iv.setPadding(0, 0, 0, 0);
            iv.clearColorFilter();
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            iv.setImageBitmap(bm);
        } else {
            // DEFAULT IMAGE when nothing is selected or image cannot load
            iv.setImageResource(android.R.drawable.ic_menu_gallery);
            iv.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            iv.setPadding(dp(12), dp(12), dp(12), dp(12));
            iv.setColorFilter(muted);
        }
    }

    // Project photo (rounded corners, no border)
    ImageView projectImage(JSONObject o, int size) {
        return plainImage(o);
    }

    // Button photo (rounded corners, no border)
    ImageView buttonImage(JSONObject o, int size) {
        return plainImage(o);
    }

    ImageView plainImage(JSONObject o) {

        ImageView iv = new ImageView(this);

        iv.setBackground(gradient(Color.rgb(29, 34, 76), Color.rgb(15, 22, 50), 14));

        applyImage(iv, o == null ? "" : o.optString("image", ""));

        round(iv, 14);

        return iv;
    }

    // ============================================================
    // PROJECT ROW (used on Home and Projects) - shows short description
    // ============================================================

    LinearLayout projectRow(final JSONObject o) {

        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.HORIZONTAL);
        c.setGravity(Gravity.CENTER_VERTICAL);
        c.setPadding(dp(10), dp(10), dp(8), dp(10));
        c.setBackground(outline(panel, line, 18));

        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, dp(110));
        p.setMargins(0, dp(8), 0, 0);
        c.setLayoutParams(p);

        c.addView(projectImage(o, 68), new LinearLayout.LayoutParams(dp(68), dp(68)));

        LinearLayout mid = new LinearLayout(this);
        mid.setOrientation(LinearLayout.VERTICAL);
        mid.setGravity(Gravity.CENTER_VERTICAL);
        mid.setPadding(dp(12), 0, dp(4), 0);

        TextView n = tv(o.optString("name", "Project"), 16, text);
        n.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        n.setSingleLine(true);
        mid.addView(n);

        String desc = o.optString("description", "").trim();

        TextView de = tv(desc.isEmpty() ? "No description added" : desc, 12, descColor);
        de.setMaxLines(2);
        de.setEllipsize(android.text.TextUtils.TruncateAt.END);
        de.setPadding(0, dp(4), 0, 0);

        if (desc.isEmpty()) de.setAlpha(0.6f);

        mid.addView(de);

        c.addView(mid, new LinearLayout.LayoutParams(0, -1, 1));

        TextView go = tv("\u203A", 28, text);
        go.setGravity(Gravity.CENTER);
        c.addView(go, new LinearLayout.LayoutParams(dp(30), dp(38)));

        c.setOnClickListener(v -> showDetails(o));

        return c;
    }

    // ============================================================
    // PROJECT DETAILS (Edit + light red Delete appear here)
    // Back button returns to the Projects list.
    // ============================================================

    void showDetails(final JSONObject o) {

        base(1);

        // Back from details -> Projects list (does NOT close the app)
        backAction = () -> showProjects();

        String name = o.optString("name", "Project");

        header(name, "Project details");

        LinearLayout c = card();
        c.setPadding(dp(12), dp(12), dp(12), dp(12));

        ImageView pic = projectImage(o, 190);

        LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(-1, dp(190));
        ip.setMargins(0, 0, 0, dp(10));

        c.addView(pic, ip);

        String desc = o.optString("description", "");

        if (!desc.isEmpty()) {
            TextView d = tv(desc, 13, descColor);
            d.setPadding(0, 0, 0, dp(10));
            c.addView(d);
        }

        TextView links = tv("Project Links", 16, text);
        links.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        c.addView(links);

        addDetailLink(c, "GitHub", o.optString("github", o.optString("url", "")));
        addDetailLink(c, "Vercel Hosted", o.optString("vercel", ""));
        addDetailLink(c, "Render Hosted", o.optString("render", ""));

        Button edit = button("Edit Project");

        LinearLayout.LayoutParams ep = new LinearLayout.LayoutParams(-1, dp(46));
        ep.topMargin = dp(10);
        c.addView(edit, ep);

        Button del = deleteButton("Delete Project");
        del.setTextSize(13);

        LinearLayout.LayoutParams dp2 = new LinearLayout.LayoutParams(-1, dp(46));
        dp2.topMargin = dp(8);
        c.addView(del, dp2);

        edit.setOnClickListener(v -> editor(findProjectIndex(o)));

        // Ask for confirmation before deleting
        del.setOnClickListener(
                v -> confirmDelete(
                        "Delete project?",
                        "\"" + o.optString("name", "this project") + "\" will be permanently removed.",
                        () -> {
                            int ix = findProjectIndex(o);
                            if (ix >= 0) {
                                projects.remove(ix);
                                save();
                                showProjects();
                            }
                        }
                )
        );
    }

    int findProjectIndex(JSONObject target) {
        for (int i = 0; i < projects.length(); i++) {
            if (projects.optJSONObject(i) == target) {
                return i;
            }
        }
        return -1;
    }

    void addDetailLink(LinearLayout parent, String label, String value) {
        if (value == null || value.trim().isEmpty()) {
            return;
        }
        parent.addView(linkRow(label, value));
    }

    LinearLayout linkRow(String label, String url) {

        LinearLayout r = new LinearLayout(this);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setPadding(dp(10), dp(7), dp(6), dp(7));
        r.setBackground(outline(panel2, line, 16));

        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(-1, dp(60));
        rp.setMargins(0, dp(7), 0, 0);
        r.setLayoutParams(rp);

        LinearLayout tx = new LinearLayout(this);
        tx.setOrientation(LinearLayout.VERTICAL);

        TextView l = tv(label, 11, link);
        l.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        tx.addView(l);

        TextView u = tv(url, 10, muted);
        u.setSingleLine(true);
        u.setEllipsize(android.text.TextUtils.TruncateAt.MIDDLE);
        tx.addView(u);

        r.addView(tx, new LinearLayout.LayoutParams(0, -2, 1));

        Button copy = smallButton("Copy");
        r.addView(copy, new LinearLayout.LayoutParams(dp(64), dp(38)));
        copy.setOnClickListener(v -> copyUrl(url));

        TextView open = tv("\u2197", 24, Color.WHITE);
        open.setGravity(Gravity.CENTER);
        open.setBackground(gradient(accent, darker(accent), 18));

        LinearLayout.LayoutParams op = new LinearLayout.LayoutParams(dp(44), dp(38));
        op.setMargins(dp(6), 0, 0, 0);

        r.addView(open, op);
        open.setOnClickListener(v -> openUrl(url));

        return r;
    }

    void copyUrl(String url) {

        ((android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE))
                .setPrimaryClip(android.content.ClipData.newPlainText("URL", url));

        Toast.makeText(this, "URL copied", Toast.LENGTH_SHORT).show();
    }

    void openUrl(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception e) {
            Toast.makeText(this, "Invalid URL", Toast.LENGTH_SHORT).show();
        }
    }

    // ============================================================
    // PROJECTS
    // ============================================================

    void showProjects() {

        base(1);

        topBar("My Projects", projects.length() + " saved locally on this phone", () -> editor(-1));

        EditText search = new EditText(this);
        search.setHint("Search projects");
        search.setTextColor(text);
        search.setHintTextColor(Color.rgb(100, 115, 150));
        search.setSingleLine();
        search.setTextSize(14);
        search.setPadding(dp(14), 0, dp(14), 0);
        search.setBackground(outline(panel2, line, 20));

        content.addView(search, new LinearLayout.LayoutParams(-1, dp(48)));

        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);

        content.addView(list);

        // Default message when no projects exist (same style as the Buttons page)
        if (projects.length() == 0) {

            LinearLayout e = card();
            e.setGravity(Gravity.CENTER);

            TextView m = tv(
                    "No projects yet.\nTap Add + to create your first project.",
                    14,
                    muted
            );
            m.setGravity(Gravity.CENTER);

            e.addView(m, new LinearLayout.LayoutParams(-1, dp(90)));
        }

        renderProjectList(list, "");

        search.addTextChangedListener(
                new android.text.TextWatcher() {

                    public void beforeTextChanged(CharSequence s, int st, int c, int a) {
                    }

                    public void onTextChanged(CharSequence s, int st, int b, int c) {
                        list.removeAllViews();
                        renderProjectList(list, s.toString());
                    }

                    public void afterTextChanged(android.text.Editable e) {
                    }
                }
        );
    }

    void renderProjectList(LinearLayout list, String q) {

        q = q.toLowerCase();

        for (int i = 0; i < projects.length(); i++) {

            JSONObject o = projects.optJSONObject(i);

            if (o == null) continue;

            String n = o.optString("name", "").toLowerCase();
            String u = o.optString("github", o.optString("url", "")).toLowerCase();

            if (q.isEmpty() || n.contains(q) || u.contains(q)) {
                list.addView(projectRow(o));
            }
        }
    }

    // ============================================================
    // BUTTON CARD (same design on Home and on the Buttons page)
    // Home         : tap = open the link
    // Buttons page : tap = open the button details page (Edit / Delete are there)
    // ============================================================

    void buttonCard(final int idx, final boolean manage) {
        buttonCard(idx, manage, content);
    }

    // True if this button is one of the (max 2) buttons actually shown on Home.
    boolean onHome(int idx) {
        int count = 0;
        for (int i = 0; i < homeButtons.length(); i++) {
            JSONObject b = homeButtons.optJSONObject(i);
            if (b != null && b.optBoolean("show", true)) {
                if (i == idx) return count < 2;
                count++;
            }
        }
        return false;
    }

    void buttonCard(final int idx, final boolean manage, LinearLayout parent) {

        final JSONObject o = homeButtons.optJSONObject(idx);

        if (o == null) return;

        final String name = o.optString("name", "Button");
        final String url = o.optString("url", "");

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(12), dp(12), dp(12), dp(12));

        // No border on the button card
        card.setBackground(gradient(accent, darker(accent), 24));
        card.setElevation(dp(4));

        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, -2);
        cp.setMargins(0, dp(4), 0, dp(10));
        parent.addView(card, cp);

        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);

        // Button photo on the LEFT.
        ImageView photo = buttonImage(o, 64);
        row.addView(photo, new LinearLayout.LayoutParams(dp(64), dp(64)));

        LinearLayout mid = new LinearLayout(this);
        mid.setOrientation(LinearLayout.VERTICAL);
        mid.setGravity(Gravity.CENTER_VERTICAL);
        mid.setPadding(dp(14), 0, dp(8), 0);

        // Button NAME is the heading
        TextView n = tv(name, 16, BUTTON_LABEL_COLOR);
        n.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        n.setSingleLine(true);
        n.setEllipsize(android.text.TextUtils.TruncateAt.END);
        mid.addView(n);

        // Home status line only on the Buttons page
        if (manage) {
            boolean flagged = o.optBoolean("show", true);
            boolean visible = flagged && onHome(idx);
            String statusText = visible
                    ? "● Visible on Home"
                    : (flagged ? "○ Home is full (max 2)" : "○ Hidden from Home");

            // visible = light green, home full = light amber, hidden = light red
            int statusColor = visible
                    ? Color.rgb(187, 247, 208)
                    : (flagged ? Color.rgb(253, 230, 138) : Color.rgb(254, 202, 202));

            TextView st = tv(statusText, 11, statusColor);
            st.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            st.setPadding(0, dp(3), 0, 0);
            mid.addView(st);
        }

        row.addView(mid, new LinearLayout.LayoutParams(0, -2, 1));

        // Link icon on the RIGHT (Home and Buttons page)
        TextView linkIcon = tv("\uD83D\uDD17", 20, Color.WHITE);
        linkIcon.setGravity(Gravity.CENTER);
        linkIcon.setBackground(bg(Color.argb(70, 255, 255, 255), 22));

        LinearLayout.LayoutParams iconParams =
                new LinearLayout.LayoutParams(dp(46), dp(42));
        iconParams.leftMargin = dp(4);
        row.addView(linkIcon, iconParams);

        card.addView(row);

        if (manage) {
            card.setOnClickListener(v -> showButtonDetails(o));
        } else {
            card.setOnClickListener(v -> launchButton(url));
        }
    }

    int findButtonIndex(JSONObject target) {
        for (int i = 0; i < homeButtons.length(); i++) {
            if (homeButtons.optJSONObject(i) == target) {
                return i;
            }
        }
        return -1;
    }

    // ============================================================
    // BUTTON DETAILS (Edit + light red Delete appear here)
    // Back button returns to the Buttons list.
    // ============================================================

    void showButtonDetails(final JSONObject o) {

        base(2);

        backAction = () -> showButtons();

        final String name = o.optString("name", "Button");

        header(name, "Button details");

        LinearLayout c = card();
        c.setPadding(dp(12), dp(12), dp(12), dp(12));

        ImageView pic = buttonImage(o, 190);

        LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(-1, dp(190));
        ip.setMargins(0, 0, 0, dp(10));
        c.addView(pic, ip);

        TextView links = tv("Button Link", 16, text);
        links.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        c.addView(links);

        addDetailLink(c, "Link", o.optString("url", ""));

        Button edit = button("Edit Button");

        LinearLayout.LayoutParams ep = new LinearLayout.LayoutParams(-1, dp(46));
        ep.topMargin = dp(10);
        c.addView(edit, ep);

        Button del = deleteButton("Delete Button");
        del.setTextSize(13);

        LinearLayout.LayoutParams delParams = new LinearLayout.LayoutParams(-1, dp(46));
        delParams.topMargin = dp(8);
        c.addView(del, delParams);

        edit.setOnClickListener(v -> editHomeButton(findButtonIndex(o)));

        // Ask for confirmation before deleting
        del.setOnClickListener(
                v -> confirmDelete(
                        "Delete button?",
                        "\"" + name + "\" will be permanently removed.",
                        () -> {
                            int ix = findButtonIndex(o);
                            if (ix >= 0) {
                                homeButtons.remove(ix);
                                saveHomeButtons();
                                showButtons();
                            }
                        }
                )
        );
    }

    // Redirects to the button's link; shows an error screen if it fails
    void launchButton(String url) {

        if (url == null || url.trim().isEmpty()) {
            showError("This button has no link. Edit the button and add a URL.");
            return;
        }

        url = url.trim();

        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://" + url;
        }

        try {
            Uri uri = Uri.parse(url);

            if (uri.getHost() == null || uri.getHost().isEmpty()) {
                showError("\"" + url + "\" is not a valid link.");
                return;
            }

            startActivity(new Intent(Intent.ACTION_VIEW, uri));

        } catch (ActivityNotFoundException e) {
            showError("No app was found to open:\n" + url);
        } catch (Exception e) {
            showError("Could not open the link:\n" + url);
        }
    }

    void showError(String message) {

        base(0);

        // Back from the error screen -> Home
        backAction = () -> showHome();

        header("Couldn't open link", "Something went wrong.");

        LinearLayout c = card();
        c.setGravity(Gravity.CENTER);

        TextView icon = tv("!", 68, red);
        icon.setGravity(Gravity.CENTER);
        c.addView(icon, new LinearLayout.LayoutParams(-1, dp(115)));

        TextView title = tv("Error", 18, text);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        c.addView(title);

        TextView m = tv(message, 13, muted);
        m.setGravity(Gravity.CENTER);
        m.setPadding(0, dp(6), 0, dp(14));
        c.addView(m);

        Button done = button("Back to Home");
        c.addView(done, new LinearLayout.LayoutParams(-1, dp(48)));

        done.setOnClickListener(v -> showHome());
    }

    // ============================================================
    // DELETE CONFIRMATION DIALOG
    // ============================================================

    void confirmDelete(String title, String message, final Runnable onConfirm) {

        final Dialog d = dialog(title, "This action cannot be undone.");

        LinearLayout box = (LinearLayout) d.findViewById(9001);

        TextView m = tv(message, 14, text);
        m.setPadding(0, dp(6), 0, dp(6));
        box.addView(m);

        LinearLayout footer = new LinearLayout(this);
        footer.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        footer.setPadding(0, dp(10), 0, 0);

        Button cancel = flatButton("Cancel", muted);

        Button confirm = button("Delete");
        confirm.setTextColor(darkRedText);
        confirm.setBackground(bg(lightRed, 22));

        footer.addView(cancel, new LinearLayout.LayoutParams(dp(86), dp(44)));
        footer.addView(confirm, new LinearLayout.LayoutParams(dp(94), dp(44)));

        ((LinearLayout) d.findViewById(9002)).addView(footer);

        cancel.setOnClickListener(v -> d.dismiss());

        confirm.setOnClickListener(
                v -> {
                    d.dismiss();
                    onConfirm.run();
                }
        );

        d.show();
    }

    // ============================================================
    // BUTTONS PAGE (same header style as Projects)
    // ============================================================

    void showButtons() {

        base(2);

        topBar("Buttons", homeButtons.length() + " shortcuts saved", () -> editHomeButton(-1));

        EditText search = new EditText(this);
        search.setHint("Search buttons");
        search.setTextColor(text);
        search.setHintTextColor(Color.rgb(100, 115, 150));
        search.setSingleLine();
        search.setTextSize(14);
        search.setPadding(dp(14), 0, dp(14), 0);
        search.setBackground(outline(panel2, line, 20));
        content.addView(search, new LinearLayout.LayoutParams(-1, dp(48)));

        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        content.addView(list);

        if (homeButtons.length() == 0) {

            LinearLayout e = card();
            e.setGravity(Gravity.CENTER);

            TextView m = tv(
                    "No custom buttons yet.\nTap Add + to create your first button.",
                    14,
                    muted
            );
            m.setGravity(Gravity.CENTER);

            e.addView(m, new LinearLayout.LayoutParams(-1, dp(90)));

        }

        renderButtonList(list, "");

        search.addTextChangedListener(new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s, int st, int c, int a) {}

            public void onTextChanged(CharSequence s, int st, int before, int count) {
                list.removeAllViews();
                renderButtonList(list, s.toString());
            }

            public void afterTextChanged(android.text.Editable e) {}
        });
    }

    void renderButtonList(LinearLayout list, String q) {
        q = q == null ? "" : q.trim().toLowerCase();

        for (int i = 0; i < homeButtons.length(); i++) {
            JSONObject o = homeButtons.optJSONObject(i);
            if (o == null) continue;

            String name = o.optString("name", "").toLowerCase();
            String url = o.optString("url", "").toLowerCase();

            if (q.isEmpty() || name.contains(q) || url.contains(q)) {
                buttonCard(i, true, list);
            }
        }
    }

    // ============================================================
    // ADD / EDIT BUTTON
    // ============================================================

    void editHomeButton(final int index) {

        JSONObject old = index >= 0 ? homeButtons.optJSONObject(index) : null;

        pendingButtonImage = old == null ? "" : old.optString("image", "");
        buttonEditorPreview = null;

        final Dialog d = dialog(
                index < 0 ? "Add Button" : "Edit Button",
                "Create a shortcut for any server, wake-up URL or project."
        );

        LinearLayout box = (LinearLayout) d.findViewById(9001);

        // ---------- BUTTON PHOTO ----------
        LinearLayout imageRow = new LinearLayout(this);
        imageRow.setGravity(Gravity.CENTER_VERTICAL);
        imageRow.setPadding(dp(10), dp(8), dp(10), dp(8));
        imageRow.setBackground(outline(panel2, line, 18));

        JSONObject imageObject = old == null ? null : old;
        buttonEditorPreview = buttonImage(imageObject, 64);
        imageRow.addView(buttonEditorPreview, new LinearLayout.LayoutParams(dp(64), dp(64)));

        TextView buttonImageLabel = tv(
                pendingButtonImage.isEmpty()
                        ? "  +  Select button photo"
                        : "  ✓  Button photo selected",
                14,
                pendingButtonImage.isEmpty() ? link : green
        );
        buttonImageLabel.setGravity(Gravity.CENTER_VERTICAL);
        imageRow.addView(buttonImageLabel, new LinearLayout.LayoutParams(0, dp(64), 1));

        box.addView(imageRow);

        imageRow.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            i.setType("image/*");
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION |
                            Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
            );
            startActivityForResult(i, PICK_BUTTON_IMAGE);
        });

        // ---------- TEXT / URL ----------
        EditText name = field("Button name", old == null ? "" : old.optString("name", ""));
        EditText url = field("Button URL", old == null ? "" : old.optString("url", ""));

        box.addView(name);
        box.addView(url);

        CheckBox show = new CheckBox(this);
        show.setText("Display this button on Home (Home shows max 2)");
        show.setTextColor(text);
        show.setTextSize(13);
        show.setChecked(old == null || old.optBoolean("show", true));
        box.addView(show);

        addDialogButtons(
                d,
                () -> d.dismiss(),
                () -> {

                    String n = name.getText().toString().trim();
                    String u = url.getText().toString().trim();

                    if (n.isEmpty() || u.isEmpty()) {
                        Toast.makeText(this, "Button name and URL are required", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    try {
                        JSONObject o = new JSONObject();

                        o.put("name", n);
                        o.put("url", u);
                        o.put("image", pendingButtonImage);
                        o.put("show", show.isChecked());

                        if (index < 0) {
                            homeButtons.put(o);
                        } else {
                            homeButtons.put(index, o);
                        }

                        saveHomeButtons();

                        d.dismiss();
                        showButtons();

                    } catch (Exception e) {
                        Toast.makeText(this, "Could not save button", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        d.show();
    }

    // ============================================================
    // SETTINGS (Theme first)
    // ============================================================

    void showManage() {

        base(3);

        header("Settings", "Everything stays local on your phone.");

        // ---------- THEME (first) ----------
        section("Theme");

        LinearLayout tc = card();

        TextView ts = tv("Choose light (glass) or dark mode.", 11, muted);
        ts.setPadding(0, 0, 0, dp(10));
        tc.addView(ts);

        LinearLayout chips = new LinearLayout(this);

        boolean light = light();

        LinearLayout.LayoutParams c1 = new LinearLayout.LayoutParams(0, dp(48), 1);
        c1.rightMargin = dp(6);
        LinearLayout.LayoutParams c2 = new LinearLayout.LayoutParams(0, dp(48), 1);
        c2.leftMargin = dp(6);

        chips.addView(themeChip("Light  \u2600", light, true), c1);
        chips.addView(themeChip("Dark  \uD83C\uDF19", !light, false), c2);

        tc.addView(chips);

        // ---------- ACCENT ----------
        section("Appearance");

        LinearLayout ap = card();
        ap.setPadding(dp(15), dp(16), dp(15), dp(18));

        TextView at = tv("Accent color", 15, text);
        at.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        ap.addView(at);

        TextView as = tv("Choose the glow used by buttons and the launcher mark.", 11, muted);
        as.setPadding(0, dp(3), 0, dp(4));
        ap.addView(as);

        LinearLayout colors = new LinearLayout(this);
        colors.setGravity(Gravity.CENTER_VERTICAL);

        int[] cols = {
                Color.rgb(139, 92, 246),
                Color.rgb(34, 211, 238),
                Color.rgb(52, 211, 153),
                Color.rgb(251, 146, 60),
                Color.rgb(236, 72, 153)
        };

        for (int col : cols) {

            TextView sw = tv("", 1, Color.WHITE);

            GradientDrawable g = bg(col, 50);
            if (col == accent) g.setStroke(dp(3), text);
            sw.setBackground(g);

            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(dp(48), dp(48));
            p.setMargins(0, dp(10), dp(12), 0);

            colors.addView(sw, p);

            sw.setOnClickListener(
                    v -> {
                        accent = col;
                        prefs.edit().putInt("accent", accent).apply();
                        showManage();
                    }
            );
        }

        ap.addView(colors, new LinearLayout.LayoutParams(-1, dp(78)));

        // ---------- WATERMARK (pinned to the bottom of Settings) ----------
        View pushDown = new View(this);
        content.addView(pushDown, new LinearLayout.LayoutParams(-1, 0, 1));

        TextView watermark = tv("This app is built by Elvin Joy", 12, muted);
        watermark.setGravity(Gravity.CENTER);
        watermark.setAlpha(0.7f);
        watermark.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        watermark.setPadding(0, dp(28), 0, dp(8));
        content.addView(watermark, new LinearLayout.LayoutParams(-1, -2));
    }

    TextView themeChip(String label, boolean selected, final boolean makeLight) {

        TextView chip = tv(label, 14, selected ? Color.WHITE : text);
        chip.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        chip.setGravity(Gravity.CENTER);
        chip.setBackground(
                selected
                        ? gradient(accent, darker(accent), 24)
                        : outline(panel2, line, 24)
        );

        chip.setOnClickListener(
                v -> {
                    prefs.edit().putBoolean("light_mode", makeLight).apply();
                    applyPalette();
                    applySystemBars();
                    showManage();
                }
        );

        return chip;
    }

    // ============================================================
    // PROJECT EDITOR
    // ============================================================

    void editor(final int index) {

        JSONObject old = index >= 0 ? projects.optJSONObject(index) : null;

        pendingImage = old == null ? "" : old.optString("image", "");

        final Dialog d = dialog(
                index < 0 ? "Add New Project" : "Edit Project",
                "Keep your project details clean and demo-ready."
        );

        LinearLayout box = (LinearLayout) d.findViewById(9001);

        LinearLayout imageRow = new LinearLayout(this);
        imageRow.setGravity(Gravity.CENTER_VERTICAL);
        imageRow.setPadding(dp(10), dp(8), dp(10), dp(8));
        imageRow.setBackground(outline(panel2, line, 18));

        editorPreview = projectImage(old, 64);

        imageRow.addView(editorPreview, new LinearLayout.LayoutParams(dp(64), dp(64)));

        editorPickLabel = tv(
                pendingImage.isEmpty()
                        ? "  +  Select project image"
                        : "  \u2713  Project image selected",
                14,
                pendingImage.isEmpty() ? link : green
        );

        editorPickLabel.setGravity(Gravity.CENTER_VERTICAL);

        imageRow.addView(editorPickLabel, new LinearLayout.LayoutParams(0, dp(64), 1));

        box.addView(imageRow);

        imageRow.setOnClickListener(
                v -> {
                    Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                    i.setType("image/*");
                    i.addCategory(Intent.CATEGORY_OPENABLE);
                    i.addFlags(
                            Intent.FLAG_GRANT_READ_URI_PERMISSION |
                                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                    );
                    startActivityForResult(i, PICK_IMAGE);
                }
        );

        EditText name = field("Project name", old == null ? "" : old.optString("name", ""));

        EditText github = field(
                "GitHub URL (optional)",
                old == null ? "" : old.optString("github", old.optString("url", ""))
        );

        EditText vercel = field("Vercel Hosted URL (optional)", old == null ? "" : old.optString("vercel", ""));
        EditText render = field("Render Hosted URL (optional)", old == null ? "" : old.optString("render", ""));
        EditText desc = field("Short description (optional)", old == null ? "" : old.optString("description", ""));

        box.addView(name);
        box.addView(github);
        box.addView(vercel);
        box.addView(render);
        box.addView(desc);

        addDialogButtons(
                d,
                () -> d.dismiss(),
                () -> {

                    String n = name.getText().toString().trim();
                    String g = github.getText().toString().trim();

                    if (n.isEmpty()) {
                        Toast.makeText(this, "Project name is required", Toast.LENGTH_SHORT).show();
                        name.requestFocus();
                        return;
                    }

                    try {

                        JSONObject o = new JSONObject();

                        o.put("name", n);
                        o.put("github", g);
                        o.put("url", g);
                        o.put("vercel", vercel.getText().toString().trim());
                        o.put("render", render.getText().toString().trim());
                        o.put("description", desc.getText().toString().trim());
                        o.put("image", pendingImage);

                        if (index < 0) {
                            projects.put(o);
                        } else {
                            projects.put(index, o);
                        }

                        save();

                        d.dismiss();

                        showProjects();

                    } catch (Exception e) {
                        Toast.makeText(this, "Could not save project", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        d.show();
    }

    // ============================================================
    // DIALOG
    // ============================================================

    Dialog dialog(String title, String subtitle) {

        final Dialog d = new Dialog(this);

        d.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout outer = new LinearLayout(this);
        outer.setId(9002);
        outer.setOrientation(LinearLayout.VERTICAL);
        outer.setPadding(dp(18), dp(16), dp(18), dp(10));
        outer.setBackground(outline(dialogFill, dialogLine, 24));

        TextView t = tv(title, 21, text);
        t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        outer.addView(t);

        TextView s = tv(subtitle, 11, muted);
        s.setPadding(0, dp(3), 0, dp(10));
        outer.addView(s);

        MaxHeightScrollView sv = new MaxHeightScrollView(this, dp(500));

        LinearLayout box = new LinearLayout(this);
        box.setId(9001);
        box.setOrientation(LinearLayout.VERTICAL);

        sv.addView(box);

        outer.addView(sv, new LinearLayout.LayoutParams(-1, -2));

        d.setContentView(outer);

        Window w = d.getWindow();

        if (w != null) {

            w.setBackgroundDrawableResource(android.R.color.transparent);
            w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);

            WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
            lp.copyFrom(w.getAttributes());

            lp.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.92f);
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
            lp.gravity = Gravity.CENTER;
            lp.dimAmount = 0.72f;

            w.setAttributes(lp);

            w.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }

        return d;
    }

    void addDialogButtons(Dialog d, final Runnable cancel, final Runnable saveAction) {

        LinearLayout footer = new LinearLayout(this);
        footer.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        footer.setPadding(0, dp(10), 0, 0);

        Button c = flatButton("Cancel", muted);
        Button s = flatButton("Save", Color.WHITE);

        s.setBackground(gradient(accent, darker(accent), 22));

        footer.addView(c, new LinearLayout.LayoutParams(dp(86), dp(44)));
        footer.addView(s, new LinearLayout.LayoutParams(dp(94), dp(44)));

        ((LinearLayout) d.findViewById(9002)).addView(footer);

        c.setOnClickListener(v -> cancel.run());
        s.setOnClickListener(v -> saveAction.run());
    }

    EditText field(String hint, String value) {

        EditText e = new EditText(this);

        e.setHint(hint);
        e.setText(value);
        e.setTextColor(text);
        e.setHintTextColor(Color.rgb(105, 121, 157));
        e.setTextSize(14);
        e.setSingleLine(true);
        e.setBackground(outline(panel2, line, 16));
        e.setPadding(dp(13), 0, dp(13), 0);

        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, dp(48));
        p.setMargins(0, dp(6), 0, 0);

        e.setLayoutParams(p);

        return e;
    }

    // ============================================================
    // IMAGE PICKER RESULT
    // ============================================================

    @Override
    protected void onActivityResult(int req, int result, Intent data) {

        super.onActivityResult(req, result, data);

        if (req == PICK_IMAGE
                && result == RESULT_OK
                && data != null
                && data.getData() != null) {

            Uri uri = data.getData();

            try {
                getContentResolver().takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                );
            } catch (Exception ignored) {
            }

            pendingImage = uri.toString();

            try {
                if (editorPreview != null) {
                    applyImage(editorPreview, pendingImage);
                }

                if (editorPickLabel != null) {
                    editorPickLabel.setText("  \u2713  Project image selected");
                    editorPickLabel.setTextColor(green);
                }
            } catch (Exception ignored) {
            }
        }

        if (req == PICK_BUTTON_IMAGE
                && result == RESULT_OK
                && data != null
                && data.getData() != null) {

            Uri uri = data.getData();

            try {
                getContentResolver().takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                );
            } catch (Exception ignored) {
            }

            pendingButtonImage = uri.toString();

            try {
                if (buttonEditorPreview != null) {
                    applyImage(buttonEditorPreview, pendingButtonImage);
                }

                // Find the currently visible picker label in its parent and update it.
                if (buttonEditorPreview != null && buttonEditorPreview.getParent() instanceof ViewGroup) {
                    ViewGroup row = (ViewGroup) buttonEditorPreview.getParent();
                    for (int i = 0; i < row.getChildCount(); i++) {
                        View child = row.getChildAt(i);
                        if (child instanceof TextView) {
                            TextView label = (TextView) child;
                            label.setText("  ✓  Button photo selected");
                            label.setTextColor(green);
                            break;
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }
    }

    // ============================================================
    // GLASS BACKGROUND (light mode): pastel gradient + soft colour blobs
    // ============================================================

    static class GlassBgDrawable extends Drawable {

        final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        final int accent;

        GlassBgDrawable(int accent) {
            this.accent = accent;
        }

        void blob(Canvas c, float x, float y, float r, int color) {
            int clear = color & 0x00FFFFFF;
            p.setShader(new RadialGradient(x, y, r, color, clear, Shader.TileMode.CLAMP));
            c.drawCircle(x, y, r, p);
        }

        @Override
        public void draw(Canvas c) {

            Rect b = getBounds();
            float w = b.width();
            float h = b.height();

            p.setShader(new LinearGradient(
                    0, 0, w, h,
                    new int[]{
                            Color.rgb(224, 231, 255),
                            Color.rgb(243, 232, 255),
                            Color.rgb(214, 246, 252)
                    },
                    null,
                    Shader.TileMode.CLAMP
            ));
            c.drawRect(b, p);

            blob(c, w * 0.88f, h * 0.10f, w * 0.65f, Color.argb(120, Color.red(accent), Color.green(accent), Color.blue(accent)));
            blob(c, w * 0.05f, h * 0.55f, w * 0.60f, Color.argb(95, 34, 211, 238));
            blob(c, w * 0.85f, h * 0.92f, w * 0.65f, Color.argb(85, 236, 72, 153));

            p.setShader(null);
        }

        @Override
        public void setAlpha(int alpha) {
        }

        @Override
        public void setColorFilter(ColorFilter cf) {
        }

        @Override
        public int getOpacity() {
            return PixelFormat.OPAQUE;
        }
    }

    // ============================================================
    // BOTTOM NAVIGATION WITH SOFT WAVE (theme aware)
    // ============================================================

    static class WaveNavLayout extends LinearLayout {

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        Paint paint2 = new Paint(Paint.ANTI_ALIAS_FLAG);

        Path path = new Path();

        float phase = 0;

        Runnable animator = new Runnable() {
            @Override
            public void run() {
                phase += 0.06f;
                invalidate();
                postDelayed(this, 33);
            }
        };

        WaveNavLayout(Context context, boolean light, int accent) {

            super(context);

            setWillNotDraw(false);

            int r = Color.red(accent);
            int g = Color.green(accent);
            int b = Color.blue(accent);

            paint.setStyle(Paint.Style.FILL);
            paint2.setStyle(Paint.Style.FILL);

            if (light) {
                paint.setColor(Color.argb(55, r, g, b));
                paint2.setColor(Color.argb(45, 34, 211, 238));
            } else {
                paint.setColor(Color.argb(85, r, g, b));
                paint2.setColor(Color.argb(55, 34, 211, 238));
            }
        }

        @Override
        protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            removeCallbacks(animator);
            post(animator);
        }

        @Override
        protected void onDetachedFromWindow() {
            removeCallbacks(animator);
            super.onDetachedFromWindow();
        }

        void drawWave(Canvas canvas, Paint p, float base, float amp, float freq, float ph) {

            float width = getWidth();
            float height = getHeight();

            path.reset();
            path.moveTo(0, height);

            for (float x = 0; x <= width; x += 8) {
                float y = height - base + (float) Math.sin((x * freq) + ph) * amp;
                path.lineTo(x, y);
            }

            path.lineTo(width, height);
            path.close();

            canvas.drawPath(path, p);
        }

        @Override
        protected void onDraw(Canvas canvas) {

            super.onDraw(canvas);

            drawWave(canvas, paint, dp(24), dp(7), 0.03f, phase);
            drawWave(canvas, paint2, dp(18), dp(6), 0.045f, -phase * 1.3f);
        }

        float dp(float v) {
            return v * getResources().getDisplayMetrics().density;
        }
    }

    // ============================================================
    // MAX HEIGHT DIALOG SCROLL
    // ============================================================

    static class MaxHeightScrollView extends ScrollView {

        int max;

        MaxHeightScrollView(Context c, int m) {
            super(c);
            max = m;
        }

        @Override
        protected void onMeasure(int w, int h) {
            int hs = MeasureSpec.makeMeasureSpec(max, MeasureSpec.AT_MOST);
            super.onMeasure(w, hs);
        }
    }

    // ============================================================
    // TRANSPARENT DRAWABLE
    // ============================================================

    static class ColorDrawableCompat {

        static android.graphics.drawable.Drawable transparent() {
            return new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT);
        }
    }
}