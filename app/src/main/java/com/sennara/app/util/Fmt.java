package com.sennara.app.util;

import java.text.DateFormat;
import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Fmt {

    public static String hms(long ms) {
        long seconds = ms / 1000;
        if (seconds < 0) {
            seconds = 0;
        }
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long rest = seconds % 60;
        return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, rest);
    }

    public static String hm(long time) {
        SimpleDateFormat format = new SimpleDateFormat("HH:mm", Locale.US);
        return format.format(new Date(time));
    }

    public static String hour(long time) {
        SimpleDateFormat format = new SimpleDateFormat("HH", Locale.US);
        return format.format(new Date(time)) + "h";
    }

    public static String date(long time, Locale locale) {
        return DateFormat.getDateInstance(DateFormat.MEDIUM, locale).format(new Date(time));
    }

    public static String dateTime(long time, Locale locale) {
        return DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, locale).format(new Date(time));
    }

    public static String longDay(long time, Locale locale) {
        return new SimpleDateFormat("EEEE d MMMM", locale).format(new Date(time));
    }

    public static String weekday(long time, Locale locale) {
        return new SimpleDateFormat("EEE", locale).format(new Date(time));
    }

    public static String month(int month, Locale locale) {
        String[] names = new DateFormatSymbols(locale).getShortMonths();
        int index = month - 1;
        if (index < 0) {
            index = 0;
        }
        if (index > 11) {
            index = 11;
        }
        return names[index];
    }

    public static String weight(int grams) {
        if (grams < 1000) {
            return grams + " g";
        }
        return String.format(Locale.US, "%.2f kg", grams / 1000.0);
    }

    public static String price(double dinars) {
        return String.format(Locale.US, "%.3f DT", dinars);
    }

    public static String num(double v, int decimals) {
        if (Double.isNaN(v)) return "–";
        return String.format(Locale.US, "%." + decimals + "f", v);
    }
}
