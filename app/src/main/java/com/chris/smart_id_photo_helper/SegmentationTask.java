package com.chris.smart_id_photo_helper;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.AsyncTask;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.segmentation.Segmentation;
import com.google.mlkit.vision.segmentation.SegmentationMask;
import com.google.mlkit.vision.segmentation.Segmenter;
import com.google.mlkit.vision.segmentation.selfie.SelfieSegmenterOptions;

import java.nio.ByteBuffer;

public class SegmentationTask extends AsyncTask<Bitmap, Void, Bitmap> {

    private Bitmap imageBitmap;
    private final String originalPath;
    private final boolean isModified;
    private final Context context;
    private final int[] backgroundArgb;

    public SegmentationTask(Context context, String originalPath, boolean isModified, int[] backgroundArgb) {
        this.originalPath = originalPath;
        this.isModified = isModified;
        this.context = context;
        this.backgroundArgb = backgroundArgb;
        for (int i = 0; i < backgroundArgb.length; i++) {
            backgroundArgb[i] = Math.max(0, backgroundArgb[i]);
            backgroundArgb[i] = Math.min(255, backgroundArgb[i]);
        }
    }


    @Override
    protected Bitmap doInBackground(Bitmap... args) {
        this.imageBitmap = args[0];
        SelfieSegmenterOptions options =
                new SelfieSegmenterOptions.Builder()
                        .setDetectorMode(SelfieSegmenterOptions.SINGLE_IMAGE_MODE)
                        .build();
        Segmenter segmenter = Segmentation.getClient(options);
        InputImage image;
        Bitmap editedBitmap = imageBitmap.copy(Bitmap.Config.ARGB_8888, true);
        Task<SegmentationMask> result = null;
        try {
            image = InputImage.fromBitmap(imageBitmap, 0);
            result =
                    segmenter.process(image)
                            .addOnSuccessListener(
                                    new OnSuccessListener<SegmentationMask>() {
                                        @Override
                                        public void onSuccess(SegmentationMask mask) {
                                            Log.d("segmentation", "success");
                                            ByteBuffer byteBuffer = mask.getBuffer();
                                            int maskWidth = mask.getWidth();
                                            int maskHeight = mask.getHeight();
                                            for (int y = 0; y < maskHeight; y++) {
                                                for (int x = 0; x < maskWidth; x++) {
                                                    // Gets the confidence of the (x,y) pixel in the mask being in the foreground.
                                                    float backgroundConfidence = 1.0f - byteBuffer.getFloat();
                                                    if (backgroundConfidence > 0.95) {
                                                        editedBitmap.setPixel(x, y, Color.argb(255, backgroundArgb[1], backgroundArgb[2], backgroundArgb[3]));
                                                    } else if (backgroundConfidence > 0.8) {
                                                        int cur = editedBitmap.getPixel(x, y);
                                                        int red = (Color.red(cur) + backgroundArgb[1] * 2) / 3;
                                                        int green = (Color.green(cur) + backgroundArgb[2] * 2) / 3;
                                                        int blue = (Color.blue(cur) + backgroundArgb[3] * 2) / 3;
                                                        editedBitmap.setPixel(x, y, Color.argb(255, red, green, blue));
                                                    } else if (backgroundConfidence > 0.65) {
                                                        int cur = editedBitmap.getPixel(x, y);
                                                        int red = (Color.red(cur) + backgroundArgb[1]) / 2;
                                                        int green = (Color.green(cur) + backgroundArgb[2]) / 2;
                                                        int blue = (Color.blue(cur) + backgroundArgb[3]) / 2;
                                                        editedBitmap.setPixel(x, y, Color.argb(255, red, green, blue));
                                                    }

                                                }
                                            }
                                            imageBitmap = editedBitmap.copy(Bitmap.Config.ARGB_8888, false);
                                            new SaveImageTask(context, originalPath, isModified).execute(imageBitmap);

                                        }
                                    })
                            .addOnFailureListener(
                                    new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            Log.d("segmentation", "failed");
                                        }
                                    });

        } catch (Exception e) {
            e.printStackTrace();
        }
        return imageBitmap;
    }

    @Override
    protected void onPostExecute(Bitmap bitmap) {

    }
}

