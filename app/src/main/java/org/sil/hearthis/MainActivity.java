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
		ServiceLocator.getServiceLocator().init(this);
		Intent chooseBook = new Intent(MainActivity.this, ChooseBookActivity.class);
		startActivity(chooseBook);
	}
	

}
