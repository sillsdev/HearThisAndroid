package org.sil.hearthis;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import Script.BibleLocation;
import Script.BookInfo;
import Script.IScriptProvider;
import Script.ScriptLine;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.media.MediaRecorder.AudioEncoder;
import android.media.MediaRecorder.AudioSource;
import android.media.MediaRecorder.OutputFormat;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.ScrollView;
import android.widget.TextView;

public class RecordActivity extends Activity implements View.OnTouchListener, WavAudioRecorder.IMonitorListener, MediaPlayer.OnCompletionListener {
	
	int _activeLine;
	LinesView _linesView;
	int _lineCount;
    int _bookNum;
    int _chapNum;
    IScriptProvider _provider;

    static final String BOOK_NUM = "bookNumber";
    static final String CHAP_NUM = "chapterNumber";
    static final String ACTIVE_LINE = "activeLine";

	//Typeface mtfl;

	// We can't use two recorders at once, so may as well be static.
	static MediaRecorder recorder = null;
	static WavAudioRecorder waveRecorder = null;
	public static boolean useWaveRecorder = true;
	LevelMeterView levelMeter;

	// Enhance: move to AudioButtonsFragment
	NextButton nextButton;
	RecordButton recordButton;
	PlayButton playButton;
	Date startRecordingTime;
	boolean starting = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_record);
		getActionBar().setTitle(R.string.record_title);
		
		//mtfl = (Typeface)Graphite.addFontResource(getAssets(), "CharisSILAfr-R.ttf", "charis", 0, "", "");
		
		Intent intent = getIntent();
		Bundle extras = intent.getExtras();
		BookInfo book = (BookInfo)extras.get("bookInfo");
        if (book != null) {
            // invoked from chapter page
            _chapNum = extras.getInt("chapter");
            _bookNum = book.BookNumber;
            _provider = book.getScriptProvider();
            _activeLine = extras.getInt("line", 0);
        }
        else {
            // re-created, maybe after rotate, maybe eventually we start up here?
            _chapNum = savedInstanceState.getInt(CHAP_NUM);
            _bookNum = savedInstanceState.getInt(BOOK_NUM);
            _activeLine = savedInstanceState.getInt(ACTIVE_LINE);
            _provider = ServiceLocator.getServiceLocator().init(this).getScriptProvider();
        }
        _lineCount = _provider.GetScriptLineCount(_bookNum, _chapNum);

		LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		_linesView = (LinesView) findViewById(R.id.textLineHolder);
		_linesView.removeAllViews();
		
		for (int i = 0; i < _lineCount; i++) {
			ScriptLine line = _provider.GetLine(_bookNum, _chapNum, i);
			TextView lineView = (TextView) inflater.inflate(R.layout.text_line, null);
//			if (i == 1)
//				lineView.setText("\u00F0\u0259 k\u02B0\u00E6t\u02B0 s\u00E6\u0301t\u02B0 o\u0303\u0300\u014A mi\u0302\u02D0");
//			else if (i == 2)
//				lineView.setText("Grandroid says 'Hello!'");
//			else
			lineView.setText(line.Text);
			//lineView.setTypeface(mtfl, 0);

			_linesView.addView(lineView);
			setTextColor(i);
			lineView.setOnTouchListener(this);
		}

		_linesView.updateScale(); // do this AFTER we get the original size above!
		
		nextButton =  (NextButton) findViewById(R.id.nextButton);
		nextButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				nextButtonClicked();
			}
		});
		
		recordButton =  (RecordButton) findViewById(R.id.recordButton);
		recordButton.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent e) {
				recordButtonTouch(e);
				return true; // we handle all touch events on this button.
			}

		});
		
		playButton =  (PlayButton) findViewById(R.id.playButton);
		playButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				playButtonClicked();
			}
		});
		if (_lineCount > 0)
			setActiveLine(_activeLine);
		levelMeter = (LevelMeterView) findViewById(R.id.levelMeter);
	}

	@Override
	protected void onResume() {
		super.onResume();
		// The activity has become visible (it is now "resumed").
		startMonitoring();
	}
	@Override
	protected void onPause() {
		super.onPause();
		stopMonitoring(); //  don't want to waste cycles monitoring while paused.
		BibleLocation location = new BibleLocation();
		location.bookNumber = _bookNum;
		location.chapterNumber = _chapNum;
		location.lineNumber = _activeLine;
		_provider.saveLocation(location);
	}

	@Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putInt(CHAP_NUM, _chapNum);
        savedInstanceState.putInt(BOOK_NUM,_bookNum);
        savedInstanceState.putInt(ACTIVE_LINE,_activeLine);
        super.onSaveInstanceState(savedInstanceState);
    }
	
	void nextButtonClicked() {
		if (_activeLine >= _lineCount - 1) {
			// Todo: move to start of next chapter or if need be book.
			return;
		}
		setActiveLine(_activeLine + 1);
	}

	void setTextColor(int lineNo) {
		TextView lineView = (TextView) _linesView.getChildAt(lineNo);
		int lineColor = getResources().getColor(R.color.contextTextLine);
		if (lineNo == _activeLine) {
			lineColor = getResources().getColor(R.color.activeTextLine);
		} else {
			String recordingFilePath = _provider.getRecordingFilePath(_bookNum, _chapNum, lineNo);
			if (new File(recordingFilePath).exists()) {
				lineColor = getResources().getColor(R.color.recordedTextLine);
			}
		}
		lineView.setTextColor(lineColor);
	}
	void setActiveLine(int lineNo) {
		int oldLine = _activeLine;
		_activeLine = lineNo;
		setTextColor(oldLine);
		setTextColor(_activeLine);

		ScrollView scrollView = (ScrollView) _linesView.getParent();
		int[] tops = new int[_linesView.getChildCount() + 1];
		for (int i = 0; i < tops.length - 1; i++) {
			tops[i] = _linesView.getChildAt(i).getTop();
		}
		tops[tops.length - 1] = _linesView.getChildAt(tops.length - 2).getBottom();
		scrollView.scrollTo(0, getNewScrollPosition(scrollView.getScrollY(), scrollView.getHeight(), _activeLine, tops));
		_recordingFilePath = _provider.getRecordingFilePath(_bookNum, _chapNum, _activeLine);
		recordButton.setIsDefault(true);
		nextButton.setIsDefault(false);
		playButton.setIsDefault(false);
		updateDisplayState();
	}

	private void updateDisplayState() {
		playButton.setButtonState(new File(_recordingFilePath).exists() ? BtnState.Normal : BtnState.Inactive);
	}

	static int getNewScrollPosition(int scrollPos, int height, int newLine, int[] tops) {
		int newScrollPos = scrollPos;
		int bottom = tops[newLine + 1];
		int bottomNext = bottom; // bottom of next line (or current, if no next)
		if (newLine < tops.length - 2) {
			bottomNext = tops[newLine + 2];
		}
		if (bottomNext > scrollPos + height) {
			// Not all of the following line is visible.
			// Initial proposal is to scroll so the bottom of the next line is just visible
			newScrollPos = bottomNext - height;
		}
		int top = tops[newLine];
		int topPrev = top; // top of previous line (or current, if no previous line)
		if (newLine > 0) {
			topPrev = tops[newLine - 1];
		}
		if (newScrollPos > topPrev) {
			// We do this after adjusting for following line because if we can't show both following
			// and previous lines, it's more important to show the previous line.
			// Next try: show previous line
			newScrollPos = topPrev;
			if (newScrollPos + height < bottom) {
				// worse still! can't show all of previous and current line
				// try showing bottom of current (and thus as much as possible of previous
				newScrollPos = bottom - height;
				if (newScrollPos > top) {
					// Can't even see all of current line! Show the top at least.
					newScrollPos = top;
				}
			}
		}
		return newScrollPos;
	}
	
	void recordButtonTouch(MotionEvent e) {
		int maskedAction = e.getActionMasked();

	    switch (maskedAction) {
		    case MotionEvent.ACTION_DOWN: {
		    	startRecording();
		      break;
		    }
		    case MotionEvent.ACTION_UP:
		    case MotionEvent.ACTION_CANCEL: {
		    	stopRecording();
		      break;
		    }
	    }
	}

	String _recordingFilePath = "";

	void startMonitoring() {
		if (waveRecorder != null)
			waveRecorder.release();
		waveRecorder = new WavAudioRecorder(AudioSource.MIC, 44100, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
		//waveRecorder.prepare(); no; this initializes (and so requires) output file.
		waveRecorder.setMonitorListener(this);
		waveRecorder.startMonitoring();
	}

	private void stopMonitoring() {
		if (waveRecorder != null) {
			waveRecorder.stop();
			waveRecorder.release();
			waveRecorder  = null;
		}
	}

	void startWaveRecorder() {
		if (waveRecorder != null)
			waveRecorder.release();
		waveRecorder = new WavAudioRecorder(AudioSource.MIC, 44100, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
		File oldRecording = new File(_recordingFilePath);
		if (oldRecording.exists())
			oldRecording.delete();
		waveRecorder.setOutputFile(_recordingFilePath);
		waveRecorder.prepare();
		waveRecorder.setMonitorListener(this);
		waveRecorder.start();
		recordButton.setWaiting(false);
		startRecordingTime = new Date();
		starting = false;
	}

	void startRecording() {
		recordButton.setButtonState(BtnState.Pushed);
		recordButton.setWaiting(true);
		if (useWaveRecorder) {
			starting = true; // protects against trying to stop the recording before we finish starting it.
			// Do the initialization of the recorder in another thread so the main one
			//  can color the button red until we really start recording.
			new Thread(new Runnable() {
				@Override
				public void run() {
					startWaveRecorder();
				}
			}).start();
			return;
		}
		if (recorder != null) {
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
		recorder.setAudioSamplingRate(44100);
		recorder.setAudioEncodingBitRate(44100);
		File file = new File(_recordingFilePath);
		File dir = file.getParentFile();
		if (!dir.exists())
			dir.mkdirs();
		recorder.setOutputFile(file.getAbsolutePath());
		try {
			recorder.prepare();
			recorder.start();
			recordButton.setWaiting(false);
			startRecordingTime = new Date();
		} catch (IOException e) {
            e.printStackTrace();
        }
	}
	
	void stopRecording() {
		Date beginStop = new Date();
		while (starting) {
			// ouch! this will probably be a short-recording problem! The thread that is
			// trying to start the recording hasn't finished! Wait until it does.
			try {
				Thread.sleep(100);
			} catch(InterruptedException e) {
				// shouldn't happen, but Java insists.
			}
		}
		recordButton.setButtonState(BtnState.Normal);
		recordButton.setWaiting(false);
		if (useWaveRecorder && waveRecorder != null)  {
			waveRecorder.stop();
			startMonitoring();
		}
	   else if (recorder != null) {
			recorder.stop();
			recorder.reset();
			recorder.release();
			File file = new File(_recordingFilePath);
			Log.d("Recorder", "Recorder finished and made file " + file.getAbsolutePath() + " with length " + file.length());
			recorder = null;
		}
		// Don't just use new Date() here. It can take ~half a second to get things stopped.
		if (beginStop.getTime() - startRecordingTime.getTime() < 500) {
			// Press not long enough; treat as failure.
			new AlertDialog.Builder(this)
					//.setTitle("Too short!")
					.setMessage("Hold down the record button while talking, and only let it go when you're done.")
					.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							// nothing to do
						}
					})
					.setIcon(android.R.drawable.ic_dialog_alert)
					.show();
			File badFile = new File(_recordingFilePath);
			if (badFile.exists()) {
				badFile.delete();
				// for now just ignore if we can't delete. (Does not throw.)
			}
			return; // skip state changes for successful recording
		}
		recordButton.setIsDefault(false);
		playButton.setIsDefault(true);
		nextButton.setIsDefault(false);
		updateDisplayState();
		_provider.noteBlockRecorded(_bookNum, _chapNum, _activeLine);
	}

	// Todo: disable when no recording exists.
	void playButtonClicked() {
		playButton.setPlaying(true);
		MediaPlayer mp = new MediaPlayer();
		mp.setOnCompletionListener(this);
		stopMonitoring();
		try {
			// Todo:  file name and location based on book, chapter, segment

//			AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
//			int maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
//			Log.d("Player", "current volume is " + audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
//					+ " of max " + maxVol);
//			audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVol, 0);
			
			File file = new File(_recordingFilePath);
			mp.setDataSource(file.getAbsolutePath());
			mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
			mp.prepare();
			mp.start();
		} catch (Exception e) {
			e.printStackTrace();
		}		
	}

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_record, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
		final int itemId = item.getItemId();
		if(itemId == R.id.sync){
            Intent sync = new Intent(this, SyncActivity.class);
            startActivity(sync);
            return true;
        }
		else if (itemId == R.id.choose) {
			Intent choose = new Intent(this, ChooseBookActivity.class);
			startActivity(choose);
			return true;
		}
        return false;
    }

	@Override
	public boolean onTouch(View view, MotionEvent motionEvent) {
		int newLine = _linesView.indexOfChild(view);
		setActiveLine(newLine);
		return false;
	}

	@Override
	public void maxLevel(int level) {
		levelMeter.setLevel(level *100 / Short.MAX_VALUE);
	}

	@Override
	public void onCompletion(MediaPlayer mediaPlayer) {
		playButton.setPlaying(false);
		playButton.setButtonState(BtnState.Normal);
		playButton.setIsDefault(false);
		recordButton.setIsDefault(false);
		nextButton.setIsDefault(true);
		startMonitoring();
	}
}
