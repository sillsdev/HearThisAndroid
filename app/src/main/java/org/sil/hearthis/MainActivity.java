package org.sil.hearthis;

import org.sil.palaso.Graphite;

import Script.BibleLocation;
import Script.BookInfo;
import Script.FileSystem;
import Script.IScriptProvider;
import Script.Project;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

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
		
		if (requestRecordAudioPermission()) {
			// We already have permission to record audio, proceed normally.
			launchChooseBookIfProject();
		}
		// Otherwise the function will have requested permission, which launches the dialog as
		// an activity; we must not launch another activity which would suppress the dialog.
		// If we eventually get permission, we will go ahead with launchChooseBookIfProject().
	}

	// completely arbitrary, especially when we're only asking for one dangerous permission.
	// I just thought it might be useful to have a fairly distinctive number, for debugging.
	private final int MY_PERMISSIONS_REQUEST_RECORD_AUDIO = 17;

	// Although the app declares that it needs permission to record audio, because it is considered
    // a dangerous permission the user must grant it explicitly through this procedure from API23 on.
    // In theory, the user could revoke it at any time, and we should check it every time we need it.
    // Since our app may be resumed when the user thinks it is being started up, it would be tempting
	// to check on resume; but this is problematic because a resume happens when the user closes the
	// permission dialog. If we then ask again if it is still denied, we are refusing to take "no"
	// for an answer, and displaying an infinite succession of permission dialogs.
	// In practice, I think users will understand that HT needs permission to record audio and will
	// grant it the first time the program starts up after installation. At least they will get that
	// one request instead of just being left to wonder why it won't record (more often, they wonder
	// why it won't play back, because it isn't obvious that it didn't record). So this may not be
	// the best or final solution to this problem, but it's a big and very possibly sufficient step.
	private boolean requestRecordAudioPermission() {
		if (ContextCompat.checkSelfPermission(this,
				Manifest.permission.RECORD_AUDIO)
				!= PackageManager.PERMISSION_GRANTED) {

			// Permission is not granted
			// Should we show an explanation? This test returns true if the user has previously
			// denied it. I don't think it's worth trying to explain why HTA needs this permission;
			// it's too obvious. If we decide to, this is how.
			//if (ActivityCompat.shouldShowRequestPermissionRationale(thisActivity,
			//		Manifest.permission.READ_CONTACTS)) {
				// Show an explanation to the user *asynchronously* -- don't block
				// this thread waiting for the user's response! After the user
				// sees the explanation, try again to request the permission.
			//} else {
				// No explanation needed, we can request the permission.
				ActivityCompat.requestPermissions(this,
						new String[] {Manifest.permission.RECORD_AUDIO},
						MY_PERMISSIONS_REQUEST_RECORD_AUDIO);

				// MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
				// app-defined int constant. The callback method gets the
				// result of the request.
			//}
			return false;
		} else {
			// Permission has already been granted
			return true;
		}
	}

	@Override
	public void onRequestPermissionsResult(
			int requestCode,
			String permissions[],
			int[] grantResults) {
		switch (requestCode) {
			case MY_PERMISSIONS_REQUEST_RECORD_AUDIO:
				if (grantResults.length > 0
						&& grantResults[0] == PackageManager.PERMISSION_GRANTED) {
					// We have our essential permission, go ahead as we normally would when first
					// created, if we didn't need to stop and ask permission
					launchChooseBookIfProject();
				} else {
					// The user denied permission to record audio. We can't do much useful.
					// However, leaving this activity active, which is all about needing to sync,
					// is not helpful (if the user has already done that). We may improve on this
					// eventually, but for now, we'll display a toast and move to he next
					// activity normally so they can find out for themselves how crippled the app
					// is without this permission.
					// If we stick with this basic approach we may want to make this more conspicuous.
					Toast.makeText(MainActivity.this, R.string.no_use_without_record, Toast.LENGTH_LONG).show();
					launchChooseBookIfProject();
				}
		}
	}



	// Among other cases, we get resumed when the sync activity closes; typically we now have
	// a project and can carry on opening it.
	@Override
	protected void onResume() {
		super.onResume();
		if (ContextCompat.checkSelfPermission(this,
				Manifest.permission.RECORD_AUDIO)
				== PackageManager.PERMISSION_GRANTED) {
			launchChooseBookIfProject();
		}
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
		if (rootDirs.size() > 1) // Todo: and we haven't remembered a location!
		{
			startActivity(new Intent(this, ChooseProjectActivity.class));
			return true;
		}
		launchProject(this);
		return true;
	}

	public static void launchProject(Activity parent) {
		IScriptProvider provider = ServiceLocator.getServiceLocator().getScriptProvider();
		BibleLocation location = provider.getLocation();
		if (location != null) {
			Intent record = new Intent(parent, RecordActivity.class);
			Project project = ServiceLocator.getServiceLocator().getProject();
			record.putExtra("bookInfo", project.Books.get(location.bookNumber));
			record.putExtra("chapter", location.chapterNumber);
			record.putExtra("line", location.lineNumber);
			parent.startActivity(record);
		} else {
			Intent chooseBook = new Intent(parent, ChooseBookActivity.class);
			parent.startActivity(chooseBook);
		}
	}

	void LaunchSyncActivity() {
		Intent sync = new Intent(this, SyncActivity.class);
		startActivity(sync);
	}
}
