package course.labs.notificationslab;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;

public class MainActivity extends Activity implements SelectionListener,
		DownloadFinishedListener {

	private static final String TAG_NAME = "name";
	private static final String TAG_USER = "user";
	private static final String TAG_TEXT = "text";
	private static final String TAG_FRIENDS_FRAGMENT = "friends_fragment";
	private static final String TAG_FEED_FRAGMENT = "feed_fragment";
	private static final String TAG_DOWNLOADER_FRAGMENT = "downloader_fragment";
	private static final String TAG_IS_DATA_AVAILABLE = "is_data_available";
	private static final String TAG_PROCESSED_FEEDS = "processed_feeds";
	static final String TAG_TWEET_DATA = "data";
	static final String TAG_FRIEND_RES_IDS = "friends";

	public static final String TWEET_FILENAME = "tweets.txt";
	public final static String[] FRIENDS_NAMES = { "taylorswift13",
			"msrebeccablack", "ladygaga" };
	public static final int IS_ALIVE = Activity.RESULT_FIRST_USER;
	public static final String DATA_REFRESHED_ACTION = "course.labs.notificationslabnew.DATA_REFRESHED";
	private static final String TAG = "Lab-Notifications";

	// ID идентификаторы файла, содержащего сохраненные данные ленты твитов
	public static final ArrayList<Integer> sRawTextFeedIds = new ArrayList<Integer>(
			Arrays.asList(R.raw.tswift, R.raw.rblack, R.raw.lgaga));

	private FragmentManager mFragmentManager;
	private FriendsFragment mFriendsFragment;
	private FeedFragment mFeedFragment;
	private DownloaderTaskFragment mDownloaderFragment;
	private boolean mIsInteractionEnabled;
	private String[] mFormattedFeeds = new String[sRawTextFeedIds.size()];;
	private boolean mIsFresh;
	private BroadcastReceiver mRefreshReceiver;
	private static final long TWO_MIN = 2 * 60 * 1000;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		mFragmentManager = getFragmentManager();

		// Восстанавливаем состояние при реконфигурации
		if (null != savedInstanceState) {
			restoreState(savedInstanceState);
		} else {
			setupFragments();
		}
	}

	// Один раз устанавливаем UI для Фрагмента
	private void setupFragments() {
		installFriendsFragment();

		// Лента свежая, если она была скачана менее 2 минут назад
		mIsFresh = (System.currentTimeMillis() - getFileStreamPath(
				TWEET_FILENAME).lastModified()) < TWO_MIN;
		if (!mIsFresh) {
			installDownloaderTaskFragment();

			Toast.makeText(getApplicationContext(), getString(R.string.download_in_progress_string),
					Toast.LENGTH_LONG).show();

			
			
			// Устанавливаем BroadcastReceiver для получения интента, когда закончится скачивание
			mRefreshReceiver = new BroadcastReceiver() {
				@Override
				public void onReceive(Context context, Intent intent) {
					if( isOrderedBroadcast() )
					{
						setResultCode(MainActivity.IS_ALIVE);
				}
					
				}
			};

		} else {
			// Обработать данные Twitter, полученные из сохраненного файла
			parseJSON(loadTweetsFromFile());

			// Включаем пользовательское взаимодействие
			mIsInteractionEnabled = true;
		}
	}

	// Добавляем FriendsFragment в Activity
	private void installFriendsFragment() {

		// Создаем новый Fragment
		mFriendsFragment = new FriendsFragment();

		// Передаем Fragment в FragmentManager
		FragmentTransaction transaction = mFragmentManager.beginTransaction();
		transaction.replace(R.id.fragment_container, mFriendsFragment,
				TAG_FRIENDS_FRAGMENT);
		transaction.commit();
	}

	// Добавляем DownloaderTaskFragment в Activity
	private void installDownloaderTaskFragment() {

		// Создаем новый Fragment
		mDownloaderFragment = new DownloaderTaskFragment();

		// Устанавливаем аргументы DownloaderTaskFragment
		Bundle args = new Bundle();
		args.putIntegerArrayList(TAG_FRIEND_RES_IDS, sRawTextFeedIds);
		mDownloaderFragment.setArguments(args);

		// Передаем Фрагмент в FragmentManager
		mFragmentManager.beginTransaction()
				.add(mDownloaderFragment, TAG_DOWNLOADER_FRAGMENT).commit();
	}

	// Регистрируем BroadcastReceiver
	@Override
	protected void onResume() {
		super.onResume();


		this.registerReceiver(mRefreshReceiver , new IntentFilter(
				MainActivity.DATA_REFRESHED_ACTION));
		
		
		
	}

	@Override
	protected void onPause() {


		if(mRefreshReceiver != null)
		{
			this.unregisterReceiver(mRefreshReceiver);
		}

		super.onPause();

	}

	/*
	 * DownloadFinishedListener метод
	 */

	// Вызывается фрагментом DownloaderTask после окончания загрузки данных
	public void notifyDataRefreshed(String[] feeds) {

		// Обрабатываем скаченные данные
		parseJSON(feeds);

		// Включаем пользовательское взаимодействие
		mIsInteractionEnabled = true;
		allowUserClicks();

	};

	// Включаем пользовательское взаимодействие с FriendFragment
	private void allowUserClicks() {
		mFriendsFragment.setAllowUserClicks(true);
	}

	/*
	 * Методы интерфейса SelectionListener
	 */

	// Уведомляет, включено ли пользовательское взаимодействие
	public boolean canAllowUserClicks() {
		return mIsInteractionEnabled;
	}

	// Инсталлирует FeedFragment, когда имя Друга выбирается в FriendsFragment
	@Override
	public void onItemSelected(int position) {
		installFeedFragment(mFormattedFeeds[position]);
	}

	// Добавляет FeedFragment в Activity
	private void installFeedFragment(String tweetData) {
		// Создаем новый Фрагмент
		mFeedFragment = new FeedFragment();

		// Устанавливаем аргументы Фрагмента
		Bundle args = new Bundle();
		args.putString(TAG_TWEET_DATA, tweetData);
		mFeedFragment.setArguments(args);

		// Передаем Фрагмент в FragmentManager
		FragmentTransaction transaction = mFragmentManager.beginTransaction();
		transaction.replace(R.id.fragment_container, mFeedFragment,
				TAG_FEED_FRAGMENT);
		transaction.addToBackStack(null);
		transaction.commit();
	}

	@Override
	protected void onSaveInstanceState(Bundle savedInstanceState) {
		if (null != mFriendsFragment) {
			savedInstanceState.putString(TAG_FRIENDS_FRAGMENT,
					mFriendsFragment.getTag());
		}
		if (null != mFeedFragment) {
			savedInstanceState.putString(TAG_FEED_FRAGMENT,
					mFeedFragment.getTag());
		}
		if (null != mDownloaderFragment) {
			savedInstanceState.putString(TAG_DOWNLOADER_FRAGMENT,
					mDownloaderFragment.getTag());
		}
		savedInstanceState.putBoolean(TAG_IS_DATA_AVAILABLE,
				mIsInteractionEnabled);
		savedInstanceState.putStringArray(TAG_PROCESSED_FEEDS, mFormattedFeeds);

		super.onSaveInstanceState(savedInstanceState);

	}

	// Восстанавливаем состояние сохраненного объекта
	private void restoreState(Bundle savedInstanceState) {

		// ТЭги фрагментов, которые были сохранены в  onSavedInstanceState
		mFriendsFragment = (FriendsFragment) mFragmentManager
				.findFragmentByTag(savedInstanceState
						.getString(TAG_FRIENDS_FRAGMENT));

		mFeedFragment = (FeedFragment) mFragmentManager
				.findFragmentByTag(savedInstanceState
						.getString(TAG_FEED_FRAGMENT));

		mDownloaderFragment = (DownloaderTaskFragment) mFragmentManager
				.findFragmentByTag(savedInstanceState
						.getString(TAG_DOWNLOADER_FRAGMENT));

		mIsInteractionEnabled = savedInstanceState
				.getBoolean(TAG_IS_DATA_AVAILABLE);
		if (mIsInteractionEnabled) {
			mFormattedFeeds = savedInstanceState
					.getStringArray(TAG_PROCESSED_FEEDS);
		}
	}

	// Преобразовываем сырые данные (в формате JSON) в текст для отображения
	private void parseJSON(String[] feeds) {
		JSONArray[] JSONFeeds = new JSONArray[feeds.length];
		for (int i = 0; i < JSONFeeds.length; i++) {
			try {
				JSONFeeds[i] = new JSONArray(feeds[i]);
			} catch (JSONException e) {
				e.printStackTrace();
			}

			String name = "";
			String tweet = "";
			JSONArray tmp = JSONFeeds[i];

			// строковый буфер для ленты
			StringBuffer tweetRec = new StringBuffer("");
			for (int j = 0; j < tmp.length(); j++) {
				try {
					tweet = tmp.getJSONObject(j).getString(TAG_TEXT);
					JSONObject user = (JSONObject) tmp.getJSONObject(j).get(
							TAG_USER);
					name = user.getString(TAG_NAME);
				} catch (JSONException e) {
					e.printStackTrace();
				}
				tweetRec.append(name + " - " + tweet + "\n\n");
			}
			mFormattedFeeds[i] = tweetRec.toString();
		}
	}

	// Полчаем текст ленты для файла
	// Сохраняем в mRawTextFeed[]

	private String[] loadTweetsFromFile() {
		BufferedReader reader = null;
		ArrayList<String> rawFeeds = new ArrayList<String>();
		try {
			FileInputStream fis = openFileInput(TWEET_FILENAME);
			reader = new BufferedReader(new InputStreamReader(fis));
			String s = null;
			int i = 0;
			while (null != (s = reader.readLine())) {
				rawFeeds.add(i, s);
				i++;
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (null != reader) {
				try {
					reader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return rawFeeds.toArray(new String[rawFeeds.size()]);
	}
}
