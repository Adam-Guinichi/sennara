package com.sennara.app.ui;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.sennara.app.R;
import com.sennara.app.data.Api;
import com.sennara.app.data.Session;
import com.sennara.app.data.SyncManager;
import com.sennara.app.ui.adapters.RowAdapter;
import com.sennara.app.util.Fmt;
import com.sennara.app.util.Geo;
import com.sennara.app.util.Labels;
import com.sennara.app.util.Ui;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

public class SellerActivity extends BaseActivity implements View.OnClickListener {
    private static final String[] CATEGORIES = {"bait", "rod", "reel", "line", "hook", "other"};
    private JSONObject shop;
    private JSONArray products = new JSONArray();
    private RowAdapter adapter;
    private View header;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!Session.isSeller(this) && !Session.isAdmin(this)) {
            finish();
            return;
        }
        setupSimple(R.layout.activity_seller, getString(R.string.my_shop));

        ListView list = findViewById(R.id.list);
        header = getLayoutInflater().inflate(R.layout.header_seller, list, false);
        list.addHeaderView(header, null, false);
        adapter = new RowAdapter(this);
        list.setAdapter(adapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Object item = parent.getItemAtPosition(position);
                if (item instanceof RowAdapter.Row) {
                    RowAdapter.Row row = (RowAdapter.Row) item;
                    editProduct((JSONObject) row.tag);
                }
            }
        });
        header.findViewById(R.id.btn_edit_shop).setOnClickListener(this);
        findViewById(R.id.fab).setOnClickListener(this);

        HashMap<String, String> params = new HashMap<>();
        params.put("action", "mine");
        send(params, false);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btn_edit_shop) {
            editShop();
        } else if (v.getId() == R.id.fab) {
            editProduct(null);
        }
    }

    private void send(HashMap<String, String> params, final boolean changed) {
        Api.call(this, "shop.php", params, new Api.Listener() {
            @Override
            public void onResult(JSONObject data, String error) {
                if (isDestroyed()) {
                    return;
                }
                if (error != null) {
                    toast(Api.message(SellerActivity.this, error));
                    return;
                }
                shop = data.optJSONObject("shop");
                products = data.optJSONArray("products");
                if (products == null) {
                    products = new JSONArray();
                }
                showShop();
                if (changed) {
                    toast(R.string.saved);
                    SyncManager.sync(SellerActivity.this, null);
                }
            }
        });
    }

    private void showShop() {
        TextView name = header.findViewById(R.id.my_shop_name);
        TextView state = header.findViewById(R.id.my_shop_status);
        TextView info = header.findViewById(R.id.my_shop_info);
        View fab = findViewById(R.id.fab);
        if (shop == null) {
            name.setText(R.string.no_shop_yet);
            info.setText(R.string.no_shop_help);
            state.setVisibility(View.GONE);
            fab.setVisibility(View.GONE);
        } else {
            name.setText(shop.optString("name"));
            info.setText(shop.optString("address") + ", " + shop.optString("city") + "\n" + shop.optString("phone"));
            state.setVisibility(View.VISIBLE);
            if (shop.optInt("approved") == 1) {
                state.setText(R.string.shop_approved);
                state.setTextColor(Ui.color(this, R.color.lv_great));
                state.setBackground(Ui.rounded(Ui.color(this, R.color.lv_great_soft), dp(10)));
            } else {
                state.setText(R.string.shop_waiting);
                state.setTextColor(Ui.color(this, R.color.lv_fair));
                state.setBackground(Ui.rounded(Ui.color(this, R.color.lv_fair_soft), dp(10)));
            }
            fab.setVisibility(View.VISIBLE);
        }
        TextView count = header.findViewById(R.id.product_count);
        count.setText(getString(R.string.product_count, products.length()));

        ArrayList<RowAdapter.Row> rows = new ArrayList<>();
        for (int i = 0; i < products.length(); i++) {
            JSONObject p = products.optJSONObject(i);
            if (p == null) {
                continue;
            }
            RowAdapter.Row row = new RowAdapter.Row();
            if (p.optString("category").equals("bait")) {
                row.icon = R.drawable.ic_hook;
            } else {
                row.icon = R.drawable.ic_store;
            }
            row.title = p.optString("name");
            row.subtitle = Labels.category(this, p.optString("category"));
            if (p.optInt("in_stock") == 1) {
                row.extra = getString(R.string.in_stock);
                row.setBadge(Fmt.price(p.optDouble("price")), R.color.primary, R.color.primary_soft);
            } else {
                row.extra = getString(R.string.out_of_stock);
                row.setBadge(Fmt.price(p.optDouble("price")), R.color.lv_fair, R.color.lv_fair_soft);
            }
            row.trailing = R.drawable.ic_edit;
            row.tag = p;
            rows.add(row);
        }
        adapter.setRows(rows);
    }

    private void editShop() {
        View form = getLayoutInflater().inflate(R.layout.dialog_shop, null);
        final EditText name = form.findViewById(R.id.d_name);
        final EditText city = form.findViewById(R.id.d_city);
        final EditText address = form.findViewById(R.id.d_address);
        final EditText phone = form.findViewById(R.id.d_phone);
        final EditText description = form.findViewById(R.id.d_desc);
        final CheckBox useLocation = form.findViewById(R.id.d_use_location);
        if (shop != null) {
            name.setText(shop.optString("name"));
            city.setText(shop.optString("city"));
            address.setText(shop.optString("address"));
            phone.setText(shop.optString("phone"));
            description.setText(shop.optString("description"));
        } else {
            phone.setText(Session.phone(this));
            city.setText(Session.city(this));
            useLocation.setChecked(true);
        }
        Geo.refresh(this, null);

        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(this);
        dialog.setTitle(R.string.edit_shop);
        dialog.setView(form);
        dialog.setNegativeButton(R.string.cancel, null);
        dialog.setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface d, int which) {
                if (name.getText().toString().trim().isEmpty()) {
                    toast(R.string.fill_all_fields);
                    return;
                }
                double lat;
                double lng;
                if (useLocation.isChecked() || shop == null) {
                    double[] here = Geo.here(SellerActivity.this);
                    lat = here[0];
                    lng = here[1];
                } else {
                    lat = shop.optDouble("lat");
                    lng = shop.optDouble("lng");
                }
                HashMap<String, String> params = new HashMap<>();
                params.put("action", "save");
                params.put("name", name.getText().toString().trim());
                params.put("city", city.getText().toString().trim());
                params.put("address", address.getText().toString().trim());
                params.put("phone", phone.getText().toString().trim());
                params.put("description", description.getText().toString().trim());
                params.put("lat", String.valueOf(lat));
                params.put("lng", String.valueOf(lng));
                send(params, true);
            }
        });
        dialog.show();
    }

    private void editProduct(final JSONObject product) {
        View form = getLayoutInflater().inflate(R.layout.dialog_product, null);
        final EditText name = form.findViewById(R.id.p_name);
        final EditText price = form.findViewById(R.id.p_price);
        final Spinner category = form.findViewById(R.id.p_category);
        final MaterialSwitch stock = form.findViewById(R.id.p_stock);

        String[] labels = new String[CATEGORIES.length];
        for (int i = 0; i < CATEGORIES.length; i++) {
            labels[i] = Labels.category(this, CATEGORIES[i]);
        }
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, labels);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        category.setAdapter(categoryAdapter);

        if (product != null) {
            name.setText(product.optString("name"));
            price.setText(Fmt.num(product.optDouble("price"), 3));
            stock.setChecked(product.optInt("in_stock") == 1);
            for (int i = 0; i < CATEGORIES.length; i++) {
                if (CATEGORIES[i].equals(product.optString("category"))) {
                    category.setSelection(i);
                }
            }
        }

        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(this);
        if (product == null) {
            dialog.setTitle(R.string.add_product);
        } else {
            dialog.setTitle(R.string.edit_product);
        }
        dialog.setView(form);
        dialog.setNegativeButton(R.string.cancel, null);
        dialog.setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface d, int which) {
                if (name.getText().toString().trim().isEmpty()) {
                    toast(R.string.fill_all_fields);
                    return;
                }
                HashMap<String, String> params = new HashMap<>();
                params.put("action", "product_save");
                if (product == null) {
                    params.put("id", "0");
                } else {
                    params.put("id", String.valueOf(product.optInt("id")));
                }
                params.put("name", name.getText().toString().trim());
                params.put("price", price.getText().toString().trim().replace(',', '.'));
                params.put("category", CATEGORIES[category.getSelectedItemPosition()]);
                if (stock.isChecked()) {
                    params.put("in_stock", "1");
                } else {
                    params.put("in_stock", "0");
                }
                send(params, true);
            }
        });
        if (product != null) {
            dialog.setNeutralButton(R.string.delete, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface d, int which) {
                    HashMap<String, String> params = new HashMap<>();
                    params.put("action", "product_delete");
                    params.put("id", String.valueOf(product.optInt("id")));
                    send(params, true);
                }
            });
        }
        dialog.show();
    }
}
