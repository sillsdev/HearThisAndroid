package Script;

import org.json.JSONArray;
import org.json.JSONObject;

public class SampleScriptProvider implements IScriptProvider {

	BibleStats stats;
	
	public SampleScriptProvider()
	{
		stats = new BibleStats();
	}
	
	@Override
	public ScriptLine GetLine(int bookNumber, int chapterNumber,
			int lineNumber0Based) {
		if (lineNumber0Based  ==0)
			return new ScriptLine("Sample Introduction -- this would be replaced with real chapter intro");
		return new ScriptLine("This would be replaced with line " + lineNumber0Based + " of the chapter");
	}

	@Override
	public int GetScriptLineCount(int bookNumber, int chapter1Based) {
		return stats.books.get(bookNumber).versesPerChapter[chapter1Based];
	}

	@Override
	public int GetTranslatedVerseCount(int bookNumberDelegateSafe,
			int chapterNumber1Based) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int GetScriptLineCount(int bookNumber) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void LoadBook(int bookNumber0Based) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getEthnologueCode() {
		// TODO Auto-generated method stub
		return null;
	}

    @Override
    public void noteBlockRecorded(int bookNumber, int chapter1Based, int blockNo) {

    }

    @Override
    public String getRecordingFileName(int bookNumber, int chapter1Based, int blockNo) {
        // Review: this makes it only possible to have one recording with the sample project.
        return "TheOneSampleRecording.mpg4";
    }
}
