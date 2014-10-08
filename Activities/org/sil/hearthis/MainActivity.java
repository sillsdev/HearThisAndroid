package org.sil.hearthis;

import org.sil.hearthis.R;
import org.sil.palaso.Graphite;

import Script.BookInfo;
import Script.Project;
import Script.RealScriptProvider;
import Script.SampleScriptProvider;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
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
        Graphite.loadGraphite();
		setContentView(R.layout.activity_main);
		// Todo: scan org.sil.hearthis/files for folders containing info.txt and open first
		// Todo: remember last project
		// Todo: if no real project available use SampleScriptProvider.
		RealScriptProvider scripture = new RealScriptProvider(getExternalFilesDir(null) + "/" + "Dhh");
		Project project = new Project("Sample", scripture);
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
			Intent chooseChapter = new Intent(MainActivity.this, ChooseChapterActivity.class);
			chooseChapter.putExtra("bookInfo", book);
			startActivity(chooseChapter);
		}
	};
}
