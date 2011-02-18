package fi.harism.facebook.net;

import org.json.JSONObject;

import fi.harism.facebook.util.FacebookController;
import android.app.Activity;
import android.os.Bundle;

public class FacebookRequest extends Request {

	private String path;
	private Bundle bundle;
	private Observer observer;
	private JSONObject response;

	public FacebookRequest(Activity activity, String path, Observer observer) {
		super(activity);
		this.path = path;
		this.bundle = null;
		this.observer = observer;
	}

	public FacebookRequest(Activity activity, String path, Bundle bundle,
			Observer observer) {
		super(activity);
		this.path = path;
		this.bundle = bundle;
		this.observer = observer;
	}

	@Override
	public void runOnThread() throws Exception {
		try {
			String r;
			FacebookController c = FacebookController.getFacebookController();
			if (bundle != null) {
				r = c.request(path, bundle);
			} else {
				r = c.request(path);
			}

			response = new JSONObject(r);

			if (response.has("error")) {
				JSONObject err = response.getJSONObject("error");
				Exception ex = new Exception(err.getString("type") + " : "
						+ err.getString("message"));
				throw ex;
			}
		} catch (Exception ex) {
			observer.onError(ex);
			throw ex;
		}
	}

	@Override
	public void runOnUiThread() throws Exception {
		observer.onComplete(response);
	}

	public interface Observer {
		public void onError(Exception ex);

		public void onComplete(JSONObject response);
	}

}
