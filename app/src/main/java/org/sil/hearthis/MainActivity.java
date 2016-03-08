package org.sil.hearthis;

import org.sil.palaso.Graphite;

import Script.BibleLocation;
import Script.BookInfo;
import Script.FileSystem;
import Script.IScriptProvider;
import Script.Project;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;

import java.io.File;
import java.util.ArrayList;

public class MainActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//Graphite.loadGraphite();
		setContentView(R.layout.activity_main);
		ServiceLocator.getServiceLocator().init(this);
		Button sync = (Button) findViewById(R.id.mainSyncButton);
		final MainActivity thisActivity = this; // required to use it in touch handler
		sync.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				// When a sync completes we want to evaluate the results and launch the book chooser if appropriate
				LaunchSyncActivity();
			}
		});

		launchChooseBookIfProject();
	}

	// Among other cases, we get resumed when the sync activity closes; typically we now have
	// a project and can carry on opening it.
	@Override
	protected void onResume() {
		super.onResume();
		launchChooseBookIfProject();
	}

	private ArrayList<String> getProjectRootDirectories() {
		FileSystem fs = ServiceLocator.getServiceLocator().getFileSystem();
		String rootDir = ServiceLocator.getServiceLocator().externalFilesDirectory;
		return fs.getDirectories(rootDir);
	}

	private boolean launchChooseBookIfProject() {
		ArrayList<String> rootDirs = getProjectRootDirectories();
		if (rootDirs.isEmpty()) {
			return false; // Leave the main activity active (allows user to sync a project).
		}
		IScriptProvider provider = ServiceLocator.getServiceLocator().getScriptProvider();
		BibleLocation location = provider.getLocation();
		if (location != null) {
			Intent record = new Intent(this, RecordActivity.class);
			Project project = ServiceLocator.getServiceLocator().getProject();
			record.putExtra("bookInfo", project.Books.get(location.bookNumber));
			record.putExtra("chapter", location.chapterNumber);
			record.putExtra("line", location.lineNumber);
			startActivity(record);
		} else {
			Intent chooseBook = new Intent(MainActivity.this, ChooseBookActivity.class);
			startActivity(chooseBook);
		}
		return true;
	}

	void LaunchSyncActivity() {
		Intent sync = new Intent(this, SyncActivity.class);
		startActivity(sync);
	}
}
