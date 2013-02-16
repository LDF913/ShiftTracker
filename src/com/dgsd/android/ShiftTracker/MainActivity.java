/*
 * Copyright 2013 Daniel Grech
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.dgsd.android.ShiftTracker;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.format.Time;
import android.widget.ArrayAdapter;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.dgsd.android.ShiftTracker.Adapter.MonthPagerAdapter;
import com.dgsd.android.ShiftTracker.Adapter.WeekPagerAdapter;
import com.dgsd.android.ShiftTracker.Fragment.DatePickerFragment;
import com.dgsd.android.ShiftTracker.Util.*;
import com.viewpagerindicator.TitlePageIndicator;
import de.keyboardsurfer.android.widget.crouton.Crouton;

public class MainActivity extends SherlockFragmentActivity implements DatePickerFragment.OnDateSelectedListener, ActionBar.OnNavigationListener {
    private static final int ANIM_TYPE_PLAIN = 0;
    private static final int ANIM_TYPE_INNER_CUBE = 1;
    private static final int ANIM_TYPE_OUTER_CUBE = 2;
    private static final int ANIM_TYPE_TWIST = 3;
    private static final int ANIM_TYPE_COMPRESS = 4;

    private static final int NAV_INDEX_WEEK = 0;
    private static final int NAV_INDEX_MONTH = 1;

    private TitlePageIndicator mIndicator;
    private ViewPager mPager;
    private WeekPagerAdapter mWeekPagerAdapter;
    private MonthPagerAdapter mMonthPagerAdapter;
    private DatePickerFragment mGoToFragment;

    private Prefs mPrefs;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final ActionBar ab = getSupportActionBar();
        ab.setDisplayShowTitleEnabled(false);
        ab.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        ab.setListNavigationCallbacks(ArrayAdapter.createFromResource(this, R.array.nav_items,
                android.R.layout.simple_spinner_dropdown_item), this);


        mPrefs = Prefs.getInstance(this);

        //Show our 'Rate in Market' dialog if needed
        AppRating.app_launched(this);

        final int currentJd = TimeUtils.getCurrentJulianDay();
        mWeekPagerAdapter = new WeekPagerAdapter(this, getSupportFragmentManager(), currentJd);
        mMonthPagerAdapter = new MonthPagerAdapter(this, getSupportFragmentManager(), currentJd);

        mPager = (ViewPager) findViewById(R.id.pager);
        mPager.setAdapter(mWeekPagerAdapter);

        if(Api.isMin(Api.HONEYCOMB) && !StApp.isFreeApp(this)) {
            final int animType = Integer.valueOf(mPrefs.get(getString(R.string.settings_key_animation), "0"));

            ViewPager.PageTransformer transformer = null;
            switch(animType) {
                case ANIM_TYPE_INNER_CUBE:
                    transformer = PageTransformerUtils.getInnerCubeTransformer();
                    break;
                case ANIM_TYPE_OUTER_CUBE:
                    transformer = PageTransformerUtils.getOuterCubeTransformer();
                    break;
                case ANIM_TYPE_TWIST:
                    transformer = PageTransformerUtils.getTwistTransformer();
                    break;
                case ANIM_TYPE_COMPRESS:
                    transformer = PageTransformerUtils.getCompressTransformer();
                    break;
            }

            if(transformer != null)
                mPager.setPageTransformer(false, transformer);
        }

        mIndicator = (TitlePageIndicator) findViewById(R.id.indicator);
        mIndicator.setViewPager(mPager, mWeekPagerAdapter.getPositionForJulianDay(currentJd));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getSupportMenuInflater().inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.settings) {
            startActivity(new Intent(this, SettingsActivity.class));
        } else if(item.getItemId() == R.id.go_to) {
            if(mGoToFragment != null && mGoToFragment.isResumed()) {
                //We're showing already!
                return true;
            }

            final int centerJd = mWeekPagerAdapter.getJulianDayForPosition(mWeekPagerAdapter.getCenterPosition());
            final int count = mWeekPagerAdapter.getCount() * 7;

            final Time time = new Time();
            time.setJulianDay(centerJd - (count / 2));
            final long min = time.toMillis(true);

            time.setJulianDay(centerJd + (count / 2));
            final long max = time.toMillis(true);

            mGoToFragment = DatePickerFragment.newInstance("Go to date..", min, max, -1);
            mGoToFragment.setOnDateSelectedListener(this);
            mGoToFragment.show(getSupportFragmentManager(), null);
        } else if(item.getItemId() == R.id.get_full_version) {
            Uri uri = Uri.parse("market://details?id=com.dgsd.android.ShiftTracker");
            startActivity(new Intent(Intent.ACTION_VIEW, uri));
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDateSelected(int typeCode, int julianDay) {
        mIndicator.setCurrentItem(mWeekPagerAdapter.getPositionForJulianDay(julianDay));
    }

    @Override
    protected void onDestroy() {
        Crouton.clearCroutonsForActivity(this);
        super.onDestroy();
    }

    @Override
    public boolean onNavigationItemSelected(final int pos, final long itemId) {
        final int currentJd = getCurrentlyDisplayedJd();

        switch(pos) {
            case NAV_INDEX_WEEK:
                mPager.setAdapter(mWeekPagerAdapter);
                mIndicator.setViewPager(mPager, mWeekPagerAdapter.getPositionForJulianDay(currentJd));
                break;
            case NAV_INDEX_MONTH:
                mPager.setAdapter(mMonthPagerAdapter);
                mIndicator.setViewPager(mPager, mMonthPagerAdapter.getPositionForJulianDay(currentJd));

                for(int i = 0; i < mMonthPagerAdapter.getCount(); i++) {
                    System.err.print("========= " + mMonthPagerAdapter.getPageTitle(i));
                    if(i == mMonthPagerAdapter.getCenterPosition())
                        System.err.print(" <-----");

                    System.err.print("\n");
                }

                break;
        }
        return true;
    }

    private int getCurrentlyDisplayedJd() {
        final PagerAdapter adapter = mPager.getAdapter();

        if(adapter == mWeekPagerAdapter)
            return mWeekPagerAdapter.getJulianDayForPosition(mPager.getCurrentItem());
        else if(adapter == mMonthPagerAdapter)
            return mMonthPagerAdapter.getSelectedJulianDay();
        else
            return TimeUtils.getCurrentJulianDay();
    }
}