package org.sil.hearthis;

import android.app.Service;

import junit.framework.Assert;

import Script.FileSystem;
import Script.RealScriptProvider;
import Script.TestFileSystem;

/**
 * Created by Thomson on 3/27/2016.
 */
public class RealScriptProviderTest {
    TestFileSystem tfs;
    FileSystem fs;
    private void makeDfaultFs() {
        tfs = new TestFileSystem(); // has a default info.txt
        fs = new FileSystem(tfs);
        ServiceLocator.getServiceLocator().setFileSystem(fs);
        tfs.project = "test";
        tfs.simulateFile(tfs.project + "/info.txt", tfs.getDefaultInfoTxtContent());
    }

    @org.junit.Test
    public void getBasicDataFromInfoTxt() {
        makeDfaultFs();
        RealScriptProvider sp = new RealScriptProvider(tfs.project);
        ServiceLocator.getServiceLocator().setScriptProvider(sp);
        Assert.assertEquals(0, sp.GetScriptLineCount(0));
        Assert.assertEquals(38, sp.GetScriptLineCount(39));
        Assert.assertEquals(12, sp.GetScriptLineCount(39, 1));
    }

    @org.junit.Test
    public void getBasicLineData() {
        makeDfaultFs();
        tfs.MakeChapterContent("Matthew", 1, new String[]{"first line", "second line", "third line"}, null);
        RealScriptProvider sp = new RealScriptProvider(tfs.project);
        ServiceLocator.getServiceLocator().setScriptProvider(sp);
        Assert.assertEquals("first line", sp.GetLine(39, 1, 0).Text);
        Assert.assertEquals("second line", sp.GetLine(39, 1, 1).Text);
        Assert.assertEquals("third line", sp.GetLine(39, 1, 2).Text);
    }

    @org.junit.Test
    public void getRecordingExists() {
        makeDfaultFs();
        tfs.MakeChapterContent("Matthew", 1, new String[]{"first line", "second line", "third line"},
                new String[] {null, "second line", null});
        RealScriptProvider sp = new RealScriptProvider(tfs.project);
        ServiceLocator.getServiceLocator().setScriptProvider(sp);
        Assert.assertEquals(false, sp.hasRecording(39, 1, 0));
        Assert.assertEquals(true, sp.hasRecording(39, 1, 1));
        Assert.assertEquals(false, sp.hasRecording(39, 1, 2));
    }
}
