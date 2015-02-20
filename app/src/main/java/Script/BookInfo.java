package Script;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.Serializable;

public class BookInfo implements Serializable
{
	private String projectName;
	private String name;
	private String abbr;
	private int chapterCount;
	private int bookNumber;

	public IScriptProvider getScriptProvider()
	{
		return s_theOneScriptProvider;
	}

	// / <summary>
	// / [0] == intro, [1] == chapter 1, etc.
	// / </summary>
	private int[] versesPerChapter;
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
		bookNumber = number;
		this.projectName = projectName;
		this.name = name;
		this.chapterCount = chapterCount;
		this.versesPerChapter = versesPerChapter;
//		if (s_theOneScriptProvider != null
//				&& s_theOneScriptProvider != scriptProvider)
//			throw new UnsupportedOperationException(
//					"need to implement support for multiple script providers");
		s_theOneScriptProvider = scriptProvider;
	}

	public String getName()
	{
		return name;
	}

	public String getAbbr()
	{
		return abbr;
	}

	public int getChapterCount()
	{
		return chapterCount;
	}

	public int getBookNumber()
	{
		return bookNumber;
	}

	public void setAbbr(String abbr)
	{
		this.abbr = abbr;
	}
}
