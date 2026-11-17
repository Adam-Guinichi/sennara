package com.sennara.app.util;

import android.content.Context;

import com.sennara.app.R;
import com.sennara.app.forecast.Conditions;

public class Labels {

    private static String s(Context c, int id) {
        return c.getString(id);
    }

    public static String type(Context c, String type) {
        switch (type) {
            case "rock": return s(c, R.string.type_rock);
            case "port": return s(c, R.string.type_port);
            case "lagoon": return s(c, R.string.type_lagoon);
            default: return s(c, R.string.type_shore);
        }
    }

    public static String habitat(Context c, String h) {
        switch (h) {
            case "rock": return s(c, R.string.type_rock);
            case "port": return s(c, R.string.type_port);
            case "lagoon": return s(c, R.string.type_lagoon);
            case "boat": return s(c, R.string.hab_boat);
            default: return s(c, R.string.hab_sand);
        }
    }

    public static String category(Context c, String category) {
        switch (category) {
            case "bait": return s(c, R.string.cat_bait);
            case "rod": return s(c, R.string.cat_rod);
            case "reel": return s(c, R.string.cat_reel);
            case "line": return s(c, R.string.cat_line);
            case "hook": return s(c, R.string.cat_hook);
            default: return s(c, R.string.cat_other);
        }
    }

    public static String bait(Context c, String bait) {
        switch (bait) {
            case "shrimp": return s(c, R.string.bait_shrimp);
            case "worm": return s(c, R.string.bait_worm);
            case "sardine": return s(c, R.string.bait_sardine);
            case "squid": return s(c, R.string.bait_squid);
            case "bread": return s(c, R.string.bait_bread);
            case "lure": return s(c, R.string.bait_lure);
            case "crab": return s(c, R.string.bait_crab);
            case "mussel": return s(c, R.string.bait_mussel);
            case "live": return s(c, R.string.bait_live);
            case "jig": return s(c, R.string.bait_jig);
            default: return bait;
        }
    }

    public static String technique(Context c, String t) {
        switch (t) {
            case "float": return s(c, R.string.tech_float);
            case "bottom": return s(c, R.string.tech_bottom);
            case "surf": return s(c, R.string.tech_surf);
            case "lure": return s(c, R.string.tech_lure);
            case "jig": return s(c, R.string.tech_jig);
            case "live": return s(c, R.string.tech_live);
            default: return t;
        }
    }

    public static String level(Context c, String level) {
        switch (level) {
            case "intermediate": return s(c, R.string.level_intermediate);
            case "advanced": return s(c, R.string.level_advanced);
            case "expert": return s(c, R.string.level_expert);
            default: return s(c, R.string.level_beginner);
        }
    }

    public static String bestTime(Context c, String bestTime) {
        switch (bestTime) {
            case "dusk": return s(c, R.string.time_dusk);
            case "night": return s(c, R.string.time_night);
            case "day": return s(c, R.string.time_day);
            default: return s(c, R.string.time_dawn);
        }
    }

    public static String role(Context c, String role) {
        switch (role) {
            case "seller": return s(c, R.string.role_seller);
            case "admin": return s(c, R.string.role_admin);
            default: return s(c, R.string.role_fisher);
        }
    }

    public static String iucn(Context c, String code) {
        switch (code) {
            case "LC": return s(c, R.string.iucn_lc);
            case "NT": return s(c, R.string.iucn_nt);
            case "VU": return s(c, R.string.iucn_vu);
            case "EN": return s(c, R.string.iucn_en);
            case "CR": return s(c, R.string.iucn_cr);
            default: return s(c, R.string.iucn_dd);
        }
    }

    public static int typeIcon(String type) {
        switch (type) {
            case "port": return R.drawable.ic_sailing;
            case "lagoon": return R.drawable.ic_water;
            case "shore": return R.drawable.ic_waves;
            default: return R.drawable.ic_place;
        }
    }

    // ---------------------------------------------------------------- verdict levels

    public static String verdict(Context c, int level) {
        switch (level) {
            case Conditions.GREAT: return s(c, R.string.v_great);
            case Conditions.GOOD: return s(c, R.string.v_good);
            case Conditions.FAIR: return s(c, R.string.v_fair);
            case Conditions.POOR: return s(c, R.string.v_poor);
            default: return s(c, R.string.v_unsafe);
        }
    }

    public static String verdictTitle(Context c, int level) {
        switch (level) {
            case Conditions.GREAT: return s(c, R.string.vt_great);
            case Conditions.GOOD: return s(c, R.string.vt_good);
            case Conditions.FAIR: return s(c, R.string.vt_fair);
            case Conditions.POOR: return s(c, R.string.vt_poor);
            default: return s(c, R.string.vt_unsafe);
        }
    }

    public static int levelColor(int level) {
        switch (level) {
            case Conditions.GREAT: return R.color.lv_great;
            case Conditions.GOOD: return R.color.lv_good;
            case Conditions.FAIR: return R.color.lv_fair;
            case Conditions.POOR: return R.color.lv_poor;
            default: return R.color.lv_unsafe;
        }
    }

    public static int levelSoft(int level) {
        switch (level) {
            case Conditions.GREAT: return R.color.lv_great_soft;
            case Conditions.GOOD: return R.color.lv_good_soft;
            case Conditions.FAIR: return R.color.lv_fair_soft;
            case Conditions.POOR: return R.color.lv_poor_soft;
            default: return R.color.lv_unsafe_soft;
        }
    }

    public static String moon(Context c, double phase) {
        if (phase < 0.03 || phase > 0.97) return s(c, R.string.moon_new);
        if (phase < 0.22) return s(c, R.string.moon_waxing_crescent);
        if (phase < 0.28) return s(c, R.string.moon_first_quarter);
        if (phase < 0.47) return s(c, R.string.moon_waxing_gibbous);
        if (phase < 0.53) return s(c, R.string.moon_full);
        if (phase < 0.72) return s(c, R.string.moon_waning_gibbous);
        if (phase < 0.78) return s(c, R.string.moon_last_quarter);
        return s(c, R.string.moon_waning_crescent);
    }
}
