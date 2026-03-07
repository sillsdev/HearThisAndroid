package script;

import org.sil.hearthis.ServiceLocator;

import java.io.Serializable;

public class BookInfo implements Serializable {
    public final String  Name;
	public String Abbr;
	public final int ChapterCount;
	public final int BookNumber;

    // This doesn't get serialized (much too expensive, and we only want to have one).
    // When a BookInfo is passed from one activity to another, (the reason to be Serializable)
    // the reconstituted one therefore won't have one.
    // One or other of the activities must ensure in such cases that before the script provider
    // of the BookInfo is needed, the ServiceLocator is ready to provide it.
	private transient IScriptProvider scriptProvider;

	public BookInfo(String projectName, int number, String name, int chapterCount,
                    int[] versesPerChapter, IScriptProvider scriptProvider)	{
		BookNumber = number;
        Name = name;
        ChapterCount = chapterCount;
        // / <summary>
        // / [0] == intro, [1] == chapter 1, etc.
        // / </summary>
        this.scriptProvider = scriptProvider;
	}

    public IScriptProvider getScriptProvider() {
        if (scriptProvider == null)
            scriptProvider = ServiceLocator.getServiceLocator().getScriptProvider();
        return scriptProvider;
    }
}
