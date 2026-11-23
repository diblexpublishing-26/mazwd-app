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
import android.graphics.drawable.RippleDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.DocumentsContract;
import android.text.Editable;
import android.text.InputType;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableString;
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
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedOutputStream;
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
    private static final int REQ_CREATE_ZIP = 1002;
    private static final String PREFS_NAME = "mazwd_prefs";
    private static final String KEY_FOLDER_URI = "folder_uri";
    private static final String KEY_PINNED_URIS = "pinned_files";
    private static final String KEY_ZOOM_PERCENT = "zoom_percent";
    private static final String KEY_SIGNATURE = "custom_signature";
    private static final String KEY_SIG_INITIALIZED = "sig_init_v3";
    private static final String KEY_THEME_INDEX = "theme_index";
    private static final String KEY_FONT_INDEX = "font_index";
    private static final String KEY_DARK_MODE = "dark_mode";
    private static final String KEY_SHOW_FAST_SLIDER = "show_fast_slider";
    private static final String KEY_SHOW_JUMP_BTN = "show_jump_btn";
    private static final String KEY_SHOW_STATS_BAR = "show_stats_bar";
    private static final String KEY_SHOW_EDITOR_TOOLBAR = "show_editor_toolbar";
    private static final String KEY_TOOLBAR_ICONS_ONLY = "toolbar_icons_only";
    private static final String KEY_TOOLBAR_ORDER = "toolbar_order_v3";
    private static final String KEY_ENABLED_TOOLS = "enabled_tools_v3";
    private static final String VAULT_FOLDER_NAME = "خزنة_النسخ_الآمنة";
    private static final String DEFAULT_SIGNATURE = "أ. شفيقة مزود | باحثة في التراث الحضرمي";

    private static final String[] THEME_NAMES = {
            "الكحلي الملكي",
            "الأخضر الزمردي",
            "العنابي الفاخر",
            "البني التراثي",
            "البترولي العصري",
            "البنفسجي الأنيق",
            "الرمادي الرسمي",
            "الأسود الماسي"
    };

    private static final int[] THEME_PRIMARY = {
            0xFF1E3A8A, 0xFF065F46, 0xFF881337, 0xFF78350F,
            0xFF0F4C5C, 0xFF5B21B6, 0xFF334155, 0xFF18181B
    };

    private static final int[] THEME_HEADER_BG = {
            0xFF0F172A, 0xFF022C22, 0xFF4C0519, 0xFF451A03,
            0xFF082F3A, 0xFF2E1065, 0xFF0F172A, 0xFF09090B
    };

    private static final int[] THEME_ACCENT = {
            0xFFF59E0B, 0xFFD97706, 0xFFE11D48, 0xFFB45309,
            0xFFCCFBF1, 0xFF8B5CF6, 0xFF475569, 0xFFF4F4F5
    };

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

    private static final String[] DEFAULT_ENABLED_TOOLS = {
            "undo", "redo", "copy", "paste", "select_all", "auto_format", "signature"
    };

    private enum Screen { HOME, DOCUMENT, SETTINGS }
    private Screen currentScreen = Screen.HOME;
    private boolean isEditMode = false;

    private SharedPreferences prefs;
    private FrameLayout rootContainer;

    private Uri folderTreeUri;
    private final List<FileItem> allFiles = new ArrayList<>();
    private final Map<String, String> contentCache = new HashMap<>();
    private String currentSearchQuery = "";

    private Uri currentFileUri;
    private String currentFileName = "";
    private String currentFileContent = "";
    private String lastDiskSavedContent = "";

    private ScrollView homeScrollView;
    private int savedHomeScrollY = 0;

    private EditText editorEditText;
    private TextView readTextView;
    private ScrollView readScrollView;
    private TextView docTitleView;
    private TextView zoomValueText;
    private TextView statsBarText;
    private LinearLayout homeFilesContainer;
    private EditText searchInput;

    private FrameLayout sliderTouchContainer;
    private View sliderThumb;
    private TextView sliderBubble;
    private LinearLayout jumpMenuCard;
    private boolean isDraggingSlider = false;

    private final List<String> undoStack = new ArrayList<>();
    private int undoIndex = -1;
    private boolean isUndoRedoAction = false;

    private final Handler saveHandler = new Handler(Looper.getMainLooper());
    private final Runnable autoSaveRunnable = new Runnable() {
        @Override
        public void run() {
            if (currentScreen == Screen.DOCUMENT && isEditMode && currentFileUri != null && editorEditText != null) {
                saveCurrentDocumentNow();
            }
        }
    };
    private final Runnable fadeSliderRunnable = new Runnable() {
        @Override
        public void run() {
            if (sliderThumb != null && !isDraggingSlider) {
                sliderThumb.animate().alpha(0.38f).setDuration(250).start();
            }
        }
    };

    private static class FileItem {
        Uri uri;
        String name;
        long lastModified;
        long size;
        boolean pinned;
    }

    private static class SnapshotItem {
        Uri uri;
        String fullFileName;
        String displayDate;
        long lastModified;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        if (!prefs.getBoolean(KEY_SIG_INITIALIZED, false)) {
            prefs.edit()
                    .putString(KEY_SIGNATURE, DEFAULT_SIGNATURE)
                    .putBoolean(KEY_SIG_INITIALIZED, true)
                    .apply();
        }

        updateStatusBarColor();

        rootContainer = new FrameLayout(this);
        rootContainer.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        rootContainer.setBackgroundColor(getBgMainColor());
        setContentView(rootContainer);

        String savedUri = prefs.getString(KEY_FOLDER_URI, null);
        if (savedUri != null) {
            try {
                folderTreeUri = Uri.parse(savedUri);
            } catch (Exception ignored) {
                folderTreeUri = null;
            }
        }

        showHomeScreen();
    }

    private boolean isDarkMode() {
        return prefs.getBoolean(KEY_DARK_MODE, false);
    }

    private int getBgMainColor() {
        return isDarkMode() ? 0xFF0F172A : 0xFFF4F6FA;
    }

    private int getCardBgColor() {
        return isDarkMode() ? 0xFF1E293B : 0xFFFFFFFF;
    }

    private int getInputBgColor() {
        return isDarkMode() ? 0xFF0F172A : 0xFFF8FAFC;
    }

    private int getSecondaryBtnBgColor() {
        return isDarkMode() ? 0xFF334155 : 0xFFF1F5F9;
    }

    private int getTextPrimaryColor() {
        return isDarkMode() ? 0xFFF1F5F9 : 0xFF0F172A;
    }

    private int getTextSecondaryColor() {
        return isDarkMode() ? 0xFF94A3B8 : 0xFF64748B;
    }

    private int getBorderColor() {
        return isDarkMode() ? 0xFF334155 : 0xFFE2E8F0;
    }

    private void updateStatusBarColor() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(getHeaderBgColor());
        }
    }

    private int getThemeIndex() {
        int idx = prefs.getInt(KEY_THEME_INDEX, 0);
        if (idx < 0 || idx >= THEME_NAMES.length) idx = 0;
        return idx;
    }

    private int getPrimaryColor() {
        return THEME_PRIMARY[getThemeIndex()];
    }

    private int getHeaderBgColor() {
        return THEME_HEADER_BG[getThemeIndex()];
    }

    private int getAccentColor() {
        return THEME_ACCENT[getThemeIndex()];
    }

    private int getFontIndex() {
        int idx = prefs.getInt(KEY_FONT_INDEX, 0);
        if (idx < 0 || idx >= FONT_NAMES.length) idx = 0;
        return idx;
    }

    private Typeface getTypefaceByIndex(int idx, boolean bold) {
        int style = bold ? Typeface.BOLD : Typeface.NORMAL;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                String resName = null;
                if (idx == 1) resName = bold ? "tajawal_bold" : "tajawal_regular";
                else if (idx == 2) resName = bold ? "mazwd_arabic_bold" : "mazwd_arabic";
                else if (idx == 3) resName = "scheherazade_bold";
                else if (idx == 4) resName = bold ? "readex_pro_bold" : "readex_pro";
                else if (idx == 5) resName = bold ? "noto_sans_arabic_bold" : "noto_sans_arabic";
                else if (idx == 6) resName = bold ? "noto_naskh_arabic_bold" : "noto_naskh_arabic";
                else if (idx == 7) resName = bold ? "ibm_plex_arabic_bold" : "ibm_plex_arabic";
                else if (idx == 8) resName = bold ? "aref_ruqaa_bold" : "aref_ruqaa";
                else if (idx == 9) resName = bold ? "reem_kufi_bold" : "reem_kufi";

                if (resName != null) {
                    int fontRes = getResources().getIdentifier(resName, "font", getPackageName());
                    if (fontRes != 0) {
                        Typeface tf = getResources().getFont(fontRes);
                        if (tf != null) return Typeface.create(tf, style);
                    }
                }
            }
        } catch (Exception ignored) {}

        switch (idx) {
            case 10:
                return Typeface.create(Typeface.SERIF, style);
            case 11:
                return Typeface.create("cursive", style);
            default:
                return Typeface.create(Typeface.SANS_SERIF, style);
        }
    }

    private void applyAppFont(TextView view, boolean bold) {
        if (view == null) return;
        int idx = getFontIndex();
        view.setTypeface(getTypefaceByIndex(idx, bold));
        view.setIncludeFontPadding(idx != 3);
    }

    private ImageView createVectorIcon(String drawableName, int color, int sizeDp) {
        ImageView iv = new ImageView(this);
        int resId = getResources().getIdentifier(drawableName, "drawable", getPackageName());
        if (resId != 0) {
            iv.setImageResource(resId);
        }
        iv.setImageTintList(ColorStateList.valueOf(color));
        iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(sizeDp), dp(sizeDp));
        lp.gravity = Gravity.CENTER;
        iv.setLayoutParams(lp);
        return iv;
    }

    private List<String> getOrderedToolIds() {
        String saved = prefs.getString(KEY_TOOLBAR_ORDER, null);
        List<String> result = new ArrayList<>();
        if (saved != null && !saved.isEmpty()) {
            String[] parts = saved.split(",");
            for (String p : parts) {
                if (!p.isEmpty() && isValidToolId(p) && !result.contains(p)) {
                    result.add(p);
                }
            }
        }
        for (String id : TOOL_IDS) {
            if (!result.contains(id)) result.add(id);
        }
        return result;
    }

    private boolean isValidToolId(String id) {
        for (String t : TOOL_IDS) {
            if (t.equals(id)) return true;
        }
        return false;
    }

    private Set<String> getEnabledToolIds() {
        Set<String> saved = prefs.getStringSet(KEY_ENABLED_TOOLS, null);
        if (saved == null) {
            return new HashSet<>(Arrays.asList(DEFAULT_ENABLED_TOOLS));
        }
        return new HashSet<>(saved);
    }

    private String getToolLabel(String id) {
        for (int i = 0; i < TOOL_IDS.length; i++) {
            if (TOOL_IDS[i].equals(id)) return TOOL_LABELS[i];
        }
        return id;
    }

    private String getToolIconRes(String id) {
        for (int i = 0; i < TOOL_IDS.length; i++) {
            if (TOOL_IDS[i].equals(id)) return TOOL_ICON_RES[i];
        }
        return "ic_auto_format";
    }

    private Dialog createBaseDialog(LinearLayout card) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(card);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.92f);
            dialog.getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        return dialog;
    }

    private Dialog showStyledDialog(String titleText, String subtitleText, View contentView,
                                    String positiveText, boolean positiveDanger, Runnable onPositive,
                                    String negativeText, Runnable onNegative) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        card.setClipChildren(false);
        card.setClipToPadding(false);
        card.setPadding(dp(20), dp(20), dp(20), dp(20));
        card.setBackground(createRoundedRect(getCardBgColor(), dp(20), getBorderColor(), dp(1)));

        final Dialog dialog = createBaseDialog(card);

        if (titleText != null && !titleText.isEmpty()) {
            TextView title = new TextView(this);
            title.setText(titleText);
            title.setTextColor(getTextPrimaryColor());
            title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
            applyAppFont(title, true);
            card.addView(title);
        }

        if (subtitleText != null && !subtitleText.isEmpty()) {
            TextView sub = new TextView(this);
            sub.setText(subtitleText);
            sub.setTextColor(getTextSecondaryColor());
            sub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
            sub.setLineSpacing(0f, 1.2f);
            applyAppFont(sub, false);
            LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            subLp.topMargin = dp(8);
            card.addView(sub, subLp);
        }

        if (contentView != null) {
            LinearLayout.LayoutParams contentLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            contentLp.topMargin = dp(14);
            card.addView(contentView, contentLp);
        }

        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setGravity(Gravity.CENTER_VERTICAL);
        btnRow.setClipChildren(false);
        btnRow.setClipToPadding(false);
        btnRow.setPadding(dp(2), dp(4), dp(2), dp(6));
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rowLp.topMargin = dp(14);
        card.addView(btnRow, rowLp);

        if (positiveText != null) {
            int bg = positiveDanger ? 0xFFDC2626 : getPrimaryColor();
            TextView posBtn = createButton(positiveText, bg, Color.WHITE, true, v -> {
                dialog.dismiss();
                if (onPositive != null) onPositive.run();
            });
            LinearLayout.LayoutParams posLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            if (negativeText != null) posLp.leftMargin = dp(8);
            posLp.bottomMargin = dp(4);
            btnRow.addView(posBtn, posLp);
        }

        if (negativeText != null) {
            TextView negBtn = createButton(negativeText, getSecondaryBtnBgColor(), getTextPrimaryColor(), false, v -> {
                dialog.dismiss();
                if (onNegative != null) onNegative.run();
            });
            LinearLayout.LayoutParams negLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            negLp.bottomMargin = dp(4);
            btnRow.addView(negBtn, negLp);
        }

        dialog.show();
        return dialog;
    }

    // ==================== 1. HOME SCREEN ====================

    private void showHomeScreen() {
        currentScreen = Screen.HOME;
        isEditMode = false;
        hideKeyboard();
        updateStatusBarColor();
        rootContainer.removeAllViews();
        rootContainer.setBackgroundColor(getBgMainColor());

        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        mainLayout.setBackgroundColor(getBgMainColor());
        rootContainer.addView(mainLayout, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout fixedTopWrapper = new LinearLayout(this);
        fixedTopWrapper.setOrientation(LinearLayout.VERTICAL);
        fixedTopWrapper.setPadding(dp(16), dp(14), dp(16), dp(6));
        fixedTopWrapper.setClipChildren(false);
        fixedTopWrapper.setClipToPadding(false);
        mainLayout.addView(fixedTopWrapper, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout headerCard = new LinearLayout(this);
        headerCard.setOrientation(LinearLayout.VERTICAL);
        headerCard.setGravity(Gravity.CENTER_HORIZONTAL);
        headerCard.setBackground(createRoundedRect(getHeaderBgColor(), dp(22), getAccentColor(), dp(1)));
        headerCard.setPadding(dp(20), dp(18), dp(20), dp(18));
        headerCard.setClipChildren(false);
        headerCard.setClipToPadding(false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            headerCard.setElevation(dp(4));
        }

        TextView titleTv = new TextView(this);
        titleTv.setText("مفكرة أ. شفيقة مزود");
        titleTv.setTextColor(Color.WHITE);
        titleTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 21);
        titleTv.setGravity(Gravity.CENTER);
        applyAppFont(titleTv, true);
        headerCard.addView(titleTv, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView subtitleTv = new TextView(this);
        subtitleTv.setText("كاتبة وباحثة في التراث الحضرمي");
        subtitleTv.setTextColor(getAccentColor());
        subtitleTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
        subtitleTv.setGravity(Gravity.CENTER);
        applyAppFont(subtitleTv, false);
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        subLp.topMargin = dp(4);
        headerCard.addView(subtitleTv, subLp);

        TextView newFileBtn = createButton("＋  ملف جديد", getPrimaryColor(), Color.WHITE, true, v -> onNewFileClicked());
        newFileBtn.setPadding(dp(14), 0, dp(14), 0);
        newFileBtn.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams newBtnLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(46));
        newBtnLp.topMargin = dp(14);
        newBtnLp.bottomMargin = dp(2);
        headerCard.addView(newFileBtn, newBtnLp);

        fixedTopWrapper.addView(headerCard, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        homeScrollView = new ScrollView(this);
        homeScrollView.setFillViewport(true);
        LinearLayout.LayoutParams scrollLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        mainLayout.addView(homeScrollView, scrollLp);

        LinearLayout scrollContent = new LinearLayout(this);
        scrollContent.setOrientation(LinearLayout.VERTICAL);
        scrollContent.setPadding(dp(16), dp(8), dp(16), dp(16));
        homeScrollView.addView(scrollContent, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout sectionRow = new LinearLayout(this);
        sectionRow.setOrientation(LinearLayout.HORIZONTAL);
        sectionRow.setGravity(Gravity.CENTER_VERTICAL);
        sectionRow.setPadding(dp(4), dp(2), dp(4), dp(6));

        TextView myFilesTitle = new TextView(this);
        myFilesTitle.setText("ملفاتي النصية");
        myFilesTitle.setTextColor(getTextPrimaryColor());
        myFilesTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        applyAppFont(myFilesTitle, true);
        LinearLayout.LayoutParams mftLp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        sectionRow.addView(myFilesTitle, mftLp);

        TextView txtBadge = new TextView(this);
        txtBadge.setText("TXT");
        txtBadge.setTextColor(isDarkMode() ? Color.WHITE : getPrimaryColor());
        txtBadge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
        applyAppFont(txtBadge, true);
        txtBadge.setPadding(dp(10), dp(3), dp(10), dp(3));
        txtBadge.setBackground(createRoundedRect(isDarkMode() ? 0xFF334155 : 0xFFE2E8F0, dp(10), 0, 0));
        sectionRow.addView(txtBadge);

        scrollContent.addView(sectionRow);

        searchInput = new EditText(this);
        searchInput.setHint("ابحث في أسماء ومحتوى ملفاتك...");
        searchInput.setHintTextColor(getTextSecondaryColor());
        searchInput.setTextColor(getTextPrimaryColor());
        searchInput.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14.5f);
        applyAppFont(searchInput, false);
        searchInput.setSingleLine(true);
        searchInput.setPadding(dp(16), dp(11), dp(16), dp(11));
        searchInput.setBackground(createRoundedRect(getCardBgColor(), dp(14), getBorderColor(), dp(1)));
        searchInput.setText(currentSearchQuery);
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s.toString();
                renderHomeFilesList();
            }
            @Override public void afterTextChanged(Editable s) {}
        });
        LinearLayout.LayoutParams searchLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        searchLp.bottomMargin = dp(12);
        scrollContent.addView(searchInput, searchLp);

        homeFilesContainer = new LinearLayout(this);
        homeFilesContainer.setOrientation(LinearLayout.VERTICAL);
        scrollContent.addView(homeFilesContainer, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout bottomBar = new LinearLayout(this);
        bottomBar.setOrientation(LinearLayout.HORIZONTAL);
        bottomBar.setGravity(Gravity.CENTER_VERTICAL);
        bottomBar.setClipToPadding(false);
        bottomBar.setClipChildren(false);
        bottomBar.setPadding(dp(16), dp(10), dp(16), dp(16));
        bottomBar.setBackgroundColor(getCardBgColor());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            bottomBar.setElevation(dp(8));
        }

        TextView settingsBtn = createButton("⚙  الإعدادات", getSecondaryBtnBgColor(), getTextPrimaryColor(), false, v -> showSettingsScreen());
        LinearLayout.LayoutParams setLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        setLp.leftMargin = dp(10);
        setLp.bottomMargin = dp(4);
        bottomBar.addView(settingsBtn, setLp);

        TextView backupBtn = createButton("نسخ احتياطي (ZIP)", getPrimaryColor(), Color.WHITE, true, v -> startZipBackup());
        LinearLayout.LayoutParams bakLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        bakLp.bottomMargin = dp(4);
        bottomBar.addView(backupBtn, bakLp);

        mainLayout.addView(bottomBar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        loadFilesFromFolder();
        renderHomeFilesList();

        if (savedHomeScrollY > 0) {
            final int restoreY = savedHomeScrollY;
            homeScrollView.post(() -> homeScrollView.scrollTo(0, restoreY));
        }
    }

    private void renderHomeFilesList() {
        if (homeFilesContainer == null) return;
        homeFilesContainer.removeAllViews();

        if (folderTreeUri == null || !hasFolderPermission(folderTreeUri)) {
            LinearLayout card = createCard();
            card.setGravity(Gravity.CENTER);
            card.setPadding(dp(22), dp(26), dp(22), dp(26));

            TextView msg = new TextView(this);
            msg.setText("لبدء استخدام المفكرة وحفظ ملفاتك النصية بأمان في جهازك، يرجى اختيار مجلد الحفظ مرة واحدة فقط.");
            msg.setTextColor(getTextPrimaryColor());
            msg.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
            msg.setGravity(Gravity.CENTER);
            msg.setLineSpacing(0f, 1.25f);
            applyAppFont(msg, false);
            card.addView(msg);

            TextView pickBtn = createButton("اختيار مجلد الحفظ في الهاتف", getPrimaryColor(), Color.WHITE, true, v -> pickFolder());
            LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            btnLp.topMargin = dp(16);
            btnLp.bottomMargin = dp(4);
            card.addView(pickBtn, btnLp);

            homeFilesContainer.addView(card);
            return;
        }

        String q = currentSearchQuery == null ? "" : currentSearchQuery.trim();
        List<FileItem> matched = new ArrayList<>();
        for (FileItem item : allFiles) {
            if (q.isEmpty()) {
                matched.add(item);
            } else {
                boolean nameMatch = smartContains(item.name, q);
                String body = getCachedFileContent(item.uri);
                boolean bodyMatch = smartContains(body, q);
                if (nameMatch || bodyMatch) {
                    matched.add(item);
                }
            }
        }

        if (matched.isEmpty()) {
            LinearLayout emptyCard = createCard();
            emptyCard.setGravity(Gravity.CENTER);
            emptyCard.setPadding(dp(20), dp(28), dp(20), dp(28));

            TextView emptyTv = new TextView(this);
            emptyTv.setText(q.isEmpty()
                    ? "لا توجد ملفات نصية بعد.\nاضغط على «＋ ملف جديد» في الأعلى لإنشاء أول ملف."
                    : "لا توجد نتائج مطابقة لبحثك.");
            emptyTv.setTextColor(getTextSecondaryColor());
            emptyTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
            emptyTv.setGravity(Gravity.CENTER);
            emptyTv.setLineSpacing(0f, 1.25f);
            applyAppFont(emptyTv, false);
            emptyCard.addView(emptyTv);

            homeFilesContainer.addView(emptyCard);
            return;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd  hh:mm a", new Locale("ar"));

        for (FileItem item : matched) {
            LinearLayout card = createCard();
            int strokeColor = item.pinned ? getAccentColor() : getBorderColor();
            int strokeWidth = item.pinned ? dp(1.5f) : dp(1);
            card.setBackground(createRoundedRect(getCardBgColor(), dp(15), strokeColor, strokeWidth));
            card.setPadding(dp(14), dp(12), dp(14), dp(12));

            LinearLayout topRow = new LinearLayout(this);
            topRow.setOrientation(LinearLayout.HORIZONTAL);
            topRow.setGravity(Gravity.CENTER_VERTICAL);

            TextView nameTv = new TextView(this);
            String displayName = stripTxtExtension(item.name);
            String fullTitle = item.pinned ? ("📌 " + displayName) : displayName;
            if (!q.isEmpty() && smartContains(displayName, q)) {
                nameTv.setText(highlightSmartText(fullTitle, q));
            } else {
                nameTv.setText(fullTitle);
            }
            nameTv.setTextColor(getTextPrimaryColor());
            nameTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15.5f);
            applyAppFont(nameTv, true);
            LinearLayout.LayoutParams nameLp = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            topRow.addView(nameTv, nameLp);

            FrameLayout moreBtn = new FrameLayout(this);
            moreBtn.setPadding(dp(8), dp(4), dp(8), dp(4));
            moreBtn.setBackground(createRipple(getSecondaryBtnBgColor(), dp(10)));
            ImageView moreIcon = createVectorIcon("ic_more_vert", getTextPrimaryColor(), 20);
            moreBtn.addView(moreIcon, new FrameLayout.LayoutParams(dp(20), dp(20), Gravity.CENTER));
            moreBtn.setOnClickListener(v -> showFileOptionsDialog(item));
            topRow.addView(moreBtn);

            card.addView(topRow);

            String dateStr = item.lastModified > 0 ? sdf.format(new Date(item.lastModified)) : "";
            TextView metaTv = new TextView(this);
            metaTv.setText(dateStr);
            metaTv.setTextColor(getTextSecondaryColor());
            metaTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            applyAppFont(metaTv, false);
            LinearLayout.LayoutParams metaLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            metaLp.topMargin = dp(3);
            card.addView(metaTv, metaLp);

            if (!q.isEmpty()) {
                String body = getCachedFileContent(item.uri);
                int[] matchRange = findFirstSmartMatch(body, q);
                if (matchRange != null) {
                    int idx = matchRange[0];
                    int start = Math.max(0, idx - 30);
                    int end = Math.min(body.length(), matchRange[1] + 50);
                    String snippet = (start > 0 ? "..." : "") + body.substring(start, end).replace("\n", " ") + (end < body.length() ? "..." : "");

                    TextView snippetTv = new TextView(this);
                    snippetTv.setText(highlightSmartText(snippet, q));
                    snippetTv.setTextColor(getTextPrimaryColor());
                    snippetTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
                    applyAppFont(snippetTv, false);
                    LinearLayout.LayoutParams snipLp = new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    snipLp.topMargin = dp(6);
                    card.addView(snippetTv, snipLp);
                }
            }

            card.setOnClickListener(v -> {
                if (homeScrollView != null) {
                    savedHomeScrollY = homeScrollView.getScrollY();
                }
                openDocument(item.uri, item.name, false);
            });

            LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            cardLp.bottomMargin = dp(9);
            homeFilesContainer.addView(card, cardLp);
        }
    }

    private void showFileOptionsDialog(FileItem item) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        card.setClipChildren(false);
        card.setClipToPadding(false);
        card.setPadding(dp(16), dp(18), dp(16), dp(16));
        card.setBackground(createRoundedRect(getCardBgColor(), dp(20), getBorderColor(), dp(1)));

        final Dialog dialog = createBaseDialog(card);

        TextView header = new TextView(this);
        header.setText(stripTxtExtension(item.name));
        header.setTextColor(getTextPrimaryColor());
        header.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16.5f);
        header.setPadding(dp(8), dp(2), dp(8), dp(10));
        applyAppFont(header, true);
        card.addView(header);

        View divider = new View(this);
        divider.setBackgroundColor(getBorderColor());
        card.addView(divider, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1)));

        String[] options = {
                item.pinned ? "إلغاء تثبيت الملف" : "تثبيت الملف",
                "إعادة تسمية الملف",
                "إنشاء نسخة مكررة من الملف",
                "إنشاء نسخة آمنة",
                "تصفح النسخ الآمنة",
                "مشاركة الملف",
                "حذف الملف"
        };

        for (int i = 0; i < options.length; i++) {
            final int which = i;
            TextView row = new TextView(this);
            row.setText(options[i]);
            row.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
            row.setTextColor(i == 6 ? 0xFFDC2626 : getTextPrimaryColor());
            row.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
            row.setPadding(dp(12), dp(13), dp(12), dp(13));
            row.setBackground(createRipple(Color.TRANSPARENT, dp(10)));
            applyAppFont(row, i == 6);
            row.setOnClickListener(v -> {
                dialog.dismiss();
                if (which == 0) {
                    togglePinFile(item.uri);
                    loadFilesFromFolder();
                    renderHomeFilesList();
                } else if (which == 1) {
                    showRenameDialog(item.uri, item.name, false);
                } else if (which == 2) {
                    duplicateFile(item);
                } else if (which == 3) {
                    createSafeSnapshot(item, false);
                } else if (which == 4) {
                    showSafeSnapshotsListDialog(item);
                } else if (which == 5) {
                    String content = readTextFromUri(item.uri);
                    shareTextContent(stripTxtExtension(item.name), content);
                } else if (which == 6) {
                    confirmDeleteFile(item);
                }
            });
            card.addView(row, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }

        dialog.show();
    }

    // ==================== SAFE SNAPSHOTS VAULT ====================

    private Uri ensureVaultFolder(boolean createIfMissing) {
        if (folderTreeUri == null) return null;
        try {
            String rootDocId = DocumentsContract.getTreeDocumentId(folderTreeUri);
            Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(folderTreeUri, rootDocId);
            Uri foundVaultUri = null;
            try (Cursor cursor = getContentResolver().query(childrenUri, new String[]{
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_MIME_TYPE
            }, null, null, null)) {
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        String docId = cursor.getString(0);
                        String name = cursor.getString(1);
                        String mime = cursor.getString(2);
                        if (VAULT_FOLDER_NAME.equals(name) && DocumentsContract.Document.MIME_TYPE_DIR.equals(mime)) {
                            foundVaultUri = DocumentsContract.buildDocumentUriUsingTree(folderTreeUri, docId);
                            break;
                        }
                    }
                }
            }
            if (foundVaultUri == null && createIfMissing) {
                Uri rootDocUri = DocumentsContract.buildDocumentUriUsingTree(folderTreeUri, rootDocId);
                foundVaultUri = DocumentsContract.createDocument(
                        getContentResolver(), rootDocUri, DocumentsContract.Document.MIME_TYPE_DIR, VAULT_FOLDER_NAME);
            }
            if (foundVaultUri != null) {
                ensureNoMediaInVault(foundVaultUri);
            }
            return foundVaultUri;
        } catch (Exception e) {
            return null;
        }
    }

    private void ensureNoMediaInVault(Uri vaultDocUri) {
        try {
            String vaultDocId = DocumentsContract.getDocumentId(vaultDocUri);
            Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(folderTreeUri, vaultDocId);
            try (Cursor c = getContentResolver().query(childrenUri, new String[]{
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME
            }, null, null, null)) {
                if (c != null) {
                    while (c.moveToNext()) {
                        String name = c.getString(0);
                        if (".nomedia".equals(name)) return;
                    }
                }
            }
            DocumentsContract.createDocument(getContentResolver(), vaultDocUri, "application/octet-stream", ".nomedia");
        } catch (Exception ignored) {}
    }

    private String getSnapshotPrefix(String fileName) {
        return stripTxtExtension(fileName) + "__نسخة_آمنة_";
    }

    private void createSafeSnapshot(FileItem item, boolean silent) {
        String content = readTextFromUri(item.uri);
        createSafeSnapshotFromContent(item.name, content, silent);
    }

    private boolean createSafeSnapshotFromContent(String fileName, String content, boolean silent) {
        try {
            Uri vaultUri = ensureVaultFolder(true);
            if (vaultUri == null) {
                if (!silent) Toast.makeText(this, "تعذر الوصول إلى خزنة النسخ الآمنة", Toast.LENGTH_SHORT).show();
                return false;
            }
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US);
            String stamp = sdf.format(new Date());
            String snapName = getSnapshotPrefix(fileName) + stamp + ".txt";
            Uri snapUri = DocumentsContract.createDocument(getContentResolver(), vaultUri, "text/plain", snapName);
            if (snapUri != null) {
                writeTextToUri(snapUri, content != null ? content : "");
                if (!silent) {
                    Toast.makeText(this, "تم حفظ نسخة آمنة من الملف بنجاح", Toast.LENGTH_SHORT).show();
                }
                return true;
            }
        } catch (Exception e) {
            if (!silent) {
                Toast.makeText(this, "تعذر إنشاء النسخة الآمنة", Toast.LENGTH_SHORT).show();
            }
        }
        return false;
    }

    private void renameAssociatedSafeSnapshots(String oldFileName, String newFileName) {
        Uri vaultUri = ensureVaultFolder(false);
        if (vaultUri == null) return;
        String oldPrefix = getSnapshotPrefix(oldFileName);
        String newPrefix = getSnapshotPrefix(newFileName);
        if (oldPrefix.equals(newPrefix)) return;

        try {
            String vaultDocId = DocumentsContract.getDocumentId(vaultUri);
            Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(folderTreeUri, vaultDocId);
            List<Uri> toRenameUris = new ArrayList<>();
            List<String> toRenameNewNames = new ArrayList<>();

            try (Cursor cursor = getContentResolver().query(childrenUri, new String[]{
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME
            }, null, null, null)) {
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        String docId = cursor.getString(0);
                        String name = cursor.getString(1);
                        if (name != null && name.startsWith(oldPrefix)) {
                            Uri docUri = DocumentsContract.buildDocumentUriUsingTree(folderTreeUri, docId);
                            String updatedName = newPrefix + name.substring(oldPrefix.length());
                            toRenameUris.add(docUri);
                            toRenameNewNames.add(updatedName);
                        }
                    }
                }
            }
            for (int i = 0; i < toRenameUris.size(); i++) {
                try {
                    DocumentsContract.renameDocument(getContentResolver(), toRenameUris.get(i), toRenameNewNames.get(i));
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
    }

    private List<SnapshotItem> getSafeSnapshotsForFile(String fileName) {
        List<SnapshotItem> list = new ArrayList<>();
        Uri vaultUri = ensureVaultFolder(false);
        if (vaultUri == null) return list;

        String prefix = getSnapshotPrefix(fileName);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd — hh:mm:ss a", new Locale("ar"));

        try {
            String vaultDocId = DocumentsContract.getDocumentId(vaultUri);
            Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(folderTreeUri, vaultDocId);
            try (Cursor cursor = getContentResolver().query(childrenUri, new String[]{
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_LAST_MODIFIED
            }, null, null, null)) {
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        String docId = cursor.getString(0);
                        String name = cursor.getString(1);
                        long modified = cursor.isNull(2) ? 0L : cursor.getLong(2);
                        if (name != null && name.startsWith(prefix) && name.toLowerCase(Locale.ROOT).endsWith(".txt")) {
                            SnapshotItem si = new SnapshotItem();
                            si.uri = DocumentsContract.buildDocumentUriUsingTree(folderTreeUri, docId);
                            si.fullFileName = name;
                            si.lastModified = modified;
                            si.displayDate = modified > 0 ? sdf.format(new Date(modified)) : stripTxtExtension(name.substring(prefix.length()));
                            list.add(si);
                        }
                    }
                }
            }
        } catch (Exception ignored) {}

        Collections.sort(list, (a, b) -> Long.compare(b.lastModified, a.lastModified));
        return list;
    }

    private void showSafeSnapshotsListDialog(FileItem item) {
        List<SnapshotItem> snapshots = getSafeSnapshotsForFile(item.name);
        if (snapshots.isEmpty()) {
            Toast.makeText(this, "لا توجد نسخ آمنة محفوظة لهذا الملف بعد", Toast.LENGTH_SHORT).show();
            return;
        }

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        card.setClipChildren(false);
        card.setClipToPadding(false);
        card.setPadding(dp(18), dp(18), dp(18), dp(16));
        card.setBackground(createRoundedRect(getCardBgColor(), dp(20), getBorderColor(), dp(1)));

        final Dialog dialog = createBaseDialog(card);

        TextView title = new TextView(this);
        title.setText("النسخ الآمنة: " + stripTxtExtension(item.name));
        title.setTextColor(getTextPrimaryColor());
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16.5f);
        applyAppFont(title, true);
        card.addView(title);

        TextView sub = new TextView(this);
        sub.setText("اضغط على أي نسخة لمعاينتها أو استعادتها:");
        sub.setTextColor(getTextSecondaryColor());
        sub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
        applyAppFont(sub, false);
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        subLp.topMargin = dp(4);
        subLp.bottomMargin = dp(10);
        card.addView(sub, subLp);

        ScrollView sv = new ScrollView(this);
        int maxH = (int) (getResources().getDisplayMetrics().heightPixels * 0.52f);
        LinearLayout listLayout = new LinearLayout(this);
        listLayout.setOrientation(LinearLayout.VERTICAL);

        for (int i = 0; i < snapshots.size(); i++) {
            final SnapshotItem snap = snapshots.get(i);
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.VERTICAL);
            row.setPadding(dp(14), dp(12), dp(14), dp(12));
            row.setBackground(createRoundedRect(getInputBgColor(), dp(12), getBorderColor(), dp(1)));

            TextView t1 = new TextView(this);
            t1.setText("نسخة آمنة #" + (snapshots.size() - i));
            t1.setTextColor(getTextPrimaryColor());
            t1.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14.5f);
            applyAppFont(t1, true);
            row.addView(t1);

            TextView t2 = new TextView(this);
            t2.setText(snap.displayDate);
            t2.setTextColor(getTextSecondaryColor());
            t2.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
            applyAppFont(t2, false);
            row.addView(t2);

            row.setOnClickListener(v -> {
                dialog.dismiss();
                showSafeSnapshotPreviewDialog(item, snap);
            });

            LinearLayout.LayoutParams rLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            rLp.bottomMargin = dp(8);
            listLayout.addView(row, rLp);
        }

        sv.addView(listLayout);
        card.addView(sv, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, snapshots.size() > 4 ? maxH : ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout closeWrap = new LinearLayout(this);
        closeWrap.setOrientation(LinearLayout.VERTICAL);
        closeWrap.setClipChildren(false);
        closeWrap.setClipToPadding(false);
        closeWrap.setPadding(dp(2), dp(4), dp(2), dp(6));

        TextView closeBtn = createButton("إغلاق", getSecondaryBtnBgColor(), getTextPrimaryColor(), false, v -> dialog.dismiss());
        LinearLayout.LayoutParams cLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cLp.topMargin = dp(10);
        cLp.bottomMargin = dp(4);
        closeWrap.addView(closeBtn, cLp);
        card.addView(closeWrap);

        dialog.show();
    }

    private void showSafeSnapshotPreviewDialog(FileItem item, SnapshotItem snap) {
        final String snapContent = readTextFromUri(snap.uri);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        card.setClipChildren(false);
        card.setClipToPadding(false);
        card.setPadding(dp(18), dp(18), dp(18), dp(16));
        card.setBackground(createRoundedRect(getCardBgColor(), dp(20), getBorderColor(), dp(1)));

        final Dialog dialog = createBaseDialog(card);

        TextView title = new TextView(this);
        title.setText("معاينة النسخة الآمنة");
        title.setTextColor(getTextPrimaryColor());
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16.5f);
        applyAppFont(title, true);
        card.addView(title);

        TextView sub = new TextView(this);
        sub.setText(snap.displayDate);
        sub.setTextColor(getTextSecondaryColor());
        sub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
        applyAppFont(sub, false);
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        subLp.topMargin = dp(3);
        subLp.bottomMargin = dp(10);
        card.addView(sub, subLp);

        ScrollView previewScroll = new ScrollView(this);
        previewScroll.setBackground(createRoundedRect(getInputBgColor(), dp(12), getBorderColor(), dp(1)));
        previewScroll.setPadding(dp(12), dp(12), dp(12), dp(12));
        int maxH = (int) (getResources().getDisplayMetrics().heightPixels * 0.45f);

        TextView bodyTv = new TextView(this);
        bodyTv.setText(snapContent.isEmpty() ? "(نسخة فارغة)" : snapContent);
        bodyTv.setTextColor(getTextPrimaryColor());
        bodyTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14.5f);
        bodyTv.setLineSpacing(0f, 1.25f);
        applyAppFont(bodyTv, false);
        previewScroll.addView(bodyTv);

        card.addView(previewScroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, maxH));

        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setGravity(Gravity.CENTER_VERTICAL);
        btnRow.setClipChildren(false);
        btnRow.setClipToPadding(false);
        btnRow.setPadding(dp(2), dp(4), dp(2), dp(6));
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rowLp.topMargin = dp(12);

        TextView restoreBtn = createButton("استعادة هذه النسخة", getPrimaryColor(), Color.WHITE, true, v -> {
            dialog.dismiss();
            String currentFileText = readTextFromUri(item.uri);
            List<SnapshotItem> existingSnaps = getSafeSnapshotsForFile(item.name);
            String latestSnapText = existingSnaps.isEmpty() ? "" : readTextFromUri(existingSnaps.get(0).uri);

            if (!currentFileText.trim().isEmpty()
                    && !currentFileText.equals(snapContent)
                    && !currentFileText.equals(latestSnapText)) {
                createSafeSnapshotFromContent(item.name, currentFileText, true);
            }

            writeTextToUri(item.uri, snapContent);
            contentCache.put(item.uri.toString(), snapContent);
            loadFilesFromFolder();
            renderHomeFilesList();
            Toast.makeText(this, "تمت استعادة النسخة الآمنة بنجاح", Toast.LENGTH_SHORT).show();
        });
        LinearLayout.LayoutParams resLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.3f);
        resLp.leftMargin = dp(8);
        resLp.bottomMargin = dp(4);
        btnRow.addView(restoreBtn, resLp);

        TextView backBtn = createButton("رجوع", getSecondaryBtnBgColor(), getTextPrimaryColor(), false, v -> {
            dialog.dismiss();
            showSafeSnapshotsListDialog(item);
        });
        LinearLayout.LayoutParams bLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.8f);
        bLp.bottomMargin = dp(4);
        btnRow.addView(backBtn, bLp);

        card.addView(btnRow, rowLp);
        dialog.show();
    }

    // ==================== 2. DOCUMENT SCREEN (READ & EDIT) ====================

    private void openDocument(Uri uri, String name, boolean startInEditMode) {
        currentScreen = Screen.DOCUMENT;
        currentFileUri = uri;
        currentFileName = name;
        currentFileContent = readTextFromUri(uri);
        lastDiskSavedContent = currentFileContent;
        isEditMode = startInEditMode;

        undoStack.clear();
        undoStack.add(currentFileContent);
        undoIndex = 0;

        renderDocumentScreen();
    }

    private void renderDocumentScreen() {
        float prevScrollFraction = 0f;
        if (readScrollView != null && readScrollView.getChildCount() > 0) {
            int range = readScrollView.getChildAt(0).getHeight() - readScrollView.getHeight();
            if (range > 0) {
                prevScrollFraction = Math.max(0f, Math.min(1f, (float) readScrollView.getScrollY() / range));
            }
        }
        final float targetScrollFraction = prevScrollFraction;

        rootContainer.removeAllViews();
        rootContainer.setBackgroundColor(getBgMainColor());

        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        mainLayout.setBackgroundColor(getBgMainColor());
        rootContainer.addView(mainLayout, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        topBar.setBackgroundColor(getHeaderBgColor());
        topBar.setPadding(dp(10), dp(10), dp(10), dp(10));

        TextView backBtn = createTopBarChip("→ رجوع", 0x22FFFFFF, Color.WHITE, v -> {
            if (isEditMode) {
                saveCurrentDocumentNow();
                isEditMode = false;
                hideKeyboard();
                renderDocumentScreen();
            } else {
                showHomeScreen();
            }
        });
        topBar.addView(backBtn);

        docTitleView = new TextView(this);
        docTitleView.setText(stripTxtExtension(currentFileName));
        docTitleView.setTextColor(Color.WHITE);
        docTitleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15.5f);
        docTitleView.setSingleLine(true);
        docTitleView.setGravity(Gravity.CENTER);
        docTitleView.setPadding(dp(8), dp(4), dp(8), dp(4));
        applyAppFont(docTitleView, true);
        docTitleView.setOnClickListener(v -> showRenameDialog(currentFileUri, currentFileName, true));
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        topBar.addView(docTitleView, titleLp);

        LinearLayout zoomBox = new LinearLayout(this);
        zoomBox.setOrientation(LinearLayout.HORIZONTAL);
        zoomBox.setGravity(Gravity.CENTER_VERTICAL);
        zoomBox.setBackground(createRoundedRect(0x22FFFFFF, dp(10), 0x33FFFFFF, dp(1)));

        TextView zoomInBtn = createZoomButton("＋", v -> changeZoom(5));
        zoomValueText = new TextView(this);
        zoomValueText.setText(getZoomPercent() + "%");
        zoomValueText.setTextColor(getAccentColor());
        zoomValueText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
        zoomValueText.setPadding(dp(6), dp(6), dp(6), dp(6));
        applyAppFont(zoomValueText, true);
        zoomValueText.setOnClickListener(v -> showResetZoomDialog());

        TextView zoomOutBtn = createZoomButton("－", v -> changeZoom(-5));

        zoomBox.addView(zoomInBtn);
        zoomBox.addView(zoomValueText);
        zoomBox.addView(zoomOutBtn);
        topBar.addView(zoomBox);

        mainLayout.addView(topBar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        boolean showEditorToolbar = prefs.getBoolean(KEY_SHOW_EDITOR_TOOLBAR, true);
        if (isEditMode && showEditorToolbar) {
            List<String> orderedIds = getOrderedToolIds();
            Set<String> enabledIds = getEnabledToolIds();
            boolean iconsOnly = prefs.getBoolean(KEY_TOOLBAR_ICONS_ONLY, false);

            HorizontalScrollView toolbarScroll = new HorizontalScrollView(this);
            toolbarScroll.setHorizontalScrollBarEnabled(false);
            toolbarScroll.setBackgroundColor(isDarkMode() ? 0xFF1E293B : 0xFFE2E8F0);
            toolbarScroll.setPadding(dp(8), dp(6), dp(8), dp(6));

            LinearLayout toolsRow = new LinearLayout(this);
            toolsRow.setOrientation(LinearLayout.HORIZONTAL);
            toolsRow.setGravity(Gravity.CENTER_VERTICAL);

            for (String toolId : orderedIds) {
                if (!enabledIds.contains(toolId)) continue;
                View chip = createEditorToolButton(toolId, iconsOnly);
                toolsRow.addView(chip);
            }

            toolbarScroll.addView(toolsRow);
            mainLayout.addView(toolbarScroll, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }

        FrameLayout paperWrapper = new FrameLayout(this);
        paperWrapper.setPadding(dp(12), dp(10), dp(12), dp(8));
        LinearLayout.LayoutParams paperWrapLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        mainLayout.addView(paperWrapper, paperWrapLp);

        readScrollView = new ScrollView(this);
        readScrollView.setFillViewport(true);
        readScrollView.setVerticalScrollBarEnabled(false);
        readScrollView.setBackground(createRoundedRect(getCardBgColor(), dp(18), getBorderColor(), dp(1)));
        paperWrapper.addView(readScrollView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        float scaledFontSize = getScaledFontSizeSp();

        if (isEditMode) {
            editorEditText = new EditText(this);
            editorEditText.setText(currentFileContent);
            editorEditText.setTextColor(getTextPrimaryColor());
            editorEditText.setTextSize(TypedValue.COMPLEX_UNIT_SP, scaledFontSize);
            editorEditText.setLineSpacing(0f, 1.22f);
            editorEditText.setGravity(Gravity.TOP | Gravity.RIGHT);
            editorEditText.setBackgroundColor(Color.TRANSPARENT);
            editorEditText.setPadding(dp(24), dp(18), dp(18), dp(80));
            editorEditText.setInputType(InputType.TYPE_CLASS_TEXT
                    | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                    | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                    | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
            applyAppFont(editorEditText, false);

            editorEditText.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    String textNow = s.toString();
                    currentFileContent = textNow;
                    updateStatsBar(textNow);

                    if (!isUndoRedoAction) {
                        while (undoStack.size() > undoIndex + 1) {
                            undoStack.remove(undoStack.size() - 1);
                        }
                        undoStack.add(textNow);
                        if (undoStack.size() > 60) {
                            undoStack.remove(0);
                        } else {
                            undoIndex++;
                        }
                    }

                    saveHandler.removeCallbacks(autoSaveRunnable);
                    saveHandler.postDelayed(autoSaveRunnable, 600);
                }
                @Override public void afterTextChanged(Editable s) {}
            });

            readScrollView.addView(editorEditText, new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        } else {
            readTextView = new TextView(this);
            readTextView.setText(currentFileContent.isEmpty() ? "هذا الملف فارغ حالياً.\nاضغط على «تحرير الملف» بالأسفل للبدء بالكتابة." : currentFileContent);
            readTextView.setTextColor(currentFileContent.isEmpty() ? getTextSecondaryColor() : getTextPrimaryColor());
            readTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, scaledFontSize);
            readTextView.setLineSpacing(0f, 1.22f);
            readTextView.setGravity(Gravity.TOP | Gravity.RIGHT);
            readTextView.setPadding(dp(24), dp(18), dp(18), dp(80));
            applyAppFont(readTextView, false);

            readScrollView.addView(readTextView, new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }

        if (prefs.getBoolean(KEY_SHOW_FAST_SLIDER, true)) {
            setupFastScrollSlider(paperWrapper);
        }

        if (prefs.getBoolean(KEY_SHOW_JUMP_BTN, true)) {
            setupQuickJumpButton(paperWrapper);
        }

        readScrollView.getViewTreeObserver().addOnScrollChangedListener(() -> {
            if (!isDraggingSlider) {
                updateSliderThumbFromScroll();
            }
            if (jumpMenuCard != null && jumpMenuCard.getVisibility() == View.VISIBLE) {
                jumpMenuCard.setVisibility(View.GONE);
            }
        });

        if (targetScrollFraction > 0f) {
            readScrollView.post(() -> {
                if (readScrollView.getChildCount() > 0) {
                    int range = readScrollView.getChildAt(0).getHeight() - readScrollView.getHeight();
                    if (range > 0) {
                        readScrollView.scrollTo(0, (int) (targetScrollFraction * range));
                    }
                }
            });
        }

        if (prefs.getBoolean(KEY_SHOW_STATS_BAR, true)) {
            statsBarText = new TextView(this);
            statsBarText.setTextColor(getTextSecondaryColor());
            statsBarText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
            statsBarText.setGravity(Gravity.CENTER);
            statsBarText.setPadding(dp(12), dp(7), dp(12), dp(7));
            statsBarText.setBackgroundColor(isDarkMode() ? 0xFF1E293B : 0xFFEFF3F8);
            applyAppFont(statsBarText, false);
            updateStatsBar(currentFileContent);
            mainLayout.addView(statsBarText, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        } else {
            statsBarText = null;
        }

        if (!isEditMode) {
            LinearLayout readBottomBar = new LinearLayout(this);
            readBottomBar.setOrientation(LinearLayout.HORIZONTAL);
            readBottomBar.setGravity(Gravity.CENTER_VERTICAL);
            readBottomBar.setClipToPadding(false);
            readBottomBar.setClipChildren(false);
            readBottomBar.setPadding(dp(16), dp(10), dp(16), dp(16));
            readBottomBar.setBackgroundColor(getCardBgColor());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                readBottomBar.setElevation(dp(8));
            }

            TextView editBtn = createButton("تحرير الملف", getPrimaryColor(), Color.WHITE, true, v -> {
                isEditMode = true;
                renderDocumentScreen();
                if (editorEditText != null) {
                    editorEditText.requestFocus();
                    showKeyboard(editorEditText);
                }
            });
            LinearLayout.LayoutParams editLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.3f);
            editLp.leftMargin = dp(10);
            editLp.bottomMargin = dp(4);
            readBottomBar.addView(editBtn, editLp);

            TextView shareBtn = createButton("مشاركة الملف", getSecondaryBtnBgColor(), getTextPrimaryColor(), false,
                    v -> shareTextContent(stripTxtExtension(currentFileName), currentFileContent));
            LinearLayout.LayoutParams shareLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            shareLp.bottomMargin = dp(4);
            readBottomBar.addView(shareBtn, shareLp);

            mainLayout.addView(readBottomBar, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }
    }

    private void stopOngoingScrollFling() {
        if (readScrollView == null) return;
        try {
            readScrollView.fling(0);
            readScrollView.smoothScrollBy(0, 0);
            long now = SystemClock.uptimeMillis();
            MotionEvent down = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, 0f, 0f, 0);
            readScrollView.onTouchEvent(down);
            down.recycle();
            MotionEvent cancel = MotionEvent.obtain(now, now, MotionEvent.ACTION_CANCEL, 0f, 0f, 0);
            readScrollView.onTouchEvent(cancel);
            cancel.recycle();
            if (editorEditText != null) {
                editorEditText.cancelPendingInputEvents();
            }
        } catch (Exception ignored) {}
    }

    private void setupFastScrollSlider(FrameLayout paperWrapper) {
        sliderTouchContainer = new FrameLayout(this);
        FrameLayout.LayoutParams containerLp = new FrameLayout.LayoutParams(
                dp(32), ViewGroup.LayoutParams.MATCH_PARENT, Gravity.LEFT | Gravity.TOP);
        containerLp.topMargin = dp(10);
        containerLp.bottomMargin = dp(58);
        containerLp.leftMargin = dp(2);

        sliderThumb = new View(this);
        sliderThumb.setBackground(createRoundedRect(getPrimaryColor(), dp(6), getAccentColor(), dp(1)));
        sliderThumb.setAlpha(0.38f);
        FrameLayout.LayoutParams thumbLp = new FrameLayout.LayoutParams(dp(8), dp(46), Gravity.LEFT | Gravity.TOP);
        thumbLp.leftMargin = dp(4);
        sliderTouchContainer.addView(sliderThumb, thumbLp);

        sliderBubble = new TextView(this);
        sliderBubble.setText("0%");
        sliderBubble.setTextColor(Color.WHITE);
        sliderBubble.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        sliderBubble.setPadding(dp(10), dp(5), dp(10), dp(5));
        sliderBubble.setBackground(createRoundedRect(getHeaderBgColor(), dp(12), getAccentColor(), dp(1)));
        sliderBubble.setVisibility(View.GONE);
        applyAppFont(sliderBubble, true);
        FrameLayout.LayoutParams bubbleLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.LEFT | Gravity.TOP);
        bubbleLp.leftMargin = dp(38);
        paperWrapper.addView(sliderBubble, bubbleLp);

        sliderTouchContainer.setOnTouchListener((v, event) -> {
            if (readScrollView == null || readScrollView.getChildCount() == 0) return false;
            int contentHeight = readScrollView.getChildAt(0).getHeight();
            int viewportHeight = readScrollView.getHeight();
            int scrollableRange = contentHeight - viewportHeight;
            if (scrollableRange <= 0) return false;

            int trackHeight = sliderTouchContainer.getHeight() - sliderThumb.getHeight();
            if (trackHeight <= 0) return false;

            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN) {
                stopOngoingScrollFling();
                if (v.getParent() != null) {
                    v.getParent().requestDisallowInterceptTouchEvent(true);
                }
            }

            if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE) {
                isDraggingSlider = true;
                saveHandler.removeCallbacks(fadeSliderRunnable);
                sliderThumb.setAlpha(0.95f);
                sliderBubble.setVisibility(View.VISIBLE);

                float y = event.getY() - (sliderThumb.getHeight() / 2f);
                float clampedY = Math.max(0f, Math.min(trackHeight, y));
                float fraction = clampedY / trackHeight;

                sliderThumb.setTranslationY(clampedY);
                sliderBubble.setTranslationY(clampedY + dp(8));
                int percent = Math.round(fraction * 100f);
                sliderBubble.setText(percent + "%");

                int targetScrollY = (int) (fraction * scrollableRange);
                readScrollView.scrollTo(0, targetScrollY);
                return true;
            } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                isDraggingSlider = false;
                sliderBubble.setVisibility(View.GONE);
                saveHandler.postDelayed(fadeSliderRunnable, 1200);
                return true;
            }
            return false;
        });

        paperWrapper.addView(sliderTouchContainer, containerLp);
    }

    private void updateSliderThumbFromScroll() {
        if (sliderTouchContainer == null || sliderThumb == null || readScrollView == null || readScrollView.getChildCount() == 0) {
            return;
        }
        int contentHeight = readScrollView.getChildAt(0).getHeight();
        int viewportHeight = readScrollView.getHeight();
        int scrollableRange = contentHeight - viewportHeight;
        if (scrollableRange <= 0) {
            sliderThumb.setVisibility(View.INVISIBLE);
            return;
        }
        sliderThumb.setVisibility(View.VISIBLE);
        int trackHeight = sliderTouchContainer.getHeight() - sliderThumb.getHeight();
        if (trackHeight <= 0) return;

        float fraction = Math.max(0f, Math.min(1f, (float) readScrollView.getScrollY() / scrollableRange));
        sliderThumb.setTranslationY(fraction * trackHeight);
        sliderThumb.setAlpha(0.75f);
        saveHandler.removeCallbacks(fadeSliderRunnable);
        saveHandler.postDelayed(fadeSliderRunnable, 1200);
    }

    private void setupQuickJumpButton(FrameLayout paperWrapper) {
        jumpMenuCard = new LinearLayout(this);
        jumpMenuCard.setOrientation(LinearLayout.VERTICAL);
        jumpMenuCard.setBackground(createRoundedRect(getHeaderBgColor(), dp(12), getAccentColor(), dp(1)));
        jumpMenuCard.setPadding(dp(6), dp(6), dp(6), dp(6));
        jumpMenuCard.setVisibility(View.GONE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            jumpMenuCard.setElevation(dp(6));
        }

        TextView jumpTopBtn = new TextView(this);
        jumpTopBtn.setText("↑  أول المستند");
        jumpTopBtn.setTextColor(Color.WHITE);
        jumpTopBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
        jumpTopBtn.setPadding(dp(12), dp(7), dp(12), dp(7));
        applyAppFont(jumpTopBtn, true);
        jumpTopBtn.setOnClickListener(v -> {
            jumpMenuCard.setVisibility(View.GONE);
            if (readScrollView != null) {
                stopOngoingScrollFling();
                readScrollView.post(() -> readScrollView.fullScroll(View.FOCUS_UP));
                if (isEditMode && editorEditText != null) {
                    editorEditText.setSelection(0);
                }
            }
        });
        jumpMenuCard.addView(jumpTopBtn);

        View div = new View(this);
        div.setBackgroundColor(0x33FFFFFF);
        jumpMenuCard.addView(div, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(1)));

        TextView jumpBottomBtn = new TextView(this);
        jumpBottomBtn.setText("↓  آخر المستند");
        jumpBottomBtn.setTextColor(Color.WHITE);
        jumpBottomBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
        jumpBottomBtn.setPadding(dp(12), dp(7), dp(12), dp(7));
        applyAppFont(jumpBottomBtn, true);
        jumpBottomBtn.setOnClickListener(v -> {
            jumpMenuCard.setVisibility(View.GONE);
            if (readScrollView != null) {
                stopOngoingScrollFling();
                readScrollView.post(() -> readScrollView.fullScroll(View.FOCUS_DOWN));
                if (isEditMode && editorEditText != null) {
                    editorEditText.setSelection(editorEditText.getText().length());
                }
            }
        });
        jumpMenuCard.addView(jumpBottomBtn);

        FrameLayout.LayoutParams menuLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.LEFT | Gravity.BOTTOM);
        menuLp.leftMargin = dp(10);
        menuLp.bottomMargin = dp(54);
        paperWrapper.addView(jumpMenuCard, menuLp);

        FrameLayout jumpBtn = new FrameLayout(this);
        jumpBtn.setBackground(createRoundedRect(getPrimaryColor(), dp(19), getAccentColor(), dp(1)));
        jumpBtn.setAlpha(0.86f);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            jumpBtn.setElevation(dp(4));
        }
        ImageView icon = createVectorIcon("ic_jump_vertical", Color.WHITE, 20);
        jumpBtn.addView(icon, new FrameLayout.LayoutParams(dp(20), dp(20), Gravity.CENTER));
        jumpBtn.setOnClickListener(v -> {
            if (jumpMenuCard.getVisibility() == View.VISIBLE) {
                jumpMenuCard.setVisibility(View.GONE);
            } else {
                jumpMenuCard.setVisibility(View.VISIBLE);
            }
        });

        FrameLayout.LayoutParams btnLp = new FrameLayout.LayoutParams(dp(38), dp(38), Gravity.LEFT | Gravity.BOTTOM);
        btnLp.leftMargin = dp(10);
        btnLp.bottomMargin = dp(10);
        paperWrapper.addView(jumpBtn, btnLp);
    }

    private View createEditorToolButton(String toolId, boolean iconsOnly) {
        String label = getToolLabel(toolId);
        String iconName = getToolIconRes(toolId);

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.HORIZONTAL);
        box.setGravity(Gravity.CENTER);
        box.setBackground(createRipple(getCardBgColor(), dp(9)));
        box.setOnClickListener(v -> executeEditorTool(toolId));

        if (iconsOnly) {
            box.setPadding(dp(11), dp(7), dp(11), dp(7));
            ImageView iv = createVectorIcon(iconName, getTextPrimaryColor(), 20);
            box.addView(iv);
        } else {
            box.setPadding(dp(11), dp(6), dp(11), dp(6));
            TextView tv = new TextView(this);
            tv.setText(label);
            tv.setTextColor(getTextPrimaryColor());
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
            applyAppFont(tv, true);
            box.addView(tv);
        }

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.leftMargin = dp(6);
        box.setLayoutParams(lp);
        return box;
    }

    private void executeEditorTool(String toolId) {
        switch (toolId) {
            case "undo":
                performUndo();
                break;
            case "redo":
                performRedo();
                break;
            case "copy":
                performCopy();
                break;
            case "paste":
                performPaste();
                break;
            case "select_all":
                if (editorEditText != null) {
                    editorEditText.requestFocus();
                    editorEditText.selectAll();
                }
                break;
            case "auto_format":
                performSmartFormat();
                break;
            case "signature":
                insertSignatureAtBottom();
                break;
            case "cut":
                performCut();
                break;
            case "select_up":
                performSelectUp();
                break;
            case "select_down":
                performSelectDown();
                break;
            case "datetime":
                insertDateTimeAtCursor();
                break;
            case "find_replace":
                showFindReplaceDialog();
                break;
        }
    }

    // ==================== FIND & REPLACE DIALOG ====================

    private void showFindReplaceDialog() {
        if (editorEditText == null) return;

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        card.setClipChildren(false);
        card.setClipToPadding(false);
        card.setPadding(dp(18), dp(18), dp(18), dp(16));
        card.setBackground(createRoundedRect(getCardBgColor(), dp(20), getBorderColor(), dp(1)));

        final Dialog dialog = createBaseDialog(card);

        TextView title = new TextView(this);
        title.setText("بحث واستبدال داخل المستند");
        title.setTextColor(getTextPrimaryColor());
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16.5f);
        applyAppFont(title, true);
        card.addView(title);

        final EditText findEt = new EditText(this);
        findEt.setHint("النص المراد البحث عنه...");
        findEt.setHintTextColor(getTextSecondaryColor());
        findEt.setTextColor(getTextPrimaryColor());
        findEt.setSingleLine(true);
        findEt.setPadding(dp(12), dp(10), dp(12), dp(10));
        findEt.setBackground(createRoundedRect(getInputBgColor(), dp(10), getBorderColor(), dp(1)));
        applyAppFont(findEt, false);
        LinearLayout.LayoutParams fLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        fLp.topMargin = dp(12);
        card.addView(findEt, fLp);

        final EditText replaceEt = new EditText(this);
        replaceEt.setHint("النص البديل (اختياري)...");
        replaceEt.setHintTextColor(getTextSecondaryColor());
        replaceEt.setTextColor(getTextPrimaryColor());
        replaceEt.setSingleLine(true);
        replaceEt.setPadding(dp(12), dp(10), dp(12), dp(10));
        replaceEt.setBackground(createRoundedRect(getInputBgColor(), dp(10), getBorderColor(), dp(1)));
        applyAppFont(replaceEt, false);
        LinearLayout.LayoutParams rLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rLp.topMargin = dp(8);
        card.addView(replaceEt, rLp);

        final TextView resultInfo = new TextView(this);
        resultInfo.setTextColor(getTextSecondaryColor());
        resultInfo.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
        applyAppFont(resultInfo, false);
        LinearLayout.LayoutParams iLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        iLp.topMargin = dp(6);
        card.addView(resultInfo, iLp);

        LinearLayout row1 = new LinearLayout(this);
        row1.setOrientation(LinearLayout.HORIZONTAL);
        row1.setClipChildren(false);
        row1.setClipToPadding(false);
        row1.setPadding(dp(2), dp(2), dp(2), dp(4));
        LinearLayout.LayoutParams row1Lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        row1Lp.topMargin = dp(10);

        TextView findNextBtn = createButton("بحث عن التالي", getPrimaryColor(), Color.WHITE, true, v -> {
            String q = findEt.getText().toString();
            if (q.isEmpty()) return;
            String text = editorEditText.getText().toString();
            List<int[]> matches = findSmartMatchRanges(text, q);
            if (matches.isEmpty()) {
                resultInfo.setText("لا توجد نتائج مطابقة.");
                return;
            }
            int cursor = editorEditText.getSelectionEnd();
            int[] chosen = matches.get(0);
            int idxNum = 1;
            for (int i = 0; i < matches.size(); i++) {
                if (matches.get(i)[0] >= cursor) {
                    chosen = matches.get(i);
                    idxNum = i + 1;
                    break;
                }
            }
            editorEditText.requestFocus();
            editorEditText.setSelection(chosen[0], chosen[1]);
            scrollToEditorSelection(chosen[0]);
            resultInfo.setText("النتيجة " + idxNum + " من " + matches.size());
        });
        LinearLayout.LayoutParams fnLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        fnLp.leftMargin = dp(6);
        fnLp.bottomMargin = dp(4);
        row1.addView(findNextBtn, fnLp);

        TextView replaceOneBtn = createButton("استبدال الحالي", getSecondaryBtnBgColor(), getTextPrimaryColor(), true, v -> {
            String q = findEt.getText().toString();
            String rep = replaceEt.getText().toString();
            if (q.isEmpty()) return;
            int selStart = Math.min(editorEditText.getSelectionStart(), editorEditText.getSelectionEnd());
            int selEnd = Math.max(editorEditText.getSelectionStart(), editorEditText.getSelectionEnd());
            if (selStart >= 0 && selEnd > selStart) {
                editorEditText.getText().replace(selStart, selEnd, rep);
                resultInfo.setText("تم استبدال الموضع المحدد.");
            } else {
                resultInfo.setText("اضغط «بحث عن التالي» أولاً لتحديد الكلمة.");
            }
        });
        LinearLayout.LayoutParams roLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        roLp.bottomMargin = dp(4);
        row1.addView(replaceOneBtn, roLp);
        card.addView(row1, row1Lp);

        LinearLayout row2 = new LinearLayout(this);
        row2.setOrientation(LinearLayout.HORIZONTAL);
        row2.setClipChildren(false);
        row2.setClipToPadding(false);
        row2.setPadding(dp(2), dp(2), dp(2), dp(6));
        LinearLayout.LayoutParams row2Lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        row2Lp.topMargin = dp(6);

        TextView replaceAllBtn = createButton("استبدال الكل", getSecondaryBtnBgColor(), getTextPrimaryColor(), true, v -> {
            String q = findEt.getText().toString();
            String rep = replaceEt.getText().toString();
            if (q.isEmpty()) return;
            String text = editorEditText.getText().toString();
            List<int[]> matches = findSmartMatchRanges(text, q);
            if (matches.isEmpty()) {
                resultInfo.setText("لا توجد نتائج لاستبدالها.");
                return;
            }
            StringBuilder sb = new StringBuilder();
            int lastEnd = 0;
            for (int[] m : matches) {
                sb.append(text, lastEnd, m[0]);
                sb.append(rep);
                lastEnd = m[1];
            }
            sb.append(text.substring(lastEnd));
            editorEditText.setText(sb.toString());
            resultInfo.setText("تم استبدال " + matches.size() + " موضع بنجاح.");
        });
        LinearLayout.LayoutParams raLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        raLp.leftMargin = dp(6);
        raLp.bottomMargin = dp(4);
        row2.addView(replaceAllBtn, raLp);

        TextView closeBtn = createButton("إغلاق", getSecondaryBtnBgColor(), getTextPrimaryColor(), false, v -> dialog.dismiss());
        LinearLayout.LayoutParams clLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        clLp.bottomMargin = dp(4);
        row2.addView(closeBtn, clLp);
        card.addView(row2, row2Lp);

        dialog.show();
    }

    private void scrollToEditorSelection(int offset) {
        if (editorEditText == null || readScrollView == null) return;
        editorEditText.post(() -> {
            Layout layout = editorEditText.getLayout();
            if (layout != null) {
                int line = layout.getLineForOffset(Math.min(offset, editorEditText.getText().length()));
                int y = layout.getLineTop(line);
                readScrollView.smoothScrollTo(0, Math.max(0, y - dp(80)));
            }
        });
    }

    // ==================== 3. SETTINGS SCREEN ====================

    private void showSettingsScreen() {
        currentScreen = Screen.SETTINGS;
        hideKeyboard();
        updateStatusBarColor();
        rootContainer.removeAllViews();
        rootContainer.setBackgroundColor(getBgMainColor());

        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        mainLayout.setBackgroundColor(getBgMainColor());
        rootContainer.addView(mainLayout, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        topBar.setBackgroundColor(getHeaderBgColor());
        topBar.setPadding(dp(12), dp(12), dp(12), dp(12));

        TextView backBtn = createTopBarChip("→ رجوع", 0x22FFFFFF, Color.WHITE, v -> showHomeScreen());
        topBar.addView(backBtn);

        TextView title = new TextView(this);
        title.setText("إعدادات المفكرة");
        title.setTextColor(Color.WHITE);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        applyAppFont(title, true);
        LinearLayout.LayoutParams tLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tLp.rightMargin = dp(12);
        topBar.addView(title, tLp);

        mainLayout.addView(topBar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        ScrollView scroll = new ScrollView(this);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setClipChildren(false);
        content.setClipToPadding(false);
        content.setPadding(dp(16), dp(16), dp(16), dp(28));
        scroll.addView(content);
        mainLayout.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        // --- SECTION 1: APPEARANCE & FONTS ---
        LinearLayout appearanceCard = createCard();
        appearanceCard.setClipChildren(false);
        appearanceCard.setClipToPadding(false);
        addSectionTitle(appearanceCard, "المظهر والخطوط");

        View darkModeRow = createSettingsSwitchRow(
                "الوضع الليلي المريح للعين",
                prefs.getBoolean(KEY_DARK_MODE, false),
                val -> {
                    prefs.edit().putBoolean(KEY_DARK_MODE, val).apply();
                    showSettingsScreen();
                });
        appearanceCard.addView(darkModeRow);

        addDivider(appearanceCard);

        TextView themeLabel = new TextView(this);
        themeLabel.setText("لون المظهر (الثيم):");
        themeLabel.setTextColor(getTextSecondaryColor());
        themeLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
        applyAppFont(themeLabel, false);
        appearanceCard.addView(themeLabel);

        int currentTheme = getThemeIndex();
        LinearLayout themeSelectorBox = createDropdownSelectorBox(
                THEME_NAMES[currentTheme],
                THEME_PRIMARY[currentTheme],
                v -> showThemeDropdownDialog());
        LinearLayout.LayoutParams tsLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tsLp.topMargin = dp(6);
        tsLp.bottomMargin = dp(12);
        appearanceCard.addView(themeSelectorBox, tsLp);

        TextView fontLabel = new TextView(this);
        fontLabel.setText("نوع الخط في التطبيق والمحرر:");
        fontLabel.setTextColor(getTextSecondaryColor());
        fontLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
        applyAppFont(fontLabel, false);
        appearanceCard.addView(fontLabel);

        int currentFont = getFontIndex();
        LinearLayout fontSelectorBox = createDropdownSelectorBox(
                FONT_NAMES[currentFont],
                0,
                v -> showFontDropdownDialog());
        LinearLayout.LayoutParams fsLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        fsLp.topMargin = dp(6);
        appearanceCard.addView(fontSelectorBox, fsLp);

        content.addView(appearanceCard);

        // --- SECTION 2: READING & EDITING OPTIONS ---
        LinearLayout displayCard = createCard();
        displayCard.setClipChildren(false);
        displayCard.setClipToPadding(false);
        LinearLayout.LayoutParams dcLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dcLp.topMargin = dp(14);
        addSectionTitle(displayCard, "خيارات شاشة القراءة والتحرير");

        displayCard.addView(createSettingsSwitchRow(
                "إظهار شريط التمرير السريع الجانبي (مع نسبة %)",
                prefs.getBoolean(KEY_SHOW_FAST_SLIDER, true),
                val -> prefs.edit().putBoolean(KEY_SHOW_FAST_SLIDER, val).apply()));

        displayCard.addView(createSettingsSwitchRow(
                "إظهار زر القفز السريع (أول / آخر المستند)",
                prefs.getBoolean(KEY_SHOW_JUMP_BTN, true),
                val -> prefs.edit().putBoolean(KEY_SHOW_JUMP_BTN, val).apply()));

        displayCard.addView(createSettingsSwitchRow(
                "إظهار شريط إحصائيات المستند (الكلمات • الحروف • الأسطر)",
                prefs.getBoolean(KEY_SHOW_STATS_BAR, true),
                val -> prefs.edit().putBoolean(KEY_SHOW_STATS_BAR, val).apply()));

        displayCard.addView(createSettingsSwitchRow(
                "إظهار شريط أدوات التحرير أعلى الورقة",
                prefs.getBoolean(KEY_SHOW_EDITOR_TOOLBAR, true),
                val -> prefs.edit().putBoolean(KEY_SHOW_EDITOR_TOOLBAR, val).apply()));

        addDivider(displayCard);

        displayCard.addView(createToolbarDisplaySegmentedRow());

        TextView customizeToolsBtn = createButton(
                "تخصيص أدوات التحرير",
                getSecondaryBtnBgColor(),
                getTextPrimaryColor(),
                true,
                v -> showToolbarCustomizationDialog());
        LinearLayout.LayoutParams ctLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ctLp.topMargin = dp(10);
        ctLp.bottomMargin = dp(4);
        displayCard.addView(customizeToolsBtn, ctLp);

        content.addView(displayCard, dcLp);

        // --- SECTION 3: SIGNATURE & FOLDER ---
        LinearLayout sigFolderCard = createCard();
        sigFolderCard.setClipChildren(false);
        sigFolderCard.setClipToPadding(false);
        LinearLayout.LayoutParams sfLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        sfLp.topMargin = dp(14);
        addSectionTitle(sigFolderCard, "التوقيع وإدارة الملفات");

        TextView sigDesc = new TextView(this);
        sigDesc.setText("نص التوقيع الذي يُدرج في نهاية المقال عند الضغط على «التوقيع»:");
        sigDesc.setTextColor(getTextSecondaryColor());
        sigDesc.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
        applyAppFont(sigDesc, false);
        sigFolderCard.addView(sigDesc);

        EditText sigInput = new EditText(this);
        sigInput.setText(prefs.getString(KEY_SIGNATURE, DEFAULT_SIGNATURE));
        sigInput.setHint("اكتب التوقيع هنا...");
        sigInput.setHintTextColor(getTextSecondaryColor());
        sigInput.setTextColor(getTextPrimaryColor());
        sigInput.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14.5f);
        applyAppFont(sigInput, false);
        sigInput.setPadding(dp(14), dp(11), dp(14), dp(11));
        sigInput.setBackground(createRoundedRect(getInputBgColor(), dp(12), getBorderColor(), dp(1)));
        LinearLayout.LayoutParams sigLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        sigLp.topMargin = dp(8);
        sigFolderCard.addView(sigInput, sigLp);

        TextView saveSigBtn = createButton("حفظ التوقيع", getPrimaryColor(), Color.WHITE, true, v -> {
            String newSig = sigInput.getText().toString().trim();
            prefs.edit().putString(KEY_SIGNATURE, newSig).apply();
            hideKeyboard();
            Toast.makeText(this, "تم حفظ التوقيع بنجاح", Toast.LENGTH_SHORT).show();
        });
        LinearLayout.LayoutParams sBtnLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        sBtnLp.topMargin = dp(10);
        sBtnLp.bottomMargin = dp(4);
        sigFolderCard.addView(saveSigBtn, sBtnLp);

        addDivider(sigFolderCard);

        TextView folderBtn = createButton("تغيير مجلد الحفظ في الهاتف", getSecondaryBtnBgColor(), getTextPrimaryColor(), false, v -> pickFolder());
        LinearLayout.LayoutParams fBtnLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        fBtnLp.bottomMargin = dp(4);
        sigFolderCard.addView(folderBtn, fBtnLp);

        content.addView(sigFolderCard, sfLp);
    }

    private void addSectionTitle(LinearLayout card, String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(getTextPrimaryColor());
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        applyAppFont(tv, true);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(10);
        card.addView(tv, lp);
    }

    private void addDivider(LinearLayout card) {
        View v = new View(this);
        v.setBackgroundColor(getBorderColor());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(1));
        lp.topMargin = dp(10);
        lp.bottomMargin = dp(10);
        card.addView(v, lp);
    }

    private LinearLayout createToolbarDisplaySegmentedRow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(2), dp(6), dp(2), dp(6));

        TextView label = new TextView(this);
        label.setText("شكل عرض الأدوات:");
        label.setTextColor(getTextPrimaryColor());
        label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f);
        applyAppFont(label, false);
        LinearLayout.LayoutParams lLp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        row.addView(label, lLp);

        final boolean[] iconsOnly = {prefs.getBoolean(KEY_TOOLBAR_ICONS_ONLY, false)};

        LinearLayout segBox = new LinearLayout(this);
        segBox.setOrientation(LinearLayout.HORIZONTAL);
        segBox.setBackground(createRoundedRect(getInputBgColor(), dp(10), getBorderColor(), dp(1)));
        segBox.setPadding(dp(3), dp(3), dp(3), dp(3));

        final TextView textOpt = new TextView(this);
        textOpt.setText("نصوص");
        textOpt.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
        textOpt.setPadding(dp(14), dp(6), dp(14), dp(6));
        applyAppFont(textOpt, true);

        final TextView iconOpt = new TextView(this);
        iconOpt.setText("رموز");
        iconOpt.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
        iconOpt.setPadding(dp(14), dp(6), dp(14), dp(6));
        applyAppFont(iconOpt, true);

        Runnable updateSegStyle = () -> {
            if (!iconsOnly[0]) {
                textOpt.setBackground(createRoundedRect(getPrimaryColor(), dp(8), 0, 0));
                textOpt.setTextColor(Color.WHITE);
                iconOpt.setBackgroundColor(Color.TRANSPARENT);
                iconOpt.setTextColor(getTextSecondaryColor());
            } else {
                iconOpt.setBackground(createRoundedRect(getPrimaryColor(), dp(8), 0, 0));
                iconOpt.setTextColor(Color.WHITE);
                textOpt.setBackgroundColor(Color.TRANSPARENT);
                textOpt.setTextColor(getTextSecondaryColor());
            }
        };
        updateSegStyle.run();

        textOpt.setOnClickListener(v -> {
            iconsOnly[0] = false;
            prefs.edit().putBoolean(KEY_TOOLBAR_ICONS_ONLY, false).apply();
            updateSegStyle.run();
        });
        iconOpt.setOnClickListener(v -> {
            iconsOnly[0] = true;
            prefs.edit().putBoolean(KEY_TOOLBAR_ICONS_ONLY, true).apply();
            updateSegStyle.run();
        });

        segBox.addView(textOpt);
        segBox.addView(iconOpt);
        row.addView(segBox);
        return row;
    }

    private interface OnBoolChanged {
        void onChanged(boolean newValue);
    }

    private View createSettingsSwitchRow(String label, boolean initialValue, OnBoolChanged callback) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(4), dp(9), dp(4), dp(9));

        final boolean[] state = {initialValue};

        final FrameLayout switchTrack = new FrameLayout(this);
        final View switchThumb = new View(this);
        switchThumb.setBackground(createRoundedRect(Color.WHITE, dp(10), 0, 0));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            switchThumb.setElevation(dp(2));
        }
        FrameLayout.LayoutParams thumbLp = new FrameLayout.LayoutParams(dp(20), dp(20), Gravity.LEFT | Gravity.CENTER_VERTICAL);
        switchTrack.addView(switchThumb, thumbLp);

        Runnable renderSwitch = () -> {
            int trackColor = state[0] ? getPrimaryColor() : (isDarkMode() ? 0xFF475569 : 0xFFCBD5E1);
            switchTrack.setBackground(createRoundedRect(trackColor, dp(13), 0, 0));
            switchThumb.setTranslationX(state[0] ? dp(24) : dp(4));
        };
        renderSwitch.run();

        LinearLayout.LayoutParams swLp = new LinearLayout.LayoutParams(dp(48), dp(26));
        swLp.leftMargin = dp(12);
        row.addView(switchTrack, swLp);

        TextView tv = new TextView(this);
        tv.setText(label);
        tv.setTextColor(getTextPrimaryColor());
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f);
        applyAppFont(tv, false);
        LinearLayout.LayoutParams tvLp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        row.addView(tv, tvLp);

        row.setOnClickListener(v -> {
            state[0] = !state[0];
            int trackColor = state[0] ? getPrimaryColor() : (isDarkMode() ? 0xFF475569 : 0xFFCBD5E1);
            switchTrack.setBackground(createRoundedRect(trackColor, dp(13), 0, 0));
            switchThumb.animate().translationX(state[0] ? dp(24) : dp(4)).setDuration(140).start();
            callback.onChanged(state[0]);
        });

        return row;
    }

    private LinearLayout createDropdownSelectorBox(String text, int colorDot, View.OnClickListener onClick) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.HORIZONTAL);
        box.setGravity(Gravity.CENTER_VERTICAL);
        box.setPadding(dp(14), dp(12), dp(14), dp(12));
        box.setBackground(createRippleWithStroke(getInputBgColor(), dp(12), getBorderColor(), dp(1)));
        box.setOnClickListener(onClick);

        if (colorDot != 0) {
            View dot = new View(this);
            dot.setBackground(createRoundedRect(colorDot, dp(9), getAccentColor(), dp(1)));
            LinearLayout.LayoutParams dotLp = new LinearLayout.LayoutParams(dp(18), dp(18));
            dotLp.leftMargin = dp(10);
            box.addView(dot, dotLp);
        }

        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(getTextPrimaryColor());
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14.5f);
        applyAppFont(tv, true);
        LinearLayout.LayoutParams tvLp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        box.addView(tv, tvLp);

        TextView arrow = new TextView(this);
        arrow.setText("▼");
        arrow.setTextColor(getTextSecondaryColor());
        arrow.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
        box.addView(arrow);

        return box;
    }

    private void showThemeDropdownDialog() {
        LinearLayout dialogCard = new LinearLayout(this);
        dialogCard.setOrientation(LinearLayout.VERTICAL);
        dialogCard.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        dialogCard.setPadding(dp(18), dp(18), dp(18), dp(18));
        dialogCard.setBackground(createRoundedRect(getCardBgColor(), dp(20), getBorderColor(), dp(1)));

        final Dialog dialog = createBaseDialog(dialogCard);

        TextView title = new TextView(this);
        title.setText("اختر لون المظهر (الثيم)");
        title.setTextColor(getTextPrimaryColor());
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        applyAppFont(title, true);
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        titleLp.bottomMargin = dp(12);
        dialogCard.addView(title, titleLp);

        ScrollView sv = new ScrollView(this);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        sv.addView(list);
        dialogCard.addView(sv, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        int currentTheme = getThemeIndex();
        for (int i = 0; i < THEME_NAMES.length; i++) {
            final int idx = i;
            boolean selected = (i == currentTheme);

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(12), dp(11), dp(12), dp(11));
            row.setBackground(createRoundedRect(
                    selected ? (isDarkMode() ? 0xFF334155 : 0xFFEFF6FF) : getInputBgColor(),
                    dp(12),
                    selected ? THEME_PRIMARY[i] : getBorderColor(),
                    dp(selected ? 2 : 1)));

            View colorSwatch = new View(this);
            colorSwatch.setBackground(createRoundedRect(THEME_PRIMARY[i], dp(10), THEME_ACCENT[i], dp(1.5f)));
            LinearLayout.LayoutParams swLp = new LinearLayout.LayoutParams(dp(22), dp(22));
            swLp.leftMargin = dp(12);
            row.addView(colorSwatch, swLp);

            TextView nameTv = new TextView(this);
            nameTv.setText(THEME_NAMES[i]);
            nameTv.setTextColor(getTextPrimaryColor());
            nameTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14.5f);
            applyAppFont(nameTv, selected);
            LinearLayout.LayoutParams nLp = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            row.addView(nameTv, nLp);

            if (selected) {
                TextView check = new TextView(this);
                check.setText("✓");
                check.setTextColor( isDarkMode() ? THEME_ACCENT[i] : THEME_PRIMARY[i]);
                check.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
                applyAppFont(check, true);
                row.addView(check);
            }

            row.setOnClickListener(v -> {
                prefs.edit().putInt(KEY_THEME_INDEX, idx).apply();
                dialog.dismiss();
                showSettingsScreen();
            });

            LinearLayout.LayoutParams rLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            rLp.bottomMargin = dp(7);
            list.addView(row, rLp);
        }

        dialog.show();
    }

    private void showFontDropdownDialog() {
        LinearLayout dialogCard = new LinearLayout(this);
        dialogCard.setOrientation(LinearLayout.VERTICAL);
        dialogCard.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        dialogCard.setPadding(dp(18), dp(18), dp(18), dp(18));
        dialogCard.setBackground(createRoundedRect(getCardBgColor(), dp(20), getBorderColor(), dp(1)));

        final Dialog dialog = createBaseDialog(dialogCard);

        TextView title = new TextView(this);
        title.setText("اختر نوع الخط");
        title.setTextColor(getTextPrimaryColor());
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        applyAppFont(title, true);
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        titleLp.bottomMargin = dp(12);
        dialogCard.addView(title, titleLp);

        ScrollView sv = new ScrollView(this);
        int maxH = (int) (getResources().getDisplayMetrics().heightPixels * 0.62f);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        sv.addView(list);
        dialogCard.addView(sv, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, maxH));

        int currentFont = getFontIndex();
        for (int i = 0; i < FONT_NAMES.length; i++) {
            final int idx = i;
            boolean selected = (i == currentFont);

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(14), dp(12), dp(14), dp(12));
            row.setBackground(createRoundedRect(
                    selected ? (isDarkMode() ? 0xFF334155 : 0xFFEFF6FF) : getInputBgColor(),
                    dp(12),
                    selected ? getPrimaryColor() : getBorderColor(),
                    dp(selected ? 2 : 1)));

            TextView nameTv = new TextView(this);
            nameTv.setText(FONT_NAMES[i]);
            nameTv.setTextColor(getTextPrimaryColor());
            nameTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f);
            nameTv.setTypeface(getTypefaceByIndex(i, selected));
            nameTv.setIncludeFontPadding(i != 3);
            LinearLayout.LayoutParams nLp = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            row.addView(nameTv, nLp);

            if (selected) {
                TextView check = new TextView(this);
                check.setText("✓");
                check.setTextColor(isDarkMode() ? getAccentColor() : getPrimaryColor());
                check.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
                applyAppFont(check, true);
                row.addView(check);
            }

            row.setOnClickListener(v -> {
                prefs.edit().putInt(KEY_FONT_INDEX, idx).apply();
                dialog.dismiss();
                showSettingsScreen();
            });

            LinearLayout.LayoutParams rLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            rLp.bottomMargin = dp(7);
            list.addView(row, rLp);
        }

        dialog.show();
    }

    // ==================== TOOLBAR CUSTOMIZATION (SMOOTH CONTINUOUS DRAG & DROP) ====================

    private void showToolbarCustomizationDialog() {
        final List<String> currentOrder = new ArrayList<>(getOrderedToolIds());
        final Set<String> currentEnabled = new HashSet<>(getEnabledToolIds());

        LinearLayout dialogCard = new LinearLayout(this);
        dialogCard.setOrientation(LinearLayout.VERTICAL);
        dialogCard.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        dialogCard.setClipChildren(false);
        dialogCard.setClipToPadding(false);
        dialogCard.setPadding(dp(16), dp(18), dp(16), dp(16));
        dialogCard.setBackground(createRoundedRect(getCardBgColor(), dp(20), getBorderColor(), dp(1)));

        final Dialog dialog = createBaseDialog(dialogCard);

        TextView title = new TextView(this);
        title.setText("تخصيص أدوات التحرير");
        title.setTextColor(getTextPrimaryColor());
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16.5f);
        applyAppFont(title, true);
        dialogCard.addView(title);

        TextView hint = new TextView(this);
        hint.setText("اسحب أي أداة من علامة (≡) لتحريكها بحرية للأعلى أو الأسفل:");
        hint.setTextColor(getTextSecondaryColor());
        hint.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
        applyAppFont(hint, false);
        LinearLayout.LayoutParams hLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hLp.topMargin = dp(4);
        hLp.bottomMargin = dp(10);
        dialogCard.addView(hint, hLp);

        LinearLayout topResetWrap = new LinearLayout(this);
        topResetWrap.setOrientation(LinearLayout.VERTICAL);
        topResetWrap.setClipChildren(false);
        topResetWrap.setClipToPadding(false);
        topResetWrap.setPadding(dp(2), dp(2), dp(2), dp(6));
        dialogCard.addView(topResetWrap, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        final ScrollView listScroll = new ScrollView(this);
        int maxListHeight = (int) (getResources().getDisplayMetrics().heightPixels * 0.48f);
        final LinearLayout listContainer = new LinearLayout(this);
        listContainer.setOrientation(LinearLayout.VERTICAL);
        listContainer.setClipChildren(false);
        listContainer.setClipToPadding(false);
        listScroll.addView(listContainer);
        dialogCard.addView(listScroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, maxListHeight));

        final int rowHeightPx = dp(48);
        final int rowMarginPx = dp(6);
        final int stepHeight = rowHeightPx + rowMarginPx;

        final Runnable[] rebuildRows = new Runnable[1];
        rebuildRows[0] = () -> {
            listContainer.removeAllViews();
            for (int i = 0; i < currentOrder.size(); i++) {
                final int startIndex = i;
                final String toolId = currentOrder.get(i);
                final boolean isChecked = currentEnabled.contains(toolId);

                final LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setPadding(dp(10), dp(6), dp(10), dp(6));
                row.setBackground(createRoundedRect(getInputBgColor(), dp(12), getBorderColor(), dp(1)));

                final TextView checkIcon = new TextView(this);
                checkIcon.setText(isChecked ? "✓" : "");
                checkIcon.setTextColor(Color.WHITE);
                checkIcon.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
                checkIcon.setGravity(Gravity.CENTER);
                applyAppFont(checkIcon, true);
                checkIcon.setBackground(createRoundedRect(
                        isChecked ? getPrimaryColor() : getCardBgColor(),
                        dp(6),
                        isChecked ? getPrimaryColor() : getTextSecondaryColor(),
                        dp(1.5f)));
                LinearLayout.LayoutParams chkLp = new LinearLayout.LayoutParams(dp(22), dp(22));
                chkLp.leftMargin = dp(10);
                row.addView(checkIcon, chkLp);

                ImageView toolIcon = createVectorIcon(getToolIconRes(toolId), getTextPrimaryColor(), 19);
                LinearLayout.LayoutParams icLp = new LinearLayout.LayoutParams(dp(19), dp(19));
                icLp.leftMargin = dp(10);
                row.addView(toolIcon, icLp);

                TextView labelTv = new TextView(this);
                labelTv.setText(getToolLabel(toolId));
                labelTv.setTextColor(getTextPrimaryColor());
                labelTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f);
                applyAppFont(labelTv, isChecked);
                LinearLayout.LayoutParams lblLp = new LinearLayout.LayoutParams(
                        0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                row.addView(labelTv, lblLp);

                View.OnClickListener toggleCheck = v -> {
                    if (currentEnabled.contains(toolId)) {
                        if (currentEnabled.size() <= 1) {
                            Toast.makeText(this, "يجب إبقاء أداة واحدة مفعلة على الأقل", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        currentEnabled.remove(toolId);
                    } else {
                        currentEnabled.add(toolId);
                    }
                    rebuildRows[0].run();
                };
                checkIcon.setOnClickListener(toggleCheck);
                labelTv.setOnClickListener(toggleCheck);

                FrameLayout dragHandleWrap = new FrameLayout(this);
                dragHandleWrap.setPadding(dp(12), dp(6), dp(8), dp(6));
                ImageView dragIcon = createVectorIcon("ic_drag_handle", getTextSecondaryColor(), 20);
                dragHandleWrap.addView(dragIcon, new FrameLayout.LayoutParams(dp(20), dp(20), Gravity.CENTER));

                final float[] downRawY = {0f};
                final int[] startScrollY = {0};
                final int[] targetIndex = {startIndex};

                dragHandleWrap.setOnTouchListener((v, event) -> {
                    int action = event.getActionMasked();
                    if (action == MotionEvent.ACTION_DOWN) {
                        downRawY[0] = event.getRawY();
                        startScrollY[0] = listScroll.getScrollY();
                        targetIndex[0] = startIndex;
                        listScroll.requestDisallowInterceptTouchEvent(true);
                        row.bringToFront();
                        row.setBackground(createRoundedRect(
                                isDarkMode() ? 0xFF334155 : 0xFFE0F2FE, dp(12), getPrimaryColor(), dp(2)));
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            row.setElevation(dp(8));
                        }
                        return true;
                    } else if (action == MotionEvent.ACTION_MOVE) {
                        int[] scrollLoc = new int[2];
                        listScroll.getLocationOnScreen(scrollLoc);
                        float fingerYInScroll = event.getRawY() - scrollLoc[1];
                        int scrollZone = dp(42);
                        if (fingerYInScroll < scrollZone) {
                            listScroll.scrollBy(0, -dp(7));
                        } else if (fingerYInScroll > listScroll.getHeight() - scrollZone) {
                            listScroll.scrollBy(0, dp(7));
                        }

                        int scrollDelta = listScroll.getScrollY() - startScrollY[0];
                        float dy = (event.getRawY() - downRawY[0]) + scrollDelta;

                        float minDy = -startIndex * stepHeight;
                        float maxDy = (currentOrder.size() - 1 - startIndex) * stepHeight;
                        float clampedDy = Math.max(minDy, Math.min(maxDy, dy));
                        row.setTranslationY(clampedDy);

                        int newTarget = Math.round((float) startIndex + (clampedDy / stepHeight));
                        newTarget = Math.max(0, Math.min(currentOrder.size() - 1, newTarget));
                        targetIndex[0] = newTarget;

                        for (int j = 0; j < listContainer.getChildCount(); j++) {
                            if (j == startIndex) continue;
                            View other = listContainer.getChildAt(j);
                            float shift = 0f;
                            if (startIndex < newTarget && j > startIndex && j <= newTarget) {
                                shift = -stepHeight;
                            } else if (startIndex > newTarget && j < startIndex && j >= newTarget) {
                                shift = stepHeight;
                            }
                            other.animate().translationY(shift).setDuration(90).start();
                        }
                        return true;
                    } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                        listScroll.requestDisallowInterceptTouchEvent(false);
                        int finalIdx = targetIndex[0];
                        if (finalIdx != startIndex && finalIdx >= 0 && finalIdx < currentOrder.size()) {
                            String moved = currentOrder.remove(startIndex);
                            currentOrder.add(finalIdx, moved);
                        }
                        rebuildRows[0].run();
                        return true;
                    }
                    return false;
                });

                row.addView(dragHandleWrap);

                LinearLayout.LayoutParams rLp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, rowHeightPx);
                rLp.bottomMargin = rowMarginPx;
                listContainer.addView(row, rLp);
            }
        };

        TextView resetBtn = createButton("استعادة الافتراضي", getSecondaryBtnBgColor(), getTextPrimaryColor(), true, v -> {
            showStyledDialog(
                    "استعادة الإعدادات الافتراضية",
                    "هل أنت متأكد أنك تريد استعادة الإعدادات الافتراضية؟",
                    null,
                    "نعم", false, () -> {
                        currentOrder.clear();
                        currentOrder.addAll(Arrays.asList(TOOL_IDS));
                        currentEnabled.clear();
                        currentEnabled.addAll(Arrays.asList(DEFAULT_ENABLED_TOOLS));
                        rebuildRows[0].run();
                    },
                    "لا", null
            );
        });
        LinearLayout.LayoutParams resLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        resLp.bottomMargin = dp(4);
        topResetWrap.addView(resetBtn, resLp);

        rebuildRows[0].run();

        LinearLayout actionsRow = new LinearLayout(this);
        actionsRow.setOrientation(LinearLayout.HORIZONTAL);
        actionsRow.setGravity(Gravity.CENTER_VERTICAL);
        actionsRow.setClipChildren(false);
        actionsRow.setClipToPadding(false);
        actionsRow.setPadding(dp(2), dp(4), dp(2), dp(6));
        LinearLayout.LayoutParams actLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        actLp.topMargin = dp(10);

        TextView saveBtn = createButton("حفظ", getPrimaryColor(), Color.WHITE, true, v -> {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < currentOrder.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append(currentOrder.get(i));
            }
            prefs.edit()
                    .putString(KEY_TOOLBAR_ORDER, sb.toString())
                    .putStringSet(KEY_ENABLED_TOOLS, currentEnabled)
                    .apply();
            dialog.dismiss();
            Toast.makeText(this, "تم حفظ تخصيص الأدوات بنجاح", Toast.LENGTH_SHORT).show();
        });
        LinearLayout.LayoutParams sLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        sLp.leftMargin = dp(8);
        sLp.bottomMargin = dp(4);
        actionsRow.addView(saveBtn, sLp);

        TextView cancelBtn = createButton("إلغاء", getSecondaryBtnBgColor(), getTextPrimaryColor(), false, v -> dialog.dismiss());
        LinearLayout.LayoutParams cLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        cLp.bottomMargin = dp(4);
        actionsRow.addView(cancelBtn, cLp);

        dialogCard.addView(actionsRow, actLp);
        dialog.show();
    }

    // ==================== EDITOR TOOLS LOGIC ====================

    private void performUndo() {
        if (editorEditText == null || undoIndex <= 0) return;
        isUndoRedoAction = true;
        undoIndex--;
        String text = undoStack.get(undoIndex);
        editorEditText.setText(text);
        editorEditText.setSelection(text.length());
        isUndoRedoAction = false;
        saveCurrentDocumentNow();
    }

    private void performRedo() {
        if (editorEditText == null || undoIndex >= undoStack.size() - 1) return;
        isUndoRedoAction = true;
        undoIndex++;
        String text = undoStack.get(undoIndex);
        editorEditText.setText(text);
        editorEditText.setSelection(text.length());
        isUndoRedoAction = false;
        saveCurrentDocumentNow();
    }

    private void performCopy() {
        if (editorEditText == null) return;
        int start = Math.min(editorEditText.getSelectionStart(), editorEditText.getSelectionEnd());
        int end = Math.max(editorEditText.getSelectionStart(), editorEditText.getSelectionEnd());
        String textToCopy;
        if (start >= 0 && end > start) {
            textToCopy = editorEditText.getText().subSequence(start, end).toString();
        } else {
            textToCopy = editorEditText.getText().toString();
        }
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm != null) {
            cm.setPrimaryClip(ClipData.newPlainText("نص مفكرة مزود", textToCopy));
            Toast.makeText(this, "تم النسخ إلى الحافظة", Toast.LENGTH_SHORT).show();
        }
    }

    private void performCut() {
        if (editorEditText == null) return;
        int start = Math.min(editorEditText.getSelectionStart(), editorEditText.getSelectionEnd());
        int end = Math.max(editorEditText.getSelectionStart(), editorEditText.getSelectionEnd());
        if (start >= 0 && end > start) {
            String selected = editorEditText.getText().subSequence(start, end).toString();
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null) {
                cm.setPrimaryClip(ClipData.newPlainText("نص مفكرة مزود", selected));
            }
            editorEditText.getText().delete(start, end);
            Toast.makeText(this, "تم قص النص المحدد", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "يرجى تحديد نص أولاً لقصه", Toast.LENGTH_SHORT).show();
        }
    }

    private void performPaste() {
        if (editorEditText == null) return;
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm != null && cm.hasPrimaryClip() && cm.getPrimaryClip() != null && cm.getPrimaryClip().getItemCount() > 0) {
            CharSequence clip = cm.getPrimaryClip().getItemAt(0).coerceToText(this);
            if (clip != null) {
                int start = Math.max(0, Math.min(editorEditText.getSelectionStart(), editorEditText.getSelectionEnd()));
                int end = Math.max(0, Math.max(editorEditText.getSelectionStart(), editorEditText.getSelectionEnd()));
                editorEditText.getText().replace(start, end, clip);
            }
        }
    }

    private void performSelectUp() {
        if (editorEditText == null) return;
        editorEditText.requestFocus();
        int cursor = Math.max(0, editorEditText.getSelectionEnd());
        editorEditText.setSelection(0, cursor);
    }

    private void performSelectDown() {
        if (editorEditText == null) return;
        editorEditText.requestFocus();
        int cursor = Math.max(0, editorEditText.getSelectionStart());
        int len = editorEditText.getText().length();
        editorEditText.setSelection(cursor, len);
    }

    private void insertDateTimeAtCursor() {
        if (editorEditText == null) return;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd - hh:mm a", new Locale("ar"));
        String dt = sdf.format(new Date());
        int start = Math.max(0, Math.min(editorEditText.getSelectionStart(), editorEditText.getSelectionEnd()));
        int end = Math.max(0, Math.max(editorEditText.getSelectionStart(), editorEditText.getSelectionEnd()));
        editorEditText.getText().replace(start, end, dt);
    }

    private void performSmartFormat() {
        if (editorEditText == null) return;
        String raw = editorEditText.getText().toString();
        if (raw.trim().isEmpty()) return;

        String[] lines = raw.split("\n");
        StringBuilder sb = new StringBuilder();
        int blankCount = 0;

        for (String line : lines) {
            String cleaned = line.trim().replaceAll("[ \\t]+", " ");
            cleaned = cleaned.replaceAll("\\s+([،؛؟.,!:])", "$1");
            cleaned = cleaned.replaceAll("([،؛؟.,!:])([^\\s\\d])", "$1 $2");

            if (cleaned.isEmpty()) {
                blankCount++;
                if (blankCount <= 1 && sb.length() > 0) {
                    sb.append("\n");
                }
            } else {
                blankCount = 0;
                if (sb.length() > 0) sb.append("\n");
                sb.append(cleaned);
            }
        }

        String formatted = sb.toString().trim();
        editorEditText.setText(formatted);
        editorEditText.setSelection(formatted.length());
        saveCurrentDocumentNow();
        Toast.makeText(this, "تم تنسيق النص وترتيب المسافات", Toast.LENGTH_SHORT).show();
    }

    private void insertSignatureAtBottom() {
        if (editorEditText == null) return;
        String sig = prefs.getString(KEY_SIGNATURE, DEFAULT_SIGNATURE).trim();
        if (sig.isEmpty()) {
            showQuickSetSignatureDialog();
            return;
        }
        String current = editorEditText.getText().toString().trim();
        String updated = current.isEmpty() ? sig : (current + "\n\n" + sig);
        editorEditText.setText(updated);
        editorEditText.setSelection(updated.length());
        saveCurrentDocumentNow();
        Toast.makeText(this, "تم إدراج التوقيع في نهاية الملف", Toast.LENGTH_SHORT).show();
    }

    private void showQuickSetSignatureDialog() {
        final EditText et = new EditText(this);
        et.setText(DEFAULT_SIGNATURE);
        et.setTextColor(getTextPrimaryColor());
        et.setPadding(dp(14), dp(12), dp(14), dp(12));
        et.setBackground(createRoundedRect(getInputBgColor(), dp(12), getBorderColor(), dp(1)));
        applyAppFont(et, false);

        showStyledDialog(
                "إعداد التوقيع",
                "اكتب نص التوقيع ليتم حفظه وإدراجه:",
                et,
                "حفظ وإدراج", false, () -> {
                    String s = et.getText().toString().trim();
                    if (!s.isEmpty()) {
                        prefs.edit().putString(KEY_SIGNATURE, s).apply();
                        insertSignatureAtBottom();
                    }
                },
                "إلغاء", null
        );
    }

    // ==================== ZOOM & STATS ====================

    private int getZoomPercent() {
        return prefs.getInt(KEY_ZOOM_PERCENT, 100);
    }

    private float getScaledFontSizeSp() {
        return 18f * (getZoomPercent() / 100f);
    }

    private void changeZoom(int delta) {
        int next = Math.max(70, Math.min(220, getZoomPercent() + delta));
        prefs.edit().putInt(KEY_ZOOM_PERCENT, next).apply();
        applyZoomToCurrentDocument();
    }

    private void showResetZoomDialog() {
        showStyledDialog(
                "حجم الخط",
                "هل تريد إعادة حجم الخط إلى الحجم الافتراضي (100%)؟",
                null,
                "إعادة لـ 100%", false, () -> {
                    prefs.edit().putInt(KEY_ZOOM_PERCENT, 100).apply();
                    applyZoomToCurrentDocument();
                },
                "إلغاء", null
        );
    }

    private void applyZoomToCurrentDocument() {
        if (zoomValueText != null) {
            zoomValueText.setText(getZoomPercent() + "%");
        }
        float size = getScaledFontSizeSp();
        if (editorEditText != null) {
            editorEditText.setTextSize(TypedValue.COMPLEX_UNIT_SP, size);
        }
        if (readTextView != null) {
            readTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, size);
        }
    }

    private void updateStatsBar(String text) {
        if (statsBarText == null) return;
        String trimmed = text == null ? "" : text.trim();
        int words = trimmed.isEmpty() ? 0 : trimmed.split("\\s+").length;
        int chars = text == null ? 0 : text.length();
        int lines = (text == null || text.isEmpty()) ? 0 : text.split("\n", -1).length;
        statsBarText.setText("الكلمات: " + words + "   •   الحروف: " + chars + "   •   الأسطر: " + lines);
    }

    // ==================== FILE OPERATIONS (SAF) ====================

    private void pickFolder() {
        try {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                    | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                    | Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
            startActivityForResult(intent, REQ_PICK_FOLDER);
        } catch (Exception e) {
            Toast.makeText(this, "تعذر فتح مستعرض المجلدات", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean hasFolderPermission(Uri treeUri) {
        if (treeUri == null) return false;
        try {
            String docId = DocumentsContract.getTreeDocumentId(treeUri);
            Uri dirUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId);
            return dirUri != null;
        } catch (Exception e) {
            return false;
        }
    }

    private void loadFilesFromFolder() {
        allFiles.clear();
        if (folderTreeUri == null || !hasFolderPermission(folderTreeUri)) return;

        Set<String> pinnedSet = prefs.getStringSet(KEY_PINNED_URIS, new HashSet<>());
        try {
            String parentDocId = DocumentsContract.getTreeDocumentId(folderTreeUri);
            Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(folderTreeUri, parentDocId);

            String[] projection = new String[]{
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_LAST_MODIFIED,
                    DocumentsContract.Document.COLUMN_SIZE,
                    DocumentsContract.Document.COLUMN_MIME_TYPE
            };

            try (Cursor cursor = getContentResolver().query(childrenUri, projection, null, null, null)) {
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        String docId = cursor.getString(0);
                        String name = cursor.getString(1);
                        long modified = cursor.isNull(2) ? 0L : cursor.getLong(2);
                        long size = cursor.isNull(3) ? 0L : cursor.getLong(3);
                        String mime = cursor.getString(4);

                        if (DocumentsContract.Document.MIME_TYPE_DIR.equals(mime)) continue;
                        if (name == null || !name.toLowerCase(Locale.ROOT).endsWith(".txt")) continue;

                        Uri fileUri = DocumentsContract.buildDocumentUriUsingTree(folderTreeUri, docId);
                        FileItem item = new FileItem();
                        item.uri = fileUri;
                        item.name = name;
                        item.lastModified = modified;
                        item.size = size;
                        item.pinned = pinnedSet != null && pinnedSet.contains(fileUri.toString());
                        allFiles.add(item);
                    }
                }
            }
        } catch (Exception ignored) {}

        Collections.sort(allFiles, (a, b) -> {
            if (a.pinned != b.pinned) return a.pinned ? -1 : 1;
            return Long.compare(b.lastModified, a.lastModified);
        });
    }

    private void onNewFileClicked() {
        if (folderTreeUri == null || !hasFolderPermission(folderTreeUri)) {
            Toast.makeText(this, "يرجى اختيار مجلد الحفظ أولاً", Toast.LENGTH_SHORT).show();
            pickFolder();
            return;
        }

        final EditText input = new EditText(this);
        input.setHint("اكتب اسم الملف هنا...");
        input.setHintTextColor(getTextSecondaryColor());
        input.setTextColor(getTextPrimaryColor());
        input.setSingleLine(true);
        input.setPadding(dp(14), dp(12), dp(14), dp(12));
        input.setBackground(createRoundedRect(getInputBgColor(), dp(12), getBorderColor(), dp(1)));
        applyAppFont(input, false);

        showStyledDialog(
                "ملف نصي جديد",
                null,
                input,
                "إنشاء الملف", false, () -> {
                    String rawName = input.getText().toString().trim();
                    if (rawName.isEmpty()) {
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US);
                        rawName = "ملاحظة_" + sdf.format(new Date());
                    }
                    String finalName = ensureTxtExtension(rawName);
                    createNewTxtFile(finalName);
                },
                "إلغاء", null
        );
    }

    private void createNewTxtFile(String fileName) {
        try {
            String parentDocId = DocumentsContract.getTreeDocumentId(folderTreeUri);
            Uri parentDocUri = DocumentsContract.buildDocumentUriUsingTree(folderTreeUri, parentDocId);
            Uri newDocUri = DocumentsContract.createDocument(getContentResolver(), parentDocUri, "text/plain", fileName);
            if (newDocUri != null) {
                writeTextToUri(newDocUri, "");
                loadFilesFromFolder();
                openDocument(newDocUri, fileName, true);
            } else {
                Toast.makeText(this, "تعذر إنشاء الملف", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "حدث خطأ أثناء إنشاء الملف", Toast.LENGTH_SHORT).show();
        }
    }

    private void duplicateFile(FileItem item) {
        try {
            String content = readTextFromUri(item.uri);
            String baseName = stripTxtExtension(item.name);
            String copyName = ensureTxtExtension(baseName + " - نسخة");

            String parentDocId = DocumentsContract.getTreeDocumentId(folderTreeUri);
            Uri parentDocUri = DocumentsContract.buildDocumentUriUsingTree(folderTreeUri, parentDocId);
            Uri newDocUri = DocumentsContract.createDocument(getContentResolver(), parentDocUri, "text/plain", copyName);
            if (newDocUri != null) {
                writeTextToUri(newDocUri, content);
                loadFilesFromFolder();
                renderHomeFilesList();
                Toast.makeText(this, "تم إنشاء نسخة مكررة من الملف", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "تعذر تكرار الملف", Toast.LENGTH_SHORT).show();
        }
    }

    private void showRenameDialog(Uri fileUri, String oldName, boolean fromOpenDoc) {
        if (fromOpenDoc && isEditMode) {
            saveCurrentDocumentNow();
        }

        final EditText input = new EditText(this);
        input.setText(stripTxtExtension(oldName));
        input.setTextColor(getTextPrimaryColor());
        input.setSingleLine(true);
        input.setPadding(dp(14), dp(12), dp(14), dp(12));
        input.setBackground(createRoundedRect(getInputBgColor(), dp(12), getBorderColor(), dp(1)));
        applyAppFont(input, false);
        input.setSelection(input.getText().length());

        showStyledDialog(
                "إعادة تسمية الملف",
                null,
                input,
                "حفظ الاسم", false, () -> {
                    String newBase = input.getText().toString().trim();
                    if (newBase.isEmpty()) return;
                    String newFullName = ensureTxtExtension(newBase);
                    if (newFullName.equals(oldName)) return;

                    try {
                        String oldUriStr = fileUri.toString();
                        Set<String> pinned = new HashSet<>(prefs.getStringSet(KEY_PINNED_URIS, new HashSet<>()));
                        boolean wasPinned = pinned.contains(oldUriStr);

                        Uri renamedUri = DocumentsContract.renameDocument(getContentResolver(), fileUri, newFullName);
                        if (renamedUri != null) {
                            if (wasPinned) {
                                pinned.remove(oldUriStr);
                                pinned.add(renamedUri.toString());
                                prefs.edit().putStringSet(KEY_PINNED_URIS, pinned).apply();
                            }

                            String cached = contentCache.remove(oldUriStr);
                            if (cached != null) {
                                contentCache.put(renamedUri.toString(), cached);
                            }

                            renameAssociatedSafeSnapshots(oldName, newFullName);

                            if (fromOpenDoc) {
                                currentFileUri = renamedUri;
                                currentFileName = newFullName;
                                if (docTitleView != null) {
                                    docTitleView.setText(stripTxtExtension(newFullName));
                                }
                            }
                            loadFilesFromFolder();
                            if (currentScreen == Screen.HOME) {
                                renderHomeFilesList();
                            }
                            Toast.makeText(this, "تم تغيير الاسم بنجاح", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Toast.makeText(this, "تعذر تغيير اسم الملف", Toast.LENGTH_SHORT).show();
                    }
                },
                "إلغاء", null
        );
    }

    private void confirmDeleteFile(FileItem item) {
        showStyledDialog(
                "تأكيد الحذف",
                "هل أنت متأكد من حذف الملف «" + stripTxtExtension(item.name) + "»؟",
                null,
                "حذف الملف", true, () -> {
                    try {
                        createSafeSnapshot(item, true);
                        boolean deleted = DocumentsContract.deleteDocument(getContentResolver(), item.uri);
                        if (deleted) {
                            contentCache.remove(item.uri.toString());
                            loadFilesFromFolder();
                            renderHomeFilesList();
                            Toast.makeText(this, "تم حذف الملف", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Toast.makeText(this, "تعذر حذف الملف", Toast.LENGTH_SHORT).show();
                    }
                },
                "إلغاء", null
        );
    }

    private void togglePinFile(Uri uri) {
        Set<String> current = new HashSet<>(prefs.getStringSet(KEY_PINNED_URIS, new HashSet<>()));
        String key = uri.toString();
        if (current.contains(key)) {
            current.remove(key);
            Toast.makeText(this, "تم إلغاء تثبيت الملف", Toast.LENGTH_SHORT).show();
        } else {
            current.add(key);
            Toast.makeText(this, "تم تثبيت الملف في الأعلى", Toast.LENGTH_SHORT).show();
        }
        prefs.edit().putStringSet(KEY_PINNED_URIS, current).apply();
    }

    private String getCachedFileContent(Uri uri) {
        String key = uri.toString();
        if (contentCache.containsKey(key)) {
            return contentCache.get(key);
        }
        String text = readTextFromUri(uri);
        contentCache.put(key, text);
        return text;
    }

    private String readTextFromUri(Uri uri) {
        if (uri == null) return "";
        StringBuilder sb = new StringBuilder();
        try (InputStream is = getContentResolver().openInputStream(uri);
             BufferedReader reader = is != null ? new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8)) : null) {
            if (reader != null) {
                char[] buf = new char[4096];
                int n;
                while ((n = reader.read(buf)) != -1) {
                    sb.append(buf, 0, n);
                }
            }
        } catch (Exception ignored) {}
        return sb.toString();
    }

    private void writeTextToUri(Uri uri, String content) {
        if (uri == null) return;
        String safeContent = content != null ? content : "";
        try (OutputStream os = getContentResolver().openOutputStream(uri, "wt")) {
            if (os != null) {
                os.write(safeContent.getBytes(StandardCharsets.UTF_8));
                os.flush();
            }
        } catch (Exception e) {
            try (OutputStream os = getContentResolver().openOutputStream(uri, "w")) {
                if (os != null) {
                    os.write(safeContent.getBytes(StandardCharsets.UTF_8));
                    os.flush();
                }
            } catch (Exception ignored) {}
        }
        contentCache.put(uri.toString(), safeContent);
    }

    private void saveCurrentDocumentNow() {
        if (currentFileUri == null || editorEditText == null) return;
        saveHandler.removeCallbacks(autoSaveRunnable);
        String newText = editorEditText.getText().toString();
        currentFileContent = newText;
        if (!newText.equals(lastDiskSavedContent)) {
            writeTextToUri(currentFileUri, newText);
            lastDiskSavedContent = newText;
        }
    }

    // ==================== ZIP BACKUP & SHARE ====================

    private void startZipBackup() {
        loadFilesFromFolder();
        if (allFiles.isEmpty()) {
            Toast.makeText(this, "لا توجد ملفات لعمل نسخة احتياطية لها", Toast.LENGTH_SHORT).show();
            return;
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        String zipName = "نسخة_احتياطية_مفكرة_مزود_" + sdf.format(new Date()) + ".zip";

        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/zip");
        intent.putExtra(Intent.EXTRA_TITLE, zipName);
        startActivityForResult(intent, REQ_CREATE_ZIP);
    }

    private void writeZipBackupToUri(Uri destZipUri) {
        try (OutputStream os = getContentResolver().openOutputStream(destZipUri);
             ZipOutputStream zos = os != null ? new ZipOutputStream(new BufferedOutputStream(os)) : null) {
            if (zos == null) return;
            for (FileItem item : allFiles) {
                String content = readTextFromUri(item.uri);
                ZipEntry entry = new ZipEntry(item.name);
                zos.putNextEntry(entry);
                byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
                zos.write(bytes, 0, bytes.length);
                zos.closeEntry();
            }
            zos.finish();
            Toast.makeText(this, "تم حفظ النسخة الاحتياطية (ZIP) بنجاح", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "تعذر إنشاء ملف النسخة الاحتياطية", Toast.LENGTH_SHORT).show();
        }
    }

    private void shareTextContent(String title, String body) {
        Intent sendIntent = new Intent(Intent.ACTION_SEND);
        sendIntent.setType("text/plain");
        sendIntent.putExtra(Intent.EXTRA_SUBJECT, title);
        sendIntent.putExtra(Intent.EXTRA_TEXT, body);
        startActivity(Intent.createChooser(sendIntent, "مشاركة النص عبر:"));
    }

    // ==================== LIFECYCLE & BACK NAVIGATION ====================

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_PICK_FOLDER && resultCode == Activity.RESULT_OK && data != null) {
            Uri treeUri = data.getData();
            if (treeUri != null) {
                final int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
                try {
                    getContentResolver().takePersistableUriPermission(treeUri, takeFlags);
                } catch (Exception ignored) {}
                folderTreeUri = treeUri;
                prefs.edit().putString(KEY_FOLDER_URI, treeUri.toString()).apply();
                contentCache.clear();
                showHomeScreen();
            }
        } else if (requestCode == REQ_CREATE_ZIP && resultCode == Activity.RESULT_OK && data != null) {
            Uri zipUri = data.getData();
            if (zipUri != null) {
                writeZipBackupToUri(zipUri);
            }
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (!hasFocus && currentScreen == Screen.DOCUMENT && isEditMode && currentFileUri != null) {
            saveCurrentDocumentNow();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (currentScreen == Screen.DOCUMENT && isEditMode && currentFileUri != null) {
            saveCurrentDocumentNow();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (currentScreen == Screen.DOCUMENT && isEditMode && currentFileUri != null) {
            saveCurrentDocumentNow();
        }
    }

    @Override
    public void onBackPressed() {
        if (currentScreen == Screen.DOCUMENT) {
            if (isEditMode) {
                saveCurrentDocumentNow();
                isEditMode = false;
                hideKeyboard();
                renderDocumentScreen();
            } else {
                showHomeScreen();
            }
        } else if (currentScreen == Screen.SETTINGS) {
            showHomeScreen();
        } else {
            super.onBackPressed();
        }
    }

    // ==================== SMART ARABIC NORMALIZATION & SEARCH ====================

    private boolean isIgnoredArabicDiacritic(char c) {
        return (c >= '\u064B' && c <= '\u065F') || c == '\u0670' || c == '\u0640';
    }

    private char normalizeArabicChar(char c) {
        switch (c) {
            case 'أ':
            case 'إ':
            case 'آ':
            case 'ٱ':
                return 'ا';
            case 'ة':
                return 'ه';
            case 'ى':
                return 'ي';
            case 'ؤ':
                return 'و';
            case 'ئ':
                return 'ي';
            default:
                return Character.toLowerCase(c);
        }
    }

    private String normalizeForSearch(String input) {
        if (input == null || input.isEmpty()) return "";
        StringBuilder sb = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (isIgnoredArabicDiacritic(c)) continue;
            sb.append(normalizeArabicChar(c));
        }
        return sb.toString();
    }

    private boolean smartContains(String text, String query) {
        if (text == null || query == null) return false;
        String normQ = normalizeForSearch(query);
        if (normQ.isEmpty()) return true;
        String normT = normalizeForSearch(text);
        return normT.contains(normQ);
    }

    private List<int[]> findSmartMatchRanges(String text, String query) {
        List<int[]> ranges = new ArrayList<>();
        if (text == null || query == null) return ranges;
        String normQ = normalizeForSearch(query);
        if (normQ.isEmpty()) return ranges;

        StringBuilder normText = new StringBuilder(text.length());
        int[] normToOrig = new int[text.length() + 1];
        int normIdx = 0;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (isIgnoredArabicDiacritic(c)) continue;
            normToOrig[normIdx] = i;
            normText.append(normalizeArabicChar(c));
            normIdx++;
        }
        normToOrig[normIdx] = text.length();
        String normStr = normText.toString();

        int pos = 0;
        while ((pos = normStr.indexOf(normQ, pos)) != -1) {
            int origStart = normToOrig[pos];
            int endNormPos = pos + normQ.length() - 1;
            int origEnd = normToOrig[endNormPos] + 1;
            while (origEnd < text.length() && isIgnoredArabicDiacritic(text.charAt(origEnd))) {
                origEnd++;
            }
            ranges.add(new int[]{origStart, origEnd});
            pos += normQ.length();
        }
        return ranges;
    }

    private int[] findFirstSmartMatch(String text, String query) {
        List<int[]> list = findSmartMatchRanges(text, query);
        return list.isEmpty() ? null : list.get(0);
    }

    private SpannableString highlightSmartText(String text, String query) {
        SpannableString span = new SpannableString(text);
        List<int[]> ranges = findSmartMatchRanges(text, query);
        for (int[] r : ranges) {
            span.setSpan(new BackgroundColorSpan(0xFFFDE047), r[0], r[1], Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            span.setSpan(new ForegroundColorSpan(0xFF0F172A), r[0], r[1], Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return span;
    }

    // ==================== UI HELPERS ====================

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

    private LinearLayout createCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        card.setBackground(createRoundedRect(getCardBgColor(), dp(16), getBorderColor(), dp(1)));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            card.setElevation(dp(2));
        }
        return card;
    }

    private TextView createButton(String text, int bgColor, int textColor, boolean bold, View.OnClickListener onClick) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(textColor);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14.5f);
        applyAppFont(tv, bold);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(dp(14), dp(12), dp(14), dp(12));
        tv.setBackground(createRipple(bgColor, dp(13)));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            tv.setElevation(dp(2));
        }
        tv.setOnClickListener(onClick);
        return tv;
    }

    private TextView createTopBarChip(String text, int bgColor, int textColor, View.OnClickListener onClick) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(textColor);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
        applyAppFont(tv, true);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(dp(12), dp(7), dp(12), dp(7));
        tv.setBackground(createRipple(bgColor, dp(10)));
        tv.setOnClickListener(onClick);
        return tv;
    }

    private TextView createZoomButton(String symbol, View.OnClickListener onClick) {
        TextView tv = new TextView(this);
        tv.setText(symbol);
        tv.setTextColor(Color.WHITE);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        applyAppFont(tv, true);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(dp(10), dp(5), dp(10), dp(5));
        tv.setBackground(createRipple(Color.TRANSPARENT, dp(8)));
        tv.setOnClickListener(onClick);
        return tv;
    }

    private GradientDrawable createRoundedRect(int fillColor, int radiusPx, int strokeColor, int strokeWidthPx) {
        GradientDrawable gd = new GradientDrawable();
        gd.setShape(GradientDrawable.RECTANGLE);
        gd.setColor(fillColor);
        gd.setCornerRadius(radiusPx);
        if (strokeWidthPx > 0) {
            gd.setStroke(strokeWidthPx, strokeColor);
        }
        return gd;
    }

    private RippleDrawable createRipple(int bgColor, int radiusPx) {
        GradientDrawable content = createRoundedRect(bgColor, radiusPx, 0, 0);
        GradientDrawable mask = createRoundedRect(Color.WHITE, radiusPx, 0, 0);
        return new RippleDrawable(ColorStateList.valueOf(0x3394A3B8), content, mask);
    }

    private RippleDrawable createRippleWithStroke(int bgColor, int radiusPx, int strokeColor, int strokeWidthPx) {
        GradientDrawable content = createRoundedRect(bgColor, radiusPx, strokeColor, strokeWidthPx);
        GradientDrawable mask = createRoundedRect(Color.WHITE, radiusPx, 0, 0);
        return new RippleDrawable(ColorStateList.valueOf(0x3394A3B8), content, mask);
    }

    private int dp(float value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics());
    }

    private void showKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    private void hideKeyboard() {
        View v = getCurrentFocus();
        if (v != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
            }
        }
    }
}
