package org.rxjournal.impl;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import org.junit.Assert;
import org.junit.Test;
import org.rxjournal.util.DSUtil;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * Created by daniel on 17/05/17.
 */
public class RxReplayRateTest {
    @Test
    public void replayRateTest() throws IOException {
        long[] time = new long[1];
        Flowable<String> errorFlowable = Flowable.create(
                e -> {
                    time[0] = System.currentTimeMillis();
                    e.onNext("one");
                    DSUtil.sleep(1000);
                    e.onNext("two");
                    e.onComplete();
                },
                BackpressureStrategy.BUFFER
        );


        RxJournal rxJournal = new RxJournal("/tmp/replayRateTest");
        rxJournal.clearCache();

        //Pass the input stream into the rxRecorder which will subscribe to it and record all events.
        //The subscription will not be activated on a new thread which will allow this program to continue.
        RxRecorder rxRecorder = rxJournal.createRxRecorder();
        rxRecorder.record(errorFlowable, "replayRateTest");

        RxPlayer rxPlayer = rxJournal.createRxPlayer();
        PlayOptions options = new PlayOptions().filter("replayRateTest");
        Observable recordedObservable = rxPlayer.play(options);

        AtomicInteger onNext = new AtomicInteger(0);
        AtomicInteger onComplete = new AtomicInteger(0);
        AtomicInteger onError = new AtomicInteger(0);

        long[] gapBetweenOneAndTwo = new long[1];
        long[] startTime = new long[1];

        recordedObservable.subscribe(i -> {
                    String s = (String) i;
                    if (s.equals("one")) {
                        startTime[0] = System.currentTimeMillis();
                        onNext.incrementAndGet();
                    }
                    if (s.equals("two")) {
                        gapBetweenOneAndTwo[0] = System.currentTimeMillis() - startTime[0];
                        onNext.incrementAndGet();
                    }
                },
                e -> {
                    onError.incrementAndGet();
                },
                () -> onComplete.incrementAndGet());

        Assert.assertEquals(2, onNext.get());
        Assert.assertEquals(0, onError.get());
        Assert.assertEquals(1, onComplete.get());
        System.out.println("Gap with actual was " + gapBetweenOneAndTwo[0]);
        Assert.assertTrue(gapBetweenOneAndTwo[0] > 900);


        rxPlayer = rxJournal.createRxPlayer();
        options = new PlayOptions().filter("replayRateTest").replayRate(PlayOptions.ReplayRate.FAST);
        recordedObservable = rxPlayer.play(options);

        AtomicInteger onNextFast = new AtomicInteger(0);
        AtomicInteger onCompleteFast = new AtomicInteger(0);
        AtomicInteger onErrorFast = new AtomicInteger(0);

        long[] gapBetweenOneAndTwoFast = new long[1];
        long[] startTimeFast = new long[1];

        recordedObservable.subscribe(i -> {
                    String s = (String) i;

                    if (s.equals("one")) {
                        startTimeFast[0] = System.currentTimeMillis();
                        onNextFast.incrementAndGet();
                    }
                    if (s.equals("two")) {
                        gapBetweenOneAndTwoFast[0] = System.currentTimeMillis() - startTimeFast[0];
                        onNextFast.incrementAndGet();
                    }
                },
                e -> {
                    onErrorFast.incrementAndGet();
                },
                () -> onCompleteFast.incrementAndGet());

        Assert.assertEquals(2, onNextFast.get());
        Assert.assertEquals(0, onErrorFast.get());
        Assert.assertEquals(1, onCompleteFast.get());
        System.out.println("Gap with fast was " + gapBetweenOneAndTwoFast[0]);
        Assert.assertTrue(gapBetweenOneAndTwoFast[0] < 2);
    }
}
