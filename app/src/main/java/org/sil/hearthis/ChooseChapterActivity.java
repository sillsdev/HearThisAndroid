package org.sil.hearthis;

import script.BookInfo;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.os.BundleCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.Objects;

public class ChooseChapterActivity extends AppCompatActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
			EdgeToEdge.enable(this);
            // Explicitly set dark icons for the white status bar when edge-to-edge is enabled
            new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView())
                    .setAppearanceLightStatusBars(false);
        }
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_chapters);

		View mainLayout = findViewById(R.id.main);
		if (mainLayout != null) {
			ViewCompat.setOnApplyWindowInsetsListener(mainLayout, (v, insets) -> {
				Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
				v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
				return insets;
			});
		}

		Objects.requireNonNull(getSupportActionBar()).setTitle(R.string.choose_chapter);
		ServiceLocator.getServiceLocator().init(this);
		Intent intent = getIntent();
		Bundle extras = intent.getExtras();
        assert extras != null;
        final BookInfo book = BundleCompat.getSerializable(extras, "bookInfo", BookInfo.class);
		
		TextView bookBox = findViewById(R.id.bookNameText);
        assert book != null;
        bookBox.setText(book.Name);
		
		LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		ViewGroup chapsFlow = findViewById(R.id.chapsFlow);
		chapsFlow.removeAllViews();

		ChapterButton chapButton;
		for (int i = 0; i <= book.ChapterCount; i++) {
			chapButton = (ChapterButton) inflater.inflate(R.layout.chap_button, chapsFlow, false);
			chapButton.init(book.getScriptProvider(), book.BookNumber, i);
			final int safeChapNum = i;
			chapButton.setOnClickListener( v -> {
				// set up activity for recording chapter safeChapNum of book
				Intent record = new Intent(ChooseChapterActivity.this, RecordActivity.class);
				record.putExtra("bookInfo", book);
				record.putExtra("chapter", safeChapNum);
				startActivity(record);
			});
			chapsFlow.addView(chapButton);
		}
	}
}
