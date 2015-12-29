package org.sil.hearthis;

import org.apmem.tools.layouts.FlowLayout.LayoutParams;
import org.sil.hearthis.R;
import org.sil.palaso.Graphite;

import Script.BookInfo;
import Script.FileSystem;
import Script.IFileSystem;
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

import java.util.ArrayList;

public class MainActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        Graphite.loadGraphite();
		setContentView(R.layout.activity_main);
		ServiceLocator.getServiceLocator().init(this);
		FileSystem fs = ServiceLocator.getServiceLocator().getFileSystem();
		String rootDir = ServiceLocator.getServiceLocator().externalFilesDirectory;
		ArrayList<String> rootDirs = fs.getDirectories(rootDir);
		if (rootDirs.isEmpty())
			return; // Leave the main activity active (allows user to sync a project).
		Intent chooseBook = new Intent(MainActivity.this, ChooseBookActivity.class);
		startActivity(chooseBook);
	}
	

}
