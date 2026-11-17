package com.sennara.app.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.sennara.app.R;
import com.sennara.app.data.Db;
import com.sennara.app.data.Models;
import com.sennara.app.util.Credits;
import com.sennara.app.util.Images;
import com.sennara.app.util.Labels;
import com.sennara.app.util.Ui;

public class TutorialDetailActivity extends BaseActivity implements View.OnClickListener {
    private Models.Tutorial tutorial;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        tutorial = Db.get(this).tutorial(getIntent().getIntExtra("id", 0));
        if (tutorial == null) {
            finish();
            return;
        }
        setContentView(R.layout.activity_tutorial);
        edgeToEdge(findViewById(R.id.root), findViewById(R.id.photo_actions), findViewById(R.id.bottom_area), true);
        Images.load((ImageView) findViewById(R.id.photo), tutorial.image, 1080);
        photoHeader(260, tutorial.title(lang()));

        TextView level = findViewById(R.id.level);
        level.setText(Labels.level(this, tutorial.level) + " · " + getString(R.string.minutes_read, tutorial.minutes));
        TextView title = findViewById(R.id.title);
        title.setText(tutorial.title(lang()));
        TextView credit = findViewById(R.id.credit);
        credit.setText(Credits.line(this, tutorial.image));

        LinearLayout steps = findViewById(R.id.steps);
        String[] lines = tutorial.body(lang()).split("\n");
        int number = 1;
        for (String line : lines) {
            if (line.trim().isEmpty()) {
                continue;
            }
            View row = getLayoutInflater().inflate(R.layout.item_step, steps, false);
            TextView numberView = row.findViewById(R.id.step_n);
            numberView.setText(String.valueOf(number));
            TextView text = row.findViewById(R.id.step_text);
            text.setText(line.trim());
            steps.addView(row);
            number++;
            Ui.rise(row, 60L * number);
        }

        findViewById(R.id.back).setOnClickListener(this);
        findViewById(R.id.video).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.back) {
            finish();
        } else if (v.getId() == R.id.video) {
            openUrl(tutorial.videoUrl);
        }
    }
}
