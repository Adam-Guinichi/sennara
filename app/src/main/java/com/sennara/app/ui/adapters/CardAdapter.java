package com.sennara.app.ui.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.sennara.app.R;
import com.sennara.app.util.Images;
import com.sennara.app.util.Ui;

import java.util.ArrayList;

public class CardAdapter extends RecyclerView.Adapter<CardAdapter.Holder> {
    public static final int SPOT = 0;
    public static final int FISH = 1;
    public static final int TUTORIAL = 2;
    public static final int BADGE = 3;
    public static final int FISH_GRID = 4;
    public static final int FISH_PICK = 5;

    public static class Card {
        public int type;
        public String image;
        public int icon;
        public String title = "";
        public String subtitle = "";
        public String pill;
        public int pillColor = R.color.primary;
        public int pillSoft = R.color.primary_soft;
        public boolean dimmed;
        public boolean selected;
        public Intent intent;
        public View.OnClickListener listener;

        public Card(int type) {
            this.type = type;
        }
    }

    private ArrayList<Card> cards = new ArrayList<>();

    public void setCards(ArrayList<Card> cards) {
        this.cards = cards;
        notifyDataSetChanged();
    }

    public ArrayList<Card> getCards() {
        return cards;
    }

    @Override
    public int getItemViewType(int position) {
        return cards.get(position).type;
    }

    @Override
    public int getItemCount() {
        return cards.size();
    }

    @Override
    public Holder onCreateViewHolder(ViewGroup parent, int type) {
        int layout;
        if (type == SPOT) {
            layout = R.layout.card_spot;
        } else if (type == TUTORIAL) {
            layout = R.layout.card_tutorial;
        } else if (type == BADGE) {
            layout = R.layout.card_badge;
        } else if (type == FISH_GRID) {
            layout = R.layout.card_fish_grid;
        } else if (type == FISH_PICK) {
            layout = R.layout.card_fish_pick;
        } else {
            layout = R.layout.card_fish;
        }
        View view = LayoutInflater.from(parent.getContext()).inflate(layout, parent, false);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(Holder h, int position) {
        final Card card = cards.get(position);
        final Context context = h.itemView.getContext();

        if (h.image != null) {
            if (card.image != null) {
                int width = 400;
                if (card.type == SPOT || card.type == TUTORIAL) {
                    width = 640;
                }
                Images.load(h.image, card.image, width);
            } else if (card.icon != 0) {
                h.image.setImageResource(card.icon);
            }
        }
        if (h.title != null) {
            h.title.setText(card.title);
        }
        if (h.subtitle != null) {
            h.subtitle.setText(card.subtitle);
            if (card.subtitle == null || card.subtitle.isEmpty()) {
                h.subtitle.setVisibility(View.GONE);
            } else {
                h.subtitle.setVisibility(View.VISIBLE);
            }
        }
        if (h.pill != null) {
            if (card.pill == null || card.pill.isEmpty()) {
                h.pill.setVisibility(View.GONE);
            } else {
                h.pill.setVisibility(View.VISIBLE);
                h.pill.setText(card.pill);
                h.pill.setTextColor(Ui.color(context, card.pillColor));
                h.pill.setBackground(Ui.rounded(Ui.color(context, card.pillSoft), Ui.dp(context, 10)));
            }
        }

        if (card.dimmed) {
            h.itemView.setAlpha(0.45f);
        } else {
            h.itemView.setAlpha(1f);
        }
        if (card.type == FISH_PICK) {
            h.itemView.setSelected(card.selected);
        }
        if (card.type == BADGE && h.image != null) {
            if (card.dimmed) {
                Ui.tint(h.image, R.color.text3);
            } else {
                Ui.tint(h.image, R.color.accent_strong);
            }
        }

        h.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (card.listener != null) {
                    card.listener.onClick(v);
                } else if (card.intent != null) {
                    context.startActivity(card.intent);
                }
            }
        });
    }

    static class Holder extends RecyclerView.ViewHolder {
        ImageView image;
        TextView title;
        TextView subtitle;
        TextView pill;

        Holder(View v) {
            super(v);
            image = v.findViewById(R.id.card_image);
            title = v.findViewById(R.id.card_title);
            subtitle = v.findViewById(R.id.card_subtitle);
            pill = v.findViewById(R.id.card_pill);
        }
    }
}
