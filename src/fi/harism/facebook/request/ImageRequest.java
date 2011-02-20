package fi.harism.facebook.request;

import java.io.InputStream;
import java.net.URL;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class ImageRequest extends Request {

	private String url;
	private Observer observer;
	private Bitmap bitmap;

	public ImageRequest(Activity activity, Request.Observer requestObserver,
			String url, Observer observer) {
		super(activity, requestObserver);
		this.url = url;
		this.observer = observer;
	}

	public Bitmap getBitmap() {
		return bitmap;
	}

	@Override
	public void runOnThread() throws Exception {
		try {
			URL u = new URL(url);
			InputStream is = u.openStream();
			bitmap = BitmapFactory.decodeStream(is);
		} catch (Exception ex) {
			observer.onError(ex);
			throw ex;
		}
	}

	@Override
	public void runOnUiThread() throws Exception {
		observer.onComplete(this);
	}

	public interface Observer {
		public void onComplete(ImageRequest imageRequest);

		public void onError(Exception ex);
	}

}
