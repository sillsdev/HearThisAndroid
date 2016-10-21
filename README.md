HearThisAndroid
===============

Simple Scripture recording on Android Devices

This project is so far a personal one, though likely to become an official SIL one soon, so I have made the main repo under SilLsdev.

Synchronizes (over local WiFi) with HearThis for Windows. Can display, record and play back Scripture.

#Building
Currently builds and works using Android Studio 2.2.1.
I have Android SDK platforms installed for 6.0 (Marshmallow/23) and 4.3 (Jelly Bean/18); possibly only the latter is needed. SDK tools I have installed are Android SDK Platform-Tools 24.0.4, Android SDK Tools 25.2.2, Android Support Library, rev 23.2.1, (Documentation for Android SDK, version 1), (Google USB Driver, rev 11), Intel X86 Emulator Accelerator (HAXM installer), 6.0.4), Android Support Repository 38.0.0, Google Repository 36). Ones in parens are those I think are most likely not needed; there may be others that could be omitted. Launching the full SDK manager shows a lot more options installed; hopefully none are relevant. I have not yet had opportunity to attempt a minimal install on a fresh system.

#Testing
HearThis Android only has minimal automated tests and I have had extreme difficulty getting even that far. I can't get any of them to work with the current version of Android Studio (not that I've spent much effort so far). Both sets ran earlier after many struggles to set up the right build configurations.

1. In app\src\test\java\org\sil\hearthis\BookButtonTest.java are some simple tests designed to run without needing an emulator or real android but directly in JUnit. One way to run these tests is to right-click BookButtonTest in the Project panel on the top left and choose "Run 'BookButtonTest'". 
To run tests in multiple files, I had to edit build configurations (Run/Edit configurations). If you do this right after a right-click on org.sil.hearthis and "Run tests in '...'" the configuration it is trying to use will be selected. I was not able to get anywhere with running tests by 'All in package', but if you choose 'all in directory' and configure the directory to be a path to <HearThisAndroid>\app\src\test\java\org\sil\hearthis, it runs the right tests. Possibly the problem is that I have the test directory in the wrong place.
Unfortunately wherever it saves the build configurations does not seem to be checked in.
2. In app\src\androidTest\java\org\sil\hearthis\MainActivityTest.java are some very minimal tests designed to run on an emulator or real device. I believe these also worked once.

The second group of tests currently all fail; my recent attempts to run the others result in reports that no tests are found to run.
There are also some tests in app\src\test\java\org\sil\hearthis\RecordActivityUnitTest.java. I am not sure these ever worked.

# License

HearThisAndroid is open source, using the [MIT License](http://sil.mit-license.org). It is Copyright SIL International.