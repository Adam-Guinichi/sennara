package com.sennara.app.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.sennara.app.R;
import com.sennara.app.data.Db;
import com.sennara.app.data.Models;
import com.sennara.app.util.Fmt;
import com.sennara.app.util.Geo;
import com.sennara.app.util.Labels;
import com.sennara.app.util.Ui;

import java.util.ArrayList;

public class ShopDetailActivity extends BaseActivity implements View.OnClickListener {
    private Models.Shop shop;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        shop = Db.get(this).shop(getIntent().getIntExtra("id", 0));
        if (shop == null) {
            finish();
            return;
        }
        setupSimple(R.layout.activity_shop, "");

        TextView name = findViewById(R.id.shop_name);
        name.setText(shop.name);
        String where = shop.city;
        if (!shop.address.isEmpty()) {
            where = shop.address + ", " + shop.city;
        }
        TextView whereView = findViewById(R.id.shop_where);
        whereView.setText(getString(R.string.shop_sub, where, Geo.kmText(Geo.kmFromHere(this, shop.lat, shop.lng))));
        TextView description = findViewById(R.id.shop_desc);
        description.setText(shop.description);
        TextView source = findViewById(R.id.shop_source);
        if (shop.osmId.isEmpty()) {
            source.setText(R.string.source_seller);
        } else {
            source.setText(getString(R.string.source_osm, shop.osmId));
        }

        boolean hasPhone = !shop.phone.isEmpty();
        View call = findViewById(R.id.call);
        View sms = findViewById(R.id.sms);
        View web = findViewById(R.id.web);
        if (hasPhone) {
            call.setVisibility(View.VISIBLE);
            sms.setVisibility(View.VISIBLE);
        } else {
            call.setVisibility(View.GONE);
            sms.setVisibility(View.GONE);
        }
        if (shop.website.isEmpty()) {
            web.setVisibility(View.GONE);
        } else {
            web.setVisibility(View.VISIBLE);
        }
        call.setOnClickListener(this);
        sms.setOnClickListener(this);
        web.setOnClickListener(this);
        source.setOnClickListener(this);
        findViewById(R.id.route).setOnClickListener(this);

        showProducts(hasPhone);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.call) {
            dial(shop.phone);
        } else if (id == R.id.sms) {
            sms(shop.phone, getString(R.string.sms_hello, shop.name));
        } else if (id == R.id.web) {
            openUrl(shop.website);
        } else if (id == R.id.route) {
            openDirections(shop.lat, shop.lng, shop.name);
        } else if (id == R.id.shop_source && !shop.osmId.isEmpty()) {
            openUrl("https://www.openstreetmap.org/" + shop.osmId);
        }
    }

    private void showProducts(boolean hasPhone) {
        LinearLayout box = findViewById(R.id.products);
        ArrayList<Models.Product> products = Db.get(this).products(shop.id);
        for (final Models.Product product : products) {
            View row = getLayoutInflater().inflate(R.layout.item_sky, box, false);
            ImageView icon = row.findViewById(R.id.sky_icon);
            if (product.category.equals("bait")) {
                icon.setImageResource(R.drawable.ic_hook);
            } else {
                icon.setImageResource(R.drawable.ic_store);
            }
            String stock;
            if (product.inStock) {
                stock = getString(R.string.in_stock);
            } else {
                stock = getString(R.string.out_of_stock);
            }
            TextView label = row.findViewById(R.id.sky_label);
            label.setText(product.name + "\n" + Labels.category(this, product.category) + " · " + stock);
            TextView price = row.findViewById(R.id.sky_time);
            price.setText(Fmt.price(product.price));
            if (product.inStock) {
                price.setTextColor(Ui.color(this, R.color.text));
            } else {
                price.setTextColor(Ui.color(this, R.color.text3));
            }
            if (hasPhone) {
                row.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        sms(shop.phone, getString(R.string.sms_product, product.name));
                    }
                });
            }
            box.addView(row);
        }

        TextView noProducts = findViewById(R.id.no_products);
        if (products.isEmpty()) {
            noProducts.setVisibility(View.VISIBLE);
        } else {
            noProducts.setVisibility(View.GONE);
        }
        if (hasPhone) {
            noProducts.setText(R.string.no_products_call);
        } else {
            noProducts.setText(R.string.no_products);
        }
    }
}
