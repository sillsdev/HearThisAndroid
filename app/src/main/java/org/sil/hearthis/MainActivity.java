package org.sil.hearthis;

import org.json.JSONObject;
import org.sil.palaso.Graphite;

import Script.BookInfo;
import Script.IScriptProvider;
import Script.Project;
import Script.RealScriptProvider;
import Script.SampleScriptProvider;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class MainActivity extends ActionBarActivity
{
	Menu mainMenu;

	@Override
	protected void onCreate(Bundle state)
	{
		super.onCreate(state);

        Graphite.loadGraphite();
		setContentView(R.layout.activity_main);

		// Todo: scan org.sil.hearthis/files for folders containing info.txt and open first

		IScriptProvider scripture;
		try
		{
			scripture = new RealScriptProvider(getExternalFilesDir(null) + "/" + "Dhh");
		}
		catch (Exception ex)
		{
			scripture = new SampleScriptProvider();
		}
		Project project = new Project("Sample", scripture);
		try
		{
			setProject(project);
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
		case R.id.open:
			Toast.makeText(this, "Open", Toast.LENGTH_LONG).show();
			return true;
		case R.id.publish:
			Toast.makeText(this, "Publish", Toast.LENGTH_LONG).show();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		mainMenu = menu;
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu_main, menu);
		return super.onCreateOptionsMenu(menu);
	}

	public void setProject(Project project)
	{
		ArrayList<BookInfo> info = project.getBooks();
		LayoutInflater inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		GridView oldTGrid = (GridView)findViewById(R.id.oldTGrid);
		BookButtonAdapter oldTAdapter = new BookButtonAdapter(this, info, 0, 38);
		oldTGrid.setAdapter(oldTAdapter);
		oldTAdapter.notifyDataSetChanged();
		oldTGrid.setOnItemClickListener(bookButtonListener);

		GridView newTGrid = (GridView)findViewById(R.id.newTGrid);
		BookButtonAdapter newTAdapter = new BookButtonAdapter(this, info, 39, 65);
		newTGrid.setAdapter(newTAdapter);
		newTAdapter.notifyDataSetChanged();
		newTGrid.setOnItemClickListener(bookButtonListener);
	}

	public AdapterView.OnItemClickListener bookButtonListener = new AdapterView.OnItemClickListener()
	{
		@Override
		public void onItemClick(AdapterView<?> parent, View v, int position, long id)
		{
			BookInfo book = (BookInfo)v.getTag();
			Intent chooseChapter = new Intent(MainActivity.this, ChaptersActivity.class);
			chooseChapter.putExtra("book", book);
			startActivity(chooseChapter);
		}
	};

	public class BookButtonAdapter extends BaseAdapter
	{
		private Context context;
		// references to button features
		private BookInfo[] buttonInfo;

		public BookButtonAdapter(Context c, ArrayList<BookInfo> infoList, int first, int last)
		{
			context = c;
			buttonInfo = new BookInfo[last - first + 1];
			for (int j = 0; j <= last - first; j++)
				buttonInfo[j] = infoList.get(first + j);
		}

		public int getCount()
		{
			return buttonInfo.length;
		}

		public Object getItem(int position)
		{
			return buttonInfo[position];
		}

		public long getItemId(int position)
		{
			return 0;
		}

		public View getView(int position, View convertView, ViewGroup parent)
		{
			TextView button;
			BookInfo myInfo = buttonInfo[position];
			if (convertView == null)
			{
				button = new TextView(context);
				button.setPadding(8, 8, 8, 8);
				button.setBackgroundColor(setButtonColour(myInfo.getBookNumber()));
				button.setTextColor(getResources().getColor(R.color.navButtonTextColor));//0xffffffff);
				button.setHeight(80);
				button.setTextSize(20);
				button.setGravity(Gravity.CENTER);
			}
			else
			{
				button = (TextView)convertView;
			}
			button.setTag(myInfo);
			button.setText(myInfo.getAbbr() != null ?
					myInfo.getAbbr() :
					myInfo.getName().substring(0, 3));
			return button;
		}

		private int setButtonColour(int bookNumber)
		{
			int navbuttoncolour = R.color.navButtonColor;
			if (bookNumber < 5)
			{
				navbuttoncolour = R.color.navButtonLawColor;
			}
			else if (bookNumber < 17)
			{
				navbuttoncolour = R.color.navButtonHistoryColor;
			}
			else if (bookNumber < 22)
			{
				navbuttoncolour = R.color.navButtonPoetryColor;
			}
			else if (bookNumber < 27)
			{
				navbuttoncolour = R.color.navButtonMajorProphetColor;
			}
			else if (bookNumber < 39)
			{
				navbuttoncolour = R.color.navButtonMinorProphetColor;
			}
			else if (bookNumber < 43)
			{
				navbuttoncolour = R.color.navButtonGospelsColor;
			}
			else if (bookNumber < 44)
			{
				navbuttoncolour = R.color.navButtonActsColor;
			}
			else if (bookNumber < 57)
			{
				navbuttoncolour = R.color.navButtonPaulineColor;
			}
			else if (bookNumber < 65)
			{
				navbuttoncolour = R.color.navButtonEpistlesColor;
			}
			else
			{
				navbuttoncolour = R.color.navButtonRevelationColor;
			}
			return getResources().getColor(navbuttoncolour);
		}
	}
}
