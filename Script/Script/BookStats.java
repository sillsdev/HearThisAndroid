package Script;

public class BookStats {
	public String Name;
	public int ChapterCount;
	public String ThreeLetterAbreviation;
	public int[] VersesPerChapter;
	
	public BookStats(String name, int count, String tla, int[] verses)
	{
		Name = name;
		ChapterCount = count;
		ThreeLetterAbreviation = tla;
		VersesPerChapter = verses;
	}
}
