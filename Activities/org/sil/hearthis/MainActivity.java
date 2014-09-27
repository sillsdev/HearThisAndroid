package org.sil.hearthis;

import org.sil.hearthis.R;

import Script.BookInfo;
import Script.Project;
import Script.SampleScriptProvider;
import android.app.Activity;
import android.content.Context;
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
			Button bookButton = (Button)inflater.inflate(R.layout.book_button, null);
			bookButton.setText(book.Name);
			bookFlow.addView(bookButton);
		}
	}
}
