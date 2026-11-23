package com.mazwd.app;

import android.app.Activity;
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
    private int savedHomeScrollY = 0;

    private final ArrayList<String> undoStack = new ArrayList<>();
    private int undoIndex = -1;
    private boolean isUndoRedoAction = false;
    private boolean isDirty = false;

    private FrameLayout rootContainer;
    private ScrollView homeScrollView;
    private EditText editorEditText;
    private TextView readTextView;
    private ScrollView readScrollView;
    private TextView docTitleView;
    private TextView statsBarView;
    private TextView zoomPercentView;
    private HorizontalScrollView editorToolbarScroll;
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
            {"البترولي العصري", "#0F4C5C", "#082F3A", "#CCFBF1"},
            {"البنفسجي الأنيق", "#5B21B6", "#2E1065", "#EDE9FE"},
            {"الرمادي الرسمي", "#334155", "#0F172A", "#E2E8F0"},
            {"التركواز الهادئ", "#115E59", "#042F2E", "#CCFBF1"}
    };

    // 12 Selectable Fonts (7 original + 5 new open-source Arabic fonts)
    private static final String[] FONT_NAMES = {
            "Sans — خط واضح وعصري (افتراضي)",
            "تجوال — خط حديث وأنيق للمقالات",
            "المراعي — خط عربي كلاسيكي مريح ومتوازن",
            "شهرزاد عريض — خط نسخي تراثي أصيل",
            "ريدكس برو — خط عصري فائق الوضوح",
            "نوتو سانس — خط جوجل القياسي المتوازن",
            "نوتو نسخ — خط نسخي مطبعي للأبحاث",
            "آي بي إم بليكس — خط تقني حديث وأنيق",
            "عارف رقعة — خط الرقعة العربي",
            "ريم كوفي — خط كوفي حديث ومميز",
            "Serif — خط إنجليزي رسمي ومريح",
            "Cursive — خط فني مائل"
    };

    // 12 Editor Tools
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
                    case 1:
                        return loadResFont(bold ? "tajawal_bold" : "tajawal_regular", style);
                    case 2:
                        return loadResFont(bold ? "mazwd_arabic_bold" : "mazwd_arabic", style);
                    case 3:
                        return loadResFont("scheherazade_bold", Typeface.BOLD);
                    case 4:
                        return loadResFont(bold ? "readex_pro_bold" : "readex_pro", style);
                    case 5:
                        return loadResFont(bold ? "noto_sans_arabic_bold" : "noto_sans_arabic", style);
                    case 6:
                        return loadResFont(bold ? "noto_naskh_arabic_bold" : "noto_naskh_arabic", style);
                    case 7:
                        return loadResFont(bold ? "ibm_plex_arabic_bold" : "ibm_plex_arabic", style);
                    case 8:
                        return loadResFont(bold ? "aref_ruqaa_bold" : "aref_ruqaa", style);
                    case 9:
                        return loadResFont(bold ? "reem_kufi_bold" : "reem_kufi", style);
                    case 10:
                        return Typeface.create(Typeface.SERIF, style);
                    case 11:
                        return Typeface.create("cursive", style);
                    default:
                        return Typeface.create("sans-serif", style);
                }
            }
        } catch (Exception ignored) {
        }
        return Typeface.create(Typeface.SANS_SERIF, style);
    }

    private Typeface loadResFont(String resName, int style) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int id = getResources().getIdentifier(resName, "font", getPackageName());
            if (id != 0) {
                Typeface base = getResources().getFont(id);
                return Typeface.create(base, style);
            }
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

    // ==================== SMART ARABIC NORMALIZATION & SEARCH ====================

    private boolean isArabicDiacritic(char c) {
        return (c >= '\u064B' && c <= '\u065F') || c == '\u0670' || c == '\u0640';
    }

    private char normalizeArabicChar(char c) {
        if (c == 'أ' || c == 'إ' || c == 'آ' || c == 'ٱ') return 'ا';
        if (c == 'ة') return 'ه';
        if (c == 'ى') return 'ي';
        return Character.toLowerCase(c);
    }

    private String normalizeForSearch(String s) {
        if (s == null || s.isEmpty()) return "";
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (!isArabicDiacritic(c)) {
                sb.append(normalizeArabicChar(c));
            }
        }
        return sb.toString();
    }

    private List<int[]> findSmartMatchRanges(String text, String query) {
        List<int[]> ranges = new ArrayList<>();
        if (text == null || query == null) return ranges;
        String normQuery = normalizeForSearch(query.trim());
        if (normQuery.isEmpty()) return ranges;

        StringBuilder normText = new StringBuilder(text.length());
        int[] mapToOrig = new int[text.length() + 1];
        int normLen = 0;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (!isArabicDiacritic(c)) {
                mapToOrig[normLen] = i;
                normText.append(normalizeArabicChar(c));
                normLen++;
            }
        }
        mapToOrig[normLen] = text.length();
        String normStr = normText.toString();

        int idx = 0;
        while ((idx = normStr.indexOf(normQuery, idx)) >= 0) {
            int origStart = mapToOrig[idx];
            int endNormIdx = idx + normQuery.length() - 1;
            int origEnd = mapToOrig[endNormIdx] + 1;
            while (origEnd < text.length() && isArabicDiacritic(text.charAt(origEnd))) {
                origEnd++;
            }
            ranges.add(new int[]{origStart, origEnd});
            idx += normQuery.length();
        }
        return ranges;
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

        // --- 1. FIXED TOP HERO HEADER CARD (Centered Title & Subtitle) ---
        LinearLayout heroCard = new LinearLayout(this);
        heroCard.setOrientation(LinearLayout.VERTICAL);
        heroCard.setGravity(Gravity.CENTER_HORIZONTAL);
        GradientDrawable heroBg = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{getDarkHeaderColor(), getPrimaryColor()}
        );
        float r = dp(24);
        heroBg.setCornerRadii(new float[]{0, 0, 0, 0, r, r, r, r});
        heroCard.setBackground(heroBg);
        heroCard.setPadding(dp(20), dp(20), dp(20), dp(20));
        heroCard.setElevation(dp(6));

        TextView titleView = new TextView(this);
        titleView.setText("مفكرة أ. شفيقة مزود");
        titleView.setTextColor(Color.WHITE);
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 23);
        titleView.setTypeface(getAppTypeface(true));
        titleView.setIncludeFontPadding(false);
        titleView.setGravity(Gravity.CENTER);
        heroCard.addView(titleView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView subtitleView = new TextView(this);
        subtitleView.setText("كاتبة وباحثة في التراث الحضرمي");
        subtitleView.setTextColor(Color.parseColor("#E2E8F0"));
        subtitleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        subtitleView.setTypeface(getAppTypeface(false));
        subtitleView.setIncludeFontPadding(false);
        subtitleView.setGravity(Gravity.CENTER);
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

        // --- 2. SCROLLABLE AREA ("ملفاتي النصية" + Search Bar + Files List) ---
        homeScrollView = new ScrollView(this);
        homeScrollView.setFillViewport(true);
        homeScrollView.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
            savedHomeScrollY = scrollY;
        });

        LinearLayout scrollContent = new LinearLayout(this);
        scrollContent.setOrientation(LinearLayout.VERTICAL);
        scrollContent.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        // Section Header ("ملفاتي النصية" + "TXT" badge)
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

        scrollContent.addView(sectionHeader);

        // Unified Search Bar (scrolls naturally with the list)
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
        scrollContent.addView(searchBox, searchBoxLp);

        // Files List Container
        homeFilesContainer = new LinearLayout(this);
        homeFilesContainer.setOrientation(LinearLayout.VERTICAL);
        homeFilesContainer.setPadding(dp(16), dp(4), dp(16), dp(18));
        scrollContent.addView(homeFilesContainer, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        homeScrollView.addView(scrollContent);
        LinearLayout.LayoutParams scrollLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        mainLayout.addView(homeScrollView, scrollLp);

        // --- 3. FIXED BOTTOM BAR (Settings & Backup ZIP) ---
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

        final int restoreY = savedHomeScrollY;
        if (reloadFiles) {
            loadFilesFromFolder();
        } else {
            renderHomeFileList();
        }
        if (restoreY > 0 && homeScrollView != null) {
            homeScrollView.post(() -> homeScrollView.scrollTo(0, restoreY));
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
        String q = currentSearchQuery.trim();

        for (NoteFile nf : allFiles) {
            nf.matchSnippet = null;
            if (q.isEmpty()) {
                filtered.add(nf);
            } else {
                String cleanName = stripTxtExtension(nf.name);
                boolean nameMatches = !findSmartMatchRanges(cleanName, q).isEmpty();

                if (nf.cachedContent == null) {
                    nf.cachedContent = readFileContent(nf.uri);
                    contentCache.put(nf.uri.toString(), nf.cachedContent);
                }
                String body = nf.cachedContent != null ? nf.cachedContent : "";
                List<int[]> bodyRanges = findSmartMatchRanges(body, q);
                boolean contentMatches = !bodyRanges.isEmpty();

                if (contentMatches) {
                    int[] firstMatch = bodyRanges.get(0);
                    int start = Math.max(0, firstMatch[0] - 30);
                    int end = Math.min(body.length(), firstMatch[1] + 45);
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

            card.setOnClickListener(v -> {
                if (homeScrollView != null) {
                    savedHomeScrollY = homeScrollView.getScrollY();
                }
                openNoteFile(nf, ScreenState.READ);
            });
            homeFilesContainer.addView(card, cardLp);
        }
    }

    private Spannable highlightSearchMatches(String text, String query) {
        SpannableString span = new SpannableString(text);
        List<int[]> ranges = findSmartMatchRanges(text, query);
        for (int[] r : ranges) {
            span.setSpan(
                    new BackgroundColorSpan(0xFFFDE047),
                    r[0],
                    r[1],
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            );
            span.setSpan(
                    new ForegroundColorSpan(Color.parseColor("#0F172A")),
                    r[0],
                    r[1],
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            );
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

        // --- 2. EDITOR TOOLBAR (Visible in EDIT mode if enabled in Settings) ---
        editorToolbarScroll = new HorizontalScrollView(this);
        editorToolbarScroll.setHorizontalScrollBarEnabled(false);
        editorToolbarScroll.setBackgroundColor(getCardBgColor());
        editorToolbarScroll.setElevation(dp(2));

        editorToolbarContainer = new LinearLayout(this);
        editorToolbarContainer.setOrientation(LinearLayout.HORIZONTAL);
        editorToolbarContainer.setGravity(Gravity.CENTER_VERTICAL);
        editorToolbarContainer.setPadding(dp(8), dp(6), dp(8), dp(6));
        editorToolbarScroll.addView(editorToolbarContainer);
        populateEditorToolbar();

        docLayout.addView(editorToolbarScroll, new LinearLayout.LayoutParams(
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

    // ==================== LEFT FAST SCROLL SLIDER (INSTANT RESPONSE DURING FLING) ====================

    private void buildLeftFastSlider(FrameLayout paperCard) {
        sliderTouchContainer = new FrameLayout(this);
        sliderTouchContainer.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        sliderTouchContainer.setAlpha(0.38f);

        View trackLine = new View(this);
        trackLine.setBackground(roundedRect(getBorderColor(), 2, 0, 0));
        FrameLayout.LayoutParams trackLp = new FrameLayout.LayoutParams(
                dp(3), ViewGroup.LayoutParams.MATCH_PARENT, Gravity.CENTER_HORIZONTAL);
        trackLp.topMargin = dp(14);
        trackLp.bottomMargin = dp(14);
        sliderTouchContainer.addView(trackLine, trackLp);

        sliderThumb = new View(this);
        sliderThumb.setBackground(roundedRect(getPrimaryColor(), 6, 1, Color.WHITE));
        sliderThumb.setElevation(dp(3));
        FrameLayout.LayoutParams thumbLp = new FrameLayout.LayoutParams(
                dp(8), dp(46), Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        thumbLp.topMargin = dp(14);
        sliderTouchContainer.addView(sliderThumb, thumbLp);

        FrameLayout.LayoutParams containerLp = new FrameLayout.LayoutParams(
                dp(28), ViewGroup.LayoutParams.MATCH_PARENT, Gravity.LEFT | Gravity.TOP);
        paperCard.addView(sliderTouchContainer, containerLp);

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
        bubbleLp.leftMargin = dp(34);
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

            if (action == MotionEvent.ACTION_DOWN) {
                // Immediately abort any ongoing fling deceleration on the ScrollView or EditText
                stopOngoingScrollFling();
            }

            if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE) {
                isSliderDragging = true;
                v.getParent().requestDisallowInterceptTouchEvent(true);
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

    private void stopOngoingScrollFling() {
        try {
            if (readScrollView != null && currentScreen == ScreenState.READ) {
                readScrollView.fling(0);
                readScrollView.smoothScrollBy(0, 0);
                long now = android.os.SystemClock.uptimeMillis();
                MotionEvent cancelFling = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, 0f, 0f, 0);
                readScrollView.onTouchEvent(cancelFling);
                MotionEvent upFling = MotionEvent.obtain(now, now, MotionEvent.ACTION_CANCEL, 0f, 0f, 0);
                readScrollView.onTouchEvent(upFling);
                cancelFling.recycle();
                upFling.recycle();
            } else if (editorEditText != null && currentScreen == ScreenState.EDIT) {
                editorEditText.cancelPendingInputEvents();
                editorEditText.scrollBy(0, 0);
            }
        } catch (Exception ignored) {
        }
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

    private float getCurrentDocumentScrollFraction() {
        if (currentScreen == ScreenState.READ && readScrollView != null) {
            int max = getReadMaxScroll();
            return max > 0 ? Math.max(0f, Math.min(1f, (float) readScrollView.getScrollY() / max)) : 0f;
        } else if (currentScreen == ScreenState.EDIT && editorEditText != null) {
            int max = getEditorMaxScroll();
            return max > 0 ? Math.max(0f, Math.min(1f, (float) editorEditText.getScrollY() / max)) : 0f;
        }
        return 0f;
    }

    private void scrollDocumentToFraction(float fraction) {
        if (currentScreen == ScreenState.READ && readScrollView != null) {
            int max = getReadMaxScroll();
            readScrollView.scrollTo(0, Math.round(fraction * max));
        } else if (currentScreen == ScreenState.EDIT && editorEditText != null) {
            int max = getEditorMaxScroll();
            editorEditText.scrollTo(0, Math.round(fraction * max));
        }
    }

    private void updateSliderPositionFromScroll() {
        if (sliderTouchContainer == null || sliderThumb == null || isSliderDragging) return;
        if (sliderTouchContainer.getVisibility() != View.VISIBLE) return;

        sliderTouchContainer.post(() -> {
            if (isSliderDragging) return;
            float fraction = getCurrentDocumentScrollFraction();
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
        stopOngoingScrollFling();
        if (currentScreen == ScreenState.READ) {
            readScrollView.post(() -> readScrollView.smoothScrollTo(0, 0));
        } else if (currentScreen == ScreenState.EDIT) {
            editorEditText.setSelection(0);
            editorEditText.post(() -> editorEditText.scrollTo(0, 0));
        }
        updateSliderPositionFromScroll();
    }

    private void jumpToDocumentBottom() {
        stopOngoingScrollFling();
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
        final float prevScrollFraction = getCurrentDocumentScrollFraction();
        final boolean isModeSwitch = (currentScreen != mode);
        currentScreen = mode;
        String currentText = editorEditText.getText().toString();
        if (jumpMenuCard != null) jumpMenuCard.setVisibility(View.GONE);

        if (mode == ScreenState.READ) {
            saveCurrentFileImmediately(false);
            hideKeyboard();
            editorToolbarScroll.setVisibility(View.GONE);
            editorEditText.setVisibility(View.GONE);
            readScrollView.setVisibility(View.VISIBLE);
            readActionBar.setVisibility(View.VISIBLE);

            if (!currentSearchQuery.isEmpty()) {
                readTextView.setText(highlightSearchMatches(currentText, currentSearchQuery));
                if (!isModeSwitch) {
                    scrollToFirstMatchInReadMode(currentText, currentSearchQuery);
                } else {
                    readScrollView.post(() -> {
                        scrollDocumentToFraction(prevScrollFraction);
                        updateSliderPositionFromScroll();
                    });
                }
            } else {
                readTextView.setText(currentText);
                readScrollView.post(() -> {
                    scrollDocumentToFraction(prevScrollFraction);
                    updateSliderPositionFromScroll();
                });
            }
        } else {
            boolean showToolbar = prefs.getBoolean("show_editor_toolbar", true);
            editorToolbarScroll.setVisibility(showToolbar ? View.VISIBLE : View.GONE);
            readScrollView.setVisibility(View.GONE);
            readActionBar.setVisibility(View.GONE);
            editorEditText.setVisibility(View.VISIBLE);
            editorEditText.requestFocus();
            editorEditText.post(() -> {
                scrollDocumentToFraction(prevScrollFraction);
                updateSliderPositionFromScroll();
            });
        }
        updateSliderPositionFromScroll();
    }

    private void scrollToFirstMatchInReadMode(String text, String query) {
        List<int[]> matches = findSmartMatchRanges(text, query);
        if (matches.isEmpty()) return;
        final int idx = matches.get(0)[0];
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
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        root.setBackground(roundedRect(getCardBgColor(), 18, 1, getBorderColor()));
        root.setPadding(dp(20), dp(18), dp(20), dp(16));

        TextView title = new TextView(this);
        title.setText("حجم النص (" + getZoomPercent() + "%)");
        title.setTextColor(getAccentTextColor());
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        title.setTypeface(getAppTypeface(true));
        title.setGravity(Gravity.RIGHT);
        root.addView(title);

        TextView msg = new TextView(this);
        msg.setText("هل تريد إعادة حجم النص إلى الحجم الافتراضي (100%)؟");
        msg.setTextColor(getTextPrimaryColor());
        msg.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14.5f);
        msg.setTypeface(getAppTypeface(false));
        msg.setGravity(Gravity.RIGHT);
        LinearLayout.LayoutParams mLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        mLp.topMargin = dp(8);
        mLp.bottomMargin = dp(16);
        root.addView(msg, mLp);

        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);

        Button confirmBtn = new Button(this);
        configureTextButton(confirmBtn, "إعادة لـ 100%", 14, true, Color.WHITE, 44);
        confirmBtn.setBackground(roundedRect(getPrimaryColor(), 10, 0, 0));
        confirmBtn.setOnClickListener(v -> {
            setZoomPercent(100);
            dialog.dismiss();
        });
        LinearLayout.LayoutParams b1Lp = new LinearLayout.LayoutParams(0, dp(44), 1.2f);
        b1Lp.leftMargin = dp(6);
        btnRow.addView(confirmBtn, b1Lp);

        Button cancelBtn = new Button(this);
        configureTextButton(cancelBtn, "إلغاء", 14, true, getTextPrimaryColor(), 44);
        cancelBtn.setBackground(roundedRect(getSecondaryBtnBgColor(), 10, 1, getBorderColor()));
        cancelBtn.setOnClickListener(v -> dialog.dismiss());
        LinearLayout.LayoutParams b2Lp = new LinearLayout.LayoutParams(0, dp(44), 1f);
        b2Lp.rightMargin = dp(6);
        btnRow.addView(cancelBtn, b2Lp);

        root.addView(btnRow);
        showStyledDialog(dialog, root, 0.88f, ViewGroup.LayoutParams.WRAP_CONTENT);
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

        if (editorToolbarScroll != null && currentScreen == ScreenState.EDIT) {
            boolean showToolbar = prefs.getBoolean("show_editor_toolbar", true);
            editorToolbarScroll.setVisibility(showToolbar ? View.VISIBLE : View.GONE);
        }

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
                int matches = findSmartMatchRanges(full, q).size();
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
            List<int[]> matches = findSmartMatchRanges(text, q);
            if (matches.isEmpty()) {
                Toast.makeText(this, "لم يتم العثور على الكلمة", Toast.LENGTH_SHORT).show();
                return;
            }
            int from = Math.max(0, editorEditText.getSelectionEnd());
            int[] chosen = matches.get(0);
            for (int[] r : matches) {
                if (r[0] >= from) {
                    chosen = r;
                    break;
                }
            }
            editorEditText.requestFocus();
            editorEditText.setSelection(chosen[0], chosen[1]);
            Toast.makeText(this, "تم تحديد الكلمة في النص", Toast.LENGTH_SHORT).show();
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
            List<int[]> matches = findSmartMatchRanges(current, q);
            if (matches.isEmpty()) {
                Toast.makeText(this, "لا توجد تطابقات لاستبدالها", Toast.LENGTH_SHORT).show();
                return;
            }
            StringBuilder sb = new StringBuilder();
            int lastEnd = 0;
            for (int[] r : matches) {
                sb.append(current, lastEnd, r[0]);
                sb.append(rep);
                lastEnd = r[1];
            }
            sb.append(current.substring(lastEnd));
            String updated = sb.toString();
            editorEditText.setText(updated);
            editorEditText.setSelection(updated.length());
            Toast.makeText(this, "تم استبدال " + matches.size() + " موضع بنجاح", Toast.LENGTH_SHORT).show();
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

        showStyledDialog(dialog, root, 0.90f, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private void showQuickSetSignatureDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        root.setBackground(roundedRect(getCardBgColor(), 18, 1, getBorderColor()));
        root.setPadding(dp(20), dp(18), dp(20), dp(16));

        TextView title = new TextView(this);
        title.setText("إعداد التوقيع الشخصي");
        title.setTextColor(getAccentTextColor());
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        title.setTypeface(getAppTypeface(true));
        title.setGravity(Gravity.RIGHT);
        root.addView(title);

        EditText input = new EditText(this);
        input.setHint(SIGNATURE_HINT);
        input.setHintTextColor(getTextSecondaryColor());
        input.setTextColor(getTextPrimaryColor());
        input.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        input.setGravity(Gravity.RIGHT | Gravity.TOP);
        input.setTypeface(getAppTypeface(false));
        input.setMinLines(2);
        input.setBackground(roundedRect(getInputBgColor(), 10, 1, getBorderColor()));
        input.setPadding(dp(14), dp(12), dp(14), dp(12));
        LinearLayout.LayoutParams inLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        inLp.topMargin = dp(12);
        inLp.bottomMargin = dp(16);
        root.addView(input, inLp);

        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);

        Button saveBtn = new Button(this);
        configureTextButton(saveBtn, "حفظ وإدراج", 14, true, Color.WHITE, 44);
        saveBtn.setBackground(roundedRect(getPrimaryColor(), 10, 0, 0));
        saveBtn.setOnClickListener(v -> {
            String newSig = input.getText().toString().trim();
            prefs.edit().putString("custom_signature", newSig).apply();
            dialog.dismiss();
            if (!newSig.isEmpty()) {
                executeEditorTool("signature");
            }
        });
        LinearLayout.LayoutParams b1Lp = new LinearLayout.LayoutParams(0, dp(44), 1.2f);
        b1Lp.leftMargin = dp(6);
        btnRow.addView(saveBtn, b1Lp);

        Button cancelBtn = new Button(this);
        configureTextButton(cancelBtn, "إلغاء", 14, true, getTextPrimaryColor(), 44);
        cancelBtn.setBackground(roundedRect(getSecondaryBtnBgColor(), 10, 1, getBorderColor()));
        cancelBtn.setOnClickListener(v -> dialog.dismiss());
        LinearLayout.LayoutParams b2Lp = new LinearLayout.LayoutParams(0, dp(44), 1f);
        b2Lp.rightMargin = dp(6);
        btnRow.addView(cancelBtn, b2Lp);

        root.addView(btnRow);
        showStyledDialog(dialog, root, 0.90f, ViewGroup.LayoutParams.WRAP_CONTENT);
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

    // ==================== 4. ORGANIZED SETTINGS DIALOG WITH MODERN SWITCHES ====================

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

        appearanceCard.addView(createSettingsSwitchRow(
                "الوضع الليلي المريح للعين (Dark Mode)",
                isDarkMode(),
                isChecked -> {
                    prefs.edit().putBoolean("dark_mode", isChecked).apply();
                    dialog.dismiss();
                    showHomeScreen(false);
                    showSettingsDialog();
                }
        ));

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

        editorOptionsCard.addView(createSettingsSwitchRow(
                "إظهار شريط التمرير السريع الجانبي (يسار الشاشة)",
                prefs.getBoolean("show_fast_slider", true),
                isChecked -> prefs.edit().putBoolean("show_fast_slider", isChecked).apply()
        ));

        editorOptionsCard.addView(createSettingsSwitchRow(
                "إظهار زر الانتقال السريع (لأول وآخر المستند)",
                prefs.getBoolean("show_jump_btn", true),
                isChecked -> prefs.edit().putBoolean("show_jump_btn", isChecked).apply()
        ));

        editorOptionsCard.addView(createSettingsSwitchRow(
                "إظهار شريط إحصائيات النص (الكلمات والحروف والأسطر)",
                prefs.getBoolean("show_stats_bar", true),
                isChecked -> prefs.edit().putBoolean("show_stats_bar", isChecked).apply()
        ));

        editorOptionsCard.addView(createSettingsSwitchRow(
                "إظهار شريط أدوات التحرير أعلى الورقة",
                prefs.getBoolean("show_editor_toolbar", true),
                isChecked -> prefs.edit().putBoolean("show_editor_toolbar", isChecked).apply()
        ));

        // Segmented Control: [ نصوص | رموز ]
        editorOptionsCard.addView(createToolbarModeSegmentedRow(null));

        Button customizeToolbarBtn = new Button(this);
        configureTextButton(customizeToolbarBtn, "🛠  تخصيص وترتيب أدوات التحرير", 14, true, getAccentTextColor(), 44);
        customizeToolbarBtn.setBackground(roundedRect(getLightAccentColor(), 12, 0, 0));
        customizeToolbarBtn.setOnClickListener(v -> showToolbarCustomizationDialog());
        LinearLayout.LayoutParams ctbLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(44));
        ctbLp.topMargin = dp(10);
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

        showStyledDialog(dialog, root, 0.93f, (int) (getResources().getDisplayMetrics().heightPixels * 0.86));
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

    // Modern Illuminated Sliding Circle Switch Row
    private View createSettingsSwitchRow(String label, boolean initialChecked, OnCheckChange listener) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        row.setPadding(dp(4), dp(9), dp(4), dp(9));

        TextView tv = new TextView(this);
        tv.setText(label);
        tv.setTextColor(getTextPrimaryColor());
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
        tv.setTypeface(getAppTypeface(false));
        tv.setIncludeFontPadding(false);
        tv.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams tvLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        tvLp.leftMargin = dp(10);
        row.addView(tv, tvLp);

        final boolean[] state = new boolean[]{initialChecked};
        FrameLayout switchTrack = new FrameLayout(this);
        switchTrack.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);

        View thumbCircle = new View(this);
        GradientDrawable thumbBg = new GradientDrawable();
        thumbBg.setShape(GradientDrawable.OVAL);
        thumbBg.setColor(Color.WHITE);
        thumbCircle.setBackground(thumbBg);
        thumbCircle.setElevation(dp(2));

        FrameLayout.LayoutParams thumbLp = new FrameLayout.LayoutParams(dp(20), dp(20), Gravity.CENTER_VERTICAL | Gravity.LEFT);
        thumbLp.leftMargin = dp(3);
        switchTrack.addView(thumbCircle, thumbLp);

        int offColor = isDarkMode() ? Color.parseColor("#475569") : Color.parseColor("#CBD5E1");
        float travelPx = dp(22);

        Runnable applyVisualState = () -> {
            int trackColor = state[0] ? getPrimaryColor() : offColor;
            switchTrack.setBackground(roundedRect(trackColor, 14, 0, 0));
            thumbCircle.animate().translationX(state[0] ? travelPx : 0f).setDuration(160).start();
        };
        int initTrackColor = state[0] ? getPrimaryColor() : offColor;
        switchTrack.setBackground(roundedRect(initTrackColor, 14, 0, 0));
        thumbCircle.setTranslationX(state[0] ? travelPx : 0f);

        row.setOnClickListener(v -> {
            state[0] = !state[0];
            applyVisualState.run();
            listener.onChanged(state[0]);
        });

        row.addView(switchTrack, new LinearLayout.LayoutParams(dp(48), dp(26)));
        return row;
    }

    // Modern Segmented Control: [ نصوص | رموز ]
    private View createToolbarModeSegmentedRow(Runnable onModeChangedCallback) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        row.setPadding(dp(4), dp(8), dp(4), dp(6));

        TextView label = new TextView(this);
        label.setText("شكل عرض الأدوات:");
        label.setTextColor(getTextPrimaryColor());
        label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
        label.setTypeface(getAppTypeface(true));
        label.setIncludeFontPadding(false);
        label.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams lLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        row.addView(label, lLp);

        LinearLayout segBox = new LinearLayout(this);
        segBox.setOrientation(LinearLayout.HORIZONTAL);
        segBox.setBackground(roundedRect(getInputBgColor(), 10, 1, getBorderColor()));
        segBox.setPadding(dp(3), dp(3), dp(3), dp(3));

        TextView textModeBtn = new TextView(this);
        textModeBtn.setText("نصوص");
        textModeBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
        textModeBtn.setGravity(Gravity.CENTER);
        textModeBtn.setIncludeFontPadding(false);
        textModeBtn.setPadding(dp(14), dp(6), dp(14), dp(6));

        TextView iconModeBtn = new TextView(this);
        iconModeBtn.setText("رموز");
        iconModeBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
        iconModeBtn.setGravity(Gravity.CENTER);
        iconModeBtn.setIncludeFontPadding(false);
        iconModeBtn.setPadding(dp(14), dp(6), dp(14), dp(6));

        Runnable refreshSegmentUI = () -> {
            boolean iconsOnly = prefs.getBoolean("toolbar_icons_only", false);
            if (!iconsOnly) {
                textModeBtn.setBackground(roundedRect(getPrimaryColor(), 8, 0, 0));
                textModeBtn.setTextColor(Color.WHITE);
                textModeBtn.setTypeface(getAppTypeface(true));

                iconModeBtn.setBackgroundColor(Color.TRANSPARENT);
                iconModeBtn.setTextColor(getTextSecondaryColor());
                iconModeBtn.setTypeface(getAppTypeface(false));
            } else {
                iconModeBtn.setBackground(roundedRect(getPrimaryColor(), 8, 0, 0));
                iconModeBtn.setTextColor(Color.WHITE);
                iconModeBtn.setTypeface(getAppTypeface(true));

                textModeBtn.setBackgroundColor(Color.TRANSPARENT);
                textModeBtn.setTextColor(getTextSecondaryColor());
                textModeBtn.setTypeface(getAppTypeface(false));
            }
        };
        refreshSegmentUI.run();

        textModeBtn.setOnClickListener(v -> {
            prefs.edit().putBoolean("toolbar_icons_only", false).apply();
            refreshSegmentUI.run();
            if (editorToolbarContainer != null) populateEditorToolbar();
            if (onModeChangedCallback != null) onModeChangedCallback.run();
        });

        iconModeBtn.setOnClickListener(v -> {
            prefs.edit().putBoolean("toolbar_icons_only", true).apply();
            refreshSegmentUI.run();
            if (editorToolbarContainer != null) populateEditorToolbar();
            if (onModeChangedCallback != null) onModeChangedCallback.run();
        });

        segBox.addView(textModeBtn);
        segBox.addView(iconModeBtn);
        row.addView(segBox);
        return row;
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

        showStyledDialog(themeDialog, box, 0.88f, ViewGroup.LayoutParams.WRAP_CONTENT);
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
            item.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14.5f);
            item.setTypeface(getTypefaceForIndex(idx, isSelected));
            item.setIncludeFontPadding(false);
            item.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
            item.setBackground(roundedRect(
                    isSelected ? getLightAccentColor() : Color.TRANSPARENT,
                    10,
                    isSelected ? 1 : 0,
                    getPrimaryColor()
            ));
            item.setPadding(dp(14), dp(11), dp(14), dp(11));

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
        LinearLayout.LayoutParams svLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        box.addView(sv, svLp);

        showStyledDialog(fontDialog, box, 0.90f, (int) (getResources().getDisplayMetrics().heightPixels * 0.78));
    }

    // ==================== 5. TOOLBAR CUSTOMIZATION (CONTINUOUS DRAG & DROP FROM TOP TO BOTTOM) ====================

    private void showToolbarCustomizationDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        root.setBackground(roundedRect(getCardBgColor(), 20, 1, getBorderColor()));
        root.setPadding(dp(18), dp(16), dp(18), dp(14));

        TextView title = new TextView(this);
        title.setText("تخصيص وترتيب أدوات التحرير");
        title.setTextColor(getAccentTextColor());
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        title.setTypeface(getAppTypeface(true));
        title.setIncludeFontPadding(false);
        title.setGravity(Gravity.RIGHT);
        root.addView(title);

        // Include the [ نصوص | رموز ] segmented toggle right at the top of customization dialog too
        root.addView(createToolbarModeSegmentedRow(null));

        TextView sub = new TextView(this);
        sub.setText("أمسك مقبض السحب (≡) واسحب الأداة لأي موضع في القائمة ثم اتركها لتسقط في مكانها:");
        sub.setTextColor(getTextSecondaryColor());
        sub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
        sub.setTypeface(getAppTypeface(false));
        sub.setGravity(Gravity.RIGHT);
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        subLp.topMargin = dp(2);
        subLp.bottomMargin = dp(10);
        root.addView(sub, subLp);

        final List<String> currentOrder = new ArrayList<>(getOrderedToolList());
        final Set<String> currentEnabled = new HashSet<>(getEnabledToolsSet());

        ScrollView listScroll = new ScrollView(this);
        LinearLayout listContainer = new LinearLayout(this);
        listContainer.setOrientation(LinearLayout.VERTICAL);
        listContainer.setClipChildren(false);
        listContainer.setClipToPadding(false);
        listScroll.addView(listContainer);

        final List<LinearLayout> rowViews = new ArrayList<>();
        final Runnable[] rebuildListRef = new Runnable[1];

        rebuildListRef[0] = () -> {
            listContainer.removeAllViews();
            rowViews.clear();
            final int stepHeight = dp(54); // 46dp height + 8dp bottomMargin

            for (int i = 0; i < currentOrder.size(); i++) {
                final int origIndex = i;
                final String toolId = currentOrder.get(origIndex);

                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setBackground(roundedRect(getInputBgColor(), 12, 1, getBorderColor()));
                row.setPadding(dp(10), dp(6), dp(10), dp(6));

                LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dp(46));
                rowLp.bottomMargin = dp(8);

                // Drag Handle (≡)
                FrameLayout dragHandle = new FrameLayout(this);
                ImageView dragIcon = new ImageView(this);
                int dragResId = getDrawableId("ic_drag_handle");
                if (dragResId != 0) {
                    dragIcon.setImageResource(dragResId);
                    dragIcon.setImageTintList(ColorStateList.valueOf(getTextSecondaryColor()));
                }
                dragHandle.addView(dragIcon, new FrameLayout.LayoutParams(dp(22), dp(22), Gravity.CENTER));
                row.addView(dragHandle, new LinearLayout.LayoutParams(dp(38), dp(38)));

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

                final float[] downRawY = new float[1];
                final int[] downScrollY = new int[1];
                final int[] targetIndexHolder = new int[]{origIndex};
                final boolean[] isDragging = new boolean[]{false};

                dragHandle.setOnTouchListener((v, event) -> {
                    int action = event.getActionMasked();
                    if (action == MotionEvent.ACTION_DOWN) {
                        downRawY[0] = event.getRawY();
                        downScrollY[0] = listScroll.getScrollY();
                        targetIndexHolder[0] = origIndex;
                        isDragging[0] = true;
                        listScroll.requestDisallowInterceptTouchEvent(true);
                        row.setTranslationZ(dp(16));
                        row.setAlpha(0.95f);
                        row.setBackground(roundedRect(getLightAccentColor(), 12, 2, getPrimaryColor()));
                        return true;
                    } else if (action == MotionEvent.ACTION_MOVE && isDragging[0]) {
                        // Auto-scroll when finger nears top or bottom of ScrollView
                        int[] scrollLoc = new int[2];
                        listScroll.getLocationOnScreen(scrollLoc);
                        float fingerYInScroll = event.getRawY() - scrollLoc[1];
                        int scrollH = listScroll.getHeight();
                        int edgeZone = dp(48);
                        if (fingerYInScroll < edgeZone) {
                            listScroll.scrollBy(0, -dp(8));
                        } else if (fingerYInScroll > scrollH - edgeZone) {
                            listScroll.scrollBy(0, dp(8));
                        }

                        int scrollDelta = listScroll.getScrollY() - downScrollY[0];
                        float totalDy = (event.getRawY() - downRawY[0]) + scrollDelta;

                        // Clamp visual translation within list bounds
                        float minDy = -origIndex * (float) stepHeight;
                        float maxDy = (currentOrder.size() - 1 - origIndex) * (float) stepHeight;
                        float clampedDy = Math.max(minDy, Math.min(maxDy, totalDy));
                        row.setTranslationY(clampedDy);

                        int shift = Math.round(clampedDy / stepHeight);
                        int newTarget = Math.max(0, Math.min(currentOrder.size() - 1, origIndex + shift));
                        targetIndexHolder[0] = newTarget;

                        // Smoothly shift intermediate rows up or down to open a gap without detaching views
                        for (int j = 0; j < rowViews.size(); j++) {
                            if (j == origIndex) continue;
                            View other = rowViews.get(j);
                            float targetTransY = 0f;
                            if (origIndex < newTarget && j > origIndex && j <= newTarget) {
                                targetTransY = -stepHeight;
                            } else if (origIndex > newTarget && j >= newTarget && j < origIndex) {
                                targetTransY = stepHeight;
                            }
                            other.setTranslationY(targetTransY);
                        }
                        return true;
                    } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                        if (isDragging[0]) {
                            isDragging[0] = false;
                            listScroll.requestDisallowInterceptTouchEvent(false);
                            int finalTarget = targetIndexHolder[0];
                            if (finalTarget != origIndex && finalTarget >= 0 && finalTarget < currentOrder.size()) {
                                String moved = currentOrder.remove(origIndex);
                                currentOrder.add(finalTarget, moved);
                            }
                            listContainer.post(rebuildListRef[0]);
                        }
                        return true;
                    }
                    return false;
                });

                rowViews.add(row);
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

        showStyledDialog(dialog, root, 0.92f, (int) (getResources().getDisplayMetrics().heightPixels * 0.84));
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

        showStyledDialog(menuDialog, root, 0.86f, ViewGroup.LayoutParams.WRAP_CONTENT);
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

    private void createSafeSnapshot(NoteFile file, boolean silent) {
        if (folderUri == null || file == null) return;
        try {
            Uri vaultDirUri = getOrCreateSafeVaultDirectoryUri(true);
            if (vaultDirUri == null) {
                if (!silent) {
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
                if (!silent) {
                    Toast.makeText(this, "تم إنشاء نسخة آمنة في الخزنة بنجاح", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            if (!silent) {
                Toast.makeText(this, "تعذر إنشاء النسخة الآمنة", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void renameAssociatedSafeSnapshots(String oldFileName, String newFileName) {
        Uri vaultDirUri = getOrCreateSafeVaultDirectoryUri(false);
        if (vaultDirUri == null) return;

        String oldPrefix = getSafeSnapshotPrefix(oldFileName);
        String newPrefix = getSafeSnapshotPrefix(newFileName);
        if (oldPrefix.equals(newPrefix)) return;

        try {
            String vaultDocId = DocumentsContract.getDocumentId(vaultDirUri);
            Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(folderUri, vaultDocId);
            String[] projection = new String[]{
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME
            };

            try (Cursor c = getContentResolver().query(childrenUri, projection, null, null, null)) {
                if (c != null) {
                    while (c.moveToNext()) {
                        String docId = c.getString(0);
                        String name = c.getString(1);
                        if (name != null && name.startsWith(oldPrefix)) {
                            Uri snapUri = DocumentsContract.buildDocumentUriUsingTree(folderUri, docId);
                            String updatedSnapName = newPrefix + name.substring(oldPrefix.length());
                            try {
                                DocumentsContract.renameDocument(getContentResolver(), snapUri, updatedSnapName);
                            } catch (Exception ignored) {
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {
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
                    showSafeSnapshotPreviewDialog(file, snap, snapshots);
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

        showStyledDialog(dialog, root, 0.90f, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private void showSafeSnapshotPreviewDialog(NoteFile targetFile, SafeSnapshot snap, List<SafeSnapshot> allSnapshots) {
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
            // Silently check if current file has unsaved updates not in the latest snapshot
            String currentFileContent = readFileContent(targetFile.uri);
            if (!currentFileContent.trim().isEmpty() && !currentFileContent.equals(snapContent)) {
                boolean alreadySavedInLatest = false;
                if (allSnapshots != null && !allSnapshots.isEmpty()) {
                    String latestSnapBody = readFileContent(allSnapshots.get(0).uri);
                    if (currentFileContent.equals(latestSnapBody)) {
                        alreadySavedInLatest = true;
                    }
                }
                if (!alreadySavedInLatest) {
                    createSafeSnapshot(targetFile, true);
                }
            }

            writeFileContent(targetFile.uri, snapContent);
            contentCache.put(targetFile.uri.toString(), snapContent);
            Toast.makeText(this, "تم استعادة النسخة الآمنة إلى الملف بنجاح", Toast.LENGTH_SHORT).show();
            previewDialog.dismiss();
            openNoteFile(targetFile, ScreenState.READ);
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

        showStyledDialog(previewDialog, root, 0.92f, (int) (getResources().getDisplayMetrics().heightPixels * 0.78));
    }

    // ==================== 7. LUXURY CUSTOM DIALOGS FOR FILE OPERATIONS ====================

    private void showStyledDialog(Dialog dialog, View rootView, float widthFraction, int heightSpec) {
        dialog.setContentView(rootView);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * widthFraction),
                    heightSpec
            );
        }
        dialog.show();
    }

    private void onNewFileClicked() {
        if (folderUri == null) {
            Toast.makeText(this, "يرجى اختيار مجلد لحفظ الملفات أولاً", Toast.LENGTH_SHORT).show();
            pickFolder();
            return;
        }

        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        root.setBackground(roundedRect(getCardBgColor(), 18, 1, getBorderColor()));
        root.setPadding(dp(20), dp(18), dp(20), dp(16));

        TextView title = new TextView(this);
        title.setText("إنشاء ملف نصي جديد");
        title.setTextColor(getAccentTextColor());
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        title.setTypeface(getAppTypeface(true));
        title.setGravity(Gravity.RIGHT);
        root.addView(title);

        EditText input = new EditText(this);
        input.setHint("اسم الملف (مثال: مقالة التراث الحضرمي)");
        input.setHintTextColor(getTextSecondaryColor());
        input.setTextColor(getTextPrimaryColor());
        input.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14.5f);
        input.setSingleLine(true);
        input.setTypeface(getAppTypeface(false));
        input.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        input.setBackground(roundedRect(getInputBgColor(), 10, 1, getBorderColor()));
        input.setPadding(dp(14), dp(12), dp(14), dp(12));
        LinearLayout.LayoutParams inLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        inLp.topMargin = dp(12);
        inLp.bottomMargin = dp(16);
        root.addView(input, inLp);

        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);

        Button createBtn = new Button(this);
        configureTextButton(createBtn, "إنشاء الملف", 14, true, Color.WHITE, 44);
        createBtn.setBackground(roundedRect(getPrimaryColor(), 10, 0, 0));
        createBtn.setOnClickListener(v -> {
            String rawName = input.getText().toString().trim();
            if (rawName.isEmpty()) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.US);
                rawName = "مذكرة_" + sdf.format(new Date());
            }
            dialog.dismiss();
            createNewTxtFile(rawName);
        });
        LinearLayout.LayoutParams b1Lp = new LinearLayout.LayoutParams(0, dp(44), 1.2f);
        b1Lp.leftMargin = dp(6);
        btnRow.addView(createBtn, b1Lp);

        Button cancelBtn = new Button(this);
        configureTextButton(cancelBtn, "إلغاء", 14, true, getTextPrimaryColor(), 44);
        cancelBtn.setBackground(roundedRect(getSecondaryBtnBgColor(), 10, 1, getBorderColor()));
        cancelBtn.setOnClickListener(v -> dialog.dismiss());
        LinearLayout.LayoutParams b2Lp = new LinearLayout.LayoutParams(0, dp(44), 1f);
        b2Lp.rightMargin = dp(6);
        btnRow.addView(cancelBtn, b2Lp);

        root.addView(btnRow);
        showStyledDialog(dialog, root, 0.90f, ViewGroup.LayoutParams.WRAP_CONTENT);
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

        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        root.setBackground(roundedRect(getCardBgColor(), 18, 1, getBorderColor()));
        root.setPadding(dp(20), dp(18), dp(20), dp(16));

        TextView title = new TextView(this);
        title.setText("إعادة تسمية الملف");
        title.setTextColor(getAccentTextColor());
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        title.setTypeface(getAppTypeface(true));
        title.setGravity(Gravity.RIGHT);
        root.addView(title);

        EditText input = new EditText(this);
        input.setText(stripTxtExtension(file.name));
        input.setTextColor(getTextPrimaryColor());
        input.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14.5f);
        input.setSingleLine(true);
        input.setTypeface(getAppTypeface(false));
        input.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        input.setBackground(roundedRect(getInputBgColor(), 10, 1, getBorderColor()));
        input.setPadding(dp(14), dp(12), dp(14), dp(12));
        input.setSelection(input.getText().length());
        LinearLayout.LayoutParams inLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        inLp.topMargin = dp(12);
        inLp.bottomMargin = dp(16);
        root.addView(input, inLp);

        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);

        Button saveBtn = new Button(this);
        configureTextButton(saveBtn, "حفظ الاسم", 14, true, Color.WHITE, 44);
        saveBtn.setBackground(roundedRect(getPrimaryColor(), 10, 0, 0));
        saveBtn.setOnClickListener(v -> {
            String newBase = input.getText().toString().trim();
            if (newBase.isEmpty()) return;
            String oldName = file.name;
            String oldUriStr = file.uri.toString();
            String newFullName = ensureTxtExtension(newBase);
            dialog.dismiss();

            try {
                Uri renamedUri = DocumentsContract.renameDocument(getContentResolver(), file.uri, newFullName);
                if (renamedUri != null) {
                    // 1. Preserve Pin state across URI change
                    Set<String> pinned = new HashSet<>(prefs.getStringSet("pinned_files", new HashSet<>()));
                    if (pinned.contains(oldUriStr) || file.pinned) {
                        pinned.remove(oldUriStr);
                        pinned.add(renamedUri.toString());
                        prefs.edit().putStringSet("pinned_files", pinned).apply();
                        file.pinned = true;
                    }

                    // 2. Update cached content map
                    String cached = contentCache.remove(oldUriStr);
                    if (cached != null) {
                        contentCache.put(renamedUri.toString(), cached);
                    }

                    // 3. Rename associated Safe Snapshots in Vault so they stay linked
                    renameAssociatedSafeSnapshots(oldName, newFullName);

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
        LinearLayout.LayoutParams b1Lp = new LinearLayout.LayoutParams(0, dp(44), 1.2f);
        b1Lp.leftMargin = dp(6);
        btnRow.addView(saveBtn, b1Lp);

        Button cancelBtn = new Button(this);
        configureTextButton(cancelBtn, "إلغاء", 14, true, getTextPrimaryColor(), 44);
        cancelBtn.setBackground(roundedRect(getSecondaryBtnBgColor(), 10, 1, getBorderColor()));
        cancelBtn.setOnClickListener(v -> dialog.dismiss());
        LinearLayout.LayoutParams b2Lp = new LinearLayout.LayoutParams(0, dp(44), 1f);
        b2Lp.rightMargin = dp(6);
        btnRow.addView(cancelBtn, b2Lp);

        root.addView(btnRow);
        showStyledDialog(dialog, root, 0.90f, ViewGroup.LayoutParams.WRAP_CONTENT);
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
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        root.setBackground(roundedRect(getCardBgColor(), 18, 1, getBorderColor()));
        root.setPadding(dp(20), dp(18), dp(20), dp(16));

        TextView title = new TextView(this);
        title.setText("تأكيد حذف الملف");
        title.setTextColor(Color.parseColor("#DC2626"));
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        title.setTypeface(getAppTypeface(true));
        title.setGravity(Gravity.RIGHT);
        root.addView(title);

        TextView msg = new TextView(this);
        msg.setText("هل أنت متأكد من حذف الملف \"" + stripTxtExtension(file.name) + "\"؟");
        msg.setTextColor(getTextPrimaryColor());
        msg.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14.5f);
        msg.setTypeface(getAppTypeface(false));
        msg.setGravity(Gravity.RIGHT);
        msg.setLineSpacing(dp(4), 1.1f);
        LinearLayout.LayoutParams mLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        mLp.topMargin = dp(10);
        mLp.bottomMargin = dp(18);
        root.addView(msg, mLp);

        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);

        // Distinct Red Delete Button regardless of active theme
        Button deleteBtn = new Button(this);
        configureTextButton(deleteBtn, "حذف الملف", 14, true, Color.WHITE, 44);
        deleteBtn.setBackground(roundedRect(Color.parseColor("#DC2626"), 10, 0, 0));
        deleteBtn.setOnClickListener(v -> {
            dialog.dismiss();
            try {
                // Save an automatic safe copy in the phone's vault directory before deletion
                createSafeSnapshot(file, true);

                boolean deleted = DocumentsContract.deleteDocument(getContentResolver(), file.uri);
                if (deleted) {
                    contentCache.remove(file.uri.toString());
                    Set<String> pinned = new HashSet<>(prefs.getStringSet("pinned_files", new HashSet<>()));
                    if (pinned.remove(file.uri.toString())) {
                        prefs.edit().putStringSet("pinned_files", pinned).apply();
                    }
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
        LinearLayout.LayoutParams b1Lp = new LinearLayout.LayoutParams(0, dp(44), 1.2f);
        b1Lp.leftMargin = dp(6);
        btnRow.addView(deleteBtn, b1Lp);

        Button cancelBtn = new Button(this);
        configureTextButton(cancelBtn, "إلغاء", 14, true, getTextPrimaryColor(), 44);
        cancelBtn.setBackground(roundedRect(getSecondaryBtnBgColor(), 10, 1, getBorderColor()));
        cancelBtn.setOnClickListener(v -> dialog.dismiss());
        LinearLayout.LayoutParams b2Lp = new LinearLayout.LayoutParams(0, dp(44), 1f);
        b2Lp.rightMargin = dp(6);
        btnRow.addView(cancelBtn, b2Lp);

        root.addView(btnRow);
        showStyledDialog(dialog, root, 0.88f, ViewGroup.LayoutParams.WRAP_CONTENT);
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
                savedHomeScrollY = 0;
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
