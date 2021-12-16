package com.chris.smart_id_photo_helper;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;

import java.io.File;
import java.io.FileOutputStream;

public class SaveImageTask extends AsyncTask<Bitmap, Void, Uri> {
    private String originalPath;
    private final Context context;
    private boolean isModified;

    public SaveImageTask(Context context, String originalPath, boolean isModified) {
        this.originalPath = originalPath;
        this.context = context;
        this.isModified = isModified;
    }

    @Override
    protected Uri doInBackground(Bitmap... args) {
        Bitmap imageBitmap = args[0];
        // If the duplicate image file for modified image has never been created, create one.
        if (!isModified) {
            originalPath = createDupPhotoFile(originalPath);
        }
        try {
            FileOutputStream fOut = new FileOutputStream(originalPath,false);
            imageBitmap.compress(Bitmap.CompressFormat.JPEG, 90, fOut);
            fOut.flush();
            fOut.close();
            Uri newFileUri = Uri.fromFile(new File(originalPath));
            Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            mediaScanIntent.setData(newFileUri);
            context.sendBroadcast(mediaScanIntent);
            return newFileUri;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Uri.EMPTY;
    }

    @Override
    protected void onPostExecute(Uri uri) {
        if (isCancelled()) uri = Uri.EMPTY;
        if (!uri.equals(Uri.EMPTY)){
            ((EditActivity)context).updateImageFilePath(uri); // Update attribute values in activities
        }

    }

    /**
     * Create a duplicate file of a image file in the same directory.
     * @param path
     * @return
     */
    private String createDupPhotoFile(String path) {
        int divIndex = path.lastIndexOf('/');
        String dir = path.substring(0, divIndex + 1);
        String fullName = path.substring(divIndex + 1);
        int extDivIndex = fullName.lastIndexOf('.');
        String nameWithoutExt = fullName.substring(0, extDivIndex);
        String ext = fullName.substring(extDivIndex);
        while (new File(String.format("%s%s.%s", dir, nameWithoutExt + "_dup", ext)).exists()) {
            nameWithoutExt = nameWithoutExt + "_dup";
        }

        File dupImage = null;
        try {
            dupImage = File.createTempFile(
                    nameWithoutExt,  /* prefix */
                    ext,         /* suffix */
                    new File(dir)      /* directory */
            );
        } catch (Exception e) {
            e.printStackTrace();
        }

        String dupPath = dupImage != null ? dupImage.getAbsolutePath() : null;
        return dupPath;
    }


}
