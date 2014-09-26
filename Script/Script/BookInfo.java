package Script;

public class BookInfo {
    private String _projectName;
    private String _name;
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
        _name = name;
        ChapterCount = chapterCount;
        _versesPerChapter = versesPerChapter;
        //_scriptProvider = scriptProvider;
		
	}
}
