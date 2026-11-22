package com.mazwd.app;

import android.app.Activity;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.DocumentsContract;
import android.text.Editable;
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
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
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
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class MainActivity extends Activity {

    private static final int REQ_OPEN_TREE = 1001;
    private static final int REQ_CREATE_ZIP = 1002;

    private static final String PREFS_NAME = "mazwd_prefs";
    private static final String KEY_TREE_URI = "tree_uri";
    private static final String KEY_THEME_INDEX = "theme_index";
    private static final String KEY_FONT_INDEX = "font_index";
    private static final String KEY_ZOOM_PERCENT = "zoom_percent";
    private static final String KEY_SIGNATURE = "custom_signature";
    private static final String KEY_SIGNATURE_INITIALIZED = "sig_init_v3";
    private static final String KEY_ENABLED_TOOLS = "enabled_tools_v3";
    private static final String KEY_TOOLBAR_ORDER = "toolbar_order_v3";
    private static final String KEY_TOOLBAR_ICONS_ONLY = "toolbar_icons_only";

    private static final String DEFAULT_SIGNATURE = "أ. شفيقة مزود | باحثة في التراث الحضرمي";
    private static final String SIGNATURE_HINT = "اكتب توقيعك هنا\nمثال: عبدالله سعيد - مدير التحرير";

    private static final int COLOR_BG_MAIN = 0xFFF5F7FA;
    private static final int COLOR_CARD_BG = 0xFFFFFFFF;
    private static final int COLOR_TEXT_DARK = 0xFF1E293B;
    private static final int COLOR_TEXT_MUTED = 0xFF64748B;
    private static final int COLOR_BORDER = 0xFFE2E8F0;
    private static final int COLOR_SEARCH_HIGHLIGHT = 0xFFFDE047;

    private static class ThemePalette {
        final String name;
        final int primary;
        final int primaryDark;
        final int accentLight;

        ThemePalette(String name, int primary, int primaryDark, int accentLight) {
            this.name = name;
            this.primary = primary;
            this.primaryDark = primaryDark;
            this.accentLight = accentLight;
        }
    }

    private static final ThemePalette[] THEMES = new ThemePalette[]{
            new ThemePalette("الكحلي الملكي", 0xFF1E3A8A, 0xFF172554, 0xFFDBEAFE),
            new ThemePalette("الأخضر الزمردي", 0xFF065F46, 0xFF064E3B, 0xFFD1FAE5),
            new ThemePalette("العنابي الدافئ", 0xFF881337, 0xFF4C0519, 0xFFFFE4E6),
            new ThemePalette("البنفسجي الهادئ", 0xFF4C1D95, 0xFF2E1065, 0xFFEDE9FE),
            new ThemePalette("الرمادي الكلاسيكي", 0xFF334155, 0xFF0F172A, 0xFFF1F5F9),
            new ThemePalette("البني التراثي", 0xFF78350F, 0xFF451A03, 0xFFFEF3C7),
            new ThemePalette("الطيني الحضرمي", 0xFF9A3412, 0xFF7C2D12, 0xFFFFEDD5),
            new ThemePalette("الأزرق الفيروزي", 0xFF155E75, 0xFF083344, 0xFFCFFAFE)
    };

    private static final String[] FONT_NAMES = new String[]{
            "Sans — خط واضح وعصري (افتراضي)",
            "تجوال — خط حديث وأنيق للمقالات",
            "المراعي — خط عربي كلاسيكي مريح ومتوازن",
            "عارف رقعة — خط الرقعة العربي",
            "ريم كوفي — خط كوفي حديث ومميز",
            "Serif — خط إنجليزي رسمي ومريح",
            "Cursive — خط فني مائل"
    };

    private static final String[] TOOL_IDS = new String[]{
            "undo",
            "redo",
            "copy",
            "paste",
            "select_all",
            "auto_format",
            "signature",
            "cut",
            "select_up",
            "select_down",
            "datetime"
    };

    private static final String[] TOOL_LABELS = new String[]{
            "تراجع",
            "إعادة",
            "نسخ",
            "لصق",
            "تحديد الكل",
            "تنسيق آلي",
            "التوقيع",
            "قص",
            "تحديد لأعلى",
            "تحديد لأسفل",
            "التاريخ والوقت"
    };

    private static final String[] TOOL_ICON_RES = new String[]{
            "ic_undo",
            "ic_redo",
            "ic_copy",
            "ic_paste",
            "ic_select_all",
            "ic_auto_format",
            "ic_signature",
            "ic_cut",
            "ic_select_up",
            "ic_select_down",
            "ic_datetime"
    };

    private static final Set<String> DEFAULT_ENABLED_TOOLS = new HashSet<>(Arrays.asList(
            "undo",
            "redo",
            "copy",
            "paste",
            "select_all",
            "auto_format",
            "signature"
    ));

    private static class FileItem implements Comparable<FileItem> {
        Uri uri;
        String name;
        String displayTitle;
        long lastModified;
        long size;
        String cachedContent;

        @Override
        public int compareTo(FileItem o) {
            return Long.compare(o.lastModified, this.lastModified);
        }
    }

    private enum ScreenState {
        HOME,
        READ,
        EDIT
    }

    private SharedPreferences prefs;
    private ThemePalette currentTheme = THEMES[0];
    private int currentFontIndex = 0;
    private int currentZoomPercent = 100;

    private Typeface fontAlmaraiReg;
    private Typeface fontAlmaraiBold;
    private Typeface fontRuqaaReg;
    private Typeface fontRuqaaBold;
    private Typeface fontTajawalReg;
    private Typeface fontTajawalBold;
    private Typeface fontKufiReg;
    private Typeface fontKufiBold;

    private ScreenState currentScreen = ScreenState.HOME;
    private FrameLayout rootContainer;

    private EditText searchInput;
    private LinearLayout filesListContainer;
    private TextView statusEmptyView;
    private String currentSearchQuery = "";
    private final List<FileItem> allFiles = new ArrayList<>();

    private FileItem activeFile;
    private String activeFileContent = "";
    private EditText documentEditor;
    private TextView readViewText;
    private TextView statsBarText;
    private TextView zoomLabelView;
    private TextView docTitleView;
    private LinearLayout toolbarWrapper;

    private final List<String> undoStack = new ArrayList<>();
    private int undoPointer = -1;
    private boolean isInternalTextChange = false;

    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            requestWindowFeature(Window.FEATURE_NO_TITLE);
        } catch (Throwable ignored) {
        }
        super.onCreate(savedInstanceState);

        try {
            prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            if (!prefs.getBoolean(KEY_SIGNATURE_INITIALIZED, false)) {
                prefs.edit()
                        .putString(KEY_SIGNATURE, DEFAULT_SIGNATURE)
                        .putBoolean(KEY_SIGNATURE_INITIALIZED, true)
                        .apply();
            }

            int themeIdx = prefs.getInt(KEY_THEME_INDEX, 0);
            if (themeIdx < 0 || themeIdx >= THEMES.length) themeIdx = 0;
            currentTheme = THEMES[themeIdx];

            currentFontIndex = prefs.getInt(KEY_FONT_INDEX, 0);
            if (currentFontIndex < 0 || currentFontIndex >= FONT_NAMES.length) currentFontIndex = 0;

            currentZoomPercent = prefs.getInt(KEY_ZOOM_PERCENT, 100);
            if (currentZoomPercent < 60 || currentZoomPercent > 220) currentZoomPercent = 100;

            loadCustomFonts();

            rootContainer = new FrameLayout(this);
            rootContainer.setBackgroundColor(COLOR_BG_MAIN);
            setContentView(rootContainer);

            applyStatusBarTheme();
            renderCurrentScreen();
            refreshFileList();
        } catch (Throwable t) {
            showRecoveryScreen(t);
        }
    }

    private void showRecoveryScreen(Throwable t) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setPadding(dp(24), dp(24), dp(24), dp(24));
        box.setBackgroundColor(COLOR_BG_MAIN);

        TextView msg = new TextView(this);
        msg.setText("حدث خطأ غير متوقع أثناء التشغيل:\n" + t.toString());
        msg.setTextColor(COLOR_TEXT_DARK);
        msg.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        msg.setGravity(Gravity.CENTER);
        box.addView(msg);

        Button resetBtn = new Button(this);
        resetBtn.setText("إعادة ضبط الإعدادات وفتح التطبيق");
        resetBtn.setOnClickListener(v -> {
            if (prefs != null) prefs.edit().clear().apply();
            recreate();
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(48));
        lp.topMargin = dp(20);
        box.addView(resetBtn, lp);

        setContentView(box);
    }

    private void loadCustomFonts() {
        fontAlmaraiReg = loadResFont("mazwd_arabic", Typeface.SANS_SERIF, Typeface.NORMAL);
        fontAlmaraiBold = loadResFont("mazwd_arabic_bold", Typeface.SANS_SERIF, Typeface.BOLD);
        fontRuqaaReg = loadResFont("aref_ruqaa", Typeface.SERIF, Typeface.NORMAL);
        fontRuqaaBold = loadResFont("aref_ruqaa_bold", Typeface.SERIF, Typeface.BOLD);
        fontTajawalReg = loadResFont("tajawal_regular", Typeface.SANS_SERIF, Typeface.NORMAL);
        fontTajawalBold = loadResFont("tajawal_bold", Typeface.SANS_SERIF, Typeface.BOLD);
        fontKufiReg = loadResFont("reem_kufi", Typeface.SANS_SERIF, Typeface.NORMAL);
        fontKufiBold = loadResFont("reem_kufi_bold", Typeface.SANS_SERIF, Typeface.BOLD);
    }

    private Typeface loadResFont(String resName, Typeface fallbackFamily, int style) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                int id = getResources().getIdentifier(resName, "font", getPackageName());
                if (id != 0) {
                    Typeface tf = getResources().getFont(id);
                    if (tf != null) return tf;
                }
            }
        } catch (Throwable ignored) {
        }
        return Typeface.create(fallbackFamily, style);
    }

    private Typeface getFontTypefaceForIndex(int index, int style) {
        boolean isBold = (style == Typeface.BOLD || style == Typeface.BOLD_ITALIC);
        switch (index) {
            case 1:
                return isBold ? fontTajawalBold : fontTajawalReg;
            case 2:
                return isBold ? fontAlmaraiBold : fontAlmaraiReg;
            case 3:
                return isBold ? fontRuqaaBold : fontRuqaaReg;
            case 4:
                return isBold ? fontKufiBold : fontKufiReg;
            case 5:
                return Typeface.create(Typeface.SERIF, style);
            case 6:
                return Typeface.create("cursive", style);
            case 0:
            default:
                return Typeface.create(Typeface.SANS_SERIF, style);
        }
    }

    private Typeface getAppTypeface(int style) {
        Typeface tf = getFontTypefaceForIndex(currentFontIndex, style);
        return tf != null ? tf : Typeface.create(Typeface.SANS_SERIF, style);
    }

    private void applyStatusBarTheme() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Window window = getWindow();
                if (window != null) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                    window.setStatusBarColor(currentTheme.primaryDark);
                    window.setNavigationBarColor(COLOR_CARD_BG);
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private void renderCurrentScreen() {
        if (rootContainer == null) return;
        rootContainer.removeAllViews();
        if (currentScreen == ScreenState.HOME) {
            rootContainer.addView(buildHomeScreen());
        } else {
            rootContainer.addView(buildDocumentScreen());
        }
    }

    // =========================================================================
    // HOME SCREEN
    // =========================================================================

    private View buildHomeScreen() {
        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setBackgroundColor(COLOR_BG_MAIN);
        mainLayout.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        // 1. Modern Rounded Hero Card
        LinearLayout heroCard = new LinearLayout(this);
        heroCard.setOrientation(LinearLayout.VERTICAL);
        heroCard.setGravity(Gravity.CENTER_HORIZONTAL);
        GradientDrawable heroBg = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{currentTheme.primaryDark, currentTheme.primary}
        );
        float r = dp(24);
        heroBg.setCornerRadii(new float[]{0, 0, 0, 0, r, r, r, r});
        heroCard.setBackground(heroBg);
        heroCard.setPadding(dp(20), dp(20), dp(20), dp(20));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            heroCard.setElevation(dp(6));
        }

        TextView titleView = new TextView(this);
        titleView.setText("مفكرة أ. شفيقة مزود");
        titleView.setTextColor(Color.WHITE);
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        titleView.setTypeface(getAppTypeface(Typeface.BOLD));
        titleView.setIncludeFontPadding(false);
        titleView.setGravity(Gravity.CENTER);
        heroCard.addView(titleView);

        TextView subtitleView = new TextView(this);
        subtitleView.setText("كاتبة وباحثة في التراث الحضرمي");
        subtitleView.setTextColor(0xFFE2E8F0);
        subtitleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        subtitleView.setTypeface(getAppTypeface(Typeface.NORMAL));
        subtitleView.setIncludeFontPadding(false);
        subtitleView.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        subLp.topMargin = dp(4);
        subLp.bottomMargin = dp(14);
        heroCard.addView(subtitleView, subLp);

        Button newFileBtn = new Button(this);
        newFileBtn.setText("＋ ملف جديد");
        newFileBtn.setAllCaps(false);
        newFileBtn.setTextColor(currentTheme.primaryDark);
        newFileBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        newFileBtn.setTypeface(getAppTypeface(Typeface.BOLD));
        newFileBtn.setIncludeFontPadding(false);
        newFileBtn.setGravity(Gravity.CENTER);
        newFileBtn.setBackground(makeRoundedRect(Color.WHITE, dp(14), 0, 0));
        newFileBtn.setPadding(dp(24), 0, dp(24), 0);
        newFileBtn.setOnClickListener(v -> showNewFileDialog());
        LinearLayout.LayoutParams newBtnLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(46));
        heroCard.addView(newFileBtn, newBtnLp);

        mainLayout.addView(heroCard, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // 2. Unified Search Box
        LinearLayout searchCard = new LinearLayout(this);
        searchCard.setOrientation(LinearLayout.HORIZONTAL);
        searchCard.setGravity(Gravity.CENTER_VERTICAL);
        searchCard.setBackground(makeRoundedRect(COLOR_CARD_BG, dp(14), COLOR_BORDER, dp(1)));
        searchCard.setPadding(dp(14), dp(4), dp(14), dp(4));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            searchCard.setElevation(dp(2));
        }

        searchInput = new EditText(this);
        searchInput.setHint("ابحث في أسماء ومحتوى ملفاتك...");
        searchInput.setHintTextColor(COLOR_TEXT_MUTED);
        searchInput.setTextColor(COLOR_TEXT_DARK);
        searchInput.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        searchInput.setTypeface(getAppTypeface(Typeface.NORMAL));
        searchInput.setIncludeFontPadding(false);
        searchInput.setSingleLine(true);
        searchInput.setBackgroundColor(Color.TRANSPARENT);
        searchInput.setPadding(dp(8), dp(10), dp(8), dp(10));
        searchInput.setText(currentSearchQuery);

        final TextView clearSearchBtn = new TextView(this);
        clearSearchBtn.setText("✕");
        clearSearchBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        clearSearchBtn.setTextColor(COLOR_TEXT_MUTED);
        clearSearchBtn.setIncludeFontPadding(false);
        clearSearchBtn.setPadding(dp(8), dp(4), dp(8), dp(4));
        clearSearchBtn.setVisibility(currentSearchQuery.isEmpty() ? View.GONE : View.VISIBLE);
        clearSearchBtn.setOnClickListener(v -> {
            searchInput.setText("");
            hideKeyboard();
        });

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s.toString().trim();
                clearSearchBtn.setVisibility(currentSearchQuery.isEmpty() ? View.GONE : View.VISIBLE);
                populateFilesList();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        LinearLayout.LayoutParams sInputLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        searchCard.addView(searchInput, sInputLp);
        searchCard.addView(clearSearchBtn);

        LinearLayout.LayoutParams searchCardLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        searchCardLp.setMargins(dp(16), dp(14), dp(16), dp(8));
        mainLayout.addView(searchCard, searchCardLp);

        // 3. Section Header: "ملفاتي النصية"
        LinearLayout sectionRow = new LinearLayout(this);
        sectionRow.setOrientation(LinearLayout.HORIZONTAL);
        sectionRow.setGravity(Gravity.CENTER_VERTICAL);
        sectionRow.setPadding(dp(18), dp(4), dp(18), dp(6));

        TextView sectionTitle = new TextView(this);
        sectionTitle.setText("ملفاتي النصية");
        sectionTitle.setTextColor(COLOR_TEXT_DARK);
        sectionTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        sectionTitle.setTypeface(getAppTypeface(Typeface.BOLD));
        sectionTitle.setIncludeFontPadding(false);
        LinearLayout.LayoutParams secTitleLp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        sectionRow.addView(sectionTitle, secTitleLp);

        TextView badgeView = new TextView(this);
        badgeView.setText("TXT");
        badgeView.setTextColor(currentTheme.primary);
        badgeView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        badgeView.setTypeface(getAppTypeface(Typeface.BOLD));
        badgeView.setIncludeFontPadding(false);
        badgeView.setBackground(makeRoundedRect(currentTheme.accentLight, dp(8), 0, 0));
        badgeView.setPadding(dp(10), dp(3), dp(10), dp(3));
        sectionRow.addView(badgeView);

        mainLayout.addView(sectionRow);

        // 4. Scrollable Files List Area
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);

        LinearLayout scrollContent = new LinearLayout(this);
        scrollContent.setOrientation(LinearLayout.VERTICAL);
        scrollContent.setPadding(dp(16), dp(4), dp(16), dp(16));

        statusEmptyView = new TextView(this);
        statusEmptyView.setGravity(Gravity.CENTER);
        statusEmptyView.setTextColor(COLOR_TEXT_MUTED);
        statusEmptyView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        statusEmptyView.setTypeface(getAppTypeface(Typeface.NORMAL));
        statusEmptyView.setPadding(dp(20), dp(40), dp(20), dp(40));
        scrollContent.addView(statusEmptyView);

        filesListContainer = new LinearLayout(this);
        filesListContainer.setOrientation(LinearLayout.VERTICAL);
        scrollContent.addView(filesListContainer);

        scrollView.addView(scrollContent);
        LinearLayout.LayoutParams scrollLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        mainLayout.addView(scrollView, scrollLp);

        // 5. Bottom Action Bar (Settings + Backup ZIP only)
        LinearLayout bottomBar = new LinearLayout(this);
        bottomBar.setOrientation(LinearLayout.HORIZONTAL);
        bottomBar.setGravity(Gravity.CENTER_VERTICAL);
        bottomBar.setBackgroundColor(COLOR_CARD_BG);
        bottomBar.setPadding(dp(16), dp(10), dp(16), dp(10));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            bottomBar.setElevation(dp(10));
        }

        Button settingsBtn = createSecondaryButton("⚙ الإعدادات");
        settingsBtn.setOnClickListener(v -> showSettingsDialog());
        LinearLayout.LayoutParams setLp = new LinearLayout.LayoutParams(0, dp(46), 1f);
        setLp.leftMargin = dp(6);
        bottomBar.addView(settingsBtn, setLp);

        Button backupBtn = createPrimaryButton("نسخ احتياطي (ZIP)");
        backupBtn.setOnClickListener(v -> startBackupFlow());
        LinearLayout.LayoutParams bakLp = new LinearLayout.LayoutParams(0, dp(46), 1f);
        bakLp.rightMargin = dp(6);
        bottomBar.addView(backupBtn, bakLp);

        mainLayout.addView(bottomBar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        populateFilesList();
        return mainLayout;
    }

    private void populateFilesList() {
        try {
            if (filesListContainer == null || statusEmptyView == null) return;
            filesListContainer.removeAllViews();

            String savedTree = prefs.getString(KEY_TREE_URI, null);
            if (savedTree == null) {
                statusEmptyView.setVisibility(View.VISIBLE);
                statusEmptyView.setText("مرحباً بك في مفكرة أ. شفيقة مزود\n\nللبدء، يرجى اختيار مجلد لحفظ وقراءة ملفاتك النصية (.txt)\nاضغط هنا أو من الإعدادات لاختيار المجلد.");
                statusEmptyView.setOnClickListener(v -> openFolderPicker());
                return;
            }
            statusEmptyView.setOnClickListener(null);

            String q = currentSearchQuery.toLowerCase(Locale.ROOT);
            List<FileItem> filtered = new ArrayList<>();
            for (FileItem item : allFiles) {
                if (q.isEmpty()) {
                    filtered.add(item);
                } else {
                    boolean nameMatch = item.displayTitle.toLowerCase(Locale.ROOT).contains(q);
                    boolean contentMatch = item.cachedContent != null &&
                            item.cachedContent.toLowerCase(Locale.ROOT).contains(q);
                    if (nameMatch || contentMatch) {
                        filtered.add(item);
                    }
                }
            }

            if (filtered.isEmpty()) {
                statusEmptyView.setVisibility(View.VISIBLE);
                if (q.isEmpty()) {
                    statusEmptyView.setText("لا توجد ملفات نصية (.txt) في هذا المجلد حتى الآن.\nاضغط على «＋ ملف جديد» بالأعلى لإنشاء أول ملف.");
                } else {
                    statusEmptyView.setText("لا توجد نتائج مطابقة لبحثك: «" + currentSearchQuery + "»");
                }
                return;
            }

            statusEmptyView.setVisibility(View.GONE);
            for (FileItem item : filtered) {
                filesListContainer.addView(buildFileCard(item, currentSearchQuery));
            }
        } catch (Throwable ignored) {
        }
    }

    private View buildFileCard(final FileItem item, final String query) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(makeRoundedRect(COLOR_CARD_BG, dp(14), COLOR_BORDER, dp(1)));
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            card.setElevation(dp(2));
        }

        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView titleTv = new TextView(this);
        titleTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        titleTv.setTypeface(getAppTypeface(Typeface.BOLD));
        titleTv.setTextColor(COLOR_TEXT_DARK);
        titleTv.setIncludeFontPadding(false);
        titleTv.setGravity(getTextDirectionGravity(item.displayTitle) | Gravity.CENTER_VERTICAL);

        if (!query.isEmpty()) {
            titleTv.setText(applySearchHighlight(item.displayTitle, query));
        } else {
            titleTv.setText(item.displayTitle);
        }

        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        titleLp.leftMargin = dp(8);
        topRow.addView(titleTv, titleLp);

        FrameLayout moreBtnContainer = new FrameLayout(this);
        moreBtnContainer.setBackground(makeRoundedRect(0xFFF1F5F9, dp(10), COLOR_BORDER, dp(1)));
        ImageView moreIcon = new ImageView(this);
        int moreId = getDrawableIdByName("ic_more_vert");
        if (moreId != 0) {
            moreIcon.setImageResource(moreId);
            moreIcon.setColorFilter(COLOR_TEXT_DARK);
        }
        FrameLayout.LayoutParams iconLp = new FrameLayout.LayoutParams(dp(20), dp(20), Gravity.CENTER);
        moreBtnContainer.addView(moreIcon, iconLp);
        moreBtnContainer.setOnClickListener(v -> showFileOptionsDialog(item));
        LinearLayout.LayoutParams moreContainerLp = new LinearLayout.LayoutParams(dp(36), dp(36));
        topRow.addView(moreBtnContainer, moreContainerLp);

        card.addView(topRow);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd • hh:mm a", new Locale("ar"));
        String dateStr = sdf.format(new Date(item.lastModified));
        String sizeStr = formatFileSize(item.size);

        TextView metaTv = new TextView(this);
        metaTv.setText(dateStr + "   |   " + sizeStr);
        metaTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        metaTv.setTypeface(getAppTypeface(Typeface.NORMAL));
        metaTv.setTextColor(COLOR_TEXT_MUTED);
        metaTv.setIncludeFontPadding(false);
        LinearLayout.LayoutParams metaLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        metaLp.topMargin = dp(4);
        card.addView(metaTv, metaLp);

        if (!query.isEmpty() && item.cachedContent != null) {
            String snippet = extractSearchSnippet(item.cachedContent, query);
            if (snippet != null) {
                TextView snippetTv = new TextView(this);
                snippetTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
                snippetTv.setTypeface(getAppTypeface(Typeface.NORMAL));
                snippetTv.setTextColor(COLOR_TEXT_DARK);
                snippetTv.setBackground(makeRoundedRect(0xFFF8FAFC, dp(8), COLOR_BORDER, dp(1)));
                snippetTv.setPadding(dp(10), dp(6), dp(10), dp(6));
                snippetTv.setText(applySearchHighlight(snippet, query));
                LinearLayout.LayoutParams snipLp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                snipLp.topMargin = dp(8);
                card.addView(snippetTv, snipLp);
            }
        }

        card.setOnClickListener(v -> openFile(item, query));

        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardLp.bottomMargin = dp(10);
        card.setLayoutParams(cardLp);
        return card;
    }

    private void showFileOptionsDialog(final FileItem item) {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        box.setBackground(makeRoundedRect(COLOR_CARD_BG, dp(18), 0, 0));
        box.setPadding(dp(20), dp(20), dp(20), dp(16));

        TextView header = new TextView(this);
        header.setText(item.displayTitle);
        header.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        header.setTypeface(getAppTypeface(Typeface.BOLD));
        header.setTextColor(currentTheme.primaryDark);
        header.setIncludeFontPadding(false);
        header.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams hLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hLp.bottomMargin = dp(14);
        box.addView(header, hLp);

        String[] actions = new String[]{
                "فتح للقراءة",
                "تحرير النص",
                "إعادة تسمية الملف",
                "مشاركة الملف",
                "حذف الملف"
        };

        for (int i = 0; i < actions.length; i++) {
            final int idx = i;
            Button btn = new Button(this);
            btn.setText(actions[i]);
            btn.setAllCaps(false);
            btn.setTypeface(getAppTypeface(Typeface.BOLD));
            btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            btn.setIncludeFontPadding(false);
            btn.setGravity(Gravity.CENTER);
            if (idx == 4) {
                btn.setTextColor(0xFFB91C1C);
                btn.setBackground(makeRoundedRect(0xFFFEF2F2, dp(12), 0xFFFECACA, dp(1)));
            } else {
                btn.setTextColor(COLOR_TEXT_DARK);
                btn.setBackground(makeRoundedRect(0xFFF8FAFC, dp(12), COLOR_BORDER, dp(1)));
            }
            LinearLayout.LayoutParams bLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(46));
            bLp.bottomMargin = dp(8);
            btn.setOnClickListener(v -> {
                dialog.dismiss();
                if (idx == 0) {
                    openFile(item, currentSearchQuery);
                } else if (idx == 1) {
                    openFile(item, currentSearchQuery);
                    currentScreen = ScreenState.EDIT;
                    renderCurrentScreen();
                } else if (idx == 2) {
                    showRenameDialog(item, false);
                } else if (idx == 3) {
                    activeFile = item;
                    activeFileContent = item.cachedContent != null ? item.cachedContent : readTextFromUri(item.uri);
                    shareCurrentDocument();
                } else if (idx == 4) {
                    showDeleteConfirmDialog(item);
                }
            });
            box.addView(btn, bLp);
        }

        dialog.setContentView(box);
        styleDialogWindow(dialog);
        dialog.show();
    }

    // =========================================================================
    // SETTINGS & CUSTOMIZATION DIALOGS
    // =========================================================================

    private void showSettingsDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        ScrollView scroll = new ScrollView(this);
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        card.setBackground(makeRoundedRect(COLOR_CARD_BG, dp(20), 0, 0));
        card.setPadding(dp(20), dp(20), dp(20), dp(20));

        TextView title = new TextView(this);
        title.setText("⚙ إعدادات المفكرة");
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 19);
        title.setTypeface(getAppTypeface(Typeface.BOLD));
        title.setTextColor(currentTheme.primaryDark);
        title.setIncludeFontPadding(false);
        title.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams tLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tLp.bottomMargin = dp(18);
        card.addView(title, tLp);

        // 1. Theme Selector (Single Dropdown Box)
        TextView themeTitle = new TextView(this);
        themeTitle.setText("المظهر اللوني للتطبيق:");
        themeTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        themeTitle.setTypeface(getAppTypeface(Typeface.BOLD));
        themeTitle.setTextColor(COLOR_TEXT_DARK);
        themeTitle.setIncludeFontPadding(false);
        LinearLayout.LayoutParams ttLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ttLp.bottomMargin = dp(8);
        card.addView(themeTitle, ttLp);

        LinearLayout themeDropdownBox = new LinearLayout(this);
        themeDropdownBox.setOrientation(LinearLayout.HORIZONTAL);
        themeDropdownBox.setGravity(Gravity.CENTER_VERTICAL);
        themeDropdownBox.setBackground(makeRoundedRect(0xFFF8FAFC, dp(12), COLOR_BORDER, dp(1)));
        themeDropdownBox.setPadding(dp(14), dp(12), dp(14), dp(12));

        View colorDot = new View(this);
        colorDot.setBackground(makeCircle(currentTheme.primary, 0, 0));
        LinearLayout.LayoutParams dotLp = new LinearLayout.LayoutParams(dp(18), dp(18));
        dotLp.leftMargin = dp(10);
        themeDropdownBox.addView(colorDot, dotLp);

        TextView themeDropdownText = new TextView(this);
        themeDropdownText.setText(currentTheme.name);
        themeDropdownText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        themeDropdownText.setTypeface(getAppTypeface(Typeface.BOLD));
        themeDropdownText.setTextColor(COLOR_TEXT_DARK);
        themeDropdownText.setIncludeFontPadding(false);
        LinearLayout.LayoutParams tdTextLp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        themeDropdownBox.addView(themeDropdownText, tdTextLp);

        TextView themeArrow = new TextView(this);
        themeArrow.setText("▼");
        themeArrow.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        themeArrow.setTextColor(COLOR_TEXT_MUTED);
        themeArrow.setIncludeFontPadding(false);
        themeDropdownBox.addView(themeArrow);

        themeDropdownBox.setOnClickListener(v -> showThemeDropdownDialog(dialog));

        LinearLayout.LayoutParams themeBoxLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        themeBoxLp.bottomMargin = dp(16);
        card.addView(themeDropdownBox, themeBoxLp);

        // 2. Font Selector (Single Dropdown Box with zero top gap)
        TextView fontTitle = new TextView(this);
        fontTitle.setText("نوع الخط في التطبيق والمحرر:");
        fontTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        fontTitle.setTypeface(getAppTypeface(Typeface.BOLD));
        fontTitle.setTextColor(COLOR_TEXT_DARK);
        fontTitle.setIncludeFontPadding(false);
        LinearLayout.LayoutParams ftLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ftLp.bottomMargin = dp(8);
        card.addView(fontTitle, ftLp);

        LinearLayout fontDropdownBox = new LinearLayout(this);
        fontDropdownBox.setOrientation(LinearLayout.HORIZONTAL);
        fontDropdownBox.setGravity(Gravity.CENTER_VERTICAL);
        fontDropdownBox.setBackground(makeRoundedRect(0xFFF8FAFC, dp(12), COLOR_BORDER, dp(1)));
        fontDropdownBox.setPadding(dp(14), dp(12), dp(14), dp(12));

        final TextView fontDropdownText = new TextView(this);
        fontDropdownText.setText(FONT_NAMES[currentFontIndex]);
        fontDropdownText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        fontDropdownText.setTypeface(getFontTypefaceForIndex(currentFontIndex, Typeface.BOLD));
        fontDropdownText.setTextColor(COLOR_TEXT_DARK);
        fontDropdownText.setIncludeFontPadding(false);
        LinearLayout.LayoutParams fdTextLp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        fontDropdownBox.addView(fontDropdownText, fdTextLp);

        TextView fontArrow = new TextView(this);
        fontArrow.setText("▼");
        fontArrow.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        fontArrow.setTextColor(COLOR_TEXT_MUTED);
        fontArrow.setIncludeFontPadding(false);
        fontDropdownBox.addView(fontArrow);

        fontDropdownBox.setOnClickListener(v -> showFontDropdownDialog(fontDropdownText, dialog));

        LinearLayout.LayoutParams fontBoxLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        fontBoxLp.bottomMargin = dp(16);
        card.addView(fontDropdownBox, fontBoxLp);

        // 3. Customize Editor Toolbar & Display Mode
        TextView toolsTitle = new TextView(this);
        toolsTitle.setText("شريط أدوات التحرير:");
        toolsTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        toolsTitle.setTypeface(getAppTypeface(Typeface.BOLD));
        toolsTitle.setTextColor(COLOR_TEXT_DARK);
        toolsTitle.setIncludeFontPadding(false);
        LinearLayout.LayoutParams tltLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tltLp.bottomMargin = dp(8);
        card.addView(toolsTitle, tltLp);

        LinearLayout iconModeRow = new LinearLayout(this);
        iconModeRow.setOrientation(LinearLayout.HORIZONTAL);
        iconModeRow.setGravity(Gravity.CENTER_VERTICAL);
        iconModeRow.setBackground(makeRoundedRect(0xFFF8FAFC, dp(12), COLOR_BORDER, dp(1)));
        iconModeRow.setPadding(dp(12), dp(10), dp(12), dp(10));

        TextView iconModeLabel = new TextView(this);
        iconModeLabel.setText("عرض أيقونات فقط بدون نص في شريط التحرير");
        iconModeLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        iconModeLabel.setTypeface(getAppTypeface(Typeface.BOLD));
        iconModeLabel.setTextColor(COLOR_TEXT_DARK);
        iconModeLabel.setIncludeFontPadding(false);
        LinearLayout.LayoutParams imlLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        iconModeRow.addView(iconModeLabel, imlLp);

        final Switch iconModeSwitch = new Switch(this);
        iconModeSwitch.setChecked(prefs.getBoolean(KEY_TOOLBAR_ICONS_ONLY, false));
        iconModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean(KEY_TOOLBAR_ICONS_ONLY, isChecked).apply();
            if (currentScreen == ScreenState.EDIT) {
                renderCurrentScreen();
            }
        });
        iconModeRow.addView(iconModeSwitch);

        LinearLayout.LayoutParams imRowLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        imRowLp.bottomMargin = dp(8);
        card.addView(iconModeRow, imRowLp);

        Button customizeToolbarBtn = createSecondaryButton("تخصيص وترتيب أدوات التحرير");
        customizeToolbarBtn.setOnClickListener(v -> showCustomizeToolbarDialog());
        LinearLayout.LayoutParams ctbLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(46));
        ctbLp.bottomMargin = dp(16);
        card.addView(customizeToolbarBtn, ctbLp);

        // 4. Custom Signature Input
        TextView sigTitle = new TextView(this);
        sigTitle.setText("التوقيع المخصص (يُدرج بزر التوقيع):");
        sigTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        sigTitle.setTypeface(getAppTypeface(Typeface.BOLD));
        sigTitle.setTextColor(COLOR_TEXT_DARK);
        sigTitle.setIncludeFontPadding(false);
        LinearLayout.LayoutParams stLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        stLp.bottomMargin = dp(6);
        card.addView(sigTitle, stLp);

        final EditText sigInput = new EditText(this);
        sigInput.setText(prefs.getString(KEY_SIGNATURE, ""));
        sigInput.setHint(SIGNATURE_HINT);
        sigInput.setHintTextColor(COLOR_TEXT_MUTED);
        sigInput.setTextColor(COLOR_TEXT_DARK);
        sigInput.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        sigInput.setTypeface(getAppTypeface(Typeface.NORMAL));
        sigInput.setBackground(makeRoundedRect(0xFFF8FAFC, dp(12), COLOR_BORDER, dp(1)));
        sigInput.setPadding(dp(12), dp(10), dp(12), dp(10));
        LinearLayout.LayoutParams siLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        siLp.bottomMargin = dp(16);
        card.addView(sigInput, siLp);

        // 5. Change Storage Folder Button
        Button folderBtn = createSecondaryButton("تغيير مجلد حفظ الملفات");
        folderBtn.setOnClickListener(v -> {
            prefs.edit().putString(KEY_SIGNATURE, sigInput.getText().toString().trim()).apply();
            dialog.dismiss();
            openFolderPicker();
        });
        LinearLayout.LayoutParams fbLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(46));
        fbLp.bottomMargin = dp(12);
        card.addView(folderBtn, fbLp);

        // 6. Save & Close Button
        Button closeBtn = createPrimaryButton("حفظ وإغلاق");
        closeBtn.setOnClickListener(v -> {
            String newSig = sigInput.getText().toString().trim();
            prefs.edit().putString(KEY_SIGNATURE, newSig).apply();
            dialog.dismiss();
            Toast.makeText(this, "تم حفظ الإعدادات", Toast.LENGTH_SHORT).show();
        });
        card.addView(closeBtn, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(46)));

        scroll.addView(card);
        dialog.setContentView(scroll);
        styleDialogWindow(dialog);
        dialog.show();
    }

    private void showThemeDropdownDialog(final Dialog parentSettingsDialog) {
        final Dialog d = new Dialog(this);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);

        ScrollView scroll = new ScrollView(this);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        list.setBackground(makeRoundedRect(COLOR_CARD_BG, dp(16), COLOR_BORDER, dp(1)));
        list.setPadding(dp(10), dp(10), dp(10), dp(10));

        int currentIdx = prefs.getInt(KEY_THEME_INDEX, 0);

        for (int i = 0; i < THEMES.length; i++) {
            final int idx = i;
            final ThemePalette palette = THEMES[i];
            boolean isSelected = (idx == currentIdx);

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setBackground(makeRoundedRect(
                    isSelected ? palette.accentLight : Color.TRANSPARENT,
                    dp(10),
                    isSelected ? palette.primary : 0,
                    isSelected ? dp(1) : 0
            ));
            row.setPadding(dp(12), dp(12), dp(12), dp(12));

            View dot = new View(this);
            dot.setBackground(makeCircle(palette.primary, 0, 0));
            LinearLayout.LayoutParams dotLp = new LinearLayout.LayoutParams(dp(18), dp(18));
            dotLp.leftMargin = dp(12);
            row.addView(dot, dotLp);

            TextView label = new TextView(this);
            label.setText(palette.name);
            label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
            label.setTypeface(getAppTypeface(isSelected ? Typeface.BOLD : Typeface.NORMAL));
            label.setTextColor(isSelected ? palette.primaryDark : COLOR_TEXT_DARK);
            label.setIncludeFontPadding(false);
            LinearLayout.LayoutParams lLp = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            row.addView(label, lLp);

            if (isSelected) {
                TextView check = new TextView(this);
                check.setText("✓");
                check.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
                check.setTypeface(getAppTypeface(Typeface.BOLD));
                check.setTextColor(palette.primary);
                check.setIncludeFontPadding(false);
                row.addView(check);
            }

            row.setOnClickListener(v -> {
                prefs.edit().putInt(KEY_THEME_INDEX, idx).apply();
                currentTheme = THEMES[idx];
                d.dismiss();
                if (parentSettingsDialog != null && parentSettingsDialog.isShowing()) {
                    parentSettingsDialog.dismiss();
                }
                applyStatusBarTheme();
                renderCurrentScreen();
                showSettingsDialog();
            });

            LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            if (i < THEMES.length - 1) rowLp.bottomMargin = dp(4);
            list.addView(row, rowLp);
        }

        scroll.addView(list);
        d.setContentView(scroll);
        styleDialogWindow(d);
        d.show();
    }

    private void showFontDropdownDialog(final TextView fontDropdownText, final Dialog parentSettingsDialog) {
        final Dialog d = new Dialog(this);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);

        ScrollView scroll = new ScrollView(this);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        list.setBackground(makeRoundedRect(COLOR_CARD_BG, dp(16), COLOR_BORDER, dp(1)));
        list.setPadding(dp(10), dp(10), dp(10), dp(10));

        for (int i = 0; i < FONT_NAMES.length; i++) {
            final int idx = i;
            boolean isSelected = (idx == currentFontIndex);

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setBackground(makeRoundedRect(
                    isSelected ? currentTheme.accentLight : Color.TRANSPARENT,
                    dp(10),
                    isSelected ? currentTheme.primary : 0,
                    isSelected ? dp(1) : 0
            ));
            row.setPadding(dp(12), dp(12), dp(12), dp(12));

            TextView label = new TextView(this);
            label.setText(FONT_NAMES[idx]);
            label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
            label.setTypeface(getFontTypefaceForIndex(idx, isSelected ? Typeface.BOLD : Typeface.NORMAL));
            label.setTextColor(isSelected ? currentTheme.primaryDark : COLOR_TEXT_DARK);
            label.setIncludeFontPadding(false);
            LinearLayout.LayoutParams lLp = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            row.addView(label, lLp);

            if (isSelected) {
                TextView check = new TextView(this);
                check.setText("✓");
                check.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
                check.setTypeface(getAppTypeface(Typeface.BOLD));
                check.setTextColor(currentTheme.primary);
                check.setIncludeFontPadding(false);
                row.addView(check);
            }

            row.setOnClickListener(v -> {
                currentFontIndex = idx;
                prefs.edit().putInt(KEY_FONT_INDEX, idx).apply();
                fontDropdownText.setText(FONT_NAMES[idx]);
                fontDropdownText.setTypeface(getFontTypefaceForIndex(idx, Typeface.BOLD));
                d.dismiss();
                if (parentSettingsDialog != null && parentSettingsDialog.isShowing()) {
                    parentSettingsDialog.dismiss();
                }
                renderCurrentScreen();
                showSettingsDialog();
            });

            LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            if (i < FONT_NAMES.length - 1) rowLp.bottomMargin = dp(4);
            list.addView(row, rowLp);
        }

        scroll.addView(list);
        d.setContentView(scroll);
        styleDialogWindow(d);
        d.show();
    }

    private Set<String> getEnabledTools() {
        Set<String> saved = prefs.getStringSet(KEY_ENABLED_TOOLS, null);
        if (saved == null) {
            return new HashSet<>(DEFAULT_ENABLED_TOOLS);
        }
        return new HashSet<>(saved);
    }

    private void saveEnabledTools(Set<String> enabled) {
        prefs.edit().putStringSet(KEY_ENABLED_TOOLS, new HashSet<>(enabled)).apply();
    }

    private List<String> getToolbarOrder() {
        String raw = prefs.getString(KEY_TOOLBAR_ORDER, null);
        List<String> order = new ArrayList<>();
        if (raw != null && !raw.trim().isEmpty()) {
            String[] parts = raw.split(",");
            for (String p : parts) {
                String id = p.trim();
                if (!id.isEmpty() && !order.contains(id) && isValidToolId(id)) {
                    order.add(id);
                }
            }
        }
        for (String defId : TOOL_IDS) {
            if (!order.contains(defId)) {
                order.add(defId);
            }
        }
        return order;
    }

    private boolean isValidToolId(String id) {
        for (String t : TOOL_IDS) {
            if (t.equals(id)) return true;
        }
        return false;
    }

    private void saveToolbarOrder(List<String> order) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < order.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(order.get(i));
        }
        prefs.edit().putString(KEY_TOOLBAR_ORDER, sb.toString()).apply();
    }

    private String getToolLabel(String id) {
        for (int i = 0; i < TOOL_IDS.length; i++) {
            if (TOOL_IDS[i].equals(id)) return TOOL_LABELS[i];
        }
        return id;
    }

    private String getToolIconResName(String id) {
        for (int i = 0; i < TOOL_IDS.length; i++) {
            if (TOOL_IDS[i].equals(id)) return TOOL_ICON_RES[i];
        }
        return "";
    }

    private int getDrawableIdByName(String resName) {
        try {
            return getResources().getIdentifier(resName, "drawable", getPackageName());
        } catch (Throwable e) {
            return 0;
        }
    }

    private void showCustomizeToolbarDialog() {
        final Dialog d = new Dialog(this);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout outer = new LinearLayout(this);
        outer.setOrientation(LinearLayout.VERTICAL);
        outer.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        outer.setBackground(makeRoundedRect(COLOR_CARD_BG, dp(20), 0, 0));
        outer.setPadding(dp(18), dp(18), dp(18), dp(16));

        TextView title = new TextView(this);
        title.setText("تخصيص وترتيب أدوات التحرير");
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        title.setTypeface(getAppTypeface(Typeface.BOLD));
        title.setTextColor(currentTheme.primaryDark);
        title.setIncludeFontPadding(false);
        title.setGravity(Gravity.CENTER);
        outer.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("اسحب من علامة (≡) للأعلى أو الأسفل لتغيير الترتيب، أو فعّل الأدوات التي تريدها:");
        subtitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        subtitle.setTypeface(getAppTypeface(Typeface.NORMAL));
        subtitle.setTextColor(COLOR_TEXT_MUTED);
        subtitle.setIncludeFontPadding(false);
        subtitle.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        subLp.topMargin = dp(4);
        subLp.bottomMargin = dp(12);
        outer.addView(subtitle, subLp);

        final ScrollView scroll = new ScrollView(this);
        final LinearLayout listContainer = new LinearLayout(this);
        listContainer.setOrientation(LinearLayout.VERTICAL);
        listContainer.setClipChildren(false);
        listContainer.setClipToPadding(false);
        scroll.addView(listContainer);

        final List<String> currentOrder = getToolbarOrder();
        final Set<String> enabledSet = getEnabledTools();

        final Runnable[] rebuildListRef = new Runnable[1];
        rebuildListRef[0] = new Runnable() {
            @Override
            public void run() {
                listContainer.removeAllViews();

                for (int i = 0; i < currentOrder.size(); i++) {
                    final int index = i;
                    final String toolId = currentOrder.get(i);
                    final String label = getToolLabel(toolId);
                    final String iconResName = getToolIconResName(toolId);

                    final LinearLayout row = new LinearLayout(MainActivity.this);
                    row.setOrientation(LinearLayout.HORIZONTAL);
                    row.setGravity(Gravity.CENTER_VERTICAL);
                    row.setBackground(makeRoundedRect(0xFFF8FAFC, dp(12), COLOR_BORDER, dp(1)));
                    row.setPadding(dp(10), dp(8), dp(10), dp(8));

                    // Drag Handle (Vector Icon)
                    FrameLayout dragHandle = new FrameLayout(MainActivity.this);
                    dragHandle.setBackground(makeRoundedRect(currentTheme.accentLight, dp(8), 0, 0));
                    ImageView dragImg = new ImageView(MainActivity.this);
                    int dragId = getDrawableIdByName("ic_drag_handle");
                    if (dragId != 0) {
                        dragImg.setImageResource(dragId);
                        dragImg.setColorFilter(currentTheme.primaryDark);
                    }
                    dragHandle.addView(dragImg, new FrameLayout.LayoutParams(dp(20), dp(20), Gravity.CENTER));
                    LinearLayout.LayoutParams dhLp = new LinearLayout.LayoutParams(dp(38), dp(36));
                    dhLp.leftMargin = dp(10);
                    row.addView(dragHandle, dhLp);

                    // Tool Vector Icon Badge
                    FrameLayout iconBadge = new FrameLayout(MainActivity.this);
                    iconBadge.setBackground(makeRoundedRect(Color.WHITE, dp(8), COLOR_BORDER, dp(1)));
                    ImageView toolImg = new ImageView(MainActivity.this);
                    int iconId = getDrawableIdByName(iconResName);
                    if (iconId != 0) {
                        toolImg.setImageResource(iconId);
                        toolImg.setColorFilter(currentTheme.primary);
                    }
                    iconBadge.addView(toolImg, new FrameLayout.LayoutParams(dp(18), dp(18), Gravity.CENTER));
                    LinearLayout.LayoutParams ibLp = new LinearLayout.LayoutParams(dp(34), dp(34));
                    ibLp.leftMargin = dp(10);
                    row.addView(iconBadge, ibLp);

                    // Tool Label
                    TextView nameTv = new TextView(MainActivity.this);
                    nameTv.setText(label);
                    nameTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
                    nameTv.setTypeface(getAppTypeface(Typeface.BOLD));
                    nameTv.setTextColor(COLOR_TEXT_DARK);
                    nameTv.setIncludeFontPadding(false);
                    LinearLayout.LayoutParams nameLp = new LinearLayout.LayoutParams(
                            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                    row.addView(nameTv, nameLp);

                    // CheckBox
                    final CheckBox cb = new CheckBox(MainActivity.this);
                    cb.setChecked(enabledSet.contains(toolId));
                    cb.setOnCheckedChangeListener((buttonView, isChecked) -> {
                        if (isChecked) {
                            enabledSet.add(toolId);
                        } else {
                            enabledSet.remove(toolId);
                        }
                        saveEnabledTools(enabledSet);
                    });
                    row.addView(cb);

                    nameTv.setOnClickListener(v -> cb.setChecked(!cb.isChecked()));

                    // Safe Touch Drag & Drop on Drag Handle
                    final float[] downRawY = new float[1];
                    final boolean[] isDragging = new boolean[]{false};
                    dragHandle.setOnTouchListener((v, event) -> {
                        int action = event.getActionMasked();
                        if (action == MotionEvent.ACTION_DOWN) {
                            isDragging[0] = true;
                            downRawY[0] = event.getRawY();
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                row.setTranslationZ(dp(12));
                            }
                            row.setAlpha(0.92f);
                            row.setBackground(makeRoundedRect(0xFFE0F2FE, dp(12), currentTheme.primary, dp(2)));
                            scroll.requestDisallowInterceptTouchEvent(true);
                            return true;
                        } else if (action == MotionEvent.ACTION_MOVE) {
                            if (!isDragging[0]) return false;
                            float dy = event.getRawY() - downRawY[0];
                            row.setTranslationY(dy);
                            int rowH = row.getHeight() + dp(8);
                            if (rowH > dp(20)) {
                                int offsetSteps = Math.round(dy / rowH);
                                int targetIndex = Math.max(0, Math.min(currentOrder.size() - 1, index + offsetSteps));
                                if (targetIndex != index) {
                                    isDragging[0] = false;
                                    String moved = currentOrder.remove(index);
                                    currentOrder.add(targetIndex, moved);
                                    saveToolbarOrder(currentOrder);
                                    row.setTranslationY(0f);
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                        row.setTranslationZ(0f);
                                    }
                                    listContainer.post(rebuildListRef[0]);
                                    return true;
                                }
                            }
                            return true;
                        } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                            if (isDragging[0]) {
                                isDragging[0] = false;
                                row.setTranslationY(0f);
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                    row.setTranslationZ(0f);
                                }
                                row.setAlpha(1.0f);
                                listContainer.post(rebuildListRef[0]);
                            }
                            scroll.requestDisallowInterceptTouchEvent(false);
                            return true;
                        }
                        return false;
                    });

                    LinearLayout.LayoutParams rLp = new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    rLp.bottomMargin = dp(8);
                    listContainer.addView(row, rLp);
                }
            }
        };

        rebuildListRef[0].run();

        LinearLayout.LayoutParams scLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(360));
        outer.addView(scroll, scLp);

        Button doneBtn = createPrimaryButton("تم");
        doneBtn.setOnClickListener(v -> {
            d.dismiss();
            if (currentScreen == ScreenState.EDIT) {
                renderCurrentScreen();
            }
        });
        LinearLayout.LayoutParams dLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(46));
        dLp.topMargin = dp(12);
        outer.addView(doneBtn, dLp);

        d.setContentView(outer);
        styleDialogWindow(d);
        d.show();
    }

    // =========================================================================
    // DOCUMENT SCREEN (READ & EDIT MODES)
    // =========================================================================

    private View buildDocumentScreen() {
        final boolean isEdit = (currentScreen == ScreenState.EDIT);

        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setBackgroundColor(COLOR_BG_MAIN);
        mainLayout.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        // 1. Clean Top Header Bar
        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        topBar.setBackgroundColor(currentTheme.primary);
        topBar.setPadding(dp(10), dp(10), dp(10), dp(10));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            topBar.setElevation(dp(6));
        }

        TextView backBtn = new TextView(this);
        backBtn.setText("→ رجوع");
        backBtn.setTextColor(Color.WHITE);
        backBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        backBtn.setTypeface(getAppTypeface(Typeface.BOLD));
        backBtn.setIncludeFontPadding(false);
        backBtn.setGravity(Gravity.CENTER);
        backBtn.setBackground(makeRoundedRect(currentTheme.primaryDark, dp(10), 0, 0));
        backBtn.setPadding(dp(12), dp(8), dp(12), dp(8));
        backBtn.setOnClickListener(v -> handleBackNavigation());
        topBar.addView(backBtn);

        docTitleView = new TextView(this);
        docTitleView.setText(activeFile != null ? activeFile.displayTitle : "ملف نصي");
        docTitleView.setTextColor(Color.WHITE);
        docTitleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        docTitleView.setTypeface(getAppTypeface(Typeface.BOLD));
        docTitleView.setIncludeFontPadding(false);
        docTitleView.setSingleLine(true);
        docTitleView.setEllipsize(TextUtils.TruncateAt.END);
        docTitleView.setGravity(Gravity.CENTER);
        docTitleView.setPadding(dp(8), dp(4), dp(8), dp(4));
        docTitleView.setOnClickListener(v -> {
            if (activeFile != null) {
                showRenameDialog(activeFile, true);
            }
        });
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        topBar.addView(docTitleView, titleLp);

        LinearLayout zoomCapsule = new LinearLayout(this);
        zoomCapsule.setOrientation(LinearLayout.HORIZONTAL);
        zoomCapsule.setGravity(Gravity.CENTER_VERTICAL);
        zoomCapsule.setBackground(makeRoundedRect(currentTheme.primaryDark, dp(10), 0x44FFFFFF, dp(1)));
        zoomCapsule.setPadding(dp(4), dp(2), dp(4), dp(2));

        TextView zoomInBtn = new TextView(this);
        zoomInBtn.setText("＋");
        zoomInBtn.setTextColor(Color.WHITE);
        zoomInBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        zoomInBtn.setTypeface(getAppTypeface(Typeface.BOLD));
        zoomInBtn.setIncludeFontPadding(false);
        zoomInBtn.setPadding(dp(8), dp(4), dp(8), dp(4));
        zoomInBtn.setOnClickListener(v -> changeZoom(5));
        zoomCapsule.addView(zoomInBtn);

        zoomLabelView = new TextView(this);
        zoomLabelView.setText(currentZoomPercent + "%");
        zoomLabelView.setTextColor(0xFFE2E8F0);
        zoomLabelView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        zoomLabelView.setTypeface(getAppTypeface(Typeface.BOLD));
        zoomLabelView.setIncludeFontPadding(false);
        zoomLabelView.setPadding(dp(4), dp(4), dp(4), dp(4));
        zoomLabelView.setOnClickListener(v -> showZoomResetDialog());
        zoomCapsule.addView(zoomLabelView);

        TextView zoomOutBtn = new TextView(this);
        zoomOutBtn.setText("－");
        zoomOutBtn.setTextColor(Color.WHITE);
        zoomOutBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        zoomOutBtn.setTypeface(getAppTypeface(Typeface.BOLD));
        zoomOutBtn.setIncludeFontPadding(false);
        zoomOutBtn.setPadding(dp(8), dp(4), dp(8), dp(4));
        zoomOutBtn.setOnClickListener(v -> changeZoom(-5));
        zoomCapsule.addView(zoomOutBtn);

        topBar.addView(zoomCapsule);
        mainLayout.addView(topBar);

        // 2. Editor Toolbar
        toolbarWrapper = new LinearLayout(this);
        toolbarWrapper.setOrientation(LinearLayout.VERTICAL);
        toolbarWrapper.setVisibility(isEdit ? View.VISIBLE : View.GONE);
        if (isEdit) {
            toolbarWrapper.addView(buildEditorToolbar());
        }
        mainLayout.addView(toolbarWrapper);

        // 3. Main Content Paper Card
        LinearLayout paperCard = new LinearLayout(this);
        paperCard.setOrientation(LinearLayout.VERTICAL);
        paperCard.setBackground(makeRoundedRect(COLOR_CARD_BG, dp(16), COLOR_BORDER, dp(1)));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            paperCard.setElevation(dp(2));
        }

        if (isEdit) {
            ScrollView editScroll = new ScrollView(this);
            editScroll.setFillViewport(true);

            documentEditor = new EditText(this);
            documentEditor.setBackgroundColor(Color.TRANSPARENT);
            documentEditor.setTextColor(COLOR_TEXT_DARK);
            documentEditor.setTypeface(getAppTypeface(Typeface.NORMAL));
            documentEditor.setGravity(Gravity.TOP | Gravity.RIGHT);
            documentEditor.setPadding(dp(16), dp(16), dp(16), dp(24));
            documentEditor.setLineSpacing(dp(4), 1.15f);
            applyZoomToTextViews();

            isInternalTextChange = true;
            documentEditor.setText(activeFileContent);
            isInternalTextChange = false;

            resetUndoHistory(activeFileContent);

            documentEditor.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    String current = s.toString();
                    activeFileContent = current;
                    updateStatsCounter(current);
                    if (!isInternalTextChange) {
                        pushUndoState(current);
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });

            editScroll.addView(documentEditor, new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            paperCard.addView(editScroll, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        } else {
            ScrollView readScroll = new ScrollView(this);
            readScroll.setFillViewport(true);

            readViewText = new TextView(this);
            readViewText.setTextColor(COLOR_TEXT_DARK);
            readViewText.setTypeface(getAppTypeface(Typeface.NORMAL));
            readViewText.setGravity(Gravity.TOP | Gravity.RIGHT);
            readViewText.setPadding(dp(18), dp(18), dp(18), dp(28));
            readViewText.setLineSpacing(dp(5), 1.2f);
            readViewText.setTextIsSelectable(true);
            applyZoomToTextViews();

            if (!currentSearchQuery.isEmpty()) {
                readViewText.setText(applySearchHighlight(activeFileContent, currentSearchQuery));
            } else {
                readViewText.setText(activeFileContent);
            }

            readScroll.addView(readViewText, new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            paperCard.addView(readScroll, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        }

        LinearLayout.LayoutParams paperLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        paperLp.setMargins(dp(12), dp(10), dp(12), dp(8));
        mainLayout.addView(paperCard, paperLp);

        // 4. Bottom Live Counter Bar
        LinearLayout counterBar = new LinearLayout(this);
        counterBar.setOrientation(LinearLayout.HORIZONTAL);
        counterBar.setGravity(Gravity.CENTER);
        counterBar.setBackgroundColor(0xFFE2E8F0);
        counterBar.setPadding(dp(12), dp(6), dp(12), dp(6));

        statsBarText = new TextView(this);
        statsBarText.setTextColor(COLOR_TEXT_DARK);
        statsBarText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        statsBarText.setTypeface(getAppTypeface(Typeface.BOLD));
        statsBarText.setIncludeFontPadding(false);
        counterBar.addView(statsBarText);

        mainLayout.addView(counterBar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        updateStatsCounter(activeFileContent);

        // 5. Read-Mode Bottom Action Bar
        if (!isEdit) {
            LinearLayout readBottomBar = new LinearLayout(this);
            readBottomBar.setOrientation(LinearLayout.HORIZONTAL);
            readBottomBar.setGravity(Gravity.CENTER_VERTICAL);
            readBottomBar.setBackgroundColor(COLOR_CARD_BG);
            readBottomBar.setPadding(dp(14), dp(10), dp(14), dp(10));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                readBottomBar.setElevation(dp(8));
            }

            Button editModeBtn = createPrimaryButton("تحرير الملف");
            editModeBtn.setOnClickListener(v -> {
                currentScreen = ScreenState.EDIT;
                renderCurrentScreen();
                if (documentEditor != null) {
                    documentEditor.requestFocus();
                    showKeyboard(documentEditor);
                }
            });
            LinearLayout.LayoutParams ebLp = new LinearLayout.LayoutParams(0, dp(46), 1f);
            ebLp.leftMargin = dp(6);
            readBottomBar.addView(editModeBtn, ebLp);

            Button shareBtn = createSecondaryButton("مشاركة الملف");
            shareBtn.setOnClickListener(v -> shareCurrentDocument());
            LinearLayout.LayoutParams sbLp = new LinearLayout.LayoutParams(0, dp(46), 1f);
            sbLp.rightMargin = dp(6);
            readBottomBar.addView(shareBtn, sbLp);

            mainLayout.addView(readBottomBar, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }

        return mainLayout;
    }

    private void showZoomResetDialog() {
        final Dialog d = new Dialog(this);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        box.setBackground(makeRoundedRect(COLOR_CARD_BG, dp(18), 0, 0));
        box.setPadding(dp(20), dp(20), dp(20), dp(16));

        TextView title = new TextView(this);
        title.setText("حجم الخط والتكبير (" + currentZoomPercent + "%)");
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        title.setTypeface(getAppTypeface(Typeface.BOLD));
        title.setTextColor(currentTheme.primaryDark);
        title.setIncludeFontPadding(false);
        title.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams tLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tLp.bottomMargin = dp(14);
        box.addView(title, tLp);

        Button resetBtn = createPrimaryButton("إعادة الحجم الافتراضي (100%)");
        resetBtn.setOnClickListener(v -> {
            currentZoomPercent = 100;
            prefs.edit().putInt(KEY_ZOOM_PERCENT, 100).apply();
            if (zoomLabelView != null) zoomLabelView.setText("100%");
            applyZoomToTextViews();
            d.dismiss();
        });
        box.addView(resetBtn, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(46)));

        d.setContentView(box);
        styleDialogWindow(d);
        d.show();
    }

    private View buildEditorToolbar() {
        HorizontalScrollView hsv = new HorizontalScrollView(this);
        hsv.setHorizontalScrollBarEnabled(false);
        hsv.setBackgroundColor(COLOR_CARD_BG);
        hsv.setPadding(dp(8), dp(6), dp(8), dp(6));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            hsv.setElevation(dp(3));
        }

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        Set<String> enabled = getEnabledTools();
        List<String> orderedIds = getToolbarOrder();
        boolean iconsOnly = prefs.getBoolean(KEY_TOOLBAR_ICONS_ONLY, false);

        for (final String id : orderedIds) {
            if (!enabled.contains(id)) continue;
            View chip = createToolbarActionView(id, iconsOnly, () -> performToolbarAction(id));
            row.addView(chip);
        }

        hsv.addView(row);
        return hsv;
    }

    private View createToolbarActionView(String toolId, boolean iconsOnly, Runnable action) {
        String label = getToolLabel(toolId);
        String iconResName = getToolIconResName(toolId);

        if (iconsOnly) {
            FrameLayout iconBox = new FrameLayout(this);
            iconBox.setBackground(makeRoundedRect(currentTheme.accentLight, dp(10), COLOR_BORDER, dp(1)));
            iconBox.setContentDescription(label);

            ImageView iv = new ImageView(this);
            int resId = getDrawableIdByName(iconResName);
            if (resId != 0) {
                iv.setImageResource(resId);
                iv.setColorFilter(currentTheme.primaryDark);
            }
            FrameLayout.LayoutParams ivLp = new FrameLayout.LayoutParams(dp(20), dp(20), Gravity.CENTER);
            iconBox.addView(iv, ivLp);

            iconBox.setOnClickListener(v -> action.run());
            iconBox.setOnLongClickListener(v -> {
                Toast.makeText(this, label, Toast.LENGTH_SHORT).show();
                return true;
            });

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(42), dp(38));
            lp.setMargins(dp(3), 0, dp(3), 0);
            iconBox.setLayoutParams(lp);
            return iconBox;
        } else {
            TextView chip = new TextView(this);
            chip.setText(label);
            chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            chip.setTypeface(getAppTypeface(Typeface.BOLD));
            chip.setTextColor(currentTheme.primaryDark);
            chip.setIncludeFontPadding(false);
            chip.setGravity(Gravity.CENTER);
            chip.setBackground(makeRoundedRect(currentTheme.accentLight, dp(10), COLOR_BORDER, dp(1)));
            chip.setPadding(dp(12), dp(7), dp(12), dp(7));
            chip.setOnClickListener(v -> action.run());

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(dp(3), 0, dp(3), 0);
            chip.setLayoutParams(lp);
            return chip;
        }
    }

    private void performToolbarAction(String toolId) {
        if (documentEditor == null) return;

        switch (toolId) {
            case "undo":
                performUndo();
                break;
            case "redo":
                performRedo();
                break;
            case "copy":
                copySelectedOrAll();
                break;
            case "cut":
                cutSelectedText();
                break;
            case "paste":
                pasteFromClipboard();
                break;
            case "select_all":
                documentEditor.requestFocus();
                documentEditor.selectAll();
                break;
            case "select_up":
                selectToStart();
                break;
            case "select_down":
                selectToEnd();
                break;
            case "auto_format":
                autoFormatText();
                break;
            case "datetime":
                insertDateTime();
                break;
            case "signature":
                insertSignature();
                break;
        }
    }

    private void resetUndoHistory(String initialText) {
        undoStack.clear();
        undoStack.add(initialText != null ? initialText : "");
        undoPointer = 0;
    }

    private void pushUndoState(String text) {
        if (undoPointer >= 0 && undoPointer < undoStack.size()) {
            if (undoStack.get(undoPointer).equals(text)) return;
        }
        while (undoStack.size() > undoPointer + 1) {
            undoStack.remove(undoStack.size() - 1);
        }
        undoStack.add(text);
        if (undoStack.size() > 80) {
            undoStack.remove(0);
        }
        undoPointer = undoStack.size() - 1;
    }

    private void performUndo() {
        if (undoPointer > 0) {
            undoPointer--;
            String prev = undoStack.get(undoPointer);
            isInternalTextChange = true;
            documentEditor.setText(prev);
            documentEditor.setSelection(prev.length());
            isInternalTextChange = false;
            activeFileContent = prev;
            updateStatsCounter(prev);
        } else {
            Toast.makeText(this, "لا يوجد تراجع إضافي", Toast.LENGTH_SHORT).show();
        }
    }

    private void performRedo() {
        if (undoPointer < undoStack.size() - 1) {
            undoPointer++;
            String next = undoStack.get(undoPointer);
            isInternalTextChange = true;
            documentEditor.setText(next);
            documentEditor.setSelection(next.length());
            isInternalTextChange = false;
            activeFileContent = next;
            updateStatsCounter(next);
        } else {
            Toast.makeText(this, "لا توجد خطوة للإعادة", Toast.LENGTH_SHORT).show();
        }
    }

    private void copySelectedOrAll() {
        int start = Math.max(0, documentEditor.getSelectionStart());
        int end = Math.max(0, documentEditor.getSelectionEnd());
        String text = documentEditor.getText().toString();
        String toCopy;
        if (start != end) {
            toCopy = text.substring(Math.min(start, end), Math.max(start, end));
        } else {
            toCopy = text;
        }
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm != null) {
            cm.setPrimaryClip(ClipData.newPlainText("mazwd_text", toCopy));
            Toast.makeText(this, "تم النسخ إلى الحافظة", Toast.LENGTH_SHORT).show();
        }
    }

    private void cutSelectedText() {
        int start = Math.max(0, documentEditor.getSelectionStart());
        int end = Math.max(0, documentEditor.getSelectionEnd());
        if (start == end) {
            Toast.makeText(this, "حدّد نصاً أولاً لقصه", Toast.LENGTH_SHORT).show();
            return;
        }
        int min = Math.min(start, end);
        int max = Math.max(start, end);
        String selected = documentEditor.getText().subSequence(min, max).toString();
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm != null) {
            cm.setPrimaryClip(ClipData.newPlainText("mazwd_cut", selected));
        }
        documentEditor.getText().delete(min, max);
        Toast.makeText(this, "تم قص النص المحدد", Toast.LENGTH_SHORT).show();
    }

    private void pasteFromClipboard() {
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm != null && cm.hasPrimaryClip() && cm.getPrimaryClip() != null
                && cm.getPrimaryClip().getItemCount() > 0) {
            CharSequence clip = cm.getPrimaryClip().getItemAt(0).coerceToText(this);
            if (clip != null) {
                int start = Math.max(0, documentEditor.getSelectionStart());
                int end = Math.max(0, documentEditor.getSelectionEnd());
                documentEditor.getText().replace(Math.min(start, end), Math.max(start, end), clip);
            }
        } else {
            Toast.makeText(this, "الحافظة فارغة", Toast.LENGTH_SHORT).show();
        }
    }

    private void selectToStart() {
        documentEditor.requestFocus();
        int cursor = Math.max(0, documentEditor.getSelectionEnd());
        documentEditor.setSelection(0, cursor);
    }

    private void selectToEnd() {
        documentEditor.requestFocus();
        int cursor = Math.max(0, documentEditor.getSelectionStart());
        int len = documentEditor.getText().length();
        documentEditor.setSelection(cursor, len);
    }

    private void autoFormatText() {
        String raw = documentEditor.getText().toString();
        if (raw.trim().isEmpty()) return;

        String formatted = raw
                .replaceAll("[ \\t]+", " ")
                .replaceAll(" *\\n *", "\n")
                .replaceAll("\\n{3,}", "\n\n")
                .replaceAll(" +([،؛؟.!:])", "$1")
                .trim();

        documentEditor.setText(formatted);
        documentEditor.setSelection(formatted.length());
        Toast.makeText(this, "تم تنسيق النص وترتيب الفقرات", Toast.LENGTH_SHORT).show();
    }

    private void insertDateTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd - hh:mm a", new Locale("ar"));
        String dt = sdf.format(new Date());
        int start = Math.max(0, documentEditor.getSelectionStart());
        documentEditor.getText().insert(start, dt);
    }

    private void insertSignature() {
        String sig = prefs.getString(KEY_SIGNATURE, "").trim();
        if (sig.isEmpty()) {
            showEmptySignatureDialog();
            return;
        }
        String block = "\n\n— " + sig + "\n";
        int pos = Math.max(0, documentEditor.getSelectionStart());
        documentEditor.getText().insert(pos, block);
    }

    private void showEmptySignatureDialog() {
        final Dialog d = new Dialog(this);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        box.setBackground(makeRoundedRect(COLOR_CARD_BG, dp(18), 0, 0));
        box.setPadding(dp(20), dp(20), dp(20), dp(16));

        TextView title = new TextView(this);
        title.setText("إضافة توقيعك المخصص");
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        title.setTypeface(getAppTypeface(Typeface.BOLD));
        title.setTextColor(currentTheme.primaryDark);
        title.setIncludeFontPadding(false);
        title.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams tLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tLp.bottomMargin = dp(12);
        box.addView(title, tLp);

        final EditText input = new EditText(this);
        input.setHint(SIGNATURE_HINT);
        input.setHintTextColor(COLOR_TEXT_MUTED);
        input.setTextColor(COLOR_TEXT_DARK);
        input.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        input.setTypeface(getAppTypeface(Typeface.NORMAL));
        input.setBackground(makeRoundedRect(0xFFF8FAFC, dp(12), COLOR_BORDER, dp(1)));
        input.setPadding(dp(12), dp(10), dp(12), dp(10));
        LinearLayout.LayoutParams inLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        inLp.bottomMargin = dp(14);
        box.addView(input, inLp);

        Button saveInsertBtn = createPrimaryButton("حفظ وإدراج التوقيع");
        saveInsertBtn.setOnClickListener(v -> {
            String newSig = input.getText().toString().trim();
            if (newSig.isEmpty()) {
                Toast.makeText(this, "يرجى كتابة التوقيع أولاً", Toast.LENGTH_SHORT).show();
                return;
            }
            prefs.edit().putString(KEY_SIGNATURE, newSig).apply();
            d.dismiss();
            if (documentEditor != null) {
                String block = "\n\n— " + newSig + "\n";
                int pos = Math.max(0, documentEditor.getSelectionStart());
                documentEditor.getText().insert(pos, block);
            }
        });
        box.addView(saveInsertBtn, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(46)));

        d.setContentView(box);
        styleDialogWindow(d);
        d.show();
    }

    private void changeZoom(int delta) {
        currentZoomPercent = Math.max(60, Math.min(220, currentZoomPercent + delta));
        prefs.edit().putInt(KEY_ZOOM_PERCENT, currentZoomPercent).apply();
        if (zoomLabelView != null) {
            zoomLabelView.setText(currentZoomPercent + "%");
        }
        applyZoomToTextViews();
    }

    private void applyZoomToTextViews() {
        float baseSp = 17f;
        float scaledSp = baseSp * (currentZoomPercent / 100f);
        if (documentEditor != null) {
            documentEditor.setTextSize(TypedValue.COMPLEX_UNIT_SP, scaledSp);
        }
        if (readViewText != null) {
            readViewText.setTextSize(TypedValue.COMPLEX_UNIT_SP, scaledSp);
        }
    }

    private void updateStatsCounter(String text) {
        if (statsBarText == null) return;
        if (text == null || text.isEmpty()) {
            statsBarText.setText("الكلمات: 0   •   الحروف: 0   •   الأسطر: 0");
            return;
        }
        int chars = text.length();
        String trimmed = text.trim();
        int words = trimmed.isEmpty() ? 0 : trimmed.split("\\s+").length;
        int lines = text.split("\r\n|\r|\n", -1).length;
        statsBarText.setText("الكلمات: " + words + "   •   الحروف: " + chars + "   •   الأسطر: " + lines);
    }

    // =========================================================================
    // FILE OPERATIONS (SAF)
    // =========================================================================

    private void openFolderPicker() {
        try {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                    | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                    | Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
            startActivityForResult(intent, REQ_OPEN_TREE);
        } catch (Throwable e) {
            Toast.makeText(this, "تعذر فتح مستعرض المجلدات", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != Activity.RESULT_OK || data == null || data.getData() == null) return;

        Uri uri = data.getData();
        if (requestCode == REQ_OPEN_TREE) {
            try {
                final int takeFlags = data.getFlags()
                        & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                getContentResolver().takePersistableUriPermission(uri, takeFlags);
            } catch (Throwable ignored) {
            }
            prefs.edit().putString(KEY_TREE_URI, uri.toString()).apply();
            Toast.makeText(this, "تم ربط المجلد بنجاح", Toast.LENGTH_SHORT).show();
            refreshFileList();
        } else if (requestCode == REQ_CREATE_ZIP) {
            performZipExport(uri);
        }
    }

    private void refreshFileList() {
        final String savedTree = prefs != null ? prefs.getString(KEY_TREE_URI, null) : null;
        if (savedTree == null) {
            allFiles.clear();
            populateFilesList();
            return;
        }

        ioExecutor.execute(() -> {
            List<FileItem> loaded = new ArrayList<>();
            try {
                Uri treeUri = Uri.parse(savedTree);
                String treeDocId = DocumentsContract.getTreeDocumentId(treeUri);
                Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, treeDocId);

                ContentResolver resolver = getContentResolver();
                String[] projection = new String[]{
                        DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                        DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                        DocumentsContract.Document.COLUMN_LAST_MODIFIED,
                        DocumentsContract.Document.COLUMN_SIZE,
                        DocumentsContract.Document.COLUMN_MIME_TYPE
                };

                try (Cursor c = resolver.query(childrenUri, projection, null, null, null)) {
                    if (c != null) {
                        while (c.moveToNext()) {
                            String docId = c.getString(0);
                            String name = c.getString(1);
                            long lastMod = c.isNull(2) ? 0L : c.getLong(2);
                            long size = c.isNull(3) ? 0L : c.getLong(3);
                            String mime = c.isNull(4) ? "" : c.getString(4);

                            if (name == null) continue;
                            if (DocumentsContract.Document.MIME_TYPE_DIR.equals(mime)) continue;
                            if (!name.toLowerCase(Locale.ROOT).endsWith(".txt")) continue;

                            Uri fileUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId);
                            FileItem item = new FileItem();
                            item.uri = fileUri;
                            item.name = name;
                            item.displayTitle = stripTxtExtension(name);
                            item.lastModified = lastMod;
                            item.size = size;
                            item.cachedContent = readTextFromUri(fileUri);
                            loaded.add(item);
                        }
                    }
                }
                Collections.sort(loaded);
            } catch (Throwable ignored) {
            }

            mainHandler.post(() -> {
                allFiles.clear();
                allFiles.addAll(loaded);
                populateFilesList();
            });
        });
    }

    private void showNewFileDialog() {
        String savedTree = prefs.getString(KEY_TREE_URI, null);
        if (savedTree == null) {
            Toast.makeText(this, "يرجى اختيار مجلد الحفظ أولاً", Toast.LENGTH_SHORT).show();
            openFolderPicker();
            return;
        }

        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        box.setBackground(makeRoundedRect(COLOR_CARD_BG, dp(18), 0, 0));
        box.setPadding(dp(20), dp(20), dp(20), dp(16));

        TextView title = new TextView(this);
        title.setText("إنشاء ملف نصي جديد");
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        title.setTypeface(getAppTypeface(Typeface.BOLD));
        title.setTextColor(currentTheme.primaryDark);
        title.setIncludeFontPadding(false);
        title.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams tLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tLp.bottomMargin = dp(12);
        box.addView(title, tLp);

        final EditText nameInput = new EditText(this);
        nameInput.setHint("اكتب اسم الملف...");
        nameInput.setHintTextColor(COLOR_TEXT_MUTED);
        nameInput.setTextColor(COLOR_TEXT_DARK);
        nameInput.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        nameInput.setTypeface(getAppTypeface(Typeface.NORMAL));
        nameInput.setSingleLine(true);
        nameInput.setBackground(makeRoundedRect(0xFFF8FAFC, dp(12), COLOR_BORDER, dp(1)));
        nameInput.setPadding(dp(12), dp(10), dp(12), dp(10));
        LinearLayout.LayoutParams inLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        inLp.bottomMargin = dp(16);
        box.addView(nameInput, inLp);

        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);

        Button cancelBtn = createSecondaryButton("إلغاء");
        cancelBtn.setOnClickListener(v -> dialog.dismiss());
        LinearLayout.LayoutParams cLp = new LinearLayout.LayoutParams(0, dp(46), 1f);
        cLp.leftMargin = dp(6);
        btnRow.addView(cancelBtn, cLp);

        Button createBtn = createPrimaryButton("إنشاء وفتح");
        createBtn.setOnClickListener(v -> {
            String rawName = nameInput.getText().toString().trim();
            if (rawName.isEmpty()) {
                Toast.makeText(this, "يرجى كتابة اسم للملف", Toast.LENGTH_SHORT).show();
                return;
            }
            dialog.dismiss();
            createNewFile(rawName);
        });
        LinearLayout.LayoutParams okLp = new LinearLayout.LayoutParams(0, dp(46), 1f);
        okLp.rightMargin = dp(6);
        btnRow.addView(createBtn, okLp);

        box.addView(btnRow);
        dialog.setContentView(box);
        styleDialogWindow(dialog);
        dialog.show();
    }

    private void createNewFile(String rawName) {
        try {
            String savedTree = prefs.getString(KEY_TREE_URI, null);
            if (savedTree == null) return;

            Uri treeUri = Uri.parse(savedTree);
            String parentDocId = DocumentsContract.getTreeDocumentId(treeUri);
            Uri parentDocUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, parentDocId);

            String fileName = rawName.toLowerCase(Locale.ROOT).endsWith(".txt") ? rawName : rawName + ".txt";
            Uri newDocUri = DocumentsContract.createDocument(
                    getContentResolver(), parentDocUri, "text/plain", fileName);

            if (newDocUri != null) {
                writeTextToUri(newDocUri, "");
                FileItem newItem = new FileItem();
                newItem.uri = newDocUri;
                newItem.name = fileName;
                newItem.displayTitle = stripTxtExtension(fileName);
                newItem.lastModified = System.currentTimeMillis();
                newItem.size = 0;
                newItem.cachedContent = "";

                activeFile = newItem;
                activeFileContent = "";
                currentScreen = ScreenState.EDIT;
                renderCurrentScreen();
                refreshFileList();
            } else {
                Toast.makeText(this, "تعذر إنشاء الملف", Toast.LENGTH_SHORT).show();
            }
        } catch (Throwable e) {
            Toast.makeText(this, "خطأ أثناء إنشاء الملف: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void openFile(FileItem item, String searchHighlightQuery) {
        activeFile = item;
        activeFileContent = item.cachedContent != null ? item.cachedContent : readTextFromUri(item.uri);
        currentScreen = ScreenState.READ;
        renderCurrentScreen();

        if (searchHighlightQuery != null && !searchHighlightQuery.isEmpty() && readViewText != null) {
            final int matchIndex = activeFileContent.toLowerCase(Locale.ROOT)
                    .indexOf(searchHighlightQuery.toLowerCase(Locale.ROOT));
            if (matchIndex >= 0) {
                readViewText.post(() -> {
                    try {
                        if (readViewText.getLayout() != null) {
                            int line = readViewText.getLayout().getLineForOffset(matchIndex);
                            int y = readViewText.getLayout().getLineTop(line);
                            View parent = (View) readViewText.getParent();
                            if (parent instanceof ScrollView) {
                                ((ScrollView) parent).smoothScrollTo(0, Math.max(0, y - dp(60)));
                            }
                        }
                    } catch (Throwable ignored) {
                    }
                });
            }
        }
    }

    private void saveActiveDocument(boolean showToast) {
        if (activeFile == null || activeFile.uri == null) return;
        if (currentScreen == ScreenState.EDIT && documentEditor != null) {
            activeFileContent = documentEditor.getText().toString();
        }
        boolean ok = writeTextToUri(activeFile.uri, activeFileContent);
        if (ok) {
            activeFile.cachedContent = activeFileContent;
            activeFile.lastModified = System.currentTimeMillis();
            activeFile.size = activeFileContent.getBytes(StandardCharsets.UTF_8).length;
            if (showToast) {
                Toast.makeText(this, "تم حفظ التعديلات", Toast.LENGTH_SHORT).show();
            }
            refreshFileList();
        } else if (showToast) {
            Toast.makeText(this, "حدث خطأ أثناء حفظ الملف", Toast.LENGTH_SHORT).show();
        }
    }

    private void showRenameDialog(final FileItem item, final boolean fromDocumentHeader) {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        box.setBackground(makeRoundedRect(COLOR_CARD_BG, dp(18), 0, 0));
        box.setPadding(dp(20), dp(20), dp(20), dp(16));

        TextView title = new TextView(this);
        title.setText("إعادة تسمية الملف");
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        title.setTypeface(getAppTypeface(Typeface.BOLD));
        title.setTextColor(currentTheme.primaryDark);
        title.setIncludeFontPadding(false);
        title.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams tLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tLp.bottomMargin = dp(12);
        box.addView(title, tLp);

        final EditText input = new EditText(this);
        input.setText(item.displayTitle);
        input.setSelection(input.getText().length());
        input.setTextColor(COLOR_TEXT_DARK);
        input.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        input.setTypeface(getAppTypeface(Typeface.NORMAL));
        input.setSingleLine(true);
        input.setBackground(makeRoundedRect(0xFFF8FAFC, dp(12), COLOR_BORDER, dp(1)));
        input.setPadding(dp(12), dp(10), dp(12), dp(10));
        LinearLayout.LayoutParams inLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        inLp.bottomMargin = dp(16);
        box.addView(input, inLp);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);

        Button cancelBtn = createSecondaryButton("إلغاء");
        cancelBtn.setOnClickListener(v -> dialog.dismiss());
        LinearLayout.LayoutParams cLp = new LinearLayout.LayoutParams(0, dp(46), 1f);
        cLp.leftMargin = dp(6);
        row.addView(cancelBtn, cLp);

        Button saveBtn = createPrimaryButton("حفظ الاسم");
        saveBtn.setOnClickListener(v -> {
            String newTitle = input.getText().toString().trim();
            if (newTitle.isEmpty()) {
                Toast.makeText(this, "الاسم لا يمكن أن يكون فارغاً", Toast.LENGTH_SHORT).show();
                return;
            }
            dialog.dismiss();
            renameFileItem(item, newTitle, fromDocumentHeader);
        });
        LinearLayout.LayoutParams sLp = new LinearLayout.LayoutParams(0, dp(46), 1f);
        sLp.rightMargin = dp(6);
        row.addView(saveBtn, sLp);

        box.addView(row);
        dialog.setContentView(box);
        styleDialogWindow(dialog);
        dialog.show();
    }

    private void renameFileItem(FileItem item, String rawNewName, boolean fromDocumentHeader) {
        try {
            if (currentScreen == ScreenState.EDIT) {
                saveActiveDocument(false);
            }
            String fullNewName = rawNewName.toLowerCase(Locale.ROOT).endsWith(".txt")
                    ? rawNewName : rawNewName + ".txt";

            Uri renamedUri = DocumentsContract.renameDocument(getContentResolver(), item.uri, fullNewName);
            if (renamedUri != null) {
                item.uri = renamedUri;
                item.name = fullNewName;
                item.displayTitle = stripTxtExtension(fullNewName);
                if (fromDocumentHeader && docTitleView != null) {
                    docTitleView.setText(item.displayTitle);
                }
                Toast.makeText(this, "تم تغيير اسم الملف", Toast.LENGTH_SHORT).show();
                refreshFileList();
            } else {
                Toast.makeText(this, "تعذر إعادة تسمية الملف", Toast.LENGTH_SHORT).show();
            }
        } catch (Throwable e) {
            Toast.makeText(this, "خطأ أثناء التسمية: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void showDeleteConfirmDialog(final FileItem item) {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        box.setBackground(makeRoundedRect(COLOR_CARD_BG, dp(18), 0, 0));
        box.setPadding(dp(20), dp(20), dp(20), dp(16));

        TextView title = new TextView(this);
        title.setText("تأكيد حذف الملف");
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        title.setTypeface(getAppTypeface(Typeface.BOLD));
        title.setTextColor(0xFFB91C1C);
        title.setIncludeFontPadding(false);
        title.setGravity(Gravity.CENTER);
        box.addView(title);

        TextView msg = new TextView(this);
        msg.setText("هل أنت متأكد من حذف الملف «" + item.displayTitle + "» نهائياً؟");
        msg.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        msg.setTypeface(getAppTypeface(Typeface.NORMAL));
        msg.setTextColor(COLOR_TEXT_DARK);
        msg.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams mLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        mLp.topMargin = dp(10);
        mLp.bottomMargin = dp(18);
        box.addView(msg, mLp);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);

        Button cancelBtn = createSecondaryButton("إلغاء");
        cancelBtn.setOnClickListener(v -> dialog.dismiss());
        LinearLayout.LayoutParams cLp = new LinearLayout.LayoutParams(0, dp(46), 1f);
        cLp.leftMargin = dp(6);
        row.addView(cancelBtn, cLp);

        Button delBtn = new Button(this);
        delBtn.setText("حذف نهائياً");
        delBtn.setAllCaps(false);
        delBtn.setTextColor(Color.WHITE);
        delBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        delBtn.setTypeface(getAppTypeface(Typeface.BOLD));
        delBtn.setIncludeFontPadding(false);
        delBtn.setGravity(Gravity.CENTER);
        delBtn.setBackground(makeRoundedRect(0xFFDC2626, dp(12), 0, 0));
        delBtn.setOnClickListener(v -> {
            dialog.dismiss();
            deleteFileItem(item);
        });
        LinearLayout.LayoutParams dLp = new LinearLayout.LayoutParams(0, dp(46), 1f);
        dLp.rightMargin = dp(6);
        row.addView(delBtn, dLp);

        box.addView(row);
        dialog.setContentView(box);
        styleDialogWindow(dialog);
        dialog.show();
    }

    private void deleteFileItem(FileItem item) {
        try {
            boolean deleted = DocumentsContract.deleteDocument(getContentResolver(), item.uri);
            if (deleted) {
                Toast.makeText(this, "تم حذف الملف", Toast.LENGTH_SHORT).show();
                if (activeFile != null && item.uri.equals(activeFile.uri)) {
                    activeFile = null;
                    currentScreen = ScreenState.HOME;
                    renderCurrentScreen();
                }
                refreshFileList();
            } else {
                Toast.makeText(this, "تعذر حذف الملف", Toast.LENGTH_SHORT).show();
            }
        } catch (Throwable e) {
            Toast.makeText(this, "خطأ أثناء الحذف: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void shareCurrentDocument() {
        if (activeFile == null) return;
        if (currentScreen == ScreenState.EDIT && documentEditor != null) {
            saveActiveDocument(false);
        }
        Intent sendIntent = new Intent(Intent.ACTION_SEND);
        sendIntent.setType("text/plain");
        sendIntent.putExtra(Intent.EXTRA_SUBJECT, activeFile.displayTitle);
        sendIntent.putExtra(Intent.EXTRA_TEXT, activeFileContent);
        startActivity(Intent.createChooser(sendIntent, "مشاركة النص عبر:"));
    }

    private void startBackupFlow() {
        if (allFiles.isEmpty()) {
            Toast.makeText(this, "لا توجد ملفات لعمل نسخة احتياطية لها", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmm", Locale.US);
            String zipName = "Mazwd_Backup_" + sdf.format(new Date()) + ".zip";
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("application/zip");
            intent.putExtra(Intent.EXTRA_TITLE, zipName);
            startActivityForResult(intent, REQ_CREATE_ZIP);
        } catch (Throwable e) {
            Toast.makeText(this, "تعذر إنشاء ملف النسخة الاحتياطية", Toast.LENGTH_SHORT).show();
        }
    }

    private void performZipExport(final Uri zipUri) {
        Toast.makeText(this, "جاري تجهيز النسخة الاحتياطية...", Toast.LENGTH_SHORT).show();
        ioExecutor.execute(() -> {
            int count = 0;
            try (OutputStream os = getContentResolver().openOutputStream(zipUri);
                 ZipOutputStream zos = new ZipOutputStream(os)) {
                for (FileItem item : allFiles) {
                    String content = item.cachedContent != null
                            ? item.cachedContent : readTextFromUri(item.uri);
                    ZipEntry entry = new ZipEntry(item.name);
                    zos.putNextEntry(entry);
                    byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
                    zos.write(bytes, 0, bytes.length);
                    zos.closeEntry();
                    count++;
                }
                zos.finish();
                final int finalCount = count;
                mainHandler.post(() -> Toast.makeText(this,
                        "تم حفظ النسخة الاحتياطية بنجاح (" + finalCount + " ملفات)",
                        Toast.LENGTH_LONG).show());
            } catch (Throwable e) {
                mainHandler.post(() -> Toast.makeText(this,
                        "فشل التصدير: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
    }

    private String readTextFromUri(Uri uri) {
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
        } catch (Throwable ignored) {
        }
        return sb.toString();
    }

    private boolean writeTextToUri(Uri uri, String content) {
        try (OutputStream os = getContentResolver().openOutputStream(uri, "wt")) {
            if (os == null) return false;
            os.write(content.getBytes(StandardCharsets.UTF_8));
            os.flush();
            return true;
        } catch (Throwable e) {
            return false;
        }
    }

    // =========================================================================
    // NAVIGATION & HELPERS
    // =========================================================================

    private void handleBackNavigation() {
        if (currentScreen == ScreenState.EDIT) {
            saveActiveDocument(true);
            hideKeyboard();
            currentScreen = ScreenState.READ;
            renderCurrentScreen();
        } else if (currentScreen == ScreenState.READ) {
            hideKeyboard();
            currentScreen = ScreenState.HOME;
            renderCurrentScreen();
        } else {
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        if (currentScreen != ScreenState.HOME) {
            handleBackNavigation();
        } else {
            super.onBackPressed();
        }
    }

    private Spannable applySearchHighlight(String fullText, String query) {
        SpannableString spannable = new SpannableString(fullText);
        if (query == null || query.isEmpty()) return spannable;

        String lowerFull = fullText.toLowerCase(Locale.ROOT);
        String lowerQuery = query.toLowerCase(Locale.ROOT);

        int start = 0;
        while (true) {
            int idx = lowerFull.indexOf(lowerQuery, start);
            if (idx < 0) break;
            int end = idx + lowerQuery.length();
            spannable.setSpan(new BackgroundColorSpan(COLOR_SEARCH_HIGHLIGHT),
                    idx, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannable.setSpan(new ForegroundColorSpan(Color.BLACK),
                    idx, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            start = end;
        }
        return spannable;
    }

    private String extractSearchSnippet(String content, String query) {
        String lowerContent = content.toLowerCase(Locale.ROOT);
        String lowerQuery = query.toLowerCase(Locale.ROOT);
        int idx = lowerContent.indexOf(lowerQuery);
        if (idx < 0) return null;

        int start = Math.max(0, idx - 35);
        int end = Math.min(content.length(), idx + query.length() + 45);
        String sub = content.substring(start, end).replace("\n", " ");
        return (start > 0 ? "..." : "") + sub + (end < content.length() ? "..." : "");
    }

    private int getTextDirectionGravity(String text) {
        if (text == null || text.isEmpty()) return Gravity.RIGHT;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.isLetter(c)) {
                byte dir = Character.getDirectionality(c);
                if (dir == Character.DIRECTIONALITY_LEFT_TO_RIGHT) {
                    return Gravity.LEFT;
                }
                return Gravity.RIGHT;
            }
        }
        return Gravity.RIGHT;
    }

    private String stripTxtExtension(String name) {
        if (name != null && name.toLowerCase(Locale.ROOT).endsWith(".txt")) {
            return name.substring(0, name.length() - 4);
        }
        return name != null ? name : "";
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " بايت";
        double kb = bytes / 1024.0;
        if (kb < 1024) return String.format(Locale.US, "%.1f ك.ب", kb);
        double mb = kb / 1024.0;
        return String.format(Locale.US, "%.2f م.ب", mb);
    }

    private Button createPrimaryButton(String text) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setAllCaps(false);
        btn.setTextColor(Color.WHITE);
        btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        btn.setTypeface(getAppTypeface(Typeface.BOLD));
        btn.setIncludeFontPadding(false);
        btn.setGravity(Gravity.CENTER);
        btn.setBackground(makeRoundedRect(currentTheme.primary, dp(12), 0, 0));
        btn.setPadding(dp(14), 0, dp(14), 0);
        return btn;
    }

    private Button createSecondaryButton(String text) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setAllCaps(false);
        btn.setTextColor(currentTheme.primaryDark);
        btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        btn.setTypeface(getAppTypeface(Typeface.BOLD));
        btn.setIncludeFontPadding(false);
        btn.setGravity(Gravity.CENTER);
        btn.setBackground(makeRoundedRect(currentTheme.accentLight, dp(12), COLOR_BORDER, dp(1)));
        btn.setPadding(dp(14), 0, dp(14), 0);
        return btn;
    }

    private GradientDrawable makeRoundedRect(int fillColor, float radius, int strokeColor, int strokeWidth) {
        GradientDrawable gd = new GradientDrawable();
        gd.setShape(GradientDrawable.RECTANGLE);
        gd.setColor(fillColor);
        gd.setCornerRadius(radius);
        if (strokeWidth > 0) {
            gd.setStroke(strokeWidth, strokeColor);
        }
        return gd;
    }

    private GradientDrawable makeCircle(int fillColor, int strokeColor, int strokeWidth) {
        GradientDrawable gd = new GradientDrawable();
        gd.setShape(GradientDrawable.OVAL);
        gd.setColor(fillColor);
        if (strokeWidth > 0) {
            gd.setStroke(strokeWidth, strokeColor);
        }
        return gd;
    }

    private void styleDialogWindow(Dialog dialog) {
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
            WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
            lp.copyFrom(window.getAttributes());
            lp.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.92f);
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
            window.setAttributes(lp);
        }
    }

    private void showKeyboard(View view) {
        try {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
            }
        } catch (Throwable ignored) {
        }
    }

    private void hideKeyboard() {
        try {
            View v = getCurrentFocus();
            if (v != null) {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private int dp(int value) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics()));
    }
}
