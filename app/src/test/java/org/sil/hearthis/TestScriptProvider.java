package org.sil.hearthis;

import java.util.HashMap;
import java.util.Map;

import Script.IScriptProvider;
import Script.BibleLocation;
import Script.ScriptLine;

/**
 * Implements IScriptProvider in a way suitable for unit tests
 * Just make a new ScriptProvider and set any special behavior wanted.
 */
public class TestScriptProvider implements IScriptProvider {
    @Override
    public ScriptLine GetLine(int bookNumber, int chapterNumber, int lineNumber0Based) {
        return null;
    }

    @Override
    public int GetScriptLineCount(int bookNumber, int chapter1Based) {
        return 0;
    }

    Map<Integer, Integer> BookTranslatedLineCounts = new HashMap<Integer, Integer>();

    public void setTranslatedBookCount(int bookNumber, int val) {
        BookTranslatedLineCounts.put(bookNumber, val);
    }

    @Override
    public int GetTranslatedLineCount(int bookNumber) {
        Integer result = BookTranslatedLineCounts.get(bookNumber);
        if (result == null)
            return 0;
        return result;
    }

    @Override
    public int GetTranslatedLineCount(int bookNumberDelegateSafe, int chapterNumber1Based) {
        return 0;
    }

    @Override
    public int GetScriptLineCount(int bookNumber) {
        return 0;
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

    }

    @Override
    public String getRecordingFilePath(int bookNumber, int chapter1Based, int blockNo) {
        return null;
    }

    @Override
    public BibleLocation getLocation() {
        return null;
    }

    @Override
    public void saveLocation(BibleLocation location) {

    }

    @Override
    public String getProjectName() {
        return "Test";
    }
}
