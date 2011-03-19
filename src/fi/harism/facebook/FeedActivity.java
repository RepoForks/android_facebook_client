package fi.harism.facebook;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import fi.harism.facebook.dao.DAONewsFeedItem;
import fi.harism.facebook.dao.DAONewsFeedList;
import fi.harism.facebook.dao.DAOObserver;
import fi.harism.facebook.dao.DAOProfile;
import fi.harism.facebook.net.RequestController;
import fi.harism.facebook.util.BitmapUtils;
import fi.harism.facebook.util.FacebookURLSpan;
import fi.harism.facebook.util.StringUtils;

/**
 * Feed Activity for showing latest News Feed events for logged in user.
 * 
 * @author harism
 */
public class FeedActivity extends BaseActivity {

	// RequestController instance.
	private RequestController requestController = null;
	// Default picture used as sender's profile picture.
	private Bitmap defaultPicture = null;
	// Rounding radius for user picture.
	// TODO: Move this value to resources instead.
	private static final int PICTURE_ROUND_RADIUS = 7;

	// Span onClick observer for profile and comments protocols.
	private SpanClickObserver spanClickObserver = null;
	// Static protocol name for showing profile.
	private static final String PROTOCOL_SHOW_PROFILE = "showprofile://";
	// Static protocol name for showing comments.
	private static final String PROTOCOL_SHOW_COMMENTS = "showcomments://";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.feed);

		// Create default picture from resources.
		defaultPicture = getGlobalState().getDefaultPicture();
		defaultPicture = BitmapUtils.roundBitmap(defaultPicture,
				PICTURE_ROUND_RADIUS);

		requestController = getGlobalState().getRequestController();
		spanClickObserver = new SpanClickObserver(this);

		showProgressDialog();
		requestController.getNewsFeed(this, new DAONewsFeedListObserver(this));
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		requestController.removeRequests(this);
	}

	@Override
	public void onPause() {
		super.onPause();
		requestController.setPaused(this, true);
	}

	@Override
	public void onResume() {
		super.onResume();
		requestController.setPaused(this, false);
	}

	/**
	 * Creates new feed item.
	 * 
	 * @param feedItemObject
	 *            Feed item JSONObject to be added.
	 */
	private View createFeedItem(DAONewsFeedItem feedItem) {
		String itemId = feedItem.getId();

		// Create default Feed Item view.
		View feedItemView = getLayoutInflater().inflate(R.layout.feed_item,
				null);
		// We use itemId to find this Feed Item if needed.
		feedItemView.setTag(itemId);

		// We need id of sender later on to trigger profile picture loading.
		String fromId = feedItem.getFromId();
		// Get sender's name or use empty string if none found.
		String fromName = feedItem.getFromName();

		// Set sender's name.
		TextView fromView = (TextView) feedItemView
				.findViewById(R.id.feed_item_from_text);
		StringUtils.setTextLink(fromView, fromName, PROTOCOL_SHOW_PROFILE
				+ fromId, spanClickObserver);

		// Get message from feed item. Message is the one user can add as a
		// description to items posted.
		String message = feedItem.getMessage();
		TextView messageView = (TextView) feedItemView
				.findViewById(R.id.feed_item_message_text);
		if (message != null) {
			StringUtils.setTextLinks(messageView, message, null);
		} else {
			messageView.setVisibility(View.GONE);
		}

		// Get name from feed item. Name is shortish description like string
		// for feed item.
		String name = feedItem.getName();
		TextView nameView = (TextView) feedItemView
				.findViewById(R.id.feed_item_name_text);
		if (name != null) {
			if (feedItem.getLink() != null) {
				StringUtils.setTextLink(nameView, name, feedItem.getLink(),
						null);
			} else {
				nameView.setText(name);
			}
		} else {
			nameView.setVisibility(View.GONE);
		}

		String caption = feedItem.getCaption();
		TextView captionView = (TextView) feedItemView
				.findViewById(R.id.feed_item_caption_text);
		if (caption != null) {
			StringUtils.setTextLinks(captionView, caption, null);
		} else {
			captionView.setVisibility(View.GONE);
		}

		// Get description from feed item. This is longer description for
		// feed item.
		String description = feedItem.getDescription();
		TextView descriptionView = (TextView) feedItemView
				.findViewById(R.id.feed_item_description_text);
		if (description != null) {
			StringUtils.setTextLinks(descriptionView, description, null);
		} else {
			descriptionView.setVisibility(View.GONE);
		}

		// Get created time from feed item.
		String created = feedItem.getCreatedTime();
		TextView detailsView = (TextView) feedItemView
				.findViewById(R.id.feed_item_details_text);
		String details = "";
		if (created != null) {
			details += StringUtils.convertFBTime(created);
			details += "  �  ";
		}
		int commentsSpanStart = details.length();
		details += "Comments(" + feedItem.getCommentCount() + ")";
		int commentsSpanEnd = details.length();
		SpannableString detailsString = new SpannableString(details);
		FacebookURLSpan commentsSpan = new FacebookURLSpan(PROTOCOL_SHOW_COMMENTS + itemId);
		commentsSpan.setObserver(spanClickObserver);
		detailsString.setSpan(commentsSpan, commentsSpanStart, commentsSpanEnd, 0);		
		detailsView.setText(detailsString);
		detailsView.setMovementMethod(LinkMovementMethod.getInstance());

		// Set default picture as sender's picture.
		ImageView fromPictureImage = (ImageView) feedItemView
				.findViewById(R.id.feed_item_from_image);
		fromPictureImage.setImageBitmap(defaultPicture);

		return feedItemView;
	}

	/**
	 * Private class for handling "me/home" Facebook request.
	 * 
	 * @author harism
	 */
	private final class DAONewsFeedListObserver implements
			DAOObserver<DAONewsFeedList> {

		private Activity activity = null;

		public DAONewsFeedListObserver(Activity activity) {
			this.activity = activity;
		}

		@Override
		public void onComplete(DAONewsFeedList newsFeedList) {
			// First hide progress dialog.
			hideProgressDialog();

			// Add feed item to viewable list of items.
			LinearLayout itemList = (LinearLayout) findViewById(R.id.feed_list);

			for (DAONewsFeedItem item : newsFeedList) {
				View view = createFeedItem(item);
				itemList.addView(view);

				requestController.getProfile(activity, item.getFromId(),
						new DAOProfileObserver(activity, item.getId()));

				if (item.getPictureUrl() != null) {
					requestController.getBitmap(activity, item.getPictureUrl(),
							new FeedItemPictureObserver(item.getId()));
				}
			}
		}

		@Override
		public void onError(Exception ex) {
			// Hide progress dialog.
			hideProgressDialog();
			// We don't want to see this happening but just in case.
			showAlertDialog(ex.getLocalizedMessage());
		}

	}

	/**
	 * Private class for handling sender's picture request.
	 * 
	 * @author harism
	 */
	private final class DAOProfileObserver implements DAOObserver<DAOProfile> {

		private Activity activity = null;
		private String itemId = null;

		public DAOProfileObserver(Activity activity, String itemId) {
			this.activity = activity;
			this.itemId = itemId;
		}

		@Override
		public void onComplete(DAOProfile profile) {
			requestController.getBitmap(activity, profile.getPictureUrl(),
					new FromPictureObserver(itemId));
		}

		@Override
		public void onError(Exception ex) {
			// We don't care about errors.
		}

	}

	/**
	 * Private class for handling feed item picture requests.
	 * 
	 * @author harism
	 */
	private final class FeedItemPictureObserver implements DAOObserver<Bitmap> {

		private String itemId = null;

		public FeedItemPictureObserver(String itemId) {
			this.itemId = itemId;
		}

		@Override
		public void onComplete(Bitmap bitmap) {
			// Get feed item list view.
			View itemList = findViewById(R.id.feed_list);
			// Find feed item using itemId.
			View itemView = itemList.findViewWithTag(itemId);
			// This shouldn't happen but just in case.
			if (itemView != null) {
				// Set image to feed item.
				ImageView iv = (ImageView) itemView
						.findViewById(R.id.feed_item_picture_image);
				iv.setImageBitmap(bitmap);
				iv.setVisibility(View.VISIBLE);
			}
		}

		@Override
		public void onError(Exception ex) {
			// We don't care about errors.
		}
	}

	/**
	 * Private class for handling actual profile picture requests.
	 * 
	 * @author harism
	 */
	private final class FromPictureObserver implements DAOObserver<Bitmap> {

		private String itemId = null;

		public FromPictureObserver(String itemId) {
			this.itemId = itemId;
		}

		@Override
		public void onComplete(Bitmap bitmap) {
			// Get feed item list view.
			View itemList = findViewById(R.id.feed_list);
			// Find our item view using itemId.
			View itemView = itemList.findViewWithTag(itemId);
			// This shouldn't happen but just in case.
			if (itemView != null) {
				// Set image to feed item view.
				ImageView iv = (ImageView) itemView
						.findViewById(R.id.feed_item_from_image);
				iv.setImageBitmap(BitmapUtils.roundBitmap(bitmap,
						PICTURE_ROUND_RADIUS));
			}
		}

		@Override
		public void onError(Exception ex) {
			// We don't care about errors.
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
			if (span.getURL().startsWith(PROTOCOL_SHOW_PROFILE)) {
				activity.showAlertDialog(span.getURL());
				return true;
			} else if (span.getURL().startsWith(PROTOCOL_SHOW_COMMENTS)) {
				activity.showAlertDialog(span.getURL());
				return true;
			}
			return false;
		}
	}
}
