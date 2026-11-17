package com.sennara.app.util;

import com.sennara.app.R;
import com.sennara.app.data.Models;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;

public class Achievements {

    public static class Badge {
        public String id;
        public int title;
        public int description;
        public int icon;
        public boolean unlocked;

        Badge(String id, int title, int description, int icon, boolean unlocked) {
            this.id = id;
            this.title = title;
            this.description = description;
            this.icon = icon;
            this.unlocked = unlocked;
        }
    }

    public static ArrayList<Badge> compute(ArrayList<Models.CatchItem> catches) {
        HashSet<String> species = new HashSet<>();
        HashSet<Integer> spots = new HashSet<>();
        int released = 0;
        int heaviest = 0;
        boolean early = false;
        boolean night = false;
        Calendar calendar = Calendar.getInstance();

        for (Models.CatchItem x : catches) {
            if (x.fishId > 0) {
                species.add("id" + x.fishId);
            } else {
                species.add(x.species.toLowerCase());
            }
            if (x.spotId > 0) {
                spots.add(x.spotId);
            }
            if (x.released) {
                released++;
            }
            if (x.weightG > heaviest) {
                heaviest = x.weightG;
            }
            calendar.setTimeInMillis(x.caughtAt);
            int hour = calendar.get(Calendar.HOUR_OF_DAY);
            if (hour < 7) {
                early = true;
            }
            if (hour >= 21 || hour < 4) {
                night = true;
            }
        }

        ArrayList<Badge> list = new ArrayList<>();
        list.add(new Badge("first", R.string.b_first, R.string.bd_first, R.drawable.ic_hook, catches.size() >= 1));
        list.add(new Badge("ten", R.string.b_ten, R.string.bd_ten, R.drawable.ic_trophy, catches.size() >= 10));
        list.add(new Badge("species", R.string.b_species, R.string.bd_species, R.drawable.ic_restaurant, species.size() >= 5));
        list.add(new Badge("explorer", R.string.b_explorer, R.string.bd_explorer, R.drawable.ic_compass, spots.size() >= 3));
        list.add(new Badge("heavy", R.string.b_heavy, R.string.bd_heavy, R.drawable.ic_scale, heaviest >= 2000));
        list.add(new Badge("early", R.string.b_early, R.string.bd_early, R.drawable.ic_sunrise, early));
        list.add(new Badge("night", R.string.b_night, R.string.bd_night, R.drawable.ic_nightlight, night));
        list.add(new Badge("guardian", R.string.b_guardian, R.string.bd_guardian, R.drawable.ic_eco, released >= 3));
        return list;
    }

    public static ArrayList<Badge> newlyUnlocked(ArrayList<Badge> before, ArrayList<Badge> after) {
        ArrayList<Badge> list = new ArrayList<>();
        for (int i = 0; i < after.size(); i++) {
            if (after.get(i).unlocked && !before.get(i).unlocked) {
                list.add(after.get(i));
            }
        }
        return list;
    }
}
