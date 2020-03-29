package com.jabistudio.androidjhlabs.filter;

/**
 * Created by Paul on 6/5/2016.
 */
public class Point {
    int y;
    int x;

    public Point(int x, int y) {
        set(x, y);
    }

    public Point() {

    }

    public void set(int x, int y) {
        this.x = x;
        this.y = y;
    }
}