package fi.harism.facebook;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.animation.AlphaAnimation;
import android.widget.Button;
import android.widget.TextView;
import fi.harism.facebook.dao.FBBitmap;
import fi.harism.facebook.dao.FBUser;
import fi.harism.facebook.net.FBClient;
import fi.harism.facebook.request.RequestUI;
import fi.harism.facebook.view.ProfilePictureView;

/**
 * Main Activity of this application. Once Activity is launched it starts to
 * fetch default information from currently logged in user in Facebook API.
 * 
 * @author harism
 */
public class MainActivity extends BaseActivity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);

		setContentView(R.layout.activity_main);
		final Activity self = this;

		// Set default picture as user picture.
		Bitmap picture = getGlobalState().getDefaultPicture();
		ProfilePictureView picView = (ProfilePictureView) findViewById(R.id.activity_main_profile_picture);
		picView.setBitmap(picture);

		// Add onClick listener to "Friends" button.
		Button friendsButton = (Button) findViewById(R.id.activity_main_button_friends);
		friendsButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// On click trigger friends activity.
				Intent i = createIntent(FriendsActivity.class);
				startActivity(i);
			}
		});

		// Add onClick listener to "News Feed" button.
		Button newsFeedButton = (Button) findViewById(R.id.activity_main_button_news);
		newsFeedButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// On click trigger feed activity.
				Intent i = createIntent(FeedActivity.class);
				i.putExtra("fi.harism.facebook.FeedActivity.path", "me/home");
				i.putExtra(
						"fi.harism.facebook.FeedActivity.title",
						getResources().getString(
								R.string.activity_feed_news_title));
				startActivity(i);
			}
		});

		// Add onClick listener to "Wall" button.
		Button wallButton = (Button) findViewById(R.id.activity_main_button_wall);
		wallButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// On click trigger feed activity.
				Intent i = createIntent(FeedActivity.class);
				i.putExtra("fi.harism.facebook.FeedActivity.path", "me/feed");
				i.putExtra(
						"fi.harism.facebook.FeedActivity.title",
						getResources().getString(
								R.string.activity_feed_profile_title));
				startActivity(i);
			}
		});

		// Add onClick listener to "Profile" button.
		Button profileButton = (Button) findViewById(R.id.activity_main_button_profile);
		profileButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent i = createIntent(UserActivity.class);
				i.putExtra("fi.harism.facebook.UserActivity.user", "me");
				startActivity(i);
			}
		});

		// Add onClick listener to "Chat" button.
		Button chatButton = (Button) findViewById(R.id.activity_main_button_chat);
		chatButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// On click trigger feed activity.
				Intent i = createIntent(ChatActivity.class);
				startActivity(i);
			}
		});

		// Add onClick listener to "Logout" button.
		View logoutButton = findViewById(R.id.activity_main_button_logout);
		logoutButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				LogoutObserver observer = new LogoutObserver();
				getGlobalState().getFBClient().logout(self, observer);
			}
		});

		// Update user information asynchronously if needed.
		FBUser fbUserMe = getGlobalState().getFBFactory().getUser("me");
		if (fbUserMe.getLevel() == FBUser.Level.FULL) {
			updateProfileInfo(fbUserMe);
		} else {
			FBUserRequest meRequest = new FBUserRequest(this, fbUserMe);
			getGlobalState().getRequestQueue().addRequest(meRequest);
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		getGlobalState().getRequestQueue().removeRequests(this);
	}

	@Override
	public void onPause() {
		super.onPause();
		getGlobalState().getRequestQueue().setPaused(this, true);
	}

	@Override
	public void onResume() {
		super.onResume();
		getGlobalState().getRequestQueue().setPaused(this, false);
	}

	private void updateProfileInfo(FBUser fbUserMe) {
		TextView nameView = (TextView) findViewById(R.id.activity_main_name);
		nameView.setText(fbUserMe.getName());

		TextView statusView = (TextView) findViewById(R.id.activity_main_status);
		if (fbUserMe.getStatus() == null || fbUserMe.getStatus().length() == 0) {
			statusView.setVisibility(View.GONE);
		} else {
			statusView.setText(fbUserMe.getStatus());
		}

		TextView loadingView = (TextView) findViewById(R.id.activity_main_loading);

		Rect r = new Rect();
		if (nameView.getLocalVisibleRect(r)
				&& statusView.getLocalVisibleRect(r)) {
			AlphaAnimation inAnimation = new AlphaAnimation(0, 1);
			inAnimation.setDuration(700);
			nameView.startAnimation(inAnimation);
			statusView.startAnimation(inAnimation);

			AlphaAnimation outAnimation = new AlphaAnimation(1, 0);
			outAnimation.setDuration(700);
			outAnimation.setFillAfter(true);
			loadingView.startAnimation(outAnimation);
		} else {
			loadingView.setVisibility(View.GONE);
		}

		FBBitmap fbBitmapMe = getGlobalState().getFBFactory().getBitmap(
				fbUserMe.getPicture());
		if (fbBitmapMe.getBitmap() != null) {
			updateProfilePicture(fbBitmapMe);
		} else {
			FBBitmapRequest request = new FBBitmapRequest(this, fbBitmapMe);
			getGlobalState().getRequestQueue().addRequest(request);
		}
	}

	private void updateProfilePicture(FBBitmap fbBitmapMe) {
		ProfilePictureView picView = (ProfilePictureView) findViewById(R.id.activity_main_profile_picture);
		picView.setBitmap(fbBitmapMe.getBitmap());
	}

	/**
	 * Class for handling profile picture request.
	 */
	private final class FBBitmapRequest extends RequestUI {

		private FBBitmap fbBitmap;

		public FBBitmapRequest(Activity activity, FBBitmap fbBitmap) {
			super(activity, activity);
			this.fbBitmap = fbBitmap;
		}

		@Override
		public void execute() throws Exception {
			fbBitmap.load();
		}

		@Override
		public void executeUI() {
			updateProfilePicture(fbBitmap);
		}
	}

	/**
	 * Class for handling "me" request.
	 */
	private final class FBUserRequest extends RequestUI {

		private FBUser mFBUser;

		public FBUserRequest(Activity activity, FBUser fbUser) {
			super(activity, activity);
			mFBUser = fbUser;
		}

		@Override
		public void execute() throws Exception {
			try {
				mFBUser.load(FBUser.Level.FULL);
			} catch (Exception ex) {
				// TODO: This is rather disastrous situation actually.
				showAlertDialog(ex.toString());
				throw ex;
			}
		}

		@Override
		public void executeUI() {
			updateProfileInfo(mFBUser);
		}

	}

	/**
	 * LogoutObserver for handling asynchronous logout procedure.
	 */
	private final class LogoutObserver implements FBClient.LogoutObserver {
		@Override
		public void onComplete() {
			// First hide progress dialog.
			hideProgressDialog();
			getGlobalState().getFBFactory().reset();
			// Switch to login view.
			Intent intent = createIntent(LoginActivity.class);
			startActivity(intent);
			finish();
		}

		@Override
		public void onError(Exception ex) {
			// Hide progress dialog.
			hideProgressDialog();
			// Show error alert.
			showAlertDialog(ex.getLocalizedMessage());
		}
	}

}
