package org.sil.hearthis;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import Script.BookInfo;
import Script.IScriptProvider;
import Script.Project;


public class ChooseBookActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_book);
        IScriptProvider scripture = ServiceLocator.getServiceLocator().init(this).getScriptProvider();
        Project project = new Project("Sample", scripture);
        getSupportActionBar().setTitle(R.string.choose_book);
        setProject(project);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_choose_book, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void setProject(Project project) {
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        ViewGroup bookFlow = (ViewGroup) findViewById(R.id.booksFlow);
        for (BookInfo book : project.Books) {
            int resid = R.layout.book_button;
            if (book.BookNumber == 39) {
                // Matthew: start new line
                resid = R.layout.newline_book_button;
            }

            // This next line is rather non-obvious. We must pass the bookFlow to the inflator
            // so that it can be used to create the Layout for the button: a custom layout which implements
            // the extra properties that the FlowLayout recognizes for its children like newline.
            // But, when we pass a parent, without the third argument inflate adds the button to the
            // flowLayout itself and returns the parent. That leaves us without an easy way to get the
            // new button, on which we want to set other properties. So we pass false (do not add
            // to parent) and thus get the button itself back from inflate. Then of course we must
            // add it to the parent ourselves.
            BookButton bookButton = (BookButton)inflater.inflate(resid, bookFlow, false);
            bookButton.Model = book;
            bookButton.setOnClickListener(bookButtonListener);
            bookButton.setTag(book);
            bookFlow.addView(bookButton);
        }
    }

    public android.view.View.OnClickListener bookButtonListener = new android.view.View.OnClickListener() {

        @Override
        public void onClick(View v) {
            BookInfo book = (BookInfo)v.getTag();
            Intent chooseChapter = new Intent(ChooseBookActivity.this, ChooseChapterActivity.class);
            chooseChapter.putExtra("bookInfo", book);
            startActivity(chooseChapter);
        }
    };
}
