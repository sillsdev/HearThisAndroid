package script;

import java.util.ArrayList;
import java.util.List;

public class Project {

	final IScriptProvider _scriptProvider;
	
	public final BibleStats Statistics;
	public final List<BookInfo> Books;

	public Project(String name, IScriptProvider scriptProvider) {
		Statistics = new BibleStats();
		Books = new ArrayList<>();
		_scriptProvider = scriptProvider;
		
		for (int bookNumber = 0; bookNumber < Statistics.Books.size(); bookNumber++) {
			BookStats stats = Statistics.Books.get(bookNumber);
			BookInfo book = new BookInfo(name, bookNumber, stats.Name(), stats.ChapterCount(), stats.VersesPerChapter(),
					_scriptProvider);
			book.Abbr = stats.ThreeLetterAbreviation();
			Books.add(book);
		}
		
	}
}
