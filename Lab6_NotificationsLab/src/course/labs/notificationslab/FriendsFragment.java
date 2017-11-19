package course.labs.notificationslab;

import android.app.Activity;
import android.app.ListFragment;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class FriendsFragment extends ListFragment {

	// Родительский Activity
	private SelectionListener mCallback;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setListAdapter(new ArrayAdapter<String>(getActivity(),
				android.R.layout.simple_list_item_1, MainActivity.FRIENDS_NAMES));
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		// Убеждаемся, что родительский Activity реализует
		// callback интерфейс.
		try {
			mCallback = (SelectionListener) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString()
					+ " должен реализовывать SelectionListener");
		}
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		// Включаем ползовательское взаимодействие только если есть данные
		setAllowUserClicks(mCallback.canAllowUserClicks());
		
	}

	// Включаем/выключаем пользовательское взаимодействие
	void setAllowUserClicks(boolean allowUserInteraction) {
		getListView().setEnabled(allowUserInteraction);
		if (allowUserInteraction) {
			getListView().setBackgroundColor(Color.WHITE);
		} else {
			getListView().setBackgroundColor(Color.DKGRAY);
		}
	}

	@Override
	public void onListItemClick(ListView l, View view, int position, long id) {
		// Уведомляем родительское Activity о выборе пользователя
		if (null != mCallback) {
			mCallback.onItemSelected(position);
		}
	}
}
