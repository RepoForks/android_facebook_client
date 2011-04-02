package fi.harism.facebook.request;

import android.app.Activity;

/**
 * Helper class which splits Request into two methods. execute() is called from
 * separate thread, and executeUI from UI thread.
 * 
 * @author harism
 */
public abstract class RequestUI extends Request {

	private Activity activity;
	private boolean isCancelled = false;

	/**
	 * 
	 * @param key
	 *            Key for identifying this Request.
	 * @param activity
	 *            Activity to execute runOnUiThread() on.
	 */
	public RequestUI(Object key, Activity activity) {
		super(key);
		this.activity = activity;
	}

	@Override
	public final void cancel() {
		isCancelled = true;
	}

	/**
	 * This method is called from separate Thread. Implement this to execute
	 * asynchronous code. If Exception is thrown request execution ends at once.
	 */
	public abstract void execute() throws Exception;

	/**
	 * This method is called always from UI thread. Implement this to update UI.
	 */
	public abstract void executeUI();

	/**
	 * Returns boolean indicating if this Request has been canceled.
	 * 
	 * @return
	 */
	public boolean isCancelled() {
		return isCancelled;
	}

	@Override
	public final void run() {
		if (!isCancelled) {
			try {
				execute();
				activity.runOnUiThread(new RunnableUI());
			} catch (Exception ex) {
				// We don't really care about errors but this prevents
				// executeUI() from being called if execute fails.
			}
		}
	}

	/**
	 * Runnable for UI thread execution.
	 * 
	 * @author harism
	 */
	private class RunnableUI implements Runnable {
		@Override
		public void run() {
			if (!isCancelled) {
				executeUI();
			}
		}
	}

}