package com.jabistudio.androidjhlabs.filter.util;


import android.graphics.Color;

/**
 * Created by Paul on 6/5/2016.
 */
public class ColorUtils {
    public static void RGBToHSV(int r1, int g1, int b1, float[] hsb) {
       float r = r1/255f;
        float g = g1/255f;
        float b = b1/255f;
        float max = Math.max(Math.max(r, g), b);
        float min = Math.min(Math.min(r, g), b);
        float h = max, s = max, v = max;

        float d = max - min;
        s = max == 0 ? 0 : d / max;

        if (max == min) {
            h = 0; // achromatic
        } else {
            if (max == r) h = (g - b) / d + (g < b ? 6 : 0);
            else if (max == g) h = (b - r) / d + 2;
            else if (max == b) h = (r - g) / d + 4;
            h /= 6;
        }
        hsb[0] = h;
        hsb[1] = s;
        hsb[2] = v;
    }

    public static int HSVToColor(float[] hsb) {
        float[] rgb=new float[3];
        return Color.HSVToColor(rgb);
    }

    public static void hsvToRgb(float h, float s, float v,float[] rgb) {
        float r=0, g=0, b=0;

        int i = (int) Math.floor(h * 6);
        float f = h * 6 - i;
        float p = v * (1 - s);
        float q = v * (1 - f * s);
        float t = v * (1 - (1 - f) * s);

        switch (i % 6) {
            case 0:
                r = v; g = t; b = p;
                break;
            case 1:
                r = q; g = v; b = p;
                break;
            case 2:
                r = p; g = v; b = t;
                break;
            case 3:
                r = p ;g = q; b = v;
                break;
            case 4:
                r = t; g = p; b = v;
                break;
            case 5:
                r = v; g = p; b = q;
                break;
        }
        rgb[0]=r*255;
        rgb[1]=g*255;
        rgb[2]=b*255;
    }

    public static int alpha(int i) {
        return i>>24&0xff;
    }

    public static int red(int i) {
        return i>>16&0xff;
    }
    public static int green(int i) {
        return i>>8&0xff;
    }
    public static int blue(int i) {
        return i&0xff;
    }

    public static int argb(int a, int r, int g, int b) {
        return a<<24|r<<16|g<<8|b;
    }
}
