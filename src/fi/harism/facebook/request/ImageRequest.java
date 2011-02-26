package fi.harism.facebook.request;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import fi.harism.facebook.BaseActivity;
import fi.harism.facebook.util.DataCache;

/**
 * ImageRequest class for loading images asynchronously.
 * 
 * @author harism
 */
public class ImageRequest extends Request {

	// Image URL.
	private String url;
	// Observer for ImageRequest.
	private ImageRequest.Observer observer;
	// Bitmap we loaded.
	private Bitmap bitmap;
	// Flag whether Bitmap should be cached.
	private boolean cacheBitmap;
	// Caller activity.
	private BaseActivity activity;

	/**
	 * Constructor for ImageRequest.
	 * 
	 * @param activity
	 *            Activity to which use for runOnUiThread.
	 * @param url
	 *            Image URL.
	 * @param observer
	 *            ImageRequest observer.
	 */
	public ImageRequest(BaseActivity activity, String url,
			ImageRequest.Observer observer) {
		super(activity);
		this.activity = activity;
		this.url = url;
		this.observer = observer;
		bitmap = null;
		cacheBitmap = false;
	}

	/**
	 * Once ImageRequest is completed successfully this method returns loaded
	 * Bitmap.
	 * 
	 * @return Loaded Bitmap or null on error.
	 */
	public Bitmap getBitmap() {
		return bitmap;
	}

	@Override
	public void runOnThread() throws Exception {
		DataCache dataCache = activity.getGlobalState().getDataCache();
		// Always check dataCache for cached image.
		if (dataCache.containsKey(url)) {
			byte bitmapData[] = dataCache.getData(url);
			bitmap = BitmapFactory.decodeByteArray(bitmapData, 0,
					bitmapData.length);
		}
		// No cached data found.
		else {
			try {
				// Open InputStream for given url.
				URL u = new URL(url);
				InputStream is = u.openStream();
				ByteArrayOutputStream imageBuffer = new ByteArrayOutputStream();

				// Read actual data from InputStream.
				int readLength;
				byte buffer[] = new byte[1024];
				while ((readLength = is.read(buffer)) != -1) {
					imageBuffer.write(buffer, 0, readLength);
				}

				buffer = imageBuffer.toByteArray();
				bitmap = BitmapFactory
						.decodeByteArray(buffer, 0, buffer.length);

				// If cacheBitmap is set, store loaded data into dataCache.
				if (cacheBitmap) {
					dataCache.setData(url, buffer);
				}
			} catch (Exception ex) {
				observer.onError(ex);
				throw ex;
			}
		}
	}

	@Override
	public void runOnUiThread() throws Exception {
		observer.onComplete(this);
	}

	/**
	 * Sets flag whether this ImageRequest should store bitmap data to DataCache
	 * once image data has been loaded. Image caching is turned off by default.
	 * 
	 * @param cacheBitmap
	 *            Boolean whether loaded image should be put to DataCache.
	 */
	public void setCacheBitmap(boolean cacheBitmap) {
		this.cacheBitmap = cacheBitmap;
	}

	/**
	 * ImageRequest observer interface.
	 */
	public interface Observer {
		/**
		 * Called once ImageRequest is done successfully.
		 * 
		 * @param imageRequest
		 *            ImageRequest object being completed.
		 */
		public void onComplete(ImageRequest imageRequest);

		/**
		 * Called if ImageRequest failed.
		 * 
		 * @param ex
		 *            Exception explaining the cause for failure.
		 */
		public void onError(Exception ex);
	}
}
