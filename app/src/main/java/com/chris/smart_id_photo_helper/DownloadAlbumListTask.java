package com.chris.smart_id_photo_helper;

import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.storage.ListResult;
import com.google.firebase.storage.StorageReference;

import java.util.List;

public class DownloadAlbumListTask extends AsyncTask<Void, Void, Void> {

    private final List<PhotoData> albumList;
    private final CustomAdapter adapter;
    private final StorageReference storageReference;
    private boolean isSuccess;

    public DownloadAlbumListTask(StorageReference storageReference, CustomAdapter adapter, List<PhotoData> albumList) {
        this.storageReference = storageReference;
        this.adapter = adapter;
        this.albumList = albumList;
        this.isSuccess = false;
    }


    @Override
    protected Void doInBackground(Void... voids) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();;
        StorageReference listRef = storageReference.child(userId + "/images/");
        listRef.listAll()
                .addOnSuccessListener(new OnSuccessListener<ListResult>() {
                    @Override
                    public void onSuccess(ListResult listResult) {
                        isSuccess = true;
                        for (StorageReference item : listResult.getItems()) {

                            item.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                @Override
                                public void onSuccess(Uri downloadUrl) {
                                    Log.d("download onsuccess", "image URL is :" + downloadUrl.toString());
                                    albumList.add(new PhotoData(item.getName(), downloadUrl.toString()));
                                    adapter.notifyDataSetChanged();
                                }

                            });

                        }


                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // Uh-oh, an error occurred!
                    }
                });
        return null;
    }

    @Override
    protected void onPostExecute(Void unused) {
        for (PhotoData data : albumList) {
            Log.d("download task", String.format("name: %s, image: %s", data.name, data.image));
        }

        Log.d("download task", "issuccess" + isSuccess);
        if (!isCancelled()) adapter.notifyDataSetChanged();
    }
}
