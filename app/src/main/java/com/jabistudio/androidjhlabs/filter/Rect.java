package com.jabistudio.androidjhlabs.filter;

/**
 * Created by Paul on 6/5/2016.
 */
public class Rect {
    int left;
    int right;
    int top;
    int bottom;

    public Rect(int x, int y, int width, int height) {
        left=x;
        top=y;
        right=left+width;
        bottom=top+height;
    }

    public Rect() {
    }

    public int width() {
        return right-left;
    }

    public int height(){
        return bottom-top;
    }

    public void set(Rect r) {
        left=r.left;
        right=r.right;
        top=r.top;
        bottom=r.bottom;
    }
}
