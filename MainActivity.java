package com.mazwd.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.DocumentsContract;
import android.text.Editable;
import android.text.InputType;
import android.text.Selection;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class MainActivity extends Activity {

    private static final int REQ_OPEN_TREE = 1001;
    private static final int REQ_CREATE_ZIP = 1002;

    private static final String PREFS_NAME = "MazwdPrefs";
    private static final String KEY_TREE_URI = "tree_uri";
    private static final String KEY_THEME = "theme_index";
    private static final String KEY_FONT = "font_index";
    private static final String KEY_ZOOM = "zoom_percent";
    private static final String KEY_SIGNATURE = "custom_signature";
    private static final String KEY_SIG_INIT = "sig_init_v3";
    private static final String KEY_TOOLBAR_ORDER = "toolbar_order_v3";
    private static final String KEY_ENABLED_TOOLS = "enabled_tools_v3";
    private static final String KEY_TOOLBAR_ICONS = "toolbar_icons_only";

    private static final String DEFAULT_SIGNATURE = "أ. شفيقة مزود | باحثة في التراث الحضرمي";
    private static final String SIGNATURE_HINT = "اكتب توقيعك هنا\nمثال: عبدالله سعيد - مدير التحرير";

    private static final String[] TOOL_IDS = {
            "undo", "redo", "copy", "paste", "select_all",
            "auto_format", "signature", "cut", "select_up", "select_down", "datetime"
    };

    private static final String[] TOOL_LABELS = {
            "تراجع", "إعادة", "نسخ", "لصق", "تحديد الكل",
            "تنسيق آلي", "التوقيع", "قص", "تحديد لأعلى", "تحديد لأسفل", "التاريخ والوقت"
    };

    private static final String[] TOOL_ICON_RES = {
            "ic_undo", "ic_redo", "ic_copy", "ic_paste", "ic_select_all",
            "ic_auto_format", "ic_signature", "ic_cut", "ic_select_up", "ic_select_down", "ic_datetime"
    };

    private static final String[] DEFAULT_ENABLED_TOOLS = {
            "undo", "redo", "copy", "paste", "select_all", "auto_format", "signature"
    };

    private static final String[] THEME_NAMES = {
            "الكحلي الملكي", "الأخضر الزمردي", "العنابي الفاخر", "البنفسجي العصري",
            "البني التراثي", "الأزرق المحيطي", "الرمادي الدافئ", "الطيني الحضرمي"
    };

    private static final int[] THEME_PRIMARY = {
            0xFF1E3A8A, 0xFF065F46, 0xFF7F1D1D, 0xFF4C1D95,
            0xFF78350F, 0xFF0C4A6E, 0xFF334155, 0xFF8C4A27
    };

    private static final int[] THEME_DARK = {
            0xFF172554, 0xFF022C22, 0xFF450A0A, 0xFF2E1065,
            0xFF451A03, 0xFF082F49, 0xFF1E293B, 0xFF5C2E14
    };

    private static final int[] THEME_ACCENT = {
            0xFFDBEAFE, 0xFFD1FAE5, 0xFFFEE2E2, 0xFFEDE9FE,
            0xFFFEF3C7, 0xFFE0F2FE, 0xFFF1F5F9, 0xFFFDEBD0
    };

    private static final String[] FONT_NAMES = {
            "Sans — خط واضح وعصري (افتراضي)",
            "تجوال — خط حديث وأنيق للمقالات",
            "الأميري — خط نسخ كلاسيكي أصيل",
            "عارف رقعة — خط الرقعة العربي",
            "ريم كوفي — خط كوفي حديث ومميز",
            "Serif — خط إنجليزي رسمي ومريح",
            "Cursive — خط فني مائل"
    };

    private static final int COLOR_BG = 0xFFF4F6F9;
    private static final int COLOR_CARD = 0xFFFFFFFF;
    private static final int COLOR_TEXT_MAIN = 0xFF1E293B;
    private static final int COLOR_TEXT_SUB = 0xFF64748B;
    private static final int COLOR_DIVIDER = 0xFFE2E8F0;
    private static final int COLOR_HIGHLIGHT = 0xFFFDE047;

    private enum Mode { HOME, READ, EDIT }
    private Mode currentMode = Mode.HOME;

    private SharedPreferences prefs;
    private Uri treeUri;
    private int themeIndex = 0;
    private int fontIndex = 0;
    private int zoomPercent = 100;
    private boolean toolbarIconsOnly = false;

    private int themePrimary = THEME_PRIMARY[0];
    private int themeDark = THEME_DARK[0];
    private int themeAccent = THEME_ACCENT[0];

    private Typeface fontAmiri, fontAmiriBold;
    private Typeface fontRuqaa, fontRuqaaBold;
    private Typeface fontTajawal, fontTajawalBold;
    private Typeface fontKufi, fontKufiBold;

    private static class FileItem {
        Uri uri;
        String name;
        String displayName;
        long lastModified;
        String contentCache;
    }

    private final List<FileItem> allFiles = new ArrayList<>();
    private FileItem currentFile = null;
    private String currentSearchQuery = "";

    private EditText editorView;
    private TextView statsTextView;
    private TextView zoomLabelView;
    private LinearLayout fileListContainer;

    private final List<String> undoStack = new ArrayList<>();
    private final List<String> redoStack = new ArrayList<>();
    private boolean isUndoRedoInProgress = false;
    private boolean isDirty = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        if (!prefs.getBoolean(KEY_SIG_INIT, false)) {
            prefs.edit()
                    .putString(KEY_SIGNATURE, DEFAULT_SIGNATURE)
                    .putBoolean(KEY_SIG_INIT, true)
                    .apply();
        }

        themeIndex = prefs.getInt(KEY_THEME, 0);
        if (themeIndex < 0 || themeIndex >= THEME_PRIMARY.length) themeIndex = 0;
        fontIndex = prefs.getInt(KEY_FONT, 0);
        if (fontIndex < 0 || fontIndex >= FONT_NAMES.length) fontIndex = 0;
        zoomPercent = prefs.getInt(KEY_ZOOM, 100);
        if (zoomPercent < 50 || zoomPercent > 250) zoomPercent = 100;
        toolbarIconsOnly = prefs.getBoolean(KEY_TOOLBAR_ICONS, false);

        String uriStr = prefs.getString(KEY_TREE_URI, null);
        if (uriStr != null) {
            try {
                treeUri = Uri.parse(uriStr);
            } catch (Exception ignored) {
                treeUri = null;
            }
        }

        loadCustomFonts();
        applyThemeColors();
        showHome();
    }

    private void loadCustomFonts() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                fontAmiri = getResources().getFont(R.font.mazwd_arabic);
                fontAmiriBold = getResources().getFont(R.font.mazwd_arabic_bold);
            } catch (Exception ignored) {}
            try {
                fontRuqaa = getResources().getFont(R.font.aref_ruqaa);
                fontRuqaaBold = getResources().getFont(R.font.aref_ruqaa_bold);
            } catch (Exception ignored) {}
            try {
                fontTajawal = getResources().getFont(R.font.tajawal_regular);
                fontTajawalBold = getResources().getFont(R.font.tajawal_bold);
            } catch (Exception ignored) {}
            try {
                fontKufi = getResources().getFont(R.font.reem_kufi);
                fontKufiBold = getResources().getFont(R.font.reem_kufi_bold);
            } catch (Exception ignored) {}
        }
    }

    private void applyThemeColors() {
        themePrimary = THEME_PRIMARY[themeIndex];
        themeDark = THEME_DARK[themeIndex];
        themeAccent = THEME_ACCENT[themeIndex];
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(themeDark);
            window.setNavigationBarColor(COLOR_CARD);
        }
    }

    private Typeface getFontTypeface(int index, boolean bold) {
        int style = bold ? Typeface.BOLD : Typeface.NORMAL;
        switch (index) {
            case 1:
                if (bold && fontTajawalBold != null) return fontTajawalBold;
                if (fontTajawal != null) return fontTajawal;
                break;
            case 2:
                if (bold && fontAmiriBold != null) return fontAmiriBold;
                if (fontAmiri != null) return fontAmiri;
                break;
            case 3:
                if (bold && fontRuqaaBold != null) return fontRuqaaBold;
                if (fontRuqaa != null) return fontRuqaa;
                break;
            case 4:
                if (bold && fontKufiBold != null) return fontKufiBold;
                if (fontKufi != null) return fontKufi;
                break;
            case 5:
                return Typeface.create(Typeface.SERIF, style);
            case 6:
                return Typeface.create("cursive", style);
            default:
                return Typeface.create(Typeface.SANS_SERIF, style);
        }
        return Typeface.create(Typeface.SANS_SERIF, style);
    }

    private List<String> getOrderedTools() {
        String saved = prefs.getString(KEY_TOOLBAR_ORDER, null);
        List<String> result = new ArrayList<>();
        Set<String> validIds = new HashSet<>(Arrays.asList(TOOL_IDS));
        if (saved != null && !saved.trim().isEmpty()) {
            String[] parts = saved.split(",");
            for (String p : parts) {
                String id = p.trim();
                if (validIds.contains(id) && !result.contains(id)) {
                    result.add(id);
                }
            }
        }
        for (String id : TOOL_IDS) {
            if (!result.contains(id)) {
                result.add(id);
            }
        }
        return result;
    }

    private Set<String> getEnabledTools() {
        Set<String> saved = prefs.getStringSet(KEY_ENABLED_TOOLS, null);
        if (saved == null) {
            return new LinkedHashSet<>(Arrays.asList(DEFAULT_ENABLED_TOOLS));
        }
        return new LinkedHashSet<>(saved);
    }

    private void saveToolbarConfig(List<String> order, Set<String> enabled) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < order.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(order.get(i));
        }
        prefs.edit()
                .putString(KEY_TOOLBAR_ORDER, sb.toString())
                .putStringSet(KEY_ENABLED_TOOLS, new HashSet<>(enabled))
                .apply();
    }

    private int getToolIndex(String id) {
        for (int i = 0; i < TOOL_IDS.length; i++) {
            if (TOOL_IDS[i].equals(id)) return i;
        }
        return 0;
    }

    private String getToolLabel(String id) {
        return TOOL_LABELS[getToolIndex(id)];
    }

    private String getToolIconResName(String id) {
        return TOOL_ICON_RES[getToolIndex(id)];
    }

    private Drawable getDrawableByName(String resName, int tintColor) {
        try {
            int resId = getResources().getIdentifier(resName, "drawable", getPackageName());
            if (resId != 0) {
                Drawable d = getDrawable(resId);
                if (d != null) {
                    Drawable copy = d.mutate();
                    copy.setTint(tintColor);
                    return copy;
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private void showHome() {
        currentMode = Mode.HOME;
        currentFile = null;
        hideKeyboard();

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(COLOR_BG);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        // 1. Top Hero Header Card
        LinearLayout heroCard = new LinearLayout(this);
        heroCard.setOrientation(LinearLayout.VERTICAL);
        heroCard.setGravity(Gravity.CENTER_HORIZONTAL);
        heroCard.setPadding(dp(20), dp(22), dp(20), dp(20));

        GradientDrawable heroBg = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{themePrimary, themeDark}
        );
        heroBg.setCornerRadii(new float[]{0, 0, 0, 0, dp(26), dp(26), dp(26), dp(26)});
        heroCard.setBackground(heroBg);
        heroCard.setElevation(dp(6));

        TextView appTitle = new TextView(this);
        appTitle.setText("مفكرة أ. شفيقة مزود");
        appTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 23);
        appTitle.setTextColor(Color.WHITE);
        appTitle.setGravity(Gravity.CENTER);
        appTitle.setTypeface(getFontTypeface(fontIndex, true));
        heroCard.addView(appTitle);

        TextView appSub = new TextView(this);
        appSub.setText("كاتبة وباحثة في التراث الحضرمي");
        appSub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        appSub.setTextColor(0xFFE2E8F0);
        appSub.setGravity(Gravity.CENTER);
        appSub.setPadding(0, dp(4), 0, dp(16));
        appSub.setTypeface(getFontTypeface(fontIndex, false));
        heroCard.addView(appSub);

        Button newFileBtn = new Button(this);
        newFileBtn.setText("＋ ملف جديد");
        newFileBtn.setAllCaps(false);
        newFileBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        newFileBtn.setTextColor(themePrimary);
        newFileBtn.setTypeface(getFontTypeface(fontIndex, true));
        GradientDrawable newBtnBg = new GradientDrawable();
        newBtnBg.setColor(Color.WHITE);
        newBtnBg.setCornerRadius(dp(14));
        newFileBtn.setBackground(newBtnBg);
        newFileBtn.setPadding(dp(28), dp(10), dp(28), dp(10));
        newFileBtn.setOnClickListener(v -> showCreateFileDialog());

        LinearLayout.LayoutParams newBtnLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        heroCard.addView(newFileBtn, newBtnLp);

        root.addView(heroCard, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // 2. Section Header Row
        LinearLayout sectionRow = new LinearLayout(this);
        sectionRow.setOrientation(LinearLayout.HORIZONTAL);
        sectionRow.setGravity(Gravity.CENTER_VERTICAL);
        sectionRow.setPadding(dp(18), dp(14), dp(18), dp(8));

        TextView sectionTitle = new TextView(this);
        sectionTitle.setText("ملفاتي النصية");
        sectionTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        sectionTitle.setTextColor(COLOR_TEXT_MAIN);
        sectionTitle.setTypeface(getFontTypeface(fontIndex, true));
        LinearLayout.LayoutParams stLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        sectionRow.addView(sectionTitle, stLp);

        TextView txtBadge = new TextView(this);
        txtBadge.setText("TXT");
        txtBadge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        txtBadge.setTextColor(themePrimary);
        txtBadge.setTypeface(Typeface.DEFAULT_BOLD);
        txtBadge.setPadding(dp(10), dp(4), dp(10), dp(4));
        GradientDrawable badgeBg = new GradientDrawable();
        badgeBg.setColor(themeAccent);
        badgeBg.setCornerRadius(dp(8));
        txtBadge.setBackground(badgeBg);
        sectionRow.addView(txtBadge);

        root.addView(sectionRow);

        // 3. Search Bar
        LinearLayout searchWrap = new LinearLayout(this);
        searchWrap.setOrientation(LinearLayout.HORIZONTAL);
        searchWrap.setGravity(Gravity.CENTER_VERTICAL);
        searchWrap.setPadding(dp(14), dp(4), dp(14), dp(4));
        GradientDrawable searchBg = new GradientDrawable();
        searchBg.setColor(COLOR_CARD);
        searchBg.setCornerRadius(dp(14));
        searchBg.setStroke(dp(1), COLOR_DIVIDER);
        searchWrap.setBackground(searchBg);

        EditText searchInput = new EditText(this);
        searchInput.setHint("🔍 ابحث في أسماء ومحتوى ملفاتك...");
        searchInput.setHintTextColor(COLOR_TEXT_SUB);
        searchInput.setTextColor(COLOR_TEXT_MAIN);
        searchInput.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        searchInput.setSingleLine(true);
        searchInput.setBackground(null);
        searchInput.setPadding(dp(8), dp(10), dp(8), dp(10));
        searchInput.setTypeface(getFontTypeface(fontIndex, false));
        searchInput.setText(currentSearchQuery);

        TextView clearSearchBtn = new TextView(this);
        clearSearchBtn.setText("✕");
        clearSearchBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        clearSearchBtn.setTextColor(COLOR_TEXT_SUB);
        clearSearchBtn.setPadding(dp(10), dp(6), dp(10), dp(6));
        clearSearchBtn.setVisibility(currentSearchQuery.isEmpty() ? View.GONE : View.VISIBLE);
        clearSearchBtn.setOnClickListener(v -> {
            searchInput.setText("");
            currentSearchQuery = "";
            refreshHomeList("");
        });

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                currentSearchQuery = s.toString().trim();
                clearSearchBtn.setVisibility(currentSearchQuery.isEmpty() ? View.GONE : View.VISIBLE);
                refreshHomeList(currentSearchQuery);
            }
        });

        searchWrap.addView(searchInput, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        searchWrap.addView(clearSearchBtn);

        LinearLayout.LayoutParams swLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        swLp.setMargins(dp(16), 0, dp(16), dp(10));
        root.addView(searchWrap, swLp);

        // 4. Scrollable File List
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        fileListContainer = new LinearLayout(this);
        fileListContainer.setOrientation(LinearLayout.VERTICAL);
        fileListContainer.setPadding(dp(16), dp(4), dp(16), dp(16));
        scrollView.addView(fileListContainer);
        root.addView(scrollView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        // 5. Bottom Action Bar (Settings + ZIP Backup)
        LinearLayout bottomBar = new LinearLayout(this);
        bottomBar.setOrientation(LinearLayout.HORIZONTAL);
        bottomBar.setGravity(Gravity.CENTER);
        bottomBar.setPadding(dp(16), dp(12), dp(16), dp(12));
        bottomBar.setBackgroundColor(COLOR_CARD);
        bottomBar.setElevation(dp(8));

        Button settingsBtn = makeSecondaryButton("⚙ الإعدادات");
        settingsBtn.setOnClickListener(v -> showSettingsDialog());

        View space = new View(this);

        Button backupBtn = makePrimaryButton("📦 نسخ احتياطي (ZIP)");
        backupBtn.setOnClickListener(v -> createZipBackup());

        bottomBar.addView(settingsBtn, new LinearLayout.LayoutParams(0, dp(48), 1f));
        bottomBar.addView(space, new LinearLayout.LayoutParams(dp(12), dp(1)));
        bottomBar.addView(backupBtn, new LinearLayout.LayoutParams(0, dp(48), 1.2f));

        root.addView(bottomBar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        setContentView(root);

        loadFilesFromStorage();
        refreshHomeList(currentSearchQuery);
    }

    private void loadFilesFromStorage() {
        allFiles.clear();
        if (treeUri == null) return;

        try {
            String treeDocId = DocumentsContract.getTreeDocumentId(treeUri);
            Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, treeDocId);
            String[] projection = new String[]{
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_MIME_TYPE,
                    DocumentsContract.Document.COLUMN_LAST_MODIFIED
            };

            try (android.database.Cursor cursor = getContentResolver().query(
                    childrenUri, projection, null, null, null)) {
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        String docId = cursor.getString(0);
                        String name = cursor.getString(1);
                        String mime = cursor.getString(2);
                        long lastMod = cursor.getLong(3);

                        if (name == null) continue;
                        if (DocumentsContract.Document.MIME_TYPE_DIR.equals(mime)) continue;
                        if (name.toLowerCase(Locale.ROOT).endsWith(".txt") || "text/plain".equals(mime)) {
                            FileItem item = new FileItem();
                            item.uri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId);
                            item.name = name;
                            item.displayName = name.toLowerCase(Locale.ROOT).endsWith(".txt")
                                    ? name.substring(0, name.length() - 4) : name;
                            item.lastModified = lastMod;
                            allFiles.add(item);
                        }
                    }
                }
            }
            Collections.sort(allFiles, (a, b) -> Long.compare(b.lastModified, a.lastModified));
        } catch (Exception e) {
            showToast("تعذر قراءة المجلد، يرجى إعادة اختيار المجلد من الإعدادات");
        }
    }

    private void refreshHomeList(String query) {
        if (fileListContainer == null) return;
        fileListContainer.removeAllViews();

        if (treeUri == null) {
            LinearLayout emptyBox = new LinearLayout(this);
            emptyBox.setOrientation(LinearLayout.VERTICAL);
            emptyBox.setGravity(Gravity.CENTER);
            emptyBox.setPadding(dp(24), dp(40), dp(24), dp(40));

            TextView msg = new TextView(this);
            msg.setText("لم يتم تحديد مجلد لحفظ الملفات النصية بعد.\nاضغط الزر بالأسفل لاختيار مجلد على هاتفك.");
            msg.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            msg.setTextColor(COLOR_TEXT_SUB);
            msg.setGravity(Gravity.CENTER);
            msg.setTypeface(getFontTypeface(fontIndex, false));
            emptyBox.addView(msg);

            Button pickBtn = makePrimaryButton("📁 اختيار مجلد الحفظ الآن");
            LinearLayout.LayoutParams pbLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            pbLp.topMargin = dp(18);
            pickBtn.setPadding(dp(24), dp(12), dp(24), dp(12));
            pickBtn.setOnClickListener(v -> openFolderPicker());
            emptyBox.addView(pickBtn, pbLp);

            fileListContainer.addView(emptyBox);
            return;
        }

        String qLower = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        int shownCount = 0;

        for (FileItem item : allFiles) {
            boolean nameMatch = qLower.isEmpty() || item.displayName.toLowerCase(Locale.ROOT).contains(qLower);
            boolean contentMatch = false;

            if (!qLower.isEmpty()) {
                if (item.contentCache == null) {
                    item.contentCache = readTextFromUri(item.uri);
                }
                if (item.contentCache != null && item.contentCache.toLowerCase(Locale.ROOT).contains(qLower)) {
                    contentMatch = true;
                }
            }

            if (nameMatch || contentMatch) {
                fileListContainer.addView(createFileCard(item, qLower));
                shownCount++;
            }
        }

        if (shownCount == 0) {
            TextView emptyTv = new TextView(this);
            emptyTv.setText(qLower.isEmpty()
                    ? "لا توجد ملفات نصية حالياً.\nاضغط «＋ ملف جديد» بالأعلى للبدء."
                    : "لا توجد نتائج مطابقة لبحثك: «" + query + "»");
            emptyTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            emptyTv.setTextColor(COLOR_TEXT_SUB);
            emptyTv.setGravity(Gravity.CENTER);
            emptyTv.setPadding(dp(20), dp(48), dp(20), dp(48));
            emptyTv.setTypeface(getFontTypeface(fontIndex, false));
            fileListContainer.addView(emptyTv);
        }
    }

    private View createFileCard(final FileItem item, final String query) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(16), dp(14), dp(12), dp(14));

        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setColor(COLOR_CARD);
        cardBg.setCornerRadius(dp(16));
        cardBg.setStroke(dp(1), COLOR_DIVIDER);
        card.setBackground(cardBg);
        card.setElevation(dp(2));

        LinearLayout textCol = new LinearLayout(this);
        textCol.setOrientation(LinearLayout.VERTICAL);

        TextView titleTv = new TextView(this);
        titleTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        titleTv.setTextColor(COLOR_TEXT_MAIN);
        titleTv.setTypeface(getFontTypeface(fontIndex, true));
        titleTv.setMaxLines(1);
        titleTv.setEllipsize(TextUtils.TruncateAt.END);

        boolean isRtl = getTextDirection(item.displayName);
        titleTv.setGravity(isRtl ? (Gravity.RIGHT | Gravity.CENTER_VERTICAL) : (Gravity.LEFT | Gravity.CENTER_VERTICAL));

        if (query != null && !query.isEmpty() && item.displayName.toLowerCase(Locale.ROOT).contains(query)) {
            titleTv.setText(highlightOccurrences(item.displayName, query));
        } else {
            titleTv.setText(item.displayName);
        }
        textCol.addView(titleTv);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd • hh:mm a", new Locale("ar"));
        TextView dateTv = new TextView(this);
        dateTv.setText(item.lastModified > 0 ? sdf.format(new Date(item.lastModified)) : "ملف نصي TXT");
        dateTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        dateTv.setTextColor(COLOR_TEXT_SUB);
        dateTv.setPadding(0, dp(4), 0, 0);
        textCol.addView(dateTv);

        if (query != null && !query.isEmpty() && item.contentCache != null
                && item.contentCache.toLowerCase(Locale.ROOT).contains(query)) {
            TextView snippetTv = new TextView(this);
            snippetTv.setText(buildHighlightedSnippet(item.contentCache, query));
            snippetTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            snippetTv.setTextColor(COLOR_TEXT_MAIN);
            snippetTv.setMaxLines(2);
            snippetTv.setEllipsize(TextUtils.TruncateAt.END);
            snippetTv.setPadding(dp(8), dp(6), dp(8), dp(6));
            GradientDrawable snipBg = new GradientDrawable();
            snipBg.setColor(0xFFF8FAFC);
            snipBg.setCornerRadius(dp(8));
            snippetTv.setBackground(snipBg);
            LinearLayout.LayoutParams snLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            snLp.topMargin = dp(6);
            textCol.addView(snippetTv, snLp);
        }

        card.addView(textCol, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        FrameLayout moreBtn = new FrameLayout(this);
        GradientDrawable moreBg = new GradientDrawable();
        moreBg.setColor(themeAccent);
        moreBg.setCornerRadius(dp(12));
        moreBtn.setBackground(moreBg);

        ImageView moreIcon = new ImageView(this);
        Drawable d = getDrawableByName("ic_more_vert", themePrimary);
        if (d != null) moreIcon.setImageDrawable(d);
        FrameLayout.LayoutParams iconLp = new FrameLayout.LayoutParams(dp(22), dp(22), Gravity.CENTER);
        moreBtn.addView(moreIcon, iconLp);

        LinearLayout.LayoutParams moreLp = new LinearLayout.LayoutParams(dp(40), dp(40));
        moreLp.rightMargin = dp(8);
        moreBtn.setOnClickListener(v -> showFileOptionsMenu(moreBtn, item));
        card.addView(moreBtn, moreLp);

        card.setOnClickListener(v -> openFileInMode(item, Mode.READ));

        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardLp.bottomMargin = dp(10);
        card.setLayoutParams(cardLp);
        return card;
    }

    private CharSequence buildHighlightedSnippet(String content, String query) {
        String lower = content.toLowerCase(Locale.ROOT);
        String qLower = query.toLowerCase(Locale.ROOT);
        int idx = lower.indexOf(qLower);
        if (idx < 0) return "";
        int start = Math.max(0, idx - 30);
        int end = Math.min(content.length(), idx + query.length() + 45);
        String sub = content.substring(start, end).replace("\n", " ");
        if (start > 0) sub = "..." + sub;
        if (end < content.length()) sub = sub + "...";
        return highlightOccurrences(sub, query);
    }

    private SpannableString highlightOccurrences(String text, String query) {
        SpannableString span = new SpannableString(text);
        if (query == null || query.isEmpty()) return span;
        String lower = text.toLowerCase(Locale.ROOT);
        String qLower = query.toLowerCase(Locale.ROOT);
        int idx = 0;
        while ((idx = lower.indexOf(qLower, idx)) >= 0) {
            span.setSpan(new BackgroundColorSpan(COLOR_HIGHLIGHT),
                    idx, idx + qLower.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            span.setSpan(new ForegroundColorSpan(Color.BLACK),
                    idx, idx + qLower.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            idx += qLower.length();
        }
        return span;
    }

    private void showFileOptionsMenu(View anchor, final FileItem item) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenu().add(0, 1, 0, "📖 قراءة الملف");
        popup.getMenu().add(0, 2, 1, "✏️ تحرير الملف");
        popup.getMenu().add(0, 3, 2, "🏷️ إعادة تسمية");
        popup.getMenu().add(0, 4, 3, "📤 مشاركة الملف");
        popup.getMenu().add(0, 5, 4, "🗑️ حذف الملف");

        popup.setOnMenuItemClickListener(menuItem -> {
            switch (menuItem.getItemId()) {
                case 1: openFileInMode(item, Mode.READ); return true;
                case 2: openFileInMode(item, Mode.EDIT); return true;
                case 3: showRenameDialog(item, null); return true;
                case 4: shareFile(item); return true;
                case 5: confirmDeleteFile(item); return true;
            }
            return false;
        });
        popup.show();
    }

    private void showCreateFileDialog() {
        if (treeUri == null) {
            showToast("يرجى اختيار مجلد لحفظ الملفات أولاً");
            openFolderPicker();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20), dp(18), dp(20), dp(10));
        box.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        TextView title = new TextView(this);
        title.setText("إنشاء ملف نصي جديد");
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        title.setTextColor(themePrimary);
        title.setTypeface(getFontTypeface(fontIndex, true));
        box.addView(title);

        EditText input = new EditText(this);
        input.setHint("اكتب اسم الملف هنا...");
        input.setSingleLine(true);
        input.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        input.setTypeface(getFontTypeface(fontIndex, false));
        LinearLayout.LayoutParams inLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        inLp.topMargin = dp(12);
        box.addView(input, inLp);

        builder.setView(box);
        builder.setPositiveButton("إنشاء وتحرير", (d, w) -> {
            String raw = input.getText().toString().trim();
            if (raw.isEmpty()) {
                raw = "ملاحظة " + new SimpleDateFormat("yyyy-MM-dd HH-mm", Locale.US).format(new Date());
            }
            createNewFile(raw);
        });
        builder.setNegativeButton("إلغاء", null);
        builder.show();
    }

    private void createNewFile(String rawName) {
        try {
            String fileName = rawName.toLowerCase(Locale.ROOT).endsWith(".txt") ? rawName : (rawName + ".txt");
            String treeDocId = DocumentsContract.getTreeDocumentId(treeUri);
            Uri parentDocUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, treeDocId);
            Uri newUri = DocumentsContract.createDocument(getContentResolver(), parentDocUri, "text/plain", fileName);
            if (newUri != null) {
                writeTextToUri(newUri, "");
                loadFilesFromStorage();
                for (FileItem f : allFiles) {
                    if (newUri.equals(f.uri)) {
                        openFileInMode(f, Mode.EDIT);
                        return;
                    }
                }
                FileItem temp = new FileItem();
                temp.uri = newUri;
                temp.name = fileName;
                temp.displayName = rawName;
                temp.lastModified = System.currentTimeMillis();
                openFileInMode(temp, Mode.EDIT);
            } else {
                showToast("تعذر إنشاء الملف");
            }
        } catch (Exception e) {
            showToast("خطأ أثناء إنشاء الملف: " + e.getMessage());
        }
    }

    private void openFileInMode(FileItem item, Mode targetMode) {
        currentFile = item;
        currentMode = targetMode;
        undoStack.clear();
        redoStack.clear();
        isDirty = false;
        showDocumentScreen();
    }

    private void switchMode(Mode newMode, boolean saveFirst) {
        if (saveFirst && currentMode == Mode.EDIT) {
            saveCurrentFile(true);
        }
        hideKeyboard();
        currentMode = newMode;
        showDocumentScreen();
    }

    private void goHome() {
        if (currentMode == Mode.EDIT && isDirty) {
            saveCurrentFile(true);
        }
        showHome();
    }

    private void saveCurrentFile(boolean showToastMsg) {
        if (currentFile == null || editorView == null) return;
        String content = editorView.getText().toString();
        boolean ok = writeTextToUri(currentFile.uri, content);
        if (ok) {
            currentFile.contentCache = content;
            currentFile.lastModified = System.currentTimeMillis();
            isDirty = false;
            if (showToastMsg) showToast("تم حفظ التعديلات تلقائياً ✓");
        } else if (showToastMsg) {
            showToast("تعذر حفظ الملف!");
        }
    }

    private void showDocumentScreen() {
        if (currentFile == null) {
            showHome();
            return;
        }

        String initialContent = (editorView != null && currentFile.contentCache != null && isDirty)
                ? editorView.getText().toString()
                : readTextFromUri(currentFile.uri);
        if (initialContent == null) initialContent = "";
        currentFile.contentCache = initialContent;

        if (undoStack.isEmpty()) {
            undoStack.add(initialContent);
        }

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(COLOR_BG);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        // 1. Clean Top Bar: [ → رجوع ] | Document Title (tap to rename) | [ ＋ | 100% | － ]
        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        topBar.setPadding(dp(12), dp(10), dp(12), dp(10));
        topBar.setBackgroundColor(themePrimary);
        topBar.setElevation(dp(4));

        TextView backBtn = new TextView(this);
        backBtn.setText("→ رجوع");
        backBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        backBtn.setTextColor(Color.WHITE);
        backBtn.setTypeface(getFontTypeface(fontIndex, true));
        backBtn.setPadding(dp(12), dp(6), dp(12), dp(6));
        GradientDrawable backBg = new GradientDrawable();
        backBg.setColor(0x33FFFFFF);
        backBg.setCornerRadius(dp(10));
        backBtn.setBackground(backBg);
        backBtn.setOnClickListener(v -> {
            if (currentMode == Mode.EDIT) {
                switchMode(Mode.READ, true);
            } else {
                goHome();
            }
        });
        topBar.addView(backBtn);

        TextView titleTv = new TextView(this);
        titleTv.setText(currentFile.displayName);
        titleTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        titleTv.setTextColor(Color.WHITE);
        titleTv.setTypeface(getFontTypeface(fontIndex, true));
        titleTv.setSingleLine(true);
        titleTv.setEllipsize(TextUtils.TruncateAt.END);
        titleTv.setGravity(Gravity.CENTER);
        titleTv.setPadding(dp(10), dp(4), dp(10), dp(4));
        titleTv.setOnClickListener(v -> showRenameDialog(currentFile, titleTv));
        topBar.addView(titleTv, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        // Zoom Capsule [ ＋ | 100% | － ]
        LinearLayout zoomCapsule = new LinearLayout(this);
        zoomCapsule.setOrientation(LinearLayout.HORIZONTAL);
        zoomCapsule.setGravity(Gravity.CENTER_VERTICAL);
        GradientDrawable zoomBg = new GradientDrawable();
        zoomBg.setColor(0x28FFFFFF);
        zoomBg.setCornerRadius(dp(10));
        zoomCapsule.setBackground(zoomBg);

        TextView zoomInBtn = new TextView(this);
        zoomInBtn.setText("＋");
        zoomInBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        zoomInBtn.setTextColor(Color.WHITE);
        zoomInBtn.setTypeface(Typeface.DEFAULT_BOLD);
        zoomInBtn.setPadding(dp(8), dp(4), dp(8), dp(4));
        zoomInBtn.setOnClickListener(v -> {
            if (zoomPercent < 250) {
                zoomPercent += 5;
                prefs.edit().putInt(KEY_ZOOM, zoomPercent).apply();
                applyZoom();
            }
        });

        zoomLabelView = new TextView(this);
        zoomLabelView.setText(zoomPercent + "%");
        zoomLabelView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        zoomLabelView.setTextColor(Color.WHITE);
        zoomLabelView.setTypeface(Typeface.DEFAULT_BOLD);
        zoomLabelView.setPadding(dp(4), dp(4), dp(4), dp(4));
        zoomLabelView.setOnClickListener(v -> {
            zoomPercent = 100;
            prefs.edit().putInt(KEY_ZOOM, zoomPercent).apply();
            applyZoom();
        });

        TextView zoomOutBtn = new TextView(this);
        zoomOutBtn.setText("－");
        zoomOutBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        zoomOutBtn.setTextColor(Color.WHITE);
        zoomOutBtn.setTypeface(Typeface.DEFAULT_BOLD);
        zoomOutBtn.setPadding(dp(8), dp(4), dp(8), dp(4));
        zoomOutBtn.setOnClickListener(v -> {
            if (zoomPercent > 50) {
                zoomPercent -= 5;
                prefs.edit().putInt(KEY_ZOOM, zoomPercent).apply();
                applyZoom();
            }
        });

        zoomCapsule.addView(zoomInBtn);
        zoomCapsule.addView(zoomLabelView);
        zoomCapsule.addView(zoomOutBtn);
        topBar.addView(zoomCapsule);

        root.addView(topBar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // 2. Editor Toolbar (Visible in EDIT mode)
        if (currentMode == Mode.EDIT) {
            HorizontalScrollView toolbarScroll = new HorizontalScrollView(this);
            toolbarScroll.setHorizontalScrollBarEnabled(false);
            toolbarScroll.setBackgroundColor(COLOR_CARD);
            toolbarScroll.setElevation(dp(2));

            LinearLayout toolbarRow = new LinearLayout(this);
            toolbarRow.setOrientation(LinearLayout.HORIZONTAL);
            toolbarRow.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
            toolbarRow.setGravity(Gravity.CENTER_VERTICAL);
            toolbarRow.setPadding(dp(10), dp(8), dp(10), dp(8));

            List<String> ordered = getOrderedTools();
            Set<String> enabled = getEnabledTools();
            for (String toolId : ordered) {
                if (enabled.contains(toolId)) {
                    toolbarRow.addView(buildToolButton(toolId));
                }
            }

            toolbarScroll.addView(toolbarRow);
            root.addView(toolbarScroll, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            View div = new View(this);
            div.setBackgroundColor(COLOR_DIVIDER);
            root.addView(div, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1)));
        }

        // 3. Main Document Area
        ScrollView docScroll = new ScrollView(this);
        docScroll.setFillViewport(true);
        docScroll.setBackgroundColor(COLOR_CARD);

        editorView = new EditText(this);
        editorView.setBackground(null);
        editorView.setTextColor(COLOR_TEXT_MAIN);
        editorView.setPadding(dp(18), dp(18), dp(18), dp(28));
        editorView.setGravity(Gravity.TOP | Gravity.START);
        editorView.setLineSpacing(dp(6), 1.15f);
        editorView.setTypeface(getFontTypeface(fontIndex, false));

        isUndoRedoInProgress = true;
        editorView.setText(initialContent);
        isUndoRedoInProgress = false;

        if (currentMode == Mode.READ) {
            editorView.setKeyListener(null);
            editorView.setFocusable(true);
            editorView.setFocusableInTouchMode(true);
            editorView.setCursorVisible(false);
            editorView.setTextIsSelectable(true);
        } else {
            editorView.setInputType(InputType.TYPE_CLASS_TEXT
                    | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                    | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
            editorView.setFocusable(true);
            editorView.setFocusableInTouchMode(true);
            editorView.setCursorVisible(true);
        }

        editorView.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                String current = s.toString();
                updateStats(current);
                if (!isUndoRedoInProgress && currentMode == Mode.EDIT) {
                    isDirty = true;
                    if (undoStack.isEmpty() || !undoStack.get(undoStack.size() - 1).equals(current)) {
                        undoStack.add(current);
                        if (undoStack.size() > 80) undoStack.remove(0);
                        redoStack.clear();
                    }
                }
            }
        });

        docScroll.addView(editorView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(docScroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        // 4. Bottom Stats Bar (Words, Characters, Lines) - Always at bottom
        View statsDiv = new View(this);
        statsDiv.setBackgroundColor(COLOR_DIVIDER);
        root.addView(statsDiv, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1)));

        statsTextView = new TextView(this);
        statsTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        statsTextView.setTextColor(COLOR_TEXT_SUB);
        statsTextView.setBackgroundColor(0xFFF8FAFC);
        statsTextView.setGravity(Gravity.CENTER);
        statsTextView.setPadding(dp(12), dp(6), dp(12), dp(6));
        statsTextView.setTypeface(getFontTypeface(fontIndex, false));
        root.addView(statsTextView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // 5. Bottom Read Action Bar (Edit + Share buttons in READ mode)
        if (currentMode == Mode.READ) {
            LinearLayout readBottomBar = new LinearLayout(this);
            readBottomBar.setOrientation(LinearLayout.HORIZONTAL);
            readBottomBar.setGravity(Gravity.CENTER);
            readBottomBar.setPadding(dp(16), dp(10), dp(16), dp(10));
            readBottomBar.setBackgroundColor(COLOR_CARD);
            readBottomBar.setElevation(dp(8));

            Button editFileBtn = makePrimaryButton("✏️ تحرير الملف");
            editFileBtn.setOnClickListener(v -> switchMode(Mode.EDIT, false));

            View space = new View(this);

            Button shareFileBtn = makeSecondaryButton("📤 مشاركة الملف");
            shareFileBtn.setOnClickListener(v -> {
                if (currentFile != null) shareFile(currentFile);
            });

            readBottomBar.addView(editFileBtn, new LinearLayout.LayoutParams(0, dp(46), 1.2f));
            readBottomBar.addView(space, new LinearLayout.LayoutParams(dp(12), dp(1)));
            readBottomBar.addView(shareFileBtn, new LinearLayout.LayoutParams(0, dp(46), 1f));

            root.addView(readBottomBar, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }

        setContentView(root);
        applyZoom();
        updateStats(initialContent);

        if (currentMode == Mode.READ && !currentSearchQuery.isEmpty()) {
            new Handler(Looper.getMainLooper()).postDelayed(this::performSearchAndHighlight, 120);
        }
    }

    private View buildToolButton(final String toolId) {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(themeAccent);
        bg.setCornerRadius(dp(10));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(38));
        lp.leftMargin = dp(6);

        if (toolbarIconsOnly) {
            FrameLayout box = new FrameLayout(this);
            box.setBackground(bg);
            box.setPadding(dp(10), dp(7), dp(10), dp(7));
            box.setContentDescription(getToolLabel(toolId));

            ImageView iv = new ImageView(this);
            Drawable icon = getDrawableByName(getToolIconResName(toolId), themePrimary);
            if (icon != null) iv.setImageDrawable(icon);
            FrameLayout.LayoutParams ivLp = new FrameLayout.LayoutParams(dp(22), dp(22), Gravity.CENTER);
            box.addView(iv, ivLp);

            box.setOnClickListener(v -> executeTool(toolId));
            box.setOnLongClickListener(v -> {
                showToast(getToolLabel(toolId));
                return true;
            });
            box.setLayoutParams(lp);
            return box;
        } else {
            TextView tv = new TextView(this);
            tv.setText(getToolLabel(toolId));
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            tv.setTextColor(themePrimary);
            tv.setTypeface(getFontTypeface(fontIndex, true));
            tv.setGravity(Gravity.CENTER);
            tv.setPadding(dp(14), dp(6), dp(14), dp(6));
            tv.setBackground(bg);
            tv.setOnClickListener(v -> executeTool(toolId));
            tv.setLayoutParams(lp);
            return tv;
        }
    }

    private void executeTool(String toolId) {
        if (editorView == null) return;
        Editable text = editorView.getText();
        int start = Math.max(0, editorView.getSelectionStart());
        int end = Math.max(0, editorView.getSelectionEnd());
        if (start > end) {
            int tmp = start;
            start = end;
            end = tmp;
        }

        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);

        switch (toolId) {
            case "undo":
                if (undoStack.size() > 1) {
                    isUndoRedoInProgress = true;
                    String current = undoStack.remove(undoStack.size() - 1);
                    redoStack.add(current);
                    String prev = undoStack.get(undoStack.size() - 1);
                    editorView.setText(prev);
                    editorView.setSelection(prev.length());
                    isUndoRedoInProgress = false;
                    isDirty = true;
                } else {
                    showToast("لا يوجد ما يمكن التراجع عنه");
                }
                break;

            case "redo":
                if (!redoStack.isEmpty()) {
                    isUndoRedoInProgress = true;
                    String next = redoStack.remove(redoStack.size() - 1);
                    undoStack.add(next);
                    editorView.setText(next);
                    editorView.setSelection(next.length());
                    isUndoRedoInProgress = false;
                    isDirty = true;
                } else {
                    showToast("لا يوجد ما يمكن إعادته");
                }
                break;

            case "copy":
                if (start == end) {
                    showToast("حدد نصاً أولاً لنسخه");
                } else if (cm != null) {
                    cm.setPrimaryClip(ClipData.newPlainText("text", text.subSequence(start, end)));
                    showToast("تم النسخ");
                }
                break;

            case "cut":
                if (start == end) {
                    showToast("حدد نصاً أولاً لقصه");
                } else if (cm != null) {
                    cm.setPrimaryClip(ClipData.newPlainText("text", text.subSequence(start, end)));
                    text.delete(start, end);
                    showToast("تم القص");
                }
                break;

            case "paste":
                if (cm != null && cm.hasPrimaryClip() && cm.getPrimaryClip() != null
                        && cm.getPrimaryClip().getItemCount() > 0) {
                    CharSequence clip = cm.getPrimaryClip().getItemAt(0).coerceToText(this);
                    if (clip != null) {
                        text.replace(start, end, clip);
                    }
                } else {
                    showToast("الحافظة فارغة");
                }
                break;

            case "select_all":
                editorView.requestFocus();
                editorView.selectAll();
                break;

            case "select_up":
                editorView.requestFocus();
                Selection.setSelection(text, end, 0);
                break;

            case "select_down":
                editorView.requestFocus();
                Selection.setSelection(text, start, text.length());
                break;

            case "auto_format":
                autoFormatCurrentText();
                break;

            case "signature": {
                String sig = prefs.getString(KEY_SIGNATURE, "");
                if (sig == null || sig.trim().isEmpty()) {
                    promptAndInsertSignature();
                } else {
                    String prefix = (start > 0 && text.charAt(start - 1) != '\n') ? "\n\n" : "";
                    text.replace(start, end, prefix + sig.trim() + "\n");
                }
                break;
            }

            case "datetime": {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd - hh:mm a", new Locale("ar"));
                String dt = sdf.format(new Date());
                text.replace(start, end, dt);
                break;
            }
        }
    }

    private void promptAndInsertSignature() {
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20), dp(18), dp(20), dp(8));
        box.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        TextView t = new TextView(this);
        t.setText("إعداد التوقيع الشخصي");
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        t.setTextColor(themePrimary);
        t.setTypeface(getFontTypeface(fontIndex, true));
        box.addView(t);

        EditText input = new EditText(this);
        input.setHint(SIGNATURE_HINT);
        input.setMinLines(2);
        input.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        input.setTypeface(getFontTypeface(fontIndex, false));
        box.addView(input);

        b.setView(box);
        b.setPositiveButton("حفظ وإدراج", (d, w) -> {
            String val = input.getText().toString().trim();
            prefs.edit().putString(KEY_SIGNATURE, val).apply();
            if (!val.isEmpty() && editorView != null) {
                int s = Math.max(0, editorView.getSelectionStart());
                Editable ed = editorView.getText();
                String prefix = (s > 0 && ed.charAt(s - 1) != '\n') ? "\n\n" : "";
                ed.insert(s, prefix + val + "\n");
            }
        });
        b.setNegativeButton("إلغاء", null);
        b.show();
    }

    private void showCustomizeToolbarDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        final List<String> tempOrder = new ArrayList<>(getOrderedTools());
        final Set<String> tempEnabled = new HashSet<>(getEnabledTools());

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(18), dp(18), dp(16));
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        GradientDrawable rootBg = new GradientDrawable();
        rootBg.setColor(COLOR_CARD);
        rootBg.setCornerRadius(dp(20));
        root.setBackground(rootBg);

        TextView title = new TextView(this);
        title.setText("🛠 تخصيص وترتيب أدوات التحرير");
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        title.setTextColor(themePrimary);
        title.setTypeface(getFontTypeface(fontIndex, true));
        root.addView(title);

        TextView sub = new TextView(this);
        sub.setText("اسحب من رمز الخطوط (≡) للأعلى أو للأسفل لترتيب الأدوات (الأعلى يظهر أولاً من اليمين):");
        sub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        sub.setTextColor(COLOR_TEXT_SUB);
        sub.setPadding(0, dp(4), 0, dp(10));
        sub.setTypeface(getFontTypeface(fontIndex, false));
        root.addView(sub);

        LinearLayout topActions = new LinearLayout(this);
        topActions.setOrientation(LinearLayout.HORIZONTAL);
        topActions.setGravity(Gravity.CENTER_VERTICAL);
        topActions.setPadding(0, 0, 0, dp(10));

        Button toggleAllBtn = makeSecondaryButton("تحديد / إلغاء الكل");
        toggleAllBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);

        View topSpace = new View(this);

        Button resetBtn = makeSecondaryButton("↺ الافتراضي");
        resetBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);

        topActions.addView(toggleAllBtn, new LinearLayout.LayoutParams(0, dp(38), 1.3f));
        topActions.addView(topSpace, new LinearLayout.LayoutParams(dp(8), dp(1)));
        topActions.addView(resetBtn, new LinearLayout.LayoutParams(0, dp(38), 1f));
        root.addView(topActions);

        final ScrollView scrollView = new ScrollView(this);
        final LinearLayout listContainer = new LinearLayout(this);
        listContainer.setOrientation(LinearLayout.VERTICAL);
        scrollView.addView(listContainer);

        LinearLayout.LayoutParams scLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(350));
        root.addView(scrollView, scLp);

        final Runnable[] rebuildListRef = new Runnable[1];
        rebuildListRef[0] = () -> {
            listContainer.removeAllViews();
            final int count = tempOrder.size();
            final View[] rowViews = new View[count];

            for (int idx = 0; idx < count; idx++) {
                final int startIndex = idx;
                final String toolId = tempOrder.get(idx);

                final LinearLayout row = new LinearLayout(MainActivity.this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setPadding(dp(8), dp(6), dp(8), dp(6));

                GradientDrawable rowBg = new GradientDrawable();
                rowBg.setColor(0xFFF8FAFC);
                rowBg.setCornerRadius(dp(12));
                rowBg.setStroke(dp(1), COLOR_DIVIDER);
                row.setBackground(rowBg);

                FrameLayout handleWrap = new FrameLayout(MainActivity.this);
                handleWrap.setPadding(dp(10), dp(10), dp(10), dp(10));
                ImageView handleIcon = new ImageView(MainActivity.this);
                Drawable hd = getDrawableByName("ic_drag_handle", COLOR_TEXT_SUB);
                if (hd != null) handleIcon.setImageDrawable(hd);
                handleWrap.addView(handleIcon, new FrameLayout.LayoutParams(dp(22), dp(22), Gravity.CENTER));
                row.addView(handleWrap);

                FrameLayout iconBadge = new FrameLayout(MainActivity.this);
                GradientDrawable ibBg = new GradientDrawable();
                ibBg.setColor(themeAccent);
                ibBg.setCornerRadius(dp(10));
                iconBadge.setBackground(ibBg);
                ImageView toolIcon = new ImageView(MainActivity.this);
                Drawable td = getDrawableByName(getToolIconResName(toolId), themePrimary);
                if (td != null) toolIcon.setImageDrawable(td);
                iconBadge.addView(toolIcon, new FrameLayout.LayoutParams(dp(20), dp(20), Gravity.CENTER));
                LinearLayout.LayoutParams ibLp = new LinearLayout.LayoutParams(dp(36), dp(36));
                ibLp.rightMargin = dp(4);
                ibLp.leftMargin = dp(10);
                row.addView(iconBadge, ibLp);

                TextView labelTv = new TextView(MainActivity.this);
                labelTv.setText(getToolLabel(toolId));
                labelTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
                labelTv.setTextColor(COLOR_TEXT_MAIN);
                labelTv.setTypeface(getFontTypeface(fontIndex, true));
                row.addView(labelTv, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

                final CheckBox cb = new CheckBox(MainActivity.this);
                cb.setChecked(tempEnabled.contains(toolId));
                cb.setButtonTintList(ColorStateList.valueOf(themePrimary));
                cb.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (isChecked) tempEnabled.add(toolId);
                    else tempEnabled.remove(toolId);
                });
                row.addView(cb);

                row.setOnClickListener(v -> cb.setChecked(!cb.isChecked()));

                handleWrap.setOnTouchListener(new View.OnTouchListener() {
                    int dragTargetIndex = startIndex;
                    float downRawY = 0f;
                    int rowStepPx = dp(56);

                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        switch (event.getActionMasked()) {
                            case MotionEvent.ACTION_DOWN:
                                scrollView.requestDisallowInterceptTouchEvent(true);
                                downRawY = event.getRawY();
                                dragTargetIndex = startIndex;
                                if (row.getHeight() > 0) rowStepPx = row.getHeight() + dp(8);
                                row.bringToFront();
                                row.setElevation(dp(8));
                                row.setAlpha(0.93f);
                                return true;

                            case MotionEvent.ACTION_MOVE: {
                                float dy = event.getRawY() - downRawY;
                                float minDy = -startIndex * (float) rowStepPx;
                                float maxDy = (count - 1 - startIndex) * (float) rowStepPx;
                                if (dy < minDy) dy = minDy;
                                if (dy > maxDy) dy = maxDy;
                                row.setTranslationY(dy);

                                int offsetSlots = Math.round(dy / rowStepPx);
                                int newTarget = Math.max(0, Math.min(count - 1, startIndex + offsetSlots));
                                if (newTarget != dragTargetIndex) {
                                    dragTargetIndex = newTarget;
                                    for (int i = 0; i < count; i++) {
                                        if (i == startIndex) continue;
                                        float shift = 0f;
                                        if (startIndex < dragTargetIndex && i > startIndex && i <= dragTargetIndex) {
                                            shift = -rowStepPx;
                                        } else if (startIndex > dragTargetIndex && i >= dragTargetIndex && i < startIndex) {
                                            shift = rowStepPx;
                                        }
                                        rowViews[i].animate().translationY(shift).setDuration(110).start();
                                    }
                                }
                                return true;
                            }

                            case MotionEvent.ACTION_UP:
                            case MotionEvent.ACTION_CANCEL:
                                scrollView.requestDisallowInterceptTouchEvent(false);
                                row.setElevation(0f);
                                row.setAlpha(1f);
                                if (dragTargetIndex != startIndex) {
                                    String moved = tempOrder.remove(startIndex);
                                    tempOrder.add(dragTargetIndex, moved);
                                }
                                rebuildListRef[0].run();
                                return true;
                        }
                        return false;
                    }
                });

                LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                rowLp.bottomMargin = dp(8);
                listContainer.addView(row, rowLp);
                rowViews[idx] = row;
            }
        };

        toggleAllBtn.setOnClickListener(v -> {
            if (tempEnabled.size() == TOOL_IDS.length) {
                tempEnabled.clear();
            } else {
                tempEnabled.addAll(Arrays.asList(TOOL_IDS));
            }
            rebuildListRef[0].run();
        });

        resetBtn.setOnClickListener(v -> {
            tempOrder.clear();
            tempOrder.addAll(Arrays.asList(TOOL_IDS));
            tempEnabled.clear();
            tempEnabled.addAll(Arrays.asList(DEFAULT_ENABLED_TOOLS));
            rebuildListRef[0].run();
        });

        rebuildListRef[0].run();

        LinearLayout bottomRow = new LinearLayout(this);
        bottomRow.setOrientation(LinearLayout.HORIZONTAL);
        bottomRow.setGravity(Gravity.CENTER);
        bottomRow.setPadding(0, dp(12), 0, 0);

        Button cancelBtn = makeSecondaryButton("إلغاء");
        cancelBtn.setOnClickListener(v -> dialog.dismiss());

        View bSpace = new View(this);

        Button saveBtn = makePrimaryButton("حفظ الترتيب");
        saveBtn.setOnClickListener(v -> {
            saveToolbarConfig(tempOrder, tempEnabled);
            dialog.dismiss();
            if (currentMode != Mode.HOME) {
                showDocumentScreen();
            }
            showToast("تم حفظ ترتيب وأدوات التحرير ✓");
        });

        bottomRow.addView(cancelBtn, new LinearLayout.LayoutParams(0, dp(44), 1f));
        bottomRow.addView(bSpace, new LinearLayout.LayoutParams(dp(10), dp(1)));
        bottomRow.addView(saveBtn, new LinearLayout.LayoutParams(0, dp(44), 1.2f));
        root.addView(bottomRow);

        dialog.setContentView(root);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.93),
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        dialog.show();
    }

    private void applyZoom() {
        if (editorView != null) {
            float baseSp = 18f;
            float scaledSp = baseSp * (zoomPercent / 100f);
            editorView.setTextSize(TypedValue.COMPLEX_UNIT_SP, scaledSp);
        }
        if (zoomLabelView != null) {
            zoomLabelView.setText(zoomPercent + "%");
        }
    }

    private void performSearchAndHighlight() {
        if (editorView == null || currentSearchQuery == null || currentSearchQuery.isEmpty()) return;
        String fullText = editorView.getText().toString();
        String lower = fullText.toLowerCase(Locale.ROOT);
        String qLower = currentSearchQuery.toLowerCase(Locale.ROOT);

        int firstIdx = lower.indexOf(qLower);
        if (firstIdx < 0) return;

        Spannable span = editorView.getText();
        int idx = 0;
        while ((idx = lower.indexOf(qLower, idx)) >= 0) {
            span.setSpan(new BackgroundColorSpan(COLOR_HIGHLIGHT),
                    idx, idx + qLower.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            span.setSpan(new ForegroundColorSpan(Color.BLACK),
                    idx, idx + qLower.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            idx += qLower.length();
        }
        editorView.requestFocus();
        editorView.setSelection(firstIdx, Math.min(fullText.length(), firstIdx + qLower.length()));
    }

    private void updateStats(String text) {
        if (statsTextView == null) return;
        if (text == null || text.isEmpty()) {
            statsTextView.setText("الكلمات: 0  •  الحروف: 0  •  الأسطر: 1");
            return;
        }
        int chars = text.length();
        String trimmed = text.trim();
        int words = trimmed.isEmpty() ? 0 : trimmed.split("\\s+").length;
        int lines = 1;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '\n') lines++;
        }
        statsTextView.setText("الكلمات: " + words + "  •  الحروف: " + chars + "  •  الأسطر: " + lines);
    }

    private void autoFormatCurrentText() {
        if (editorView == null) return;
        String current = editorView.getText().toString();
        if (current.trim().isEmpty()) {
            showToast("النص فارغ");
            return;
        }
        String formatted = autoFormatArabicText(current);
        editorView.setText(formatted);
        editorView.setSelection(formatted.length());
        showToast("تم تنسيق النص آلياً ✨");
    }

    private String autoFormatArabicText(String input) {
        String text = input.replace("\r\n", "\n").replace("\r", "\n");
        String[] lines = text.split("\n");
        StringBuilder out = new StringBuilder();
        int blankCount = 0;

        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.isEmpty()) {
                blankCount++;
                if (blankCount <= 1 && out.length() > 0) {
                    out.append("\n");
                }
                continue;
            }
            blankCount = 0;

            line = line.replaceAll("[ \\t]+", " ");
            line = line.replaceAll("\\s+([،؛؟!.,:])", "$1");
            line = line.replaceAll("([،؛؟!.,:])([^\\s\\n\"'»)\\]])", "$1 $2");
            line = line.replaceAll("(\\s|^)و\\s+", "$1و");

            if (out.length() > 0 && out.charAt(out.length() - 1) != '\n') {
                out.append("\n");
            }
            out.append(line).append("\n");
        }
        return out.toString().trim();
    }

    private void showRenameDialog(final FileItem item, final TextView titleViewToUpdate) {
        if (item == null) return;
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20), dp(18), dp(20), dp(8));
        box.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        TextView t = new TextView(this);
        t.setText("إعادة تسمية الملف");
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        t.setTextColor(themePrimary);
        t.setTypeface(getFontTypeface(fontIndex, true));
        box.addView(t);

        EditText input = new EditText(this);
        input.setText(item.displayName);
        input.setSingleLine(true);
        input.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        input.setTypeface(getFontTypeface(fontIndex, false));
        input.setSelection(item.displayName.length());
        LinearLayout.LayoutParams inLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        inLp.topMargin = dp(10);
        box.addView(input, inLp);

        b.setView(box);
        b.setPositiveButton("حفظ الاسم", (d, w) -> {
            String newRaw = input.getText().toString().trim();
            if (newRaw.isEmpty() || newRaw.equals(item.displayName)) return;
            String newFileName = newRaw.toLowerCase(Locale.ROOT).endsWith(".txt") ? newRaw : (newRaw + ".txt");
            try {
                Uri renamedUri = DocumentsContract.renameDocument(getContentResolver(), item.uri, newFileName);
                if (renamedUri != null) {
                    item.uri = renamedUri;
                    item.name = newFileName;
                    item.displayName = newRaw.toLowerCase(Locale.ROOT).endsWith(".txt")
                            ? newRaw.substring(0, newRaw.length() - 4) : newRaw;
                    if (titleViewToUpdate != null) {
                        titleViewToUpdate.setText(item.displayName);
                    }
                    if (currentMode == Mode.HOME) {
                        loadFilesFromStorage();
                        refreshHomeList(currentSearchQuery);
                    }
                    showToast("تم تغيير اسم الملف ✓");
                }
            } catch (Exception e) {
                showToast("تعذر تغيير الاسم");
            }
        });
        b.setNegativeButton("إلغاء", null);
        b.show();
    }

    private void confirmDeleteFile(final FileItem item) {
        new AlertDialog.Builder(this)
                .setTitle("حذف الملف")
                .setMessage("هل أنت متأكد من حذف الملف «" + item.displayName + "» نهائياً؟")
                .setPositiveButton("حذف", (d, w) -> {
                    try {
                        boolean deleted = DocumentsContract.deleteDocument(getContentResolver(), item.uri);
                        if (deleted) {
                            showToast("تم حذف الملف");
                            loadFilesFromStorage();
                            refreshHomeList(currentSearchQuery);
                        }
                    } catch (Exception e) {
                        showToast("تعذر حذف الملف");
                    }
                })
                .setNegativeButton("إلغاء", null)
                .show();
    }

    private void shareFile(FileItem item) {
        String content = (currentFile == item && editorView != null && currentMode != Mode.HOME)
                ? editorView.getText().toString()
                : readTextFromUri(item.uri);
        if (content == null) content = "";
        Intent sendIntent = new Intent(Intent.ACTION_SEND);
        sendIntent.setType("text/plain");
        sendIntent.putExtra(Intent.EXTRA_SUBJECT, item.displayName);
        sendIntent.putExtra(Intent.EXTRA_TEXT, content);
        startActivity(Intent.createChooser(sendIntent, "مشاركة «" + item.displayName + "» عبر:"));
    }

    private void createZipBackup() {
        if (treeUri == null || allFiles.isEmpty()) {
            showToast("لا توجد ملفات لعمل نسخة احتياطية");
            return;
        }
        String stamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.US).format(new Date());
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/zip");
        intent.putExtra(Intent.EXTRA_TITLE, "Mazwd_Backup_" + stamp + ".zip");
        startActivityForResult(intent, REQ_CREATE_ZIP);
    }

    private void showSettingsDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(20), dp(20), dp(18));
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(COLOR_CARD);
        bg.setCornerRadius(dp(22));
        root.setBackground(bg);

        TextView title = new TextView(this);
        title.setText("⚙ إعدادات المفكرة");
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        title.setTextColor(themePrimary);
        title.setTypeface(getFontTypeface(fontIndex, true));
        title.setPadding(0, 0, 0, dp(14));
        root.addView(title);

        // 1. Color Themes
        TextView themeLabel = new TextView(this);
        themeLabel.setText("🎨 لون المظهر:");
        themeLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        themeLabel.setTextColor(COLOR_TEXT_MAIN);
        themeLabel.setTypeface(getFontTypeface(fontIndex, true));
        root.addView(themeLabel);

        final int[] selectedTheme = {themeIndex};
        LinearLayout themeContainer = new LinearLayout(this);
        themeContainer.setOrientation(LinearLayout.VERTICAL);
        themeContainer.setPadding(0, dp(8), 0, dp(14));

        final List<TextView> themeChips = new ArrayList<>();
        for (int rowIdx = 0; rowIdx < 4; rowIdx++) {
            LinearLayout r = new LinearLayout(this);
            r.setOrientation(LinearLayout.HORIZONTAL);
            for (int col = 0; col < 2; col++) {
                final int idx = rowIdx * 2 + col;
                final TextView chip = new TextView(this);
                chip.setText(THEME_NAMES[idx]);
                chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
                chip.setGravity(Gravity.CENTER);
                chip.setPadding(dp(8), dp(10), dp(8), dp(10));
                chip.setTypeface(getFontTypeface(fontIndex, true));

                LinearLayout.LayoutParams cLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                cLp.setMargins(dp(4), dp(4), dp(4), dp(4));
                r.addView(chip, cLp);
                themeChips.add(chip);

                chip.setOnClickListener(v -> {
                    selectedTheme[0] = idx;
                    for (int k = 0; k < themeChips.size(); k++) {
                        TextView tc = themeChips.get(k);
                        GradientDrawable g = new GradientDrawable();
                        g.setCornerRadius(dp(10));
                        if (k == selectedTheme[0]) {
                            g.setColor(THEME_PRIMARY[k]);
                            tc.setTextColor(Color.WHITE);
                        } else {
                            g.setColor(THEME_ACCENT[k]);
                            g.setStroke(dp(1), THEME_PRIMARY[k]);
                            tc.setTextColor(THEME_PRIMARY[k]);
                        }
                        tc.setBackground(g);
                    }
                });
            }
            themeContainer.addView(r);
        }
        // Initialize chip styles
        for (int k = 0; k < themeChips.size(); k++) {
            TextView tc = themeChips.get(k);
            GradientDrawable g = new GradientDrawable();
            g.setCornerRadius(dp(10));
            if (k == selectedTheme[0]) {
                g.setColor(THEME_PRIMARY[k]);
                tc.setTextColor(Color.WHITE);
            } else {
                g.setColor(THEME_ACCENT[k]);
                g.setStroke(dp(1), THEME_PRIMARY[k]);
                tc.setTextColor(THEME_PRIMARY[k]);
            }
            tc.setBackground(g);
        }
        root.addView(themeContainer);

        // 2. Font Selection Spinner (with full vertical padding so Amiri never clips)
        TextView fontLabel = new TextView(this);
        fontLabel.setText("🔤 نوع الخط:");
        fontLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        fontLabel.setTextColor(COLOR_TEXT_MAIN);
        fontLabel.setTypeface(getFontTypeface(fontIndex, true));
        root.addView(fontLabel);

        final Spinner fontSpin = new Spinner(this);
        GradientDrawable spinBg = new GradientDrawable();
        spinBg.setColor(0xFFF8FAFC);
        spinBg.setCornerRadius(dp(12));
        spinBg.setStroke(dp(1), COLOR_DIVIDER);
        fontSpin.setBackground(spinBg);

        ArrayAdapter<String> fontAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, FONT_NAMES) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView tv = (TextView) super.getView(position, convertView, parent);
                tv.setTextColor(COLOR_TEXT_MAIN);
                tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
                tv.setTypeface(getFontTypeface(position, false));
                tv.setIncludeFontPadding(true);
                tv.setSingleLine(false);
                tv.setPadding(dp(12), dp(12), dp(12), dp(12));
                return tv;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                TextView tv = (TextView) super.getDropDownView(position, convertView, parent);
                tv.setTextColor(COLOR_TEXT_MAIN);
                tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
                tv.setTypeface(getFontTypeface(position, false));
                tv.setIncludeFontPadding(true);
                tv.setSingleLine(false);
                tv.setMinHeight(dp(54));
                tv.setGravity(Gravity.CENTER_VERTICAL | Gravity.RIGHT);
                tv.setPadding(dp(16), dp(14), dp(16), dp(14));
                tv.setLayoutParams(new AbsListView.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                return tv;
            }
        };
        fontSpin.setAdapter(fontAdapter);
        fontSpin.setSelection(fontIndex);
        LinearLayout.LayoutParams spLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        spLp.topMargin = dp(6);
        spLp.bottomMargin = dp(14);
        root.addView(fontSpin, spLp);

        // 3. Toolbar Display Style (Text vs Icons) + Customize Button
        TextView tbLabel = new TextView(this);
        tbLabel.setText("🛠 شريط أدوات التحرير:");
        tbLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        tbLabel.setTextColor(COLOR_TEXT_MAIN);
        tbLabel.setTypeface(getFontTypeface(fontIndex, true));
        root.addView(tbLabel);

        RadioGroup modeGroup = new RadioGroup(this);
        modeGroup.setOrientation(RadioGroup.VERTICAL);
        modeGroup.setPadding(0, dp(4), 0, dp(8));

        final RadioButton rbText = new RadioButton(this);
        rbText.setText("عرض كنصوص (قص، نسخ، تنسيق آلي...)");
        rbText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        rbText.setTextColor(COLOR_TEXT_MAIN);
        rbText.setButtonTintList(ColorStateList.valueOf(themePrimary));
        rbText.setTypeface(getFontTypeface(fontIndex, false));

        final RadioButton rbIcons = new RadioButton(this);
        rbIcons.setText("عرض كرموز (أيقونات حديثة)");
        rbIcons.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        rbIcons.setTextColor(COLOR_TEXT_MAIN);
        rbIcons.setButtonTintList(ColorStateList.valueOf(themePrimary));
        rbIcons.setTypeface(getFontTypeface(fontIndex, false));

        modeGroup.addView(rbText);
        modeGroup.addView(rbIcons);
        if (toolbarIconsOnly) rbIcons.setChecked(true);
        else rbText.setChecked(true);
        root.addView(modeGroup);

        Button customizeToolbarBtn = makeSecondaryButton("🛠 تخصيص وترتيب أدوات التحرير");
        customizeToolbarBtn.setOnClickListener(v -> showCustomizeToolbarDialog());
        LinearLayout.LayoutParams ctLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(44));
        ctLp.bottomMargin = dp(14);
        root.addView(customizeToolbarBtn, ctLp);

        // 4. Custom Signature Field
        TextView sigLabel = new TextView(this);
        sigLabel.setText("✒️ التوقيع الشخصي:");
        sigLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        sigLabel.setTextColor(COLOR_TEXT_MAIN);
        sigLabel.setTypeface(getFontTypeface(fontIndex, true));
        root.addView(sigLabel);

        final EditText sigInput = new EditText(this);
        sigInput.setText(prefs.getString(KEY_SIGNATURE, ""));
        sigInput.setHint(SIGNATURE_HINT);
        sigInput.setHintTextColor(COLOR_TEXT_SUB);
        sigInput.setTextColor(COLOR_TEXT_MAIN);
        sigInput.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        sigInput.setMinLines(2);
        sigInput.setGravity(Gravity.TOP | Gravity.RIGHT);
        sigInput.setPadding(dp(12), dp(10), dp(12), dp(10));
        sigInput.setTypeface(getFontTypeface(fontIndex, false));
        GradientDrawable sigBg = new GradientDrawable();
        sigBg.setColor(0xFFF8FAFC);
        sigBg.setCornerRadius(dp(12));
        sigBg.setStroke(dp(1), COLOR_DIVIDER);
        sigInput.setBackground(sigBg);
        LinearLayout.LayoutParams sgLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        sgLp.topMargin = dp(6);
        sgLp.bottomMargin = dp(14);
        root.addView(sigInput, sgLp);

        // 5. Change Storage Folder Button
        Button folderBtn = makeSecondaryButton("📁 تغيير مجلد حفظ الملفات");
        folderBtn.setOnClickListener(v -> {
            dialog.dismiss();
            openFolderPicker();
        });
        LinearLayout.LayoutParams fdLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(44));
        fdLp.bottomMargin = dp(16);
        root.addView(folderBtn, fdLp);

        // 6. Save / Cancel Buttons
        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setGravity(Gravity.CENTER);

        Button cancelBtn = makeSecondaryButton("إلغاء");
        cancelBtn.setOnClickListener(v -> dialog.dismiss());

        View space = new View(this);

        Button saveBtn = makePrimaryButton("حفظ الإعدادات");
        saveBtn.setOnClickListener(v -> {
            themeIndex = selectedTheme[0];
            fontIndex = fontSpin.getSelectedItemPosition();
            toolbarIconsOnly = rbIcons.isChecked();
            String newSig = sigInput.getText().toString().trim();

            prefs.edit()
                    .putInt(KEY_THEME, themeIndex)
                    .putInt(KEY_FONT, fontIndex)
                    .putBoolean(KEY_TOOLBAR_ICONS, toolbarIconsOnly)
                    .putString(KEY_SIGNATURE, newSig)
                    .apply();

            applyThemeColors();
            dialog.dismiss();
            if (currentMode == Mode.HOME) {
                showHome();
            } else {
                showDocumentScreen();
            }
            showToast("تم حفظ الإعدادات ✓");
        });

        btnRow.addView(cancelBtn, new LinearLayout.LayoutParams(0, dp(46), 1f));
        btnRow.addView(space, new LinearLayout.LayoutParams(dp(10), dp(1)));
        btnRow.addView(saveBtn, new LinearLayout.LayoutParams(0, dp(46), 1.2f));
        root.addView(btnRow);

        scroll.addView(root);
        dialog.setContentView(scroll);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.93),
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        dialog.show();
    }

    private void openFolderPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                | Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
        startActivityForResult(intent, REQ_OPEN_TREE);
    }

    private Button makePrimaryButton(String text) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setAllCaps(false);
        btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        btn.setTextColor(Color.WHITE);
        btn.setTypeface(getFontTypeface(fontIndex, true));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(themePrimary);
        bg.setCornerRadius(dp(12));
        btn.setBackground(bg);
        return btn;
    }

    private Button makeSecondaryButton(String text) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setAllCaps(false);
        btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        btn.setTextColor(themePrimary);
        btn.setTypeface(getFontTypeface(fontIndex, true));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(themeAccent);
        bg.setCornerRadius(dp(12));
        bg.setStroke(dp(1), themePrimary);
        btn.setBackground(bg);
        return btn;
    }

    private boolean getTextDirection(String text) {
        if (text == null || text.isEmpty()) return true;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            byte dir = Character.getDirectionality(c);
            if (dir == Character.DIRECTIONALITY_RIGHT_TO_LEFT
                    || dir == Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC) {
                return true;
            }
            if (dir == Character.DIRECTIONALITY_LEFT_TO_RIGHT) {
                return false;
            }
        }
        return true;
    }

    private String readTextFromUri(Uri uri) {
        if (uri == null) return "";
        StringBuilder sb = new StringBuilder();
        try (InputStream is = getContentResolver().openInputStream(uri);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            boolean first = true;
            while ((line = reader.readLine()) != null) {
                if (!first) sb.append("\n");
                sb.append(line);
                first = false;
            }
        } catch (Exception e) {
            return "";
        }
        return sb.toString();
    }

    private boolean writeTextToUri(Uri uri, String content) {
        if (uri == null) return false;
        try (OutputStream os = getContentResolver().openOutputStream(uri, "wt")) {
            if (os != null) {
                os.write(content.getBytes(StandardCharsets.UTF_8));
                os.flush();
                return true;
            }
        } catch (Exception e) {
            try (OutputStream os2 = getContentResolver().openOutputStream(uri)) {
                if (os2 != null) {
                    os2.write(content.getBytes(StandardCharsets.UTF_8));
                    os2.flush();
                    return true;
                }
            } catch (Exception ignored) {}
        }
        return false;
    }

    private void hideKeyboard() {
        try {
            View v = getCurrentFocus();
            if (v != null) {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
            }
        } catch (Exception ignored) {}
    }

    private void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private int dp(int val) {
        return Math.round(val * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null || data.getData() == null) return;

        Uri uri = data.getData();
        if (requestCode == REQ_OPEN_TREE) {
            try {
                final int flags = data.getFlags()
                        & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                getContentResolver().takePersistableUriPermission(uri, flags);
                treeUri = uri;
                prefs.edit().putString(KEY_TREE_URI, uri.toString()).apply();
                showToast("تم ربط مجلد الحفظ بنجاح ✓");
                showHome();
            } catch (Exception e) {
                showToast("تعذر حفظ صلاحية المجلد");
            }
        } else if (requestCode == REQ_CREATE_ZIP) {
            try (OutputStream os = getContentResolver().openOutputStream(uri);
                 ZipOutputStream zos = new ZipOutputStream(os)) {
                int count = 0;
                for (FileItem item : allFiles) {
                    String content = readTextFromUri(item.uri);
                    ZipEntry entry = new ZipEntry(item.name);
                    zos.putNextEntry(entry);
                    byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
                    zos.write(bytes, 0, bytes.length);
                    zos.closeEntry();
                    count++;
                }
                zos.finish();
                showToast("تم حفظ النسخة الاحتياطية (" + count + " ملفات) ✓");
            } catch (Exception e) {
                showToast("حدث خطأ أثناء إنشاء ملف ZIP");
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (currentMode == Mode.EDIT) {
            switchMode(Mode.READ, true);
        } else if (currentMode == Mode.READ) {
            goHome();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (currentMode == Mode.EDIT && isDirty) {
            saveCurrentFile(false);
        }
    }
}
