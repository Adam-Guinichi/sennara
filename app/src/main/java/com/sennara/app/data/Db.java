package com.sennara.app.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.sennara.app.forecast.Conditions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class Db extends SQLiteOpenHelper {
    private static final String DB_NAME = "sennara.db";
    private static final int DB_VERSION = 2;

    private static Db instance;

    public static synchronized Db get(Context c) {
        if (instance == null) {
            instance = new Db(c.getApplicationContext());
        }
        return instance;
    }

    private Db(Context c) {
        super(c, DB_NAME, null, DB_VERSION);
    }

    // ================================================================ creating the tables

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE spots (id INTEGER PRIMARY KEY, slug TEXT, name TEXT, name_ar TEXT, city TEXT, city_ar TEXT,"
                + " type TEXT, best_time TEXT, desc_en TEXT, desc_fr TEXT, desc_ar TEXT, image TEXT, top_species TEXT,"
                + " lat REAL, lng REAL, avg_rating REAL, n_reviews INTEGER, n_catches INTEGER)");

        db.execSQL("CREATE TABLE fish (id INTEGER PRIMARY KEY, slug TEXT, sci_name TEXT, name_en TEXT, name_fr TEXT, name_ar TEXT,"
                + " family TEXT, habitats TEXT, baits TEXT, techniques TEXT, size_rule TEXT, iucn TEXT,"
                + " about_en TEXT, about_fr TEXT, about_ar TEXT, image TEXT, wikidata TEXT,"
                + " month_from INTEGER, month_to INTEGER, min_size_cm INTEGER, min_weight_g INTEGER, max_cm INTEGER)");

        db.execSQL("CREATE TABLE spot_fish (spot_id INTEGER, fish_id INTEGER)");

        db.execSQL("CREATE TABLE shops (id INTEGER PRIMARY KEY, owner_id INTEGER, osm_id TEXT, name TEXT, city TEXT,"
                + " address TEXT, phone TEXT, website TEXT, description TEXT, lat REAL, lng REAL)");

        db.execSQL("CREATE TABLE products (id INTEGER PRIMARY KEY, shop_id INTEGER, name TEXT, category TEXT,"
                + " price REAL, in_stock INTEGER)");

        db.execSQL("CREATE TABLE tutorials (id INTEGER PRIMARY KEY, slug TEXT, level TEXT, title_en TEXT, title_fr TEXT,"
                + " title_ar TEXT, body_en TEXT, body_fr TEXT, body_ar TEXT, image TEXT, video_url TEXT,"
                + " minutes INTEGER, sort_order INTEGER)");

        db.execSQL("CREATE TABLE rules (id INTEGER PRIMARY KEY, kind TEXT, title_en TEXT, title_fr TEXT, title_ar TEXT,"
                + " body_en TEXT, body_fr TEXT, body_ar TEXT, source_url TEXT, sort_order INTEGER)");

        db.execSQL("CREATE TABLE catches (uuid TEXT PRIMARY KEY, user_id INTEGER, spot_id INTEGER, spot_name TEXT,"
                + " fish_id INTEGER, species TEXT, weight_g INTEGER, length_cm INTEGER, bait TEXT, released INTEGER,"
                + " note TEXT, photo TEXT, lat REAL, lng REAL, caught_at INTEGER, updated_at INTEGER,"
                + " deleted INTEGER DEFAULT 0, synced INTEGER DEFAULT 0)");

        db.execSQL("CREATE TABLE favorites (user_id INTEGER, spot_id INTEGER, fav INTEGER, synced INTEGER,"
                + " PRIMARY KEY (user_id, spot_id))");

        db.execSQL("CREATE TABLE weather (spot_id INTEGER, t INTEGER, temp REAL, rain_prob REAL, rain REAL, code INTEGER,"
                + " cloud REAL, pressure REAL, wind REAL, wind_dir REAL, gust REAL, wave REAL, wave_period REAL, sst REAL,"
                + " PRIMARY KEY (spot_id, t))");

        db.execSQL("CREATE TABLE kv (k TEXT PRIMARY KEY, v TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        String[] tables = {"spots", "fish", "spot_fish", "shops", "products", "tutorials", "rules",
                "catches", "favorites", "weather", "kv"};
        for (String table : tables) {
            db.execSQL("DROP TABLE IF EXISTS " + table);
        }
        onCreate(db);
    }

    // ================================================================ copy of the server data

    public void replaceAll(JSONObject data) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete("spots", null, null);
            JSONArray spots = data.optJSONArray("spots");
            for (int i = 0; spots != null && i < spots.length(); i++) {
                insertSpot(db, spots.optJSONObject(i));
            }

            db.delete("fish", null, null);
            JSONArray fish = data.optJSONArray("fish");
            for (int i = 0; fish != null && i < fish.length(); i++) {
                insertFish(db, fish.optJSONObject(i));
            }

            db.delete("spot_fish", null, null);
            JSONArray links = data.optJSONArray("spot_fish");
            for (int i = 0; links != null && i < links.length(); i++) {
                JSONObject o = links.optJSONObject(i);
                ContentValues v = new ContentValues();
                v.put("spot_id", o.optInt("spot_id"));
                v.put("fish_id", o.optInt("fish_id"));
                db.insert("spot_fish", null, v);
            }

            db.delete("shops", null, null);
            JSONArray shops = data.optJSONArray("shops");
            for (int i = 0; shops != null && i < shops.length(); i++) {
                insertShop(db, shops.optJSONObject(i));
            }

            db.delete("products", null, null);
            JSONArray products = data.optJSONArray("products");
            for (int i = 0; products != null && i < products.length(); i++) {
                JSONObject o = products.optJSONObject(i);
                ContentValues v = new ContentValues();
                v.put("id", o.optInt("id"));
                v.put("shop_id", o.optInt("shop_id"));
                v.put("name", text(o, "name"));
                v.put("category", text(o, "category"));
                v.put("price", o.optDouble("price", 0));
                v.put("in_stock", o.optInt("in_stock"));
                db.insert("products", null, v);
            }

            db.delete("tutorials", null, null);
            JSONArray tutorials = data.optJSONArray("tutorials");
            for (int i = 0; tutorials != null && i < tutorials.length(); i++) {
                insertTutorial(db, tutorials.optJSONObject(i));
            }

            db.delete("rules", null, null);
            JSONArray rules = data.optJSONArray("rules");
            for (int i = 0; rules != null && i < rules.length(); i++) {
                JSONObject o = rules.optJSONObject(i);
                ContentValues v = new ContentValues();
                v.put("id", o.optInt("id"));
                v.put("kind", text(o, "kind"));
                v.put("title_en", text(o, "title_en"));
                v.put("title_fr", text(o, "title_fr"));
                v.put("title_ar", text(o, "title_ar"));
                v.put("body_en", text(o, "body_en"));
                v.put("body_fr", text(o, "body_fr"));
                v.put("body_ar", text(o, "body_ar"));
                v.put("source_url", text(o, "source_url"));
                v.put("sort_order", o.optInt("sort_order"));
                db.insert("rules", null, v);
            }

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    private void insertSpot(SQLiteDatabase db, JSONObject o) {
        ContentValues v = new ContentValues();
        v.put("id", o.optInt("id"));
        v.put("slug", text(o, "slug"));
        v.put("name", text(o, "name"));
        v.put("name_ar", text(o, "name_ar"));
        v.put("city", text(o, "city"));
        v.put("city_ar", text(o, "city_ar"));
        v.put("type", text(o, "type"));
        v.put("best_time", text(o, "best_time"));
        v.put("desc_en", text(o, "desc_en"));
        v.put("desc_fr", text(o, "desc_fr"));
        v.put("desc_ar", text(o, "desc_ar"));
        v.put("image", text(o, "image"));
        v.put("top_species", text(o, "top_species"));
        v.put("lat", o.optDouble("lat", 0));
        v.put("lng", o.optDouble("lng", 0));
        v.put("avg_rating", o.optDouble("avg_rating", 0));
        v.put("n_reviews", o.optInt("n_reviews"));
        v.put("n_catches", o.optInt("n_catches"));
        db.insert("spots", null, v);
    }

    private void insertFish(SQLiteDatabase db, JSONObject o) {
        ContentValues v = new ContentValues();
        v.put("id", o.optInt("id"));
        v.put("slug", text(o, "slug"));
        v.put("sci_name", text(o, "sci_name"));
        v.put("name_en", text(o, "name_en"));
        v.put("name_fr", text(o, "name_fr"));
        v.put("name_ar", text(o, "name_ar"));
        v.put("family", text(o, "family"));
        v.put("habitats", text(o, "habitats"));
        v.put("baits", text(o, "baits"));
        v.put("techniques", text(o, "techniques"));
        v.put("size_rule", text(o, "size_rule"));
        v.put("iucn", text(o, "iucn"));
        v.put("about_en", text(o, "about_en"));
        v.put("about_fr", text(o, "about_fr"));
        v.put("about_ar", text(o, "about_ar"));
        v.put("image", text(o, "image"));
        v.put("wikidata", text(o, "wikidata"));
        v.put("month_from", o.optInt("month_from", 1));
        v.put("month_to", o.optInt("month_to", 12));
        v.put("min_size_cm", o.optInt("min_size_cm"));
        v.put("min_weight_g", o.optInt("min_weight_g"));
        v.put("max_cm", o.optInt("max_cm"));
        db.insert("fish", null, v);
    }

    private void insertShop(SQLiteDatabase db, JSONObject o) {
        ContentValues v = new ContentValues();
        v.put("id", o.optInt("id"));
        v.put("owner_id", o.optInt("owner_id"));
        v.put("osm_id", text(o, "osm_id"));
        v.put("name", text(o, "name"));
        v.put("city", text(o, "city"));
        v.put("address", text(o, "address"));
        v.put("phone", text(o, "phone"));
        v.put("website", text(o, "website"));
        v.put("description", text(o, "description"));
        v.put("lat", o.optDouble("lat", 0));
        v.put("lng", o.optDouble("lng", 0));
        db.insert("shops", null, v);
    }

    private void insertTutorial(SQLiteDatabase db, JSONObject o) {
        ContentValues v = new ContentValues();
        v.put("id", o.optInt("id"));
        v.put("slug", text(o, "slug"));
        v.put("level", text(o, "level"));
        v.put("title_en", text(o, "title_en"));
        v.put("title_fr", text(o, "title_fr"));
        v.put("title_ar", text(o, "title_ar"));
        v.put("body_en", text(o, "body_en"));
        v.put("body_fr", text(o, "body_fr"));
        v.put("body_ar", text(o, "body_ar"));
        v.put("image", text(o, "image"));
        v.put("video_url", text(o, "video_url"));
        v.put("minutes", o.optInt("minutes"));
        v.put("sort_order", o.optInt("sort_order"));
        db.insert("tutorials", null, v);
    }

    private static String text(JSONObject o, String key) {
        if (o.isNull(key)) {
            return "";
        }
        return o.optString(key);
    }

    public ArrayList<String> imagePaths() {
        ArrayList<String> list = new ArrayList<>();
        String[] tables = {"spots", "fish", "tutorials"};
        for (String table : tables) {
            Cursor c = getReadableDatabase().rawQuery("SELECT image FROM " + table + " WHERE image <> ''", null);
            try {
                while (c.moveToNext()) {
                    list.add(c.getString(0));
                }
            } finally {
                c.close();
            }
        }
        return list;
    }

    // ================================================================ spots

    public ArrayList<Models.Spot> spots() {
        ArrayList<Models.Spot> list = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery("SELECT * FROM spots ORDER BY id", null);
        try {
            while (c.moveToNext()) {
                list.add(readSpot(c));
            }
        } finally {
            c.close();
        }
        return list;
    }

    public Models.Spot spot(int id) {
        Cursor c = getReadableDatabase().rawQuery("SELECT * FROM spots WHERE id = ?", new String[]{String.valueOf(id)});
        try {
            if (c.moveToFirst()) {
                return readSpot(c);
            }
            return null;
        } finally {
            c.close();
        }
    }

    private Models.Spot readSpot(Cursor c) {
        Models.Spot s = new Models.Spot();
        s.id = getInt(c, "id");
        s.slug = getText(c, "slug");
        s.name = getText(c, "name");
        s.nameAr = getText(c, "name_ar");
        s.city = getText(c, "city");
        s.cityAr = getText(c, "city_ar");
        s.type = getText(c, "type");
        s.bestTime = getText(c, "best_time");
        s.descEn = getText(c, "desc_en");
        s.descFr = getText(c, "desc_fr");
        s.descAr = getText(c, "desc_ar");
        s.image = getText(c, "image");
        s.topSpecies = getText(c, "top_species");
        s.lat = getDouble(c, "lat");
        s.lng = getDouble(c, "lng");
        s.avgRating = getDouble(c, "avg_rating");
        s.nReviews = getInt(c, "n_reviews");
        s.nCatches = getInt(c, "n_catches");
        return s;
    }

    public HashMap<Integer, ArrayList<Integer>> spotFishMap() {
        HashMap<Integer, ArrayList<Integer>> map = new HashMap<>();
        Cursor c = getReadableDatabase().rawQuery("SELECT spot_id, fish_id FROM spot_fish", null);
        try {
            while (c.moveToNext()) {
                int spotId = c.getInt(0);
                int fishId = c.getInt(1);
                ArrayList<Integer> list = map.get(spotId);
                if (list == null) {
                    list = new ArrayList<>();
                    map.put(spotId, list);
                }
                list.add(fishId);
            }
        } finally {
            c.close();
        }
        return map;
    }

    public ArrayList<Models.Fish> fishAtSpot(int spotId) {
        ArrayList<Models.Fish> list = new ArrayList<>();
        String sql = "SELECT f.* FROM fish f JOIN spot_fish sf ON sf.fish_id = f.id WHERE sf.spot_id = ? ORDER BY f.id";
        Cursor c = getReadableDatabase().rawQuery(sql, new String[]{String.valueOf(spotId)});
        try {
            while (c.moveToNext()) {
                list.add(readFish(c));
            }
        } finally {
            c.close();
        }
        return list;
    }

    public ArrayList<Models.Spot> spotsWithFish(int fishId) {
        ArrayList<Models.Spot> list = new ArrayList<>();
        String sql = "SELECT s.* FROM spots s JOIN spot_fish sf ON sf.spot_id = s.id WHERE sf.fish_id = ?";
        Cursor c = getReadableDatabase().rawQuery(sql, new String[]{String.valueOf(fishId)});
        try {
            while (c.moveToNext()) {
                list.add(readSpot(c));
            }
        } finally {
            c.close();
        }
        return list;
    }

    // ================================================================ fish

    public ArrayList<Models.Fish> fish() {
        ArrayList<Models.Fish> list = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery("SELECT * FROM fish ORDER BY id", null);
        try {
            while (c.moveToNext()) {
                list.add(readFish(c));
            }
        } finally {
            c.close();
        }
        return list;
    }

    public Models.Fish fish(int id) {
        Cursor c = getReadableDatabase().rawQuery("SELECT * FROM fish WHERE id = ?", new String[]{String.valueOf(id)});
        try {
            if (c.moveToFirst()) {
                return readFish(c);
            }
            return null;
        } finally {
            c.close();
        }
    }

    private Models.Fish readFish(Cursor c) {
        Models.Fish f = new Models.Fish();
        f.id = getInt(c, "id");
        f.slug = getText(c, "slug");
        f.sci = getText(c, "sci_name");
        f.nameEn = getText(c, "name_en");
        f.nameFr = getText(c, "name_fr");
        f.nameAr = getText(c, "name_ar");
        f.family = getText(c, "family");
        f.habitats = getText(c, "habitats");
        f.baits = getText(c, "baits");
        f.techniques = getText(c, "techniques");
        f.sizeRule = getText(c, "size_rule");
        f.iucn = getText(c, "iucn");
        f.aboutEn = getText(c, "about_en");
        f.aboutFr = getText(c, "about_fr");
        f.aboutAr = getText(c, "about_ar");
        f.image = getText(c, "image");
        f.wikidata = getText(c, "wikidata");
        f.monthFrom = getInt(c, "month_from");
        f.monthTo = getInt(c, "month_to");
        f.minSizeCm = getInt(c, "min_size_cm");
        f.minWeightG = getInt(c, "min_weight_g");
        f.maxCm = getInt(c, "max_cm");
        return f;
    }

    // ================================================================ shops and products

    public ArrayList<Models.Shop> shops() {
        ArrayList<Models.Shop> list = new ArrayList<>();
        String sql = "SELECT s.*, (SELECT COUNT(*) FROM products p WHERE p.shop_id = s.id) AS n FROM shops s ORDER BY s.name";
        Cursor c = getReadableDatabase().rawQuery(sql, null);
        try {
            while (c.moveToNext()) {
                list.add(readShop(c));
            }
        } finally {
            c.close();
        }
        return list;
    }

    public Models.Shop shop(int id) {
        String sql = "SELECT s.*, (SELECT COUNT(*) FROM products p WHERE p.shop_id = s.id) AS n FROM shops s WHERE s.id = ?";
        Cursor c = getReadableDatabase().rawQuery(sql, new String[]{String.valueOf(id)});
        try {
            if (c.moveToFirst()) {
                return readShop(c);
            }
            return null;
        } finally {
            c.close();
        }
    }

    private Models.Shop readShop(Cursor c) {
        Models.Shop s = new Models.Shop();
        s.id = getInt(c, "id");
        s.ownerId = getInt(c, "owner_id");
        s.osmId = getText(c, "osm_id");
        s.name = getText(c, "name");
        s.city = getText(c, "city");
        s.address = getText(c, "address");
        s.phone = getText(c, "phone");
        s.website = getText(c, "website");
        s.description = getText(c, "description");
        s.lat = getDouble(c, "lat");
        s.lng = getDouble(c, "lng");
        s.productCount = getInt(c, "n");
        return s;
    }

    public ArrayList<Models.Product> products(int shopId) {
        ArrayList<Models.Product> list = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery("SELECT * FROM products WHERE shop_id = ? ORDER BY price",
                new String[]{String.valueOf(shopId)});
        try {
            while (c.moveToNext()) {
                Models.Product p = new Models.Product();
                p.id = getInt(c, "id");
                p.shopId = shopId;
                p.name = getText(c, "name");
                p.category = getText(c, "category");
                p.price = getDouble(c, "price");
                p.inStock = getInt(c, "in_stock") == 1;
                list.add(p);
            }
        } finally {
            c.close();
        }
        return list;
    }

    // ================================================================ tutorials and rules

    public ArrayList<Models.Tutorial> tutorials() {
        ArrayList<Models.Tutorial> list = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery("SELECT * FROM tutorials ORDER BY sort_order, id", null);
        try {
            while (c.moveToNext()) {
                list.add(readTutorial(c));
            }
        } finally {
            c.close();
        }
        return list;
    }

    public Models.Tutorial tutorial(int id) {
        Cursor c = getReadableDatabase().rawQuery("SELECT * FROM tutorials WHERE id = ?", new String[]{String.valueOf(id)});
        try {
            if (c.moveToFirst()) {
                return readTutorial(c);
            }
            return null;
        } finally {
            c.close();
        }
    }

    private Models.Tutorial readTutorial(Cursor c) {
        Models.Tutorial t = new Models.Tutorial();
        t.id = getInt(c, "id");
        t.minutes = getInt(c, "minutes");
        t.slug = getText(c, "slug");
        t.level = getText(c, "level");
        t.titleEn = getText(c, "title_en");
        t.titleFr = getText(c, "title_fr");
        t.titleAr = getText(c, "title_ar");
        t.bodyEn = getText(c, "body_en");
        t.bodyFr = getText(c, "body_fr");
        t.bodyAr = getText(c, "body_ar");
        t.image = getText(c, "image");
        t.videoUrl = getText(c, "video_url");
        return t;
    }

    public ArrayList<Models.Rule> rules() {
        ArrayList<Models.Rule> list = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery("SELECT * FROM rules ORDER BY sort_order, id", null);
        try {
            while (c.moveToNext()) {
                Models.Rule r = new Models.Rule();
                r.id = getInt(c, "id");
                r.kind = getText(c, "kind");
                r.titleEn = getText(c, "title_en");
                r.titleFr = getText(c, "title_fr");
                r.titleAr = getText(c, "title_ar");
                r.bodyEn = getText(c, "body_en");
                r.bodyFr = getText(c, "body_fr");
                r.bodyAr = getText(c, "body_ar");
                r.sourceUrl = getText(c, "source_url");
                list.add(r);
            }
        } finally {
            c.close();
        }
        return list;
    }

    // ================================================================ catch log (saved here first, then synced)

    public ArrayList<Models.CatchItem> catches(int userId) {
        ArrayList<Models.CatchItem> list = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT * FROM catches WHERE user_id = ? AND deleted = 0 ORDER BY caught_at DESC",
                new String[]{String.valueOf(userId)});
        try {
            while (c.moveToNext()) {
                list.add(readCatch(c));
            }
        } finally {
            c.close();
        }
        return list;
    }

    public Models.CatchItem catchItem(String uuid) {
        Cursor c = getReadableDatabase().rawQuery("SELECT * FROM catches WHERE uuid = ?", new String[]{uuid});
        try {
            if (c.moveToFirst()) {
                return readCatch(c);
            }
            return null;
        } finally {
            c.close();
        }
    }

    private Models.CatchItem readCatch(Cursor c) {
        Models.CatchItem x = new Models.CatchItem();
        x.uuid = getText(c, "uuid");
        x.userId = getInt(c, "user_id");
        x.spotId = getInt(c, "spot_id");
        x.fishId = getInt(c, "fish_id");
        x.spotName = getText(c, "spot_name");
        x.species = getText(c, "species");
        x.bait = getText(c, "bait");
        x.note = getText(c, "note");
        x.photo = getText(c, "photo");
        x.weightG = getInt(c, "weight_g");
        x.lengthCm = getInt(c, "length_cm");
        x.released = getInt(c, "released") == 1;
        x.lat = getDouble(c, "lat");
        x.lng = getDouble(c, "lng");
        x.caughtAt = c.getLong(c.getColumnIndexOrThrow("caught_at"));
        x.updatedAt = c.getLong(c.getColumnIndexOrThrow("updated_at"));
        x.deleted = getInt(c, "deleted") == 1;
        x.synced = getInt(c, "synced") == 1;
        return x;
    }

    public void saveCatch(Models.CatchItem x) {
        x.updatedAt = System.currentTimeMillis();
        ContentValues v = new ContentValues();
        v.put("uuid", x.uuid);
        v.put("user_id", x.userId);
        v.put("spot_id", x.spotId);
        v.put("spot_name", x.spotName);
        v.put("fish_id", x.fishId);
        v.put("species", x.species);
        v.put("weight_g", x.weightG);
        v.put("length_cm", x.lengthCm);
        v.put("bait", x.bait);
        if (x.released) {
            v.put("released", 1);
        } else {
            v.put("released", 0);
        }
        v.put("note", x.note);
        v.put("photo", x.photo);
        v.put("lat", x.lat);
        v.put("lng", x.lng);
        v.put("caught_at", x.caughtAt);
        v.put("updated_at", x.updatedAt);
        if (x.deleted) {
            v.put("deleted", 1);
        } else {
            v.put("deleted", 0);
        }
        v.put("synced", 0);
        getWritableDatabase().insertWithOnConflict("catches", null, v, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public void deleteCatch(String uuid) {
        ContentValues v = new ContentValues();
        v.put("deleted", 1);
        v.put("synced", 0);
        v.put("updated_at", System.currentTimeMillis());
        getWritableDatabase().update("catches", v, "uuid = ?", new String[]{uuid});
    }

    public int unsyncedCount(int userId) {
        String id = String.valueOf(userId);
        int total = 0;
        Cursor c = getReadableDatabase().rawQuery("SELECT COUNT(*) FROM catches WHERE user_id = ? AND synced = 0", new String[]{id});
        try {
            if (c.moveToFirst()) {
                total = total + c.getInt(0);
            }
        } finally {
            c.close();
        }
        c = getReadableDatabase().rawQuery("SELECT COUNT(*) FROM favorites WHERE user_id = ? AND synced = 0", new String[]{id});
        try {
            if (c.moveToFirst()) {
                total = total + c.getInt(0);
            }
        } finally {
            c.close();
        }
        return total;
    }

    public int catchCountSince(int userId, long since) {
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT COUNT(*) FROM catches WHERE user_id = ? AND deleted = 0 AND caught_at >= ?",
                new String[]{String.valueOf(userId), String.valueOf(since)});
        try {
            if (c.moveToFirst()) {
                return c.getInt(0);
            }
            return 0;
        } finally {
            c.close();
        }
    }

    public JSONArray unsyncedCatches(int userId) {
        JSONArray array = new JSONArray();
        Cursor c = getReadableDatabase().rawQuery("SELECT * FROM catches WHERE user_id = ? AND synced = 0",
                new String[]{String.valueOf(userId)});
        try {
            while (c.moveToNext()) {
                Models.CatchItem x = readCatch(c);
                JSONObject o = new JSONObject();
                o.put("uuid", x.uuid);
                o.put("spot_id", x.spotId);
                o.put("spot_name", x.spotName);
                o.put("fish_id", x.fishId);
                o.put("species", x.species);
                o.put("weight_g", x.weightG);
                o.put("length_cm", x.lengthCm);
                o.put("bait", x.bait);
                o.put("released", x.released ? 1 : 0);
                o.put("note", x.note);
                o.put("lat", x.lat);
                o.put("lng", x.lng);
                o.put("caught_at", x.caughtAt);
                o.put("updated_at", x.updatedAt);
                o.put("deleted", x.deleted ? 1 : 0);
                array.put(o);
            }
        } catch (JSONException e) {
        } finally {
            c.close();
        }
        return array;
    }

    public void mergeRemoteCatches(int userId, JSONArray remote) {
        if (remote == null) {
            return;
        }
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            for (int i = 0; i < remote.length(); i++) {
                JSONObject o = remote.optJSONObject(i);
                if (o == null) {
                    continue;
                }
                String uuid = o.optString("uuid");
                long remoteTime = o.optLong("updated_at");
                Models.CatchItem local = catchItem(uuid);
                if (local != null && local.updatedAt > remoteTime) {
                    continue;
                }
                ContentValues v = new ContentValues();
                v.put("uuid", uuid);
                v.put("user_id", userId);
                v.put("spot_id", o.optInt("spot_id"));
                v.put("spot_name", o.optString("spot_name"));
                v.put("fish_id", o.optInt("fish_id"));
                v.put("species", o.optString("species"));
                v.put("weight_g", o.optInt("weight_g"));
                v.put("length_cm", o.optInt("length_cm"));
                v.put("bait", o.optString("bait"));
                v.put("released", o.optInt("released"));
                v.put("note", o.optString("note"));
                if (local == null) {
                    v.put("photo", "");
                } else {
                    v.put("photo", local.photo);
                }
                v.put("lat", o.optDouble("lat", 0));
                v.put("lng", o.optDouble("lng", 0));
                v.put("caught_at", o.optLong("caught_at"));
                v.put("updated_at", remoteTime);
                v.put("deleted", o.optInt("deleted"));
                v.put("synced", 1);
                db.insertWithOnConflict("catches", null, v, SQLiteDatabase.CONFLICT_REPLACE);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    // ================================================================ favourite spots

    public boolean isFavorite(int userId, int spotId) {
        Cursor c = getReadableDatabase().rawQuery("SELECT fav FROM favorites WHERE user_id = ? AND spot_id = ?",
                new String[]{String.valueOf(userId), String.valueOf(spotId)});
        try {
            return c.moveToFirst() && c.getInt(0) == 1;
        } finally {
            c.close();
        }
    }

    public void setFavorite(int userId, int spotId, boolean fav) {
        ContentValues v = new ContentValues();
        v.put("user_id", userId);
        v.put("spot_id", spotId);
        if (fav) {
            v.put("fav", 1);
        } else {
            v.put("fav", 0);
        }
        v.put("synced", 0);
        getWritableDatabase().insertWithOnConflict("favorites", null, v, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public HashSet<Integer> favoriteIds(int userId) {
        HashSet<Integer> ids = new HashSet<>();
        Cursor c = getReadableDatabase().rawQuery("SELECT spot_id FROM favorites WHERE user_id = ? AND fav = 1",
                new String[]{String.valueOf(userId)});
        try {
            while (c.moveToNext()) {
                ids.add(c.getInt(0));
            }
        } finally {
            c.close();
        }
        return ids;
    }

    public JSONArray pendingFavorites(int userId) {
        JSONArray array = new JSONArray();
        Cursor c = getReadableDatabase().rawQuery("SELECT spot_id, fav FROM favorites WHERE user_id = ? AND synced = 0",
                new String[]{String.valueOf(userId)});
        try {
            while (c.moveToNext()) {
                JSONObject o = new JSONObject();
                o.put("spot_id", c.getInt(0));
                o.put("fav", c.getInt(1));
                array.put(o);
            }
        } catch (JSONException e) {
        } finally {
            c.close();
        }
        return array;
    }

    public void replaceFavorites(int userId, JSONArray ids) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete("favorites", "user_id = ?", new String[]{String.valueOf(userId)});
            for (int i = 0; ids != null && i < ids.length(); i++) {
                ContentValues v = new ContentValues();
                v.put("user_id", userId);
                v.put("spot_id", ids.optInt(i));
                v.put("fav", 1);
                v.put("synced", 1);
                db.insert("favorites", null, v);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    // ================================================================ weather cache

    public void replaceWeather(HashMap<Integer, ArrayList<Conditions.Hour>> bySpot) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete("weather", null, null);
            for (Map.Entry<Integer, ArrayList<Conditions.Hour>> entry : bySpot.entrySet()) {
                for (Conditions.Hour h : entry.getValue()) {
                    ContentValues v = new ContentValues();
                    v.put("spot_id", entry.getKey());
                    v.put("t", h.t);
                    v.put("temp", h.temp);
                    v.put("rain_prob", h.rainProb);
                    v.put("rain", h.rain);
                    v.put("code", h.code);
                    v.put("cloud", h.cloud);
                    v.put("pressure", h.pressure);
                    v.put("wind", h.wind);
                    v.put("wind_dir", h.windDir);
                    v.put("gust", h.gust);
                    v.put("wave", h.wave);
                    v.put("wave_period", h.wavePeriod);
                    v.put("sst", h.sst);
                    db.insert("weather", null, v);
                }
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public ArrayList<Conditions.Hour> weather(int spotId) {
        ArrayList<Conditions.Hour> list = new ArrayList<>();
        long from = System.currentTimeMillis() - 4 * 3600000L;
        Cursor c = getReadableDatabase().rawQuery("SELECT * FROM weather WHERE spot_id = ? AND t >= ? ORDER BY t",
                new String[]{String.valueOf(spotId), String.valueOf(from)});
        try {
            while (c.moveToNext()) {
                Conditions.Hour h = new Conditions.Hour();
                h.t = c.getLong(c.getColumnIndexOrThrow("t"));
                h.temp = getDouble(c, "temp");
                h.rainProb = getDouble(c, "rain_prob");
                h.rain = getDouble(c, "rain");
                h.code = getInt(c, "code");
                h.cloud = getDouble(c, "cloud");
                h.pressure = getDouble(c, "pressure");
                h.wind = getDouble(c, "wind");
                h.windDir = getDouble(c, "wind_dir");
                h.gust = getDouble(c, "gust");
                h.wave = getDouble(c, "wave");
                h.wavePeriod = getDouble(c, "wave_period");
                h.sst = getDouble(c, "sst");
                list.add(h);
            }
        } finally {
            c.close();
        }
        return list;
    }

    // ================================================================ small key → value table

    public String getValue(String key) {
        Cursor c = getReadableDatabase().rawQuery("SELECT v FROM kv WHERE k = ?", new String[]{key});
        try {
            if (c.moveToFirst()) {
                return c.getString(0);
            }
            return null;
        } finally {
            c.close();
        }
    }

    public void setValue(String key, String value) {
        ContentValues v = new ContentValues();
        v.put("k", key);
        v.put("v", value);
        getWritableDatabase().insertWithOnConflict("kv", null, v, SQLiteDatabase.CONFLICT_REPLACE);
    }

    // ================================================================ reading one column of the current row

    private static String getText(Cursor c, String column) {
        String value = c.getString(c.getColumnIndexOrThrow(column));
        if (value == null) {
            return "";
        }
        return value;
    }

    private static int getInt(Cursor c, String column) {
        return (int) Math.round(c.getDouble(c.getColumnIndexOrThrow(column)));
    }

    private static double getDouble(Cursor c, String column) {
        int index = c.getColumnIndexOrThrow(column);
        if (c.isNull(index)) {
            return Double.NaN;
        }
        return c.getDouble(index);
    }
}
