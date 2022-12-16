package org.sil.hearthis;

import Script.BibleLocation;
import Script.FileSystem;
import Script.IScriptProvider;
import Script.Project;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.util.ArrayList;

public class MainActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
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
			// We don't yet have permission.
			// We will ask again, if necessary, when the user presses the record button.
			// But we really want it now so the volume meter can work. Explain this while requesting.
			// Using a toast didn't work.
			//Toast.makeText(MainActivity.this, R.string.record_for_volume, Toast.LENGTH_LONG).show();
			// This is a somewhat more standard way to decide whether to explain that we need
			// permission.
			if (ActivityCompat.shouldShowRequestPermissionRationale(this,
					Manifest.permission.RECORD_AUDIO)) {
				new AlertDialog.Builder(this)
						.setTitle(R.string.need_permissions)
						.setMessage(R.string.record_for_volume)
						.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								ActivityCompat.requestPermissions(MainActivity.this,
										new String[]{Manifest.permission.RECORD_AUDIO},
										MY_PERMISSIONS_REQUEST_RECORD_AUDIO);
							}
						})
						.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								// No permission, proceed as usual
								launchChooseBookIfProject();
							}
						})
						.create().show();
			} else {
				// No explanation needed, we can request the permission.
				ActivityCompat.requestPermissions(this,
						new String[]{Manifest.permission.RECORD_AUDIO},
						MY_PERMISSIONS_REQUEST_RECORD_AUDIO);

				// MY_PERMISSIONS_REQUEST_RECORD_AUDIO is an
				// app-defined int constant. The callback method gets the
				// result of the request.
			}
			return false;
		} else {
			// Permission has already been granted
			return true;
		}
	}

	// This gets called by the system when the user has somehow responded to our request for
	// permission to record audio. Once we have a response, we move to the appropriate activity,
	// depending on whether the user has previously selected a project and passage.
	@Override
	public void onRequestPermissionsResult(
			int requestCode,
			String permissions[],
			int[] grantResults) {
		switch (requestCode) {
			case MY_PERMISSIONS_REQUEST_RECORD_AUDIO:
				if (grantResults.length > 0) {
					if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
						// We have our essential permission. Nothing special to do, just means the
						// volume meter will start working
					} else {
						// The user denied permission to record audio. We'll ask again if they try
						// to record.
					}
					// Either way, once the user closes the dialog, show the appropriate next activity.
					launchChooseBookIfProject();
				}
		}
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
