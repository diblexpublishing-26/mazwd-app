```java
package com.mazwd.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.UriPermission;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.DocumentsContract;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class MainActivity extends Activity {

    static final int PICK_TREE = 40;

    final int COLOR_PRIMARY       = Color.rgb(21, 76, 121);
    final int COLOR_PRIMARY_DARK  = Color.rgb(14, 54, 88);
    final int COLOR_PRIMARY_LIGHT = Color.rgb(232, 241, 248);
    final int COLOR_BG            = Color.rgb(244, 246, 249);
    final int COLOR_SURFACE       = Color.WHITE;
    final int COLOR_PAPER         = Color.rgb(252, 251, 248);
    final int COLOR_TEXT_MAIN     = Color.rgb(26, 33, 41);
    final int COLOR_TEXT_MUTED    = Color.rgb(100, 116, 139);
    final int COLOR_BORDER        = Color.rgb(226, 232, 240);

    LinearLayout root, content;
    Uri tree;
    FileItem activeFile;
    String currentScreen = "LIST";
    TextView status, zoomBadgeView, readerTextView;
    EditText editor, searchInput;
    SharedPreferences prefs;
    Typeface arabic, arabicBold;

    ArrayList<FileItem> files = new ArrayList<>();
    ArrayDeque<String> undo = new ArrayDeque<>(), redo = new ArrayDeque<>();
    boolean applyingEdit = false;
    boolean pendingNewFileAfterFolderPick = false;
    String beforeEdit = "";
    int pad;

    final Handler saveHandler = new Handler(Looper.getMainLooper());
    Runnable pendingSaveRunnable;

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        pad = dp(16);
        try {
            arabic = getResources().getFont(R.font.mazwd_arabic);
            arabicBold = getResources().getFont(R.font.mazwd_arabic_bold);
        } catch (Exception ignored) {
            arabic = Typeface.SERIF;
            arabicBold = Typeface.create(Typeface.SERIF, Typeface.BOLD);
        }

        prefs = getSharedPreferences("mazwd", 0);
        String saved = prefs.getString("tree", null);
        if (saved != null) {
            tree = Uri.parse(saved);
            if (!hasValidTreePermission()) {
                tree = null;
            }
        }
        showList();
    }

    int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    Typeface chosenFont(boolean bold) {
        int f = prefs == null ? 0 : prefs.getInt("fontFace", 0);
        int style = bold ? Typeface.BOLD : Typeface.NORMAL;
        switch (f) {
            case 0:
                return Typeface.create("sans-serif", style);
            case 1:
                return Typeface.create("serif", style);
            case 2:
                if (bold && arabicBold != null) return arabicBold;
                return arabic != null ? arabic : Typeface.create("serif", style);
            case 3:
                return Typeface.create("monospace", style);
            case 4:
                return Typeface.create("cursive", style);
            default:
                return Typeface.create("sans-serif", style);
        }
    }

    float getScaledFontSize() {
        int baseSize = prefs.getInt("font", 20);
        int scale = prefs.getInt("fontScale", 100);
        return baseSize * (scale / 100f);
    }

    void adjustZoom(int deltaPercent) {
        int current = prefs.getInt("fontScale", 100);
        int next = Math.max(60, Math.min(220, current + deltaPercent));
        prefs.edit().putInt("fontScale", next).apply();

        float newSize = getScaledFontSize();
        if (readerTextView != null) {
            readerTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, newSize);
        }
        if (editor != null) {
            editor.setTextSize(TypedValue.COMPLEX_UNIT_SP, newSize);
        }
        if (zoomBadgeView != null) {
            zoomBadgeView.setText(next + "%");
        }
    }

    GradientDrawable shape(int color, float radiusPx) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(radiusPx);
        return g;
    }

    GradientDrawable borderedShape(int bgColor, int borderColor, int borderWidthPx, float radiusPx) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(bgColor);
        g.setCornerRadius(radiusPx);
        if (borderWidthPx > 0) {
            g.setStroke(borderWidthPx, borderColor);
        }
        return g;
    }

    Drawable interactiveShape(int bgColor, int borderColor, int borderWidthPx, float radiusPx) {
        GradientDrawable contentDrawable = borderedShape(bgColor, borderColor, borderWidthPx, radiusPx);
        GradientDrawable maskDrawable = shape(Color.WHITE, radiusPx);
        int rippleColor = Color.argb(32, 21, 76, 121);
        return new RippleDrawable(android.content.res.ColorStateList.valueOf(rippleColor), contentDrawable, maskDrawable);
    }

    TextView text(String s, float sizeSp) {
        TextView v = new TextView(this);
        v.setText(s);
        v.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp);
        v.setTextColor(COLOR_TEXT_MAIN);
        v.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        v.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
        v.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        v.setTextDirection(View.TEXT_DIRECTION_RTL);
        v.setIncludeFontPadding(true);
        v.setTypeface(chosenFont(false));
        return v;
    }

    TextView label(String s, float sizeSp, int color, boolean bold) {
        TextView v = text(s, sizeSp);
        v.setTextColor(color);
        v.setTypeface(chosenFont(bold));
        return v;
    }

    Button button(String s, View.OnClickListener l) {
        Button b = new Button(this);
        b.setText(s);
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        b.setTextColor(COLOR_PRIMARY);
        b.setTypeface(chosenFont(true));
        b.setAllCaps(false);
        b.setMinHeight(0);
        b.setMinWidth(0);
        b.setMinimumHeight(0);
        b.setMinimumWidth(0);
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(14), dp(8), dp(14), dp(8));
        b.setBackground(interactiveShape(COLOR_SURFACE, COLOR_BORDER, dp(1), dp(14)));
        b.setStateListAnimator(null);
        b.setOnClickListener(l);
        return b;
    }

    Button tonalButton(String s, View.OnClickListener l) {
        Button b = button(s, l);
        b.setTextColor(COLOR_PRIMARY);
        b.setBackground(interactiveShape(COLOR_PRIMARY_LIGHT, 0, 0, dp(14)));
        return b;
    }

    Button primaryButton(String s, View.OnClickListener l) {
        Button b = button(s, l);
        b.setTextColor(Color.WHITE);
        GradientDrawable contentDrawable = shape(COLOR_PRIMARY, dp(14));
        GradientDrawable maskDrawable = shape(Color.WHITE, dp(14));
        int rippleColor = Color.argb(60, 255, 255, 255);
        b.setBackground(new RippleDrawable(android.content.res.ColorStateList.valueOf(rippleColor), contentDrawable, maskDrawable));
        return b;
    }

    void base() {
        cancelPendingSave();
        editor = null;
        readerTextView = null;
        zoomBadgeView = null;

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        root.setBackgroundColor(COLOR_BG);
        root.setPadding(pad, dp(10), pad, dp(12));
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            v.setPadding(
                    pad,
                    dp(10) + insets.getSystemWindowInsetTop(),
                    pad,
                    dp(12) + insets.getSystemWindowInsetBottom()
            );
            return insets;
        });
        setContentView(root);
    }

    void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    void hideKeyboard() {
        try {
            View v = getCurrentFocus();
            if (v != null) {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
            }
        } catch (Exception ignored) {}
    }

    void showList() {
        hideKeyboard();
        currentScreen = "LIST";
        activeFile = null;
        base();

        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.VERTICAL);
        head.setPadding(dp(20), dp(18), dp(20), dp(18));
        GradientDrawable headBg = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{COLOR_PRIMARY, COLOR_PRIMARY_DARK}
        );
        headBg.setCornerRadius(dp(24));
        head.setBackground(headBg);
        head.setElevation(dp(4));

        TextView title = label("مفكرة مزود", 26, Color.WHITE, true);
        head.addView(title, new LinearLayout.LayoutParams(-1, -2));

        TextView subtitle = label("مساحتك الهادئة لحفظ الأفكار والملفات النصية", 14, Color.rgb(210, 228, 245), false);
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(-1, -2);
        subLp.setMargins(0, dp(4), 0, dp(14));
        head.addView(subtitle, subLp);

        LinearLayout summary = new LinearLayout(this);
        summary.setOrientation(LinearLayout.HORIZONTAL);
        summary.setGravity(Gravity.CENTER_VERTICAL);

        TextView hint = label(tree != null ? "✓ الحفظ التلقائي مفعّل" : "⚠ اختر مجلداً للبدء", 13, Color.rgb(230, 240, 250), false);
        summary.addView(hint, new LinearLayout.LayoutParams(0, -2, 1f));

        Button newBtn = button("＋  ملف جديد", v -> newFile());
        newBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        newBtn.setTextColor(COLOR_PRIMARY);
        newBtn.setPadding(dp(18), dp(10), dp(18), dp(10));
        newBtn.setBackground(interactiveShape(Color.WHITE, 0, 0, dp(14)));
        summary.addView(newBtn, new LinearLayout.LayoutParams(-2, -2));

        head.addView(summary, new LinearLayout.LayoutParams(-1, -2));
        root.addView(head, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout section = new LinearLayout(this);
        section.setOrientation(LinearLayout.HORIZONTAL);
        section.setGravity(Gravity.CENTER_VERTICAL);
        section.setPadding(dp(4), dp(14), dp(4), dp(8));

        TextView sectionTitle = label("ملفاتي النصية", 19, COLOR_TEXT_MAIN, true);
        section.addView(sectionTitle, new LinearLayout.LayoutParams(0, -2, 1f));

        Button folderBtn = tonalButton("📁 المجلد", v -> pickFolder());
        folderBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        folderBtn.setPadding(dp(12), dp(6), dp(12), dp(6));
        section.addView(folderBtn, new LinearLayout.LayoutParams(-2, -2));

        root.addView(section, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout filters = new LinearLayout(this);
        filters.setOrientation(LinearLayout.HORIZONTAL);
        filters.setGravity(Gravity.CENTER_VERTICAL);
        filters.setPadding(0, 0, 0, dp(10));

        searchInput = new EditText(this);
        searchInput.setHint("🔍 ابحث في أسماء ملفاتك...");
        searchInput.setHintTextColor(COLOR_TEXT_MUTED);
        searchInput.setTextColor(COLOR_TEXT_MAIN);
        searchInput.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        searchInput.setSingleLine(true);
        searchInput.setTypeface(chosenFont(false));
        searchInput.setTextDirection(View.TEXT_DIRECTION_RTL);
        searchInput.setPadding(dp(16), dp(12), dp(16), dp(12));
        searchInput.setBackground(borderedShape(COLOR_SURFACE, COLOR_BORDER, dp(1), dp(16)));
        filters.addView(searchInput, new LinearLayout.LayoutParams(0, -2, 1f));

        Button sort = button("⇅ " + sortLabel(), v -> {
            int next = (prefs.getInt("sort", 0) + 1) % 3;
            prefs.edit().putInt("sort", next).apply();
            showList();
        });
        sort.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        sort.setPadding(dp(14), dp(12), dp(14), dp(12));
        LinearLayout.LayoutParams sortLp = new LinearLayout.LayoutParams(-2, -1);
        sortLp.setMarginStart(dp(8));
        filters.addView(sort, sortLp);

        root.addView(filters, new LinearLayout.LayoutParams(-1, -2));

        searchInput.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            public void onTextChanged(CharSequence s, int a, int b, int c) {
                filterFiles(s.toString());
            }
            public void afterTextChanged(Editable e) {}
        });

        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(2), dp(4), dp(2), dp(16));

        ScrollView sc = new ScrollView(this);
        sc.setClipToPadding(false);
        sc.setFillViewport(true);
        sc.addView(content, new LinearLayout.LayoutParams(-1, -2));
        root.addView(sc, new LinearLayout.LayoutParams(-1, 0, 1f));

        LinearLayout bottom = new LinearLayout(this);
        bottom.setOrientation(LinearLayout.HORIZONTAL);
        bottom.setGravity(Gravity.CENTER_VERTICAL);
        bottom.setPadding(0, dp(8), 0, dp(4));

        Button backup = primaryButton("📦 نسخ احتياطي (ZIP)", v -> backup());
        backup.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        backup.setPadding(dp(12), dp(12), dp(12), dp(12));
        bottom.addView(backup, new LinearLayout.LayoutParams(0, -2, 1f));

        Button settings = button("⚙ الإعدادات والخط", v -> settings());
        settings.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        settings.setPadding(dp(12), dp(12), dp(12), dp(12));
        LinearLayout.LayoutParams sl = new LinearLayout.LayoutParams(0, -2, 1f);
        sl.setMarginStart(dp(10));
        bottom.addView(settings, sl);

        root.addView(bottom, new LinearLayout.LayoutParams(-1, -2));

        status = label("", 12, COLOR_TEXT_MUTED, false);
        status.setPadding(dp(4), dp(4), dp(4), 0);
        root.addView(status, new LinearLayout.LayoutParams(-1, -2));

        if (tree == null) {
            showEmptyFolderState();
        } else {
            loadFiles();
        }
    }

    void showEmptyFolderState() {
        content.removeAllViews();
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setPadding(dp(24), dp(32), dp(24), dp(32));
        card.setBackground(borderedShape(COLOR_SURFACE, COLOR_BORDER, dp(1), dp(22)));

        TextView icon = label("📂", 40, COLOR_PRIMARY, false);
        icon.setGravity(Gravity.CENTER);
        card.addView(icon, new LinearLayout.LayoutParams(-1, -2));

        TextView title = label("اختر مجلد حفظ الملاحظات", 19, COLOR_TEXT_MAIN, true);
        title.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams tLp = new LinearLayout.LayoutParams(-1, -2);
        tLp.setMargins(0, dp(12), 0, dp(6));
        card.addView(title, tLp);

        TextView desc = label("تعمل مفكرة مزود مباشرة على ملفات جهازك بصيغة TXT لضمان بقاء ملفاتك آمنة وتحت سيطرتك دائماً.", 14, COLOR_TEXT_MUTED, false);
        desc.setGravity(Gravity.CENTER);
        desc.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        desc.setLineSpacing(0, 1.25f);
        LinearLayout.LayoutParams dLp = new LinearLayout.LayoutParams(-1, -2);
        dLp.setMargins(0, 0, 0, dp(20));
        card.addView(desc, dLp);

        Button pickBtn = primaryButton("اختيار مجلد الملفات الآن", v -> pickFolder());
        pickBtn.setPadding(dp(24), dp(14), dp(24), dp(14));
        card.addView(pickBtn, new LinearLayout.LayoutParams(-1, -2));

        content.addView(card, new LinearLayout.LayoutParams(-1, -2));
    }

    String sortLabel() {
        int s = prefs.getInt("sort", 0);
        return s == 0 ? "الأحدث" : s == 1 ? "الاسم" : "الأقدم";
    }

    void filterFiles(String query) {
        if (content == null || tree == null) return;
        content.removeAllViews();
        String q = query.trim().toLowerCase(Locale.ROOT);

        ArrayList<FileItem> matched = new ArrayList<>();
        for (FileItem f : files) {
            if (q.isEmpty() || f.name.toLowerCase(Locale.ROOT).contains(q)) {
                matched.add(f);
            }
        }

        TextView count = label("عدد الملفات: " + matched.size() + " ملف TXT", 13, COLOR_TEXT_MUTED, false);
        count.setPadding(dp(6), dp(2), dp(6), dp(8));
        content.addView(count, new LinearLayout.LayoutParams(-1, -2));

        if (matched.isEmpty()) {
            LinearLayout empty = new LinearLayout(this);
            empty.setOrientation(LinearLayout.VERTICAL);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(dp(20), dp(28), dp(20), dp(28));
            empty.setBackground(borderedShape(COLOR_SURFACE, COLOR_BORDER, dp(1), dp(20)));

            TextView msg = label(files.isEmpty() ? "لا توجد ملفات TXT في هذا المجلد حتى الآن." : "لا توجد نتائج مطابقة لبحثك.", 16, COLOR_TEXT_MUTED, false);
            msg.setGravity(Gravity.CENTER);
            empty.addView(msg, new LinearLayout.LayoutParams(-1, -2));

            if (files.isEmpty()) {
                Button createFirst = tonalButton("＋ إنشاء أول ملف نصي", v -> newFile());
                createFirst.setPadding(dp(20), dp(10), dp(20), dp(10));
                LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(-2, -2);
                btnLp.setMargins(0, dp(14), 0, 0);
                empty.addView(createFirst, btnLp);
            }
            content.addView(empty, new LinearLayout.LayoutParams(-1, -2));
        } else {
            for (FileItem f : matched) {
                addFile(f);
            }
        }
    }

    void loadFiles() {
        if (content == null) return;
        content.removeAllViews();
        files.clear();

        try {
            String treeDocId = DocumentsContract.getTreeDocumentId(tree);
            Uri children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, treeDocId);
            Cursor c = getContentResolver().query(
                    children,
                    new String[]{
                            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                            DocumentsContract.Document.COLUMN_MIME_TYPE,
                            DocumentsContract.Document.COLUMN_LAST_MODIFIED
                    },
                    null, null, null
            );

            if (c != null) {
                while (c.moveToNext()) {
                    String id = c.getString(0);
                    String name = c.getString(1);
                    String mime = c.getString(2);
                    long mod = c.isNull(3) ? 0L : c.getLong(3);

                    if (name != null && !DocumentsContract.Document.MIME_TYPE_DIR.equals(mime)
                            && name.toLowerCase(Locale.ROOT).endsWith(".txt")) {
                        files.add(new FileItem(name, mod, id));
                    }
                }
                c.close();
            }

            Collections.sort(files, (a, b) -> {
                boolean ap = isPinned(a), bp = isPinned(b);
                if (ap != bp) return ap ? -1 : 1;
                int s = prefs.getInt("sort", 0);
                return s == 1
                        ? a.name.compareToIgnoreCase(b.name)
                        : s == 2
                        ? Long.compare(a.modified, b.modified)
                        : Long.compare(b.modified, a.modified);
            });
        } catch (Exception e) {
            status.setText("تعذر قراءة المجلد: " + e.getMessage());
        }

        filterFiles(searchInput != null ? searchInput.getText().toString() : "");
    }

    void addFile(FileItem f) {
        boolean pinned = isPinned(f);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(16), dp(14), dp(12), dp(14));
        row.setBackground(interactiveShape(
                pinned ? Color.rgb(248, 251, 255) : COLOR_SURFACE,
                pinned ? Color.rgb(186, 214, 242) : COLOR_BORDER,
                dp(1),
                dp(18)
        ));
        row.setElevation(dp(1));

        LinearLayout details = new LinearLayout(this);
        details.setOrientation(LinearLayout.VERTICAL);

        String displayTitle = f.name.endsWith(".txt") ? f.name.substring(0, f.name.length() - 4) : f.name;
        TextView name = label((pinned ? "📌 " : "") + displayTitle, 17, COLOR_TEXT_MAIN, true);
        name.setSingleLine(true);
        name.setEllipsize(TextUtils.TruncateAt.END);
        details.addView(name, new LinearLayout.LayoutParams(-1, -2));

        TextView when = label("آخر تعديل: " + date(f.modified), 12, COLOR_TEXT_MUTED, false);
        when.setSingleLine(true);
        LinearLayout.LayoutParams whenLp = new LinearLayout.LayoutParams(-1, -2);
        whenLp.setMargins(0, dp(4), 0, 0);
        details.addView(when, whenLp);

        LinearLayout.LayoutParams detailLp = new LinearLayout.LayoutParams(0, -2, 1f);
        detailLp.setMarginEnd(dp(10));
        row.addView(details, detailLp);

        Button more = tonalButton("⋮", v -> menu(f));
        more.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        more.setPadding(0, 0, 0, 0);
        row.addView(more, new LinearLayout.LayoutParams(dp(44), dp(44)));

        row.setOnClickListener(v -> read(f));
        row.setOnLongClickListener(v -> {
            menu(f);
            return true;
        });

        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(-1, -2);
        rowLp.setMargins(0, 0, 0, dp(10));
        content.addView(row, rowLp);
    }

    String date(long t) {
        return t <= 0 ? "غير معروف" : new SimpleDateFormat("yyyy/MM/dd  •  HH:mm", Locale.US).format(new Date(t));
    }

    void read(FileItem f) {
        hideKeyboard();
        currentScreen = "READ";
        activeFile = f;
        base();

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setPadding(0, dp(4), 0, dp(10));

        Button backBtn = button("→ القائمة", v -> showList());
        backBtn.setPadding(dp(12), dp(8), dp(12), dp(8));
        top.addView(backBtn, new LinearLayout.LayoutParams(-2, -2));

        TextView t = label(f.name, 17, COLOR_TEXT_MAIN, true);
        t.setSingleLine(true);
        t.setEllipsize(TextUtils.TruncateAt.MIDDLE);
        t.setPadding(dp(10), 0, dp(10), 0);
        top.addView(t, new LinearLayout.LayoutParams(0, -2, 1f));

        Button zoomOut = tonalButton("A-", v -> adjustZoom(-10));
        zoomOut.setPadding(dp(10), dp(6), dp(10), dp(6));
        top.addView(zoomOut, new LinearLayout.LayoutParams(-2, dp(38)));

        zoomBadgeView = label(prefs.getInt("fontScale", 100) + "%", 12, COLOR_PRIMARY, true);
        zoomBadgeView.setGravity(Gravity.CENTER);
        zoomBadgeView.setPadding(dp(6), 0, dp(6), 0);
        top.addView(zoomBadgeView, new LinearLayout.LayoutParams(-2, -2));

        Button zoomIn = tonalButton("A+", v -> adjustZoom(+10));
        zoomIn.setPadding(dp(10), dp(6), dp(10), dp(6));
        top.addView(zoomIn, new LinearLayout.LayoutParams(-2, dp(38)));

        Button moreBtn = button("⋮", v -> menu(f));
        moreBtn.setPadding(dp(10), dp(6), dp(10), dp(6));
        LinearLayout.LayoutParams moreLp = new LinearLayout.LayoutParams(dp(38), dp(38));
        moreLp.setMarginStart(dp(6));
        top.addView(moreBtn, moreLp);

        root.addView(top, new LinearLayout.LayoutParams(-1, -2));

        String fileContent = readFile(f);
        readerTextView = label(fileContent.isEmpty() ? "(الملف فارغ — اضغط على تحرير الملف للبدء بالكتابة)" : fileContent,
                getScaledFontSize(),
                fileContent.isEmpty() ? COLOR_TEXT_MUTED : COLOR_TEXT_MAIN,
                false);
        readerTextView.setTextIsSelectable(true);
        readerTextView.setGravity(Gravity.TOP | Gravity.START);
        readerTextView.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
        readerTextView.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG_RTL);
        readerTextView.setLineSpacing(0, 1.38f);
        readerTextView.setPadding(dp(18), dp(18), dp(18), dp(24));

        ScrollView sc = new ScrollView(this);
        sc.setClipToPadding(false);
        sc.setFillViewport(true);
        sc.setBackground(borderedShape(COLOR_PAPER, COLOR_BORDER, dp(1), dp(20)));
        sc.addView(readerTextView, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout.LayoutParams scLp = new LinearLayout.LayoutParams(-1, 0, 1f);
        scLp.setMargins(0, 0, 0, dp(10));
        root.addView(sc, scLp);

        LinearLayout bottomBar = new LinearLayout(this);
        bottomBar.setOrientation(LinearLayout.HORIZONTAL);
        bottomBar.setGravity(Gravity.CENTER_VERTICAL);

        Button editBtn = primaryButton("✎  تحرير الملف", v -> edit(f));
        editBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        editBtn.setPadding(dp(16), dp(14), dp(16), dp(14));
        bottomBar.addView(editBtn, new LinearLayout.LayoutParams(0, -2, 1f));

        Button shareBtn = button("مشاركة", v -> shareFile(f));
        shareBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        shareBtn.setPadding(dp(18), dp(14), dp(18), dp(14));
        LinearLayout.LayoutParams shareLp = new LinearLayout.LayoutParams(-2, -2);
        shareLp.setMarginStart(dp(10));
        bottomBar.addView(shareBtn, shareLp);

        root.addView(bottomBar, new LinearLayout.LayoutParams(-1, -2));
    }

    void edit(FileItem f) {
        currentScreen = "EDIT";
        activeFile = f;
        undo.clear();
        redo.clear();
        base();

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setPadding(0, dp(4), 0, dp(8));

        Button backToReader = button("→ القارئ", v -> {
            save(f, true);
            read(f);
        });
        backToReader.setPadding(dp(12), dp(8), dp(12), dp(8));
        top.addView(backToReader, new LinearLayout.LayoutParams(-2, -2));

        TextView t = label(f.name, 16, COLOR_TEXT_MAIN, true);
        t.setSingleLine(true);
        t.setEllipsize(TextUtils.TruncateAt.MIDDLE);
        t.setPadding(dp(8), 0, dp(8), 0);
        top.addView(t, new LinearLayout.LayoutParams(0, -2, 1f));

        Button zoomOut = tonalButton("A-", v -> adjustZoom(-10));
        zoomOut.setPadding(dp(10), dp(6), dp(10), dp(6));
        top.addView(zoomOut, new LinearLayout.LayoutParams(-2, dp(38)));

        zoomBadgeView = label(prefs.getInt("fontScale", 100) + "%", 12, COLOR_PRIMARY, true);
        zoomBadgeView.setGravity(Gravity.CENTER);
        zoomBadgeView.setPadding(dp(4), 0, dp(4), 0);
        top.addView(zoomBadgeView, new LinearLayout.LayoutParams(-2, -2));

        Button zoomIn = tonalButton("A+", v -> adjustZoom(+10));
        zoomIn.setPadding(dp(10), dp(6), dp(10), dp(6));
        top.addView(zoomIn, new LinearLayout.LayoutParams(-2, dp(38)));

        Button saveNowBtn = primaryButton("✓ حفظ", v -> {
            save(f, true);
            toast("تم حفظ الملف بنجاح");
        });
        saveNowBtn.setPadding(dp(12), dp(6), dp(12), dp(6));
        LinearLayout.LayoutParams saveLp = new LinearLayout.LayoutParams(-2, dp(38));
        saveLp.setMarginStart(dp(6));
        top.addView(saveNowBtn, saveLp);

        root.addView(top, new LinearLayout.LayoutParams(-1, -2));

        if (prefs.getBoolean("toolbar", true)) {
            HorizontalScrollView toolScroll = new HorizontalScrollView(this);
            toolScroll.setHorizontalScrollBarEnabled(false);
            toolScroll.setPadding(0, 0, 0, dp(8));

            LinearLayout tools = new LinearLayout(this);
            tools.setOrientation(LinearLayout.HORIZONTAL);
            tools.setGravity(Gravity.CENTER_VERTICAL);

            String[] labels = {"↶ تراجع", "↷ إعادة", "تحديد الكل", "نسخ", "لصق", "التاريخ والوقت", "نسخة أمان"};
            for (String x : labels) {
                Button b = tonalButton(x, v -> tool(x));
                b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
                b.setPadding(dp(12), dp(6), dp(12), dp(6));
                LinearLayout.LayoutParams bLp = new LinearLayout.LayoutParams(-2, -2);
                bLp.setMarginEnd(dp(6));
                tools.addView(b, bLp);
            }

            toolScroll.addView(tools, new ViewGroup.LayoutParams(-2, -2));
            root.addView(toolScroll, new LinearLayout.LayoutParams(-1, -2));
        }

        String initialText = readFile(f);
        editor = new EditText(this);
        editor.setText(initialText);
        beforeEdit = initialText;
        editor.setTextSize(TypedValue.COMPLEX_UNIT_SP, getScaledFontSize());
        editor.setTypeface(chosenFont(false));
        editor.setTextColor(COLOR_TEXT_MAIN);
        editor.setHint("ابدأ الكتابة هنا...");
        editor.setHintTextColor(COLOR_TEXT_MUTED);
        editor.setGravity(Gravity.TOP | Gravity.START);
        editor.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
        editor.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG_RTL);
        editor.setLineSpacing(0, 1.35f);
        editor.setPadding(dp(16), dp(16), dp(16), dp(16));
        editor.setBackground(borderedShape(COLOR_PAPER, COLOR_BORDER, dp(1), dp(18)));
        editor.setSingleLine(false);
        editor.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);

        root.addView(editor, new LinearLayout.LayoutParams(-1, 0, 1f));

        status = label(formatStats(initialText) + "  •  محفوظ", 12, COLOR_TEXT_MUTED, false);
        status.setPadding(dp(6), dp(6), dp(6), dp(2));
        root.addView(status, new LinearLayout.LayoutParams(-1, -2));

        editor.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int c, int d) {
                if (!applyingEdit) beforeEdit = s.toString();
            }

            public void onTextChanged(CharSequence s, int a, int b, int c) {
                String currentText = s.toString();
                if (!applyingEdit && !currentText.equals(beforeEdit)) {
                    if (undo.size() > 50) undo.removeFirst();
                    undo.addLast(beforeEdit);
                    redo.clear();
                }
                status.setText(formatStats(currentText) + "  •  جارٍ الحفظ التلقائي...");
                scheduleAutoSave(f);
            }

            public void afterTextChanged(Editable e) {}
        });
    }

    String formatStats(String txt) {
        int chars = txt.length();
        String trimmed = txt.trim();
        int words = trimmed.isEmpty() ? 0 : trimmed.split("\\s+").length;
        return words + " كلمة  |  " + chars + " حرف";
    }

    void scheduleAutoSave(FileItem f) {
        cancelPendingSave();
        pendingSaveRunnable = () -> save(f, false);
        saveHandler.postDelayed(pendingSaveRunnable, 800);
    }

    void cancelPendingSave() {
        if (pendingSaveRunnable != null) {
            saveHandler.removeCallbacks(pendingSaveRunnable);
            pendingSaveRunnable = null;
        }
    }

    void tool(String x) {
        if (editor == null) return;
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);

        if (x.equals("تحديد الكل")) {
            editor.requestFocus();
            editor.selectAll();
        } else if (x.equals("نسخ")) {
            int start = editor.getSelectionStart();
            int end = editor.getSelectionEnd();
            CharSequence selected = (start >= 0 && end > start)
                    ? editor.getText().subSequence(Math.min(start, end), Math.max(start, end))
                    : editor.getText();
            if (clipboard != null) {
                clipboard.setPrimaryClip(ClipData.newPlainText("مفكرة مزود", selected));
                toast("تم النسخ إلى الحافظة");
            }
        } else if (x.equals("لصق")) {
            if (clipboard != null && clipboard.hasPrimaryClip() && clipboard.getPrimaryClip() != null) {
                ClipData.Item item = clipboard.getPrimaryClip().getItemAt(0);
                CharSequence pasteData = item.coerceToText(this);
                if (pasteData != null) {
                    int start = Math.max(editor.getSelectionStart(), 0);
                    int end = Math.max(editor.getSelectionEnd(), 0);
                    editor.getText().replace(Math.min(start, end), Math.max(start, end), pasteData);
                }
            }
        } else if (x.equals("التاريخ والوقت")) {
            String stamp = new SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.US).format(new Date());
            int pos = Math.max(editor.getSelectionStart(), 0);
            editor.getText().insert(pos, stamp);
        } else if (x.equals("نسخة أمان")) {
            if (activeFile != null) {
                save(activeFile, true);
                safetyCopy(activeFile);
            }
        } else if (x.equals("↶ تراجع")) {
            if (!undo.isEmpty()) {
                redo.addLast(editor.getText().toString());
                applyEdit(undo.removeLast());
            } else {
                toast("لا توجد خطوات سابقة للتراجع");
            }
        } else if (x.equals("↷ إعادة")) {
            if (!redo.isEmpty()) {
                undo.addLast(editor.getText().toString());
                applyEdit(redo.removeLast());
            } else {
                toast("لا توجد خطوات للإعادة");
            }
        }
    }

    void applyEdit(String value) {
        applyingEdit = true;
        editor.setText(value);
        editor.setSelection(value.length());
        applyingEdit = false;
        if (activeFile != null) {
            scheduleAutoSave(activeFile);
        }
    }

    boolean hasValidTreePermission() {
        if (tree == null) return false;
        List<UriPermission> perms = getContentResolver().getPersistedUriPermissions();
        for (UriPermission p : perms) {
            if (p.getUri().equals(tree) && p.isReadPermission() && p.isWritePermission()) {
                return true;
            }
        }
        return false;
    }

    Uri getTreeRootDocumentUri() {
        if (tree == null) return null;
        return DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree));
    }

    Uri child(String id) {
        return DocumentsContract.buildDocumentUriUsingTree(tree, id);
    }

    void pickFolder() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(i, PICK_TREE);
    }

    @Override
    protected void onActivityResult(int r, int c, Intent d) {
        super.onActivityResult(r, c, d);
        if (r == PICK_TREE && c == RESULT_OK && d != null && d.getData() != null) {
            tree = d.getData();
            final int takeFlags = d.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            try {
                getContentResolver().takePersistableUriPermission(tree, takeFlags);
            } catch (Exception ignored) {}
            prefs.edit().putString("tree", tree.toString()).apply();
            showList();

            if (pendingNewFileAfterFolderPick) {
                pendingNewFileAfterFolderPick = false;
                newFile();
            }
        } else {
            pendingNewFileAfterFolderPick = false;
        }
    }

    String readFile(FileItem f) {
        if (f == null || tree == null) return "";
        try (InputStream in = getContentResolver().openInputStream(child(f.id));
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            if (in == null) return "";
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "تعذر فتح الملف: " + e.getMessage();
        }
    }

    void save(FileItem f, boolean immediate) {
        if (f == null || editor == null || tree == null) return;
        cancelPendingSave();
        String textToSave = editor.getText().toString();
        try {
            OutputStream out;
            try {
                out = getContentResolver().openOutputStream(child(f.id), "wt");
            } catch (Exception e) {
                out = getContentResolver().openOutputStream(child(f.id), "w");
            }
            if (out != null) {
                try {
                    out.write(textToSave.getBytes(StandardCharsets.UTF_8));
                    out.flush();
                } finally {
                    out.close();
                }
            }
            f.modified = System.currentTimeMillis();
            if (status != null) {
                status.setText(formatStats(textToSave) + "  •  تم الحفظ ✓");
            }
        } catch (Exception e) {
            if (status != null) {
                status.setText("فشل الحفظ: " + e.getMessage());
            }
        }
    }

    void newFile() {
        if (tree == null || !hasValidTreePermission()) {
            new AlertDialog.Builder(this)
                    .setTitle("اختيار مجلد الحفظ أولاً")
                    .setMessage("يرجى اختيار المجلد الذي ترغب في حفظ ملفاتك النصية بداخله أولاً.")
                    .setPositiveButton("اختيار المجلد", (d, w) -> {
                        pendingNewFileAfterFolderPick = true;
                        pickFolder();
                    })
                    .setNegativeButton("إلغاء", null)
                    .show();
            return;
        }

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(dp(20), dp(12), dp(20), dp(4));

        final EditText input = new EditText(this);
        input.setHint("مثال: أفكار وملاحظات");
        input.setSingleLine(true);
        input.setTextDirection(View.TEXT_DIRECTION_RTL);
        input.setPadding(dp(14), dp(12), dp(14), dp(12));
        input.setBackground(borderedShape(COLOR_BG, COLOR_BORDER, dp(1), dp(12)));
        container.addView(input, new LinearLayout.LayoutParams(-1, -2));

        new AlertDialog.Builder(this)
                .setTitle("إنشاء ملف نصي جديد")
                .setView(container)
                .setPositiveButton("إنشاء وفتح", (d, w) -> {
                    String raw = input.getText().toString().trim();
                    if (raw.isEmpty()) {
                        raw = "ملاحظة " + new SimpleDateFormat("yyyy-MM-dd HH-mm", Locale.US).format(new Date());
                    }
                    create(raw);
                })
                .setNegativeButton("إلغاء", null)
                .show();
    }

    void create(String rawName) {
        if (tree == null) {
            pickFolder();
            return;
        }
        String cleanName = rawName.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
        if (cleanName.isEmpty()) cleanName = "ملاحظة جديدة";
        if (!cleanName.toLowerCase(Locale.ROOT).endsWith(".txt")) {
            cleanName += ".txt";
        }

        try {
            Uri parent = getTreeRootDocumentUri();
            Uri u = DocumentsContract.createDocument(getContentResolver(), parent, "text/plain", cleanName);
            if (u != null) {
                String actualName = queryDisplayName(u, cleanName);
                FileItem f = new FileItem(actualName, System.currentTimeMillis(), DocumentsContract.getDocumentId(u));
                files.add(0, f);
                edit(f);
            }
        } catch (Exception e) {
            new AlertDialog.Builder(this)
                    .setTitle("تعذر إنشاء الملف")
                    .setMessage("تأكد من صلاحية الوصول للمجلد أو اختر المجلد مرة أخرى.\n\nالتفاصيل: " + e.getMessage())
                    .setPositiveButton("اختيار المجلد", (d, w) -> pickFolder())
                    .setNegativeButton("إغلاق", null)
                    .show();
        }
    }

    String queryDisplayName(Uri docUri, String fallback) {
        try (Cursor c = getContentResolver().query(docUri, new String[]{DocumentsContract.Document.COLUMN_DISPLAY_NAME}, null, null, null)) {
            if (c != null && c.moveToFirst()) {
                String n = c.getString(0);
                if (n != null && !n.isEmpty()) return n;
            }
        } catch (Exception ignored) {}
        return fallback;
    }

    void menu(FileItem f) {
        String[] options = new String[]{
                isPinned(f) ? "📌 إلغاء تثبيت الملف" : "📌 تثبيت الملف في الأعلى",
                "✎ تحرير الملف",
                "📤 مشاركة محتوى الملف",
                "🏷 إعادة تسمية الملف",
                "📄 إنشاء نسخة مكررة",
                "🛡 حفظ نسخة أمان الآن",
                "🕒 استعراض نسخ الأمان السابقة",
                "🗑 حذف الملف نهائياً"
        };

        new AlertDialog.Builder(this)
                .setTitle(f.name)
                .setItems(options, (d, w) -> {
                    if (w == 0) togglePin(f);
                    else if (w == 1) edit(f);
                    else if (w == 2) shareFile(f);
                    else if (w == 3) rename(f);
                    else if (w == 4) duplicate(f);
                    else if (w == 5) safetyCopy(f);
                    else if (w == 6) showSafetyVersions(f);
                    else if (w == 7) confirmDelete(f);
                })
                .show();
    }

    boolean isPinned(FileItem f) {
        return prefs.getBoolean("pin:" + f.name, false);
    }

    void togglePin(FileItem f) {
        prefs.edit().putBoolean("pin:" + f.name, !isPinned(f)).apply();
        if ("LIST".equals(currentScreen)) showList();
        else toast(isPinned(f) ? "تم تثبيت الملف" : "تم إلغاء التثبيت");
    }

    void shareFile(FileItem f) {
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("text/plain");
        i.putExtra(Intent.EXTRA_SUBJECT, f.name);
        i.putExtra(Intent.EXTRA_TEXT, readFile(f));
        startActivity(Intent.createChooser(i, "مشاركة الملف"));
    }

    void rename(FileItem f) {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(dp(20), dp(12), dp(20), dp(4));

        final EditText in = new EditText(this);
        String baseName = f.name.endsWith(".txt") ? f.name.substring(0, f.name.length() - 4) : f.name;
        in.setText(baseName);
        in.setSingleLine(true);
        in.setTextDirection(View.TEXT_DIRECTION_RTL);
        in.setPadding(dp(14), dp(12), dp(14), dp(12));
        in.setBackground(borderedShape(COLOR_BG, COLOR_BORDER, dp(1), dp(12)));
        container.addView(in, new LinearLayout.LayoutParams(-1, -2));

        new AlertDialog.Builder(this)
                .setTitle("إعادة تسمية الملف")
                .setView(container)
                .setPositiveButton("حفظ الاسم", (d, w) -> {
                    String newName = in.getText().toString().trim();
                    if (newName.isEmpty()) return;
                    if (!newName.toLowerCase(Locale.ROOT).endsWith(".txt")) {
                        newName += ".txt";
                    }
                    if (newName.equals(f.name)) return;

                    try {
                        boolean wasPinned = isPinned(f);
                        Uri renamedUri = DocumentsContract.renameDocument(getContentResolver(), child(f.id), newName);
                        if (renamedUri != null) {
                            if (wasPinned) {
                                prefs.edit().remove("pin:" + f.name).putBoolean("pin:" + newName, true).apply();
                            }
                            f.id = DocumentsContract.getDocumentId(renamedUri);
                            f.name = queryDisplayName(renamedUri, newName);
                            toast("تم تغيير الاسم بنجاح");
                            if ("READ".equals(currentScreen)) read(f);
                            else showList();
                        }
                    } catch (Exception e) {
                        toast("تعذر إعادة التسمية: " + e.getMessage());
                    }
                })
                .setNegativeButton("إلغاء", null)
                .show();
    }

    void duplicate(FileItem f) {
        try {
            Uri parent = getTreeRootDocumentUri();
            String copyName = f.name.replace(".txt", "") + " - نسخة.txt";
            Uri u = DocumentsContract.createDocument(getContentResolver(), parent, "text/plain", copyName);
            if (u != null) {
                try (OutputStream out = getContentResolver().openOutputStream(u)) {
                    if (out != null) {
                        out.write(readFile(f).getBytes(StandardCharsets.UTF_8));
                    }
                }
                toast("تم إنشاء نسخة مكررة");
                if ("LIST".equals(currentScreen)) showList();
            }
        } catch (Exception e) {
            toast("فشل نسخ الملف: " + e.getMessage());
        }
    }

    Uri findChildByName(Uri parentDocUri, String wanted) {
        try {
            String id = DocumentsContract.getDocumentId(parentDocUri);
            Uri children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, id);
            try (Cursor c = getContentResolver().query(children,
                    new String[]{DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME},
                    null, null, null)) {
                if (c != null) {
                    while (c.moveToNext()) {
                        if (wanted.equals(c.getString(1))) {
                            String childId = c.getString(0);
                            return DocumentsContract.buildDocumentUriUsingTree(tree, childId);
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    Uri ensureDirectory(String name) {
        try {
            Uri rootDoc = getTreeRootDocumentUri();
            Uri existing = findChildByName(rootDoc, name);
            return existing != null
                    ? existing
                    : DocumentsContract.createDocument(getContentResolver(), rootDoc, DocumentsContract.Document.MIME_TYPE_DIR, name);
        } catch (Exception e) {
            return null;
        }
    }

    void safetyCopy(FileItem f) {
        try {
            Uri dir = ensureDirectory("نسخ آمنة");
            if (dir == null) throw new IOException("تعذر الوصول لمجلد النسخ الآمنة");
            String base = f.name.replace(".txt", "");
            String name = base + "-" + new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(new Date()) + ".txt";
            Uri u = DocumentsContract.createDocument(getContentResolver(), dir, "text/plain", name);
            if (u != null) {
                try (OutputStream out = getContentResolver().openOutputStream(u)) {
                    if (out != null) out.write(readFile(f).getBytes(StandardCharsets.UTF_8));
                }
                toast("تم حفظ نسخة أمان بنجاح");
            }
        } catch (Exception e) {
            toast("فشل إنشاء نسخة الأمان: " + e.getMessage());
        }
    }

    void showSafetyVersions(FileItem f) {
        try {
            Uri rootDoc = getTreeRootDocumentUri();
            Uri dir = findChildByName(rootDoc, "نسخ آمنة");
            if (dir == null) {
                new AlertDialog.Builder(this)
                        .setTitle("نسخ الأمان السابقة")
                        .setMessage("لا توجد نسخ أمان محفوظة بعد.")
                        .setPositiveButton("حسناً", null)
                        .show();
                return;
            }
            String prefix = f.name.replace(".txt", "");
            ArrayList<String> names = new ArrayList<>();
            String id = DocumentsContract.getDocumentId(dir);
            Uri children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, id);
            try (Cursor c = getContentResolver().query(children, new String[]{DocumentsContract.Document.COLUMN_DISPLAY_NAME}, null, null, null)) {
                if (c != null) {
                    while (c.moveToNext()) {
                        String n = c.getString(0);
                        if (n != null && n.startsWith(prefix + "-") && n.endsWith(".txt")) {
                            names.add(n);
                        }
                    }
                }
            }
            Collections.sort(names, Collections.reverseOrder());

            if (names.isEmpty()) {
                new AlertDialog.Builder(this)
                        .setTitle("نسخ الأمان السابقة")
                        .setMessage("لا توجد نسخ أمان لهذا الملف بعد.")
                        .setPositiveButton("حسناً", null)
                        .show();
            } else {
                new AlertDialog.Builder(this)
                        .setTitle("نسخ الأمان السابقة")
                        .setItems(names.toArray(new String[0]), (d, w) -> {
                            Uri v = findChildByName(dir, names.get(w));
                            if (v != null) showVersionText(names.get(w), v);
                        })
                        .setNegativeButton("إغلاق", null)
                        .show();
            }
        } catch (Exception e) {
            toast("تعذر عرض نسخ الأمان: " + e.getMessage());
        }
    }

    void showVersionText(String name, Uri uri) {
        try (InputStream in = getContentResolver().openInputStream(uri);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            if (in != null) {
                byte[] b = new byte[8192];
                int n;
                while ((n = in.read(b)) != -1) out.write(b, 0, n);
            }
            String contentText = new String(out.toByteArray(), StandardCharsets.UTF_8);
            TextView v = text(contentText, 17);
            v.setTextIsSelectable(true);
            v.setLineSpacing(0, 1.3f);
            v.setPadding(dp(20), dp(16), dp(20), dp(16));
            ScrollView s = new ScrollView(this);
            s.addView(v);
            new AlertDialog.Builder(this)
                    .setTitle(name)
                    .setView(s)
                    .setPositiveButton("إغلاق", null)
                    .show();
        } catch (Exception e) {
            toast("تعذر فتح النسخة: " + e.getMessage());
        }
    }

    void confirmDelete(FileItem f) {
        new AlertDialog.Builder(this)
                .setTitle("تحذير حذف الملف")
                .setMessage("هل أنت متأكد من حذف الملف «" + f.name + "» نهائياً؟ لا يمكن التراجع عن هذه الخطوة.")
                .setPositiveButton("نعم، احذف", (d, w) -> {
                    try {
                        DocumentsContract.deleteDocument(getContentResolver(), child(f.id));
                        prefs.edit().remove("pin:" + f.name).apply();
                        toast("تم حذف الملف");
                        showList();
                    } catch (Exception e) {
                        toast("تعذر الحذف: " + e.getMessage());
                    }
                })
                .setNegativeButton("إلغاء", null)
                .show();
    }

    void backup() {
        if (tree == null) {
            pickFolder();
            return;
        }
        if (files.isEmpty()) {
            toast("لا توجد ملفات لعمل نسخة احتياطية لها");
            return;
        }
        try {
            String name = "Backup-" + new SimpleDateFormat("yyyy-MM-dd-HHmmss", Locale.US).format(new Date()) + ".zip";
            Uri dir = ensureDirectory("Backup");
            if (dir == null) throw new IOException("تعذر إنشاء مجلد Backup");
            Uri z = DocumentsContract.createDocument(getContentResolver(), dir, "application/zip", name);
            if (z == null) throw new IOException("تعذر إنشاء ملف ZIP");

            try (ZipOutputStream out = new ZipOutputStream(getContentResolver().openOutputStream(z))) {
                for (FileItem f : files) {
                    out.putNextEntry(new ZipEntry(f.name));
                    out.write(readFile(f).getBytes(StandardCharsets.UTF_8));
                    out.closeEntry();
                }
            }
            if (status != null) status.setText("✓ تم حفظ النسخة الاحتياطية في مجلد Backup/" + name);
            toast("تم إنشاء النسخة الاحتياطية بنجاح");
        } catch (Exception e) {
            toast("فشل النسخ الاحتياطي: " + e.getMessage());
        }
    }

    void settings() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        box.setPadding(dp(22), dp(14), dp(22), dp(8));

        int currentScale = prefs.getInt("fontScale", 100);
        final TextView sizeLabel = label("حجم الخط (التكبير والتصغير): " + currentScale + "%", 16, COLOR_TEXT_MAIN, true);
        box.addView(sizeLabel, new LinearLayout.LayoutParams(-1, -2));

        final SeekBar size = new SeekBar(this);
        size.setMax(160);
        size.setProgress(Math.max(0, Math.min(160, currentScale - 60)));
        LinearLayout.LayoutParams seekLp = new LinearLayout.LayoutParams(-1, -2);
        seekLp.setMargins(0, dp(8), 0, dp(16));
        box.addView(size, seekLp);

        TextView faceLabel = label("نوع الخط المستخدم", 16, COLOR_TEXT_MAIN, true);
        box.addView(faceLabel, new LinearLayout.LayoutParams(-1, -2));

        final Spinner faces = new Spinner(this);
        String[] names = {
                "Sans — واضح وعصري (افتراضي)",
                "Serif — رسمي ومريح للقراءة",
                "Amiri — خط عربي كلاسيكي أصيل",
                "Monospace — ثابت العرض",
                "Cursive — خط انسيابي ناعم"
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, names);
        faces.setAdapter(adapter);
        faces.setSelection(prefs.getInt("fontFace", 0));
        LinearLayout.LayoutParams spinLp = new LinearLayout.LayoutParams(-1, -2);
        spinLp.setMargins(0, dp(6), 0, dp(16));
        box.addView(faces, spinLp);

        final CheckBox toolbar = new CheckBox(this);
        toolbar.setText("إظهار شريط أدوات التحرير المساعد");
        toolbar.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        toolbar.setTextColor(COLOR_TEXT_MAIN);
        toolbar.setTypeface(chosenFont(false));
        toolbar.setChecked(prefs.getBoolean("toolbar", true));
        toolbar.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        box.addView(toolbar, new LinearLayout.LayoutParams(-1, -2));

        size.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar b, int p, boolean from) {
                int value = 60 + p;
                sizeLabel.setText("حجم الخط (التكبير والتصغير): " + value + "%");
            }
            public void onStartTrackingTouch(SeekBar b) {}
            public void onStopTrackingTouch(SeekBar b) {}
        });

        new AlertDialog.Builder(this)
                .setTitle("إعدادات القراءة والكتابة")
                .setView(box)
                .setPositiveButton("حفظ وتطبيق", (d, w) -> {
                    prefs.edit()
                            .putInt("fontScale", 60 + size.getProgress())
                            .putInt("fontFace", faces.getSelectedItemPosition())
                            .putBoolean("toolbar", toolbar.isChecked())
                            .apply();
                    showList();
                    toast("تم تطبيق الإعدادات");
                })
                .setNeutralButton("تغيير المجلد", (d, w) -> pickFolder())
                .setNegativeButton("إلغاء", null)
                .show();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if ("EDIT".equals(currentScreen) && activeFile != null && editor != null) {
            save(activeFile, true);
        }
    }

    @Override
    public void onBackPressed() {
        if ("EDIT".equals(currentScreen) && activeFile != null) {
            save(activeFile, true);
            read(activeFile);
            return;
        }
        if ("READ".equals(currentScreen)) {
            showList();
            return;
        }
        super.onBackPressed();
    }

    static class FileItem {
        String name, id;
        long modified;

        FileItem(String n, long m, String i) {
            name = n;
            modified = m;
            id = i;
        }
    }
}
```

---
