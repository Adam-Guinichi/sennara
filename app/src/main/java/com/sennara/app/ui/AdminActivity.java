package com.sennara.app.ui;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.sennara.app.R;
import com.sennara.app.data.Api;
import com.sennara.app.data.Session;
import com.sennara.app.data.SyncManager;
import com.sennara.app.ui.adapters.RowAdapter;
import com.sennara.app.util.Labels;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class AdminActivity extends BaseActivity {
    private static final String[] ROLES = {"fisher", "seller", "admin"};
    private JSONObject data;
    private RowAdapter adapter;
    private ChipGroup tabs;
    private View header;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!Session.isAdmin(this)) {
            finish();
            return;
        }
        setupSimple(R.layout.activity_admin, getString(R.string.admin_panel));
        ListView list = findViewById(R.id.list);
        edgeToEdge(findViewById(R.id.root), findViewById(R.id.top_bar), list, false);

        header = getLayoutInflater().inflate(R.layout.header_admin, list, false);
        list.addHeaderView(header, null, false);
        adapter = new RowAdapter(this);
        list.setAdapter(adapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Object item = parent.getItemAtPosition(position);
                if (item instanceof RowAdapter.Row) {
                    RowAdapter.Row row = (RowAdapter.Row) item;
                    onRowClicked((JSONObject) row.tag);
                }
            }
        });
        tabs = header.findViewById(R.id.tabs);
        tabs.setOnCheckedStateChangeListener(new ChipGroup.OnCheckedStateChangeListener() {
            @Override
            public void onCheckedChanged(ChipGroup group, List<Integer> checkedIds) {
                showData();
            }
        });

        HashMap<String, String> params = new HashMap<>();
        params.put("action", "overview");
        send(params, false);
    }

    private String tab() {
        View chip = tabs.findViewById(tabs.getCheckedChipId());
        if (chip == null) {
            return "spots";
        }
        return String.valueOf(chip.getTag());
    }

    private void send(HashMap<String, String> params, final boolean changed) {
        Api.call(this, "admin.php", params, new Api.Listener() {
            @Override
            public void onResult(JSONObject answer, String error) {
                if (isDestroyed()) {
                    return;
                }
                if (error != null) {
                    toast(Api.message(AdminActivity.this, error));
                    return;
                }
                data = answer;
                showData();
                if (changed) {
                    toast(R.string.saved);
                    SyncManager.sync(AdminActivity.this, null);
                }
            }
        });
    }

    private void showData() {
        if (data == null) {
            return;
        }
        JSONObject stats = data.optJSONObject("stats");
        if (stats != null) {
            setNumber(R.id.st_users, stats.optInt("users"));
            setNumber(R.id.st_spots, stats.optInt("spots"));
            setNumber(R.id.st_shops, stats.optInt("shops"));
            setNumber(R.id.st_catches, stats.optInt("catches"));
            setNumber(R.id.st_reviews, stats.optInt("reviews"));
        }

        String tab = tab();
        JSONArray array;
        if (tab.equals("users")) {
            array = data.optJSONArray("users");
        } else if (tab.equals("reviews")) {
            array = data.optJSONArray("reviews");
        } else {
            array = data.optJSONArray("pending_" + tab);
        }

        ArrayList<RowAdapter.Row> rows = new ArrayList<>();
        for (int i = 0; array != null && i < array.length(); i++) {
            JSONObject o = array.optJSONObject(i);
            if (o == null) {
                continue;
            }
            RowAdapter.Row row = new RowAdapter.Row();
            row.tag = o;
            if (tab.equals("users")) {
                String role = o.optString("role");
                row.icon = R.drawable.ic_person;
                row.title = o.optString("name");
                row.subtitle = o.optString("email");
                if (role.equals("admin")) {
                    row.setBadge(Labels.role(this, role), R.color.lv_unsafe, R.color.lv_unsafe_soft);
                } else if (role.equals("seller")) {
                    row.setBadge(Labels.role(this, role), R.color.lv_great, R.color.lv_great_soft);
                } else {
                    row.setBadge(Labels.role(this, role), R.color.primary, R.color.primary_soft);
                }
            } else if (tab.equals("reviews")) {
                row.icon = R.drawable.ic_star;
                row.title = o.optString("spot") + " · " + o.optInt("rating") + "★";
                row.subtitle = getString(R.string.by_author, o.optString("author"));
                row.extra = o.optString("comment");
                row.trailing = R.drawable.ic_delete;
            } else {
                row.image = o.optString("image");
                if (tab.equals("shops")) {
                    row.icon = R.drawable.ic_store;
                } else {
                    row.icon = R.drawable.ic_place;
                }
                row.title = o.optString("name");
                row.subtitle = o.optString("city") + " · " + getString(R.string.by_author, o.optString("author"));
                if (o.isNull("description")) {
                    row.extra = "";
                } else {
                    row.extra = o.optString("description");
                }
                row.setBadge(getString(R.string.waiting), R.color.lv_fair, R.color.lv_fair_soft);
            }
            rows.add(row);
        }
        adapter.setRows(rows);
        if (rows.isEmpty()) {
            header.findViewById(R.id.empty).setVisibility(View.VISIBLE);
        } else {
            header.findViewById(R.id.empty).setVisibility(View.GONE);
        }
    }

    private void setNumber(int id, int value) {
        TextView view = header.findViewById(id);
        view.setText(String.valueOf(value));
    }

    private void onRowClicked(final JSONObject o) {
        final String id = String.valueOf(o.optInt("id"));
        String tab = tab();
        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(this);

        if (tab.equals("users")) {
            String[] labels = new String[ROLES.length];
            int current = 0;
            for (int i = 0; i < ROLES.length; i++) {
                labels[i] = Labels.role(this, ROLES[i]);
                if (ROLES[i].equals(o.optString("role"))) {
                    current = i;
                }
            }
            dialog.setTitle(getString(R.string.role_of, o.optString("name")));
            dialog.setSingleChoiceItems(labels, current, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface d, int which) {
                    d.dismiss();
                    HashMap<String, String> params = new HashMap<>();
                    params.put("action", "set_role");
                    params.put("id", id);
                    params.put("role", ROLES[which]);
                    send(params, true);
                }
            });
            dialog.setNegativeButton(R.string.cancel, null);

        } else if (tab.equals("reviews")) {
            dialog.setTitle(o.optString("spot"));
            if (o.optString("comment").isEmpty()) {
                dialog.setMessage(getString(R.string.by_author, o.optString("author")));
            } else {
                dialog.setMessage(o.optString("comment"));
            }
            dialog.setNegativeButton(R.string.cancel, null);
            dialog.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface d, int which) {
                    HashMap<String, String> params = new HashMap<>();
                    params.put("action", "delete_review");
                    params.put("id", id);
                    send(params, true);
                }
            });

        } else {
            final String kind;
            if (tab.equals("shops")) {
                kind = "shop";
            } else {
                kind = "spot";
            }
            String description = "";
            if (!o.isNull("description")) {
                description = o.optString("description");
            }
            dialog.setTitle(o.optString("name"));
            if (description.isEmpty()) {
                dialog.setMessage(o.optString("city"));
            } else {
                dialog.setMessage(description);
            }
            dialog.setNeutralButton(R.string.cancel, null);
            dialog.setNegativeButton(R.string.reject, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface d, int which) {
                    HashMap<String, String> params = new HashMap<>();
                    params.put("action", "reject");
                    params.put("kind", kind);
                    params.put("id", id);
                    send(params, true);
                }
            });
            dialog.setPositiveButton(R.string.approve, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface d, int which) {
                    HashMap<String, String> params = new HashMap<>();
                    params.put("action", "approve");
                    params.put("kind", kind);
                    params.put("id", id);
                    send(params, true);
                }
            });
        }
        dialog.show();
    }
}
