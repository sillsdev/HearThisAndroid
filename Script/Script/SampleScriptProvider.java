package Script;

public class SampleScriptProvider implements IScriptProvider {

	BibleStats _stats;
	
	public SampleScriptProvider() {
		_stats = new BibleStats();
	}
	
	@Override
	public ScriptLine GetLine(int bookNumber, int chapterNumber,
			int lineNumber0Based) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int GetScriptLineCount(int bookNumber, int chapter1Based) {
		// TODO Auto-generated method stub
		return 0;
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

}
