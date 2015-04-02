package Script;

import org.sil.hearthis.ServiceLocator;

import java.io.IOException;
import java.io.Serializable;

public class BookInfo implements Serializable {
	private String _projectName;
	public String Name;
	public String Abbr;
	public int ChapterCount;
	public int BookNumber;

	// / <summary>
	// / [0] == intro, [1] == chapter 1, etc.
	// / </summary>
	private int[] _versesPerChapter;
    // This doesn't get serialized (much too expensive, and we only want to have one).
    // When a BookInfo is passed from one activity to another, (the reason to be Serializable)
    // the reconstituted one therefore won't have one.
    // One or other of the activities must ensure in such cases that before the script provider
    // of the BookInfo is needed, the ServiceLocator is ready to provide it.
	private transient IScriptProvider scriptProvider;

	public BookInfo(String projectName, int number, String name, int chapterCount,
                    int[] versesPerChapter, IScriptProvider scriptProvider)

	{
		BookNumber = number;
		_projectName = projectName;
		Name = name;
		ChapterCount = chapterCount;
		_versesPerChapter = versesPerChapter;
		if (scriptProvider != null
				&& scriptProvider != scriptProvider)
			throw new UnsupportedOperationException(
					"need to implement support for multiple script providers");
		this.scriptProvider = scriptProvider;
	}

    public IScriptProvider getScriptProvider() {
        if (scriptProvider == null)
            scriptProvider = ServiceLocator.getServiceLocator().getScriptProvider();
        return scriptProvider;
    }
}
