package com.sennara.app.ui.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.sennara.app.R;
import com.sennara.app.util.Images;
import com.sennara.app.util.Ui;

import java.util.ArrayList;

public class RowAdapter extends ArrayAdapter<RowAdapter.Row> {

    public static class Row {
        public String image;
        public int icon;
        public String title = "";
        public String subtitle = "";
        public String extra = "";
        public String badge;
        public int badgeColor = R.color.primary;
        public int badgeSoft = R.color.primary_soft;
        public int trailing;
        public int trailingColor = R.color.text3;
        public Object tag;

        public void setBadge(String text, int color, int softColor) {
            badge = text;
            badgeColor = color;
            badgeSoft = softColor;
        }
    }

    public RowAdapter(Context c) {
        super(c, R.layout.item_row, new ArrayList<Row>());
    }

    public void setRows(ArrayList<Row> rows) {
        clear();
        addAll(rows);
        notifyDataSetChanged();
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        if (view == null) {
            view = LayoutInflater.from(getContext()).inflate(R.layout.item_row, parent, false);
        }
        Row row = getItem(position);

        ImageView photo = view.findViewById(R.id.row_photo);
        ImageView icon = view.findViewById(R.id.row_icon);
        if (row.image != null && !row.image.isEmpty()) {
            photo.setVisibility(View.VISIBLE);
            icon.setVisibility(View.GONE);
            Images.load(photo, row.image, 240);
        } else {
            photo.setVisibility(View.GONE);
            icon.setVisibility(View.VISIBLE);
            if (row.icon == 0) {
                icon.setImageResource(R.drawable.ic_place);
            } else {
                icon.setImageResource(row.icon);
            }
        }

        TextView title = view.findViewById(R.id.row_title);
        title.setText(row.title);
        showText((TextView) view.findViewById(R.id.row_subtitle), row.subtitle);
        showText((TextView) view.findViewById(R.id.row_extra), row.extra);

        TextView badge = view.findViewById(R.id.row_badge);
        if (row.badge == null || row.badge.isEmpty()) {
            badge.setVisibility(View.GONE);
        } else {
            badge.setVisibility(View.VISIBLE);
            badge.setText(row.badge);
            badge.setTextColor(Ui.color(getContext(), row.badgeColor));
            badge.setBackground(Ui.rounded(Ui.color(getContext(), row.badgeSoft), Ui.dp(getContext(), 10)));
        }

        ImageView trailing = view.findViewById(R.id.row_trailing);
        if (row.trailing == 0) {
            trailing.setVisibility(View.GONE);
        } else {
            trailing.setVisibility(View.VISIBLE);
            trailing.setImageResource(row.trailing);
            Ui.tint(trailing, row.trailingColor);
        }
        return view;
    }

    private static void showText(TextView view, String text) {
        view.setText(text);
        if (text == null || text.isEmpty()) {
            view.setVisibility(View.GONE);
        } else {
            view.setVisibility(View.VISIBLE);
        }
    }
}
