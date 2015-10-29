package com.dgsd.android.shifttracker.view;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.TextView;

import java.util.Date;
import java.util.GregorianCalendar;

public class MonthViewCell extends TextView {

    private int day = -1;
    private int month = -1;
    private int year = -1;

    private boolean marked;

    public MonthViewCell(final Context context) {
        super(context);
    }

    public MonthViewCell(final Context context, final AttributeSet attrs) {
        super(context, attrs);
    }

    public MonthViewCell(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, widthMeasureSpec);
    }

    public void populate(int day, int month, int year) {
        setEnabled(true);
        this.day = day;
        this.month = month;
        this.year = year;

        setText(String.valueOf(this.day));
    }

    public void clear() {
        day = -1;
        month = -1;
        year = -1;
        setText("");
        setEnabled(false);
    }

    public void isMarked(boolean isMarked) {
        marked = isMarked;
        setTypeface(isMarked ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
    }

    public void isToday(boolean isToday) {
        setActivated(isToday);
        isMarked(this.marked || isToday);
    }

    public boolean hasDate() {
        return day != -1;
    }

    public Date getDate() {
        return new GregorianCalendar(year, month, day).getTime();
    }
}