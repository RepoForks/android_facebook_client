package fi.harism.facebook;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import fi.harism.facebook.dao.FBBitmap;
import fi.harism.facebook.dao.FBFriendList;
import fi.harism.facebook.dao.FBUser;
import fi.harism.facebook.request.RequestUI;
import fi.harism.facebook.util.BitmapUtils;
import fi.harism.facebook.util.FacebookURLSpan;
import fi.harism.facebook.util.StringUtils;
import fi.harism.facebook.view.ProfilePictureView;

/**
 * Friends list Activity. Once created it first loads "me/friends" from Facebook
 * Graph API. Once friend list is received it creates corresponding friend items
 * to view and triggers asynchronous loading of profile pictures.
 * 
 * This Activity implements also search functionality for friend list.
 * 
 * @author harism
 */
public class FriendsActivity extends BaseActivity {

	// Default profile picture.
	private Bitmap defaultPicture = null;
	// Radius value for rounding profile images.
	private static final int PICTURE_ROUND_RADIUS = 7;
	// Span onClick observer for profile and comments protocols.
	private SpanClickObserver spanClickObserver = null;
	// Static protocol name for showing profile.
	private static final String PROTOCOL_SHOW_PROFILE = "showprofile://";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_friends);

		spanClickObserver = new SpanClickObserver(this);

		// Add text changed observer to search editor.
		SearchEditorObserver searchObserver = new SearchEditorObserver();
		EditText searchEditor = (EditText) findViewById(R.id.friends_edit_search);
		searchEditor.addTextChangedListener(searchObserver);

		// Create default picture shared among friend items.
		defaultPicture = getGlobalState().getDefaultPicture();
		defaultPicture = BitmapUtils.roundBitmap(defaultPicture,
				PICTURE_ROUND_RADIUS);

		// Trigger asynchronous friend list loading if needed.
		FBFriendList fbFriendList = getGlobalState().getFBFactory()
				.getFriendList();
		if (fbFriendList.getFriends().size() > 0) {
			updateFriendList(fbFriendList);
		} else {
			// Show progress dialog.
			showProgressDialog();
			FBFriendListRequest request = new FBFriendListRequest(this,
					fbFriendList);
			getGlobalState().getRequestQueue().addRequest(request);
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

	/**
	 * This method creates a new View instance for user item. It sets user name
	 * to given value and stores userId as a tag to view for later use.
	 * 
	 * @param userId
	 *            User Id.
	 * @param name
	 *            User name.
	 * @return New friend item view.
	 */
	private final View createFriendItem(String userId, String name) {
		// Create new friend item View.
		View friendItemView = getLayoutInflater().inflate(R.layout.view_friend,
				null);

		// Find name TextView and set its value.
		TextView nameTextView = (TextView) friendItemView
				.findViewById(R.id.view_friend_name);
		StringUtils.setTextLink(nameTextView, name, PROTOCOL_SHOW_PROFILE
				+ userId, spanClickObserver);
		// nameTextView.setText(name);

		// Set default profile picture.
		// ProfilePictureView profilePic = (ProfilePictureView) friendItemView
		// .findViewById(R.id.view_friend_picture);
		// profilePic.setBitmap(defaultPicture);

		// Store user id as a tag to friend item View.
		friendItemView.setTag(userId);

		// This is rather useless at the moment, as we are calling this method
		// mostly once search text has not been changed. But just in case for
		// the future, toggle friend item View's visibility according to search
		// text.
		EditText searchEditText = (EditText) findViewById(R.id.friends_edit_search);
		String searchText = searchEditText.getText().toString();
		toggleFriendItemVisibility(friendItemView, searchText);

		return friendItemView;
	}

	/**
	 * Toggles friend item View's visibility according to given search text.
	 * Method tries to find given search text within the name TextView found
	 * inside given friendItem.
	 * 
	 * @param friendItem
	 *            Friend item View.
	 * @param searchText
	 *            Current search text.
	 */
	private final void toggleFriendItemVisibility(View friendItem,
			String searchText) {
		// We are not case sensitive.
		searchText = searchText.toLowerCase();

		// Locate name TextView.
		TextView nameTextView = (TextView) friendItem
				.findViewById(R.id.view_friend_name);
		// Get name from TextView.
		String friendName = nameTextView.getText().toString();
		// We are still not case sensitive.
		friendName = friendName.toLowerCase();

		// Toggle friend item visibility regarding to if searchText is found
		// within name. This is rather naive approach but works good enough :)
		if (friendName.contains(searchText)) {
			friendItem.setVisibility(View.VISIBLE);
		} else {
			friendItem.setVisibility(View.GONE);
		}
	}

	/**
	 * Updates friend list to screen.
	 */
	private final void updateFriendList(FBFriendList fbFriendList) {
		// LinearLayout which is inside ScrollView.
		LinearLayout friendsView = (LinearLayout) findViewById(R.id.friends_list);
		friendsView.removeAllViews();

		for (FBUser friend : fbFriendList.getFriends()) {
			String userId = friend.getId();
			String name = friend.getName();
			String pictureUrl = friend.getPicture();

			// Create default friend item view.
			View friendView = createFriendItem(userId, name);
			// Add friend item view to scrollable list.
			friendsView.addView(friendView);

			ProfilePictureView profilePic = (ProfilePictureView) friendView
					.findViewById(R.id.view_friend_picture);
			FBBitmap picture = getGlobalState().getFBFactory().getBitmap(
					pictureUrl);
			Bitmap bitmap = picture.getBitmap();
			if (bitmap != null) {
				profilePic.setBitmap(BitmapUtils.roundBitmap(bitmap,
						PICTURE_ROUND_RADIUS));
			} else {
				profilePic.setBitmap(defaultPicture);
				FBBitmapRequest request = new FBBitmapRequest(this, profilePic,
						picture);
				getGlobalState().getRequestQueue().addRequest(request);
			}
		}
	}

	/**
	 * Request for handling profile picture loading.
	 */
	private final class FBBitmapRequest extends RequestUI {

		private ProfilePictureView profilePic;
		private FBBitmap fbBitmap;
		private Bitmap bitmap;

		public FBBitmapRequest(Activity activity,
				ProfilePictureView profilePic, FBBitmap fbBitmap) {
			super(activity, activity);
			this.profilePic = profilePic;
			this.fbBitmap = fbBitmap;
		}

		@Override
		public void execute() throws Exception {
			fbBitmap.load();
			bitmap = BitmapUtils.roundBitmap(fbBitmap.getBitmap(),
					PICTURE_ROUND_RADIUS);
		}

		@Override
		public void executeUI() {
			profilePic.setBitmap(bitmap);
		}
	}

	/**
	 * Request for handling "me/friends" loading.
	 */
	private final class FBFriendListRequest extends RequestUI {

		private FBFriendList fbFriendList;

		public FBFriendListRequest(Activity activity, FBFriendList fbFriendList) {
			super(activity, activity);
			this.fbFriendList = fbFriendList;
		}

		@Override
		public void execute() throws Exception {
			try {
				fbFriendList.load();
			} catch (Exception ex) {
				hideProgressDialog();
				showAlertDialog(ex.toString());
				throw ex;
			}
		}

		@Override
		public void executeUI() {
			updateFriendList(fbFriendList);
			hideProgressDialog();
		}
	}

	/**
	 * Observer for handling search text changes.
	 */
	private final class SearchEditorObserver implements TextWatcher {
		@Override
		public void afterTextChanged(Editable editable) {
			// Get editor text.
			String searchText = editable.toString();
			// Find LinearLayout containing our friend items.
			LinearLayout friendList = (LinearLayout) findViewById(R.id.friends_list);
			// Iterate through all child Views.
			for (int i = 0; i < friendList.getChildCount(); ++i) {
				LinearLayout friendItem = (LinearLayout) friendList
						.getChildAt(i);
				toggleFriendItemVisibility(friendItem, searchText);
			}
		}

		@Override
		public void beforeTextChanged(CharSequence arg0, int arg1, int arg2,
				int arg3) {
			// We are not interested in this callback.
		}

		@Override
		public void onTextChanged(CharSequence arg0, int arg1, int arg2,
				int arg3) {
			// We are not interested in this callback.
		}
	}

	/**
	 * Click listener for our own protocols. Rest is handled by default handler.
	 */
	private final class SpanClickObserver implements
			FacebookURLSpan.ClickObserver {
		private BaseActivity activity = null;

		public SpanClickObserver(BaseActivity activity) {
			this.activity = activity;
		}

		@Override
		public boolean onClick(FacebookURLSpan span) {
			String url = span.getURL();
			if (url.startsWith(PROTOCOL_SHOW_PROFILE)) {
				showAlertDialog(url);
				return true;
			}
			return false;
		}
	}

}
