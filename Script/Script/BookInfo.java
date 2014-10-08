package Script;

import java.io.Serializable;

public class BookInfo implements Serializable {
	private String _projectName;
	public String Name;
	public String Abbr;
	public int ChapterCount;
	public int BookNumber;

	public IScriptProvider getScriptProvider() {
		return s_theOneScriptProvider;
	}

	// / <summary>
	// / [0] == intro, [1] == chapter 1, etc.
	// / </summary>
	private int[] _versesPerChapter;
	// private IScriptProvider _scriptProvider;

	// We'd like an instance variable IScriptProvider.
	// But, BookInfo needs to be serializable, and a ScriptProvider
	// typically has a lot of data (the whole content of Scripture)
	// and is much too expensive to serialize constantly.
	// Eventually we may implement some trick with a serializable key into a map
	// with some mechanism to garbage-collect them when no longer needed.
	// For now, we only ever have one, so just store it here.
	static IScriptProvider s_theOneScriptProvider;

	BookInfo(String projectName, int number, String name, int chapterCount,
			int[] versesPerChapter, IScriptProvider scriptProvider)

	{
		BookNumber = number;
		_projectName = projectName;
		Name = name;
		ChapterCount = chapterCount;
		_versesPerChapter = versesPerChapter;
		if (s_theOneScriptProvider != null
				&& s_theOneScriptProvider != scriptProvider)
			throw new UnsupportedOperationException(
					"need to implement support for multiple script providers");
		s_theOneScriptProvider = scriptProvider;
	}
}
