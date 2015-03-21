package org.sil.hearthis;

import org.apmem.tools.layouts.FlowLayout.LayoutParams;
import org.sil.hearthis.R;
import org.sil.palaso.Graphite;

import Script.BookInfo;
import Script.IScriptProvider;
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
		IScriptProvider scripture = GetProvider(this);
		Project project = new Project("Sample", scripture);
		setProject(project);
	}

    public static IScriptProvider GetProvider(Activity activity)
    {
        // Todo: Dhh is just for testing, eventually we have to pick a project if there is
        // more than one, do something appropriate if there are none. Maybe go right to sync?
        return new RealScriptProvider(activity.getExternalFilesDir(null) + "/" + "Dhh");
    }
	
	public void setProject(Project project) {
		LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		ViewGroup bookFlow = (ViewGroup) findViewById(R.id.booksFlow);
		for (BookInfo book : project.Books) {
			int resid = R.layout.book_button;
			if (book.BookNumber == 39) {
				// Matthew: start new line
				resid = R.layout.newline_book_button;
			}
			
			// This next line is rather non-obvious. We must pass the bookFlow to the inflator
			// so that it can be used to create the Layout for the button: a custom layout which implements
			// the extra properties that the FlowLayout recognizes for its children like newline.
			// But, when we pass a parent, without the third argument inflate adds the button to the
			// flowLayout itself and returns the parent. That leaves us without an easy way to get the
			// new button, on which we want to set other properties. So we pass false (do not add
			// to parent) and thus get the button itself back from inflate. Then of course we must
			// add it to the parent ourselves.
			BookButton bookButton = (BookButton)inflater.inflate(resid, bookFlow, false);
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
