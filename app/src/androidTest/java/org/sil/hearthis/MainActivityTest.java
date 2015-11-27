package org.sil.hearthis;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.*;
//import static org.mockito.Mockito.*;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.test.ActivityInstrumentationTestCase2;
import android.test.ActivityUnitTestCase;
import android.test.AndroidTestCase;

import org.junit.Test;
import org.junit.runner.RunWith;

import Script.FileSystem;
import Script.TestFileSystem;
//import Script.TestFileSystem;
//import org.mockito.Mock;
//import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.*;

/**
 * Created by Thomson on 5/9/2015.
 */
//@RunWith(MockitoJUnitRunner.class)
//public class MainActivityTest  extends ActivityInstrumentationTestCase2<MainActivity> {
public class MainActivityTest  extends ActivityUnitTestCase<MainActivity> {
    MainActivity mainActivity;

//    @Mock
//    Context mMockContext;

    public MainActivityTest() {
        super(MainActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        //setApplication(new org.sil.hearthis.);
    }

    @Test
    //@UiThreadTest
    public void testOnCreate() throws Exception {
        // Simulates no files at all installed
        ServiceLocator.getServiceLocator().setFileSystem(new FileSystem(new TestFileSystem()));
//        Intent mLaunchIntent = new Intent(mMockContext, MainActivity.class);
//        mainActivity = startActivity(mLaunchIntent, null, null);

        //final Intent anyIntent = getStartedActivityIntent();
        //mainActivity = startActivity(anyIntent, null, null);
        getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                mainActivity = startActivity(new Intent(Intent.ACTION_MAIN), null, null);
            }
        });       // Should not launch other activity, just stay up
        //assertNull(anyIntent);
        getInstrumentation().
    }
}