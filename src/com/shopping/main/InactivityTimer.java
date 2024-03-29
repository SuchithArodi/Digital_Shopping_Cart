package com.shopping.main;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import android.app.Activity;

/**
 * Finishes an activity after a period of inactivity.
 */
final class InactivityTimer {

	private static final int INACTIVITY_DELAY_SECONDS = 5 * 60;

	private final ScheduledExecutorService inactivityTimer = Executors
			.newSingleThreadScheduledExecutor(new DaemonThreadFactory());
	private final Activity activity;
	private ScheduledFuture<?> inactivityFuture = null;

	InactivityTimer(Activity activity) {
		this.activity = activity;
		onActivity();
	}

	void onActivity() {
		cancel();
		inactivityFuture = inactivityTimer.schedule(
				new FinishListener(activity), INACTIVITY_DELAY_SECONDS,
				TimeUnit.SECONDS);
	}

	private void cancel() {
		if (inactivityFuture != null) {
			inactivityFuture.cancel(true);
			inactivityFuture = null;
		}
	}

	void shutdown() {
		cancel();
		inactivityTimer.shutdown();
	}

	private static final class DaemonThreadFactory implements ThreadFactory {
		public Thread newThread(Runnable runnable) {
			Thread thread = new Thread(runnable);
			thread.setDaemon(true);
			return thread;
		}
	}

}
