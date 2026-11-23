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
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.DocumentsContract;
import android.text.Editable;
import android.text.InputType;
import android.text.Layout;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class MainActivity extends Activity {

    private static final int REQ_PICK_FOLDER = 1001;
    private static final int REQ_CREATE_BACKUP_ZIP = 1002;
    private static final String PREFS_NAME = "mazwd_prefs";
    private static final String SAFE_VAULT_FOLDER_NAME = "خزنة_النسخ_الآمنة";

    private static final String DEFAULT_SIGNATURE = "أ. شفيقة مزود | باحثة في التراث الحضرمي";
    private static final String SIGNATURE_HINT = "اكتب توقيعك هنا\nمثال: عبدالله سعيد - مدير التحرير";

    private enum ScreenState { HOME, READ, EDIT }
    private ScreenState currentScreen = ScreenState.HOME;

    private SharedPreferences prefs;
    private Uri folderUri;

    private static class NoteFile {
        Uri uri;
        String name;
        long lastModified;
        long size;
        boolean pinned;
        String cachedContent;
        String matchSnippet;
    }

    private static class SafeSnapshot {
        Uri uri;
        String fileName;
        String displayDate;
        long lastModified;
        long size;
    }

    private final List<NoteFile> allFiles = new ArrayList<>();
    private final Map<String, String> contentCache = new HashMap<>();
    private NoteFile currentFile = null;
    private String currentSearchQuery = "";

    private final ArrayList<String> undoStack = new ArrayList<>();
    private int undoIndex = -1;
    private boolean isUndoRedoAction = false;
    private boolean isDirty = false;

    private FrameLayout rootContainer;
    private EditText editorEditText;
    private TextView readTextView;
    private ScrollView readScrollView;
    private TextView docTitleView;
    private TextView statsBarView;
    private TextView zoomPercentView;
    private LinearLayout editorToolbarContainer;
    private LinearLayout readActionBar;
    private LinearLayout homeFilesContainer;
    private EditText searchInput;

    // Fast Slider & Quick Jump views
    private FrameLayout sliderTouchContainer;
    private View sliderThumb;
    private TextView sliderBubble;
    private LinearLayout jumpMenuCard;
    private View jumpButton;
    private boolean isSliderDragging = false;

    private final Handler autoSaveHandler = new Handler(Looper.getMainLooper());
    private final Handler sliderFadeHandler = new Handler(Looper.getMainLooper());
    private final Runnable sliderFadeRunnable = () -> {
        if (sliderTouchContainer != null && !isSliderDragging) {
            sliderTouchContainer.animate().alpha(0.38f).setDuration(300).start();
        }
    };

    // 8 Luxury Themes: {Name, PrimaryHex, DarkHeaderHex, LightAccentHex}
    private static final String[][] THEMES = {
            {"الكحلي الملكي", "#1E3A8A", "#172554", "#DBEAFE"},
            {"الأخضر الزمردي", "#065F46", "#064E3B", "#D1FAE5"},
            {"العنابي الفاخر", "#881337", "#4C0519", "#FFE4E6"},
            {"البني التراثي", "#78350F", "#451A03", "#FEF3C7"},
            {"الطيني الحضرمي", "#9A3412", "#431407", "#FFEDD5"},
            {"البنفسجي الأنيق", "#5B21B6", "#2E1065", "#EDE9FE"},
            {"الرمادي الرسمي", "#334155", "#0F172A", "#E2E8F0"},
            {"التركواز الهادئ", "#115E59", "#042F2E", "#CCFBF1"}
    };

    // 7 Selectable Fonts
    private static final String[] FONT_NAMES = {
            "Sans — خط واضح وعصري (افتراضي)",
            "تجوال — خط حديث وأنيق للمقالات",
            "المراعي — خط عربي كلاسيكي مريح ومتوازن",
            "عارف رقعة — خط الرقعة العربي",
            "ريم كوفي — خط كوفي حديث ومميز",
            "Serif — خط إنجليزي رسمي ومريح",
            "Cursive — خط فني مائل"
    };

    // 12 Editor Tools (11 original + "find_replace" optional)
    private static final String[] TOOL_IDS = {
            "undo", "redo", "copy", "paste", "select_all",
            "auto_format", "signature", "cut", "select_up",
            "select_down", "datetime", "find_replace"
    };

    private static final String[] TOOL_LABELS = {
            "تراجع", "إعادة", "نسخ", "لصق", "تحديد الكل",
            "تنسيق آلي", "التوقيع", "قص", "تحديد لأعلى",
            "تحديد لأسفل", "التاريخ والوقت", "بحث واستبدال"
    };

    private static final String[] TOOL_ICON_RES = {
            "ic_undo", "ic_redo", "ic_copy", "ic_paste", "ic_select_all",
            "ic_auto_format", "ic_signature", "ic_cut", "ic_select_up",
            "ic_select_down", "ic_datetime", "ic_find_replace"
    };

    private static final Set<String> DEFAULT_ENABLED_TOOLS = new HashSet<>(Arrays.asList(
            "undo", "redo", "copy", "paste", "select_all", "auto_format", "signature"
    ));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        if (!prefs.getBoolean("sig_init_v3", false)) {
            prefs.edit()
                    .putString("custom_signature", DEFAULT_SIGNATURE)
                    .putBoolean("sig_init_v3", true)
                    .apply();
        }

        String savedUri = prefs.getString("folder_uri", null);
        if (savedUri != null) {
            try {
                folderUri = Uri.parse(savedUri);
            } catch (Exception ignored) {
                folderUri = null;
            }
        }

        rootContainer = new FrameLayout(this);
        rootContainer.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        rootContainer.setClipChildren(false);
        rootContainer.setClipToPadding(false);
        setContentView(rootContainer);

        updateStatusBarColor();
        showHomeScreen(true);
    }

    // ==================== DARK MODE & THEME HELPERS ====================

    private boolean isDarkMode() {
        return prefs.getBoolean("dark_mode", false);
    }

    private int getBgColor() {
        return isDarkMode() ? Color.parseColor("#0F172A") : Color.parseColor("#F4F6F9");
    }

    private int getCardBgColor() {
        return isDarkMode() ? Color.parseColor("#1E293B") : Color.WHITE;
    }

    private int getInputBgColor() {
        return isDarkMode() ? Color.parseColor("#0F172A") : Color.parseColor("#F8FAFC");
    }

    private int getSecondaryBtnBgColor() {
        return isDarkMode() ? Color.parseColor("#263449") : Color.parseColor("#F1F5F9");
    }

    private int getTextPrimaryColor() {
        return isDarkMode() ? Color.parseColor("#F1F5F9") : Color.parseColor("#1E293B");
    }

    private int getTextSecondaryColor() {
        return isDarkMode() ? Color.parseColor("#94A3B8") : Color.parseColor("#64748B");
    }

    private int getBorderColor() {
        return isDarkMode() ? Color.parseColor("#334155") : Color.parseColor("#E2E8F0");
    }

    private void updateStatusBarColor() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(getDarkHeaderColor());
            window.setNavigationBarColor(getCardBgColor());
        }
    }

    private int getThemeIndex() {
        int idx = prefs.getInt("theme_index", 0);
        if (idx < 0 || idx >= THEMES.length) return 0;
        return idx;
    }

    private int getPrimaryColor() {
        return Color.parseColor(THEMES[getThemeIndex()][1]);
    }

    private int getDarkHeaderColor() {
        return Color.parseColor(THEMES[getThemeIndex()][2]);
    }

    private int getLightAccentColor() {
        if (isDarkMode()) {
            return Color.parseColor("#283548");
        }
        return Color.parseColor(THEMES[getThemeIndex()][3]);
    }

    private int getAccentTextColor() {
        return isDarkMode() ? Color.parseColor("#E2E8F0") : getPrimaryColor();
    }

    private int getFontIndex() {
        int idx = prefs.getInt("font_index", 0);
        if (idx < 0 || idx >= FONT_NAMES.length) return 0;
        return idx;
    }

    private Typeface getAppTypeface(boolean bold) {
        return getTypefaceForIndex(getFontIndex(), bold);
    }

    private Typeface getTypefaceForIndex(int idx, boolean bold) {
        int style = bold ? Typeface.BOLD : Typeface.NORMAL;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                switch (idx) {
                    case 1: {
                        String resName = bold ? "tajawal_bold" : "tajawal_regular";
                        int id = getResources().getIdentifier(resName, "font", getPackageName());
                        if (id != 0) return getResources().getFont(id);
                        break;
                    }
                    case 2: {
                        String resName = bold ? "mazwd_arabic_bold" : "mazwd_arabic";
                        int id = getResources().getIdentifier(resName, "font", getPackageName());
                        if (id != 0) return getResources().getFont(id);
                        break;
                    }
                    case 3: {
                        String resName = bold ? "aref_ruqaa_bold" : "aref_ruqaa";
                        int id = getResources().getIdentifier(resName, "font", getPackageName());
                        if (id != 0) return getResources().getFont(id);
                        break;
                    }
                    case 4: {
                        String resName = bold ? "reem_kufi_bold" : "reem_kufi";
                        int id = getResources().getIdentifier(resName, "font", getPackageName());
                        if (id != 0) return TypedValueFont(id, style);
                        break;
                    }
                    case 5:
                        return Typeface.create(Typeface.SERIF, style);
                    case 6:
                        return Typeface.create("cursive", style);
                    default:
                        return Typeface.create("sans-serif", style);
                }
            }
        } catch (Exception ignored) {
        }
        return Typeface.create(Typeface.SANS_SERIF, style);
    }

    private Typeface TypedValueFont(int resId, int style) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Typeface base = getResources().getFont(resId);
            return Typeface.create(base, style);
        }
        return Typeface.create(Typeface.SANS_SERIF, style);
    }

    private int getZoomPercent() {
        return prefs.getInt("zoom_percent", 100);
    }

    private void setZoomPercent(int percent) {
        if (percent < 60) percent = 60;
        if (percent > 220) percent = 220;
        prefs.edit().putInt("zoom_percent", percent).apply();
        applyZoomToViews();
    }

    private float getScaledTextSize() {
        return 18f * (getZoomPercent() / 100f);
    }

    private void applyZoomToViews() {
        float size = getScaledTextSize();
        if (readTextView != null) {
            readTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, size);
            readTextView.setTypeface(getAppTypeface(false));
        }
        if (editorEditText != null) {
            editorEditText.setTextSize(TypedValue.COMPLEX_UNIT_SP, size);
            editorEditText.setTypeface(getAppTypeface(false));
        }
        if (zoomPercentView != null) {
            zoomPercentView.setText(getZoomPercent() + "%");
        }
        updateSliderPositionFromScroll();
    }

    private int dp(int value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics()
        );
    }

    private GradientDrawable roundedRect(int fillColor, int cornerRadiusDp, int strokeWidthDp, int strokeColor) {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(fillColor);
        gd.setCornerRadius(dp(cornerRadiusDp));
        if (strokeWidthDp > 0) {
            gd.setStroke(dp(strokeWidthDp), strokeColor);
        }
        return gd;
    }

    private void configureTextButton(Button btn, String text, float sizeSp, boolean bold, int textColor, int heightDp) {
        btn.setText(text);
        btn.setAllCaps(false);
        btn.setIncludeFontPadding(false);
        btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp);
        btn.setTypeface(getAppTypeface(bold));
        btn.setTextColor(textColor);
        btn.setGravity(Gravity.CENTER);
        btn.setPadding(dp(12), 0, dp(12), 0);
        if (heightDp > 0) {
            btn.setMinHeight(dp(heightDp));
            btn.setMinimumHeight(dp(heightDp));
        }
    }

    private int getDrawableId(String name) {
        return getResources().getIdentifier(name, "drawable", getPackageName());
    }

    // ==================== 1. HOME SCREEN ====================

    private void showHomeScreen(boolean reloadFiles) {
        currentScreen = ScreenState.HOME;
        hideKeyboard();
        updateStatusBarColor();
        rootContainer.removeAllViews();
        rootContainer.setBackgroundColor(getBgColor());

        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        mainLayout.setBackgroundColor(getBgColor());
        mainLayout.setClipChildren(false);
        mainLayout.setClipToPadding(false);
        rootContainer.addView(mainLayout, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // --- TOP HERO HEADER CARD ---
        LinearLayout heroCard = new LinearLayout(this);
        heroCard.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable heroBg = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{getDarkHeaderColor(), getPrimaryColor()}
        );
        float r = dp(24);
        heroBg.setCornerRadii(new float[]{0, 0, 0, 0, r, r, r, r});
        heroCard.setBackground(heroBg);
        heroCard.setPadding(dp(20), dp(22), dp(20), dp(22));
        heroCard.setElevation(dp(6));

        TextView titleView = new TextView(this);
        titleView.setText("مفكرة أ. شفيقة مزود");
        titleView.setTextColor(Color.WHITE);
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 23);
        titleView.setTypeface(getAppTypeface(true));
        titleView.setIncludeFontPadding(false);
        titleView.setGravity(Gravity.RIGHT);
        heroCard.addView(titleView);

        TextView subtitleView = new TextView(this);
        subtitleView.setText("كاتبة وباحثة في التراث الحضرمي");
        subtitleView.setTextColor(Color.parseColor("#E2E8F0"));
        subtitleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        subtitleView.setTypeface(getAppTypeface(false));
        subtitleView.setIncludeFontPadding(false);
        subtitleView.setGravity(Gravity.RIGHT);
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        subLp.topMargin = dp(4);
        subLp.bottomMargin = dp(16);
        heroCard.addView(subtitleView, subLp);

        Button newFileBtn = new Button(this);
        configureTextButton(newFileBtn, "＋  ملف جديد", 16, true, getPrimaryColor(), 46);
        newFileBtn.setBackground(roundedRect(Color.WHITE, 14, 0, 0));
        newFileBtn.setElevation(dp(2));
        newFileBtn.setOnClickListener(v -> onNewFileClicked());
        LinearLayout.LayoutParams newBtnLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(46));
        heroCard.addView(newFileBtn, newBtnLp);

        mainLayout.addView(heroCard, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // --- SECTION HEADER ("ملفاتي النصية" + "TXT" badge) ---
        LinearLayout sectionHeader = new LinearLayout(this);
        sectionHeader.setOrientation(LinearLayout.HORIZONTAL);
        sectionHeader.setGravity(Gravity.CENTER_VERTICAL);
        sectionHeader.setPadding(dp(18), dp(14), dp(18), dp(6));

        TextView myFilesTitle = new TextView(this);
        myFilesTitle.setText("ملفاتي النصية");
        myFilesTitle.setTextColor(getTextPrimaryColor());
        myFilesTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        myFilesTitle.setTypeface(getAppTypeface(true));
        myFilesTitle.setIncludeFontPadding(false);
        LinearLayout.LayoutParams mftLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        sectionHeader.addView(myFilesTitle, mftLp);

        TextView txtBadge = new TextView(this);
        txtBadge.setText("TXT");
        txtBadge.setTextColor(getAccentTextColor());
        txtBadge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        txtBadge.setTypeface(Typeface.DEFAULT_BOLD);
        txtBadge.setIncludeFontPadding(false);
        txtBadge.setBackground(roundedRect(getLightAccentColor(), 8, 0, 0));
        txtBadge.setPadding(dp(10), dp(4), dp(10), dp(4));
        sectionHeader.addView(txtBadge);

        mainLayout.addView(sectionHeader);

        // --- UNIFIED SEARCH BAR ---
        LinearLayout searchBox = new LinearLayout(this);
        searchBox.setOrientation(LinearLayout.HORIZONTAL);
        searchBox.setGravity(Gravity.CENTER_VERTICAL);
        searchBox.setBackground(roundedRect(getCardBgColor(), 14, 1, getBorderColor()));
        searchBox.setPadding(dp(14), dp(2), dp(14), dp(2));
        searchBox.setElevation(dp(1));
        LinearLayout.LayoutParams searchBoxLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(48));
        searchBoxLp.setMargins(dp(16), dp(4), dp(16), dp(8));

        TextView searchIcon = new TextView(this);
        searchIcon.setText("🔍");
        searchIcon.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        searchBox.addView(searchIcon);

        searchInput = new EditText(this);
        searchInput.setHint("ابحث في أسماء ومحتوى ملفاتك...");
        searchInput.setHintTextColor(getTextSecondaryColor());
        searchInput.setTextColor(getTextPrimaryColor());
        searchInput.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        searchInput.setTypeface(getAppTypeface(false));
        searchInput.setIncludeFontPadding(false);
        searchInput.setBackgroundColor(Color.TRANSPARENT);
        searchInput.setSingleLine(true);
        searchInput.setPadding(dp(8), 0, dp(8), 0);
        searchInput.setText(currentSearchQuery);

        TextView clearSearchBtn = new TextView(this);
        clearSearchBtn.setText("✕");
        clearSearchBtn.setTextColor(getTextSecondaryColor());
        clearSearchBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        clearSearchBtn.setPadding(dp(8), dp(4), dp(4), dp(4));
        clearSearchBtn.setVisibility(currentSearchQuery.isEmpty() ? View.GONE : View.VISIBLE);
        clearSearchBtn.setOnClickListener(v -> {
            searchInput.setText("");
            currentSearchQuery = "";
            renderHomeFileList();
        });

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s.toString().trim();
                clearSearchBtn.setVisibility(currentSearchQuery.isEmpty() ? View.GONE : View.VISIBLE);
                renderHomeFileList();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        LinearLayout.LayoutParams siLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
        searchBox.addView(searchInput, siLp);
        searchBox.addView(clearSearchBtn);
        mainLayout.addView(searchBox, searchBoxLp);

        // --- SCROLLABLE FILES LIST ---
        ScrollView filesScroll = new ScrollView(this);
        filesScroll.setFillViewport(true);
        homeFilesContainer = new LinearLayout(this);
        homeFilesContainer.setOrientation(LinearLayout.VERTICAL);
        homeFilesContainer.setPadding(dp(16), dp(4), dp(16), dp(16));
        filesScroll.addView(homeFilesContainer);

        LinearLayout.LayoutParams scrollLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        mainLayout.addView(filesScroll, scrollLp);

        // --- BOTTOM BAR (Lifted slightly so bottom rounded corners are 100% visible) ---
        LinearLayout bottomBar = new LinearLayout(this);
        bottomBar.setOrientation(LinearLayout.HORIZONTAL);
        bottomBar.setGravity(Gravity.CENTER_VERTICAL);
        bottomBar.setBackgroundColor(getCardBgColor());
        bottomBar.setPadding(dp(14), dp(10), dp(14), dp(14));
        bottomBar.setElevation(dp(10));
        bottomBar.setClipToPadding(false);
        bottomBar.setClipChildren(false);

        Button settingsBtn = new Button(this);
        configureTextButton(settingsBtn, "⚙  الإعدادات", 14, true, getTextPrimaryColor(), 46);
        settingsBtn.setBackground(roundedRect(getSecondaryBtnBgColor(), 12, 1, getBorderColor()));
        settingsBtn.setStateListAnimator(null);
        settingsBtn.setElevation(dp(1));
        settingsBtn.setOnClickListener(v -> showSettingsDialog());
        LinearLayout.LayoutParams b1Lp = new LinearLayout.LayoutParams(0, dp(46), 1f);
        b1Lp.leftMargin = dp(6);
        b1Lp.topMargin = dp(2);
        b1Lp.bottomMargin = dp(3);
        bottomBar.addView(settingsBtn, b1Lp);

        Button backupBtn = new Button(this);
        configureTextButton(backupBtn, "نسخ احتياطي (ZIP)", 14, true, Color.WHITE, 46);
        backupBtn.setBackground(roundedRect(getPrimaryColor(), 12, 0, 0));
        backupBtn.setStateListAnimator(null);
        backupBtn.setElevation(dp(2));
        backupBtn.setOnClickListener(v -> startBackupZipFlow());
        LinearLayout.LayoutParams b2Lp = new LinearLayout.LayoutParams(0, dp(46), 1f);
        b2Lp.rightMargin = dp(6);
        b2Lp.topMargin = dp(2);
        b2Lp.bottomMargin = dp(3);
        bottomBar.addView(backupBtn, b2Lp);

        mainLayout.addView(bottomBar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        if (reloadFiles) {
            loadFilesFromFolder();
        } else {
            renderHomeFileList();
        }
    }

    private void loadFilesFromFolder() {
        allFiles.clear();
        if (folderUri == null) {
            renderHomeFileList();
            return;
        }

        Set<String> pinnedSet = prefs.getStringSet("pinned_files", new HashSet<>());
        try {
            String treeDocId = DocumentsContract.getTreeDocumentId(folderUri);
            Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(folderUri, treeDocId);

            String[] projection = new String[]{
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_MIME_TYPE,
                    DocumentsContract.Document.COLUMN_LAST_MODIFIED,
                    DocumentsContract.Document.COLUMN_SIZE
            };

            try (Cursor c = getContentResolver().query(childrenUri, projection, null, null, null)) {
                if (c != null) {
                    while (c.moveToNext()) {
                        String docId = c.getString(0);
                        String name = c.getString(1);
                        String mime = c.getString(2);
                        long modified = c.isNull(3) ? 0L : c.getLong(3);
                        long size = c.isNull(4) ? 0L : c.getLong(4);

                        if (name == null) continue;
                        if (DocumentsContract.Document.MIME_TYPE_DIR.equals(mime)) continue;
                        if (!name.toLowerCase(Locale.ROOT).endsWith(".txt")) continue;

                        Uri fileUri = DocumentsContract.buildDocumentUriUsingTree(folderUri, docId);
                        NoteFile nf = new NoteFile();
                        nf.uri = fileUri;
                        nf.name = name;
                        nf.lastModified = modified;
                        nf.size = size;
                        nf.pinned = pinnedSet.contains(fileUri.toString());
                        nf.cachedContent = contentCache.get(fileUri.toString());
                        allFiles.add(nf);
                    }
                }
            }
        } catch (Exception e) {
            folderUri = null;
        }

        Collections.sort(allFiles, (a, b) -> {
            if (a.pinned != b.pinned) return a.pinned ? -1 : 1;
            return Long.compare(b.lastModified, a.lastModified);
        });

        renderHomeFileList();
    }

    private void renderHomeFileList() {
        if (homeFilesContainer == null) return;
        homeFilesContainer.removeAllViews();

        if (folderUri == null) {
            LinearLayout emptyCard = new LinearLayout(this);
            emptyCard.setOrientation(LinearLayout.VERTICAL);
            emptyCard.setGravity(Gravity.CENTER);
            emptyCard.setBackground(roundedRect(getCardBgColor(), 18, 1, getBorderColor()));
            emptyCard.setPadding(dp(24), dp(32), dp(24), dp(32));

            TextView msg = new TextView(this);
            msg.setText("مرحباً بك في مفكرة أ. شفيقة مزود\n\nيرجى اختيار مجلد على هاتفك لحفظ وقراءة ملفاتك النصية (.txt) بأمان.");
            msg.setTextColor(getTextPrimaryColor());
            msg.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            msg.setTypeface(getAppTypeface(false));
            msg.setGravity(Gravity.CENTER);
            msg.setLineSpacing(dp(4), 1.1f);
            emptyCard.addView(msg);

            Button pickBtn = new Button(this);
            configureTextButton(pickBtn, "📁  اختيار مجلد الملفات", 15, true, Color.WHITE, 46);
            pickBtn.setBackground(roundedRect(getPrimaryColor(), 12, 0, 0));
            LinearLayout.LayoutParams pLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, dp(46));
            pLp.topMargin = dp(18);
            pickBtn.setPadding(dp(24), 0, dp(24), 0);
            pickBtn.setOnClickListener(v -> pickFolder());
            emptyCard.addView(pickBtn, pLp);

            homeFilesContainer.addView(emptyCard);
            return;
        }

        List<NoteFile> filtered = new ArrayList<>();
        String q = currentSearchQuery.toLowerCase(Locale.ROOT);

        for (NoteFile nf : allFiles) {
            nf.matchSnippet = null;
            if (q.isEmpty()) {
                filtered.add(nf);
            } else {
                String cleanName = stripTxtExtension(nf.name);
                boolean nameMatches = cleanName.toLowerCase(Locale.ROOT).contains(q);

                if (nf.cachedContent == null) {
                    nf.cachedContent = readFileContent(nf.uri);
                    contentCache.put(nf.uri.toString(), nf.cachedContent);
                }
                String body = nf.cachedContent != null ? nf.cachedContent : "";
                int idx = body.toLowerCase(Locale.ROOT).indexOf(q);
                boolean contentMatches = idx >= 0;

                if (contentMatches) {
                    int start = Math.max(0, idx - 30);
                    int end = Math.min(body.length(), idx + q.length() + 45);
                    String snippet = body.substring(start, end).replace("\n", " ").trim();
                    if (start > 0) snippet = "..." + snippet;
                    if (end < body.length()) snippet = snippet + "...";
                    nf.matchSnippet = snippet;
                }

                if (nameMatches || contentMatches) {
                    filtered.add(nf);
                }
            }
        }

        if (filtered.isEmpty()) {
            TextView emptyText = new TextView(this);
            emptyText.setText(q.isEmpty()
                    ? "لا توجد ملفات نصية في هذا المجلد حتى الآن.\nاضغط على (＋ ملف جديد) بالأعلى للبدء."
                    : "لا توجد نتائج مطابقة للبحث: \"" + currentSearchQuery + "\"");
            emptyText.setTextColor(getTextSecondaryColor());
            emptyText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
            emptyText.setTypeface(getAppTypeface(false));
            emptyText.setGravity(Gravity.CENTER);
            emptyText.setPadding(dp(20), dp(44), dp(20), dp(44));
            homeFilesContainer.addView(emptyText);
            return;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd • hh:mm a", new Locale("ar"));

        for (NoteFile nf : filtered) {
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.HORIZONTAL);
            card.setGravity(Gravity.CENTER_VERTICAL);
            int strokeColor = nf.pinned ? getPrimaryColor() : getBorderColor();
            int strokeWidth = nf.pinned ? 2 : 1;
            card.setBackground(roundedRect(getCardBgColor(), 14, strokeWidth, strokeColor));
            card.setPadding(dp(14), dp(12), dp(10), dp(12));
            card.setElevation(dp(2));

            LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            cardLp.bottomMargin = dp(10);

            LinearLayout infoCol = new LinearLayout(this);
            infoCol.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams infoLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);

            String displayTitle = (nf.pinned ? "📌 " : "") + stripTxtExtension(nf.name);
            TextView nameView = new TextView(this);
            if (!q.isEmpty()) {
                nameView.setText(highlightSearchMatches(displayTitle, currentSearchQuery));
            } else {
                nameView.setText(displayTitle);
            }
            nameView.setTextColor(getTextPrimaryColor());
            nameView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            nameView.setTypeface(getAppTypeface(true));
            nameView.setIncludeFontPadding(false);
            nameView.setTextDirection(View.TEXT_DIRECTION_ANY_RTL);
            infoCol.addView(nameView);

            String dateStr = nf.lastModified > 0 ? sdf.format(new Date(nf.lastModified)) : "";
            String sizeStr = formatSize(nf.size);
            TextView metaView = new TextView(this);
            metaView.setText(dateStr + "   •   " + sizeStr);
            metaView.setTextColor(getTextSecondaryColor());
            metaView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            metaView.setTypeface(getAppTypeface(false));
            metaView.setIncludeFontPadding(false);
            metaView.setGravity(Gravity.RIGHT);
            LinearLayout.LayoutParams metaLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            metaLp.topMargin = dp(4);
            infoCol.addView(metaView, metaLp);

            if (nf.matchSnippet != null && !q.isEmpty()) {
                TextView snippetView = new TextView(this);
                snippetView.setText(highlightSearchMatches(nf.matchSnippet, currentSearchQuery));
                snippetView.setTextColor(getTextPrimaryColor());
                snippetView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
                snippetView.setTypeface(getAppTypeface(false));
                snippetView.setBackground(roundedRect(getInputBgColor(), 8, 1, getBorderColor()));
                snippetView.setPadding(dp(8), dp(4), dp(8), dp(4));
                LinearLayout.LayoutParams snipLp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                snipLp.topMargin = dp(6);
                infoCol.addView(snippetView, snipLp);
            }

            card.addView(infoCol, infoLp);

            // 3-Dots Button
            FrameLayout moreBtn = new FrameLayout(this);
            moreBtn.setBackground(roundedRect(getSecondaryBtnBgColor(), 10, 0, 0));
            ImageView moreIcon = new ImageView(this);
            int moreDrawableId = getDrawableId("ic_more_vert");
            if (moreDrawableId != 0) {
                moreIcon.setImageResource(moreDrawableId);
                moreIcon.setImageTintList(ColorStateList.valueOf(getTextPrimaryColor()));
            }
            FrameLayout.LayoutParams iconLp = new FrameLayout.LayoutParams(dp(20), dp(20), Gravity.CENTER);
            moreBtn.addView(moreIcon, iconLp);

            LinearLayout.LayoutParams moreLp = new LinearLayout.LayoutParams(dp(38), dp(38));
            moreLp.rightMargin = dp(8);
            moreBtn.setOnClickListener(v -> showFileOptionsMenu(nf));
            card.addView(moreBtn, moreLp);

            card.setOnClickListener(v -> openNoteFile(nf, ScreenState.READ));
            homeFilesContainer.addView(card, cardLp);
        }
    }

    private Spannable highlightSearchMatches(String text, String query) {
        SpannableString span = new SpannableString(text);
        if (query == null || query.isEmpty()) return span;
        String lowerText = text.toLowerCase(Locale.ROOT);
        String lowerQuery = query.toLowerCase(Locale.ROOT);
        int idx = 0;
        while ((idx = lowerText.indexOf(lowerQuery, idx)) >= 0) {
            span.setSpan(
                    new BackgroundColorSpan(0xFFFDE047),
                    idx,
                    idx + lowerQuery.length(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            );
            span.setSpan(
                    new ForegroundColorSpan(Color.parseColor("#0F172A")),
                    idx,
                    idx + lowerQuery.length(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            );
            idx += lowerQuery.length();
        }
        return span;
    }

    // ==================== 2. DOCUMENT SCREEN (READ & EDIT + SLIDER + QUICK JUMP) ====================

    private void openNoteFile(NoteFile file, ScreenState initialMode) {
        currentFile = file;
        String content = readFileContent(file.uri);
        contentCache.put(file.uri.toString(), content);

        undoStack.clear();
        undoStack.add(content);
        undoIndex = 0;
        isDirty = false;

        buildDocumentView(content, initialMode);
    }

    private void buildDocumentView(String content, ScreenState mode) {
        currentScreen = mode;
        updateStatusBarColor();
        rootContainer.removeAllViews();
        rootContainer.setBackgroundColor(getBgColor());

        LinearLayout docLayout = new LinearLayout(this);
        docLayout.setOrientation(LinearLayout.VERTICAL);
        docLayout.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        docLayout.setBackgroundColor(getBgColor());
        docLayout.setClipChildren(false);
        docLayout.setClipToPadding(false);
        rootContainer.addView(docLayout, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // --- 1. CLEAN TOP BAR (Back Button + Document Title + Zoom Capsule) ---
        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        topBar.setBackgroundColor(getDarkHeaderColor());
        topBar.setPadding(dp(10), dp(10), dp(10), dp(10));
        topBar.setElevation(dp(4));

        Button backBtn = new Button(this);
        configureTextButton(backBtn, "→ رجوع", 13, true, Color.WHITE, 36);
        backBtn.setBackground(roundedRect(Color.parseColor("#33FFFFFF"), 10, 0, 0));
        backBtn.setPadding(dp(12), 0, dp(12), 0);
        backBtn.setOnClickListener(v -> handleBackNavigation());
        topBar.addView(backBtn, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(36)));

        docTitleView = new TextView(this);
        docTitleView.setText(stripTxtExtension(currentFile.name));
        docTitleView.setTextColor(Color.WHITE);
        docTitleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        docTitleView.setTypeface(getAppTypeface(true));
        docTitleView.setIncludeFontPadding(false);
        docTitleView.setSingleLine(true);
        docTitleView.setEllipsize(TextUtils.TruncateAt.END);
        docTitleView.setGravity(Gravity.CENTER);
        docTitleView.setPadding(dp(8), dp(4), dp(8), dp(4));
        docTitleView.setOnClickListener(v -> showRenameDialog(currentFile));
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        topBar.addView(docTitleView, titleLp);

        // Zoom Capsule [ ＋ | 100% | － ]
        LinearLayout zoomBar = new LinearLayout(this);
        zoomBar.setOrientation(LinearLayout.HORIZONTAL);
        zoomBar.setGravity(Gravity.CENTER_VERTICAL);
        zoomBar.setBackground(roundedRect(Color.parseColor("#28FFFFFF"), 10, 0, 0));
        zoomBar.setPadding(dp(2), dp(2), dp(2), dp(2));

        TextView zoomInBtn = new TextView(this);
        zoomInBtn.setText("＋");
        zoomInBtn.setTextColor(Color.WHITE);
        zoomInBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        zoomInBtn.setTypeface(Typeface.DEFAULT_BOLD);
        zoomInBtn.setGravity(Gravity.CENTER);
        zoomInBtn.setPadding(dp(8), dp(2), dp(8), dp(2));
        zoomInBtn.setOnClickListener(v -> setZoomPercent(getZoomPercent() + 5));

        zoomPercentView = new TextView(this);
        zoomPercentView.setText(getZoomPercent() + "%");
        zoomPercentView.setTextColor(Color.WHITE);
        zoomPercentView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        zoomPercentView.setTypeface(Typeface.DEFAULT_BOLD);
        zoomPercentView.setGravity(Gravity.CENTER);
        zoomPercentView.setPadding(dp(4), dp(2), dp(4), dp(2));
        zoomPercentView.setOnClickListener(v -> showResetZoomDialog());

        TextView zoomOutBtn = new TextView(this);
        zoomOutBtn.setText("－");
        zoomOutBtn.setTextColor(Color.WHITE);
        zoomOutBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        zoomOutBtn.setTypeface(Typeface.DEFAULT_BOLD);
        zoomOutBtn.setGravity(Gravity.CENTER);
        zoomOutBtn.setPadding(dp(8), dp(2), dp(8), dp(2));
        zoomOutBtn.setOnClickListener(v -> setZoomPercent(getZoomPercent() - 5));

        zoomBar.addView(zoomInBtn);
        zoomBar.addView(zoomPercentView);
        zoomBar.addView(zoomOutBtn);
        topBar.addView(zoomBar);

        docLayout.addView(topBar);

        // --- 2. EDITOR TOOLBAR (Visible in EDIT mode) ---
        HorizontalScrollView toolbarScroll = new HorizontalScrollView(this);
        toolbarScroll.setHorizontalScrollBarEnabled(false);
        toolbarScroll.setBackgroundColor(getCardBgColor());
        toolbarScroll.setElevation(dp(2));

        editorToolbarContainer = new LinearLayout(this);
        editorToolbarContainer.setOrientation(LinearLayout.HORIZONTAL);
        editorToolbarContainer.setGravity(Gravity.CENTER_VERTICAL);
        editorToolbarContainer.setPadding(dp(8), dp(6), dp(8), dp(6));
        toolbarScroll.addView(editorToolbarContainer);
        populateEditorToolbar();

        docLayout.addView(toolbarScroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // --- 3. DOCUMENT PAPER AREA + LEFT SLIDER + QUICK JUMP BUTTON ---
        FrameLayout paperCard = new FrameLayout(this);
        paperCard.setBackground(roundedRect(getCardBgColor(), 14, 1, getBorderColor()));
        paperCard.setElevation(dp(1));
        LinearLayout.LayoutParams paperLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        paperLp.setMargins(dp(10), dp(8), dp(10), dp(8));

        // READ ScrollView
        readScrollView = new ScrollView(this);
        readScrollView.setFillViewport(true);
        readTextView = new TextView(this);
        readTextView.setTextColor(getTextPrimaryColor());
        readTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, getScaledTextSize());
        readTextView.setTypeface(getAppTypeface(false));
        readTextView.setLineSpacing(dp(6), 1.15f);
        readTextView.setPadding(dp(28), dp(18), dp(18), dp(28));
        readTextView.setTextIsSelectable(true);
        readTextView.setTextDirection(View.TEXT_DIRECTION_ANY_RTL);
        readScrollView.addView(readTextView);

        // EDIT EditText
        editorEditText = new EditText(this);
        editorEditText.setText(content);
        editorEditText.setTextColor(getTextPrimaryColor());
        editorEditText.setTextSize(TypedValue.COMPLEX_UNIT_SP, getScaledTextSize());
        editorEditText.setTypeface(getAppTypeface(false));
        editorEditText.setLineSpacing(dp(6), 1.15f);
        editorEditText.setGravity(Gravity.TOP | Gravity.RIGHT);
        editorEditText.setTextDirection(View.TEXT_DIRECTION_ANY_RTL);
        editorEditText.setBackgroundColor(Color.TRANSPARENT);
        editorEditText.setPadding(dp(28), dp(18), dp(18), dp(28));
        editorEditText.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);

        editorEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                String current = s.toString();
                updateStatsBar(current);
                isDirty = true;
                if (!isUndoRedoAction) {
                    recordUndoState(current);
                }
                scheduleAutoSave();
                updateSliderPositionFromScroll();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        paperCard.addView(readScrollView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        paperCard.addView(editorEditText, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // --- LEFT FAST SCROLL SLIDER ---
        buildLeftFastSlider(paperCard);

        // --- QUICK JUMP FLOATING BUTTON & MINI MENU (LIKE TEXTPAD) ---
        buildQuickJumpOverlay(paperCard);

        docLayout.addView(paperCard, paperLp);

        // --- 4. BOTTOM STATS BAR + READ ACTION BAR ---
        LinearLayout bottomControlsContainer = new LinearLayout(this);
        bottomControlsContainer.setOrientation(LinearLayout.VERTICAL);
        bottomControlsContainer.setBackgroundColor(getCardBgColor());
        bottomControlsContainer.setElevation(dp(8));
        bottomControlsContainer.setClipChildren(false);
        bottomControlsContainer.setClipToPadding(false);

        statsBarView = new TextView(this);
        statsBarView.setTextColor(getTextSecondaryColor());
        statsBarView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        statsBarView.setTypeface(getAppTypeface(true));
        statsBarView.setIncludeFontPadding(false);
        statsBarView.setGravity(Gravity.CENTER);
        statsBarView.setPadding(dp(12), dp(8), dp(12), dp(6));
        boolean showStats = prefs.getBoolean("show_stats_bar", true);
        statsBarView.setVisibility(showStats ? View.VISIBLE : View.GONE);
        bottomControlsContainer.addView(statsBarView);

        readActionBar = new LinearLayout(this);
        readActionBar.setOrientation(LinearLayout.HORIZONTAL);
        readActionBar.setGravity(Gravity.CENTER_VERTICAL);
        readActionBar.setPadding(dp(14), dp(6), dp(14), dp(14));
        readActionBar.setClipChildren(false);
        readActionBar.setClipToPadding(false);

        Button editModeBtn = new Button(this);
        configureTextButton(editModeBtn, "تحرير الملف", 15, true, Color.WHITE, 46);
        editModeBtn.setBackground(roundedRect(getPrimaryColor(), 12, 0, 0));
        editModeBtn.setStateListAnimator(null);
        editModeBtn.setElevation(dp(2));
        editModeBtn.setOnClickListener(v -> switchDocumentMode(ScreenState.EDIT));
        LinearLayout.LayoutParams ebLp = new LinearLayout.LayoutParams(0, dp(46), 1.3f);
        ebLp.leftMargin = dp(6);
        ebLp.topMargin = dp(2);
        ebLp.bottomMargin = dp(3);
        readActionBar.addView(editModeBtn, ebLp);

        Button shareFileBtn = new Button(this);
        configureTextButton(shareFileBtn, "مشاركة الملف", 14, true, getTextPrimaryColor(), 46);
        shareFileBtn.setBackground(roundedRect(getSecondaryBtnBgColor(), 12, 1, getBorderColor()));
        shareFileBtn.setStateListAnimator(null);
        shareFileBtn.setElevation(dp(1));
        shareFileBtn.setOnClickListener(v -> shareTextContent(currentFile.name, editorEditText.getText().toString()));
        LinearLayout.LayoutParams sbLp = new LinearLayout.LayoutParams(0, dp(46), 1f);
        sbLp.rightMargin = dp(6);
        sbLp.topMargin = dp(2);
        sbLp.bottomMargin = dp(3);
        readActionBar.addView(shareFileBtn, sbLp);

        bottomControlsContainer.addView(readActionBar);
        docLayout.addView(bottomControlsContainer, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        updateStatsBar(content);
        switchDocumentMode(mode);
    }

    // ==================== LEFT FAST SCROLL SLIDER ====================

    private void buildLeftFastSlider(FrameLayout paperCard) {
        sliderTouchContainer = new FrameLayout(this);
        sliderTouchContainer.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        sliderTouchContainer.setAlpha(0.38f);

        // Subtle vertical track line
        View trackLine = new View(this);
        trackLine.setBackground(roundedRect(getBorderColor(), 2, 0, 0));
        FrameLayout.LayoutParams trackLp = new FrameLayout.LayoutParams(
                dp(3), ViewGroup.LayoutParams.MATCH_PARENT, Gravity.CENTER_HORIZONTAL);
        trackLp.topMargin = dp(14);
        trackLp.bottomMargin = dp(14);
        sliderTouchContainer.addView(trackLine, trackLp);

        // Draggable Pill Thumb
        sliderThumb = new View(this);
        sliderThumb.setBackground(roundedRect(getPrimaryColor(), 6, 1, Color.WHITE));
        sliderThumb.setElevation(dp(3));
        FrameLayout.LayoutParams thumbLp = new FrameLayout.LayoutParams(
                dp(8), dp(46), Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        thumbLp.topMargin = dp(14);
        sliderTouchContainer.addView(sliderThumb, thumbLp);

        FrameLayout.LayoutParams containerLp = new FrameLayout.LayoutParams(
                dp(26), ViewGroup.LayoutParams.MATCH_PARENT, Gravity.LEFT | Gravity.TOP);
        paperCard.addView(sliderTouchContainer, containerLp);

        // Percentage Bubble next to thumb
        sliderBubble = new TextView(this);
        sliderBubble.setText("0%");
        sliderBubble.setTextColor(Color.WHITE);
        sliderBubble.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        sliderBubble.setTypeface(Typeface.DEFAULT_BOLD);
        sliderBubble.setGravity(Gravity.CENTER);
        sliderBubble.setBackground(roundedRect(getPrimaryColor(), 10, 0, 0));
        sliderBubble.setPadding(dp(10), dp(4), dp(10), dp(4));
        sliderBubble.setElevation(dp(6));
        sliderBubble.setVisibility(View.GONE);
        FrameLayout.LayoutParams bubbleLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.LEFT | Gravity.TOP);
        bubbleLp.leftMargin = dp(32);
        bubbleLp.topMargin = dp(14);
        paperCard.addView(sliderBubble, bubbleLp);

        boolean showSlider = prefs.getBoolean("show_fast_slider", true);
        sliderTouchContainer.setVisibility(showSlider ? View.VISIBLE : View.GONE);

        sliderTouchContainer.setOnTouchListener((v, event) -> {
            if (jumpMenuCard != null) jumpMenuCard.setVisibility(View.GONE);
            int action = event.getActionMasked();
            float pad = dp(14);
            float thumbH = dp(46);
            float usableH = Math.max(1f, v.getHeight() - (2f * pad) - thumbH);

            if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE) {
                isSliderDragging = true;
                sliderFadeHandler.removeCallbacks(sliderFadeRunnable);
                sliderTouchContainer.setAlpha(1.0f);

                float y = event.getY() - pad - (thumbH / 2f);
                float fraction = Math.max(0f, Math.min(1f, y / usableH));
                sliderThumb.setTranslationY(fraction * usableH);

                if (sliderBubble != null) {
                    sliderBubble.setText(Math.round(fraction * 100f) + "%");
                    sliderBubble.setVisibility(View.VISIBLE);
                    sliderBubble.setTranslationY(fraction * usableH);
                }

                scrollDocumentToFraction(fraction);
                v.getParent().requestDisallowInterceptTouchEvent(true);
                return true;
            } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                isSliderDragging = false;
                if (sliderBubble != null) sliderBubble.setVisibility(View.GONE);
                sliderFadeHandler.postDelayed(sliderFadeRunnable, 1400);
                return true;
            }
            return false;
        });

        readScrollView.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
            if (currentScreen == ScreenState.READ && !isSliderDragging) {
                updateSliderPositionFromScroll();
            }
            if (jumpMenuCard != null && jumpMenuCard.getVisibility() == View.VISIBLE) {
                jumpMenuCard.setVisibility(View.GONE);
            }
        });

        editorEditText.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
            if (currentScreen == ScreenState.EDIT && !isSliderDragging) {
                updateSliderPositionFromScroll();
            }
            if (jumpMenuCard != null && jumpMenuCard.getVisibility() == View.VISIBLE) {
                jumpMenuCard.setVisibility(View.GONE);
            }
        });
    }

    private int getReadMaxScroll() {
        if (readScrollView == null || readTextView == null) return 0;
        return Math.max(0, readTextView.getHeight() - readScrollView.getHeight());
    }

    private int getEditorMaxScroll() {
        if (editorEditText == null) return 0;
        Layout layout = editorEditText.getLayout();
        if (layout == null) return 0;
        int totalH = layout.getHeight() + editorEditText.getTotalPaddingTop() + editorEditText.getTotalPaddingBottom();
        return Math.max(0, totalH - editorEditText.getHeight());
    }

    private void scrollDocumentToFraction(float fraction) {
        if (currentScreen == ScreenState.READ) {
            int max = getReadMaxScroll();
            readScrollView.scrollTo(0, Math.round(fraction * max));
        } else if (currentScreen == ScreenState.EDIT) {
            int max = getEditorMaxScroll();
            editorEditText.scrollTo(0, Math.round(fraction * max));
        }
    }

    private void updateSliderPositionFromScroll() {
        if (sliderTouchContainer == null || sliderThumb == null || isSliderDragging) return;
        if (sliderTouchContainer.getVisibility() != View.VISIBLE) return;

        sliderTouchContainer.post(() -> {
            if (isSliderDragging) return;
            int max;
            int cur;
            if (currentScreen == ScreenState.READ) {
                max = getReadMaxScroll();
                cur = readScrollView != null ? readScrollView.getScrollY() : 0;
            } else {
                max = getEditorMaxScroll();
                cur = editorEditText != null ? editorEditText.getScrollY() : 0;
            }
            float fraction = max > 0 ? Math.max(0f, Math.min(1f, (float) cur / max)) : 0f;
            float pad = dp(14);
            float thumbH = dp(46);
            float usableH = Math.max(1f, sliderTouchContainer.getHeight() - (2f * pad) - thumbH);
            sliderThumb.setTranslationY(fraction * usableH);

            sliderFadeHandler.removeCallbacks(sliderFadeRunnable);
            sliderTouchContainer.setAlpha(0.9f);
            sliderFadeHandler.postDelayed(sliderFadeRunnable, 1200);
        });
    }

    // ==================== QUICK JUMP BUTTON (TOP / BOTTOM LIKE TEXTPAD) ====================

    private void buildQuickJumpOverlay(FrameLayout paperCard) {
        // Floating Mini Menu Card (appears above button when tapped)
        jumpMenuCard = new LinearLayout(this);
        jumpMenuCard.setOrientation(LinearLayout.VERTICAL);
        jumpMenuCard.setBackground(roundedRect(getCardBgColor(), 12, 1, getBorderColor()));
        jumpMenuCard.setElevation(dp(8));
        jumpMenuCard.setPadding(dp(4), dp(4), dp(4), dp(4));
        jumpMenuCard.setVisibility(View.GONE);

        TextView jumpTopItem = new TextView(this);
        jumpTopItem.setText("↑  أول المستند");
        jumpTopItem.setTextColor(getTextPrimaryColor());
        jumpTopItem.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        jumpTopItem.setTypeface(getAppTypeface(true));
        jumpTopItem.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        jumpTopItem.setPadding(dp(14), dp(10), dp(14), dp(10));
        jumpTopItem.setOnClickListener(v -> {
            jumpMenuCard.setVisibility(View.GONE);
            jumpToDocumentTop();
        });
        jumpMenuCard.addView(jumpTopItem);

        View divider = new View(this);
        divider.setBackgroundColor(getBorderColor());
        jumpMenuCard.addView(divider, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(1)));

        TextView jumpBottomItem = new TextView(this);
        jumpBottomItem.setText("↓  آخر المستند");
        jumpBottomItem.setTextColor(getTextPrimaryColor());
        jumpBottomItem.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        jumpBottomItem.setTypeface(getAppTypeface(true));
        jumpBottomItem.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        jumpBottomItem.setPadding(dp(14), dp(10), dp(14), dp(10));
        jumpBottomItem.setOnClickListener(v -> {
            jumpMenuCard.setVisibility(View.GONE);
            jumpToDocumentBottom();
        });
        jumpMenuCard.addView(jumpBottomItem);

        FrameLayout.LayoutParams menuLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM | Gravity.LEFT);
        menuLp.leftMargin = dp(30);
        menuLp.bottomMargin = dp(60);
        paperCard.addView(jumpMenuCard, menuLp);

        // Floating Quick Jump Button
        FrameLayout jumpBtnContainer = new FrameLayout(this);
        jumpBtnContainer.setBackground(roundedRect(getSecondaryBtnBgColor(), 20, 1, getBorderColor()));
        jumpBtnContainer.setElevation(dp(4));
        jumpBtnContainer.setAlpha(0.88f);

        ImageView jumpIcon = new ImageView(this);
        int jumpIconId = getDrawableId("ic_jump_vertical");
        if (jumpIconId != 0) {
            jumpIcon.setImageResource(jumpIconId);
            jumpIcon.setImageTintList(ColorStateList.valueOf(getAccentTextColor()));
            FrameLayout.LayoutParams icLp = new FrameLayout.LayoutParams(dp(20), dp(20), Gravity.CENTER);
            jumpBtnContainer.addView(jumpIcon, icLp);
        } else {
            TextView fallbackArrow = new TextView(this);
            fallbackArrow.setText("⇅");
            fallbackArrow.setTextColor(getAccentTextColor());
            fallbackArrow.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            fallbackArrow.setGravity(Gravity.CENTER);
            jumpBtnContainer.addView(fallbackArrow);
        }

        jumpBtnContainer.setOnClickListener(v -> {
            if (jumpMenuCard.getVisibility() == View.VISIBLE) {
                jumpMenuCard.setVisibility(View.GONE);
            } else {
                jumpMenuCard.setVisibility(View.VISIBLE);
            }
        });

        FrameLayout.LayoutParams btnLp = new FrameLayout.LayoutParams(
                dp(38), dp(38), Gravity.BOTTOM | Gravity.LEFT);
        btnLp.leftMargin = dp(30);
        btnLp.bottomMargin = dp(14);
        paperCard.addView(jumpBtnContainer, btnLp);
        jumpButton = jumpBtnContainer;

        boolean showJump = prefs.getBoolean("show_jump_btn", true);
        jumpButton.setVisibility(showJump ? View.VISIBLE : View.GONE);
    }

    private void jumpToDocumentTop() {
        if (currentScreen == ScreenState.READ) {
            readScrollView.post(() -> readScrollView.smoothScrollTo(0, 0));
        } else if (currentScreen == ScreenState.EDIT) {
            editorEditText.setSelection(0);
            editorEditText.post(() -> editorEditText.scrollTo(0, 0));
        }
        updateSliderPositionFromScroll();
    }

    private void jumpToDocumentBottom() {
        if (currentScreen == ScreenState.READ) {
            readScrollView.post(() -> {
                int max = getReadMaxScroll();
                readScrollView.smoothScrollTo(0, max);
            });
        } else if (currentScreen == ScreenState.EDIT) {
            int len = editorEditText.getText() != null ? editorEditText.getText().length() : 0;
            editorEditText.setSelection(len);
            editorEditText.post(() -> {
                int max = getEditorMaxScroll();
                editorEditText.scrollTo(0, max);
            });
        }
        updateSliderPositionFromScroll();
    }

    private void switchDocumentMode(ScreenState mode) {
        currentScreen = mode;
        String currentText = editorEditText.getText().toString();
        if (jumpMenuCard != null) jumpMenuCard.setVisibility(View.GONE);

        if (mode == ScreenState.READ) {
            saveCurrentFileImmediately(false);
            hideKeyboard();
            ((View) editorToolbarContainer.getParent()).setVisibility(View.GONE);
            editorEditText.setVisibility(View.GONE);
            readScrollView.setVisibility(View.VISIBLE);
            readActionBar.setVisibility(View.VISIBLE);

            if (!currentSearchQuery.isEmpty()) {
                readTextView.setText(highlightSearchMatches(currentText, currentSearchQuery));
                scrollToFirstMatchInReadMode(currentText, currentSearchQuery);
            } else {
                readTextView.setText(currentText);
            }
        } else {
            ((View) editorToolbarContainer.getParent()).setVisibility(View.VISIBLE);
            readScrollView.setVisibility(View.GONE);
            readActionBar.setVisibility(View.GONE);
            editorEditText.setVisibility(View.VISIBLE);
            editorEditText.requestFocus();
        }
        updateSliderPositionFromScroll();
    }

    private void scrollToFirstMatchInReadMode(String text, String query) {
        int idx = text.toLowerCase(Locale.ROOT).indexOf(query.toLowerCase(Locale.ROOT));
        if (idx < 0) return;
        readTextView.post(() -> {
            Layout layout = readTextView.getLayout();
            if (layout != null) {
                int line = layout.getLineForOffset(idx);
                int y = layout.getLineTop(line);
                readScrollView.smoothScrollTo(0, Math.max(0, y - dp(60)));
            }
        });
    }

    private void updateStatsBar(String text) {
        if (statsBarView == null) return;
        int chars = text.length();
        String trimmed = text.trim();
        int words = trimmed.isEmpty() ? 0 : trimmed.split("\\s+").length;
        int lines = text.isEmpty() ? 1 : text.split("\n", -1).length;
        statsBarView.setText("الكلمات: " + words + "   •   الحروف: " + chars + "   •   الأسطر: " + lines);
    }

    private void showResetZoomDialog() {
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle("حجم النص (" + getZoomPercent() + "%)");
        b.setMessage("هل تريد إعادة حجم النص إلى الحجم الافتراضي (100%)؟");
        b.setPositiveButton("إعادة لـ 100%", (d, w) -> setZoomPercent(100));
        b.setNegativeButton("إلغاء", null);
        b.show();
    }

    // ==================== 3. EDITOR TOOLBAR & FIND/REPLACE ====================

    private List<String> getOrderedToolList() {
        String orderStr = prefs.getString("toolbar_order_v3", null);
        List<String> result = new ArrayList<>();
        if (orderStr != null && !orderStr.isEmpty()) {
            String[] parts = orderStr.split(",");
            for (String p : parts) {
                if (isValidToolId(p) && !result.contains(p)) {
                    result.add(p);
                }
            }
        }
        for (String id : TOOL_IDS) {
            if (!result.contains(id)) result.add(id);
        }
        return result;
    }

    private Set<String> getEnabledToolsSet() {
        Set<String> saved = prefs.getStringSet("enabled_tools_v3", null);
        if (saved == null) {
            return new HashSet<>(DEFAULT_ENABLED_TOOLS);
        }
        return new HashSet<>(saved);
    }

    private boolean isValidToolId(String id) {
        for (String t : TOOL_IDS) {
            if (t.equals(id)) return true;
        }
        return false;
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
        return "ic_auto_format";
    }

    private void populateEditorToolbar() {
        if (editorToolbarContainer == null) return;
        editorToolbarContainer.removeAllViews();

        List<String> ordered = getOrderedToolList();
        Set<String> enabled = getEnabledToolsSet();
        boolean iconsOnly = prefs.getBoolean("toolbar_icons_only", false);

        for (String toolId : ordered) {
            if (!enabled.contains(toolId)) continue;

            if (iconsOnly) {
                FrameLayout iconBtn = new FrameLayout(this);
                iconBtn.setBackground(roundedRect(getSecondaryBtnBgColor(), 10, 1, getBorderColor()));

                ImageView img = new ImageView(this);
                int drawableId = getDrawableId(getToolIconResName(toolId));
                if (drawableId != 0) {
                    img.setImageResource(drawableId);
                    img.setImageTintList(ColorStateList.valueOf(getAccentTextColor()));
                }
                FrameLayout.LayoutParams imgLp = new FrameLayout.LayoutParams(dp(20), dp(20), Gravity.CENTER);
                iconBtn.addView(img, imgLp);

                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(42), dp(36));
                lp.leftMargin = dp(6);
                iconBtn.setOnClickListener(v -> executeEditorTool(toolId));
                iconBtn.setOnLongClickListener(v -> {
                    Toast.makeText(this, getToolLabel(toolId), Toast.LENGTH_SHORT).show();
                    return true;
                });
                editorToolbarContainer.addView(iconBtn, lp);
            } else {
                Button toolBtn = new Button(this);
                configureTextButton(toolBtn, getToolLabel(toolId), 13, true, getAccentTextColor(), 36);
                toolBtn.setBackground(roundedRect(getSecondaryBtnBgColor(), 10, 1, getBorderColor()));
                toolBtn.setPadding(dp(12), 0, dp(12), 0);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, dp(36));
                lp.leftMargin = dp(6);
                toolBtn.setOnClickListener(v -> executeEditorTool(toolId));
                editorToolbarContainer.addView(toolBtn, lp);
            }
        }
    }

    private void executeEditorTool(String toolId) {
        if (editorEditText == null) return;
        int start = Math.max(0, editorEditText.getSelectionStart());
        int end = Math.max(0, editorEditText.getSelectionEnd());
        if (start > end) {
            int tmp = start;
            start = end;
            end = tmp;
        }
        Editable editable = editorEditText.getText();
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);

        switch (toolId) {
            case "undo":
                if (undoIndex > 0) {
                    isUndoRedoAction = true;
                    undoIndex--;
                    String prev = undoStack.get(undoIndex);
                    editorEditText.setText(prev);
                    editorEditText.setSelection(Math.min(prev.length(), start));
                    isUndoRedoAction = false;
                } else {
                    Toast.makeText(this, "لا يوجد تراجع إضافي", Toast.LENGTH_SHORT).show();
                }
                break;

            case "redo":
                if (undoIndex < undoStack.size() - 1) {
                    isUndoRedoAction = true;
                    undoIndex++;
                    String next = undoStack.get(undoIndex);
                    editorEditText.setText(next);
                    editorEditText.setSelection(Math.min(next.length(), start));
                    isUndoRedoAction = false;
                } else {
                    Toast.makeText(this, "لا توجد إعادة إضافية", Toast.LENGTH_SHORT).show();
                }
                break;

            case "copy":
                if (start == end) {
                    Toast.makeText(this, "حدد نصاً أولاً للنسخ", Toast.LENGTH_SHORT).show();
                } else {
                    String selected = editable.subSequence(start, end).toString();
                    clipboard.setPrimaryClip(ClipData.newPlainText("mazwd_text", selected));
                    Toast.makeText(this, "تم نسخ النص", Toast.LENGTH_SHORT).show();
                }
                break;

            case "cut":
                if (start == end) {
                    Toast.makeText(this, "حدد نصاً أولاً للقص", Toast.LENGTH_SHORT).show();
                } else {
                    String selected = editable.subSequence(start, end).toString();
                    clipboard.setPrimaryClip(ClipData.newPlainText("mazwd_text", selected));
                    editable.delete(start, end);
                    Toast.makeText(this, "تم قص النص", Toast.LENGTH_SHORT).show();
                }
                break;

            case "paste":
                if (clipboard.hasPrimaryClip() && clipboard.getPrimaryClip() != null
                        && clipboard.getPrimaryClip().getItemCount() > 0) {
                    CharSequence clipText = clipboard.getPrimaryClip().getItemAt(0).coerceToText(this);
                    if (clipText != null) {
                        editable.replace(start, end, clipText);
                    }
                } else {
                    Toast.makeText(this, "الحافظة فارغة", Toast.LENGTH_SHORT).show();
                }
                break;

            case "select_all":
                editorEditText.selectAll();
                break;

            case "select_up":
                editorEditText.setSelection(0, end);
                break;

            case "select_down":
                editorEditText.setSelection(start, editable.length());
                break;

            case "auto_format": {
                String formatted = autoFormatArabicText(editable.toString());
                editorEditText.setText(formatted);
                editorEditText.setSelection(formatted.length());
                Toast.makeText(this, "تم التنسيق الآلي للنص", Toast.LENGTH_SHORT).show();
                break;
            }

            case "signature": {
                String sig = prefs.getString("custom_signature", "").trim();
                if (sig.isEmpty()) {
                    showQuickSetSignatureDialog();
                    return;
                }
                String prefix = (editable.length() > 0 && !editable.toString().endsWith("\n")) ? "\n\n" : "";
                String insert = prefix + sig + "\n";
                editable.replace(start, end, insert);
                break;
            }

            case "datetime": {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd - hh:mm a", new Locale("ar"));
                String dt = sdf.format(new Date());
                editable.replace(start, end, dt);
                break;
            }

            case "find_replace": {
                showFindReplaceDialog();
                break;
            }
        }
    }

    private void showFindReplaceDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        root.setBackground(roundedRect(getCardBgColor(), 18, 1, getBorderColor()));
        root.setPadding(dp(20), dp(18), dp(20), dp(18));

        TextView title = new TextView(this);
        title.setText("بحث واستبدال في المستند");
        title.setTextColor(getAccentTextColor());
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        title.setTypeface(getAppTypeface(true));
        title.setGravity(Gravity.RIGHT);
        root.addView(title);

        TextView countLabel = new TextView(this);
        countLabel.setText("اكتب الكلمة المراد البحث عنها");
        countLabel.setTextColor(getTextSecondaryColor());
        countLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        countLabel.setTypeface(getAppTypeface(false));
        countLabel.setGravity(Gravity.RIGHT);
        LinearLayout.LayoutParams clLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        clLp.topMargin = dp(4);
        clLp.bottomMargin = dp(10);
        root.addView(countLabel, clLp);

        EditText findInput = new EditText(this);
        findInput.setHint("النص المراد البحث عنه...");
        findInput.setHintTextColor(getTextSecondaryColor());
        findInput.setTextColor(getTextPrimaryColor());
        findInput.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        findInput.setTypeface(getAppTypeface(false));
        findInput.setBackground(roundedRect(getInputBgColor(), 10, 1, getBorderColor()));
        findInput.setPadding(dp(12), dp(10), dp(12), dp(10));
        findInput.setSingleLine(true);
        root.addView(findInput);

        EditText replaceInput = new EditText(this);
        replaceInput.setHint("الاستبدال بـ...");
        replaceInput.setHintTextColor(getTextSecondaryColor());
        replaceInput.setTextColor(getTextPrimaryColor());
        replaceInput.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        replaceInput.setTypeface(getAppTypeface(false));
        replaceInput.setBackground(roundedRect(getInputBgColor(), 10, 1, getBorderColor()));
        replaceInput.setPadding(dp(12), dp(10), dp(12), dp(10));
        replaceInput.setSingleLine(true);
        LinearLayout.LayoutParams repLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        repLp.topMargin = dp(10);
        repLp.bottomMargin = dp(14);
        root.addView(replaceInput, repLp);

        findInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                String q = s.toString();
                if (q.isEmpty()) {
                    countLabel.setText("اكتب الكلمة المراد البحث عنها");
                    return;
                }
                String full = editorEditText.getText().toString();
                int matches = countOccurrences(full, q);
                countLabel.setText("عدد التطابقات في المستند: " + matches);
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        LinearLayout actionsRow = new LinearLayout(this);
        actionsRow.setOrientation(LinearLayout.HORIZONTAL);
        actionsRow.setGravity(Gravity.CENTER_VERTICAL);

        Button findNextBtn = new Button(this);
        configureTextButton(findNextBtn, "بحث عن التالي", 13, true, getTextPrimaryColor(), 42);
        findNextBtn.setBackground(roundedRect(getSecondaryBtnBgColor(), 10, 1, getBorderColor()));
        findNextBtn.setOnClickListener(v -> {
            String q = findInput.getText().toString();
            if (q.isEmpty()) return;
            String text = editorEditText.getText().toString();
            int from = Math.max(0, editorEditText.getSelectionEnd());
            int idx = text.indexOf(q, from);
            if (idx < 0 && from > 0) {
                idx = text.indexOf(q, 0);
            }
            if (idx >= 0) {
                editorEditText.requestFocus();
                editorEditText.setSelection(idx, idx + q.length());
                Toast.makeText(this, "تم تحديد الكلمة في النص", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "لم يتم العثور على الكلمة", Toast.LENGTH_SHORT).show();
            }
        });
        LinearLayout.LayoutParams fnLp = new LinearLayout.LayoutParams(0, dp(42), 1f);
        fnLp.leftMargin = dp(6);
        actionsRow.addView(findNextBtn, fnLp);

        Button replaceAllBtn = new Button(this);
        configureTextButton(replaceAllBtn, "استبدال الكل", 13, true, Color.WHITE, 42);
        replaceAllBtn.setBackground(roundedRect(getPrimaryColor(), 10, 0, 0));
        replaceAllBtn.setOnClickListener(v -> {
            String q = findInput.getText().toString();
            String rep = replaceInput.getText().toString();
            if (q.isEmpty()) {
                Toast.makeText(this, "أدخل النص المراد البحث عنه أولاً", Toast.LENGTH_SHORT).show();
                return;
            }
            String current = editorEditText.getText().toString();
            int matches = countOccurrences(current, q);
            if (matches == 0) {
                Toast.makeText(this, "لا توجد تطابقات لاستبدالها", Toast.LENGTH_SHORT).show();
                return;
            }
            String updated = current.replace(q, rep);
            editorEditText.setText(updated);
            editorEditText.setSelection(updated.length());
            Toast.makeText(this, "تم استبدال " + matches + " موضع بنجاح", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });
        LinearLayout.LayoutParams raLp = new LinearLayout.LayoutParams(0, dp(42), 1f);
        raLp.rightMargin = dp(6);
        actionsRow.addView(replaceAllBtn, raLp);

        root.addView(actionsRow);

        Button closeBtn = new Button(this);
        configureTextButton(closeBtn, "إغلاق", 13, true, getTextSecondaryColor(), 38);
        closeBtn.setBackgroundColor(Color.TRANSPARENT);
        closeBtn.setOnClickListener(v -> dialog.dismiss());
        LinearLayout.LayoutParams cbLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(38));
        cbLp.topMargin = dp(8);
        root.addView(closeBtn, cbLp);

        dialog.setContentView(root);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.90),
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }
        dialog.show();
    }

    private int countOccurrences(String text, String target) {
        if (text == null || target == null || target.isEmpty()) return 0;
        int count = 0;
        int idx = 0;
        while ((idx = text.indexOf(target, idx)) >= 0) {
            count++;
            idx += target.length();
        }
        return count;
    }

    private void showQuickSetSignatureDialog() {
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle("إعداد التوقيع الشخصي");

        EditText input = new EditText(this);
        input.setHint(SIGNATURE_HINT);
        input.setGravity(Gravity.RIGHT | Gravity.TOP);
        input.setTypeface(getAppTypeface(false));
        input.setPadding(dp(16), dp(14), dp(16), dp(14));
        b.setView(input);

        b.setPositiveButton("حفظ وإدراج", (d, w) -> {
            String newSig = input.getText().toString().trim();
            prefs.edit().putString("custom_signature", newSig).apply();
            if (!newSig.isEmpty()) {
                executeEditorTool("signature");
            }
        });
        b.setNegativeButton("إلغاء", null);
        b.show();
    }

    private String autoFormatArabicText(String input) {
        if (input == null || input.isEmpty()) return "";
        String s = input.replace("\r\n", "\n").replace("\r", "\n");
        String[] lines = s.split("\n", -1);
        StringBuilder sb = new StringBuilder();
        int emptyCount = 0;

        for (String line : lines) {
            String clean = line.trim().replaceAll("[ \\t]+", " ");
            clean = clean.replaceAll("\\s+([،؛؟!.:])", "$1");
            clean = clean.replaceAll("([،؛؟!])(?=[^\\s\\n])", "$1 ");

            if (clean.isEmpty()) {
                emptyCount++;
                if (emptyCount <= 1 && sb.length() > 0) {
                    sb.append("\n");
                }
            } else {
                emptyCount = 0;
                if (sb.length() > 0) sb.append("\n");
                sb.append(clean);
            }
        }
        return sb.toString().trim();
    }

    private void recordUndoState(String text) {
        if (undoIndex >= 0 && undoIndex < undoStack.size() && undoStack.get(undoIndex).equals(text)) {
            return;
        }
        while (undoStack.size() > undoIndex + 1) {
            undoStack.remove(undoStack.size() - 1);
        }
        undoStack.add(text);
        if (undoStack.size() > 60) {
            undoStack.remove(0);
        }
        undoIndex = undoStack.size() - 1;
    }

    // ==================== 4. ORGANIZED SETTINGS DIALOG ====================

    private void showSettingsDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        root.setBackground(roundedRect(getBgColor(), 20, 1, getBorderColor()));
        root.setPadding(dp(16), dp(16), dp(16), dp(14));

        // Header
        LinearLayout headerRow = new LinearLayout(this);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);
        headerRow.setPadding(dp(4), 0, dp(4), dp(10));

        TextView headerTitle = new TextView(this);
        headerTitle.setText("⚙  إعدادات المفكرة");
        headerTitle.setTextColor(getAccentTextColor());
        headerTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 19);
        headerTitle.setTypeface(getAppTypeface(true));
        headerTitle.setIncludeFontPadding(false);
        LinearLayout.LayoutParams htLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        headerRow.addView(headerTitle, htLp);

        TextView closeX = new TextView(this);
        closeX.setText("✕");
        closeX.setTextColor(getTextSecondaryColor());
        closeX.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        closeX.setPadding(dp(8), dp(2), dp(8), dp(2));
        closeX.setOnClickListener(v -> dialog.dismiss());
        headerRow.addView(closeX);
        root.addView(headerRow);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);

        // ========== CARD 1: المظهر والخطوط ==========
        LinearLayout appearanceCard = createSettingsSectionCard("المظهر والخطوط");

        // Dark Mode Toggle
        CheckBox darkModeCheck = createSettingsCheckRow(
                "تفعيل الوضع الليلي المريح للعين (Dark Mode)",
                isDarkMode(),
                isChecked -> {
                    prefs.edit().putBoolean("dark_mode", isChecked).apply();
                    dialog.dismiss();
                    showHomeScreen(false);
                    showSettingsDialog();
                }
        );
        appearanceCard.addView(darkModeCheck);

        // Theme Selector Dropdown
        TextView themeLabel = createSettingsLabel("ثيم الألوان:");
        appearanceCard.addView(themeLabel);

        LinearLayout themeSelectBox = new LinearLayout(this);
        themeSelectBox.setOrientation(LinearLayout.HORIZONTAL);
        themeSelectBox.setGravity(Gravity.CENTER_VERTICAL);
        themeSelectBox.setBackground(roundedRect(getInputBgColor(), 12, 1, getBorderColor()));
        themeSelectBox.setPadding(dp(14), dp(11), dp(14), dp(11));

        View currentThemeDot = new View(this);
        currentThemeDot.setBackground(roundedRect(getPrimaryColor(), 8, 0, 0));
        LinearLayout.LayoutParams dotLp = new LinearLayout.LayoutParams(dp(16), dp(16));
        dotLp.leftMargin = dp(10);
        themeSelectBox.addView(currentThemeDot, dotLp);

        TextView currentThemeText = new TextView(this);
        currentThemeText.setText(THEMES[getThemeIndex()][0]);
        currentThemeText.setTextColor(getTextPrimaryColor());
        currentThemeText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        currentThemeText.setTypeface(getAppTypeface(true));
        currentThemeText.setIncludeFontPadding(false);
        currentThemeText.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams cttLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        themeSelectBox.addView(currentThemeText, cttLp);

        TextView themeArrow = new TextView(this);
        themeArrow.setText("▼");
        themeArrow.setTextColor(getTextSecondaryColor());
        themeArrow.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        themeSelectBox.addView(themeArrow);

        themeSelectBox.setOnClickListener(v -> showThemeDropdownDialog(dialog));
        appearanceCard.addView(themeSelectBox);

        // Font Selector Dropdown
        TextView fontLabel = createSettingsLabel("نوع الخط:");
        appearanceCard.addView(fontLabel);

        LinearLayout fontSelectBox = new LinearLayout(this);
        fontSelectBox.setOrientation(LinearLayout.HORIZONTAL);
        fontSelectBox.setGravity(Gravity.CENTER_VERTICAL);
        fontSelectBox.setBackground(roundedRect(getInputBgColor(), 12, 1, getBorderColor()));
        fontSelectBox.setPadding(dp(14), dp(11), dp(14), dp(11));

        TextView currentFontText = new TextView(this);
        currentFontText.setText(FONT_NAMES[getFontIndex()]);
        currentFontText.setTextColor(getTextPrimaryColor());
        currentFontText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        currentFontText.setTypeface(getAppTypeface(true));
        currentFontText.setIncludeFontPadding(false);
        currentFontText.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams cftLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        fontSelectBox.addView(currentFontText, cftLp);

        TextView fontArrow = new TextView(this);
        fontArrow.setText("▼");
        fontArrow.setTextColor(getTextSecondaryColor());
        fontArrow.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        fontSelectBox.addView(fontArrow);

        fontSelectBox.setOnClickListener(v -> showFontDropdownDialog(dialog));
        appearanceCard.addView(fontSelectBox);

        content.addView(appearanceCard);

        // ========== CARD 2: خيارات شاشة القراءة والتحرير ==========
        LinearLayout editorOptionsCard = createSettingsSectionCard("خيارات شاشة القراءة والتحرير");

        CheckBox fastSliderCheck = createSettingsCheckRow(
                "إظهار شريط التمرير السريع الجانبي (يسار الشاشة)",
                prefs.getBoolean("show_fast_slider", true),
                isChecked -> prefs.edit().putBoolean("show_fast_slider", isChecked).apply()
        );
        editorOptionsCard.addView(fastSliderCheck);

        CheckBox jumpBtnCheck = createSettingsCheckRow(
                "إظهار زر الانتقال السريع (لأول وآخر المستند)",
                prefs.getBoolean("show_jump_btn", true),
                isChecked -> prefs.edit().putBoolean("show_jump_btn", isChecked).apply()
        );
        editorOptionsCard.addView(jumpBtnCheck);

        CheckBox statsBarCheck = createSettingsCheckRow(
                "إظهار شريط إحصائيات النص (الكلمات والحروف والأسطر)",
                prefs.getBoolean("show_stats_bar", true),
                isChecked -> prefs.edit().putBoolean("show_stats_bar", isChecked).apply()
        );
        editorOptionsCard.addView(statsBarCheck);

        CheckBox iconsOnlyCheck = createSettingsCheckRow(
                "عرض شريط الأدوات كرموز فقط (بدلاً من النص)",
                prefs.getBoolean("toolbar_icons_only", false),
                isChecked -> prefs.edit().putBoolean("toolbar_icons_only", isChecked).apply()
        );
        editorOptionsCard.addView(iconsOnlyCheck);

        Button customizeToolbarBtn = new Button(this);
        configureTextButton(customizeToolbarBtn, "🛠  تخصيص وترتيب أدوات التحرير", 14, true, getAccentTextColor(), 44);
        customizeToolbarBtn.setBackground(roundedRect(getLightAccentColor(), 12, 0, 0));
        customizeToolbarBtn.setOnClickListener(v -> showToolbarCustomizationDialog());
        LinearLayout.LayoutParams ctbLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(44));
        ctbLp.topMargin = dp(8);
        editorOptionsCard.addView(customizeToolbarBtn, ctbLp);

        content.addView(editorOptionsCard);

        // ========== CARD 3: التوقيع وإدارة الملفات ==========
        LinearLayout filesCard = createSettingsSectionCard("التوقيع وإدارة الملفات");

        TextView sigLabel = createSettingsLabel("التوقيع الشخصي:");
        filesCard.addView(sigLabel);

        EditText sigInput = new EditText(this);
        sigInput.setText(prefs.getString("custom_signature", ""));
        sigInput.setHint(SIGNATURE_HINT);
        sigInput.setHintTextColor(getTextSecondaryColor());
        sigInput.setTextColor(getTextPrimaryColor());
        sigInput.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        sigInput.setTypeface(getAppTypeface(false));
        sigInput.setGravity(Gravity.RIGHT | Gravity.TOP);
        sigInput.setMinLines(2);
        sigInput.setBackground(roundedRect(getInputBgColor(), 10, 1, getBorderColor()));
        sigInput.setPadding(dp(12), dp(10), dp(12), dp(10));
        filesCard.addView(sigInput);

        Button saveSigBtn = new Button(this);
        configureTextButton(saveSigBtn, "حفظ التوقيع", 14, true, Color.WHITE, 42);
        saveSigBtn.setBackground(roundedRect(getPrimaryColor(), 10, 0, 0));
        LinearLayout.LayoutParams ssLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(42));
        ssLp.topMargin = dp(8);
        saveSigBtn.setOnClickListener(v -> {
            String newSig = sigInput.getText().toString().trim();
            prefs.edit().putString("custom_signature", newSig).apply();
            Toast.makeText(this, newSig.isEmpty() ? "تم تفريغ التوقيع" : "تم حفظ التوقيع بنجاح", Toast.LENGTH_SHORT).show();
        });
        filesCard.addView(saveSigBtn, ssLp);

        Button changeFolderBtn = new Button(this);
        configureTextButton(changeFolderBtn, "📁  تغيير مجلد حفظ الملفات", 14, true, getTextPrimaryColor(), 44);
        changeFolderBtn.setBackground(roundedRect(getSecondaryBtnBgColor(), 12, 1, getBorderColor()));
        LinearLayout.LayoutParams cfLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(44));
        cfLp.topMargin = dp(10);
        changeFolderBtn.setOnClickListener(v -> {
            dialog.dismiss();
            pickFolder();
        });
        filesCard.addView(changeFolderBtn, cfLp);

        content.addView(filesCard);

        scroll.addView(content);
        LinearLayout.LayoutParams scLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        root.addView(scroll, scLp);

        Button closeBtn = new Button(this);
        configureTextButton(closeBtn, "إغلاق", 15, true, Color.WHITE, 46);
        closeBtn.setBackground(roundedRect(getPrimaryColor(), 12, 0, 0));
        LinearLayout.LayoutParams clLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(46));
        clLp.topMargin = dp(10);
        closeBtn.setOnClickListener(v -> {
            String newSig = sigInput.getText().toString().trim();
            prefs.edit().putString("custom_signature", newSig).apply();
            dialog.dismiss();
            showHomeScreen(false);
        });
        root.addView(closeBtn, clLp);

        dialog.setContentView(root);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.93),
                    (int) (getResources().getDisplayMetrics().heightPixels * 0.86)
            );
        }
        dialog.show();
    }

    private LinearLayout createSettingsSectionCard(String sectionTitle) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(roundedRect(getCardBgColor(), 14, 1, getBorderColor()));
        card.setPadding(dp(14), dp(12), dp(14), dp(14));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(12);
        card.setLayoutParams(lp);

        TextView title = new TextView(this);
        title.setText(sectionTitle);
        title.setTextColor(getAccentTextColor());
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        title.setTypeface(getAppTypeface(true));
        title.setIncludeFontPadding(false);
        title.setGravity(Gravity.RIGHT);
        LinearLayout.LayoutParams tLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tLp.bottomMargin = dp(6);
        card.addView(title, tLp);

        View div = new View(this);
        div.setBackgroundColor(getBorderColor());
        LinearLayout.LayoutParams dLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(1));
        dLp.bottomMargin = dp(6);
        card.addView(div, dLp);

        return card;
    }

    private interface OnCheckChange {
        void onChanged(boolean isChecked);
    }

    private CheckBox createSettingsCheckRow(String label, boolean checked, OnCheckChange listener) {
        CheckBox cb = new CheckBox(this);
        cb.setText("  " + label);
        cb.setTextColor(getTextPrimaryColor());
        cb.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
        cb.setTypeface(getAppTypeface(false));
        cb.setChecked(checked);
        cb.setPadding(dp(4), dp(6), dp(4), dp(6));
        cb.setOnCheckedChangeListener((buttonView, isChecked) -> listener.onChanged(isChecked));
        return cb;
    }

    private TextView createSettingsLabel(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(getTextSecondaryColor());
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tv.setTypeface(getAppTypeface(true));
        tv.setIncludeFontPadding(false);
        tv.setGravity(Gravity.RIGHT);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(10);
        lp.bottomMargin = dp(6);
        tv.setLayoutParams(lp);
        return tv;
    }

    private void showThemeDropdownDialog(Dialog parentSettingsDialog) {
        Dialog themeDialog = new Dialog(this);
        themeDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        box.setBackground(roundedRect(getCardBgColor(), 16, 1, getBorderColor()));
        box.setPadding(dp(12), dp(12), dp(12), dp(12));

        TextView title = new TextView(this);
        title.setText("اختر ثيم الألوان");
        title.setTextColor(getAccentTextColor());
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        title.setTypeface(getAppTypeface(true));
        title.setIncludeFontPadding(false);
        title.setGravity(Gravity.RIGHT);
        title.setPadding(dp(8), dp(4), dp(8), dp(10));
        box.addView(title);

        ScrollView sv = new ScrollView(this);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);

        int selected = getThemeIndex();
        for (int i = 0; i < THEMES.length; i++) {
            final int idx = i;
            boolean isSelected = (idx == selected);

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setBackground(roundedRect(
                    isSelected ? getLightAccentColor() : Color.TRANSPARENT,
                    10,
                    isSelected ? 1 : 0,
                    getPrimaryColor()
            ));
            row.setPadding(dp(12), dp(11), dp(12), dp(11));

            View colorDot = new View(this);
            colorDot.setBackground(roundedRect(Color.parseColor(THEMES[idx][1]), 8, 0, 0));
            LinearLayout.LayoutParams dotLp = new LinearLayout.LayoutParams(dp(18), dp(18));
            dotLp.leftMargin = dp(12);
            row.addView(colorDot, dotLp);

            TextView item = new TextView(this);
            item.setText((isSelected ? "✓  " : "") + THEMES[idx][0]);
            item.setTextColor(isSelected ? getAccentTextColor() : getTextPrimaryColor());
            item.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
            item.setTypeface(getAppTypeface(isSelected));
            item.setIncludeFontPadding(false);
            item.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
            row.addView(item, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            row.setOnClickListener(v -> {
                prefs.edit().putInt("theme_index", idx).apply();
                updateStatusBarColor();
                themeDialog.dismiss();
                if (parentSettingsDialog != null && parentSettingsDialog.isShowing()) {
                    parentSettingsDialog.dismiss();
                }
                showHomeScreen(false);
                showSettingsDialog();
            });

            list.addView(row);
        }

        sv.addView(list);
        box.addView(sv);

        themeDialog.setContentView(box);
        if (themeDialog.getWindow() != null) {
            themeDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            themeDialog.getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.88),
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }
        themeDialog.show();
    }

    private void showFontDropdownDialog(Dialog parentSettingsDialog) {
        Dialog fontDialog = new Dialog(this);
        fontDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        box.setBackground(roundedRect(getCardBgColor(), 16, 1, getBorderColor()));
        box.setPadding(dp(12), dp(12), dp(12), dp(12));

        TextView title = new TextView(this);
        title.setText("اختر نوع الخط");
        title.setTextColor(getAccentTextColor());
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        title.setTypeface(getAppTypeface(true));
        title.setIncludeFontPadding(false);
        title.setGravity(Gravity.RIGHT);
        title.setPadding(dp(8), dp(4), dp(8), dp(10));
        box.addView(title);

        ScrollView sv = new ScrollView(this);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);

        int selected = getFontIndex();
        for (int i = 0; i < FONT_NAMES.length; i++) {
            final int idx = i;
            boolean isSelected = (idx == selected);

            TextView item = new TextView(this);
            item.setText((isSelected ? "✓  " : "") + FONT_NAMES[idx]);
            item.setTextColor(isSelected ? getAccentTextColor() : getTextPrimaryColor());
            item.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
            item.setTypeface(getTypefaceForIndex(idx, isSelected));
            item.setIncludeFontPadding(false);
            item.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
            item.setBackground(roundedRect(
                    isSelected ? getLightAccentColor() : Color.TRANSPARENT,
                    10,
                    isSelected ? 1 : 0,
                    getPrimaryColor()
            ));
            item.setPadding(dp(14), dp(12), dp(14), dp(12));

            LinearLayout.LayoutParams itemLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            itemLp.bottomMargin = dp(4);

            item.setOnClickListener(v -> {
                prefs.edit().putInt("font_index", idx).apply();
                fontDialog.dismiss();
                if (parentSettingsDialog != null && parentSettingsDialog.isShowing()) {
                    parentSettingsDialog.dismiss();
                }
                showHomeScreen(false);
                showSettingsDialog();
            });

            list.addView(item, itemLp);
        }

        sv.addView(list);
        box.addView(sv);

        fontDialog.setContentView(box);
        if (fontDialog.getWindow() != null) {
            fontDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            fontDialog.getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.88),
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }
        fontDialog.show();
    }

    // ==================== 5. TOOLBAR CUSTOMIZATION WITH SMOOTH DRAG & DROP ====================

    private void showToolbarCustomizationDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        root.setBackground(roundedRect(getCardBgColor(), 20, 1, getBorderColor()));
        root.setPadding(dp(18), dp(18), dp(18), dp(16));

        TextView title = new TextView(this);
        title.setText("تخصيص وترتيب أدوات التحرير");
        title.setTextColor(getAccentTextColor());
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        title.setTypeface(getAppTypeface(true));
        title.setIncludeFontPadding(false);
        title.setGravity(Gravity.RIGHT);
        root.addView(title);

        TextView sub = new TextView(this);
        sub.setText("اسحب الأداة من مقبض السحب (≡) للأعلى أو للأسفل لتغيير ترتيبها، أو ضع علامة (✓) لإظهارها:");
        sub.setTextColor(getTextSecondaryColor());
        sub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
        sub.setTypeface(getAppTypeface(false));
        sub.setGravity(Gravity.RIGHT);
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        subLp.topMargin = dp(4);
        subLp.bottomMargin = dp(12);
        root.addView(sub, subLp);

        final List<String> currentOrder = new ArrayList<>(getOrderedToolList());
        final Set<String> currentEnabled = new HashSet<>(getEnabledToolsSet());

        ScrollView listScroll = new ScrollView(this);
        LinearLayout listContainer = new LinearLayout(this);
        listContainer.setOrientation(LinearLayout.VERTICAL);
        listContainer.setClipChildren(false);
        listContainer.setClipToPadding(false);
        listScroll.addView(listContainer);

        final Runnable[] rebuildListRef = new Runnable[1];
        rebuildListRef[0] = () -> {
            listContainer.removeAllViews();
            int rowHeightEstimate = dp(54);

            for (int i = 0; i < currentOrder.size(); i++) {
                final int index = i;
                final String toolId = currentOrder.get(index);

                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setBackground(roundedRect(getInputBgColor(), 12, 1, getBorderColor()));
                row.setPadding(dp(10), dp(6), dp(10), dp(6));

                LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dp(46));
                rowLp.bottomMargin = dp(8);

                // Drag Handle
                FrameLayout dragHandle = new FrameLayout(this);
                ImageView dragIcon = new ImageView(this);
                int dragResId = getDrawableId("ic_drag_handle");
                if (dragResId != 0) {
                    dragIcon.setImageResource(dragResId);
                    dragIcon.setImageTintList(ColorStateList.valueOf(getTextSecondaryColor()));
                }
                dragHandle.addView(dragIcon, new FrameLayout.LayoutParams(dp(22), dp(22), Gravity.CENTER));
                row.addView(dragHandle, new LinearLayout.LayoutParams(dp(36), dp(36)));

                // Vector Tool Icon Badge
                FrameLayout iconBadge = new FrameLayout(this);
                iconBadge.setBackground(roundedRect(getLightAccentColor(), 8, 0, 0));
                ImageView toolIcon = new ImageView(this);
                int iconId = getDrawableId(getToolIconResName(toolId));
                if (iconId != 0) {
                    toolIcon.setImageResource(iconId);
                    toolIcon.setImageTintList(ColorStateList.valueOf(getAccentTextColor()));
                }
                iconBadge.addView(toolIcon, new FrameLayout.LayoutParams(dp(18), dp(18), Gravity.CENTER));
                LinearLayout.LayoutParams badgeLp = new LinearLayout.LayoutParams(dp(32), dp(32));
                badgeLp.rightMargin = dp(6);
                badgeLp.leftMargin = dp(10);
                row.addView(iconBadge, badgeLp);

                // Tool Label
                TextView nameView = new TextView(this);
                nameView.setText(getToolLabel(toolId));
                nameView.setTextColor(getTextPrimaryColor());
                nameView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
                nameView.setTypeface(getAppTypeface(true));
                nameView.setIncludeFontPadding(false);
                nameView.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
                LinearLayout.LayoutParams nameLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                row.addView(nameView, nameLp);

                // CheckBox
                CheckBox cb = new CheckBox(this);
                cb.setChecked(currentEnabled.contains(toolId));
                cb.setOnCheckedChangeListener((btn, isChecked) -> {
                    if (isChecked) currentEnabled.add(toolId);
                    else currentEnabled.remove(toolId);
                });
                row.addView(cb);

                final float[] startRawY = new float[1];
                final int[] currentDragIdx = new int[]{index};
                final boolean[] isDragging = new boolean[]{false};

                dragHandle.setOnTouchListener((v, event) -> {
                    int action = event.getActionMasked();
                    if (action == MotionEvent.ACTION_DOWN) {
                        startRawY[0] = event.getRawY();
                        currentDragIdx[0] = index;
                        isDragging[0] = true;
                        listScroll.requestDisallowInterceptTouchEvent(true);
                        row.setTranslationZ(dp(12));
                        row.setAlpha(0.92f);
                        row.setBackground(roundedRect(getLightAccentColor(), 12, 2, getPrimaryColor()));
                        return true;
                    } else if (action == MotionEvent.ACTION_MOVE && isDragging[0]) {
                        float dy = event.getRawY() - startRawY[0];
                        row.setTranslationY(dy);
                        int shift = Math.round(dy / rowHeightEstimate);
                        int targetIdx = Math.max(0, Math.min(currentOrder.size() - 1, currentDragIdx[0] + shift));
                        if (targetIdx != currentDragIdx[0]) {
                            String moved = currentOrder.remove(currentDragIdx[0]);
                            currentOrder.add(targetIdx, moved);
                            currentDragIdx[0] = targetIdx;
                            startRawY[0] = event.getRawY();
                            row.setTranslationY(0f);
                            listContainer.post(rebuildListRef[0]);
                            return false;
                        }
                        return true;
                    } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                        if (isDragging[0]) {
                            isDragging[0] = false;
                            row.setTranslationY(0f);
                            row.setTranslationZ(0f);
                            row.setAlpha(1.0f);
                            listScroll.requestDisallowInterceptTouchEvent(false);
                            listContainer.post(rebuildListRef[0]);
                        }
                        return true;
                    }
                    return false;
                });

                listContainer.addView(row, rowLp);
            }
        };
        rebuildListRef[0].run();

        LinearLayout.LayoutParams scLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        root.addView(listScroll, scLp);

        Button saveBtn = new Button(this);
        configureTextButton(saveBtn, "حفظ ترتيب وإعدادات الأدوات", 15, true, Color.WHITE, 46);
        saveBtn.setBackground(roundedRect(getPrimaryColor(), 12, 0, 0));
        LinearLayout.LayoutParams sbLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(46));
        sbLp.topMargin = dp(10);
        saveBtn.setOnClickListener(v -> {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < currentOrder.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append(currentOrder.get(i));
            }
            prefs.edit()
                    .putString("toolbar_order_v3", sb.toString())
                    .putStringSet("enabled_tools_v3", currentEnabled)
                    .apply();
            if (editorToolbarContainer != null) {
                populateEditorToolbar();
            }
            Toast.makeText(this, "تم حفظ تخصيص الأدوات", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });
        root.addView(saveBtn, sbLp);

        dialog.setContentView(root);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.92),
                    (int) (getResources().getDisplayMetrics().heightPixels * 0.82)
            );
        }
        dialog.show();
    }

    // ==================== 6. CUSTOM 3-DOTS MENU & SAFE VAULT (النسخ الآمنة) ====================

    private void showFileOptionsMenu(NoteFile file) {
        Dialog menuDialog = new Dialog(this);
        menuDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        root.setBackground(roundedRect(getCardBgColor(), 18, 1, getBorderColor()));
        root.setPadding(dp(16), dp(16), dp(16), dp(12));

        // File Title Header
        TextView header = new TextView(this);
        header.setText(stripTxtExtension(file.name));
        header.setTextColor(getAccentTextColor());
        header.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        header.setTypeface(getAppTypeface(true));
        header.setIncludeFontPadding(false);
        header.setGravity(Gravity.RIGHT);
        header.setPadding(dp(6), dp(2), dp(6), dp(10));
        root.addView(header);

        View topDivider = new View(this);
        topDivider.setBackgroundColor(getPrimaryColor());
        LinearLayout.LayoutParams tdLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(2));
        tdLp.bottomMargin = dp(4);
        root.addView(topDivider, tdLp);

        // Exact requested order:
        // 1. تثبيت الملف
        // 2. إعادة تسمية الملف
        // 3. إنشاء نسخة مكررة من الملف
        // 4. إنشاء نسخة آمنة
        // 5. تصفح النسخ الآمنة
        // 6. مشاركة الملف
        // 7. حذف الملف
        String[] labels = new String[]{
                file.pinned ? "إلغاء تثبيت الملف" : "تثبيت الملف",
                "إعادة تسمية الملف",
                "إنشاء نسخة مكررة من الملف",
                "إنشاء نسخة آمنة",
                "تصفح النسخ الآمنة",
                "مشاركة الملف",
                "حذف الملف"
        };

        for (int i = 0; i < labels.length; i++) {
            final int idx = i;
            boolean isDelete = (i == labels.length - 1);

            TextView optionView = new TextView(this);
            optionView.setText(labels[i]);
            int textColor = isDelete
                    ? (isDarkMode() ? Color.parseColor("#F87171") : Color.parseColor("#DC2626"))
                    : getTextPrimaryColor();
            optionView.setTextColor(textColor);
            optionView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
            optionView.setTypeface(getAppTypeface(isDelete));
            optionView.setIncludeFontPadding(false);
            optionView.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
            optionView.setPadding(dp(10), dp(13), dp(10), dp(13));

            optionView.setOnClickListener(v -> {
                menuDialog.dismiss();
                switch (idx) {
                    case 0:
                        togglePinFile(file);
                        break;
                    case 1:
                        showRenameDialog(file);
                        break;
                    case 2:
                        duplicateFile(file);
                        break;
                    case 3:
                        createSafeSnapshot(file, false);
                        break;
                    case 4:
                        showBrowseSafeSnapshotsDialog(file);
                        break;
                    case 5: {
                        String body = readFileContent(file.uri);
                        shareTextContent(file.name, body);
                        break;
                    }
                    case 6:
                        confirmDeleteFile(file);
                        break;
                }
            });

            root.addView(optionView);

            if (i < labels.length - 1) {
                View div = new View(this);
                div.setBackgroundColor(getBorderColor());
                root.addView(div, new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dp(1)));
            }
        }

        menuDialog.setContentView(root);
        if (menuDialog.getWindow() != null) {
            menuDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            menuDialog.getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.86),
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }
        menuDialog.show();
    }

    // --- SAFE VAULT LOGIC (النسخ الآمنة في مسار المجلد داخل الهاتف) ---

    private Uri getOrCreateSafeVaultDirectoryUri(boolean createIfMissing) {
        if (folderUri == null) return null;
        try {
            String treeDocId = DocumentsContract.getTreeDocumentId(folderUri);
            Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(folderUri, treeDocId);
            String[] projection = new String[]{
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_MIME_TYPE
            };

            try (Cursor c = getContentResolver().query(childrenUri, projection, null, null, null)) {
                if (c != null) {
                    while (c.moveToNext()) {
                        String docId = c.getString(0);
                        String name = c.getString(1);
                        String mime = c.getString(2);
                        if (SAFE_VAULT_FOLDER_NAME.equals(name)
                                && DocumentsContract.Document.MIME_TYPE_DIR.equals(mime)) {
                            return DocumentsContract.buildDocumentUriUsingTree(folderUri, docId);
                        }
                    }
                }
            }

            if (createIfMissing) {
                Uri parentDocUri = DocumentsContract.buildDocumentUriUsingTree(folderUri, treeDocId);
                return DocumentsContract.createDocument(
                        getContentResolver(),
                        parentDocUri,
                        DocumentsContract.Document.MIME_TYPE_DIR,
                        SAFE_VAULT_FOLDER_NAME
                );
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private String getSafeSnapshotPrefix(String originalFileName) {
        return stripTxtExtension(originalFileName) + "__نسخة_آمنة_";
    }

    private void createSafeSnapshot(NoteFile file, boolean silentBeforeDelete) {
        if (folderUri == null || file == null) return;
        try {
            Uri vaultDirUri = getOrCreateSafeVaultDirectoryUri(true);
            if (vaultDirUri == null) {
                if (!silentBeforeDelete) {
                    Toast.makeText(this, "تعذر الوصول إلى خزنة النسخ الآمنة", Toast.LENGTH_SHORT).show();
                }
                return;
            }

            String content = readFileContent(file.uri);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US);
            String snapName = getSafeSnapshotPrefix(file.name) + sdf.format(new Date()) + ".txt";

            Uri snapUri = DocumentsContract.createDocument(
                    getContentResolver(), vaultDirUri, "text/plain", snapName);
            if (snapUri != null) {
                writeFileContent(snapUri, content);
                if (!silentBeforeDelete) {
                    Toast.makeText(this, "تم إنشاء نسخة آمنة في الخزنة بنجاح", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            if (!silentBeforeDelete) {
                Toast.makeText(this, "تعذر إنشاء النسخة الآمنة", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private List<SafeSnapshot> loadSafeSnapshotsForFile(NoteFile file) {
        List<SafeSnapshot> list = new ArrayList<>();
        Uri vaultDirUri = getOrCreateSafeVaultDirectoryUri(false);
        if (vaultDirUri == null) return list;

        String prefix = getSafeSnapshotPrefix(file.name);
        SimpleDateFormat displayFormat = new SimpleDateFormat("yyyy/MM/dd • hh:mm:ss a", new Locale("ar"));

        try {
            String vaultDocId = DocumentsContract.getDocumentId(vaultDirUri);
            Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(folderUri, vaultDocId);
            String[] projection = new String[]{
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_LAST_MODIFIED,
                    DocumentsContract.Document.COLUMN_SIZE
            };

            try (Cursor c = getContentResolver().query(childrenUri, projection, null, null, null)) {
                if (c != null) {
                    while (c.moveToNext()) {
                        String docId = c.getString(0);
                        String name = c.getString(1);
                        long modified = c.isNull(2) ? 0L : c.getLong(2);
                        long size = c.isNull(3) ? 0L : c.getLong(3);

                        if (name != null && name.startsWith(prefix) && name.toLowerCase(Locale.ROOT).endsWith(".txt")) {
                            SafeSnapshot snap = new SafeSnapshot();
                            snap.uri = DocumentsContract.buildDocumentUriUsingTree(folderUri, docId);
                            snap.fileName = name;
                            snap.lastModified = modified;
                            snap.size = size;
                            snap.displayDate = modified > 0 ? displayFormat.format(new Date(modified)) : stripTxtExtension(name);
                            list.add(snap);
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }

        Collections.sort(list, (a, b) -> Long.compare(b.lastModified, a.lastModified));
        return list;
    }

    private void showBrowseSafeSnapshotsDialog(NoteFile file) {
        List<SafeSnapshot> snapshots = loadSafeSnapshotsForFile(file);

        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        root.setBackground(roundedRect(getCardBgColor(), 18, 1, getBorderColor()));
        root.setPadding(dp(18), dp(18), dp(18), dp(14));

        TextView title = new TextView(this);
        title.setText("النسخ الآمنة لملف: " + stripTxtExtension(file.name));
        title.setTextColor(getAccentTextColor());
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16.5f);
        title.setTypeface(getAppTypeface(true));
        title.setGravity(Gravity.RIGHT);
        root.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("اضغط على أي نسخة لمعاينتها أو استعادتها إلى الملف الأصلي:");
        subtitle.setTextColor(getTextSecondaryColor());
        subtitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
        subtitle.setTypeface(getAppTypeface(false));
        subtitle.setGravity(Gravity.RIGHT);
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        subLp.topMargin = dp(4);
        subLp.bottomMargin = dp(12);
        root.addView(subtitle, subLp);

        if (snapshots.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("لا توجد نسخ آمنة محفوظة لهذا الملف حتى الآن.\nيمكنك حفظ نسخة آمنة في أي وقت من قائمة الثلاث نقاط (⋮).");
            empty.setTextColor(getTextSecondaryColor());
            empty.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            empty.setTypeface(getAppTypeface(false));
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(dp(16), dp(28), dp(16), dp(28));
            root.addView(empty);
        } else {
            ScrollView sv = new ScrollView(this);
            LinearLayout list = new LinearLayout(this);
            list.setOrientation(LinearLayout.VERTICAL);

            for (int i = 0; i < snapshots.size(); i++) {
                final SafeSnapshot snap = snapshots.get(i);
                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.VERTICAL);
                row.setBackground(roundedRect(getInputBgColor(), 12, 1, getBorderColor()));
                row.setPadding(dp(14), dp(10), dp(14), dp(10));
                LinearLayout.LayoutParams rLp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                rLp.bottomMargin = dp(8);

                TextView snapTitle = new TextView(this);
                snapTitle.setText("نسخة آمنة #" + (snapshots.size() - i));
                snapTitle.setTextColor(getTextPrimaryColor());
                snapTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14.5f);
                snapTitle.setTypeface(getAppTypeface(true));
                snapTitle.setGravity(Gravity.RIGHT);
                row.addView(snapTitle);

                TextView snapMeta = new TextView(this);
                snapMeta.setText(snap.displayDate + "   •   " + formatSize(snap.size));
                snapMeta.setTextColor(getTextSecondaryColor());
                snapMeta.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
                snapMeta.setTypeface(getAppTypeface(false));
                snapMeta.setGravity(Gravity.RIGHT);
                row.addView(snapMeta);

                row.setOnClickListener(v -> {
                    dialog.dismiss();
                    showSafeSnapshotPreviewDialog(file, snap);
                });

                list.addView(row, rLp);
            }
            sv.addView(list);
            LinearLayout.LayoutParams svLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
            root.addView(sv, svLp);
        }

        Button closeBtn = new Button(this);
        configureTextButton(closeBtn, "إغلاق", 14, true, Color.WHITE, 42);
        closeBtn.setBackground(roundedRect(getPrimaryColor(), 10, 0, 0));
        LinearLayout.LayoutParams cLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(42));
        cLp.topMargin = dp(10);
        closeBtn.setOnClickListener(v -> dialog.dismiss());
        root.addView(closeBtn, cLp);

        dialog.setContentView(root);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.90),
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }
        dialog.show();
    }

    private void showSafeSnapshotPreviewDialog(NoteFile targetFile, SafeSnapshot snap) {
        String snapContent = readFileContent(snap.uri);

        Dialog previewDialog = new Dialog(this);
        previewDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        root.setBackground(roundedRect(getCardBgColor(), 18, 1, getBorderColor()));
        root.setPadding(dp(18), dp(18), dp(18), dp(14));

        TextView title = new TextView(this);
        title.setText("معاينة النسخة الآمنة");
        title.setTextColor(getAccentTextColor());
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16.5f);
        title.setTypeface(getAppTypeface(true));
        title.setGravity(Gravity.RIGHT);
        root.addView(title);

        TextView meta = new TextView(this);
        meta.setText(snap.displayDate);
        meta.setTextColor(getTextSecondaryColor());
        meta.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        meta.setGravity(Gravity.RIGHT);
        LinearLayout.LayoutParams mLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        mLp.bottomMargin = dp(10);
        root.addView(meta, mLp);

        ScrollView sv = new ScrollView(this);
        sv.setBackground(roundedRect(getInputBgColor(), 12, 1, getBorderColor()));
        TextView contentTv = new TextView(this);
        contentTv.setText(snapContent);
        contentTv.setTextColor(getTextPrimaryColor());
        contentTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14.5f);
        contentTv.setTypeface(getAppTypeface(false));
        contentTv.setLineSpacing(dp(4), 1.15f);
        contentTv.setPadding(dp(14), dp(12), dp(14), dp(12));
        contentTv.setTextIsSelectable(true);
        sv.addView(contentTv);

        LinearLayout.LayoutParams svLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        root.addView(sv, svLp);

        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams brLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        brLp.topMargin = dp(12);

        Button restoreBtn = new Button(this);
        configureTextButton(restoreBtn, "استعادة هذه النسخة", 14, true, Color.WHITE, 44);
        restoreBtn.setBackground(roundedRect(getPrimaryColor(), 10, 0, 0));
        restoreBtn.setOnClickListener(v -> {
            writeFileContent(targetFile.uri, snapContent);
            contentCache.put(targetFile.uri.toString(), snapContent);
            Toast.makeText(this, "تم استعادة النسخة الآمنة إلى الملف بنجاح", Toast.LENGTH_SHORT).show();
            previewDialog.dismiss();
            loadFilesFromFolder();
        });
        LinearLayout.LayoutParams b1Lp = new LinearLayout.LayoutParams(0, dp(44), 1.3f);
        b1Lp.leftMargin = dp(6);
        btnRow.addView(restoreBtn, b1Lp);

        Button cancelBtn = new Button(this);
        configureTextButton(cancelBtn, "إغلاق", 14, true, getTextPrimaryColor(), 44);
        cancelBtn.setBackground(roundedRect(getSecondaryBtnBgColor(), 10, 1, getBorderColor()));
        cancelBtn.setOnClickListener(v -> previewDialog.dismiss());
        LinearLayout.LayoutParams b2Lp = new LinearLayout.LayoutParams(0, dp(44), 1f);
        b2Lp.rightMargin = dp(6);
        btnRow.addView(cancelBtn, b2Lp);

        root.addView(btnRow, brLp);

        previewDialog.setContentView(root);
        if (previewDialog.getWindow() != null) {
            previewDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            previewDialog.getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.92),
                    (int) (getResources().getDisplayMetrics().heightPixels * 0.78)
            );
        }
        previewDialog.show();
    }

    // ==================== 7. FILE OPERATIONS (NEW, RENAME, DUPLICATE, DELETE, BACKUP) ====================

    private void onNewFileClicked() {
        if (folderUri == null) {
            Toast.makeText(this, "يرجى اختيار مجلد لحفظ الملفات أولاً", Toast.LENGTH_SHORT).show();
            pickFolder();
            return;
        }

        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle("إنشاء ملف نصي جديد");

        EditText input = new EditText(this);
        input.setHint("اسم الملف (مثال: مقالة التراث الحضرمي)");
        input.setSingleLine(true);
        input.setTypeface(getAppTypeface(false));
        input.setPadding(dp(18), dp(14), dp(18), dp(14));
        b.setView(input);

        b.setPositiveButton("إنشاء", (d, w) -> {
            String rawName = input.getText().toString().trim();
            if (rawName.isEmpty()) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.US);
                rawName = "مذكرة_" + sdf.format(new Date());
            }
            createNewTxtFile(rawName);
        });
        b.setNegativeButton("إلغاء", null);
        b.show();
    }

    private void createNewTxtFile(String baseName) {
        try {
            String finalName = ensureTxtExtension(baseName);
            String treeDocId = DocumentsContract.getTreeDocumentId(folderUri);
            Uri parentDocUri = DocumentsContract.buildDocumentUriUsingTree(folderUri, treeDocId);
            Uri newDocUri = DocumentsContract.createDocument(
                    getContentResolver(), parentDocUri, "text/plain", finalName);

            if (newDocUri != null) {
                writeFileContent(newDocUri, "");
                NoteFile nf = new NoteFile();
                nf.uri = newDocUri;
                nf.name = finalName;
                nf.lastModified = System.currentTimeMillis();
                nf.size = 0;
                nf.pinned = false;
                allFiles.add(0, nf);
                openNoteFile(nf, ScreenState.EDIT);
            } else {
                Toast.makeText(this, "تعذر إنشاء الملف", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "خطأ أثناء إنشاء الملف: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void togglePinFile(NoteFile file) {
        Set<String> pinned = new HashSet<>(prefs.getStringSet("pinned_files", new HashSet<>()));
        String key = file.uri.toString();
        if (pinned.contains(key)) {
            pinned.remove(key);
            file.pinned = false;
            Toast.makeText(this, "تم إلغاء التثبيت", Toast.LENGTH_SHORT).show();
        } else {
            pinned.add(key);
            file.pinned = true;
            Toast.makeText(this, "تم تثبيت الملف في الأعلى", Toast.LENGTH_SHORT).show();
        }
        prefs.edit().putStringSet("pinned_files", pinned).apply();
        loadFilesFromFolder();
    }

    private void showRenameDialog(NoteFile file) {
        if (currentScreen == ScreenState.EDIT) {
            saveCurrentFileImmediately(false);
        }
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle("إعادة تسمية الملف");

        EditText input = new EditText(this);
        input.setText(stripTxtExtension(file.name));
        input.setSingleLine(true);
        input.setTypeface(getAppTypeface(false));
        input.setPadding(dp(18), dp(14), dp(18), dp(14));
        b.setView(input);

        b.setPositiveButton("حفظ الاسم", (d, w) -> {
            String newBase = input.getText().toString().trim();
            if (newBase.isEmpty()) return;
            String newFullName = ensureTxtExtension(newBase);
            try {
                Uri renamedUri = DocumentsContract.renameDocument(getContentResolver(), file.uri, newFullName);
                if (renamedUri != null) {
                    file.uri = renamedUri;
                    file.name = newFullName;
                    if (docTitleView != null && currentScreen != ScreenState.HOME) {
                        docTitleView.setText(stripTxtExtension(newFullName));
                    }
                    loadFilesFromFolder();
                    Toast.makeText(this, "تم تغيير الاسم بنجاح", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Toast.makeText(this, "تعذر تغيير الاسم: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
        b.setNegativeButton("إلغاء", null);
        b.show();
    }

    private void duplicateFile(NoteFile file) {
        try {
            String content = readFileContent(file.uri);
            String copyName = stripTxtExtension(file.name) + " - نسخة.txt";
            String treeDocId = DocumentsContract.getTreeDocumentId(folderUri);
            Uri parentDocUri = DocumentsContract.buildDocumentUriUsingTree(folderUri, treeDocId);
            Uri newUri = DocumentsContract.createDocument(
                    getContentResolver(), parentDocUri, "text/plain", copyName);
            if (newUri != null) {
                writeFileContent(newUri, content);
                Toast.makeText(this, "تم إنشاء نسخة مكررة من الملف", Toast.LENGTH_SHORT).show();
                loadFilesFromFolder();
            }
        } catch (Exception e) {
            Toast.makeText(this, "تعذر نسخ الملف: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void confirmDeleteFile(NoteFile file) {
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle("تأكيد حذف الملف");
        b.setMessage("هل أنت متأكد من حذف الملف \"" + stripTxtExtension(file.name) + "\"؟");
        b.setPositiveButton("حذف", (d, w) -> {
            try {
                // Save an automatic safe copy in the phone's vault directory before deletion
                createSafeSnapshot(file, true);

                boolean deleted = DocumentsContract.deleteDocument(getContentResolver(), file.uri);
                if (deleted) {
                    contentCache.remove(file.uri.toString());
                    Toast.makeText(this, "تم حذف الملف", Toast.LENGTH_SHORT).show();
                    if (currentScreen != ScreenState.HOME) {
                        showHomeScreen(true);
                    } else {
                        loadFilesFromFolder();
                    }
                }
            } catch (Exception e) {
                Toast.makeText(this, "تعذر الحذف: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
        b.setNegativeButton("إلغاء", null);
        b.show();
    }

    private void shareTextContent(String title, String body) {
        Intent sendIntent = new Intent(Intent.ACTION_SEND);
        sendIntent.setType("text/plain");
        sendIntent.putExtra(Intent.EXTRA_SUBJECT, stripTxtExtension(title));
        sendIntent.putExtra(Intent.EXTRA_TEXT, body);
        startActivity(Intent.createChooser(sendIntent, "مشاركة النص عبر"));
    }

    private void startBackupZipFlow() {
        if (allFiles.isEmpty()) {
            Toast.makeText(this, "لا توجد ملفات لعمل نسخة احتياطية لها", Toast.LENGTH_SHORT).show();
            return;
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.US);
        String zipName = "نسخة_احتياطية_مفكرة_شفيقة_مزود_" + sdf.format(new Date()) + ".zip";

        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/zip");
        intent.putExtra(Intent.EXTRA_TITLE, zipName);
        startActivityForResult(intent, REQ_CREATE_BACKUP_ZIP);
    }

    private void writeBackupZip(Uri targetZipUri) {
        try (OutputStream os = getContentResolver().openOutputStream(targetZipUri);
             ZipOutputStream zos = new ZipOutputStream(os)) {
            int count = 0;
            for (NoteFile nf : allFiles) {
                String content = readFileContent(nf.uri);
                ZipEntry entry = new ZipEntry(nf.name);
                zos.putNextEntry(entry);
                byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
                zos.write(bytes, 0, bytes.length);
                zos.closeEntry();
                count++;
            }
            zos.finish();
            Toast.makeText(this, "تم حفظ النسخة الاحتياطية بنجاح (" + count + " ملفات)", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "فشل إنشاء النسخة الاحتياطية: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // ==================== 8. STORAGE & NAVIGATION HELPERS ====================

    private void pickFolder() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                | Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
        startActivityForResult(intent, REQ_PICK_FOLDER);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null) return;

        if (requestCode == REQ_PICK_FOLDER) {
            Uri treeUri = data.getData();
            if (treeUri != null) {
                final int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
                try {
                    getContentResolver().takePersistableUriPermission(treeUri, takeFlags);
                } catch (Exception ignored) {
                }
                folderUri = treeUri;
                prefs.edit().putString("folder_uri", treeUri.toString()).apply();
                contentCache.clear();
                showHomeScreen(true);
            }
        } else if (requestCode == REQ_CREATE_BACKUP_ZIP) {
            Uri zipUri = data.getData();
            if (zipUri != null) {
                writeBackupZip(zipUri);
            }
        }
    }

    private String readFileContent(Uri uri) {
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
        } catch (Exception ignored) {
        }
        return sb.toString();
    }

    private void writeFileContent(Uri uri, String content) {
        try (OutputStream os = getContentResolver().openOutputStream(uri, "wt")) {
            if (os != null) {
                os.write(content.getBytes(StandardCharsets.UTF_8));
                os.flush();
            }
        } catch (Exception e) {
            try (OutputStream os = getContentResolver().openOutputStream(uri)) {
                if (os != null) {
                    os.write(content.getBytes(StandardCharsets.UTF_8));
                    os.flush();
                }
            } catch (Exception ignored) {
            }
        }
    }

    private final Runnable autoSaveRunnable = () -> saveCurrentFileImmediately(false);

    private void scheduleAutoSave() {
        autoSaveHandler.removeCallbacks(autoSaveRunnable);
        autoSaveHandler.postDelayed(autoSaveRunnable, 1200);
    }

    private void saveCurrentFileImmediately(boolean showToast) {
        autoSaveHandler.removeCallbacks(autoSaveRunnable);
        if (currentFile == null || editorEditText == null || !isDirty) return;
        String text = editorEditText.getText().toString();
        writeFileContent(currentFile.uri, text);
        contentCache.put(currentFile.uri.toString(), text);
        currentFile.lastModified = System.currentTimeMillis();
        currentFile.size = text.getBytes(StandardCharsets.UTF_8).length;
        isDirty = false;
        if (showToast) {
            Toast.makeText(this, "تم الحفظ", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleBackNavigation() {
        if (currentScreen == ScreenState.EDIT) {
            saveCurrentFileImmediately(false);
            switchDocumentMode(ScreenState.READ);
        } else if (currentScreen == ScreenState.READ) {
            saveCurrentFileImmediately(false);
            showHomeScreen(true);
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

    @Override
    protected void onPause() {
        super.onPause();
        if (currentScreen == ScreenState.EDIT || currentScreen == ScreenState.READ) {
            saveCurrentFileImmediately(false);
        }
    }

    private void hideKeyboard() {
        View v = getCurrentFocus();
        if (v != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
        }
    }

    private String stripTxtExtension(String name) {
        if (name == null) return "";
        if (name.toLowerCase(Locale.ROOT).endsWith(".txt")) {
            return name.substring(0, name.length() - 4);
        }
        return name;
    }

    private String ensureTxtExtension(String name) {
        if (name.toLowerCase(Locale.ROOT).endsWith(".txt")) return name;
        return name + ".txt";
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " بايت";
        double kb = bytes / 1024.0;
        if (kb < 1024) return String.format(Locale.US, "%.1f ك.ب", kb);
        double mb = kb / 1024.0;
        return String.format(Locale.US, "%.1f م.ب", mb);
    }
}
