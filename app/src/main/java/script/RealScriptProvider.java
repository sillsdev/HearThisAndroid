package script;

import android.util.Log;
import org.sil.hearthis.RecordActivity;
import org.sil.hearthis.ServiceLocator;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

public class RealScriptProvider implements IScriptProvider {
    private static final String TAG = "RealScriptProvider";

    final String _path;
    final List<BookData> Books = new ArrayList<>();
    public static final String infoFileName = "info.xml";
    FileSystem getFileSystem() {
        return ServiceLocator.getServiceLocator().getFileSystem();
    }
	class ChapterData {
		public String bookName;
		public int chapterNumber;
		public int lineCount;
		public int translatedCount; // not currently accurate; useful only for empty if 0.
		String[] lines = new String[0];
        String[] recordings = new String[0];
        String getChapFolder() {return _path + "/" + bookName + "/" + chapterNumber;}
        String getChapInfoFile() {return getChapFolder() + "/" + infoFileName;}

        String recordingFilePath(int blockNo) {
            return getChapFolder() + "/" + (blockNo) + (RecordActivity.useWaveRecorder ? ".wav" : ".mp4");
        }
		String[] getLines() {
			if (lineCount == 0 || lines != null && lineCount == lines.length) // none, or already loaded.
            {
                return lines;
            }
            if (getFileSystem().FileExists(getChapInfoFile()))
            {
                lines = new String[lineCount];
                recordings = new String[lineCount];
                try {
                    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                    DocumentBuilder builder = factory.newDocumentBuilder();
                    Document dom = builder.parse(getFileSystem().ReadFile(getChapInfoFile()));
                    Element root = dom.getDocumentElement();
                    NodeList source = root.getElementsByTagName("Source");
                    if (source.getLength() == 1) {
                        NodeList lineNodes = ((Element)source.item(0)).getElementsByTagName("ScriptLine");
                        for(int i = 0; i < lineNodes.getLength() && i < lines.length; i++) {
                            Element line = (Element)lineNodes.item(i);
                            NodeList textNodes = line.getElementsByTagName("Text");
                            if (textNodes.getLength() > 0) {
                                lines[i] = textNodes.item(0).getTextContent();
                            }
                            else {
                                lines[i] = "";
                            }
                        }
                    }
                    NodeList recordingNode = root.getElementsByTagName("Recordings");
                    if (recordingNode.getLength() == 1) {
                        NodeList recordingNodes = ((Element)recordingNode.item(0)).getElementsByTagName("ScriptLine");
                        for(int i = 0; i < recordingNodes.getLength(); i++) {
                            Element line = (Element)recordingNodes.item(i);
                            NodeList textNodes = line.getElementsByTagName("Text");
                            NodeList numberNodes = line.getElementsByTagName("LineNumber");
                            if (numberNodes.getLength() > 0) {
                                int lineNumber = -1;
                                try {
                                    lineNumber = Integer.parseInt(numberNodes.item(0).getTextContent());
                                } catch (NumberFormatException e) {
                                    Log.w(TAG, "Failed to parse line number in " + getChapInfoFile(), e);
                                }
                                if (textNodes.getLength() > 0 && lineNumber >= 1 && lineNumber <= recordings.length) {
                                    recordings[lineNumber - 1] = textNodes.item(0).getTextContent();
                                }
                            }
                        }
                    }
                }
                catch(Exception e) {
                    Log.e(TAG, "Error in getLines for " + getChapInfoFile(), e);
                }
            }
            return lines;
        }

        final String recordingsEltName = "Recordings";

        void noteLineRecorded(int lineNoZeroBased) {
            int lineNo = lineNoZeroBased + 1;
            try {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document dom = builder.parse(getFileSystem().ReadFile(getChapInfoFile()));
                Element root = dom.getDocumentElement();
                NodeList source = root.getElementsByTagName("Source");
                if (source.getLength() == 1) {
                    NodeList lineNodes = ((Element) source.item(0)).getElementsByTagName("ScriptLine");
                    if (lineNoZeroBased < lineNodes.getLength()) {
                        Element line = (Element) lineNodes.item(lineNoZeroBased);
                        NodeList recordingsNodes = root.getElementsByTagName(recordingsEltName);
                        Element recording;
                        if (recordingsNodes.getLength() != 0) {
                            recording = (Element) recordingsNodes.item(0);
                        } else {
                            recording = dom.createElement(recordingsEltName);
                            root.appendChild(recording);
                        }
                        NodeList recordings = recording.getElementsByTagName("ScriptLine");
                        Node currentRecording = findNodeByEltValue(recordings, "" + lineNo);
                        Node newRecording = line.cloneNode(true);
                        if (currentRecording != null) {
                            recording.replaceChild(newRecording, currentRecording);
                        } else {
                            Node insertBefore = findNodeToInsertBefore(recordings, lineNo);
                            recording.insertBefore(newRecording, insertBefore);
                            String infoTxt = getFileSystem().getFile(getInfoTxtPath());
                            String updated = incrementRecordingCount(infoTxt);
                            getFileSystem().putFile(getInfoTxtPath(), updated);
                        }
                    }
                }
                getFileSystem().Delete(getChapInfoFile());
                TransformerFactory transformerFactory = TransformerFactory.newInstance();
                Transformer transformer = transformerFactory.newTransformer();
                DOMSource domSource = new DOMSource(root);
                OutputStream fos = getFileSystem().WriteFile(getChapInfoFile());
                StreamResult streamResult = new StreamResult(fos);
                transformer.transform(domSource, streamResult);
                fos.flush();
                fos.close();
            } catch (Exception e) {
                Log.e(TAG, "Error in noteLineRecorded for " + getChapInfoFile(), e);
            }
        }

        String incrementRecordingCount(String oldInfoTxt) {
            String ls = System.lineSeparator();
            String[] lines = oldInfoTxt.split(ls);
            StringBuilder sb = new StringBuilder();
            for (String line : lines) {
                String[] parts = line.split(";");
                if (parts.length < 2 || !(parts[0].equals(bookName))) {
                    sb.append(line);
                    sb.append(ls);
                    continue;
                }
                String[] counts = parts[1].split(",");
                if (chapterNumber < counts.length) {
                    String myCount = counts[chapterNumber];
                    String[] sourceRec = myCount.split(":");
                    if (sourceRec.length == 2) {
                        try {
                            int recCount = Integer.parseInt(sourceRec[1]);
                            recCount++;
                            sb.append(bookName);
                            sb.append(";");
                            for (int i = 0; i < chapterNumber; i++) {
                                sb.append(counts[i]);
                                sb.append(",");
                            }
                            sb.append(sourceRec[0]);
                            sb.append(":");
                            sb.append(recCount);
                            for (int i = chapterNumber + 1; i < counts.length; i++) {
                                sb.append(",");
                                sb.append(counts[i]);
                            }
                            sb.append(ls);
                            continue;
                        } catch (NumberFormatException e) {
                            Log.e(TAG, "Failed to parse recording count in info.txt: " + sourceRec[1], e);
                        }
                    }
                }
                sb.append(line);
                sb.append(ls);
            }
            return sb.toString();
        }

        Element findChildByTagName(Element parent) {
            NodeList list = parent.getElementsByTagName("LineNumber");
            if (list.getLength() > 0)
                return (Element) list.item(0);
            return null;
        }

        String findChildContentByTagName(Element parent) {
            Element child = findChildByTagName(parent);
            if (child == null)
                return "";
            return child.getTextContent();
        }

        Node findNodeByEltValue(NodeList nodes, String val) {
            for (int i = 0; i < nodes.getLength(); i++) {
                Element item = (Element) nodes.item(i);
                if (findChildContentByTagName(item).equals(val))
                    return item;
            }
            return null;
        }

        Node findNodeToInsertBefore(NodeList nodes, int val) {
            for (int i = 0; i < nodes.getLength(); i++) {
                Element item = (Element) nodes.item(i);
                String thisVal = findChildContentByTagName(item);
                if (!thisVal.isEmpty()) {
                    try {
                        int thisNum = Integer.parseInt(thisVal);
                        if (thisNum > val)
                            return item;
                    } catch (NumberFormatException e) {
                        Log.v(TAG, "Non-numeric LineNumber value: " + thisVal);
                    }
                }
            }
            return null;
        }

        public boolean hasRecording(int blockNo) {
            getLines(); // Make sure we've read the data file if any
            if (recordings == null)
                return false;
            if (blockNo >= recordings.length)
                return false;
            String recording = recordings[blockNo];
            return recording != null && !recording.isEmpty();
        }
    }
	class BookData {
		public String name;
		public final List<ChapterData> chapters = new ArrayList<>();
	}
	public RealScriptProvider(String path) {
		_path = path;
		try	{
            if (!getFileSystem().FileExists(getInfoTxtPath()))
                return;
            BufferedReader buf = new BufferedReader(new InputStreamReader(getFileSystem().ReadFile(getInfoTxtPath()), StandardCharsets.UTF_8));
			int ibook = 0;
			for (String line = buf.readLine(); line != null; ibook++, line = buf.readLine()) {
				String[] parts = line.split(";");
				BookData bookdata = new BookData();
				Books.add(bookdata);
				if (parts.length > 0)
					bookdata.name = parts[0];
				if (parts.length > 1) {
					String[] chapParts = parts[1].split(",");
					for (String chapSrc : chapParts) {
						String[] counts = chapSrc.split(":");
						ChapterData cd = new ChapterData();
						cd.chapterNumber = bookdata.chapters.size();
						bookdata.chapters.add(cd);
						cd.bookName = bookdata.name;
                        if (counts.length >= 2) {
                            try {
						        cd.lineCount = Integer.parseInt(counts[0]);
						        cd.translatedCount = Integer.parseInt(counts[1]);
                            } catch (NumberFormatException e) {
                                Log.e(TAG, "Failed to parse counts in info.txt: " + chapSrc, e);
                            }
                        }
					}
				}
			}
            buf.close();
		} catch (Exception ex) {
            Log.e(TAG, "Error initializing RealScriptProvider for path: " + path, ex);
		}
	}

    private String getInfoTxtPath() {
        return _path + "/info.txt";
    }

    @Override
	public ScriptLine GetLine(int bookNumber, int chapter1Based,
			int lineNumber0Based) {
		ChapterData chapter = GetChapter(bookNumber, chapter1Based);
		if (chapter == null)
			return new ScriptLine("");
        String[] lines = chapter.getLines();
        if (lines == null || lineNumber0Based >= lines.length)
            return new ScriptLine("");
        return new ScriptLine(lines[lineNumber0Based]);
	}

	ChapterData GetChapter(int bookNumber, int chapter1Based) {
        if (bookNumber < 0 || bookNumber >= Books.size())
            return null;
		BookData book = Books.get(bookNumber);
		if (chapter1Based < 0 || chapter1Based >= book.chapters.size())
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
	public int GetTranslatedLineCount(int bookNumber, int chapter1Based) {
		ChapterData chap = GetChapter(bookNumber, chapter1Based);
		if (chap == null)
			return 0;
		return chap.translatedCount;
	}

    @Override
    public int GetTranslatedLineCount(int bookNumber) {
        if (bookNumber < 0 || bookNumber >= Books.size())
            return 0;
        BookData book = Books.get(bookNumber);
        int total = 0;
        for (int i = 0; i < book.chapters.size(); i++)
            total += GetTranslatedLineCount(bookNumber, i);
        return total;
    }

	@Override
	public int GetScriptLineCount(int bookNumber) {
        if (bookNumber < 0 || bookNumber >= Books.size())
            return 0;
		BookData book = Books.get(bookNumber);
		int total = 0;
		for (int i = 0; i < book.chapters.size(); i++)
			total += GetScriptLineCount(bookNumber, i);
		return total;
	}

	@Override
	public void LoadBook(int bookNumber0Based) {
	}

	@Override
	public String getEthnologueCode() {
		return null;
	}

    @Override
    public void noteBlockRecorded(int bookNumber, int chapter1Based, int blockNo) {
        ChapterData chap = GetChapter(bookNumber, chapter1Based);
        if (chap == null)
            return;
        chap.noteLineRecorded(blockNo);
    }

    @Override
    public String getRecordingFilePath(int bookNumber, int chapter1Based, int blockNo) {
        ChapterData chap = GetChapter(bookNumber, chapter1Based);
        if (chap == null)
            return null;
        return chap.recordingFilePath(blockNo);
    }

    String getStatusPath() {
        return _path+ "/status.txt";
    }

    @Override
    public BibleLocation getLocation() {
        String statusPath = getStatusPath();
        if (!getFileSystem().FileExists(statusPath))
            return null;
        String content;
        try {
            content = getFileSystem().getFile(statusPath);
        } catch (IOException e) {
            Log.e(TAG, "Failed to read status file: " + statusPath, e);
            return null;
        }
        String[] parts = content.split(";");
        if (parts.length != 3)
            return null;
        BibleLocation result = new BibleLocation();
        try {
            result.bookNumber = Integer.parseInt(parts[0]);
            result.chapterNumber = Integer.parseInt(parts[1]);
            result.lineNumber = Integer.parseInt(parts[2]);
        } catch (NumberFormatException e) {
            Log.e(TAG, "Failed to parse status file content: " + content, e);
            return null;
        }
        return result;
    }

    @Override
    public void saveLocation(BibleLocation location) {
        try {
            // Use Locale.US to ensure numbers are formatted as standard digits (0-9)
            String content = String.format(java.util.Locale.US, "%d;%d;%d",
                    location.bookNumber, location.chapterNumber, location.lineNumber);

            getFileSystem().putFile(getStatusPath(), content);
        } catch (IOException e) {
            Log.e(TAG, "Failed to save location to " + getStatusPath(), e);
        }
    }

    @Override
    public String getProjectName() {
        int slashIndex = _path.lastIndexOf('/');
        if (slashIndex < 0)
            return _path;
        return _path.substring(slashIndex + 1);
    }

    @Override
    public boolean hasRecording(int bookNumber, int chapter1Based, int blockNo) {
        ChapterData chap = GetChapter(bookNumber, chapter1Based);
        if (chap == null)
            return false;
        return chap.hasRecording(blockNo);
    }

    public String getProjectName(int bookNumber) {
        if (bookNumber < 0 || bookNumber >= Books.size())
            return "";
        return Books.get(bookNumber).name;
    }
}
