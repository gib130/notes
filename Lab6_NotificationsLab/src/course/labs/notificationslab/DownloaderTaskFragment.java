package course.labs.notificationslab;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.Fragment;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;

public class DownloaderTaskFragment extends Fragment {

	private DownloadFinishedListener mCallback;
	private Context mContext;
	private final int MY_NOTIFICATION_ID = 11151990;

	@SuppressWarnings("unused")
	private static final String TAG = "Lab-Notifications";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Предохраняет во время реконфигурации
		setRetainInstance(true);



		DownloaderTask task = new DownloaderTask();

		Bundle bundle = getArguments();
		List<Integer> test = bundle.getIntegerArrayList(MainActivity.TAG_FRIEND_RES_IDS);
		Integer[] objectArray = new Integer[test.size()];

		for(int ctr = 0; ctr < test.size(); ctr++) {
			objectArray[ctr] = Integer.valueOf(test.get(ctr)); // returns Integer value
		}

		task.execute(objectArray);



		
	}

	// Ассоциируем текущий родительский Activity с mCallback
	// Сохраняем контекст приложения для использования в методе downloadTweets()
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		mContext = activity.getApplicationContext();

		// Убеждаеся, что родительский активити  реализует
		// верный callback интерфейс.
		try {
			mCallback = (DownloadFinishedListener) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString()
					+ " должен реализовывать DownloadFinishedListener");
		}
	}

	// Обнуляем mCallback
	@Override
	public void onDetach() {
		super.onDetach();
		mCallback = null;
	}




	
	
	
	
	
	



	public class DownloaderTask extends AsyncTask<Integer,Void,String[]> {
		private String[] downloadTweets(Integer resourceIDS[]) {

			final int simulatedDelay = 2000;
			String[] feeds = new String[resourceIDS.length];
			boolean downLoadCompleted = false;

			try {
				for (int idx = 0; idx < resourceIDS.length; idx++) {
					InputStream inputStream;
					BufferedReader in;
					try {
						// Прикидываемся, что скачиванием занимает много времени
						Thread.sleep(simulatedDelay);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}

					inputStream = mContext.getResources().openRawResource(
							resourceIDS[idx]);
					in = new BufferedReader(new InputStreamReader(inputStream));

					String readLine;
					StringBuffer buf = new StringBuffer();

					while ((readLine = in.readLine()) != null) {
						buf.append(readLine);
					}

					feeds[idx] = buf.toString();

					if (null != in) {
						in.close();
					}
				}

				downLoadCompleted = true;
				saveTweetsToFile(feeds);

			} catch (IOException e) {
				e.printStackTrace();
			}

			// Уведомляем пользователя, что скачивание завершено
			notify(downLoadCompleted);

			return feeds;

		}

		@Override
		protected String[] doInBackground(Integer... integers) {
			return downloadTweets(integers);
		}

		@Override
		protected void onPostExecute(String[] strings) {
			mCallback.notifyDataRefreshed(strings);
		}


		// Раскомментируйте этот вспомогательный метод.
		// Если необходимо, уведомляет пользователя, что скачивание твитов завершено.
		// Отправляет широковещательный запрос обратно в BroadcastReceiver в
		// MainActivity чтобы определить, нужно ли создавать уведомление.


		private void notify(final boolean success) {

			final Intent restartMainActivityIntent = new Intent(mContext,
					MainActivity.class);
			restartMainActivityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

			// Отправляет широковещательный запрос, чтобы определить, активен ли MainActivity,
			// виден ли он на экране. Создает новый BroadcastReceiver
			// чтобы получить результат обозначающий состояние MainActivity

			// Действие для данного широковещательного запроса
			// MainActivity.DATA_REFRESHED_ACTION
			// Результат MainActivity.IS_ALIVE обозначает, что MainActivity
			// активен и виден на экране устройства.

			mContext.sendOrderedBroadcast(new Intent(
							MainActivity.DATA_REFRESHED_ACTION), null,
					new BroadcastReceiver() {

						final String failMsg = mContext
								.getString(R.string.download_failed_string);
						final String successMsg = mContext
								.getString(R.string.download_succes_string);
						final String notificationSentMsg = mContext
								.getString(R.string.notification_sent_string);
                        final String downloadInProgress = mContext
                                .getString(R.string.download_in_progress_string);

						@Override
						public void onReceive(Context context, Intent intent) {

                            String id = intent.getStringExtra("test");
							if (getResultCode() == 0) {
                                PendingIntent contentIntent = PendingIntent.getActivity(mContext,0,restartMainActivityIntent,FLAG_UPDATE_CURRENT);


								RemoteViews mContentView = new RemoteViews(
										mContext.getPackageName(),
										R.layout.custom_notification);

								Notification.Builder notificationBuilder = new Notification.Builder(mContext);
                                notificationBuilder.setContentIntent(contentIntent)
                                        .setSmallIcon(android.R.drawable.stat_sys_warning)
                                        .setContentTitle("SUCCESS BRO")
                                        .setAutoCancel(true)
                                        .setContentText(successMsg); // Текст уведомления
                                Notification notification = notificationBuilder.build();
                                NotificationManager notificationManager = (NotificationManager) context
                                        .getSystemService(Context.NOTIFICATION_SERVICE);
                                notificationManager.notify(MY_NOTIFICATION_ID, notification);
								Toast.makeText(mContext, notificationSentMsg,
										Toast.LENGTH_LONG).show();

							} else {
								Toast.makeText(mContext,
										success ? successMsg : failMsg,
										Toast.LENGTH_LONG).show();
							}
						}
					}, null, 0, null, null);
		}


		// Раскомментируйте этот вспомогательный метод
		// Сохраняет твиты в файл


		private void saveTweetsToFile(String[] result) {
			PrintWriter writer = null;
			try {
				FileOutputStream fos = mContext.openFileOutput(
						MainActivity.TWEET_FILENAME, Context.MODE_PRIVATE);
				writer = new PrintWriter(new BufferedWriter(
						new OutputStreamWriter(fos)));

				for (String s : result) {
					writer.println(s);
				}
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				if (null != writer) {
					writer.close();
				}
			}
		}
	}



	
	
	
	
	
	
	
	
	
	
}