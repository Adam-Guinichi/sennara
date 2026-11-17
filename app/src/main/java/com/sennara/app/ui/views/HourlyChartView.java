package com.sennara.app.ui.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import com.sennara.app.R;
import com.sennara.app.forecast.Conditions;
import com.sennara.app.util.Fmt;
import com.sennara.app.util.Labels;

import java.util.ArrayList;

public class HourlyChartView extends View {

    public interface OnHourSelected {
        void onHour(int index);
    }

    private Paint bar = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint track = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint highlight = new Paint(Paint.ANTI_ALIAS_FLAG);
    private RectF rect = new RectF();
    private ArrayList<Conditions.Result> data = new ArrayList<>();
    private int selected = 0;
    private OnHourSelected listener;

    public HourlyChartView(Context c, AttributeSet attrs) {
        super(c, attrs);
        text.setTextSize(12 * getResources().getDisplayMetrics().scaledDensity);
        text.setColor(ContextCompat.getColor(c, R.color.text2));
        text.setTextAlign(Paint.Align.CENTER);
        text.setTypeface(ResourcesCompat.getFont(c, R.font.plex));
        track.setColor(ContextCompat.getColor(c, R.color.surface2));
        highlight.setColor(ContextCompat.getColor(c, R.color.primary));
        highlight.setStyle(Paint.Style.STROKE);
        highlight.setStrokeWidth(dp(2));
    }

    public void setData(ArrayList<Conditions.Result> results, int max) {
        data = new ArrayList<>();
        for (int i = 0; i < results.size() && i < max; i++) {
            data.add(results.get(i));
        }
        selected = 0;
        requestLayout();
        invalidate();
    }

    public void setOnHourSelected(OnHourSelected l) {
        listener = l;
    }

    private float slot() {
        return dp(30);
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int width = (int) (data.size() * slot()) + getPaddingLeft() + getPaddingRight();
        int available = MeasureSpec.getSize(widthSpec);
        if (available > width) {
            width = available;
        }
        setMeasuredDimension(width, (int) dp(150));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float top = dp(8);
        float bottom = getHeight() - dp(26);
        float barWidth = dp(16);
        boolean rightToLeft = getLayoutDirection() == LAYOUT_DIRECTION_RTL;

        for (int i = 0; i < data.size(); i++) {
            Conditions.Result r = data.get(i);
            float centerX;
            if (rightToLeft) {
                centerX = getWidth() - getPaddingRight() - (i + 0.5f) * slot();
            } else {
                centerX = getPaddingLeft() + (i + 0.5f) * slot();
            }

            rect.set(centerX - barWidth / 2, top, centerX + barWidth / 2, bottom);
            canvas.drawRoundRect(rect, dp(8), dp(8), track);

            int value = r.score;
            if (r.unsafe) {
                value = 100;
            }
            float height = (bottom - top) * Math.max(0.08f, value / 100f);
            rect.set(centerX - barWidth / 2, bottom - height, centerX + barWidth / 2, bottom);
            bar.setColor(ContextCompat.getColor(getContext(), Labels.levelColor(r.level)));
            if (i == selected) {
                bar.setAlpha(255);
            } else {
                bar.setAlpha(170);
            }
            canvas.drawRoundRect(rect, dp(8), dp(8), bar);

            if (i == selected) {
                rect.set(centerX - barWidth / 2 - dp(3), top - dp(3), centerX + barWidth / 2 + dp(3), bottom + dp(3));
                canvas.drawRoundRect(rect, dp(10), dp(10), highlight);
            }

            if (i % 3 == 0 || i == selected) {
                if (i == selected) {
                    text.setColor(ContextCompat.getColor(getContext(), R.color.text));
                } else {
                    text.setColor(ContextCompat.getColor(getContext(), R.color.text3));
                }
                canvas.drawText(Fmt.hour(r.t), centerX, getHeight() - dp(6), text);
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE) {
            float x;
            if (getLayoutDirection() == LAYOUT_DIRECTION_RTL) {
                x = getWidth() - getPaddingRight() - event.getX();
            } else {
                x = event.getX() - getPaddingLeft();
            }
            int index = (int) (x / slot());
            if (index >= 0 && index < data.size() && index != selected) {
                selected = index;
                invalidate();
                if (listener != null) {
                    listener.onHour(index);
                }
            }
            if (getParent() != null) {
                getParent().requestDisallowInterceptTouchEvent(action == MotionEvent.ACTION_MOVE);
            }
            return true;
        }
        if (action == MotionEvent.ACTION_UP) {
            performClick();
        }
        return true;
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }
}
