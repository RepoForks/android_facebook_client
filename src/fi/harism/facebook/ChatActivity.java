package fi.harism.facebook;

import java.util.Vector;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.animation.AlphaAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import fi.harism.facebook.dao.FBBitmap;
import fi.harism.facebook.dao.FBChat;
import fi.harism.facebook.dao.FBObserver;
import fi.harism.facebook.dao.FBUser;
import fi.harism.facebook.util.BitmapUtils;

public class ChatActivity extends BaseActivity implements FBChat.Observer {

	private FBChat fbChat;
	//private FBBitmapCache fbBitmapCache;
	private Bitmap defaultPicture;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_chat);

		Button connectButton = (Button) findViewById(R.id.chat_button_connect);
		connectButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				connect();
			}
		});

		Button closeButton = (Button) findViewById(R.id.chat_button_disconnect);
		closeButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				close();
			}
		});

		Button logButton = (Button) findViewById(R.id.chat_button_showlog);
		logButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				showAlertDialog(fbChat.getLog());
			}
		});

		//fbBitmapCache = getGlobalState().getFBFactory().getBitmapCache();
		Bitmap bitmap = getGlobalState().getDefaultPicture();
		defaultPicture = BitmapUtils.roundBitmap(bitmap, 7);

		fbChat = getGlobalState().getFBFactory().getChat(this);
		Vector<FBUser> users = fbChat.getUsers();
		for (FBUser user : users) {
			onPresenceChanged(user);
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		fbChat.onDestroy();
	}

	private void connect() {
		//fbChat.connect();
	}

	private void close() {
		fbChat.disconnect();
	}

	@Override
	public void onConnected() {
		runOnUiThread(new Runnable() {
			public void run() {
				showAlertDialog("Connected.");
			}
		});
	}

	@Override
	public void onDisconnected() {
		runOnUiThread(new Runnable() {
			public void run() {
				showAlertDialog("Disconnected.");
				// TODO: It is possible there is presence request waiting in
				// request queue at this point.
				LinearLayout list = (LinearLayout) findViewById(R.id.chat_user_list);
				list.removeAllViews();
			}
		});
	}

	@Override
	public void onPresenceChanged(final FBUser user) {
		runOnUiThread(new Runnable() {
			public void run() {
				handlePresenceChange(user);
			}
		});
	}

	@Override
	public void onMessage(FBUser user, String message) {
	}

	private void handlePresenceChange(FBUser user) {
		LinearLayout list = (LinearLayout) findViewById(R.id.chat_user_list);
		View v = list.findViewWithTag(user.getId());

		if (v != null && user.getPresence() == FBUser.Presence.GONE) {
			list.removeView(v);
		} else if (v != null) {
			// Update user presence somehow.
		} else {
			v = getLayoutInflater().inflate(R.layout.view_friend, null);
			TextView tv = (TextView) v.findViewById(R.id.view_friend_name);
			tv.setText(user.getName());

			// Search picture Container and set default profile picture into it.
			View imageContainer = v.findViewById(R.id.view_friend_picture);
			ImageView bottomView = (ImageView) imageContainer
					.findViewById(R.id.view_layered_image_bottom);
			bottomView.setImageBitmap(defaultPicture);

			v.setTag(user.getId());
			v.setTag(R.id.view_storage, user);
			v.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View item) {
					Intent i = createIntent(ChatSessionActivity.class);
					i.putExtra("fi.harism.facebook.ChatSessionActivity",
							(FBUser) item.getTag(R.id.view_storage));
					startActivity(i);
				}
			});

			list.addView(v);

			if (user.getPicture() != null) {
				//fbBitmapCache.load(user.getPicture(), user.getId(),
				//		new FBBitmapObserver());
			}
		}
	}

	private class FBBitmapObserver implements FBObserver<FBBitmap> {

		@Override
		public void onComplete(final FBBitmap bitmap) {
			runOnUiThread(new Runnable() {
				public void run() {
					LinearLayout list = (LinearLayout) findViewById(R.id.chat_user_list);
					View v = null; //list.findViewWithTag(bitmap.getId());
					if (v != null) {
						// Search picture Container and set profile picture into
						// it.
						View imageContainer = v
								.findViewById(R.id.view_friend_picture);
						ImageView topImage = (ImageView) imageContainer
								.findViewById(R.id.view_layered_image_top);
						ImageView bottomImage = (ImageView) imageContainer
								.findViewById(R.id.view_layered_image_bottom);
						
						Rect r = new Rect();
						if (imageContainer.getLocalVisibleRect(r)) {
							AlphaAnimation inAnimation = new AlphaAnimation(0, 1);
							AlphaAnimation outAnimation = new AlphaAnimation(1, 0);
							inAnimation.setDuration(700);
							outAnimation.setDuration(700);
							outAnimation.setFillAfter(true);
							
							topImage.setAnimation(inAnimation);
							bottomImage.startAnimation(outAnimation);
						} else {
							bottomImage.setAlpha(0);
						}						
						
						topImage.setImageBitmap(BitmapUtils.roundBitmap(
								bitmap.getBitmap(), 7));
					}
				}
			});
		}

		@Override
		public void onError(Exception error) {
		}

	}

}
