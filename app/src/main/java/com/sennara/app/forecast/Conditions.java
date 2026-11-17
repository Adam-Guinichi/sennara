package com.sennara.app.forecast;

import com.sennara.app.R;
import com.sennara.app.data.Models;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;

public class Conditions {
    public static final int UNSAFE = 0;
    public static final int POOR = 1;
    public static final int FAIR = 2;
    public static final int GOOD = 3;
    public static final int GREAT = 4;

    private static final long MINUTE = 60000L;
    private static final long HOUR = 3600000L;
    private static final long DAY = 86400000L;

    // ================================================================ small classes

    public static class Hour {
        public long t;
        public int code;
        public double temp;
        public double rainProb;
        public double rain;
        public double cloud;
        public double pressure;
        public double wind;
        public double windDir;
        public double gust;
        public double wave;
        public double wavePeriod;
        public double sst;
    }

    public static class Reason {
        public int text;
        public Object[] args;
        public boolean good;
        public int icon;

        Reason(int text, boolean good, int icon) {
            this.text = text;
            this.good = good;
            this.icon = icon;
            this.args = new Object[0];
        }

        Reason(int text, boolean good, int icon, Object[] args) {
            this.text = text;
            this.good = good;
            this.icon = icon;
            this.args = args;
        }
    }

    public static class Result {
        public long t;
        public int score;
        public int level;
        public boolean unsafe;
        public Hour hour;
        public ArrayList<Reason> pros = new ArrayList<>();
        public ArrayList<Reason> cons = new ArrayList<>();
    }

    public static class Window {
        public long start;
        public long end;
        public int score;
        public int level;
    }

    public static class DaySummary {
        public long day;
        public int best;
        public int level;
        public int unsafeHours;
        public long bestTime;
    }

    // ================================================================ scoring

    public static ArrayList<Result> evaluate(Models.Spot spot, ArrayList<Hour> hours, int fishInSeason) {
        ArrayList<Result> results = new ArrayList<>();
        HashMap<Long, Astro.Day> days = new HashMap<>();

        for (int i = 0; i < hours.size(); i++) {
            Hour h = hours.get(i);
            long start = Astro.dayStart(h.t);

            Astro.Day today = days.get(start);
            if (today == null) {
                today = Astro.day(h.t, spot.lat, spot.lng);
                days.put(start, today);
            }
            Astro.Day yesterday = days.get(start - DAY);
            if (yesterday == null) {
                yesterday = Astro.day(start - DAY / 2, spot.lat, spot.lng);
                days.put(start - DAY, yesterday);
            }
            Astro.Day tomorrow = days.get(start + DAY);
            if (tomorrow == null) {
                tomorrow = Astro.day(start + DAY + DAY / 2, spot.lat, spot.lng);
                days.put(start + DAY, tomorrow);
            }

            double pressureBefore;
            if (i >= 3) {
                pressureBefore = hours.get(i - 3).pressure;
            } else {
                pressureBefore = hours.get(0).pressure;
            }
            double pressureChange = h.pressure - pressureBefore;

            results.add(score(spot, h, pressureChange, today, yesterday, tomorrow, fishInSeason));
        }
        return results;
    }

    static Result score(Models.Spot spot, Hour h, double pressureChange, Astro.Day day, Astro.Day prev, Astro.Day next,
                        int fishInSeason) {
        Result r = new Result();
        r.t = h.t;
        r.hour = h;
        long t = h.t + 30 * MINUTE;

        // ---- 1. Light: fish feed most around sunrise and sunset (30 points).
        long[][] windows = {prev.duskWindow(), day.dawnWindow(), day.duskWindow(), next.dawnWindow()};
        long away = Long.MAX_VALUE;
        int inside = -1;
        for (int k = 0; k < windows.length; k++) {
            long windowStart = windows[k][0];
            long windowEnd = windows[k][1];
            long distance = 0;
            if (t < windowStart) {
                distance = windowStart - t;
            } else if (t > windowEnd) {
                distance = t - windowEnd;
            } else {
                inside = k;
            }
            if (distance < away) {
                away = distance;
            }
        }
        double minutesAway = away / MINUTE;
        double light = Math.max(4, 30 - minutesAway / 6.0);
        boolean night = t > day.sunset + HOUR || t < day.sunrise - HOUR;
        if (spot.bestTime.equals("night") && night) {
            light = Math.max(light, 22);
            r.pros.add(new Reason(R.string.r_night_spot, true, R.drawable.ic_nightlight));
        } else if (spot.bestTime.equals("day") && !night) {
            light = Math.max(light, 20);
        }
        if (inside == 1 || inside == 3) {
            r.pros.add(new Reason(R.string.r_dawn, true, R.drawable.ic_sunrise));
        } else if (inside == 0 || inside == 2) {
            r.pros.add(new Reason(R.string.r_dusk, true, R.drawable.ic_sunset));
        }

        // ---- 2. Solunar periods (12 points for a major period, 8 for a minor one).
        double solunar = 0;
        for (long[] w : day.majors) {
            if (t >= w[0] && t <= w[1]) {
                solunar = 12;
            }
        }
        if (solunar == 0) {
            for (long[] w : day.minors) {
                if (t >= w[0] && t <= w[1]) {
                    solunar = 8;
                }
            }
        }
        if (solunar == 12) {
            r.pros.add(new Reason(R.string.r_major, true, R.drawable.ic_moon));
        } else if (solunar == 8) {
            r.pros.add(new Reason(R.string.r_minor, true, R.drawable.ic_moon));
        }

        // ---- 3. Moon phase: strongest tides around new moon and full moon (8 points).
        double moon = 8 * Math.abs(Math.cos(2 * Math.PI * day.phase));
        if (moon >= 6.5) {
            if (day.phase < 0.25 || day.phase > 0.75) {
                r.pros.add(new Reason(R.string.r_new_moon, true, R.drawable.ic_moon));
            } else {
                r.pros.add(new Reason(R.string.r_full_moon, true, R.drawable.ic_moon));
            }
        }

        // ---- 4. Wind (15 points).
        double wind;
        String direction = compass(h.windDir);
        Object[] windArgs = {Math.round(h.wind), direction};
        if (Double.isNaN(h.wind)) {
            wind = 9;
        } else if (h.wind < 5) {
            wind = 10;
        } else if (h.wind <= 20) {
            wind = 15;
            r.pros.add(new Reason(R.string.r_breeze, true, R.drawable.ic_air, windArgs));
        } else if (h.wind <= 30) {
            wind = 8;
            r.cons.add(new Reason(R.string.r_windy, false, R.drawable.ic_air, windArgs));
        } else if (h.wind <= 40) {
            wind = 3;
            r.cons.add(new Reason(R.string.r_strong_wind, false, R.drawable.ic_air, windArgs));
        } else {
            wind = 0;
        }
        if (!Double.isNaN(h.gust) && h.gust >= 40 && h.gust < 60) {
            r.cons.add(new Reason(R.string.r_gusts, false, R.drawable.ic_air, new Object[]{Math.round(h.gust)}));
        }

        // ---- 5. Pressure trend over 3 hours (10 points).
        double pressure;
        if (Double.isNaN(pressureChange)) {
            pressure = 6;
        } else if (pressureChange <= -3) {
            pressure = 5;
            r.cons.add(new Reason(R.string.r_pressure_crash, false, R.drawable.ic_trend_down));
        } else if (pressureChange <= -0.8) {
            pressure = 10;
            r.pros.add(new Reason(R.string.r_pressure_falling, true, R.drawable.ic_trend_down));
        } else if (pressureChange < 0.8) {
            pressure = 6;
        } else {
            pressure = 4;
            r.cons.add(new Reason(R.string.r_pressure_rising, false, R.drawable.ic_trend_up));
        }

        // ---- 6. Sea state (10 points).
        double sea;
        Object[] waveArgs = {oneDecimal(h.wave)};
        if (spot.type.equals("lagoon")) {
            sea = 8;
        } else if (Double.isNaN(h.wave)) {
            sea = 7;
        } else if (h.wave < 0.3) {
            sea = 6;
            r.cons.add(new Reason(R.string.r_flat_sea, false, R.drawable.ic_waves));
        } else if (h.wave <= 1.2) {
            sea = 10;
            r.pros.add(new Reason(R.string.r_good_waves, true, R.drawable.ic_waves, waveArgs));
        } else if (h.wave <= 2.0) {
            sea = 4;
            r.cons.add(new Reason(R.string.r_big_waves, false, R.drawable.ic_waves, waveArgs));
        } else {
            sea = 0;
        }

        // ---- 7. Sky (10 points).
        double sky;
        if (!Double.isNaN(h.rain) && h.rain >= 2) {
            sky = 2;
            r.cons.add(new Reason(R.string.r_heavy_rain, false, R.drawable.ic_rain));
        } else if (!Double.isNaN(h.rain) && h.rain >= 0.2) {
            sky = 6;
        } else if (!Double.isNaN(h.cloud) && h.cloud >= 40) {
            sky = 10;
            r.pros.add(new Reason(R.string.r_overcast, true, R.drawable.ic_cloud));
        } else {
            sky = 7;
        }

        // ---- Total out of 95, scaled to 100.
        double total = (light + solunar + moon + wind + pressure + sea + sky) * 100 / 95.0;
        if (fishInSeason == 0) {
            total = total * 0.85;
            r.cons.add(new Reason(R.string.r_off_season, false, R.drawable.ic_calendar));
        } else if (fishInSeason >= 3) {
            r.pros.add(new Reason(R.string.r_in_season, true, R.drawable.ic_restaurant, new Object[]{fishInSeason}));
        }
        long rounded = Math.round(total);
        if (rounded < 0) {
            rounded = 0;
        }
        if (rounded > 100) {
            rounded = 100;
        }
        r.score = (int) rounded;
        r.level = level(r.score);

        // ---- Safety first: these conditions replace the score with "Stay ashore".
        if (h.code >= 95) {
            makeUnsafe(r, new Reason(R.string.r_thunder, false, R.drawable.ic_storm));
        }
        boolean gale = (!Double.isNaN(h.gust) && h.gust >= 60) || (!Double.isNaN(h.wind) && h.wind >= 45);
        if (gale) {
            double strongest = h.gust;
            if (Double.isNaN(strongest)) {
                strongest = h.wind;
            }
            makeUnsafe(r, new Reason(R.string.r_gale, false, R.drawable.ic_air, new Object[]{Math.round(strongest)}));
        }
        double maxWave = 2.5;
        if (spot.type.equals("rock")) {
            maxWave = 2.0;
        }
        if (!Double.isNaN(h.wave) && h.wave >= maxWave && !spot.type.equals("lagoon")) {
            makeUnsafe(r, new Reason(R.string.r_rough_sea, false, R.drawable.ic_waves, waveArgs));
        }
        if (!Double.isNaN(h.rain) && h.rain >= 8) {
            makeUnsafe(r, new Reason(R.string.r_downpour, false, R.drawable.ic_rain));
        }
        return r;
    }

    private static void makeUnsafe(Result r, Reason why) {
        r.unsafe = true;
        r.level = UNSAFE;
        r.cons.add(0, why);
    }

    public static int level(int score) {
        if (score >= 75) {
            return GREAT;
        }
        if (score >= 55) {
            return GOOD;
        }
        if (score >= 35) {
            return FAIR;
        }
        return POOR;
    }

    // ================================================================ reading the results

    public static Result now(ArrayList<Result> results) {
        long now = System.currentTimeMillis();
        for (Result r : results) {
            if (r.t <= now && now < r.t + HOUR) {
                return r;
            }
        }
        for (Result r : results) {
            if (r.t >= now) {
                return r;
            }
        }
        return null;
    }

    public static Window bestWindow(ArrayList<Result> results, long from, long span) {
        Window best = null;
        for (int i = 0; i + 1 < results.size(); i++) {
            Result a = results.get(i);
            Result b = results.get(i + 1);
            if (a.t + HOUR <= from || a.t > from + span) {
                continue;
            }
            if (a.unsafe || b.unsafe) {
                continue;
            }
            int average = (a.score + b.score) / 2;
            if (best == null || average > best.score) {
                best = new Window();
                best.start = a.t;
                best.end = b.t + HOUR;
                best.score = average;
                best.level = level(average);
            }
        }
        return best;
    }

    public static ArrayList<DaySummary> days(ArrayList<Result> results) {
        ArrayList<DaySummary> list = new ArrayList<>();
        DaySummary current = null;
        Calendar calendar = Calendar.getInstance();
        for (Result r : results) {
            long start = Astro.dayStart(r.t);
            if (current == null || current.day != start) {
                current = new DaySummary();
                current.day = start;
                current.best = -1;
                list.add(current);
            }
            calendar.setTimeInMillis(r.t);
            int hour = calendar.get(Calendar.HOUR_OF_DAY);
            if (hour < 5 || hour > 22) {
                continue;
            }
            if (r.unsafe) {
                current.unsafeHours++;
                continue;
            }
            if (r.score > current.best) {
                current.best = r.score;
                current.bestTime = r.t;
            }
        }
        for (DaySummary d : list) {
            if (d.best < 0) {
                d.level = UNSAFE;
            } else if (d.unsafeHours >= 8) {
                d.level = Math.min(POOR, level(d.best));
            } else {
                d.level = level(d.best);
            }
        }
        return list;
    }

    public static String compass(double degrees) {
        if (Double.isNaN(degrees)) {
            return "";
        }
        String[] names = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};
        double positive = ((degrees % 360) + 360) % 360;
        int index = (int) Math.round(positive / 45.0) % 8;
        return names[index];
    }

    private static String oneDecimal(double value) {
        return String.format(Locale.US, "%.1f", value);
    }
}
