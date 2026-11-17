package com.sennara.app.util;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;

import com.google.android.material.chip.Chip;
import com.sennara.app.R;
import com.sennara.app.forecast.Conditions;

import java.util.ArrayList;

public class Ui {

    public static int dp(Context c, float value) {
        return Math.round(value * c.getResources().getDisplayMetrics().density);
    }

    public static int color(Context c, int colorRes) {
        return ContextCompat.getColor(c, colorRes);
    }

    public static GradientDrawable rounded(int color, float radius) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(color);
        shape.setCornerRadius(radius);
        return shape;
    }

    public static void verdictPill(TextView pill, int level) {
        Context c = pill.getContext();
        pill.setText(Labels.verdict(c, level));
        pill.setTextColor(color(c, Labels.levelColor(level)));
        pill.setBackground(rounded(color(c, Labels.levelSoft(level)), dp(c, 10)));
    }

    public static void verdictPillSolid(TextView pill, int level) {
        Context c = pill.getContext();
        pill.setText(Labels.verdict(c, level));
        pill.setTextColor(color(c, R.color.white));
        pill.setBackground(rounded(color(c, Labels.levelColor(level)), dp(c, 10)));
    }

    public static void scorePill(TextView pill, int score, int level) {
        Context c = pill.getContext();
        if (level == Conditions.UNSAFE) {
            pill.setText("!");
        } else {
            pill.setText(String.valueOf(score));
        }
        pill.setTextColor(color(c, Labels.levelColor(level)));
        pill.setBackground(rounded(color(c, Labels.levelSoft(level)), dp(c, 10)));
    }

    public static void tint(ImageView icon, int colorRes) {
        ImageViewCompat.setImageTintList(icon, ColorStateList.valueOf(color(icon.getContext(), colorRes)));
    }

    public static Chip infoChip(Context c, String text, int icon) {
        Chip chip = new Chip(c);
        chip.setText(text);
        chip.setClickable(false);
        chip.setCheckable(false);
        chip.setChipBackgroundColorResource(R.color.primary_soft);
        chip.setTextColor(color(c, R.color.primary));
        chip.setChipStrokeWidth(0);
        chip.setEnsureMinTouchTargetSize(false);
        if (icon != 0) {
            chip.setChipIconResource(icon);
            chip.setChipIconTintResource(R.color.primary);
            chip.setChipIconSize(dp(c, 16));
        }
        return chip;
    }

    public static void reasons(ViewGroup box, ArrayList<Conditions.Reason> reasons, int max, boolean onPhoto) {
        box.removeAllViews();
        Context c = box.getContext();
        for (int i = 0; i < reasons.size() && i < max; i++) {
            Conditions.Reason reason = reasons.get(i);
            View row = View.inflate(c, R.layout.item_reason, null);
            ImageView icon = row.findViewById(R.id.reason_icon);
            TextView text = row.findViewById(R.id.reason_text);
            icon.setImageResource(reason.icon);
            text.setText(c.getString(reason.text, reason.args));
            if (onPhoto) {
                if (reason.good) {
                    tint(icon, R.color.on_photo_good);
                } else {
                    tint(icon, R.color.on_photo_bad);
                }
                text.setTextColor(color(c, R.color.white));
            } else {
                if (reason.good) {
                    tint(icon, R.color.lv_great);
                } else {
                    tint(icon, R.color.lv_unsafe);
                }
            }
            box.addView(row);
        }
    }

    public static void pop(View v, long delay) {
        v.setScaleX(0.4f);
        v.setScaleY(0.4f);
        v.setAlpha(0f);
        v.animate().scaleX(1f).scaleY(1f).alpha(1f).setStartDelay(delay).setDuration(420)
                .setInterpolator(new OvershootInterpolator(2.2f)).start();
    }

    public static void rise(View v, long delay) {
        v.setAlpha(0f);
        v.setTranslationY(dp(v.getContext(), 16));
        v.animate().alpha(1f).translationY(0).setStartDelay(delay).setDuration(320).start();
    }

    public static String initials(String name) {
        String result = "";
        String[] words = name.trim().split(" ");
        for (String word : words) {
            if (!word.isEmpty() && result.length() < 2) {
                result = result + word.charAt(0);
            }
        }
        if (result.isEmpty()) {
            return "?";
        }
        return result.toUpperCase();
    }
}
