package com.example.color_detector;

import android.graphics.Color;

import java.util.HashMap;
import java.util.Map;

public class ColorDictionary {
    private static final Map<String, Integer> colorMap = new HashMap<>();


    static {
        colorMap.put("red", Color.parseColor("#FF0000"));
        colorMap.put("green", Color.parseColor("#008000"));
        colorMap.put("blue", Color.parseColor("#0000FF"));
        colorMap.put("yellow", Color.parseColor("#FFFF00"));
        colorMap.put("orange", Color.parseColor("#FFA500"));
        colorMap.put("purple", Color.parseColor("#800080"));
        colorMap.put("cyan", Color.parseColor("#00FFFF"));
        colorMap.put("magenta", Color.parseColor("#FF00FF"));
        colorMap.put("lime", Color.parseColor("#00FF00"));
        colorMap.put("pink", Color.parseColor("#FFC0CB"));
        colorMap.put("teal", Color.parseColor("#008080"));
        colorMap.put("lavender", Color.parseColor("#E6E6FA"));
        colorMap.put("brown", Color.parseColor("#A52A2A"));
        colorMap.put("beige", Color.parseColor("#F5F5DC"));
        colorMap.put("maroon", Color.parseColor("#800000"));
        colorMap.put("mint", Color.parseColor("#98FF98"));
        colorMap.put("olive", Color.parseColor("#808000"));
        colorMap.put("coral", Color.parseColor("#FF7F50"));
        colorMap.put("navy", Color.parseColor("#000080"));
        colorMap.put("grey", Color.parseColor("#808080"));
        colorMap.put("white", Color.parseColor("#FFFFFF"));
        colorMap.put("black", Color.parseColor("#000000"));
    }



    public static String getClosestColorName(int color) {
        String closestColorName = "inconnu"; // "unknown" in French
        double minDistance = Double.MAX_VALUE; // Correct constant for the maximum double value

        for (Map.Entry<String, Integer> entry : colorMap.entrySet()) {
            double distance = colorDistance(color, entry.getValue());
            if (distance < minDistance) {
                minDistance = distance;
                closestColorName = entry.getKey();
            }
        }

        return closestColorName;
    }


    //Search what is the nearest color with euclidien distance
    private static double colorDistance(int color1, int color2) {
        int red1 = (color1 >> 16) & 0xff;
        int green1 = (color1 >> 8) & 0xff;
        int blue1 = (color1) & 0xff;

        int red2 = (color2 >> 16) & 0xff;
        int green2 = (color2 >> 8) & 0xff;
        int blue2 = (color2) & 0xff;

        int redDiff = red1 - red2;
        int greenDiff = green1 - green2;
        int blueDiff = blue1 - blue2;

        return Math.sqrt(redDiff * redDiff + greenDiff * greenDiff + blueDiff * blueDiff);
    }
}

