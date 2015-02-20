package org.sil.hearthis;

import Script.BookInfo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.TextView;

import org.json.JSONObject;

import java.util.ArrayList;

public class ChaptersActivity extends Activity
{
	private BookInfo book;

	@Override
	protected void onCreate(Bundle state)
	{
		super.onCreate(state);
		setContentView(R.layout.activity_chapters);
		book = (BookInfo)getIntent().getExtras().get("book");

		TextView bookBox = (TextView)findViewById(R.id.bookNameText);
		bookBox.setText(book.getName());
		
		GridView chapterGrid = (GridView)findViewById(R.id.chapterGrid);
		ChapterButtonAdapter chapterAdapter = new ChapterButtonAdapter(this, book.getChapterCount());
		chapterGrid.setAdapter(chapterAdapter);
		chapterAdapter.notifyDataSetChanged();
		chapterGrid.setOnItemClickListener(new AdapterView.OnItemClickListener()
		{
			@Override
			public void onItemClick(AdapterView<?> adapterView, View view, int i, long l)
			{
				Intent record = new Intent(ChaptersActivity.this, RecordActivity.class);
				record.putExtra("book", book);
				record.putExtra("chapter", i);
				startActivity(record);
			}
		});
	}

	public class ChapterButtonAdapter extends BaseAdapter
	{
		private Context context;
		private int[] numbers;
		// references to button features

		public ChapterButtonAdapter(Context c, int chapterCount)
		{
			context = c;
			numbers = new int[chapterCount + 1];
			for (int j = 0; j <= chapterCount; j++)
				numbers[j] = j;
		}

		public int getCount()
		{
			return numbers.length;
		}

		public Object getItem(int position)
		{
			return numbers[position];
		}

		public long getItemId(int position)
		{
			return 0;
		}

		public View getView(int position, View convertView, ViewGroup parent)
		{
			TextView button;
			final int number = numbers[position];
			if (convertView == null)
			{
				button = new TextView(context);
				button.setPadding(8, 8, 8, 8);
				button.setTextColor(getResources().getColor(R.color.navButtonTextColor));//0xffffffff);
				button.setHeight(80);
				button.setTextSize(24);
				button.setGravity(Gravity.CENTER);
				button.setBackgroundColor(getResources().getColor(R.color.navButtonColor));
			}
			else
			{
				button = (TextView)convertView;
			}
			button.setTag(number);
			button.setText(number == 0 ?
					getResources().getString(R.string.intro) :
					Integer.toString(number));
			return button;
		}
	}
}
