package org.sil.hearthis;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

import script.BibleLocation;
import script.BookInfo;
import script.IScriptProvider;
import script.ScriptLine;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.AudioDeviceInfo;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.media.MediaRecorder.AudioEncoder;
import android.media.MediaRecorder.AudioSource;
import android.media.MediaRecorder.OutputFormat;
import android.os.Build;
import android.os.Bundle;


import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.os.BundleCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class RecordActivity extends AppCompatActivity implements View.OnClickListener, WavAudioRecorder.IMonitorListener, MediaPlayer.OnCompletionListener {

	int _activeLine;
	LinearLayout _linesView;
	int _lineCount;
	int _bookNum;
	int _chapNum;
	IScriptProvider _provider;

	static final String BOOK_NUM = "bookNumber";
	static final String CHAP_NUM = "chapterNumber";
	static final String ACTIVE_LINE = "activeLine";
	boolean usingSpeaker;
	boolean wasUsingSpeaker;
	MediaPlayer playButtonPlayer;

	// Back to instance variables to avoid resource contention, but using safe lifecycle management.
	private MediaRecorder recorder = null;
	private WavAudioRecorder waveRecorder = null;
	public static final boolean useWaveRecorder = true;
	LevelMeterView levelMeter;

	NextButton nextButton;
	RecordButton recordButton;
	PlayButton playButton;
	long startRecordingTime;
	private final Object startingLock = new Object();
	volatile boolean starting = false;

	String _recordingFilePath;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
			EdgeToEdge.enable(this);
            // Explicitly set dark icons for the white status bar when edge-to-edge is enabled
            new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView())
                    .setAppearanceLightStatusBars(false);
		}
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
			getWindow().getAttributes().layoutInDisplayCutoutMode =
					WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
		}
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_record);

		View root = findViewById(R.id.recordActivityRoot);
		if (root == null){
			root = findViewById(android.R.id.content);
		}

		ViewCompat.setOnApplyWindowInsetsListener(root, (v, windowInsets) -> {
			Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());

			v.setPadding(insets.left, insets.top, insets.right, 0);

			if (_linesView != null && _linesView.getParent() instanceof ScrollView scrollView) {
				scrollView.setPadding(0, 0, 0, insets.bottom);
				scrollView.setClipToPadding(false);
			}

			return windowInsets;
		});

		Objects.requireNonNull(getSupportActionBar()).setTitle(R.string.record_title);
		ServiceLocator.getServiceLocator().init(this);

		Intent intent = getIntent();
		Bundle extras = intent.getExtras();
		assert extras != null;
		BookInfo book = BundleCompat.getSerializable(extras, "bookInfo", BookInfo.class);
		if (book != null) {
			_chapNum = extras.getInt("chapter");
			_bookNum = book.BookNumber;
			_provider = book.getScriptProvider();
			_activeLine = extras.getInt("line", 0);
		} else if (savedInstanceState != null) {
			_chapNum = savedInstanceState.getInt(CHAP_NUM);
			_bookNum = savedInstanceState.getInt(BOOK_NUM);
			_activeLine = savedInstanceState.getInt(ACTIVE_LINE);
			_provider = ServiceLocator.getServiceLocator().init(this).getScriptProvider();
		} else {
			finish();
			return;
		}
		_lineCount = _provider.GetScriptLineCount(_bookNum, _chapNum);

		LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		_linesView = findViewById(R.id.textLineHolder);
		_linesView.removeAllViews();

		for (int i = 0; i < _lineCount; i++) {
			ScriptLine line = _provider.GetLine(_bookNum, _chapNum, i);
			TextView lineView = (TextView) inflater.inflate(R.layout.text_line, _linesView, false);
			lineView.setText(line.Text);
			//lineView.setTypeface(mtfl, 0);

			_linesView.addView(lineView);
			setTextColor(i);
			lineView.setOnClickListener(this);
		}

		((LinesView) findViewById(R.id.zoomView)).updateScale();

		nextButton = findViewById(R.id.nextButton);
		nextButton.setOnClickListener(v -> nextButtonClicked());

		recordButton = findViewById(R.id.recordButton);
		recordButton.setOnTouchListener((v, e) -> {
			if (e.getAction() == MotionEvent.ACTION_DOWN){
				v.performClick();
			}
			recordButtonTouch(e);
			return true; // we handle all touch events on this button.
		});

		playButton = findViewById(R.id.playButton);
		playButton.setOnClickListener(v -> playButtonClicked());
		if (_lineCount > 0)
			setActiveLine(_activeLine);
		levelMeter = findViewById(R.id.levelMeter);
	}

	@Override
	protected void onResume() {
		super.onResume();
		startMonitoring();

		AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		wasUsingSpeaker = isSpeakerphoneOn(am);
		if (usingSpeaker) {
			am.setMode(AudioManager.MODE_IN_COMMUNICATION);
			setSpeakerphoneOn(am, true);
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		stopMonitoring();
		stopPlaying();

		BibleLocation location = new BibleLocation();
		location.bookNumber = _bookNum;
		location.chapterNumber = _chapNum;
		location.lineNumber = _activeLine;
		_provider.saveLocation(location);

		AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		if (usingSpeaker) {
			setSpeakerphoneOn(am, false);
			am.setMode(AudioManager.MODE_NORMAL);
		}
	}

	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
		savedInstanceState.putInt(CHAP_NUM, _chapNum);
		savedInstanceState.putInt(BOOK_NUM, _bookNum);
		savedInstanceState.putInt(ACTIVE_LINE, _activeLine);
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
		int lineColor = ContextCompat.getColor(this, R.color.contextTextLine);
		if (lineNo == _activeLine) {
			lineColor = ContextCompat.getColor(this, R.color.activeTextLine);
		} else {
			String recordingFilePath = _provider.getRecordingFilePath(_bookNum, _chapNum, lineNo);
			if (new File(recordingFilePath).exists()) {
				lineColor = ContextCompat.getColor(this, R.color.recordedTextLine);
			} else if (_provider.hasRecording(_bookNum, _chapNum, lineNo)) {
				lineColor = ContextCompat.getColor(this, R.color.recordedElsewhereTextLine);
			}
		}
		lineView.setTextColor(lineColor);
	}

	void setActiveLine(int lineNo) {
		int oldLine = _activeLine;
		_activeLine = lineNo;
		setTextColor(oldLine);
		setTextColor(_activeLine);

		if (_linesView.getParent() instanceof ScrollView scrollView) {
			int[] tops = new int[_linesView.getChildCount() + 1];
			for (int i = 0; i < tops.length - 1; i++) {
				tops[i] = _linesView.getChildAt(i).getTop();
			}
			tops[tops.length - 1] = _linesView.getChildAt(tops.length - 2).getBottom();
			scrollView.scrollTo(0, getNewScrollPosition(scrollView.getScrollY(), scrollView.getHeight(), _activeLine, tops));
		}
		_recordingFilePath = _provider.getRecordingFilePath(_bookNum, _chapNum, _activeLine);
		recordButton.setIsDefault(true);
		nextButton.setIsDefault(false);
		playButton.setIsDefault(false);
		updateDisplayState();
	}

	private void updateDisplayState() {
		boolean recordingExists = new File(_recordingFilePath).exists();
		playButton.setButtonState(recordingExists ? BtnState.Normal : BtnState.Inactive);
		playButton.setEnabled(recordingExists);
	}

	static int getNewScrollPosition(int scrollPos, int height, int newLine, int[] tops) {
		int newScrollPos = scrollPos;
		int bottom = tops[newLine + 1];
		int bottomNext = bottom;
		if (newLine < tops.length - 2) {
			bottomNext = tops[newLine + 2];
		}
		if (bottomNext > scrollPos + height) {
			// Not all the following line is visible.
			// Initial proposal is to scroll so the bottom of the next line is just visible
			newScrollPos = bottomNext - height;
		}
		int top = tops[newLine];
		int topPrev = top;
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
		if (!requestRecordAudioPermission())
			return; // if we don't already have this, we can't record at this point.
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

	void startMonitoring() {
		if (waveRecorder != null)
			waveRecorder.release();
		waveRecorder = new WavAudioRecorder(AudioSource.MIC, 44100, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
		waveRecorder.setMonitorListener(this);
		waveRecorder.startMonitoring();
	}

	private void stopMonitoring() {
		if (waveRecorder != null) {
			waveRecorder.stop();
			waveRecorder.release();
			waveRecorder = null;
		}
	}

	void startWaveRecorder() {
		if (waveRecorder != null)
			waveRecorder.release();
		waveRecorder = new WavAudioRecorder(AudioSource.MIC, 44100, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
		File oldRecording = new File(_recordingFilePath);
		if (oldRecording.exists())
			if (!oldRecording.delete()){
				Log.e("Recorder","Error deleting old recording at" + _recordingFilePath);
			}
		waveRecorder.setOutputFile(_recordingFilePath);
		waveRecorder.prepare();
		waveRecorder.setMonitorListener(this);
		waveRecorder.start();
		recordButton.setWaiting(false);
		startRecordingTime = System.currentTimeMillis();
		synchronized (startingLock) {
			starting = false;
			startingLock.notifyAll();
		}
	}

	void startRecording() {
		stopPlaying();
		recordButton.setButtonState(BtnState.Pushed);
		recordButton.setWaiting(true);
		if (useWaveRecorder) {
			synchronized (startingLock) {
				starting = true; // protects against trying to stop the recording before we finish starting it.
			}
			// Do the initialization of the recorder in another thread so the main one
			//  can color the button red until we really start recording.
			// Wrap waveRecorder initialization logic in a background thread for smoother UI.
			new Thread(this::startWaveRecorder).start();
			return;
		}
		if (recorder != null) {
			recorder.release();
		}
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
			recorder = new MediaRecorder(this);
		} else {
			recorder = createLegacyMediaRecorder();
		}
		recorder.setAudioSource(AudioSource.MIC);
		// Looking for a good combination that produces a usable file.
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
		if (dir != null && !dir.exists())
			if (!dir.mkdirs()){
				Log.e("Recorder","Error creating directory at " + _recordingFilePath);
			}
		recorder.setOutputFile(file.getAbsolutePath());
		try {
			recorder.prepare();
			recorder.start();
			recordButton.setWaiting(false);
			startRecordingTime = System.currentTimeMillis();
		} catch (IOException e) {
			Log.e("Recorder", "Error preparing recorder", e);
		}
	}

	@SuppressWarnings("deprecation")
	private MediaRecorder createLegacyMediaRecorder() {
		return new MediaRecorder();
	}

	// completely arbitrary, especially when we're only asking for one dangerous permission.
	// I just thought it might be useful to have a fairly distinctive number, for debugging.
	private final int RECORD_ACTIVITY_RECORD_PERMISSION = 37;

	// Although the app declares that it needs permission to record audio, because it is considered
	// a dangerous permission the user must grant it explicitly through this procedure from API23 on.
	// In theory, the user could revoke it at any time, so we check it every time we need it. This
	// also means that the request comes up the first time the user clicks the Record button (unless
	// it was granted at application startup), and every subsequent time until it is granted.
	// There doesn't seem to be much alternative.
	private boolean requestRecordAudioPermission() {
		if (ContextCompat.checkSelfPermission(this,
				Manifest.permission.RECORD_AUDIO)
				!= PackageManager.PERMISSION_GRANTED) {

			// Permission has not yet been granted, so ask for it.
			// Should we show an explanation? I don't think it's worth trying to explain why
			// HTA needs this permission; it's too obvious. If we decide to, see similar code in
			// MainActivity.
			ActivityCompat.requestPermissions(this,
					new String[]{Manifest.permission.RECORD_AUDIO},
					RECORD_ACTIVITY_RECORD_PERMISSION);
			// For now, we can't record. Asynchronously, we'll get the result of the request,
			// and if permission is granted, the next time the user tries to record all will be well.
			return false;
		} else {
			// Permission has already been granted
			return true;
		}
	}

	@Override
	public void onRequestPermissionsResult(
			int requestCode,
			@NonNull String[] permissions,
			@NonNull int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		if (requestCode == RECORD_ACTIVITY_RECORD_PERMISSION) {
			if (grantResults.length > 0) {
					// We seem to get spurious callbacks with no results at all, before the user
					// even responds. This might be because multiple events on the record button
					// result in multiple requests. So just ignore any callback with no results.
				if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
					// The user denied permission to record audio. We can't do much useful.
					// This toast just might help.
					Toast.makeText(this, R.string.no_use_without_record, Toast.LENGTH_LONG).show();
				}
			}
		}
	}

	void stopRecording() {
		long beginStop = System.currentTimeMillis();
		synchronized (startingLock) {
			while (starting) {
				// ouch! this will probably be a short-recording problem! The thread that is
				// trying to start the recording hasn't finished! Wait until it does.
				try {
					startingLock.wait(100);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					break;
				}
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
		// Don't just use current time here. It can take ~half a second to get things stopped.
		if (beginStop - startRecordingTime < 500) {
			// Press not long enough; treat as failure.
			new AlertDialog.Builder(this)
					//.setTitle("Too short!")
					.setMessage(R.string.record_too_short)
					.setPositiveButton(android.R.string.ok, (dialog, which) -> {
						// nothing to do
					})
					.setIcon(android.R.drawable.ic_dialog_alert)
					.show();
			File badFile = new File(_recordingFilePath);
			if (badFile.exists()) {
				if (!badFile.delete()){
					Log.e("Recorder","Error deleting bad file at " + _recordingFilePath);
				}
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

	void playButtonClicked() {
		stopPlaying();
		playButton.setPlaying(true);
		playButtonPlayer = new MediaPlayer();
		playButtonPlayer.setOnCompletionListener(this);
		stopMonitoring();
        //noinspection CommentedOutCode
        try {
			// Todo:  file name and location based on book, chapter, segment

//			AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
//			int maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
//			Log.d("Player", "current volume is " + audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
//					+ " of max " + maxVol);
//			audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVol, 0);

			File file = new File(_recordingFilePath);
			playButtonPlayer.setDataSource(file.getAbsolutePath());
			playButtonPlayer.setAudioAttributes(new AudioAttributes.Builder()
					.setUsage(AudioAttributes.USAGE_MEDIA)
					.setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
					.build());
			playButtonPlayer.prepare();
			playButtonPlayer.start();
		} catch (Exception e) {
			Log.e("Player", "Error playing audio", e);
		}
	}

	private void stopPlaying() {
		if (playButtonPlayer != null) {
			try {
				if (playButtonPlayer.isPlaying()) {
					playButtonPlayer.stop();
				}
			} catch (IllegalStateException e) {
				Log.e("Player", "Error stopping audio", e);
			}
			playButtonPlayer.release();
			playButtonPlayer = null;
		}
	}

	@SuppressWarnings("deprecation")
	private boolean isSpeakerphoneOn(AudioManager am) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
			AudioDeviceInfo device = am.getCommunicationDevice();
			return device != null && device.getType() == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER;
		} else {
			return am.isSpeakerphoneOn();
		}
	}

	@SuppressWarnings("deprecation")
	private void setSpeakerphoneOn(AudioManager am, boolean on) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
			if (on) {
				List<AudioDeviceInfo> devices = am.getAvailableCommunicationDevices();
				AudioDeviceInfo speakerDevice = null;
				for (AudioDeviceInfo device : devices) {
					if (device.getType() == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER) {
						speakerDevice = device;
						break;
					}
				}
				if (speakerDevice != null) {
					am.setCommunicationDevice(speakerDevice);
				}
			} else {
				am.clearCommunicationDevice();
			}
		} else {
			am.setSpeakerphoneOn(on);
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
		else if (itemId == R.id.speakers) {
			usingSpeaker = !item.isChecked();
			item.setChecked(usingSpeaker);
			AudioManager am = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
			am.setMode(usingSpeaker ? AudioManager.MODE_IN_COMMUNICATION : AudioManager.MODE_NORMAL);
			setSpeakerphoneOn(am, usingSpeaker);
		}
        return false;
    }

	// Handles click in text line, based on call to setOnClickListener for each child
	@Override
	public void onClick(View view) {
		int newLine = _linesView.indexOfChild(view);
		setActiveLine(newLine);
	}

	@Override
	public void maxLevel(int level) {
		double percentMax = (double)level / 32768;
		// definition of decibels is that 0db is the maximum level possible
		// other levels are negative numbers, 20 times the log of the max level.
		// The effect is that each -6 db corresponds roughly to half the
		// maximum level.
		double db = 20 * Math.log10(percentMax);
		// -48db is a very low level of ambient noise. -36db is considered satisfactory
		// for field recordings. Decided to compromise and make our scale 40db long.
		// This means each 2db corresponds to one LED. A good level of ambient noise is
		// therefore no more than two leds (36db).
		// So, we want to scale so that 0 is -40db or less, 100 is 0db
		int displayLevel = Math.max(0, 100 + (int) Math.round(db * 100 / 40));
		levelMeter.setLevel(displayLevel);
	}

	@Override
	public void onCompletion(MediaPlayer mediaPlayer) {
		stopPlaying();
		playButton.setPlaying(false);
		playButton.setButtonState(BtnState.Normal);
		playButton.setIsDefault(false);
		recordButton.setIsDefault(false);
		nextButton.setIsDefault(true);
		startMonitoring();
	}
}
