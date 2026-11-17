package com.sennara.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.material.chip.ChipGroup;
import com.sennara.app.R;
import com.sennara.app.data.Db;
import com.sennara.app.data.Models;
import com.sennara.app.ui.adapters.RowAdapter;
import com.sennara.app.util.Geo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class ShopsActivity extends TabActivity {
    private RowAdapter adapter;
    private EditText search;
    private ChipGroup filters;
    private double[] here;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupTab(R.layout.activity_shops, R.id.nav_shops);
        search = findViewById(R.id.search);
        filters = findViewById(R.id.filters);

        ListView list = findViewById(R.id.list);
        adapter = new RowAdapter(this);
        list.setAdapter(adapter);
        list.setEmptyView(findViewById(R.id.empty));
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Models.Shop shop = (Models.Shop) adapter.getItem(position).tag;
                Intent intent = new Intent(ShopsActivity.this, ShopDetailActivity.class);
                intent.putExtra("id", shop.id);
                startActivity(intent);
            }
        });

        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                reload();
            }
        });
        filters.setOnCheckedStateChangeListener(new ChipGroup.OnCheckedStateChangeListener() {
            @Override
            public void onCheckedChanged(ChipGroup group, List<Integer> checkedIds) {
                reload();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        reload();
        Geo.refresh(this, new Runnable() {
            @Override
            public void run() {
                reload();
            }
        });
    }

    private double distance(Models.Shop shop) {
        return Geo.km(here[0], here[1], shop.lat, shop.lng);
    }

    private void reload() {
        String q = search.getText().toString().trim().toLowerCase(Locale.ROOT);
        View chip = filters.findViewById(filters.getCheckedChipId());
        boolean onlyWithProducts = chip != null && "products".equals(chip.getTag());
        here = Geo.here(this);

        ArrayList<Models.Shop> shops = new ArrayList<>();
        for (Models.Shop shop : Db.get(this).shops()) {
            if (onlyWithProducts && shop.productCount == 0) {
                continue;
            }
            if (found(shop.name, q) || found(shop.city, q) || found(shop.address, q) || found(shop.description, q)) {
                shops.add(shop);
            }
        }

        Collections.sort(shops, new Comparator<Models.Shop>() {
            @Override
            public int compare(Models.Shop a, Models.Shop b) {
                return Double.compare(distance(a), distance(b));
            }
        });

        ArrayList<RowAdapter.Row> rows = new ArrayList<>();
        for (Models.Shop shop : shops) {
            RowAdapter.Row row = new RowAdapter.Row();
            row.icon = R.drawable.ic_store;
            row.title = shop.name;
            row.subtitle = getString(R.string.shop_sub, shop.city, Geo.kmText(distance(shop)));
            if (shop.phone.isEmpty()) {
                row.extra = getString(R.string.no_phone);
            } else {
                row.extra = shop.phone;
            }
            if (shop.productCount > 0) {
                row.setBadge(getString(R.string.product_count, shop.productCount), R.color.lv_great, R.color.lv_great_soft);
            } else if (!shop.osmId.isEmpty()) {
                row.setBadge(getString(R.string.osm_badge), R.color.text2, R.color.surface2);
            }
            row.trailing = R.drawable.ic_chevron;
            row.tag = shop;
            rows.add(row);
        }
        adapter.setRows(rows);
        TextView count = findViewById(R.id.count);
        count.setText(getString(R.string.shops_count, rows.size()));
    }

    @Override
    protected void onReselected() {
        ListView list = findViewById(R.id.list);
        list.smoothScrollToPosition(0);
    }
}
