package Script;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

public class RealScriptProvider implements IScriptProvider {
	
	String _path;
	List<BookData> Books = new ArrayList<BookData>();
	class ChapterData {
		public String bookName;
		public int chapterNumber;
		public int lineCount;
		public int translatedCount; // not currently accurate; useful only for empty if 0.
		String[] lines = new String[0];
		String[] getLines() {
			if (lineCount == 0 || lineCount == lines.length) // none, or already loaded.
				return lines;
			//  ToDo: look for lines files in appropriate folder.
			String chapFolder = _path + "/" + bookName + "/" + chapterNumber;
			lines = new String[lineCount];
			for (int i = 0; i < lineCount; i++) {
				String lineFile = chapFolder + "/" + i + ".txt";
				File f = new File(lineFile);
				if (f.exists()) {
					int length = (int) f.length();
					byte[] encoded = new byte[length];
					try {
						new RandomAccessFile(f, "r").read(encoded);
					} catch (FileNotFoundException e) {
						// Can't ever happen (short of other programs interfering) but java insists it be caught
						e.printStackTrace();
					} catch (IOException e) {
						// don't see how (short of hardware failure) but java insists it be caught
						e.printStackTrace();
					}
					//byte[] encoded = f.readAllBytes(); // Java 7
					try {
						lines[i] = new String(encoded, "UTF-8");
					} catch (UnsupportedEncodingException e) {
						// Don't see how it can ever happen, but java insists it be caught
						e.printStackTrace();
					}
				}
				else {
					lines[i] = "";
				}
			}
			return lines;
		}
	}
	class BookData {
		public String name;
		public List<ChapterData> chapters = new ArrayList<ChapterData>();
	}
	public RealScriptProvider(String path) {
		_path = path;
		try	{
			String infoPath = path + "/info.txt";
			BufferedReader buf = new BufferedReader(new InputStreamReader(new FileInputStream(infoPath),"UTF-8"));
			int ibook = 0;
			for (String line = buf.readLine(); line != null; ibook++, line = buf.readLine()) {
				String[] parts = line.split(";");
				BookData bookdata = new BookData();
				Books.add(bookdata);
				if (parts.length > 0)
					bookdata.name = parts[0]; // else get from stats??
				if (parts.length > 1) {
					String[] chapParts = parts[1].split(",");
					for (String chapSrc : chapParts) {
						String[] counts = chapSrc.split(":");
						ChapterData cd = new ChapterData();
						cd.chapterNumber = bookdata.chapters.size(); // before add!
						bookdata.chapters.add(cd);
						cd.bookName = bookdata.name;
						cd.lineCount = Integer.parseInt(counts[0]);
						cd.translatedCount = Integer.parseInt(counts[1]);
					}
				}
			}
		} catch (IOException ex) { // most likely file not found
		}
	}

	@Override
	public ScriptLine GetLine(int bookNumber, int chapter1Based,
			int lineNumber0Based) {
		ChapterData chapter = GetChapter(bookNumber, chapter1Based);
		if (chapter == null)
			return new ScriptLine("");
		return new ScriptLine(chapter.getLines()[lineNumber0Based]);
	}
	
	ChapterData GetChapter(int bookNumber, int chapter1Based) {
		BookData book = Books.get(bookNumber);
		if (chapter1Based >= book.chapters.size())
			return null;
		return book.chapters.get(chapter1Based);
	}

	@Override
	public int GetScriptLineCount(int bookNumber, int chapter1Based) {
		ChapterData chap = GetChapter(bookNumber, chapter1Based);
		if (chap == null)
			return 0;
		return chap.lineCount;
	}

	@Override
	public int GetTranslatedVerseCount(int bookNumber, int chapter1Based) {
		ChapterData chap = GetChapter(bookNumber, chapter1Based);
		if (chap == null)
			return 0;
		return chap.translatedCount;
	}

	@Override
	public int GetScriptLineCount(int bookNumber) {
		BookData book = Books.get(bookNumber);
		int total = 0;
		for (int i = 0; i < book.chapters.size(); i++)
			total += GetScriptLineCount(bookNumber, i);
		return total;
	}

	@Override
	public void LoadBook(int bookNumber0Based) {
		// nothing to do here in this version.
		
	}

	@Override
	public String getEthnologueCode() {
		// TODO need to enhance creation and reading in info.txt to handle this if we need it.
		return null;
	}

}
