package Script;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

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

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public class RealScriptProvider implements IScriptProvider {
	
	String _path;
	List<BookData> Books = new ArrayList<BookData>();
    public String infoFileName = "info.xml";
	class ChapterData {
		public String bookName;
		public int chapterNumber;
		public int lineCount;
		public int translatedCount; // not currently accurate; useful only for empty if 0.
		String[] lines = new String[0];
        String chapFolder = _path + "/" + bookName + "/" + chapterNumber;
        String chapInfoFile = chapFolder + "/" + infoFileName;

        String recordingFileName(int blockNo) {
            return chapFolder + "/" + blockNo + ".mpg4";
        }
		String[] getLines() {
			if (lineCount == 0 || lineCount == lines.length) // none, or already loaded.
				return lines;
            File infoFile = new File(chapInfoFile);
            if (infoFile.exists())
            {
                lines = new String[lineCount];
                try {
                    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                    DocumentBuilder builder = factory.newDocumentBuilder();
                    Document dom = builder.parse(new FileInputStream(chapInfoFile));
                    Element root = dom.getDocumentElement();
                    NodeList source = root.getElementsByTagName("Source");
                    if (source.getLength() == 1) {
                        NodeList lineNodes = source.item(0).getChildNodes();
                        // Enhance: handle pathological case where lineCount recorded in info.txt
                        // does not match number of ScriptLine elements in Source.
                        for(int i = 0; i < lineNodes.getLength(); i++) {
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
                }
                catch(IOException e) {
                    e.printStackTrace();
                }
                catch (ParserConfigurationException e) {
                    e.printStackTrace();
                }
                catch (SAXException e) {
                    e.printStackTrace();
                }
            }

//			for (int i = 0; i < lineCount; i++) {
//				String lineFile = chapFolder + "/" + i + ".txt";
//				File f = new File(lineFile);
//				if (f.exists()) {
//					int length = (int) f.length();
//					byte[] encoded = new byte[length];
//					try {
//						new RandomAccessFile(f, "r").read(encoded);
//					} catch (FileNotFoundException e) {
//						// Can't ever happen (short of other programs interfering) but java insists it be caught
//						e.printStackTrace();
//					} catch (IOException e) {
//						// don't see how (short of hardware failure) but java insists it be caught
//						e.printStackTrace();
//					}
//					//byte[] encoded = f.readAllBytes(); // Java 7
//					try {
//						lines[i] = new String(encoded, "UTF-8");
//					} catch (UnsupportedEncodingException e) {
//						// Don't see how it can ever happen, but java insists it be caught
//						e.printStackTrace();
//					}
//				}
//				else {
//					lines[i] = "";
//				}
//			}
			return lines;
		}

        final String recordingsEltName = "Recordings";
        final String lineNoEltName = "LineNumber";
        // When a line is recorded, we want to copy the content to the block that records what
        // was last recorded.
        void noteLineRecorded(int lineNo)
        {
            try {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document dom = builder.parse(new FileInputStream(chapInfoFile));
                Element root = dom.getDocumentElement();
                NodeList source = root.getElementsByTagName("Source");
                if (source.getLength() == 1) {
                    NodeList lineNodes = source.item(0).getChildNodes();
                    Element line = (Element)lineNodes.item(lineNo);
                    NodeList recordingsNodes = root.getElementsByTagName(recordingsEltName);
                    Element recording;
                    if (recordingsNodes.getLength() != 0) {
                        recording = (Element) recordingsNodes.item(0);
                    }
                    else {
                        recording = dom.createElement(recordingsEltName);
                        root.appendChild(recording);
                    }
                    NodeList recordings = recording.getChildNodes();
                    Node currentRecording = findNodeByEltValue(recordings, lineNoEltName, ""+lineNo);
                    Node newRecording = line.cloneNode(true);
                    if (currentRecording != null) {
                        recording.replaceChild(currentRecording, newRecording);
                    } else {
                        Node insertBefore = findNodeToInsertBefore(recordings, lineNoEltName, lineNo);
                        recording.insertBefore(insertBefore, newRecording);
                    }
                }
            }
            catch(IOException e) {
                e.printStackTrace();
            }
            catch (ParserConfigurationException e) {
                e.printStackTrace();
            }
            catch (SAXException e) {
                e.printStackTrace();
            }
        }

        Element findChildByTagName(Element parent, String name) {
            NodeList list = parent.getElementsByTagName(name);
            if (list.getLength() > 0)
                return (Element)list.item(0);
            return null;
        }

        String findChildContentByTagName(Element parent, String name) {
            Element child = findChildByTagName(parent, name);
            if (child == null)
                return "";
            return child.getNodeValue();
        }

        Node findNodeByEltValue(NodeList nodes, String childName, String val)
        {
            for (int i = 0; i < nodes.getLength(); i++) {
                Element item = (Element)nodes.item(i);
                if (findChildContentByTagName(item, childName) == val)
                    return item;
            }
            return null;
        }

        Node findNodeToInsertBefore(NodeList nodes, String childName, int val) {
            for (int i = 0; i < nodes.getLength(); i++) {
                Element item = (Element) nodes.item(i);
                String thisVal = findChildContentByTagName(item, childName);
                int thisNum = Integer.parseInt(thisVal);
                if (thisNum > val)
                    return item;
            }
            return null;
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

    @Override
    public void noteBlockRecorded(int bookNumber, int chapter1Based, int blockNo) {
        ChapterData chap = GetChapter(bookNumber, chapter1Based);
        if (chap == null)
            return; // or throw??
        chap.noteLineRecorded(blockNo);

    }

    @Override
    public String getRecordingFileName(int bookNumber, int chapter1Based, int blockNo) {
        ChapterData chap = GetChapter(bookNumber, chapter1Based);
        if (chap == null)
            return null; // or throw??
        return chap.recordingFileName(blockNo);
    }

}
