package Script;

import java.util.ArrayList;
import java.util.List;

public class Project {
	
	IScriptProvider _scriptProvider;
	
	public BibleStats Statistics;
	public List<BookInfo> Books;

	public Project(String name, IScriptProvider scriptProvider) {
		Statistics = new BibleStats();
		Books = new ArrayList<BookInfo>();
		_scriptProvider = scriptProvider;
		
		for (int bookNumber = 0; bookNumber < Statistics.Books.size(); bookNumber++) {
			BookStats stats = Statistics.Books.get(bookNumber);
			BookInfo book = new BookInfo(name, bookNumber, stats.Name, stats.ChapterCount, stats.VersesPerChapter,
					_scriptProvider);
			Books.add(book);
		}
		
	}
}
