package Script;

import java.io.Serializable;

public class BookInfo implements Serializable {
    private String _projectName;
    public String Name;
    public int ChapterCount;
    public int BookNumber;
   
    /// <summary>
    /// [0] == intro, [1] == chapter 1, etc.
    /// </summary>
    private int[] _versesPerChapter;
    //private IScriptProvider _scriptProvider;

	BookInfo(String projectName, int number, String name, int chapterCount, int[] versesPerChapter
			//,IScriptProvider scriptProvider
			)
			
	{
        BookNumber = number;
        _projectName = projectName;
        Name = name;
        ChapterCount = chapterCount;
        _versesPerChapter = versesPerChapter;
        //_scriptProvider = scriptProvider;
		
	}
}
