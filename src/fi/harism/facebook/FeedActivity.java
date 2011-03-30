package fi.harism.facebook;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.view.Window;
import android.view.animation.AlphaAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import fi.harism.facebook.dao.FBBitmap;
import fi.harism.facebook.dao.FBCommentList;
import fi.harism.facebook.dao.FBFeedItem;
import fi.harism.facebook.dao.FBFeedList;
import fi.harism.facebook.dao.FBObserver;
import fi.harism.facebook.dialog.CommentsDialog;
import fi.harism.facebook.util.BitmapUtils;
import fi.harism.facebook.util.FacebookURLSpan;
import fi.harism.facebook.util.StringUtils;

/**
 * Feed Activity for showing latest News Feed events for logged in user.
 * 
 * @author harism
 */
public abstract class FeedActivity extends BaseActivity {

	//private FBBitmapCache fbBitmapCache;
	private FBFeedList fbFeedList;

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
	// Static protocol name for showing likes.
	private static final String PROTOCOL_SHOW_LIKES = "showlikes://";

	public abstract FBFeedList getFeedList();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_feed);

		// Create default picture from resources.
		defaultPicture = getGlobalState().getDefaultPicture();
		defaultPicture = BitmapUtils.roundBitmap(defaultPicture,
				PICTURE_ROUND_RADIUS);

		spanClickObserver = new SpanClickObserver(this);
		//fbBitmapCache = getGlobalState().getFBFactory().getBitmapCache();
		fbFeedList = getFeedList();

		View updateButton = findViewById(R.id.feed_button_update);
		updateButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				showAlertDialog("TODO: Implement me.");
			}
		});

		showProgressDialog();
		fbFeedList.load(new FBFeedListObserver());
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		fbFeedList.cancel();
		//fbBitmapCache.cancel();
	}

	@Override
	public void onPause() {
		super.onPause();
		fbFeedList.setPaused(true);
		//fbBitmapCache.setPaused(true);
	}

	@Override
	public void onResume() {
		super.onResume();
		fbFeedList.setPaused(false);
		//fbBitmapCache.setPaused(false);
	}

	/**
	 * Creates new feed item.
	 * 
	 * @param feedItemObject
	 *            Feed item JSONObject to be added.
	 */
	private View createFeedItem(FBFeedItem feedItem) {
		String itemId = feedItem.getId();

		// Create default Feed Item view.
		View feedItemView = getLayoutInflater().inflate(R.layout.view_feed_post,
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
		details += "Comments";
		int commentsSpanEnd = details.length();
		details += "  �  ";
		int likesSpanStart = details.length();
		details += "Likes";
		int likesSpanEnd = details.length();
		SpannableString detailsString = new SpannableString(details);
		FacebookURLSpan commentsSpan = new FacebookURLSpan(
				PROTOCOL_SHOW_COMMENTS + itemId);
		commentsSpan.setObserver(spanClickObserver);
		detailsString.setSpan(commentsSpan, commentsSpanStart, commentsSpanEnd,
				0);
		FacebookURLSpan likesSpan = new FacebookURLSpan(PROTOCOL_SHOW_LIKES
				+ itemId);
		likesSpan.setObserver(spanClickObserver);
		detailsString.setSpan(likesSpan, likesSpanStart, likesSpanEnd, 0);
		detailsView.setText(detailsString);
		detailsView.setMovementMethod(LinkMovementMethod.getInstance());

		// Set default picture as sender's picture.
		ImageView fromPictureImage = (ImageView) feedItemView
				.findViewById(R.id.feed_item_from_image);
		fromPictureImage.setImageBitmap(defaultPicture);

		return feedItemView;
	}

	private final class FBFeedListObserver implements FBObserver<FBFeedList>,
			Runnable {

		private FBFeedList feedList;

		@Override
		public void onComplete(final FBFeedList feedList) {
			this.feedList = feedList;
			runOnUiThread(this);
		}

		@Override
		public void onError(Exception ex) {
			// Hide progress dialog.
			hideProgressDialog();
			// We don't want to see this happening but just in case.
			showAlertDialog(ex.getLocalizedMessage());
		}

		@Override
		public void run() {
			// Add feed item to viewable list of items.
			LinearLayout itemList = (LinearLayout) findViewById(R.id.feed_list);

			for (FBFeedItem item : feedList) {
				View view = createFeedItem(item);
				itemList.addView(view);

				if (item.getFromPictureUrl() != null) {
					//fbBitmapCache.load(item.getFromPictureUrl(), item.getId(),
					//		new FromPictureObserver());
				}

				if (item.getPictureUrl() != null) {
					//fbBitmapCache.load(item.getPictureUrl(), item.getId(),
					//		new FeedPictureObserver());
				}
			}
			
			hideProgressDialog();
		}

	}

	/**
	 * Private class for handling feed item picture requests.
	 * 
	 * @author harism
	 */
	private final class FeedPictureObserver implements FBObserver<FBBitmap>,
			Runnable {

		private FBBitmap bitmap;

		@Override
		public void onComplete(final FBBitmap bitmap) {
			this.bitmap = bitmap;
			runOnUiThread(this);
		}

		@Override
		public void onError(Exception ex) {
			// We don't care about errors.
		}

		@Override
		public void run() {
			// Get feed item list view.
			View itemList = findViewById(R.id.feed_list);
			// Find feed item using itemId.
			View itemView = null; //itemList.findViewWithTag(bitmap.getId());
			// This shouldn't happen but just in case.
			if (itemView != null) {
				// Set image to feed item.
				ImageView iv = (ImageView) itemView
						.findViewById(R.id.feed_item_picture_image);
				
				Rect r = new Rect();
				if (itemView.getLocalVisibleRect(r)) {
					AlphaAnimation inAnimation = new AlphaAnimation(0, 1);
					inAnimation.setDuration(700);
					iv.setAnimation(inAnimation);
				}
				
				iv.setImageBitmap(bitmap.getBitmap());
				iv.setVisibility(View.VISIBLE);
			}
		}
	}

	/**
	 * Private class for handling actual profile picture requests.
	 * 
	 * @author harism
	 */
	private final class FromPictureObserver implements FBObserver<FBBitmap>,
			Runnable {

		private FBBitmap bitmap;

		@Override
		public void onComplete(final FBBitmap bitmap) {
			this.bitmap = bitmap;
			runOnUiThread(this);
		}

		@Override
		public void onError(Exception ex) {
			// We don't care about errors.
		}

		@Override
		public void run() {
			// Get feed item list view.
			View itemList = findViewById(R.id.feed_list);
			// Find our item view using itemId.
			View itemView = null; // itemList.findViewWithTag(bitmap.getId());
			// This shouldn't happen but just in case.
			if (itemView != null) {
				// Set image to feed item view.
				ImageView iv = (ImageView) itemView
						.findViewById(R.id.feed_item_from_image);
				iv.setImageBitmap(BitmapUtils.roundBitmap(bitmap.getBitmap(),
						PICTURE_ROUND_RADIUS));
			}
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
			} else if (url.startsWith(PROTOCOL_SHOW_COMMENTS)) {
				showProgressDialog();
				String itemId = url.substring(PROTOCOL_SHOW_COMMENTS.length());
				FBCommentList commentList = getGlobalState().getFBFactory()
						.getCommentList(itemId);
				commentList.load(new FBObserver<FBCommentList>() {
					@Override
					public void onComplete(final FBCommentList comments) {
						hideProgressDialog();
						runOnUiThread(new Runnable() {
							public void run() {
								new CommentsDialog(activity, comments).show();
							}
						});
					}

					@Override
					public void onError(Exception error) {
						hideProgressDialog();
					}
				});
				return true;
			} else if (url.startsWith(PROTOCOL_SHOW_LIKES)) {
				showAlertDialog(url);
				return true;
			}
			return false;
		}
	}
}
