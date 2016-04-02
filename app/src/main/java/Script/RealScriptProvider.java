package Script;

import org.sil.hearthis.RecordActivity;
import org.sil.hearthis.ServiceLocator;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

public class RealScriptProvider implements IScriptProvider {
	
	String _path;
	List<BookData> Books = new ArrayList<BookData>();
    public static String infoFileName = "info.xml";
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
            // Enhance: instead of assuming line number of nth block is blockNo,
            // Extract the <ScriptLine> from the chapter info file as in noteLineRecorded,
            // and use its LineNumber element.
            return getChapFolder() + "/" + (blockNo) + (RecordActivity.useWaveRecorder ? ".wav" : ".mp4");
        }
		String[] getLines() {
			if (lineCount == 0 || lineCount == lines.length) // none, or already loaded.
				return lines;
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
                        // getChildren does not work because it also gets various text (white space) nodes.
                        NodeList lineNodes = ((Element)source.item(0)).getElementsByTagName("ScriptLine");
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
                    NodeList recordingNode = root.getElementsByTagName("Recordings");
                    if (recordingNode.getLength() == 1) {
                        // getChildren does not work because it also gets various text (white space) nodes.
                        NodeList recordingNodes = ((Element)recordingNode.item(0)).getElementsByTagName("ScriptLine");
                        for(int i = 0; i < recordingNodes.getLength(); i++) {
                            Element line = (Element)recordingNodes.item(i);
                            NodeList textNodes = line.getElementsByTagName("Text");
                            NodeList numberNodes = line.getElementsByTagName("LineNumber");
                            int lineNumber = -1;
                            try {
                                lineNumber = Integer.parseInt(numberNodes.item(0).getTextContent());
                            } catch (NumberFormatException e) {
                                e.printStackTrace();
                            }
                            if (textNodes.getLength() > 0 && lineNumber >= 1 && lineNumber <= recordings.length) {
                                recordings[lineNumber - 1] = textNodes.item(0).getTextContent();
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
                    Element line = (Element) lineNodes.item(lineNoZeroBased);
                    NodeList recordingsNodes = root.getElementsByTagName(recordingsEltName);
                    Element recording;
                    if (recordingsNodes.getLength() != 0) {
                        recording = (Element) recordingsNodes.item(0);
                    } else {
                        recording = dom.createElement(recordingsEltName);
                        root.appendChild(recording);
                    }
                    NodeList recordings = ((Element) recording).getElementsByTagName("ScriptLine");
                    Node currentRecording = findNodeByEltValue(recordings, lineNoEltName, "" + lineNo);
                    Node newRecording = line.cloneNode(true);
                    if (currentRecording != null) {
                        recording.replaceChild(newRecording, currentRecording);
                    } else {
                        Node insertBefore = findNodeToInsertBefore(recordings, lineNoEltName, lineNo);
                        recording.insertBefore(newRecording, insertBefore); // insertBefore may be null, means at end.
                        String infoTxt = getFileSystem().getFile(getInfoTxtPath());
                        String updated = incrementRecordingCount(infoTxt);
                        getFileSystem().putFile(getInfoTxtPath(), updated);
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
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ParserConfigurationException e) {
                e.printStackTrace();
            } catch (SAXException e) {
                e.printStackTrace();
            } catch (DOMException e) {
                e.printStackTrace();
            } catch (TransformerConfigurationException e) {
                e.printStackTrace();
            } catch (TransformerException e) {
                e.printStackTrace();
            }
        }

        String incrementRecordingCount(String oldInfoTxt) {
            String ls = System.getProperty("line.separator");
            String[] lines = oldInfoTxt.split(ls);
            StringBuilder sb = new StringBuilder();
            for (String line : lines) {
                String[] parts = line.split(";");
                if (!(parts[0].equals(bookName))) {
                    sb.append(line);
                    sb.append(ls);
                    continue;
                }
                String[] counts = parts[1].split(",");
                String myCount = counts[chapterNumber];
                String[] sourceRec = myCount.split(":");
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
            }
            return sb.toString();
        }

        Element findChildByTagName(Element parent, String name) {
            NodeList list = parent.getElementsByTagName(name);
            if (list.getLength() > 0)
                return (Element) list.item(0);
            return null;
        }

        String findChildContentByTagName(Element parent, String name) {
            Element child = findChildByTagName(parent, name);
            if (child == null)
                return "";
            return child.getTextContent();
        }

        Node findNodeByEltValue(NodeList nodes, String childName, String val) {
            for (int i = 0; i < nodes.getLength(); i++) {
                Element item = (Element) nodes.item(i);
                if (findChildContentByTagName(item, childName).equals(val))
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

        public boolean hasRecording(int blockNo) {
            getLines(); // Make sure we've read the data file if any
            if (recordings == null)
                return false;
            if (blockNo >= recordings.length)
                return false;
            String recording = recordings[blockNo];
            return recording != null && recording.length() > 0;
        }
    }
	class BookData {
		public String name;
		public List<ChapterData> chapters = new ArrayList<ChapterData>();
	}
	public RealScriptProvider(String path) {
		_path = path;
		try	{
            if (!getFileSystem().FileExists(getInfoTxtPath()))
                return; // no info about any books, leave the collection empty.
            BufferedReader buf = new BufferedReader(new InputStreamReader(getFileSystem().ReadFile(getInfoTxtPath()),"UTF-8"));
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

    private String getInfoTxtPath() {
        return _path + "/info.txt";
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
	public int GetTranslatedLineCount(int bookNumber, int chapter1Based) {
		ChapterData chap = GetChapter(bookNumber, chapter1Based);
		if (chap == null)
			return 0;
		return chap.translatedCount;
	}

    @Override
    public int GetTranslatedLineCount(int bookNumber) {
        BookData book = Books.get(bookNumber);
        int total = 0;
        for (int i = 0; i < book.chapters.size(); i++)
            total += GetTranslatedLineCount(bookNumber, i);
        return total;
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
    public String getRecordingFilePath(int bookNumber, int chapter1Based, int blockNo) {
        ChapterData chap = GetChapter(bookNumber, chapter1Based);
        if (chap == null)
            return null; // or throw??
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
            return null;
        }
        String[] parts = content.split(";");
        if (parts.length != 3)
            return null;
        BibleLocation result = new BibleLocation();
        result.bookNumber = Integer.parseInt(parts[0]);
        result.chapterNumber = Integer.parseInt(parts[1]);
        result.lineNumber = Integer.parseInt(parts[2]);
        return result;
    }

    @Override
    public void saveLocation(BibleLocation location) {
        try {
            getFileSystem().putFile(getStatusPath(),
                    String.format("%d;%d;%d", location.bookNumber, location.chapterNumber, location.lineNumber));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getProjectName() {
        int slashIndex = _path.lastIndexOf('/');
        if (slashIndex < 0)
            return _path;
        return _path.substring(slashIndex + 1, _path.length());
    }

    @Override
    public boolean hasRecording(int bookNumber, int chapter1Based, int blockNo) {
        ChapterData chap = GetChapter(bookNumber, chapter1Based);
        if (chap == null)
            return false;
        return chap.hasRecording(blockNo);
    }

}
