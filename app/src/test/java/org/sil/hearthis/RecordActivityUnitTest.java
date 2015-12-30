package org.sil.hearthis;

import junit.framework.Assert;

/**
 * Created by Thomson on 12/30/2015.
 * Tests those aspects of RecordActivity that can be done without instrumentation
 * (that is, without creating an instance, which is more-or-less impossible to do
 * for an activity in a unit test)
 */
public class RecordActivityUnitTest {
    @org.junit.Test
    public void SelectOnly_AlreadyVisible_DoesNotScroll()
    {
        int[] tops = {0,10};
        Assert.assertEquals(0, RecordActivity.getNewScrollPosition(0, 50, 0, tops));
    }

    @org.junit.Test
    public void SelectLast_LastNotVisible_Scrolls()
    {
        int[] tops = {0, 10, 25};
        Assert.assertEquals(5, RecordActivity.getNewScrollPosition(0, 20, 1, tops));
    }

    @org.junit.Test
    public void SelectSecondLastOfMany_LastNotVisible_Scrolls()
    {
        int[] tops = {0, 20, 35, 45, 60}; // lines 20, 15, 10, 15
        // select third line; want to see all of 2nd through 4th, which is just possible
        // viewport needs to end up showing those 40 pixels, plus 5 above to make
        // smallest viable motion, so last 45 pixels.
        Assert.assertEquals(15, RecordActivity.getNewScrollPosition(2, 45, 2, tops));
    }

    @org.junit.Test
    public void SelectSecond_FirstNotVisible_Scrolls()
    {
        int[] tops = {0, 20, 35, 45, 60}; // lines 20, 15, 10, 15
        // select third line; want to see all of 2nd through 4th, which is just possible
        // viewport needs to end up showing those 40 pixels, plus 5 above to make
        // smallest viable motion, so last 45 pixels; however we allow 3 extra to
        // make sure last line is fully visible.
        Assert.assertEquals(0, RecordActivity.getNewScrollPosition(10, 45, 1, tops));
    }

    @org.junit.Test
    public void SelectThird_WindowTooSmallForThree_ShowsPrevAndCurrent()
    {
        int[] tops = {0, 20, 35, 45, 60}; // lines 20, 15, 10, 15
        // select third line; want to see all of 2nd through 4th, which is just possible
        // viewport needs to end up showing those 40 pixels, plus 5 above to make
        // smallest viable motion, so last 45 pixels; however we allow 3 extra to
        // make sure last line is fully visible.
        Assert.assertEquals(20, RecordActivity.getNewScrollPosition(10, 30, 2, tops));
    }

    @org.junit.Test
    public void SelectThird_WindowTooSmallForTwo_ShowsCurrentAndPartOfPrev()
    {
        int[] tops = {0, 20, 35, 45, 60}; // lines 20, 15, 10, 15
        // select third line; want to see all of 2nd through 4th, which is just possible
        // viewport needs to end up showing those 40 pixels, plus 5 above to make
        // smallest viable motion, so last 45 pixels; however we allow 3 extra to
        // make sure last line is fully visible.
        Assert.assertEquals(25, RecordActivity.getNewScrollPosition(10, 20, 2, tops));
    }

    @org.junit.Test
    public void SelectThird_WindowTooSmallForOne_ShowsTopOfCurrent()
    {
        int[] tops = {0, 20, 35, 45, 60}; // lines 20, 15, 10, 15
        // select third line; want to see all of 2nd through 4th, which is just possible
        // viewport needs to end up showing those 40 pixels, plus 5 above to make
        // smallest viable motion, so last 45 pixels; however we allow 3 extra to
        // make sure last line is fully visible.
        Assert.assertEquals(35, RecordActivity.getNewScrollPosition(10, 8, 2, tops));
    }

    @org.junit.Test
    public void SelectThird_PrevAndNextVisible_DoesNotScroll()
    {
        int[] tops = {0, 20, 35, 45, 60}; // lines 20, 15, 10, 15
        // select third line; want to see all of 2nd through 4th, which is just possible
        // viewport needs to end up showing those 40 pixels, plus 5 above to make
        // smallest viable motion, so last 45 pixels; however we allow 3 extra to
        // make sure last line is fully visible.
        Assert.assertEquals(5, RecordActivity.getNewScrollPosition(5, 60, 2, tops));
    }
}
