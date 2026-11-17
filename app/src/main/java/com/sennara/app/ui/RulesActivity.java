package com.sennara.app.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.sennara.app.R;
import com.sennara.app.data.Db;
import com.sennara.app.data.Models;
import com.sennara.app.util.Ui;

public class RulesActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupSimple(R.layout.activity_rules, getString(R.string.rules_title));
        showRules();
        showSizes();
    }

    private void showRules() {
        LinearLayout box = findViewById(R.id.rules);
        for (final Models.Rule rule : Db.get(this).rules()) {
            View card = getLayoutInflater().inflate(R.layout.item_rule, box, false);
            TextView kind = card.findViewById(R.id.rule_kind);
            if (rule.kind.equals("tn")) {
                kind.setText(R.string.rule_tn);
                kind.setTextColor(Ui.color(this, R.color.lv_unsafe));
                kind.setBackground(Ui.rounded(Ui.color(this, R.color.lv_unsafe_soft), dp(10)));
            } else {
                if (rule.kind.equals("ref")) {
                    kind.setText(R.string.rule_ref);
                } else {
                    kind.setText(R.string.rule_tip);
                }
                kind.setTextColor(Ui.color(this, R.color.primary));
                kind.setBackground(Ui.rounded(Ui.color(this, R.color.primary_soft), dp(10)));
            }
            TextView title = card.findViewById(R.id.rule_title);
            title.setText(rule.title(lang()));
            TextView body = card.findViewById(R.id.rule_body);
            body.setText(rule.body(lang()));

            View source = card.findViewById(R.id.rule_source);
            if (rule.sourceUrl.isEmpty()) {
                source.setVisibility(View.GONE);
            } else {
                source.setVisibility(View.VISIBLE);
                source.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        openUrl(rule.sourceUrl);
                    }
                });
            }
            box.addView(card);
        }
    }

    private void showSizes() {
        LinearLayout table = findViewById(R.id.sizes);
        for (Models.Fish fish : Db.get(this).fish()) {
            if (fish.minSizeCm == 0 && fish.minWeightG == 0) {
                continue;
            }
            View row = getLayoutInflater().inflate(R.layout.item_sky, table, false);
            row.findViewById(R.id.sky_icon).setVisibility(View.GONE);
            TextView name = row.findViewById(R.id.sky_label);
            name.setText(fish.name(lang()) + "\n" + fish.sci);
            TextView size = row.findViewById(R.id.sky_time);
            if (fish.minWeightG > 0) {
                size.setText(fish.minWeightG / 1000 + " kg");
            } else {
                size.setText(fish.minSizeCm + " cm");
            }
            if (fish.sizeRule.equals("tn_rule")) {
                size.setTextColor(Ui.color(this, R.color.lv_unsafe));
            }
            table.addView(row);
        }
    }
}
