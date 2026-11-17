package com.sennara.app.ui.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import com.sennara.app.R;
import com.sennara.app.data.Models;

import java.util.Calendar;

public class SeasonBarView extends View {
    private Paint on = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint off = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private RectF rect = new RectF();
    private int from = 1;
    private int to = 12;
    private String[] letters = {"J", "F", "M", "A", "M", "J", "J", "A", "S", "O", "N", "D"};

    public SeasonBarView(Context c, AttributeSet attrs) {
        super(c, attrs);
        float density = getResources().getDisplayMetrics().density;
        on.setColor(ContextCompat.getColor(c, R.color.lv_great));
        off.setColor(ContextCompat.getColor(c, R.color.surface2));
        ring.setColor(ContextCompat.getColor(c, R.color.text));
        ring.setStyle(Paint.Style.STROKE);
        ring.setStrokeWidth(2 * density);
        text.setTextAlign(Paint.Align.CENTER);
        text.setTextSize(12 * getResources().getDisplayMetrics().scaledDensity);
        text.setTypeface(ResourcesCompat.getFont(c, R.font.plex));
    }

    public void setSeason(int from, int to, String[] monthLetters) {
        this.from = from;
        this.to = to;
        if (monthLetters != null && monthLetters.length == 12) {
            letters = monthLetters;
        }
        invalidate();
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int height = (int) (44 * getResources().getDisplayMetrics().density);
        setMeasuredDimension(MeasureSpec.getSize(widthSpec), height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float d = getResources().getDisplayMetrics().density;
        float gap = 4 * d;
        float cell = (getWidth() - 11 * gap) / 12f;
        float height = getHeight() - 4 * d;
        int currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1;
        boolean rightToLeft = getLayoutDirection() == LAYOUT_DIRECTION_RTL;

        for (int month = 1; month <= 12; month++) {
            int position = month - 1;
            if (rightToLeft) {
                position = 12 - month;
            }
            float x = position * (cell + gap);
            rect.set(x, 2 * d, x + cell, height);

            boolean inSeason = Models.inMonths(month, from, to);
            if (inSeason) {
                canvas.drawRoundRect(rect, 8 * d, 8 * d, on);
                text.setColor(ContextCompat.getColor(getContext(), R.color.white));
            } else {
                canvas.drawRoundRect(rect, 8 * d, 8 * d, off);
                text.setColor(ContextCompat.getColor(getContext(), R.color.text3));
            }
            if (month == currentMonth) {
                canvas.drawRoundRect(rect, 8 * d, 8 * d, ring);
            }
            canvas.drawText(letters[month - 1], x + cell / 2, height / 2 + 6 * d, text);
        }
    }
}
