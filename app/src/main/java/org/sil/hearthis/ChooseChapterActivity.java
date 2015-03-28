package org.sil.hearthis;

import Script.BookInfo;
import Script.Project;
import Script.SampleScriptProvider;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

public class ChooseChapterActivity extends Activity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_chapters);
		Intent intent = getIntent();
		Bundle extras = intent.getExtras();
		final BookInfo book = (BookInfo)extras.get("bookInfo");
		
		TextView bookBox = (TextView)findViewById(R.id.bookNameText);
		bookBox.setText(book.Name);
		
		LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		ViewGroup chapsFlow = (ViewGroup) findViewById(R.id.chapsFlow);
		chapsFlow.removeAllViews();
		for (int i = 0; i <= book.ChapterCount; i++) {
            ChapterButton chapButton = (ChapterButton) inflater.inflate(R.layout.chap_button, null);
			chapButton.init(book.getScriptProvider(), book.BookNumber, i);
			final int safeChapNum = i;
			chapButton.setOnClickListener(new android.view.View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					// set up activity for recording chapter safeChapNum of book						
					Intent record = new Intent(ChooseChapterActivity.this, RecordActivity.class);
					record.putExtra("bookInfo", book);
					record.putExtra("chapter", safeChapNum);
					startActivity(record);
				}
			});
			chapsFlow.addView(chapButton);
		}

	}
}
