package org.sil.hearthis;

import java.io.File;

import org.sil.palaso.Graphite;

import Script.BookInfo;
import Script.IScriptProvider;
import Script.ScriptLine;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.media.MediaRecorder.AudioEncoder;
import android.media.MediaRecorder.AudioSource;
import android.media.MediaRecorder.OutputFormat;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class RecordActivity extends ActionBarActivity
{
	ViewGroup linesView;
	Button record;
	Menu recordMenu;

	int activeLine = 0;
	int lineCount;
	int chapter;
	BookInfo book;
	IScriptProvider provider;
	int backColour = 0xffffff;

	String fileName = "";
	
	Typeface mtfl;
	
	MediaRecorder recorder = null;
	
	@Override
	protected void onCreate(Bundle state)
	{
		super.onCreate(state);
		setContentView(R.layout.activity_record);
		
		mtfl = (Typeface)Graphite.addFontResource(getAssets(), "CharisSILAfr-R.ttf", "charis", 0, "", "");

		if (state != null)
			activeLine = state.getInt("activeLine");
		Intent intent = getIntent();
		Bundle extras = intent.getExtras();
		book = (BookInfo)extras.get("book");
		chapter = extras.getInt("chapter");
		int bookNum = book.getBookNumber();
		provider = book.getScriptProvider();
		lineCount = provider.GetScriptLineCount(bookNum, chapter);

		LayoutInflater inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		linesView = (ViewGroup)findViewById(R.id.textLineHolder);
		linesView.removeAllViews();
		
		for (int i = 0; i < lineCount; i++)
		{
			ScriptLine line = provider.GetLine(bookNum, chapter, i);
			TextView lineView = (TextView) inflater.inflate(R.layout.text_line, null);
//			if (i == 1)
//				lineView.setText("\u00F0\u0259 k\u02B0\u00E6t\u02B0 s\u00E6\u0301t\u02B0 o\u0303\u0300\u014A mi\u0302\u02D0");
//			else if (i == 2)
//				lineView.setText("Grandroid says 'Hello!'");
//			else
			lineView.setText(line.Text);
			lineView.setTag(i);
			lineView.setTypeface(mtfl, 0);
			if (i == activeLine)
			{
				lineView.setTextColor(getResources().getColor(R.color.activeTextLine));
			}
			lineView.setOnClickListener(new OnClickListener()
			{
				@Override
				public void onClick(View view)
				{
					((TextView)linesView.getChildAt(activeLine)).setTextColor(getResources().getColor(R.color.labelTextColor));
					activeLine = (Integer)view.getTag();
					((TextView)view).setTextColor(getResources().getColor(R.color.activeTextLine));
				}
			});

			linesView.addView(lineView);
		}

		record = (Button)findViewById(R.id.recordButton);
		record.setOnTouchListener(new OnTouchListener()
		{
			@Override
			public boolean onTouch(View v, MotionEvent e)
			{
				recordButtonTouch(e);
				return true; // we handle all touch events on this button.
			}

		});
		if (state == null)
			Toast.makeText(this, R.string.volumeButtonRecord, Toast.LENGTH_LONG).show();
	}

	@Override
	public void onSaveInstanceState(Bundle state)
	{
		super.onSaveInstanceState(state);
		state.putInt("activeLine", activeLine);
	}

	void nextButtonClicked()
	{
		if (activeLine >= lineCount -1)
		{
			// Todo: move to start of next chapter or if need be book.
			return;
		}
		TextView lineView = (TextView) linesView.getChildAt(activeLine);
		int topPrev = lineView.getTop();
		lineView.setTextColor(getResources().getColor(R.color.contextTextLine));
		activeLine++;
		lineView = (TextView)linesView.getChildAt(activeLine);
		lineView.setTextColor(getResources().getColor(R.color.activeTextLine));
		
		int top = lineView.getTop();
		int bottom = lineView.getBottom();
		int bottomNext = bottom;
		if (activeLine < lineCount - 1)
		{
			bottomNext = linesView.getChildAt(activeLine +1).getBottom();
		}
		ScrollView scrollView = (ScrollView) linesView.getParent();
		int scrollPos = scrollView.getScrollY();
		int height = scrollView.getHeight();
		
		if (bottomNext < scrollPos + height)
			return; // bottom of next line is already visible, nothing to do
		
		// Initial proposal is to scroll so the bottom of the next line is just visible
		scrollPos = bottomNext - height - 3;
		if (scrollPos > topPrev)
		{
			// bother! Can't show all of previous and following context lines.
			// it's more important to show the previous line.
			// Next try: show previous line
			scrollPos = topPrev;
			if (scrollPos + height < bottom)
			{
				// worse still! can't show all of previous and current line
				// try showing bottom of current
				scrollPos = bottomNext - height - 3;
				if (scrollPos > top)
				{
					// Can't even see all of current line! Show the top at least.
					scrollPos = top;
				}
			}
		}
		scrollView.scrollTo(0, scrollPos);
	}
	
	void recordButtonTouch(MotionEvent e)
	{
		int maskedAction = e.getActionMasked();

		switch (maskedAction)
		{
		case MotionEvent.ACTION_DOWN:
			record.setBackgroundColor(0xffff0000);
			startRecording();
			break;

		case MotionEvent.ACTION_UP:
		case MotionEvent.ACTION_CANCEL:
			record.setBackgroundColor(0x3f000000);
			stopRecording();
			break;
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event)
	{
		switch (keyCode)
		{
		case KeyEvent.KEYCODE_VOLUME_DOWN:
		case KeyEvent.KEYCODE_VOLUME_UP:
			record.setBackgroundColor(0xffff0000);
			startRecording();
			return true;
		default:
			return onKeyDown(keyCode, event);
		}
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event)
	{
		switch (keyCode)
		{
		case KeyEvent.KEYCODE_VOLUME_DOWN:
		case KeyEvent.KEYCODE_VOLUME_UP:
			record.setBackgroundColor(0x3f000000);
			stopRecording();
			return true;
		default:
			return super.onKeyUp(keyCode, event);
		}
	}

	@Override
	public boolean onKeyLongPress(int keyCode, KeyEvent event)
	{
		switch (keyCode)
		{
		case KeyEvent.KEYCODE_VOLUME_DOWN:
		case KeyEvent.KEYCODE_VOLUME_UP:
			// ignore the long press, as the button is being held
			return true;
		default:
			return super.onKeyUp(keyCode, event);
		}
	}

	void startRecording()
	{
		if (recorder != null)
		{
		  recorder.release();
		}
		recorder = new MediaRecorder();
		recorder.setAudioSource(AudioSource.MIC);
		// Looking for a good combination that produces a useable file.
		// THREE_GPP/AMR_NB was suggested at http://www.grokkingandroid.com/recording-audio-using-androids-mediarecorder-framework/
		// Eclipse complains that AMR_NB not supported in API 8 (requires 10).
		// http://www.techotopia.com/index.php/Android_Audio_Recording_and_Playback_using_MediaPlayer_and_MediaRecorder
		// also suggests THREE_GPP/AMR_NB. In another place they suggest AAC_ADTS/AAC.
		// THREE_GPP/AMR_NB produces a small file which neither phone nor WMP can play.
		// THREE_GPP/DEFAULT likewise.
		// This combination produces a file that WMP can play.
		recorder.setOutputFormat(OutputFormat.MPEG_4);
		recorder.setAudioEncoder(AudioEncoder.AAC);
		fileName = provider.getRecordingFileName(book.getBookNumber(), chapter, activeLine);
		File file = new File(getExternalFilesDir(null), fileName);
		recorder.setOutputFile(file.getAbsolutePath());
		try
		{
		recorder.prepare();
		recorder.start();
		}
		catch (Exception e)
		{
			e.printStackTrace();
//				  Log.e("giftlist", "io problems while preparing [" +
//						file.getAbsolutePath() + "]: " + e.getMessage());
		}		
	}
	
	void stopRecording()
	{
	   if (recorder != null)
	   {
		   try
		   {
		   recorder.stop();
		   }
		   catch (Exception e)
		   {
			   e.printStackTrace();
		   }
		   recorder.reset();
		   recorder.release();
		   File file = new File(getExternalFilesDir(null), fileName);
		   Log.d("Recorder", "Recorder finished and made file " + file.getAbsolutePath() + " with length " + file.length());
		   recorder = null;
		   provider.noteBlockRecorded(book.getBookNumber(), chapter, activeLine);
	   }
	}
	
	void playButtonClicked()
	{
		MediaPlayer mp = new MediaPlayer();
		try
		{
			// Todo:  file name and location based on book, chapter, segment

//			AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
//			int maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
//			Log.d("Player", "current volume is " + audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
//					+ " of max " + maxVol);
//			audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVol, 0);
			
			File file = new File(getExternalFilesDir(null), fileName);
			mp.setDataSource(file.getAbsolutePath());
			mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
			mp.prepare();
			mp.start();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		recordMenu = menu;
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu_record, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch(item.getItemId())
		{
		case R.id.play:
			playButtonClicked();
			return true;
		case R.id.next:
			nextButtonClicked();
			return true;
		case R.id.sync:
			Intent sync = new Intent(this, SyncActivity.class);
			startActivity(sync);
			return true;
		case R.id.help_record:
			Toast.makeText(this, R.string.volumeButtonRecord, Toast.LENGTH_LONG).show();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
