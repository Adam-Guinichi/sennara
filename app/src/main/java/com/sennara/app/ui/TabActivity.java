package com.sennara.app.ui;

import android.content.Intent;
import android.view.MenuItem;
import android.view.View;

import androidx.activity.OnBackPressedCallback;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.sennara.app.R;

public abstract class TabActivity extends BaseActivity {
    protected BottomNavigationView nav;
    private int navItem;

    protected void setupTab(int layout, int item) {
        setContentView(layout);
        navItem = item;
        View root = findViewById(R.id.root);
        nav = findViewById(R.id.bottom_nav);
        edgeToEdge(root, root, null, false);
        nav.setSelectedItemId(navItem);

        nav.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem menuItem) {
                int id = menuItem.getItemId();
                if (id == navItem) {
                    return true;
                }
                Class<?> screen;
                if (id == R.id.nav_home) {
                    screen = HomeActivity.class;
                } else if (id == R.id.nav_spots) {
                    screen = SpotsActivity.class;
                } else if (id == R.id.nav_fish) {
                    screen = FishActivity.class;
                } else if (id == R.id.nav_shops) {
                    screen = ShopsActivity.class;
                } else {
                    screen = LogbookActivity.class;
                }
                goTo(screen);
                return true;
            }
        });

        nav.setOnItemReselectedListener(new NavigationBarView.OnItemReselectedListener() {
            @Override
            public void onNavigationItemReselected(MenuItem menuItem) {
                onReselected();
            }
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (navItem == R.id.nav_home) {
                    moveTaskToBack(true);
                } else {
                    goTo(HomeActivity.class);
                }
            }
        });
    }

    private void goTo(Class<?> screen) {
        Intent intent = new Intent(this, screen);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(intent);
        overridePendingTransition(0, 0);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (nav != null && nav.getSelectedItemId() != navItem) {
            nav.getMenu().findItem(navItem).setChecked(true);
        }
    }

    protected void onReselected() {
    }
}
