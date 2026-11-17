package com.sennara.app.ui;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.os.LocaleListCompat;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.core.widget.NestedScrollView;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.sennara.app.R;
import com.sennara.app.util.Ui;

import java.util.Locale;

public abstract class BaseActivity extends AppCompatActivity {

    protected void edgeToEdge(View root, final View top, final View bottom, boolean lightStatusIcons) {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        final int topStart;
        if (top == null) {
            topStart = 0;
        } else {
            topStart = top.getPaddingTop();
        }
        final int bottomStart;
        if (bottom == null) {
            bottomStart = 0;
        } else {
            bottomStart = bottom.getPaddingBottom();
        }

        ViewCompat.setOnApplyWindowInsetsListener(root, new OnApplyWindowInsetsListener() {
            @Override
            public WindowInsetsCompat onApplyWindowInsets(View v, WindowInsetsCompat insets) {
                Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
                Insets keyboard = insets.getInsets(WindowInsetsCompat.Type.ime());
                if (top != null) {
                    top.setPadding(top.getPaddingLeft(), topStart + bars.top, top.getPaddingRight(), top.getPaddingBottom());
                }
                View fadeBar = findViewById(R.id.scroll_bar);
                if (fadeBar != null && fadeBar != top) {
                    fadeBar.setPadding(0, bars.top, 0, 0);
                }
                if (bottom != null) {
                    int extra = Math.max(bars.bottom, keyboard.bottom);
                    bottom.setPadding(bottom.getPaddingLeft(), bottom.getPaddingTop(), bottom.getPaddingRight(), bottomStart + extra);
                }
                return insets;
            }
        });

        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(getWindow(), root);
        controller.setAppearanceLightStatusBars(!lightStatusIcons && !isNightMode());
        controller.setAppearanceLightNavigationBars(!isNightMode());
    }

    protected boolean isNightMode() {
        int mode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return mode == Configuration.UI_MODE_NIGHT_YES;
    }

    protected void photoHeader(final int headerDp, CharSequence title) {
        NestedScrollView scroll = findViewById(R.id.scroll);
        final View bar = findViewById(R.id.scroll_bar);
        TextView barTitle = findViewById(R.id.scroll_title);
        barTitle.setText(title);

        scroll.setOnScrollChangeListener(new NestedScrollView.OnScrollChangeListener() {
            @Override
            public void onScrollChange(NestedScrollView v, int x, int y, int oldX, int oldY) {
                float start = dp(headerDp) - bar.getHeight() - dp(24);
                float alpha = (y - start) / dp(48);
                if (alpha < 0) {
                    alpha = 0;
                }
                if (alpha > 1) {
                    alpha = 1;
                }
                bar.setAlpha(alpha);
                WindowCompat.getInsetsController(getWindow(), v).setAppearanceLightStatusBars(alpha > 0.5f && !isNightMode());
            }
        });
    }

    protected void setupSimple(int layout, CharSequence title) {
        setContentView(layout);
        View root = findViewById(R.id.root);
        View bar = findViewById(R.id.top_bar);
        View bottom = findViewById(R.id.bottom_area);
        if (bottom == null) {
            bottom = findViewById(R.id.scroll);
        }
        edgeToEdge(root, bar, bottom, false);

        TextView titleView = findViewById(R.id.top_title);
        if (titleView != null) {
            titleView.setText(title);
        }
        View back = findViewById(R.id.top_back);
        if (back != null) {
            back.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                }
            });
        }
    }

    protected void open(Class<?> screen) {
        Intent intent = new Intent(this, screen);
        startActivity(intent);
    }

    protected void toast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_LONG).show();
    }

    protected void toast(int text) {
        Toast.makeText(this, text, Toast.LENGTH_LONG).show();
    }

    protected Locale locale() {
        return getResources().getConfiguration().getLocales().get(0);
    }

    protected String lang() {
        String language = locale().getLanguage();
        if (language.equals("fr") || language.equals("ar")) {
            return language;
        }
        return "en";
    }

    protected int dp(int value) {
        return Ui.dp(this, value);
    }

    protected String text(int editTextId) {
        EditText field = findViewById(editTextId);
        return field.getText().toString().trim();
    }

    public static boolean found(String field, String query) {
        if (query.isEmpty()) {
            return true;
        }
        if (field == null) {
            return false;
        }
        return field.toLowerCase(Locale.ROOT).contains(query);
    }

    // ================================================================ language

    protected void bindLanguageToggle() {
        MaterialButtonToggleGroup group = findViewById(R.id.lang_group);
        String current = lang();
        if (current.equals("fr")) {
            group.check(R.id.lang_fr);
        } else if (current.equals("ar")) {
            group.check(R.id.lang_ar);
        } else {
            group.check(R.id.lang_en);
        }

        group.addOnButtonCheckedListener(new MaterialButtonToggleGroup.OnButtonCheckedListener() {
            @Override
            public void onButtonChecked(MaterialButtonToggleGroup g, int id, boolean checked) {
                if (!checked) {
                    return;
                }
                String tag = "en";
                if (id == R.id.lang_fr) {
                    tag = "fr";
                } else if (id == R.id.lang_ar) {
                    tag = "ar-TN";
                }
                if (!tag.startsWith(lang())) {
                    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag));
                }
            }
        });
    }

    // ================================================================ implicit intents: another app does the job

    protected void openDirections(double lat, double lng, String label) {
        String place = lat + "," + lng;
        try {
            Intent navigation = new Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=" + place));
            startActivity(navigation);
        } catch (ActivityNotFoundException e) {
            try {
                Uri geo = Uri.parse("geo:" + place + "?q=" + place + "(" + Uri.encode(label) + ")");
                startActivity(new Intent(Intent.ACTION_VIEW, geo));
            } catch (ActivityNotFoundException e2) {
                openUrl("https://www.google.com/maps/dir/?api=1&destination=" + place);
            }
        }
    }

    protected void dial(String phone) {
        try {
            Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone.replace(" ", "")));
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            toast(R.string.error_no_app);
        }
    }

    protected void sms(String phone, String message) {
        try {
            Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + phone.replace(" ", "")));
            intent.putExtra("sms_body", message);
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            toast(R.string.error_no_app);
        }
    }

    protected void openUrl(String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            toast(R.string.error_no_app);
        }
    }

    protected void shareText(String text) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, text);
        startActivity(Intent.createChooser(intent, null));
    }
}
