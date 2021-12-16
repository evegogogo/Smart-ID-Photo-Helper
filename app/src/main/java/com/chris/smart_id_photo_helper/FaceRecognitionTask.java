package com.chris.smart_id_photo_helper;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.util.List;

public class FaceRecognitionTask extends AsyncTask<Bitmap, Void, Bitmap> {

    private final Context context;
    private final int reqCode;
    private Bitmap imageBitmap;

    public FaceRecognitionTask(Context context, int reqCode) {
        this.context = context;
        this.reqCode = reqCode;
    }


    @Override
    protected Bitmap doInBackground(Bitmap... args) {
        this.imageBitmap = args[0];
        FaceDetectorOptions options =
                new FaceDetectorOptions.Builder()
                        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                        .build();
        FaceDetector detector = FaceDetection.getClient(options);
        InputImage image;
        Bitmap editedBitmap = imageBitmap.copy(Bitmap.Config.ARGB_8888, true);
        Task<List<Face>> result = null;
        try {
            image = InputImage.fromBitmap(imageBitmap, 0);
            result =
                    detector.process(image)
                            .addOnSuccessListener(
                                    new OnSuccessListener<List<Face>>() {
                                        @Override
                                        public void onSuccess(List<Face> faces) {
                                            Face closestFace = null;
                                            float maxWidth = 0.0f;

                                            for (Face face : faces) {
                                                float width = face.getBoundingBox().width();
                                                if (closestFace == null || width > maxWidth) {
                                                    closestFace = face;
                                                }
                                            }

                                            ((EditActivity) context).updateFaceInfo(closestFace, reqCode);
                                        }
                                    })
                            .addOnFailureListener(
                                    new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            // Task failed with an exception
                                            // ...
                                            e.printStackTrace();
                                            ((EditActivity) context).updateFaceInfo(null, reqCode);
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

