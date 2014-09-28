package org.sil.hearthis;

import org.sil.hearthis.R;

import Script.BookInfo;
import Script.Project;
import Script.SampleScriptProvider;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.renderscript.Sampler;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

public class MainActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		Project project = new Project("Sample", new SampleScriptProvider());
		setProject(project);
	}
	
	public void setProject(Project project) {
		LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		ViewGroup bookFlow = (ViewGroup) findViewById(R.id.booksFlow);
		for (BookInfo book : project.Books) {
			BookButton bookButton = (BookButton)inflater.inflate(R.layout.book_button, null);
			bookButton.Model = book;
			bookButton.setOnClickListener(bookButtonListener);
			bookButton.setTag(book);
			bookFlow.addView(bookButton);
		}
	}
	
	public android.view.View.OnClickListener bookButtonListener = new android.view.View.OnClickListener() {
		
		@Override
		public void onClick(View v) {
			BookInfo book = (BookInfo)v.getTag();
			LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			ViewGroup chapsFlow = (ViewGroup) findViewById(R.id.chapssFlow);
			chapsFlow.removeAllViews();
			for (int i = 0; i <= book.ChapterCount; i++) {
				Button chapButton = (Button) inflater.inflate(R.layout.chap_button, null);
				chapButton.setText(Integer.toString(i));
				int safeChapNum = i;
				chapButton.setOnClickListener(new android.view.View.OnClickListener() {
					
					@Override
					public void onClick(View v) {
						// Todo: set up activity for recording chapter safeChapNum of book						
					}
				});
				chapsFlow.addView(chapButton);
			}
			
		}
	};
}
