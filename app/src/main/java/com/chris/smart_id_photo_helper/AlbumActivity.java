package com.chris.smart_id_photo_helper;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class AlbumActivity extends AppCompatActivity {
    ListView listView;
    Button homeBtn;
    StorageReference storageReference;
    ArrayList<PhotoData> albumList = new ArrayList<>();
    CustomAdapter adapter;
    String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_album);
        homeBtn = findViewById(R.id.homeButton);
        storageReference = FirebaseStorage.getInstance().getReference();
        listView = findViewById(R.id.album_list);
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        adapter = new CustomAdapter(this, albumList);
        new DownloadAlbumListTask(storageReference, adapter, albumList).execute();

        ListView listView = findViewById(R.id.album_list);
        listView.setAdapter(adapter);
        registerForContextMenu(listView);

        homeBtn.setOnClickListener(v -> {
            Intent homeActivity = new Intent(this, HomeActivity.class);
            startActivity(homeActivity);
        });


//        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//            @Override
//            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//                Log.d("tag", (String)parent.getItemAtPosition(position));
//                String item = (String) listView.getItemAtPosition(position);
//                Toast.makeText(AlbumActivity.this,"You selected : " + item,Toast.LENGTH_SHORT).show();
//                downloadImage(item);
//            }
//        });
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        if (v.getId() == R.id.album_list) {
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
            menu.setHeaderTitle(albumList.get(info.position).name);
            String[] menuItems = new String[]{"Download", "Delete"};
            for (int i = 0; i < menuItems.length; i++) {
                menu.add(Menu.NONE, i, i, menuItems[i]);
            }
        }
    }

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        int menuItemIndex = item.getItemId();
        String[] menuItems = new String[]{"Download", "Delete"};
        String menuName = menuItems[menuItemIndex];
        String listItemName = albumList.get(info.position).name;
        if (menuName.equals("Download")) {
            downloadImage(listItemName);
        } else if (menuName.equals("Delete")) {
            deleteImage(listItemName);
        }
        return true;
    }

    // TODO: add new parameter for username
    private void deleteImage(String url) {
        StorageReference desertRef = storageReference.child(userId + "/images/" + url);
        ;

        desertRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Toast.makeText(AlbumActivity.this, "Deleted", Toast.LENGTH_SHORT).show();

                for (int i = 0; i < albumList.size(); i++) {
                    String name = albumList.get(i).name;
                    if (name.equals(url)) {
                        albumList.remove(i);
                        break;
                    }
                }
                adapter.notifyDataSetChanged();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Uh-oh, an error occurred!
            }
        });

    }

    // TODO: add new parameter for username
    private void downloadImage(String url) {
        StorageReference islandRef = storageReference.child(userId+ "/images/" + url);
        File tempFile = null;
        File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        try {
            tempFile = File.createTempFile("testfile", ".jpg", storageDir);
            File localFile = new File(tempFile.getAbsolutePath());
            Log.d("tag", "Absoulte URI of Image is" + Uri.fromFile(localFile));
            Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            Uri localFileUri = Uri.fromFile(localFile);
            mediaScanIntent.setData(localFileUri);
            this.sendBroadcast(mediaScanIntent);

        } catch (IOException e) {
            e.printStackTrace();
        }

        islandRef.getFile(tempFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                // Local temp file has been created
                Toast.makeText(AlbumActivity.this, "Downloaded", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle any errors
            }
        });
    }
}
