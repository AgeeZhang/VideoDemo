package com.zzj.videolibrary.utils;

public class TimeFormatUtil {

    public static String formatTime(float timeTemp) {
        int second = (int) Math.floor(timeTemp % 60);
        int minuteTemp = (int) Math.floor(timeTemp / 60);
        if (minuteTemp > 0) {
            int minute = minuteTemp % 60;
            int hour = minuteTemp / 60;
            if (hour > 0) {
                return repair(hour) + ":" + repair(minute) + ":" + repair(second);
            } else {
                return "00:" + repair(minute) + ":" + repair(second);
            }
        } else {
            return "00:00:" + repair(second);
        }
    }

    private static String repair(int number) {
        return number >= 10 ? (number + "") : ("0" + number);
    }
}
