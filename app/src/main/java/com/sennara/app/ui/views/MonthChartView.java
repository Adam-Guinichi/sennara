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

public class MonthChartView extends View {
    private Paint bar = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint label = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint value = new Paint(Paint.ANTI_ALIAS_FLAG);
    private RectF rect = new RectF();
    private int[] counts = new int[0];
    private String[] names = new String[0];

    public MonthChartView(Context c, AttributeSet attrs) {
        super(c, attrs);
        label.setTextAlign(Paint.Align.CENTER);
        label.setTextSize(12 * getResources().getDisplayMetrics().scaledDensity);
        label.setColor(ContextCompat.getColor(c, R.color.text3));
        label.setTypeface(ResourcesCompat.getFont(c, R.font.plex));
        value.set(label);
        value.setColor(ContextCompat.getColor(c, R.color.text));
    }

    public void setData(int[] counts, String[] names) {
        this.counts = counts;
        this.names = names;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (counts.length == 0) {
            return;
        }
        float d = getResources().getDisplayMetrics().density;
        int max = 1;
        for (int count : counts) {
            if (count > max) {
                max = count;
            }
        }
        float slot = (getWidth() - getPaddingLeft() - getPaddingRight()) / (float) counts.length;
        float top = 20 * d;
        float bottom = getHeight() - 22 * d;
        float barWidth = Math.min(28 * d, slot * 0.5f);
        boolean rightToLeft = getLayoutDirection() == LAYOUT_DIRECTION_RTL;

        for (int k = 0; k < counts.length; k++) {
            int i = k;
            if (rightToLeft) {
                i = counts.length - 1 - k;
            }
            float centerX = getPaddingLeft() + (k + 0.5f) * slot;
            float height = (bottom - top) * counts[i] / (float) max;

            if (i == counts.length - 1) {
                bar.setColor(ContextCompat.getColor(getContext(), R.color.accent));
            } else {
                bar.setColor(ContextCompat.getColor(getContext(), R.color.primary_soft_strong));
            }
            rect.set(centerX - barWidth / 2, bottom - Math.max(height, 4 * d), centerX + barWidth / 2, bottom);
            canvas.drawRoundRect(rect, 8 * d, 8 * d, bar);

            if (counts[i] > 0) {
                canvas.drawText(String.valueOf(counts[i]), centerX, bottom - height - 6 * d, value);
            }
            canvas.drawText(names[i], centerX, getHeight() - 4 * d, label);
        }
    }
}
