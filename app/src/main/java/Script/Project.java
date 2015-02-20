package Script;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class Project
{
	
	IScriptProvider scriptProvider;
	
	BibleStats statistics;
	ArrayList<BookInfo> books;
	String projectName;

	public Project(String name, IScriptProvider scriptProvider)
	{
		projectName = name;
		statistics = new BibleStats();
		books = new ArrayList<BookInfo>();
		this.scriptProvider = scriptProvider;
		
		for (int bookNumber = 0; bookNumber < statistics.books.size(); bookNumber++)
		{
			BookStats stats = statistics.books.get(bookNumber);
			BookInfo book = new BookInfo(name, bookNumber, stats.name, stats.chapterCount, stats.versesPerChapter,
					this.scriptProvider);
			book.setAbbr(stats.threeLetterAbbreviation);
			books.add(book);
		}
	}

	public ArrayList<BookInfo> getBooks()
	{
		return books;
	}
}
