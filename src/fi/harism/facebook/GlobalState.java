package fi.harism.facebook;

import android.app.Application;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import fi.harism.facebook.util.FacebookController;

/**
 * GlobalState class extends Application and is used as base class for our
 * application. It is used for storing application wide data among Activities.
 * 
 * @author harism
 */
public class GlobalState extends Application {

	// Instance of FacebookController.
	private FacebookController facebookController = null;
	// Default profile picture.
	private Bitmap defaultPicture = null;

	/**
	 * Returns instance of FacebookController. Creates one once this method is
	 * called for the first time.
	 * 
	 * @return FacebookController instance.
	 */
	public FacebookController getFacebookController() {
		if (facebookController == null) {
			facebookController = new FacebookController();
		}
		return facebookController;
	}

	/**
	 * Returns instance of default profile picture. Creates one once this method
	 * is called for the first time.
	 * 
	 * @return Default profile picture Bitmap.
	 */
	public Bitmap getDefaultPicture() {
		if (defaultPicture == null) {
			defaultPicture = BitmapFactory.decodeResource(getResources(),
					R.drawable.picture_default);
		}
		return defaultPicture;
	}

}
