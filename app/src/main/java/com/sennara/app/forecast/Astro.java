package com.sennara.app.forecast;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.TimeZone;

public class Astro {
    private static final double RAD = Math.PI / 180.0;
    private static final long MINUTE = 60000L;
    private static final long HOUR = 3600000L;
    private static final long DAY = 86400000L;
    private static final double SYNODIC_MONTH = 29.530588853;
    private static final long KNOWN_NEW_MOON = 947182440000L;

    public static class Day {
        public long start;
        public long sunrise;
        public long sunset;
        public long moonrise = -1;
        public long moonset = -1;
        public long transit = -1;
        public double phase;
        public double illumination;
        public ArrayList<long[]> majors = new ArrayList<>();
        public ArrayList<long[]> minors = new ArrayList<>();

        public long[] dawnWindow() {
            return new long[]{sunrise - 60 * MINUTE, sunrise + 90 * MINUTE};
        }

        public long[] duskWindow() {
            return new long[]{sunset - 90 * MINUTE, sunset + 60 * MINUTE};
        }
    }

    public static long dayStart(long t) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(t);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    public static Day day(long anyTime, double lat, double lng) {
        Day d = new Day();
        d.start = dayStart(anyTime);
        long[] sun = sunTimes(d.start, lat, lng);
        d.sunrise = sun[0];
        d.sunset = sun[1];
        long noon = d.start + 12 * HOUR;
        d.phase = moonPhase(noon);
        d.illumination = (1 - Math.cos(2 * Math.PI * d.phase)) / 2;
        moonEvents(d, lat, lng);
        return d;
    }

    // ================================================================ Sun

    public static long[] sunTimes(long dayStart, double lat, double lng) {
        Calendar local = Calendar.getInstance();
        local.setTimeInMillis(dayStart + 12 * HOUR);
        int dayOfYear = local.get(Calendar.DAY_OF_YEAR);

        Calendar utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        utc.clear();
        utc.set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH), 0, 0, 0);
        long base = utc.getTimeInMillis();

        double rise = sunEventHours(dayOfYear, lat, lng, true);
        double set = sunEventHours(dayOfYear, lat, lng, false);
        if (Double.isNaN(rise) || Double.isNaN(set)) {
            rise = 5;
            set = 17;
        }
        long riseMs = base + (long) (rise * HOUR);
        long setMs = base + (long) (set * HOUR);
        if (setMs < riseMs) {
            setMs = setMs + DAY;
        }
        return new long[]{riseMs, setMs};
    }

    private static double sunEventHours(int dayOfYear, double lat, double lng, boolean sunrise) {
        double lngHour = lng / 15.0;
        double t;
        if (sunrise) {
            t = dayOfYear + (6 - lngHour) / 24.0;
        } else {
            t = dayOfYear + (18 - lngHour) / 24.0;
        }
        double meanAnomaly = 0.9856 * t - 3.289;
        double trueLongitude = rev(meanAnomaly + 1.916 * sin(meanAnomaly) + 0.020 * sin(2 * meanAnomaly) + 282.634);

        double rightAscension = rev(Math.atan(0.91764 * Math.tan(trueLongitude * RAD)) / RAD);
        double lQuadrant = Math.floor(trueLongitude / 90) * 90;
        double raQuadrant = Math.floor(rightAscension / 90) * 90;
        rightAscension = (rightAscension + lQuadrant - raQuadrant) / 15.0;

        double sinDec = 0.39782 * sin(trueLongitude);
        double cosDec = Math.cos(Math.asin(sinDec));
        double cosH = (cos(90.833) - sinDec * sin(lat)) / (cosDec * cos(lat));
        if (cosH > 1 || cosH < -1) {
            return Double.NaN;
        }
        double hourAngle = Math.acos(cosH) / RAD;
        if (sunrise) {
            hourAngle = 360 - hourAngle;
        }
        hourAngle = hourAngle / 15.0;

        double ut = hourAngle + rightAscension - 0.06571 * t - 6.622 - lngHour;
        ut = ut % 24;
        if (ut < 0) {
            ut = ut + 24;
        }
        return ut;
    }

    // ================================================================ Moon

    public static double moonPhase(long time) {
        double days = (time - KNOWN_NEW_MOON) / (double) DAY;
        double phase = (days % SYNODIC_MONTH) / SYNODIC_MONTH;
        if (phase < 0) {
            phase = phase + 1;
        }
        return phase;
    }

    private static double dayNumber(long ms) {
        return ms / 86400000.0 + 2440587.5 - 2451543.5;
    }

    static double[] moonRaDec(double d) {
        double node = rev(125.1228 - 0.0529538083 * d);
        double inclination = 5.1454;
        double perigee = rev(318.0634 + 0.1643573223 * d);
        double distance = 60.2666;
        double e = 0.054900;
        double meanAnomaly = rev(115.3654 + 13.0649929509 * d);

        double ecc = meanAnomaly + (180 / Math.PI) * e * sin(meanAnomaly) * (1 + e * cos(meanAnomaly));
        for (int k = 0; k < 5; k++) {
            ecc = ecc - (ecc - (180 / Math.PI) * e * sin(ecc) - meanAnomaly) / (1 - e * cos(ecc));
        }
        double xv = distance * (cos(ecc) - e);
        double yv = distance * Math.sqrt(1 - e * e) * sin(ecc);
        double trueAnomaly = Math.atan2(yv, xv) / RAD;
        double r = Math.hypot(xv, yv);

        double angle = trueAnomaly + perigee;
        double xh = r * (cos(node) * cos(angle) - sin(node) * sin(angle) * cos(inclination));
        double yh = r * (sin(node) * cos(angle) + cos(node) * sin(angle) * cos(inclination));
        double zh = r * sin(angle) * sin(inclination);
        double lon = Math.atan2(yh, xh) / RAD;
        double lat = Math.atan2(zh, Math.hypot(xh, yh)) / RAD;

        double sunAnomaly = rev(356.0470 + 0.9856002585 * d);
        double sunPerigee = 282.9404 + 4.70935E-5 * d;
        double sunLongitude = rev(sunAnomaly + sunPerigee);
        double moonLongitude = rev(node + perigee + meanAnomaly);
        double elongation = rev(moonLongitude - sunLongitude);
        double f = rev(moonLongitude - node);
        double m = meanAnomaly;
        double dd = elongation;
        double ms = sunAnomaly;
        lon = lon - 1.274 * sin(m - 2 * dd) + 0.658 * sin(2 * dd) - 0.186 * sin(ms) - 0.059 * sin(2 * m - 2 * dd)
                - 0.057 * sin(m - 2 * dd + ms) + 0.053 * sin(m + 2 * dd) + 0.046 * sin(2 * dd - ms) + 0.041 * sin(m - ms)
                - 0.035 * sin(dd) - 0.031 * sin(m + ms) - 0.015 * sin(2 * f - 2 * dd) + 0.011 * sin(m - 4 * dd);
        lat = lat - 0.173 * sin(f - 2 * dd) - 0.055 * sin(m - f - 2 * dd) - 0.046 * sin(m + f - 2 * dd)
                + 0.033 * sin(f + 2 * dd) + 0.017 * sin(2 * m + f);

        double obliquity = 23.4393 - 3.563E-7 * d;
        double xe = cos(lon) * cos(lat);
        double ye = sin(lon) * cos(lat) * cos(obliquity) - sin(lat) * sin(obliquity);
        double ze = sin(lon) * cos(lat) * sin(obliquity) + sin(lat) * cos(obliquity);
        double ra = rev(Math.atan2(ye, xe) / RAD);
        double dec = Math.atan2(ze, Math.hypot(xe, ye)) / RAD;
        return new double[]{ra, dec};
    }

    static double[] moonAltitude(long ms, double lat, double lng) {
        double d = dayNumber(ms);
        double[] raDec = moonRaDec(d);
        double d0 = Math.floor(d);
        double ut = (d - d0) * 24;
        double sunAnomaly0 = rev(356.0470 + 0.9856002585 * d0);
        double sunPerigee0 = 282.9404 + 4.70935E-5 * d0;
        double siderealTime0 = rev(sunAnomaly0 + sunPerigee0 + 180);
        double localSiderealTime = rev(siderealTime0 + ut * 15.04107 + lng);
        double hourAngle = rev(localSiderealTime - raDec[0]);
        double altitude = Math.asin(sin(lat) * sin(raDec[1]) + cos(lat) * cos(raDec[1]) * cos(hourAngle)) / RAD;
        return new double[]{altitude, hourAngle};
    }

    private static void moonEvents(Day day, double lat, double lng) {
        double horizon = 0.125;
        long step = 10 * MINUTE;
        double[] before = moonAltitude(day.start, lat, lng);
        for (long t = day.start + step; t <= day.start + DAY; t = t + step) {
            double[] now = moonAltitude(t, lat, lng);

            if (before[0] < horizon && now[0] >= horizon) {
                day.moonrise = between(t - step, t, before[0] - horizon, now[0] - horizon);
                day.minors.add(new long[]{day.moonrise - 30 * MINUTE, day.moonrise + 30 * MINUTE});
            } else if (before[0] >= horizon && now[0] < horizon) {
                day.moonset = between(t - step, t, before[0] - horizon, now[0] - horizon);
                day.minors.add(new long[]{day.moonset - 30 * MINUTE, day.moonset + 30 * MINUTE});
            }

            if (before[1] > 270 && now[1] < 90) {
                long overhead = between(t - step, t, before[1] - 360, now[1]);
                day.transit = overhead;
                day.majors.add(new long[]{overhead - 60 * MINUTE, overhead + 60 * MINUTE});
            } else if (before[1] < 180 && now[1] >= 180) {
                long underfoot = between(t - step, t, before[1] - 180, now[1] - 180);
                day.majors.add(new long[]{underfoot - 60 * MINUTE, underfoot + 60 * MINUTE});
            }
            before = now;
        }
    }

    private static long between(long t1, long t2, double v1, double v2) {
        if (v2 == v1) {
            return t1;
        }
        return t1 + (long) ((t2 - t1) * (-v1 / (v2 - v1)));
    }

    private static double rev(double x) {
        x = x % 360;
        if (x < 0) {
            x = x + 360;
        }
        return x;
    }

    private static double sin(double degrees) {
        return Math.sin(degrees * RAD);
    }

    private static double cos(double degrees) {
        return Math.cos(degrees * RAD);
    }
}
