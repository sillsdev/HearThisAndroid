package org.sil.hearthis;

import Script.BookInfo;
import Script.IScriptProvider;
import Script.Project;
import Script.SampleScriptProvider;
import Script.ScriptLine;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

public class RecordActivity extends Activity {
	
	int _activeLine;
	ViewGroup _linesView;
	int _lineCount;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_record);
		
		Intent intent = getIntent();
		Bundle extras = intent.getExtras();
		BookInfo book = (BookInfo)extras.get("bookInfo");
		int chapNum = extras.getInt("chapter");
		int bookNum = book.BookNumber;
		IScriptProvider provider = book.getScriptProvider();
		_lineCount = provider.GetScriptLineCount(bookNum, chapNum);
		_activeLine = 0;
		
		LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		_linesView = (ViewGroup) findViewById(R.id.textLineHolder);
		_linesView.removeAllViews();
		
		for (int i = 0; i < _lineCount; i++) {
			ScriptLine line = provider.GetLine(bookNum, chapNum, i);
			TextView lineView = (TextView) inflater.inflate(R.layout.text_line, null);
			lineView.setText(line.Text);
			if (i == 0) {
				lineView.setTextColor(getResources().getColor(R.color.activeTextLine));
			}
			_linesView.addView(lineView);
		}
		
		Button next =  (Button) findViewById(R.id.nextButton);
		next.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				nextButtonClicked();
			}
		});
	}
	
	void nextButtonClicked() {
		if (_activeLine >= _lineCount-1) {
			// Todo: move to start of next chapter or if need be book.
			return;
		}
		TextView lineView = (TextView) _linesView.getChildAt(_activeLine);
		int topPrev = lineView.getTop();
		lineView.setTextColor(getResources().getColor(R.color.contextTextLine));
		_activeLine++;
		lineView = (TextView)_linesView.getChildAt(_activeLine);
		lineView.setTextColor(getResources().getColor(R.color.activeTextLine));
		
		int top = lineView.getTop();
		int bottom = lineView.getBottom();
		int bottomNext = bottom;
		if (_activeLine < _lineCount - 1) {
			bottomNext = _linesView.getChildAt(_activeLine+1).getBottom();
		}
		ScrollView scrollView = (ScrollView) _linesView.getParent();
		int scrollPos = scrollView.getScrollY();
		int height = scrollView.getHeight();
		
		if (bottomNext < scrollPos + height)
			return; // bottom of next line is already visible, nothing to do
		
		// Initial proposal is to scroll so the bottom of the next line is just visible
		scrollPos = bottomNext - height - 3;
		if (scrollPos > topPrev) {
			// bother! Can't show all of previous and following context lines.
			// it's more important to show the previous line.
			// Next try: show previous line
			scrollPos = topPrev;
			if (scrollPos + height < bottom) {
				// worse still! can't show all of previous and current line
				// try showing bottom of current
				scrollPos = bottomNext - height - 3;
				if (scrollPos > top) {
					// Can't even see all of current line! Show the top at least.
					scrollPos = top;
				}
			}
		}
		scrollView.scrollTo(0, scrollPos);
	}
}
