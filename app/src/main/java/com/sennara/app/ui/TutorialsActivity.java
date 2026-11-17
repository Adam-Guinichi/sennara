package com.sennara.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.sennara.app.R;
import com.sennara.app.data.Db;
import com.sennara.app.data.Models;
import com.sennara.app.util.Images;
import com.sennara.app.util.Labels;

public class TutorialsActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupSimple(R.layout.activity_tutorials, getString(R.string.learn_title));
        LinearLayout box = findViewById(R.id.list);
        for (final Models.Tutorial tutorial : Db.get(this).tutorials()) {
            View card = getLayoutInflater().inflate(R.layout.item_tutorial_big, box, false);
            ImageView image = card.findViewById(R.id.t_image);
            Images.load(image, tutorial.image, 900);
            TextView level = card.findViewById(R.id.t_level);
            level.setText(Labels.level(this, tutorial.level) + " · " + getString(R.string.minutes_read, tutorial.minutes));
            TextView title = card.findViewById(R.id.t_title);
            title.setText(tutorial.title(lang()));
            card.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(TutorialsActivity.this, TutorialDetailActivity.class);
                    intent.putExtra("id", tutorial.id);
                    startActivity(intent);
                }
            });
            box.addView(card);
        }
    }
}
