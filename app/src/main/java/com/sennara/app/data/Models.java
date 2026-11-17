package com.sennara.app.data;

import java.util.ArrayList;

public class Models {

    public static String pick(String lang, String en, String fr, String ar) {
        String text = en;
        if (lang.equals("fr")) {
            text = fr;
        } else if (lang.equals("ar")) {
            text = ar;
        }
        if (text == null || text.isEmpty()) {
            return en;
        }
        return text;
    }

    public static ArrayList<String> split(String text) {
        ArrayList<String> list = new ArrayList<>();
        if (text == null) {
            return list;
        }
        String[] parts = text.split(",");
        for (String part : parts) {
            if (!part.trim().isEmpty()) {
                list.add(part.trim());
            }
        }
        return list;
    }

    public static boolean inMonths(int month, int from, int to) {
        if (from <= to) {
            return month >= from && month <= to;
        } else {
            return month >= from || month <= to;
        }
    }

    // ================================================================ the classes

    public static class Spot {
        public int id;
        public int nReviews;
        public int nCatches;
        public String slug = "";
        public String name = "";
        public String nameAr = "";
        public String city = "";
        public String cityAr = "";
        public String type = "shore";
        public String bestTime = "dawn";
        public String descEn = "";
        public String descFr = "";
        public String descAr = "";
        public String image = "";
        public String topSpecies = "";
        public double lat;
        public double lng;
        public double avgRating;

        public String name(String lang) {
            if (lang.equals("ar") && !nameAr.isEmpty()) {
                return nameAr;
            }
            return name;
        }

        public String city(String lang) {
            if (lang.equals("ar") && !cityAr.isEmpty()) {
                return cityAr;
            }
            return city;
        }

        public String desc(String lang) {
            return pick(lang, descEn, descFr, descAr);
        }
    }

    public static class Fish {
        public int id;
        public int monthFrom = 1;
        public int monthTo = 12;
        public int minSizeCm;
        public int minWeightG;
        public int maxCm;
        public String slug = "";
        public String sci = "";
        public String nameEn = "";
        public String nameFr = "";
        public String nameAr = "";
        public String family = "";
        public String habitats = "";
        public String baits = "";
        public String techniques = "";
        public String sizeRule = "none";
        public String iucn = "";
        public String aboutEn = "";
        public String aboutFr = "";
        public String aboutAr = "";
        public String image = "";
        public String wikidata = "";

        public String name(String lang) {
            return pick(lang, nameEn, nameFr, nameAr);
        }

        public String about(String lang) {
            return pick(lang, aboutEn, aboutFr, aboutAr);
        }

        public boolean inSeason(int month) {
            return inMonths(month, monthFrom, monthTo);
        }

        public ArrayList<String> habitatList() {
            return split(habitats);
        }

        public ArrayList<String> baitList() {
            return split(baits);
        }

        public ArrayList<String> techniqueList() {
            return split(techniques);
        }

        public boolean isCephalopod() {
            return slug.equals("octopus") || slug.equals("cuttlefish") || slug.equals("squid");
        }
    }

    public static class Shop {
        public int id;
        public int ownerId;
        public int productCount;
        public String osmId = "";
        public String name = "";
        public String city = "";
        public String address = "";
        public String phone = "";
        public String website = "";
        public String description = "";
        public double lat;
        public double lng;
    }

    public static class Product {
        public int id;
        public int shopId;
        public String name = "";
        public String category = "other";
        public double price;
        public boolean inStock;
    }

    public static class Tutorial {
        public int id;
        public int minutes;
        public String slug = "";
        public String level = "beginner";
        public String titleEn = "";
        public String titleFr = "";
        public String titleAr = "";
        public String bodyEn = "";
        public String bodyFr = "";
        public String bodyAr = "";
        public String image = "";
        public String videoUrl = "";

        public String title(String lang) {
            return pick(lang, titleEn, titleFr, titleAr);
        }

        public String body(String lang) {
            return pick(lang, bodyEn, bodyFr, bodyAr);
        }
    }

    public static class Rule {
        public int id;
        public String kind = "tip";
        public String titleEn = "";
        public String titleFr = "";
        public String titleAr = "";
        public String bodyEn = "";
        public String bodyFr = "";
        public String bodyAr = "";
        public String sourceUrl = "";

        public String title(String lang) {
            return pick(lang, titleEn, titleFr, titleAr);
        }

        public String body(String lang) {
            return pick(lang, bodyEn, bodyFr, bodyAr);
        }
    }

    public static class CatchItem {
        public String uuid;
        public String spotName = "";
        public String species = "";
        public String bait = "";
        public String note = "";
        public String photo = "";
        public int userId;
        public int spotId;
        public int fishId;
        public int weightG;
        public int lengthCm;
        public boolean released;
        public boolean deleted;
        public boolean synced;
        public double lat;
        public double lng;
        public long caughtAt;
        public long updatedAt;
    }
}
