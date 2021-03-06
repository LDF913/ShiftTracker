package com.dgsd.android.shifttracker.util;

import android.app.Activity;

import com.dgsd.android.shifttracker.R;
import com.dgsd.android.shifttracker.STTestRunner;
import com.dgsd.android.shifttracker.activity.HomeActivity;
import com.dgsd.android.shifttracker.activity.SettingsActivity;
import com.dgsd.android.shifttracker.fragment.BaseFragment;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.shadows.support.v4.SupportFragmentTestUtil;
import org.robolectric.util.ActivityController;

import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.observers.TestSubscriber;
import rx.schedulers.TestScheduler;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(STTestRunner.class)
public class RxUtilsTest {

    @Test
    public void testConstructor() {
        new RxUtils(); // For code coverage
    }

    @Test
    public void testBindActivity() {
        final Observable<String> observable = Observable.just("").delay(2, TimeUnit.SECONDS);
        final TestSubscriber<String> subscriber = new TestSubscriber<>();

        ActivityController<SettingsActivity> controller = Robolectric.buildActivity(SettingsActivity.class);

        controller.create();
        controller.resume();

        RxUtils.bindActivity(controller.get(), observable).subscribe(subscriber);
        assertThat(subscriber.isUnsubscribed()).isFalse();

        controller.pause();
        assertThat(subscriber.isUnsubscribed()).isTrue();
    }

    @Test
    public void testBindFragment() throws InterruptedException {
        final Observable<String> observable = Observable.just("").delay(2, TimeUnit.SECONDS);
        final TestSubscriber<String> subscriber = new TestSubscriber<>();

        DummyFragment frag = new DummyFragment();

        SupportFragmentTestUtil.startFragment(frag);

        frag.onResume();
        RxUtils.bindFragment(frag, observable).subscribe(subscriber);
        assertThat(subscriber.isUnsubscribed()).isFalse();

        frag.onPause();
        assertThat(subscriber.isUnsubscribed()).isTrue();
    }

    static class DummyFragment extends BaseFragment {

        @Override
        protected int getLayoutId() {
            return R.layout.act_single_fragment;
        }
    }
}