package fi.harism.facebook.dao;

import java.util.Iterator;
import java.util.Vector;

import org.json.JSONArray;
import org.json.JSONObject;

import android.app.Activity;
import android.os.Bundle;
import fi.harism.facebook.net.FacebookClient;
import fi.harism.facebook.request.FacebookRequest;
import fi.harism.facebook.request.RequestQueue;

/**
 * Class for retrieving News Feed list and storing it into memory.
 * 
 * @author harism
 */
public class DAOFeedList implements Iterable<DAOFeedItem> {

	// RequestQueue instance.
	private RequestQueue requestQueue = null;
	// FacebookClient instance.
	private FacebookClient facebookClient = null;
	// List of News Feed items.
	private Vector<DAOFeedItem> feedItemList = null;
	// Boolean for checking if news feed list is loaded already.
	private boolean feedItemListLoaded = false;
	// Feed path.
	private String feedPath = null;
	// Path for News Feed.
	public static final String NEWS_FEED = "/me/home";
	// Path for Wall.
	public static final String PROFILE_FEED = "/me/feed";

	/**
	 * Constructor for News Feed list.
	 * 
	 * @param requestQueue
	 *            RequestQueue instance.
	 * @param facebookClient
	 *            FacebookClient instance.
	 */
	public DAOFeedList(RequestQueue requestQueue,
			FacebookClient facebookClient, String feedPath) {
		this.requestQueue = requestQueue;
		this.facebookClient = facebookClient;
		this.feedPath = feedPath;
		feedItemList = new Vector<DAOFeedItem>();
	}

	/**
	 * Accessor for getting News Feed item at given index.
	 * 
	 * @param index
	 *            Index of DAONewsFeedItem.
	 * @return DAONewsFeedItem at given index.
	 */
	public DAOFeedItem at(int index) {
		return feedItemList.elementAt(index);
	}

	/**
	 * Triggers asynchronous loading of News Feed if it hasn't been loaded yet.
	 * Otherwise notifies observer asap.
	 * 
	 * @param activity
	 *            Activity which triggered this request.
	 * @param observer
	 *            Observer for this request.
	 */
	public void getInstance(Activity activity,
			final DAOObserver<DAOFeedList> observer) {
		// We need "this" pointer later on.
		final DAOFeedList self = this;
		if (feedItemListLoaded) {
			// Call observer on UI thread.
			activity.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					observer.onComplete(self);
				}
			});
		} else {
			// Create Facebook request.
			Bundle b = new Bundle();
			b.putString(
					"fields",
					"id,type,from,message,picture,link,name,caption,description,created_time,comments");
			FacebookRequest r = new FacebookRequest(activity, feedPath, b,
					facebookClient, new FacebookRequest.Observer() {
						@Override
						public void onComplete(FacebookRequest facebookRequest) {
							try {
								JSONArray feedItems = facebookRequest
										.getResponse().getJSONArray("data");
								feedItemList = new Vector<DAOFeedItem>();
								for (int i = 0; i < feedItems.length(); ++i) {
									JSONObject item = feedItems
											.getJSONObject(i);
									String id = item.getString("id");
									String type = item.getString("type");
									String fromId = item.getJSONObject("from")
											.getString("id");
									String fromName = item
											.getJSONObject("from").getString(
													"name");
									String message = item.optString("message",
											null);
									String picture = item.optString("picture",
											null);
									String link = item.optString("link", null);
									String name = item.optString("name", null);
									String caption = item.optString("caption",
											null);
									String description = item.optString(
											"description", null);
									String createdTime = item.optString(
											"created_time", null);
									int commentCount = 0;
									JSONObject commentsObject = item
											.optJSONObject("comments");
									if (commentsObject != null) {
										commentCount = commentsObject
												.getInt("count");
									}
									feedItemList.add(new DAOFeedItem(id,
											type, fromId, fromName, message,
											picture, link, name, caption,
											description, createdTime,
											commentCount));
								}
								feedItemListLoaded = true;
								observer.onComplete(self);
							} catch (Exception ex) {
								observer.onError(ex);
							}
						}

						@Override
						public void onError(Exception ex) {
							observer.onError(ex);
						}
					});
			requestQueue.addRequest(r);
		}
	}

	@Override
	public Iterator<DAOFeedItem> iterator() {
		// Return Iterator for a copy of News Feed list instead.
		Vector<DAOFeedItem> copy = new Vector<DAOFeedItem>(feedItemList);
		return copy.iterator();
	}

	/**
	 * Accessor for retrieving size of this list.
	 * 
	 * @return Number of News Feed items on this list.
	 */
	public int size() {
		return feedItemList.size();
	}

}
