package org.sil.hearthis;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;

import Script.FileSystem;
import Script.RealScriptProvider;

public class ChooseProjectActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_project);
        getSupportActionBar().setTitle(R.string.choose_project);
        ServiceLocator.getServiceLocator().init(this);
        final ArrayList<String> rootDirs = getProjectRootDirectories();
        ListView projectsList = (ListView) findViewById(R.id.projects_list);
        ArrayList<String> rootNames = new ArrayList<String>();
        for (int i = 0; i < rootDirs.size(); i++)  {
            String path = rootDirs.get(i);
            rootNames.add(path.substring(path.lastIndexOf('/')+1, path.length()));
        }
        projectsList.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, rootNames));
        projectsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                onItemClicked(rootDirs.get(i));
            }
        });
    }

    void onItemClicked(String projectPath) {
        ServiceLocator.getServiceLocator().setScriptProvider(new RealScriptProvider(projectPath));
        MainActivity.launchProject(this);
    }

    private ArrayList<String> getProjectRootDirectories() {
        FileSystem fs = ServiceLocator.getServiceLocator().getFileSystem();
        String rootDir = ServiceLocator.getServiceLocator().externalFilesDirectory;
        return fs.getDirectories(rootDir);
    }
}
