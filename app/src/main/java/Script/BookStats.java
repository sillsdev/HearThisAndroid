package Script;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

public class BookStats
{
	public String name;
	public int chapterCount;
	public String threeLetterAbbreviation;
	public int[] versesPerChapter;
	
	public BookStats(String name, int count, String tla, int[] verses)
	{
		this.name = name;
		chapterCount = count;
		threeLetterAbbreviation = tla;
		versesPerChapter = verses;
	}
}
