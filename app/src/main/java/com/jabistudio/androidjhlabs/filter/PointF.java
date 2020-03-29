package com.jabistudio.androidjhlabs.filter;

/**
 * Created by Paul on 6/5/2016.
 */
public class PointF {
    float y;
    float x;

    public PointF(float x, float y) {
        set(x, y);
    }

    public PointF() {

    }

    public void set(float x, float y) {
        this.x = x;
        this.y = y;
    }
}
