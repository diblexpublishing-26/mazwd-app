package com.mazwd.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.DocumentsContract;
import android.text.Editable;
import android.text.SpannableString;
import android.text.Spanned;
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
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
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
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class MainActivity extends Activity {

    private static final int REQ_OPEN_TREE = 1001;
    private static final String PREFS = "mazwd_prefs";
    private static final String KEY_TREE_URI = "tree_uri";
    private static final String DEFAULT_SIGNATURE = "أ. شفيقة مزود | باحثة في التراث الحضرمي";
    private static final String SIGNATURE_HINT = "اكتب توقيعك هنا\nمثال: عبدالله سعيد - مدير التحرير";

    private enum Mode { HOME, READ, EDIT }
    private enum SortField { DATE, NAME }

    private Mode mode = Mode.HOME;
    private SortField sortField = SortField.DATE;
    private boolean asc = false;
    private float fontScale = 1.0f;
    private int fontChoice = 0; // 0=Sans, 1=Tajawal, 2=Amiri, 3=ArefRuqaa, 4=ReemKufi, 5=Serif, 6=Cursive
    private int themeChoice = 0;
    private boolean showToolbar = true;
    private boolean toolbarIconsOnly = false; // false = نصوص, true = رموز
    private String signatureText = DEFAULT_SIGNATURE;
    private String searchQuery = "";
    private String activeHighlightQuery = "";

    private static final String[] FONT_NAMES = {
            "Sans — خط واضح وعصري (افتراضي)",
            "تجوال — خط حديث وأنيق للمقالات",
            "الأميري — خط نسخ كلاسيكي أصيل",
            "عارف رقعة — خط الرقعة العربي",
            "ريم كوفي — خط كوفي حديث ومميز",
            "Serif — خط إنجليزي رسمي ومريح",
            "Cursive — خط فني مائل"
    };

    private static final String[] TOOL_IDS = {
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

    private static final String[] TOOL_LABELS = {
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

    private static final String[] TOOL_ICON_RES = {
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

    private static final String[] DEFAULT_ENABLED_TOOLS = {
            "undo",
            "redo",
            "copy",
            "paste",
            "select_all",
            "auto_format",
            "signature"
    };

    private final List<String> orderedToolIds = new ArrayList<>();
    private final Set<String> enabledToolIds = new HashSet<>();

    private static final String[] THEME_NAMES = {
            "الأزرق الملكي (افتراضي)",
            "الطيني الحضرمي",
            "الأخضر الزمردي",
            "القهوة العربية",
            "الأرجواني الفاخر",
            "العنابي الدافئ",
            "الفيروزي الهادئ",
            "الكحلي الكلاسيكي"
    };

    private static final int[][] THEME_COLORS = {
            {0xFF1E3A8A, 0xFF172554, 0xFFEFF6FF}, // الأزرق الملكي
            {0xFF9A3412, 0xFF7C2D12, 0xFFFFF7ED}, // الطيني الحضرمي
            {0xFF065F46, 0xFF064E3B, 0xFFECFDF5}, // الأخضر الزمردي
            {0xFF5C4033, 0xFF3E2723, 0xFFF5F0EB}, // القهوة العربية
            {0xFF5B21B6, 0xFF4C1D95, 0xFFF5F3FF}, // الأرجواني الفاخر
            {0xFF881337, 0xFF4C0519, 0xFFFFF1F2}, // العنابي الدافئ
            {0xFF0F766E, 0xFF134E4A, 0xFFF0FDFA}, // الفيروزي الهادئ
            {0xFF1E293B, 0xFF0F172A, 0xFFF1F5F9}  // الكحلي الكلاسيكي
    };

    private final int COLOR_BG = 0xFFF4F6F9;
    private final int COLOR_CARD = 0xFFFFFFFF;
    private int COLOR_PRIMARY = 0xFF1E3A8A;
    private int COLOR_PRIMARY_DARK = 0xFF172554;
    private int COLOR_ACCENT_SOFT = 0xFFEFF6FF;
    private final int COLOR_TEXT_PRIMARY = 0xFF111827;
    private final int COLOR_TEXT_SECONDARY = 0xFF6B7280;
    private final int COLOR_DIVIDER = 0xFFE5E7EB;

    private SharedPreferences prefs;
    private Uri treeUri;
    private LinearLayout root;
    private LinearLayout filesBox;
    private TextView statsView;
    private TextView docTitle;
    private TextView docZoomLabel;
    private TextView readView;
    private ScrollView readScrollView;
    private EditText editor;
    private DocFile currentFile;
    private boolean internalChange = false;
    private final Stack<String> undo = new Stack<>();
    private final Stack<String> redo = new Stack<>();
    private String lastText = "";
    private final Map<Uri, String> searchSnippets = new HashMap<>();

    private final Handler autoSaveHandler = new Handler(Looper.getMainLooper());
    private final Runnable autoSaveRunnable = new Runnable() {
        @Override
        public void run() {
            if (mode == Mode.EDIT && currentFile != null) {
                saveNow(false);
            }
        }
    };

    private static class DocFile {
        Uri uri;
        String name;
        long modified;

        DocFile(Uri u, String n, long m) {
            uri = u;
            name = n;
            modified = m;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        fontScale = prefs.getFloat("font_scale", 1.0f);
        fontChoice = prefs.getInt("font_choice", 0);
        if (fontChoice < 0 || fontChoice >= FONT_NAMES.length) fontChoice = 0;
        themeChoice = prefs.getInt("theme_choice", 0);
        if (themeChoice < 0 || themeChoice >= THEME_NAMES.length) themeChoice = 0;
        showToolbar = prefs.getBoolean("show_toolbar", true);
        toolbarIconsOnly = prefs.getBoolean("toolbar_icons_only", false);

        if (!prefs.getBoolean("sig_init_v3", false)) {
            signatureText = prefs.getString("custom_signature", DEFAULT_SIGNATURE);
            prefs.edit()
                    .putString("custom_signature", signatureText)
                    .putBoolean("sig_init_v3", true)
                    .apply();
        } else {
            signatureText = prefs.getString("custom_signature", "");
        }

        loadToolsConfig();
        applyThemeColors();

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(COLOR_BG);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        setContentView(root);

        String saved = prefs.getString(KEY_TREE_URI, null);
        if (saved != null) {
            try {
                treeUri = Uri.parse(saved);
            } catch (Exception ignored) {
            }
        }

        if (treeUri == null || !hasTreePermission(treeUri)) {
            askForFolder();
        } else {
            showHome();
        }
    }

    private void loadToolsConfig() {
        orderedToolIds.clear();
        String savedOrder = prefs.getString("toolbar_order_v3", null);
        if (savedOrder != null && !savedOrder.trim().isEmpty()) {
            for (String part : savedOrder.split(",")) {
                String id = part.trim();
                if (toolIndex(id) >= 0 && !orderedToolIds.contains(id)) {
                    orderedToolIds.add(id);
                }
            }
        }
        for (String id : TOOL_IDS) {
            if (!orderedToolIds.contains(id)) {
                orderedToolIds.add(id);
            }
        }

        Set<String> savedEnabled = prefs.getStringSet("enabled_tools_v3", null);
        enabledToolIds.clear();
        if (savedEnabled == null) {
            enabledToolIds.addAll(Arrays.asList(DEFAULT_ENABLED_TOOLS));
        } else {
            for (String id : savedEnabled) {
                if (toolIndex(id) >= 0) {
                    enabledToolIds.add(id);
                }
            }
        }
    }

    private void saveToolsConfig() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < orderedToolIds.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(orderedToolIds.get(i));
        }
        prefs.edit()
                .putString("toolbar_order_v3", sb.toString())
                .putStringSet("enabled_tools_v3", new HashSet<>(enabledToolIds))
                .apply();
    }

    private int toolIndex(String id) {
        for (int i = 0; i < TOOL_IDS.length; i++) {
            if (TOOL_IDS[i].equals(id)) return i;
        }
        return -1;
    }

    private String getToolLabel(String id) {
        int idx = toolIndex(id);
        return idx >= 0 ? TOOL_LABELS[idx] : id;
    }

    private String getToolIconResName(String id) {
        int idx = toolIndex(id);
        return idx >= 0 ? TOOL_ICON_RES[idx] : "ic_auto_format";
    }

    private void applyThemeColors() {
        COLOR_PRIMARY = THEME_COLORS[themeChoice][0];
        COLOR_PRIMARY_DARK = THEME_COLORS[themeChoice][1];
        COLOR_ACCENT_SOFT = THEME_COLORS[themeChoice][2];

        Window window = getWindow();
        if (window != null) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(COLOR_PRIMARY_DARK);
            window.setNavigationBarColor(COLOR_CARD);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mode == Mode.EDIT && currentFile != null) {
            autoSaveHandler.removeCallbacks(autoSaveRunnable);
            saveNow(false);
        }
    }

    private boolean hasTreePermission(Uri uri) {
        try {
            for (android.content.UriPermission p : getContentResolver().getPersistedUriPermissions()) {
                if (p.getUri().equals(uri) && p.isReadPermission() && p.isWritePermission()) {
                    return true;
                }
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    private void askForFolder() {
        root.removeAllViews();

        LinearLayout wrapper = new LinearLayout(this);
        wrapper.setOrientation(LinearLayout.VERTICAL);
        wrapper.setGravity(Gravity.CENTER);
        wrapper.setPadding(dp(24), dp(24), dp(24), dp(24));

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setPadding(dp(28), dp(36), dp(28), dp(36));
        card.setBackground(roundRect(COLOR_CARD, dp(24), COLOR_DIVIDER));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            card.setElevation(dp(4));
        }

        TextView icon = new TextView(this);
        icon.setText("📓");
        icon.setTextSize(TypedValue.COMPLEX_UNIT_SP, 48);
        icon.setGravity(Gravity.CENTER);

        TextView title = new TextView(this);
        title.setText("مفكرة أ. شفيقة مزود");
        title.setTextColor(COLOR_TEXT_PRIMARY);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, dp(12), 0, dp(4));
        applyFont(title, true);

        TextView sub = new TextView(this);
        sub.setText("كاتبة وباحثة في التراث الحضرمي");
        sub.setTextColor(COLOR_PRIMARY);
        sub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        sub.setGravity(Gravity.CENTER);
        sub.setPadding(0, 0, 0, dp(12));
        applyFont(sub, true);

        TextView info = new TextView(this);
        info.setText("مرحباً بك. للبدء في حفظ مقالاتك وملفاتك النصية بأمان، يرجى اختيار أو إنشاء مجلد خاص بالمفكرة على هاتفك (يُطلب مرة واحدة فقط).");
        info.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        info.setTextColor(COLOR_TEXT_SECONDARY);
        info.setGravity(Gravity.CENTER);
        info.setLineSpacing(dp(4), 1.15f);
        info.setPadding(dp(8), dp(4), dp(8), dp(28));
        applyFont(info, false);

        Button pick = createPrimaryButton("📁 اختيار مجلد الحفظ", () -> {
            Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                    | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                    | Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
            startActivityForResult(i, REQ_OPEN_TREE);
        });
        pick.setPadding(dp(28), dp(14), dp(28), dp(14));

        card.addView(icon);
        card.addView(title);
        card.addView(sub);
        card.addView(info);
        card.addView(pick);

        wrapper.addView(card, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(wrapper, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_OPEN_TREE && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                final int flags = data.getFlags()
                        & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                try {
                    getContentResolver().takePersistableUriPermission(uri, flags);
                } catch (Exception ignored) {
                }
                treeUri = uri;
                prefs.edit().putString(KEY_TREE_URI, uri.toString()).apply();
                showHome();
            }
        }
    }

    private void showHome() {
        mode = Mode.HOME;
        currentFile = null;
        activeHighlightQuery = "";
        autoSaveHandler.removeCallbacks(autoSaveRunnable);
        root.removeAllViews();

        // البطاقة العلوية الفخمة بنفس تصميم 2.1 المحبوب
        LinearLayout heroHeader = new LinearLayout(this);
        heroHeader.setOrientation(LinearLayout.VERTICAL);
        heroHeader.setBackground(createHeaderGradient());
        heroHeader.setPadding(dp(22), dp(22), dp(22), dp(22));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            heroHeader.setElevation(dp(6));
        }

        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout titleBox = new LinearLayout(this);
        titleBox.setOrientation(LinearLayout.VERTICAL);
        titleBox.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        TextView appTitle = new TextView(this);
        appTitle.setText("مفكرة أ. شفيقة مزود");
        appTitle.setTextColor(Color.WHITE);
        appTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 23);
        applyFont(appTitle, true);

        TextView appSub = new TextView(this);
        appSub.setText("كاتبة وباحثة في التراث الحضرمي");
        appSub.setTextColor(0xFFE0E7FF);
        appSub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
        appSub.setPadding(0, dp(4), 0, 0);
        applyFont(appSub, false);

        titleBox.addView(appTitle);
        titleBox.addView(appSub);

        Button addBtn = new Button(this);
        addBtn.setText("＋ ملف جديد");
        addBtn.setAllCaps(false);
        addBtn.setTextColor(COLOR_PRIMARY_DARK);
        addBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        addBtn.setBackground(roundRect(Color.WHITE, dp(14), Color.WHITE));
        addBtn.setPadding(dp(16), dp(10), dp(16), dp(10));
        applyFont(addBtn, true);
        addBtn.setOnClickListener(v -> createNewFileDialog());

        topRow.addView(titleBox);
        topRow.addView(addBtn);
        heroHeader.addView(topRow);
        root.addView(heroHeader);

        // منطقة المحتوى
        LinearLayout contentArea = new LinearLayout(this);
        contentArea.setOrientation(LinearLayout.VERTICAL);
        contentArea.setPadding(dp(16), dp(14), dp(16), dp(8));
        contentArea.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        LinearLayout sectionRow = new LinearLayout(this);
        sectionRow.setOrientation(LinearLayout.HORIZONTAL);
        sectionRow.setGravity(Gravity.CENTER_VERTICAL);
        sectionRow.setPadding(dp(2), 0, dp(2), dp(10));

        TextView listLabel = new TextView(this);
        listLabel.setText("ملفاتي النصية");
        listLabel.setTextColor(COLOR_TEXT_PRIMARY);
        listLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 19);
        listLabel.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        applyFont(listLabel, true);

        TextView badgeTxt = new TextView(this);
        badgeTxt.setText("TXT");
        badgeTxt.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        badgeTxt.setTextColor(COLOR_PRIMARY);
        badgeTxt.setBackground(roundRect(COLOR_ACCENT_SOFT, dp(10), COLOR_ACCENT_SOFT));
        badgeTxt.setPadding(dp(12), dp(5), dp(12), dp(5));
        applyFont(badgeTxt, true);

        sectionRow.addView(listLabel);
        sectionRow.addView(badgeTxt);
        contentArea.addView(sectionRow);

        // شريط البحث الشامل + زر الفرز
        LinearLayout searchSortRow = new LinearLayout(this);
        searchSortRow.setOrientation(LinearLayout.HORIZONTAL);
        searchSortRow.setGravity(Gravity.CENTER_VERTICAL);
        searchSortRow.setPadding(0, 0, 0, dp(10));

        EditText searchInput = new EditText(this);
        searchInput.setHint("🔍 ابحث في أسماء ومحتوى ملفاتك...");
        searchInput.setHintTextColor(COLOR_TEXT_SECONDARY);
        searchInput.setTextColor(COLOR_TEXT_PRIMARY);
        searchInput.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        searchInput.setSingleLine(true);
        searchInput.setText(searchQuery);
        searchInput.setBackground(roundRect(COLOR_CARD, dp(14), COLOR_DIVIDER));
        searchInput.setPadding(dp(14), dp(10), dp(14), dp(10));
        applyFont(searchInput, false);
        LinearLayout.LayoutParams searchLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        searchLp.leftMargin = dp(8);
        searchInput.setLayoutParams(searchLp);

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                searchQuery = s.toString();
                refreshFiles();
            }
        });

        String sortTitle = (sortField == SortField.DATE ? "⇅ التاريخ" : "⇅ الاسم") + (asc ? " ↑" : " ↓");
        Button sortBtn = createSecondaryButton(sortTitle, () -> {
            if (sortField == SortField.DATE && !asc) {
                asc = true;
            } else if (sortField == SortField.DATE) {
                sortField = SortField.NAME;
                asc = true;
            } else if (sortField == SortField.NAME && asc) {
                asc = false;
            } else {
                sortField = SortField.DATE;
                asc = false;
            }
            showHome();
        });

        searchSortRow.addView(searchInput);
        searchSortRow.addView(sortBtn);
        contentArea.addView(searchSortRow);

        statsView = new TextView(this);
        statsView.setTextColor(COLOR_TEXT_SECONDARY);
        statsView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        statsView.setPadding(dp(4), 0, dp(4), dp(8));
        applyFont(statsView, false);
        contentArea.addView(statsView);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        filesBox = new LinearLayout(this);
        filesBox.setOrientation(LinearLayout.VERTICAL);
        filesBox.setPadding(0, dp(2), 0, dp(16));
        scroll.addView(filesBox);
        contentArea.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        root.addView(contentArea);

        // الشريط السفلي الفخم (زرّان فقط جنباً إلى جنب كما في 2.1: الإعدادات + نسخ احتياطي بلون مميز)
        LinearLayout bottomBar = new LinearLayout(this);
        bottomBar.setOrientation(LinearLayout.HORIZONTAL);
        bottomBar.setGravity(Gravity.CENTER);
        bottomBar.setBackgroundColor(COLOR_CARD);
        bottomBar.setPadding(dp(16), dp(12), dp(16), dp(12));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            bottomBar.setElevation(dp(10));
        }

        Button settingsBtn = createSecondaryButton("⚙ الإعدادات", this::showSettingsDialog);
        settingsBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        settingsBtn.setPadding(dp(14), dp(12), dp(14), dp(12));

        Button backupBtn = createPrimaryButton("📦 نسخ احتياطي (ZIP)", this::backupNow);
        backupBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        backupBtn.setPadding(dp(14), dp(12), dp(14), dp(12));

        LinearLayout.LayoutParams navLp1 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        navLp1.leftMargin = dp(10);
        LinearLayout.LayoutParams navLp2 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);

        bottomBar.addView(settingsBtn, navLp1);
        bottomBar.addView(backupBtn, navLp2);
        root.addView(bottomBar);

        refreshFiles();
    }

    private void refreshFiles() {
        if (filesBox == null) return;
        filesBox.removeAllViews();
        searchSnippets.clear();

        List<DocFile> allList = listTxtFiles(treeUri);
        List<DocFile> list = new ArrayList<>();
        String q = searchQuery == null ? "" : searchQuery.trim().toLowerCase(Locale.ROOT);

        for (DocFile f : allList) {
            if (q.isEmpty()) {
                list.add(f);
            } else {
                boolean titleMatch = displayName(f).toLowerCase(Locale.ROOT).contains(q);
                String content = readText(f);
                String lowerContent = content.toLowerCase(Locale.ROOT);
                int matchIdx = lowerContent.indexOf(q);

                if (titleMatch || matchIdx >= 0) {
                    list.add(f);
                    if (matchIdx >= 0) {
                        int start = Math.max(0, matchIdx - 30);
                        int end = Math.min(content.length(), matchIdx + q.length() + 40);
                        String rawSnippet = content.substring(start, end).replace('\n', ' ').replace('\r', ' ').trim();
                        String prefix = start > 0 ? "..." : "";
                        String suffix = end < content.length() ? "..." : "";
                        searchSnippets.put(f.uri, "📄 " + prefix + rawSnippet + suffix);
                    }
                }
            }
        }

        Collections.sort(list, new Comparator<DocFile>() {
            @Override
            public int compare(DocFile a, DocFile b) {
                int r;
                if (sortField == SortField.DATE) {
                    r = Long.compare(a.modified, b.modified);
                } else {
                    r = displayName(a).compareToIgnoreCase(displayName(b));
                }
                return asc ? r : -r;
            }
        });

        if (statsView != null) {
            if (q.isEmpty()) {
                statsView.setText("عدد الملفات: " + list.size() + " ملف TXT");
            } else {
                statsView.setText("نتائج البحث: " + list.size() + " من أصل " + allList.size());
            }
        }

        if (list.isEmpty()) {
            LinearLayout emptyBox = new LinearLayout(this);
            emptyBox.setOrientation(LinearLayout.VERTICAL);
            emptyBox.setGravity(Gravity.CENTER);
            emptyBox.setPadding(dp(24), dp(40), dp(24), dp(40));
            emptyBox.setBackground(roundRect(COLOR_CARD, dp(18), COLOR_DIVIDER));

            TextView emptyIcon = new TextView(this);
            emptyIcon.setText(q.isEmpty() ? "📝" : "🔍");
            emptyIcon.setTextSize(TypedValue.COMPLEX_UNIT_SP, 40);
            emptyIcon.setGravity(Gravity.CENTER);

            TextView emptyTitle = new TextView(this);
            emptyTitle.setText(q.isEmpty() ? "لا توجد ملفات نصية بعد" : "لا توجد نتائج مطابقة لبحثك");
            emptyTitle.setTextColor(COLOR_TEXT_PRIMARY);
            emptyTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
            emptyTitle.setGravity(Gravity.CENTER);
            emptyTitle.setPadding(0, dp(10), 0, dp(4));
            applyFont(emptyTitle, true);

            TextView emptySub = new TextView(this);
            emptySub.setText(q.isEmpty()
                    ? "اضغط على زر «＋ ملف جديد» في الأعلى لبدء تدوين ملاحظاتك وأفكارك."
                    : "جرّب البحث بكلمة أخرى في عناوين أو نصوص مستنداتك.");
            emptySub.setTextColor(COLOR_TEXT_SECONDARY);
            emptySub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            emptySub.setGravity(Gravity.CENTER);
            applyFont(emptySub, false);

            emptyBox.addView(emptyIcon);
            emptyBox.addView(emptyTitle);
            emptyBox.addView(emptySub);
            filesBox.addView(emptyBox);
            return;
        }

        for (DocFile f : list) {
            filesBox.addView(fileCard(f));
        }
    }

    private View fileCard(DocFile f) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setBackground(roundRect(COLOR_CARD, dp(16), COLOR_DIVIDER));
        card.setPadding(dp(16), dp(14), dp(14), dp(14));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            card.setElevation(dp(2));
        }

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(10));
        card.setLayoutParams(lp);

        LinearLayout textContainer = new LinearLayout(this);
        textContainer.setOrientation(LinearLayout.VERTICAL);
        textContainer.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        TextView name = new TextView(this);
        String dispName = displayName(f);
        String q = searchQuery == null ? "" : searchQuery.trim();
        if (!q.isEmpty()) {
            name.setText(highlightQueryInText(dispName, q));
        } else {
            name.setText(dispName);
        }
        name.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17 * fontScale);
        name.setTextColor(COLOR_TEXT_PRIMARY);
        name.setMaxLines(1);
        name.setEllipsize(TextUtils.TruncateAt.END);
        name.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG_RTL);
        applyFont(name, true);

        TextView date = new TextView(this);
        String dt = new SimpleDateFormat("yyyy/MM/dd  •  HH:mm", Locale.getDefault()).format(new Date(f.modified));
        date.setText("آخر تعديل: " + dt);
        date.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        date.setTextColor(COLOR_TEXT_SECONDARY);
        date.setPadding(0, dp(4), 0, 0);
        applyFont(date, false);

        textContainer.addView(name);
        textContainer.addView(date);

        String snippet = searchSnippets.get(f.uri);
        if (snippet != null && !q.isEmpty()) {
            TextView snippetView = new TextView(this);
            snippetView.setText(highlightQueryInText(snippet, q));
            snippetView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            snippetView.setTextColor(COLOR_TEXT_PRIMARY);
            snippetView.setMaxLines(2);
            snippetView.setEllipsize(TextUtils.TruncateAt.END);
            snippetView.setPadding(dp(10), dp(6), dp(10), dp(6));
            snippetView.setBackground(roundRect(COLOR_ACCENT_SOFT, dp(8), COLOR_ACCENT_SOFT));
            LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            slp.topMargin = dp(6);
            snippetView.setLayoutParams(slp);
            applyFont(snippetView, false);
            textContainer.addView(snippetView);
        }

        textContainer.setOnClickListener(v -> {
            activeHighlightQuery = (searchQuery != null) ? searchQuery.trim() : "";
            read(f);
        });

        // زر الثلاث نقاط بأيقونة فيكتور متوسطة 100% في قلب المربع
        FrameLayout moreBtn = new FrameLayout(this);
        moreBtn.setBackground(roundRect(COLOR_ACCENT_SOFT, dp(12), COLOR_ACCENT_SOFT));
        ImageView dotsIcon = createVectorImageView("ic_more_vert", dp(22), COLOR_PRIMARY);
        FrameLayout.LayoutParams dlp = new FrameLayout.LayoutParams(dp(22), dp(22), Gravity.CENTER);
        moreBtn.addView(dotsIcon, dlp);
        moreBtn.setOnClickListener(v -> showFileActions(f));

        LinearLayout.LayoutParams moreLp = new LinearLayout.LayoutParams(dp(42), dp(42));
        moreLp.rightMargin = dp(10);
        moreBtn.setLayoutParams(moreLp);

        card.addView(textContainer);
        card.addView(moreBtn);
        card.setOnClickListener(v -> {
            activeHighlightQuery = (searchQuery != null) ? searchQuery.trim() : "";
            read(f);
        });
        return card;
    }

    private ImageView createVectorImageView(String drawableName, int sizePx, int tintColor) {
        ImageView iv = new ImageView(this);
        int resId = getResources().getIdentifier(drawableName, "drawable", getPackageName());
        if (resId != 0) {
            iv.setImageResource(resId);
        }
        iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
        iv.setColorFilter(tintColor, PorterDuff.Mode.SRC_IN);
        iv.setLayoutParams(new ViewGroup.LayoutParams(sizePx, sizePx));
        return iv;
    }

    private SpannableString highlightQueryInText(String text, String query) {
        SpannableString span = new SpannableString(text);
        if (query == null || query.isEmpty()) return span;
        String lowerText = text.toLowerCase(Locale.ROOT);
        String lowerQ = query.toLowerCase(Locale.ROOT);
        int idx = 0;
        while ((idx = lowerText.indexOf(lowerQ, idx)) >= 0) {
            int end = idx + lowerQ.length();
            span.setSpan(new BackgroundColorSpan(0xFFFDE047), idx, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            span.setSpan(new ForegroundColorSpan(0xFF111827), idx, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            idx = end;
        }
        return span;
    }

    private void showFileActions(DocFile f) {
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setCustomTitle(createDialogTitleView("📄 " + displayName(f), true));
        String[] items = {
                "📖 فتح للقراءة",
                "✏️ تعديل النص",
                "🏷 إعادة تسمية الملف",
                "📋 إنشاء نسخة مكررة",
                "📤 مشاركة الملف",
                "🗑 حذف الملف نهائياً"
        };
        b.setItems(items, (d, which) -> {
            if (which == 0) {
                activeHighlightQuery = "";
                read(f);
            } else if (which == 1) {
                activeHighlightQuery = "";
                edit(f);
            } else if (which == 2) {
                rename(f);
            } else if (which == 3) {
                duplicate(f);
            } else if (which == 4) {
                shareFile(f);
            } else if (which == 5) {
                confirmDelete(f);
            }
        });
        b.setNegativeButton("إغلاق", null);
        b.show();
    }

    private void shareFile(DocFile f) {
        String content = readText(f);
        Intent sendIntent = new Intent(Intent.ACTION_SEND);
        sendIntent.setType("text/plain");
        sendIntent.putExtra(Intent.EXTRA_SUBJECT, displayName(f));
        sendIntent.putExtra(Intent.EXTRA_TEXT, content);
        startActivity(Intent.createChooser(sendIntent, "مشاركة: " + displayName(f)));
    }

    // الشريط العلوي الفخم والحديث داخل المستند: [ ← رجوع ] [ اسم المستند بمساحة واسعة ] [ + 116% - ]
    private LinearLayout buildDocumentTopBar(DocFile f) {
        LinearLayout docHeader = new LinearLayout(this);
        docHeader.setOrientation(LinearLayout.HORIZONTAL);
        docHeader.setGravity(Gravity.CENTER_VERTICAL);
        docHeader.setBackgroundColor(COLOR_CARD);
        docHeader.setPadding(dp(12), dp(10), dp(12), dp(10));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            docHeader.setElevation(dp(4));
        }

        Button backBtn = new Button(this);
        backBtn.setText("→ رجوع");
        backBtn.setAllCaps(false);
        backBtn.setTextColor(COLOR_PRIMARY_DARK);
        backBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
        backBtn.setBackground(roundRect(COLOR_ACCENT_SOFT, dp(12), COLOR_ACCENT_SOFT));
        backBtn.setPadding(dp(12), dp(6), dp(12), dp(6));
        backBtn.setMinimumWidth(0);
        backBtn.setMinWidth(0);
        backBtn.setMinimumHeight(0);
        backBtn.setMinHeight(0);
        applyFont(backBtn, true);
        backBtn.setOnClickListener(v -> goBack());

        docTitle = new TextView(this);
        docTitle.setText(displayName(f));
        docTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        docTitle.setTextColor(COLOR_TEXT_PRIMARY);
        docTitle.setMaxLines(1);
        docTitle.setEllipsize(TextUtils.TruncateAt.END);
        docTitle.setGravity(Gravity.CENTER_VERTICAL | Gravity.RIGHT);
        docTitle.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG_RTL);
        docTitle.setPadding(dp(12), dp(4), dp(10), dp(4));
        docTitle.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        applyFont(docTitle, true);
        docTitle.setOnClickListener(v -> rename(currentFile != null ? currentFile : f));

        LinearLayout zoomCapsule = new LinearLayout(this);
        zoomCapsule.setOrientation(LinearLayout.HORIZONTAL);
        zoomCapsule.setGravity(Gravity.CENTER_VERTICAL);
        zoomCapsule.setBackground(roundRect(COLOR_BG, dp(12), COLOR_DIVIDER));
        zoomCapsule.setPadding(dp(2), dp(2), dp(2), dp(2));

        TextView plusBtn = new TextView(this);
        plusBtn.setText("＋");
        plusBtn.setTextColor(COLOR_PRIMARY);
        plusBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        plusBtn.setGravity(Gravity.CENTER);
        plusBtn.setPadding(dp(8), dp(4), dp(8), dp(4));
        plusBtn.setOnClickListener(v -> adjustZoomByStep(0.05f));

        docZoomLabel = new TextView(this);
        docZoomLabel.setText(Math.round(fontScale * 100) + "%");
        docZoomLabel.setTextColor(COLOR_PRIMARY_DARK);
        docZoomLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
        docZoomLabel.setGravity(Gravity.CENTER);
        docZoomLabel.setPadding(dp(4), dp(4), dp(4), dp(4));
        applyFont(docZoomLabel, true);
        docZoomLabel.setOnClickListener(v -> {
            fontScale = 1.0f;
            prefs.edit().putFloat("font_scale", fontScale).apply();
            applyCurrentZoomToDocument();
            toast("تمت استعادة حجم الخط الافتراضي 100%");
        });

        TextView minusBtn = new TextView(this);
        minusBtn.setText("－");
        minusBtn.setTextColor(COLOR_PRIMARY);
        minusBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        minusBtn.setGravity(Gravity.CENTER);
        minusBtn.setPadding(dp(8), dp(4), dp(8), dp(4));
        minusBtn.setOnClickListener(v -> adjustZoomByStep(-0.05f));

        zoomCapsule.addView(plusBtn);
        zoomCapsule.addView(docZoomLabel);
        zoomCapsule.addView(minusBtn);

        docHeader.addView(backBtn);
        docHeader.addView(docTitle);
        docHeader.addView(zoomCapsule);
        return docHeader;
    }

    private void adjustZoomByStep(float delta) {
        float next = Math.round((fontScale + delta) * 20f) / 20f;
        if (next < 0.60f) next = 0.60f;
        if (next > 2.00f) next = 2.00f;
        fontScale = next;
        prefs.edit().putFloat("font_scale", fontScale).apply();
        applyCurrentZoomToDocument();
    }

    private void applyCurrentZoomToDocument() {
        if (docZoomLabel != null) {
            docZoomLabel.setText(Math.round(fontScale * 100) + "%");
        }
        if (readView != null) {
            readView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 21f * fontScale);
        }
        if (editor != null) {
            editor.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f * fontScale);
        }
    }

    private void read(DocFile f) {
        mode = Mode.READ;
        currentFile = f;
        autoSaveHandler.removeCallbacks(autoSaveRunnable);
        hideKeyboard();
        root.removeAllViews();

        root.addView(buildDocumentTopBar(f));

        View topDivider = new View(this);
        topDivider.setBackgroundColor(COLOR_DIVIDER);
        root.addView(topDivider, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1)));

        String content = readText(f);

        readScrollView = new ScrollView(this);
        readScrollView.setFillViewport(true);
        readScrollView.setBackgroundColor(COLOR_CARD);

        readView = new TextView(this);
        int firstMatchPos = -1;
        if (content.isEmpty()) {
            readView.setText("هذا الملف فارغ حالياً.\n\nاضغط هنا أو على زر «تحرير الملف» بالأسفل للبدء في الكتابة...");
            readView.setTextColor(COLOR_TEXT_SECONDARY);
        } else if (activeHighlightQuery != null && !activeHighlightQuery.isEmpty()) {
            firstMatchPos = content.toLowerCase(Locale.ROOT).indexOf(activeHighlightQuery.toLowerCase(Locale.ROOT));
            readView.setText(highlightQueryInText(content, activeHighlightQuery));
            readView.setTextColor(COLOR_TEXT_PRIMARY);
        } else {
            readView.setText(content);
            readView.setTextColor(COLOR_TEXT_PRIMARY);
        }

        readView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 21f * fontScale);
        readView.setPadding(dp(22), dp(22), dp(22), dp(40));
        readView.setLineSpacing(dp(6), 1.25f);
        readView.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG_RTL);
        applyFont(readView, false);
        readView.setOnClickListener(v -> edit(f));

        readScrollView.addView(readView);
        root.addView(readScrollView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        // شريط الإحصائيات السفلي + زرا تحرير الملف ومشاركة الملف (كما في 2.1 بالضبط)
        View bottomDivider = new View(this);
        bottomDivider.setBackgroundColor(COLOR_DIVIDER);
        root.addView(bottomDivider, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1)));

        TextView statsBar = new TextView(this);
        statsBar.setText(computeTextStats(content));
        statsBar.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
        statsBar.setTextColor(COLOR_TEXT_SECONDARY);
        statsBar.setBackgroundColor(COLOR_BG);
        statsBar.setGravity(Gravity.CENTER);
        statsBar.setPadding(dp(16), dp(8), dp(16), dp(8));
        applyFont(statsBar, false);
        root.addView(statsBar);

        LinearLayout readBottomBar = new LinearLayout(this);
        readBottomBar.setOrientation(LinearLayout.HORIZONTAL);
        readBottomBar.setGravity(Gravity.CENTER);
        readBottomBar.setBackgroundColor(COLOR_CARD);
        readBottomBar.setPadding(dp(16), dp(10), dp(16), dp(12));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            readBottomBar.setElevation(dp(8));
        }

        Button editBtn = createPrimaryButton("✏️ تحرير الملف", () -> edit(f));
        editBtn.setPadding(dp(14), dp(12), dp(14), dp(12));

        Button shareBtn = createSecondaryButton("📤 مشاركة الملف", () -> shareFile(f));
        shareBtn.setPadding(dp(14), dp(12), dp(14), dp(12));

        LinearLayout.LayoutParams eLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.2f);
        eLp.leftMargin = dp(10);
        LinearLayout.LayoutParams sLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);

        readBottomBar.addView(editBtn, eLp);
        readBottomBar.addView(shareBtn, sLp);
        root.addView(readBottomBar);

        if (firstMatchPos >= 0) {
            final int targetOffset = firstMatchPos;
            readView.post(() -> {
                try {
                    if (readView.getLayout() != null) {
                        int safeOffset = Math.min(targetOffset, readView.getText().length());
                        int line = readView.getLayout().getLineForOffset(safeOffset);
                        int y = readView.getLayout().getLineTop(line);
                        readScrollView.smoothScrollTo(0, Math.max(0, y - dp(80)));
                    }
                } catch (Exception ignored) {
                }
            });
        }
    }

    private void edit(DocFile f) {
        mode = Mode.EDIT;
        currentFile = f;
        root.removeAllViews();

        root.addView(buildDocumentTopBar(f));

        View topDivider = new View(this);
        topDivider.setBackgroundColor(COLOR_DIVIDER);
        root.addView(topDivider, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1)));

        // شريط أدوات التحرير مرتباً من اليمين إلى اليسار حسب ترتيب المستخدم
        if (showToolbar && !enabledToolIds.isEmpty()) {
            final HorizontalScrollView toolsScroll = new HorizontalScrollView(this);
            toolsScroll.setHorizontalScrollBarEnabled(false);
            toolsScroll.setBackgroundColor(COLOR_BG);
            toolsScroll.setPadding(dp(10), dp(8), dp(10), dp(8));
            toolsScroll.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

            LinearLayout tools = new LinearLayout(this);
            tools.setOrientation(LinearLayout.HORIZONTAL);
            tools.setGravity(Gravity.CENTER_VERTICAL | Gravity.RIGHT);
            tools.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

            for (String toolId : orderedToolIds) {
                if (!enabledToolIds.contains(toolId)) continue;
                tools.addView(createToolbarActionView(toolId));
            }
            toolsScroll.addView(tools);
            toolsScroll.post(() -> toolsScroll.fullScroll(View.FOCUS_RIGHT));
            root.addView(toolsScroll);

            View divider = new View(this);
            divider.setBackgroundColor(COLOR_DIVIDER);
            root.addView(divider, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1)));
        }

        editor = new EditText(this);
        lastText = readText(f);
        undo.clear();
        redo.clear();
        internalChange = true;
        editor.setText(lastText);
        internalChange = false;

        editor.setGravity(Gravity.TOP | Gravity.RIGHT);
        editor.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG_RTL);
        editor.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f * fontScale);
        editor.setTextColor(COLOR_TEXT_PRIMARY);
        editor.setHint("ابدأ بكتابة أفكارك هنا...");
        editor.setHintTextColor(COLOR_TEXT_SECONDARY);
        editor.setBackgroundColor(COLOR_CARD);
        editor.setPadding(dp(20), dp(20), dp(20), dp(36));
        editor.setLineSpacing(dp(6), 1.25f);
        applyFont(editor, false);

        root.addView(editor, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        // شريط الإحصائيات الحية في الأسفل كما في 2.1
        View bottomDivider = new View(this);
        bottomDivider.setBackgroundColor(COLOR_DIVIDER);
        root.addView(bottomDivider, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1)));

        final TextView liveStats = new TextView(this);
        liveStats.setBackgroundColor(COLOR_BG);
        liveStats.setTextColor(COLOR_TEXT_SECONDARY);
        liveStats.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
        liveStats.setGravity(Gravity.CENTER);
        liveStats.setPadding(dp(16), dp(8), dp(16), dp(8));
        liveStats.setText(computeTextStats(lastText));
        applyFont(liveStats, false);
        root.addView(liveStats);

        if (activeHighlightQuery != null && !activeHighlightQuery.isEmpty()) {
            int matchPos = lastText.toLowerCase(Locale.ROOT).indexOf(activeHighlightQuery.toLowerCase(Locale.ROOT));
            if (matchPos >= 0) {
                final int start = matchPos;
                final int end = Math.min(lastText.length(), matchPos + activeHighlightQuery.length());
                editor.post(() -> {
                    try {
                        editor.requestFocus();
                        editor.setSelection(start, end);
                    } catch (Exception ignored) {
                    }
                });
            }
        }

        editor.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                if (internalChange) return;
                undo.push(lastText);
                if (undo.size() > 100) undo.remove(0);
                redo.clear();
                lastText = s.toString();
                liveStats.setText(computeTextStats(lastText));

                autoSaveHandler.removeCallbacks(autoSaveRunnable);
                autoSaveHandler.postDelayed(autoSaveRunnable, 700);
            }
        });
    }

    private View createToolbarActionView(final String toolId) {
        final String label = getToolLabel(toolId);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(dp(3), 0, dp(3), 0);

        if (toolbarIconsOnly) {
            FrameLayout box = new FrameLayout(this);
            box.setBackground(roundRect(COLOR_CARD, dp(10), COLOR_DIVIDER));
            box.setPadding(dp(11), dp(8), dp(11), dp(8));
            box.setLayoutParams(lp);

            ImageView icon = createVectorImageView(getToolIconResName(toolId), dp(20), COLOR_PRIMARY);
            FrameLayout.LayoutParams ilp = new FrameLayout.LayoutParams(dp(20), dp(20), Gravity.CENTER);
            box.addView(icon, ilp);

            box.setOnClickListener(v -> executeTool(toolId));
            box.setOnLongClickListener(v -> {
                toast(label);
                return true;
            });
            return box;
        } else {
            Button b = new Button(this);
            b.setText(label);
            b.setAllCaps(false);
            b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            b.setTextColor(COLOR_TEXT_PRIMARY);
            b.setBackground(roundRect(COLOR_CARD, dp(10), COLOR_DIVIDER));
            b.setPadding(dp(13), dp(7), dp(13), dp(7));
            b.setMinimumWidth(0);
            b.setMinWidth(0);
            b.setMinimumHeight(0);
            b.setMinHeight(0);
            applyFont(b, true);
            b.setLayoutParams(lp);
            b.setOnClickListener(v -> executeTool(toolId));
            return b;
        }
    }

    private void executeTool(String toolId) {
        if (editor == null) return;

        if ("undo".equals(toolId)) {
            if (!undo.isEmpty()) {
                internalChange = true;
                redo.push(editor.getText().toString());
                String prev = undo.pop();
                lastText = prev;
                editor.setText(prev);
                editor.setSelection(prev.length());
                internalChange = false;
            } else {
                toast("لا توجد خطوة سابقة للتراجع");
            }
        } else if ("redo".equals(toolId)) {
            if (!redo.isEmpty()) {
                internalChange = true;
                undo.push(editor.getText().toString());
                String next = redo.pop();
                lastText = next;
                editor.setText(next);
                editor.setSelection(next.length());
                internalChange = false;
            } else {
                toast("لا توجد خطوة للإعادة");
            }
        } else if ("auto_format".equals(toolId)) {
            autoFormatDocument();
        } else if ("signature".equals(toolId)) {
            insertSignature();
        } else if ("select_up".equals(toolId)) {
            selectParagraphAndAbove();
        } else if ("select_down".equals(toolId)) {
            selectParagraphAndBelow();
        } else if ("select_all".equals(toolId)) {
            editor.requestFocus();
            editor.selectAll();
        } else if ("copy".equals(toolId)) {
            int s = Math.max(0, editor.getSelectionStart());
            int e = Math.max(0, editor.getSelectionEnd());
            int min = Math.min(s, e);
            int max = Math.max(s, e);
            String sub = (min != max)
                    ? editor.getText().subSequence(min, max).toString()
                    : editor.getText().toString();
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null) {
                cm.setPrimaryClip(ClipData.newPlainText("text", sub));
                toast(min != max ? "تم نسخ النص المحدد" : "تم نسخ كامل النص");
            }
        } else if ("cut".equals(toolId)) {
            int s = Math.max(0, editor.getSelectionStart());
            int e = Math.max(0, editor.getSelectionEnd());
            int min = Math.min(s, e);
            int max = Math.max(s, e);
            if (min == max) {
                toast("حدد نصاً أولاً لقصه");
                return;
            }
            String sub = editor.getText().subSequence(min, max).toString();
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null) {
                cm.setPrimaryClip(ClipData.newPlainText("text", sub));
            }
            editor.getText().delete(min, max);
            toast("تم قص النص المحدد");
        } else if ("paste".equals(toolId)) {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null && cm.hasPrimaryClip() && cm.getPrimaryClip() != null && cm.getPrimaryClip().getItemCount() > 0) {
                CharSequence paste = cm.getPrimaryClip().getItemAt(0).coerceToText(this);
                if (paste != null) {
                    int s = Math.max(0, editor.getSelectionStart());
                    int e = Math.max(0, editor.getSelectionEnd());
                    editor.getText().replace(Math.min(s, e), Math.max(s, e), paste);
                }
            } else {
                toast("الحافظة فارغة");
            }
        } else if ("datetime".equals(toolId)) {
            String now = new SimpleDateFormat("yyyy/MM/dd - HH:mm", Locale.getDefault()).format(new Date());
            int s = Math.max(0, editor.getSelectionStart());
            int e = Math.max(0, editor.getSelectionEnd());
            editor.getText().replace(Math.min(s, e), Math.max(s, e), now);
        }
    }

    private void insertSignature() {
        if (editor == null) return;
        String sig = signatureText == null ? "" : signatureText.trim();
        if (sig.isEmpty()) {
            AlertDialog.Builder b = new AlertDialog.Builder(this);
            b.setCustomTitle(createDialogTitleView("✒ إعداد التوقيع", false));
            final EditText input = new EditText(this);
            input.setHint(SIGNATURE_HINT);
            input.setTextColor(COLOR_TEXT_PRIMARY);
            input.setHintTextColor(COLOR_TEXT_SECONDARY);
            input.setBackground(roundRect(COLOR_BG, dp(12), COLOR_DIVIDER));
            input.setPadding(dp(16), dp(14), dp(16), dp(14));
            applyFont(input, false);

            LinearLayout box = new LinearLayout(this);
            box.setPadding(dp(20), dp(10), dp(20), dp(6));
            box.addView(input, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            b.setView(box);
            b.setPositiveButton("حفظ وإدراج", (d, w) -> {
                String newSig = input.getText().toString().trim();
                if (!newSig.isEmpty()) {
                    signatureText = newSig;
                    prefs.edit().putString("custom_signature", signatureText).apply();
                    insertSignature();
                }
            });
            b.setNegativeButton("إلغاء", null);
            b.show();
            return;
        }
        int s = Math.max(0, editor.getSelectionStart());
        int e = Math.max(0, editor.getSelectionEnd());
        int pos = Math.min(s, e);
        String current = editor.getText().toString();
        String prefix = (pos > 0 && current.charAt(pos - 1) != '\n') ? "\n" : "";
        editor.getText().replace(pos, Math.max(s, e), prefix + sig);
        toast("تم إدراج التوقيع");
    }

    private void selectParagraphAndAbove() {
        if (editor == null) return;
        String text = editor.getText().toString();
        if (text.isEmpty()) {
            toast("المستند فارغ");
            return;
        }
        int selEnd = Math.max(0, Math.max(editor.getSelectionStart(), editor.getSelectionEnd()));
        int anchor = Math.min(selEnd, text.length());
        int paraEnd = text.indexOf('\n', anchor);
        if (paraEnd == -1) paraEnd = text.length();
        editor.requestFocus();
        editor.setSelection(0, paraEnd);
        toast("تم تحديد الفقرة الحالية وما فوقها");
    }

    private void selectParagraphAndBelow() {
        if (editor == null) return;
        String text = editor.getText().toString();
        if (text.isEmpty()) {
            toast("المستند فارغ");
            return;
        }
        int selStart = Math.max(0, Math.min(editor.getSelectionStart(), editor.getSelectionEnd()));
        int anchor = Math.min(selStart, text.length());
        int paraStart = 0;
        if (anchor > 0) {
            int prevNewline = text.lastIndexOf('\n', anchor - 1);
            paraStart = (prevNewline == -1) ? 0 : prevNewline + 1;
        }
        editor.requestFocus();
        editor.setSelection(paraStart, text.length());
        toast("تم تحديد الفقرة الحالية وما تحتها");
    }

    private void autoFormatDocument() {
        if (editor == null) return;
        String original = editor.getText().toString();
        if (original.trim().isEmpty()) {
            toast("النص فارغ");
            return;
        }

        String text = original.replace("\r\n", "\n").replace("\r", "\n");
        String[] rawLines = text.split("\n", -1);
        StringBuilder sb = new StringBuilder();
        int emptyCount = 0;

        for (String rawLine : rawLines) {
            String line = rawLine.replaceAll("[ \\t\\xA0\\u200B]+", " ").trim();
            line = line.replaceAll(" +([،؛؟:.!,\\)\\]»])", "$1");
            line = line.replaceAll("([(\\[«]) +", "$1");
            line = line.replaceAll("([،؛؟:!])([^ \\n\\)\\]».,،؛؟:!])", "$1 $2");
            line = line.replaceAll("(\\.)([\\p{L}])", "$1 $2");

            if (line.isEmpty()) {
                emptyCount++;
                if (emptyCount <= 1 && sb.length() > 0) {
                    sb.append("\n");
                }
            } else {
                emptyCount = 0;
                if (sb.length() > 0) {
                    sb.append("\n");
                }
                sb.append(line);
            }
        }

        String formatted = sb.toString().trim();
        if (formatted.equals(original)) {
            toast("النص منسّق ومرتب بالفعل ✨");
            return;
        }

        undo.push(original);
        redo.clear();
        internalChange = true;
        lastText = formatted;
        editor.setText(formatted);
        editor.setSelection(formatted.length());
        internalChange = false;
        saveNow(false);
        toast("تم تنسيق النص وترتيب المسافات والفقرات ✨");
    }

    private String computeTextStats(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "الكلمات: 0   •   الحروف: 0   •   الأسطر: 0";
        }
        String trimmed = text.trim();
        int words = trimmed.split("\\s+").length;
        int chars = text.length();
        int lines = text.split("\r\n|\r|\n").length;
        return "الكلمات: " + words + "   •   الحروف: " + chars + "   •   الأسطر: " + lines;
    }

    private void saveNow(boolean showConfirmation) {
        if (currentFile == null || editor == null) return;
        writeText(currentFile.uri, editor.getText().toString());
        currentFile.modified = System.currentTimeMillis();
        if (showConfirmation) {
            toast("تم حفظ التعديلات بنجاح ✓");
        }
    }

    private void showSettingsDialog() {
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setCustomTitle(createDialogTitleView("إعدادات القراءة والكتابة", false));

        ScrollView scroll = new ScrollView(this);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20), dp(14), dp(20), dp(14));
        box.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        // 1. حجم الخط
        final int[] tempScalePercent = {Math.round(fontScale * 100)};
        TextView sizeTitle = new TextView(this);
        sizeTitle.setText("حجم الخط: " + tempScalePercent[0] + "%");
        sizeTitle.setTextColor(COLOR_TEXT_PRIMARY);
        sizeTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        sizeTitle.setGravity(Gravity.RIGHT);
        sizeTitle.setPadding(0, dp(2), 0, dp(4));
        applyFont(sizeTitle, true);

        SeekBar seek = new SeekBar(this);
        seek.setMax(140); // 60% .. 200%
        seek.setProgress(Math.round((fontScale - 0.6f) * 100));
        seek.setPadding(0, dp(6), 0, dp(16));
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tempScalePercent[0] = 60 + progress;
                sizeTitle.setText("حجم الخط: " + tempScalePercent[0] + "%");
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // 2. نوع الخط (بدون صندوق المعاينة الزائد، ومع حل مشكلة الخط الأميري)
        TextView fontTitle = new TextView(this);
        fontTitle.setText("نوع الخط المستخدم");
        fontTitle.setTextColor(COLOR_TEXT_PRIMARY);
        fontTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        fontTitle.setGravity(Gravity.RIGHT);
        fontTitle.setPadding(0, dp(4), 0, dp(6));
        applyFont(fontTitle, true);

        Spinner fontSpinner = new Spinner(this);
        ArrayAdapter<String> fontAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, FONT_NAMES) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                return createFontSpinnerItemView(position, false);
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                return createFontSpinnerItemView(position, true);
            }
        };
        fontSpinner.setAdapter(fontAdapter);
        fontSpinner.setSelection(fontChoice);
        fontSpinner.setBackground(roundRect(COLOR_BG, dp(12), COLOR_DIVIDER));
        fontSpinner.setPadding(dp(8), dp(4), dp(8), dp(4));
        LinearLayout.LayoutParams fontSpinLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        fontSpinLp.bottomMargin = dp(14);
        fontSpinner.setLayoutParams(fontSpinLp);

        // 3. ثيم الألوان
        TextView themeTitle = new TextView(this);
        themeTitle.setText("ثيم وألوان التطبيق");
        themeTitle.setTextColor(COLOR_TEXT_PRIMARY);
        themeTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        themeTitle.setGravity(Gravity.RIGHT);
        themeTitle.setPadding(0, dp(4), 0, dp(6));
        applyFont(themeTitle, true);

        Spinner themeSpinner = new Spinner(this);
        ArrayAdapter<String> themeAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, THEME_NAMES) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View v = super.getView(position, convertView, parent);
                if (v instanceof TextView) {
                    TextView tv = (TextView) v;
                    tv.setTextColor(THEME_COLORS[position][0]);
                    tv.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
                    applyFont(tv, true);
                }
                return v;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View v = super.getDropDownView(position, convertView, parent);
                if (v instanceof TextView) {
                    TextView tv = (TextView) v;
                    tv.setText("●  " + THEME_NAMES[position]);
                    tv.setTextColor(THEME_COLORS[position][0]);
                    tv.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
                    tv.setPadding(dp(16), dp(12), dp(16), dp(12));
                    applyFont(tv, true);
                }
                return v;
            }
        };
        themeSpinner.setAdapter(themeAdapter);
        themeSpinner.setSelection(themeChoice);
        themeSpinner.setBackground(roundRect(COLOR_BG, dp(12), COLOR_DIVIDER));
        themeSpinner.setPadding(dp(8), dp(6), dp(8), dp(6));

        // 4. شريط أدوات التحرير + شكل العرض (نصوص / رموز) + التخصيص
        LinearLayout toolbarSwitchRow = new LinearLayout(this);
        toolbarSwitchRow.setOrientation(LinearLayout.HORIZONTAL);
        toolbarSwitchRow.setGravity(Gravity.CENTER_VERTICAL);
        toolbarSwitchRow.setPadding(0, dp(16), 0, dp(8));

        TextView toolbarLabel = new TextView(this);
        toolbarLabel.setText("شريط أدوات التحرير");
        toolbarLabel.setTextColor(COLOR_TEXT_PRIMARY);
        toolbarLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        toolbarLabel.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        toolbarLabel.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        applyFont(toolbarLabel, true);

        final Switch toolbarSwitch = new Switch(this);
        toolbarSwitch.setChecked(showToolbar);

        toolbarSwitchRow.addView(toolbarLabel);
        toolbarSwitchRow.addView(toolbarSwitch);

        // أزرار تبديل أنيقة بين (نصوص) و (رموز)
        final boolean[] tempIconsOnly = {toolbarIconsOnly};
        LinearLayout styleToggleRow = new LinearLayout(this);
        styleToggleRow.setOrientation(LinearLayout.HORIZONTAL);
        styleToggleRow.setBackground(roundRect(COLOR_BG, dp(12), COLOR_DIVIDER));
        styleToggleRow.setPadding(dp(4), dp(4), dp(4), dp(4));
        LinearLayout.LayoutParams stLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        stLp.bottomMargin = dp(10);
        styleToggleRow.setLayoutParams(stLp);

        final TextView tabTexts = new TextView(this);
        tabTexts.setText("عرض كنصوص (قص، نسخ)");
        tabTexts.setGravity(Gravity.CENTER);
        tabTexts.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tabTexts.setPadding(dp(8), dp(9), dp(8), dp(9));
        tabTexts.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        final TextView tabIcons = new TextView(this);
        tabIcons.setText("عرض كرموز (أيقونات)");
        tabIcons.setGravity(Gravity.CENTER);
        tabIcons.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tabIcons.setPadding(dp(8), dp(9), dp(8), dp(9));
        tabIcons.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        final Runnable updateStyleTabs = () -> {
            if (!tempIconsOnly[0]) {
                tabTexts.setBackground(roundRect(COLOR_PRIMARY, dp(9), COLOR_PRIMARY));
                tabTexts.setTextColor(Color.WHITE);
                applyFont(tabTexts, true);
                tabIcons.setBackgroundColor(Color.TRANSPARENT);
                tabIcons.setTextColor(COLOR_TEXT_SECONDARY);
                applyFont(tabIcons, false);
            } else {
                tabIcons.setBackground(roundRect(COLOR_PRIMARY, dp(9), COLOR_PRIMARY));
                tabIcons.setTextColor(Color.WHITE);
                applyFont(tabIcons, true);
                tabTexts.setBackgroundColor(Color.TRANSPARENT);
                tabTexts.setTextColor(COLOR_TEXT_SECONDARY);
                applyFont(tabTexts, false);
            }
        };
        updateStyleTabs.run();

        tabTexts.setOnClickListener(v -> {
            tempIconsOnly[0] = false;
            updateStyleTabs.run();
        });
        tabIcons.setOnClickListener(v -> {
            tempIconsOnly[0] = true;
            updateStyleTabs.run();
        });

        styleToggleRow.addView(tabTexts);
        styleToggleRow.addView(tabIcons);

        Button customizeToolsBtn = createSecondaryButton("🛠 تخصيص وترتيب أدوات التحرير",
                this::showToolbarCustomizationDialog);
        LinearLayout.LayoutParams custLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        custLp.bottomMargin = dp(14);
        customizeToolsBtn.setLayoutParams(custLp);

        // 5. التوقيع الشخصي
        TextView sigTitle = new TextView(this);
        sigTitle.setText("التوقيع الشخصي (يُدرج بزر التوقيع)");
        sigTitle.setTextColor(COLOR_TEXT_PRIMARY);
        sigTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        sigTitle.setGravity(Gravity.RIGHT);
        sigTitle.setPadding(0, dp(4), 0, dp(6));
        applyFont(sigTitle, true);

        final EditText sigInput = new EditText(this);
        sigInput.setText(signatureText);
        sigInput.setHint(SIGNATURE_HINT);
        sigInput.setTextColor(COLOR_TEXT_PRIMARY);
        sigInput.setHintTextColor(COLOR_TEXT_SECONDARY);
        sigInput.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        sigInput.setMinLines(2);
        sigInput.setMaxLines(3);
        sigInput.setGravity(Gravity.RIGHT | Gravity.TOP);
        sigInput.setBackground(roundRect(COLOR_BG, dp(12), COLOR_DIVIDER));
        sigInput.setPadding(dp(14), dp(10), dp(14), dp(10));
        applyFont(sigInput, false);

        // 6. زر تغيير المجلد
        Button changeFolderBtn = createSecondaryButton("📁 تغيير مجلد حفظ الملفات", null);
        LinearLayout.LayoutParams folderLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        folderLp.topMargin = dp(16);
        folderLp.bottomMargin = dp(6);
        changeFolderBtn.setLayoutParams(folderLp);

        box.addView(sizeTitle);
        box.addView(seek);
        box.addView(fontTitle);
        box.addView(fontSpinner);
        box.addView(themeTitle);
        box.addView(themeSpinner);
        box.addView(toolbarSwitchRow);
        box.addView(styleToggleRow);
        box.addView(customizeToolsBtn);
        box.addView(sigTitle);
        box.addView(sigInput);
        box.addView(changeFolderBtn);

        // أزرار الحفظ والإلغاء
        LinearLayout actionsRow = new LinearLayout(this);
        actionsRow.setOrientation(LinearLayout.HORIZONTAL);
        actionsRow.setGravity(Gravity.CENTER_VERTICAL);
        actionsRow.setPadding(0, dp(16), 0, dp(4));

        Button saveSettingsBtn = createPrimaryButton("حفظ التغييرات", null);
        LinearLayout.LayoutParams saveBtnLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.3f);
        saveBtnLp.leftMargin = dp(8);
        saveSettingsBtn.setLayoutParams(saveBtnLp);

        Button cancelSettingsBtn = createSecondaryButton("إلغاء", null);
        LinearLayout.LayoutParams cancelBtnLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.9f);
        cancelSettingsBtn.setLayoutParams(cancelBtnLp);

        actionsRow.addView(saveSettingsBtn);
        actionsRow.addView(cancelSettingsBtn);
        box.addView(actionsRow);

        scroll.addView(box);
        b.setView(scroll);

        final AlertDialog dialog = b.create();

        changeFolderBtn.setOnClickListener(v -> {
            dialog.dismiss();
            askForFolder();
        });

        cancelSettingsBtn.setOnClickListener(v -> dialog.dismiss());

        saveSettingsBtn.setOnClickListener(v -> {
            fontScale = tempScalePercent[0] / 100f;
            fontChoice = fontSpinner.getSelectedItemPosition();
            themeChoice = themeSpinner.getSelectedItemPosition();
            showToolbar = toolbarSwitch.isChecked();
            toolbarIconsOnly = tempIconsOnly[0];
            signatureText = sigInput.getText().toString().trim();

            prefs.edit()
                    .putFloat("font_scale", fontScale)
                    .putInt("font_choice", fontChoice)
                    .putInt("theme_choice", themeChoice)
                    .putBoolean("show_toolbar", showToolbar)
                    .putBoolean("toolbar_icons_only", toolbarIconsOnly)
                    .putString("custom_signature", signatureText)
                    .putBoolean("sig_init_v3", true)
                    .apply();

            applyThemeColors();
            dialog.dismiss();
            if (mode == Mode.HOME) showHome();
            else if (mode == Mode.READ && currentFile != null) read(currentFile);
            else if (mode == Mode.EDIT && currentFile != null) edit(currentFile);
        });

        dialog.show();
    }

    private TextView createFontSpinnerItemView(int position, boolean isDropDown) {
        TextView tv = new TextView(this);
        tv.setText(FONT_NAMES[position]);
        tv.setTextColor(COLOR_TEXT_PRIMARY);
        tv.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        tv.setTextDirection(View.TEXT_DIRECTION_RTL);
        tv.setSingleLine(false);
        tv.setMaxLines(2);
        tv.setIncludeFontPadding(true);
        float size = (position == 2) ? 16.5f : 17.0f;
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, size);
        tv.setTypeface(getFontByIndex(position, false));
        int vPad = isDropDown ? dp(14) : dp(10);
        tv.setPadding(dp(14), vPad, dp(14), vPad);
        tv.setLayoutParams(new AbsListView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        if (isDropDown) {
            tv.setBackgroundColor(Color.WHITE);
        }
        return tv;
    }

    // نافذة تخصيص وترتيب الأدوات بنظام سحب وإفلات مستقر 100% ومظهر أنيق
    private void showToolbarCustomizationDialog() {
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setCustomTitle(createDialogTitleView("🛠 تخصيص وترتيب أدوات التحرير", false));

        LinearLayout outer = new LinearLayout(this);
        outer.setOrientation(LinearLayout.VERTICAL);
        outer.setPadding(dp(16), dp(10), dp(16), dp(10));
        outer.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        TextView hint = new TextView(this);
        hint.setText("اسحب أي أداة من مقبض (☰) لإعادة ترتيبها، وستظهر في شريط الكتابة من اليمين لليسار بنفس هذا الترتيب:");
        hint.setTextColor(COLOR_TEXT_SECONDARY);
        hint.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
        hint.setGravity(Gravity.RIGHT);
        hint.setPadding(dp(4), 0, dp(4), dp(10));
        applyFont(hint, false);
        outer.addView(hint);

        final List<String> tempOrder = new ArrayList<>(orderedToolIds);
        final Set<String> tempEnabled = new HashSet<>(enabledToolIds);
        final Map<String, CheckBox> checkBoxesMap = new HashMap<>();
        final boolean[] internalSync = {false};

        // شريط علوي أنيق: تحديد الكل + استعادة الافتراضي
        LinearLayout topControls = new LinearLayout(this);
        topControls.setOrientation(LinearLayout.HORIZONTAL);
        topControls.setGravity(Gravity.CENTER_VERTICAL);
        topControls.setBackground(roundRect(COLOR_ACCENT_SOFT, dp(12), COLOR_DIVIDER));
        topControls.setPadding(dp(12), dp(6), dp(10), dp(6));

        final CheckBox allCheck = new CheckBox(this);
        allCheck.setText("تحديد / إلغاء تحديد الكل");
        allCheck.setTextColor(COLOR_PRIMARY_DARK);
        allCheck.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
        allCheck.setChecked(tempEnabled.size() == TOOL_IDS.length);
        allCheck.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        applyFont(allCheck, true);

        Button resetDefaultBtn = createSecondaryButton("↺ الافتراضي", null);
        resetDefaultBtn.setPadding(dp(12), dp(4), dp(12), dp(4));

        topControls.addView(allCheck);
        topControls.addView(resetDefaultBtn);

        LinearLayout.LayoutParams topLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        topLp.bottomMargin = dp(10);
        outer.addView(topControls, topLp);

        final ScrollView scroll = new ScrollView(this);
        final LinearLayout listContainer = new LinearLayout(this);
        listContainer.setOrientation(LinearLayout.VERTICAL);
        listContainer.setPadding(0, dp(2), 0, dp(6));
        scroll.addView(listContainer);

        final int rowHeightPx = dp(52);
        final int rowMarginPx = dp(7);
        final int slotPx = rowHeightPx + rowMarginPx;

        final Runnable buildList = new Runnable() {
            @Override
            public void run() {
                listContainer.removeAllViews();
                checkBoxesMap.clear();

                for (int i = 0; i < tempOrder.size(); i++) {
                    final String toolId = tempOrder.get(i);

                    final LinearLayout row = new LinearLayout(MainActivity.this);
                    row.setOrientation(LinearLayout.HORIZONTAL);
                    row.setGravity(Gravity.CENTER_VERTICAL);
                    row.setBackground(roundRect(COLOR_CARD, dp(12), COLOR_DIVIDER));
                    row.setPadding(dp(10), 0, dp(10), 0);

                    LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, rowHeightPx);
                    rlp.bottomMargin = rowMarginPx;
                    row.setLayoutParams(rlp);

                    // مقبض السحب الفيكتور (☰)
                    FrameLayout handleBox = new FrameLayout(MainActivity.this);
                    handleBox.setPadding(dp(10), dp(10), dp(10), dp(10));
                    ImageView handleIcon = createVectorImageView("ic_drag_handle", dp(20), COLOR_TEXT_SECONDARY);
                    handleBox.addView(handleIcon, new FrameLayout.LayoutParams(dp(20), dp(20), Gravity.CENTER));

                    // الأيقونة الفيكتور للأداة داخل مربع ناعم
                    FrameLayout iconBadge = new FrameLayout(MainActivity.this);
                    iconBadge.setBackground(roundRect(COLOR_ACCENT_SOFT, dp(9), COLOR_ACCENT_SOFT));
                    ImageView toolIcon = createVectorImageView(getToolIconResName(toolId), dp(19), COLOR_PRIMARY);
                    iconBadge.addView(toolIcon, new FrameLayout.LayoutParams(dp(19), dp(19), Gravity.CENTER));
                    LinearLayout.LayoutParams ibLp = new LinearLayout.LayoutParams(dp(34), dp(34));
                    ibLp.rightMargin = dp(4);
                    ibLp.leftMargin = dp(10);
                    iconBadge.setLayoutParams(ibLp);

                    // اسم الأداة
                    TextView labelTv = new TextView(MainActivity.this);
                    labelTv.setText(getToolLabel(toolId));
                    labelTv.setTextColor(COLOR_TEXT_PRIMARY);
                    labelTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14.5f);
                    labelTv.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
                    labelTv.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
                    applyFont(labelTv, true);

                    // مربع التفعيل
                    final CheckBox cb = new CheckBox(MainActivity.this);
                    cb.setChecked(tempEnabled.contains(toolId));
                    checkBoxesMap.put(toolId, cb);

                    cb.setOnCheckedChangeListener((buttonView, isChecked) -> {
                        if (internalSync[0]) return;
                        if (isChecked) tempEnabled.add(toolId);
                        else tempEnabled.remove(toolId);

                        internalSync[0] = true;
                        allCheck.setChecked(tempEnabled.size() == TOOL_IDS.length);
                        internalSync[0] = false;
                    });

                    row.setOnClickListener(v -> cb.setChecked(!cb.isChecked()));

                    // خوارزمية السحب السلسة: لا تحذف أي عنصر أثناء اللمس، بل تحرك السطور بانسيابية وتثبت الترتيب عند رفع الإصبع
                    handleBox.setOnTouchListener(new View.OnTouchListener() {
                        float startRawY = 0f;
                        int startIndex = -1;
                        int currentTargetIndex = -1;

                        @Override
                        public boolean onTouch(View v, MotionEvent event) {
                            int action = event.getActionMasked();
                            if (action == MotionEvent.ACTION_DOWN) {
                                startIndex = listContainer.indexOfChild(row);
                                if (startIndex < 0) return false;
                                currentTargetIndex = startIndex;
                                startRawY = event.getRawY();
                                scroll.requestDisallowInterceptTouchEvent(true);
                                row.bringToFront();
                                row.setBackground(roundRect(COLOR_ACCENT_SOFT, dp(12), COLOR_PRIMARY));
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                    row.setElevation(dp(8));
                                }
                                return true;
                            } else if (action == MotionEvent.ACTION_MOVE) {
                                if (startIndex < 0) return false;
                                int count = listContainer.getChildCount();
                                float dy = event.getRawY() - startRawY;
                                float minDy = -startIndex * slotPx;
                                float maxDy = (count - 1 - startIndex) * slotPx;
                                if (dy < minDy) dy = minDy;
                                if (dy > maxDy) dy = maxDy;

                                row.setTranslationY(dy);

                                int newTarget = Math.round(startIndex + (dy / slotPx));
                                if (newTarget < 0) newTarget = 0;
                                if (newTarget >= count) newTarget = count - 1;
                                currentTargetIndex = newTarget;

                                for (int idx = 0; idx < count; idx++) {
                                    if (idx == startIndex) continue;
                                    View child = listContainer.getChildAt(idx);
                                    float targetTrans = 0f;
                                    if (startIndex < currentTargetIndex && idx > startIndex && idx <= currentTargetIndex) {
                                        targetTrans = -slotPx;
                                    } else if (startIndex > currentTargetIndex && idx < startIndex && idx >= currentTargetIndex) {
                                        targetTrans = slotPx;
                                    }
                                    child.setTranslationY(targetTrans);
                                }
                                return true;
                            } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                                scroll.requestDisallowInterceptTouchEvent(false);
                                int count = listContainer.getChildCount();
                                for (int idx = 0; idx < count; idx++) {
                                    listContainer.getChildAt(idx).setTranslationY(0f);
                                }
                                row.setBackground(roundRect(COLOR_CARD, dp(12), COLOR_DIVIDER));
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                    row.setElevation(0f);
                                }
                                if (startIndex >= 0 && currentTargetIndex >= 0 && startIndex != currentTargetIndex) {
                                    String moved = tempOrder.remove(startIndex);
                                    tempOrder.add(currentTargetIndex, moved);
                                    buildList.run();
                                }
                                startIndex = -1;
                                return true;
                            }
                            return false;
                        }
                    });

                    row.addView(handleBox);
                    row.addView(iconBadge);
                    row.addView(labelTv);
                    row.addView(cb);
                    listContainer.addView(row);
                }
            }
        };

        buildList.run();

        allCheck.setOnClickListener(v -> {
            boolean selectAll = (tempEnabled.size() < TOOL_IDS.length);
            internalSync[0] = true;
            tempEnabled.clear();
            if (selectAll) {
                tempEnabled.addAll(Arrays.asList(TOOL_IDS));
            }
            allCheck.setChecked(selectAll);
            for (CheckBox cb : checkBoxesMap.values()) {
                cb.setChecked(selectAll);
            }
            internalSync[0] = false;
        });

        resetDefaultBtn.setOnClickListener(v -> {
            tempOrder.clear();
            tempOrder.addAll(Arrays.asList(TOOL_IDS));
            tempEnabled.clear();
            tempEnabled.addAll(Arrays.asList(DEFAULT_ENABLED_TOOLS));
            internalSync[0] = true;
            allCheck.setChecked(false);
            internalSync[0] = false;
            buildList.run();
            toast("تمت استعادة الترتيب والأدوات الافتراضية");
        });

        LinearLayout.LayoutParams scrollLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(340));
        outer.addView(scroll, scrollLp);

        LinearLayout actionsRow = new LinearLayout(this);
        actionsRow.setOrientation(LinearLayout.HORIZONTAL);
        actionsRow.setGravity(Gravity.CENTER_VERTICAL);
        actionsRow.setPadding(0, dp(12), 0, dp(2));

        Button saveOrderBtn = createPrimaryButton("حفظ الترتيب", null);
        LinearLayout.LayoutParams sLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.3f);
        sLp.leftMargin = dp(8);
        saveOrderBtn.setLayoutParams(sLp);

        Button cancelOrderBtn = createSecondaryButton("إلغاء", null);
        LinearLayout.LayoutParams cLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.9f);
        cancelOrderBtn.setLayoutParams(cLp);

        actionsRow.addView(saveOrderBtn);
        actionsRow.addView(cancelOrderBtn);
        outer.addView(actionsRow);

        b.setView(outer);
        final AlertDialog dialog = b.create();

        cancelOrderBtn.setOnClickListener(v -> dialog.dismiss());
        saveOrderBtn.setOnClickListener(v -> {
            orderedToolIds.clear();
            orderedToolIds.addAll(tempOrder);
            enabledToolIds.clear();
            enabledToolIds.addAll(tempEnabled);
            saveToolsConfig();
            dialog.dismiss();
            toast("تم حفظ ترتيب وأدوات التحرير ✓");
        });

        dialog.show();
    }

    private void createNewFileDialog() {
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setCustomTitle(createDialogTitleView("📝 إنشاء ملف نصي جديد", false));

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20), dp(12), dp(20), dp(4));

        final EditText input = new EditText(this);
        input.setHint("اكتب اسم الملف (مثال: أفكار وملاحظات)");
        input.setSingleLine(true);
        input.setTextColor(COLOR_TEXT_PRIMARY);
        input.setHintTextColor(COLOR_TEXT_SECONDARY);
        input.setBackground(roundRect(COLOR_BG, dp(12), COLOR_DIVIDER));
        input.setPadding(dp(14), dp(12), dp(14), dp(12));
        applyFont(input, false);
        box.addView(input);

        b.setView(box);
        b.setPositiveButton("إنشاء وفتح", (d, w) -> {
            String raw = input.getText().toString().trim();
            if (raw.isEmpty()) raw = "ملاحظة جديدة";
            String name = sanitizeFileName(raw);
            if (!name.toLowerCase(Locale.ROOT).endsWith(".txt")) name += ".txt";
            DocFile f = createFile(treeUri, uniqueName(treeUri, name));
            if (f != null) {
                activeHighlightQuery = "";
                edit(f);
            } else {
                toast("تعذر إنشاء الملف، تأكد من صلاحية المجلد");
            }
        });
        b.setNegativeButton("إلغاء", null);
        b.show();
    }

    private void rename(DocFile f) {
        if (f == null) return;
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setCustomTitle(createDialogTitleView("🏷 إعادة تسمية الملف", false));

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20), dp(12), dp(20), dp(4));

        final EditText input = new EditText(this);
        input.setText(displayName(f));
        input.setSingleLine(true);
        input.setTextColor(COLOR_TEXT_PRIMARY);
        input.setBackground(roundRect(COLOR_BG, dp(12), COLOR_DIVIDER));
        input.setPadding(dp(14), dp(12), dp(14), dp(12));
        input.setSelection(input.getText().length());
        applyFont(input, false);
        box.addView(input);

        b.setView(box);
        b.setPositiveButton("حفظ الاسم", (d, w) -> {
            String raw = input.getText().toString().trim();
            if (raw.isEmpty()) return;
            String newName = sanitizeFileName(raw);
            if (!newName.toLowerCase(Locale.ROOT).endsWith(".txt")) newName += ".txt";
            if (newName.equalsIgnoreCase(f.name)) return;

            String finalName = uniqueName(treeUri, newName);
            try {
                Uri renamedUri = DocumentsContract.renameDocument(getContentResolver(), f.uri, finalName);
                if (renamedUri != null) {
                    f.uri = renamedUri;
                    f.name = finalName;
                    if (currentFile != null) {
                        currentFile.uri = renamedUri;
                        currentFile.name = finalName;
                    }
                    if (docTitle != null && mode != Mode.HOME) {
                        docTitle.setText(displayName(f));
                    }
                    toast("تم تغيير الاسم بنجاح ✓");
                } else {
                    toast("تعذر تغيير الاسم");
                }
            } catch (Exception e) {
                toast("تعذر إعادة تسمية الملف");
            }
            if (mode == Mode.HOME) {
                refreshFiles();
            }
        });
        b.setNegativeButton("إلغاء", null);
        b.show();
    }

    private void duplicate(DocFile f) {
        String base = displayName(f) + " - نسخة.txt";
        DocFile copy = createFile(treeUri, uniqueName(treeUri, base));
        if (copy != null) {
            writeText(copy.uri, readText(f));
            refreshFiles();
            toast("تم إنشاء نسخة مكررة ✓");
        } else {
            toast("تعذر نسخ الملف");
        }
    }

    private void confirmDelete(DocFile f) {
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setCustomTitle(createDialogTitleView("⚠️ تأكيد الحذف", false));
        b.setMessage("هل أنت متأكد من حذف الملف «" + displayName(f) + "» نهائياً؟");
        b.setPositiveButton("نعم، احذف", (d, w) -> {
            try {
                boolean deleted = DocumentsContract.deleteDocument(getContentResolver(), f.uri);
                if (deleted) {
                    toast("تم حذف الملف");
                } else {
                    toast("تعذر حذف الملف");
                }
            } catch (Exception e) {
                toast("حدث خطأ أثناء الحذف");
            }
            refreshFiles();
        });
        b.setNegativeButton("إلغاء", null);
        b.show();
    }

    private void backupNow() {
        try {
            List<DocFile> files = listTxtFiles(treeUri);
            if (files.isEmpty()) {
                toast("لا توجد ملفات لعمل نسخة احتياطية");
                return;
            }
            Uri backupDir = ensureDirectory(treeUri, "النسخ_الاحتياطية");
            if (backupDir == null) {
                toast("تعذر إنشاء مجلد النسخ الاحتياطي");
                return;
            }
            String stamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.US).format(new Date());
            String zipName = "Mazwd_Backup_" + stamp + ".zip";
            Uri zipUri = DocumentsContract.createDocument(getContentResolver(), backupDir, "application/zip", zipName);
            if (zipUri == null) {
                toast("تعذر إنشاء ملف النسخة الاحتياطية");
                return;
            }

            OutputStream os = getContentResolver().openOutputStream(zipUri, "w");
            if (os == null) return;
            ZipOutputStream zos = new ZipOutputStream(os);
            for (DocFile f : files) {
                zos.putNextEntry(new ZipEntry(f.name));
                byte[] bytes = readText(f).getBytes(StandardCharsets.UTF_8);
                zos.write(bytes);
                zos.closeEntry();
            }
            zos.finish();
            zos.close();
            toast("تم حفظ النسخة الاحتياطية (" + files.size() + " ملف) في مجلد النسخ_الاحتياطية ✓");
        } catch (Exception e) {
            toast("فشل النسخ الاحتياطي: " + e.getMessage());
        }
    }

    private Uri getRootDocUri(Uri tree) {
        return DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree));
    }

    private Uri ensureDirectory(Uri tree, String name) {
        Uri rootDocUri = getRootDocUri(tree);
        Uri existing = findChildByName(tree, rootDocUri, name, true);
        if (existing != null) return existing;
        try {
            return DocumentsContract.createDocument(
                    getContentResolver(), rootDocUri, DocumentsContract.Document.MIME_TYPE_DIR, name);
        } catch (Exception e) {
            return null;
        }
    }

    private Uri findChildByName(Uri tree, Uri parentDocUri, String target, boolean dirOnly) {
        String parentDocId = DocumentsContract.getDocumentId(parentDocUri);
        Uri children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, parentDocId);
        String[] proj = {
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE
        };
        try (Cursor c = getContentResolver().query(children, proj, null, null, null)) {
            if (c == null) return null;
            while (c.moveToNext()) {
                String id = c.getString(0);
                String n = c.getString(1);
                String mime = c.getString(2);
                boolean isDir = DocumentsContract.Document.MIME_TYPE_DIR.equals(mime);
                if (target.equalsIgnoreCase(n) && (!dirOnly || isDir)) {
                    return DocumentsContract.buildDocumentUriUsingTree(tree, id);
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private List<DocFile> listTxtFiles(Uri tree) {
        List<DocFile> out = new ArrayList<>();
        if (tree == null) return out;
        String rootDocId = DocumentsContract.getTreeDocumentId(tree);
        Uri children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, rootDocId);
        String[] proj = {
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_LAST_MODIFIED,
                DocumentsContract.Document.COLUMN_MIME_TYPE
        };
        try (Cursor c = getContentResolver().query(children, proj, null, null, null)) {
            if (c == null) return out;
            while (c.moveToNext()) {
                String id = c.getString(0);
                String name = c.getString(1);
                long mod = c.getLong(2);
                String mime = c.getString(3);
                if (name != null
                        && !DocumentsContract.Document.MIME_TYPE_DIR.equals(mime)
                        && name.toLowerCase(Locale.ROOT).endsWith(".txt")) {
                    Uri u = DocumentsContract.buildDocumentUriUsingTree(tree, id);
                    out.add(new DocFile(u, name, mod));
                }
            }
        } catch (Exception ignored) {
        }
        return out;
    }

    private DocFile createFile(Uri tree, String name) {
        try {
            Uri rootDocUri = getRootDocUri(tree);
            Uri u = DocumentsContract.createDocument(getContentResolver(), rootDocUri, "text/plain", name);
            if (u == null) return null;
            return new DocFile(u, name, System.currentTimeMillis());
        } catch (Exception e) {
            return null;
        }
    }

    private String uniqueName(Uri tree, String desired) {
        Set<String> names = new HashSet<>();
        for (DocFile f : listTxtFiles(tree)) {
            names.add(f.name.toLowerCase(Locale.ROOT));
        }
        if (!names.contains(desired.toLowerCase(Locale.ROOT))) return desired;
        String base = desired.replaceAll("(?i)\\.txt$", "");
        int i = 2;
        while (true) {
            String c = base + " (" + i + ").txt";
            if (!names.contains(c.toLowerCase(Locale.ROOT))) return c;
            i++;
        }
    }

    private String sanitizeFileName(String name) {
        return name.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
    }

    private String readText(DocFile f) {
        StringBuilder sb = new StringBuilder();
        try (InputStream is = getContentResolver().openInputStream(f.uri)) {
            if (is == null) return "";
            BufferedReader r = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            String line;
            boolean first = true;
            while ((line = r.readLine()) != null) {
                if (!first) sb.append('\n');
                sb.append(line);
                first = false;
            }
        } catch (Exception ignored) {
        }
        return sb.toString();
    }

    private void writeText(Uri uri, String content) {
        try {
            OutputStream os = getContentResolver().openOutputStream(uri, "wt");
            if (os == null) os = getContentResolver().openOutputStream(uri);
            if (os != null) {
                os.write(content.getBytes(StandardCharsets.UTF_8));
                os.flush();
                os.close();
            }
        } catch (Exception ignored) {
        }
    }

    private String displayName(DocFile f) {
        return f.name.replaceAll("(?i)\\.txt$", "");
    }

    private void hideKeyboard() {
        try {
            View v = getCurrentFocus();
            if (v == null) v = root;
            if (v != null) {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }
            }
        } catch (Exception ignored) {
        }
    }

    private void goBack() {
        if (mode == Mode.EDIT && currentFile != null) {
            autoSaveHandler.removeCallbacks(autoSaveRunnable);
            saveNow(false);
            hideKeyboard();
            read(currentFile);
        } else if (mode == Mode.READ) {
            showHome();
        } else {
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        goBack();
    }

    private View createDialogTitleView(String titleText, boolean autoDirectionForFileName) {
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        header.setPadding(dp(20), dp(18), dp(20), dp(12));

        TextView tv = new TextView(this);
        tv.setText(titleText);
        tv.setTextColor(COLOR_PRIMARY_DARK);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        tv.setIncludeFontPadding(true);
        tv.setPadding(0, dp(2), 0, dp(4));
        if (autoDirectionForFileName) {
            tv.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG_RTL);
            tv.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        } else {
            tv.setTextDirection(View.TEXT_DIRECTION_RTL);
            tv.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        }
        applyFont(tv, true);

        View line = new View(this);
        line.setBackgroundColor(COLOR_DIVIDER);
        LinearLayout.LayoutParams lineLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(1));
        lineLp.topMargin = dp(8);

        header.addView(tv);
        header.addView(line, lineLp);
        return header;
    }

    private Button createPrimaryButton(String text, Runnable onClick) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        b.setTextColor(Color.WHITE);
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        b.setBackground(roundRect(COLOR_PRIMARY, dp(12), COLOR_PRIMARY));
        b.setPadding(dp(16), dp(10), dp(16), dp(10));
        applyFont(b, true);
        if (onClick != null) b.setOnClickListener(v -> onClick.run());
        return b;
    }

    private Button createSecondaryButton(String text, Runnable onClick) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        b.setTextColor(COLOR_TEXT_PRIMARY);
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
        b.setBackground(roundRect(COLOR_CARD, dp(12), COLOR_DIVIDER));
        b.setPadding(dp(14), dp(9), dp(14), dp(9));
        applyFont(b, true);
        if (onClick != null) b.setOnClickListener(v -> onClick.run());
        return b;
    }

    private GradientDrawable roundRect(int fillColor, int radiusPx, int strokeColor) {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(fillColor);
        gd.setCornerRadius(radiusPx);
        gd.setStroke(dp(1), strokeColor);
        return gd;
    }

    private GradientDrawable createHeaderGradient() {
        GradientDrawable gd = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{COLOR_PRIMARY, COLOR_PRIMARY_DARK});
        gd.setCornerRadii(new float[]{0, 0, 0, 0, dp(26), dp(26), dp(26), dp(26)});
        return gd;
    }

    private Typeface loadFontResource(String resName, Typeface fallback, int style) {
        try {
            int resId = getResources().getIdentifier(resName, "font", getPackageName());
            if (resId != 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Typeface tf = getResources().getFont(resId);
                if (tf != null) return tf;
            }
        } catch (Exception ignored) {
        }
        return Typeface.create(fallback, style);
    }

    private Typeface getFontByIndex(int index, boolean bold) {
        int style = bold ? Typeface.BOLD : Typeface.NORMAL;
        if (index == 1) {
            return loadFontResource(bold ? "tajawal_bold" : "tajawal_regular", Typeface.SANS_SERIF, style);
        } else if (index == 2) {
            return loadFontResource(bold ? "mazwd_arabic_bold" : "mazwd_arabic", Typeface.SERIF, style);
        } else if (index == 3) {
            return loadFontResource(bold ? "aref_ruqaa_bold" : "aref_ruqaa", Typeface.SERIF, style);
        } else if (index == 4) {
            return loadFontResource(bold ? "reem_kufi_bold" : "reem_kufi", Typeface.SANS_SERIF, style);
        } else if (index == 5) {
            return Typeface.create(Typeface.SERIF, style);
        } else if (index == 6) {
            return Typeface.create("cursive", style);
        }
        return Typeface.create("sans-serif", style);
    }

    private void applyFont(TextView tv, boolean bold) {
        tv.setIncludeFontPadding(true);
        tv.setTypeface(getFontByIndex(fontChoice, bold));
    }

    private int dp(float v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }
}
