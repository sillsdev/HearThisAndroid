package org.sil.hearthis;

import android.os.Build;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Objects;

import script.FileSystem;
import script.RealScriptProvider;

public class ChooseProjectActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            EdgeToEdge.enable(this);
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_project);

        final ListView projectsList = findViewById(R.id.projects_list);
        
        if (projectsList != null) {
            ViewCompat.setOnApplyWindowInsetsListener(projectsList, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        Objects.requireNonNull(getSupportActionBar()).setTitle(R.string.choose_project);
        ServiceLocator.getServiceLocator().init(this);
        
        final ArrayList<String> rootDirs = getProjectRootDirectories();
        ArrayList<String> rootNames = new ArrayList<>();
        for (int i = 0; i < rootDirs.size(); i++)  {
            String path = rootDirs.get(i);
            rootNames.add(path.substring(path.lastIndexOf('/')+1));
        }
        
        if (projectsList != null) {
            projectsList.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, rootNames));
            projectsList.setOnItemClickListener((parent, view, position, id) -> onItemClicked(rootDirs.get(position)));
        }
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
