package com.chris.smart_id_photo_helper;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceLandmark;
import com.skydoves.colorpickerview.ColorEnvelope;
import com.skydoves.colorpickerview.ColorPickerDialog;
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener;
import com.theartofdev.edmodo.cropper.CropImage;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

public class EditActivity extends AppCompatActivity {
    ImageView selectedImage;
    Button uploadBtn;
    Button segmentationBtn;
    Button startCroppingBtn;
    ImageButton colorPickerBtn;
    public static final int SMART_RECOGNITION = 1001;
    public static final int SMART_CROP = 1002;
    Uri imageUri;
    String imageFilePath;
    Bitmap imageBitmap;
    StorageReference storageReference;
    boolean isModified;
    private int[] backgroundColorArgb;
    Button detectionBtn;
    public static final int STORAGE_PERM_CODE = 101;
    Button smartCropBtn;
    private Face face;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit);
        selectedImage = findViewById(R.id.imageView2);
        isModified = false;
        String imagePath = this.getIntent().getExtras().getString("imageUri");
        imageUri = Uri.parse(imagePath);
        selectedImage.setImageURI(imageUri);
        segmentationBtn = findViewById(R.id.segmentationButton);
        startCroppingBtn = findViewById(R.id.cropImageButton);
        uploadBtn = findViewById(R.id.uploadButton);
        colorPickerBtn = findViewById(R.id.colorPickerButton);
        detectionBtn = findViewById(R.id.detectionButton);
        smartCropBtn = findViewById(R.id.smartCropButton);
        imageFilePath = convertContentUriToFileUri(imageUri);
        backgroundColorArgb = new int[]{255, 255, 255, 255};
        face = null;

        segmentationBtn.setOnClickListener(v -> {
            getStoragePermission();
        });

        startCroppingBtn.setOnClickListener(v -> {

            CropImage.activity(imageUri).start(this);

        });


        colorPickerBtn.setOnClickListener(v -> {
            new ColorPickerDialog.Builder(this)
                    .setTitle("Select The Background Color Of Your Photo")
                    .setPreferenceName("MyColorPickerDialog")
                    .setPositiveButton(getString(R.string.color_picker_select_button_text),
                            new ColorEnvelopeListener() {
                                @Override
                                public void onColorSelected(ColorEnvelope envelope, boolean fromUser) {
                                    backgroundColorArgb = envelope.getArgb();
                                    setColorPickerButtonTint();
                                }
                            })
                    .setNegativeButton(getString(R.string.color_picker_cancel_button_text),
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    dialogInterface.dismiss();
                                }
                            })
                    .attachAlphaSlideBar(false) //no alpha for jpg
                    .attachBrightnessSlideBar(true)  // the default value is true.
                    .setBottomSpace(12) // set a bottom space between the last slidebar and buttons.
                    .show();
        });

        File f = new File(imagePath);
        uploadBtn.setOnClickListener(v -> {
            uploadImageToFirebase(f.getName(), imageUri, this);
        });

        detectionBtn.setOnClickListener(v -> {
            runDetectionTask();
        });
        smartCropBtn.setOnClickListener(v -> {
            runSmartCropTask();
        });

        loadImageFileAsBitmap();


        storageReference = FirebaseStorage.getInstance().getReference();
    }

    private void uploadImageToFirebase(String name, Uri imageUri, Context context) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        StorageReference image = storageReference.child(userId + "/images/" + name);
        image.putFile(imageUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                image.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        Log.d("tag", "onSuccess: Upload URL is " + uri.toString());
                        Intent albumActivity = new Intent(context, AlbumActivity.class);
                        startActivity(albumActivity);
                    }
                });
                Toast.makeText(EditActivity.this, "Upload success.", Toast.LENGTH_SHORT).show();
            }

        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(EditActivity.this, "Upload Failed.", Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void getStoragePermission() {
        if ((ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) || (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_PERM_CODE);
        } else {
            runSegmentationTask();// TODO: change it to fit other functions if needed
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_PERM_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this.getApplicationContext(), permissions[1]) == PackageManager.PERMISSION_GRANTED) {
                runSegmentationTask(); // TODO: change it to fit other functions if needed
            } else {
                Toast.makeText(this, "Camera permission is required", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Receive the result from the image cropping activity.
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                Uri resultUri = result.getUri();
                try {
                    // Load bitmap from result image's content Uri
                    this.imageBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), resultUri);
                    // Save as local file
                    new SaveImageTask(this, imageFilePath, isModified).execute(imageBitmap);
                } catch (Exception e) {
                    e.printStackTrace();
                }

            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
                error.printStackTrace();
            }
        }
    }

    /**
     * Load a local image file into a bitmap
     */
    private void loadImageFileAsBitmap() {
        File imageFile = new File(imageFilePath);
        ExifInterface exif = null;
        BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
        bitmapOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;
        try {
            FileInputStream fIn = new FileInputStream(imageFile);
            imageBitmap = BitmapFactory.decodeStream(fIn, null, bitmapOptions);
            exif = new ExifInterface(imageFilePath);
            fIn.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
        int orientation = exif == null ? ExifInterface.ORIENTATION_NORMAL : exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
        // adjust image orientation
        Matrix matrix = new Matrix();
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.postRotate(90);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.postRotate(180);
                break;

            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.postRotate(270);
                break;
        }

        int bitmapHeight = imageBitmap.getHeight();
        int bitmapWidth = imageBitmap.getWidth();

        // update the bitmap
        imageBitmap = Bitmap.createBitmap(imageBitmap, 0, 0, bitmapWidth, bitmapHeight, matrix, true);

    }

    /**
     * Update the values of activity attributes. Called from AsyncTask.
     *
     * @param fileUri
     */
    protected void updateImageFilePath(Uri fileUri) {
        this.isModified = true;
        this.imageFilePath = fileUri.getPath();
        File file = new File(imageFilePath);
        Uri contentUri = FileProvider.getUriForFile(this, "com.chris.android.fileprovider", file);

        this.imageUri = contentUri;
        selectedImage.invalidate();
        selectedImage.setImageURI(null);
        selectedImage.setImageURI(imageUri);
        loadImageFileAsBitmap();
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        mediaScanIntent.setData(imageUri);
        this.sendBroadcast(mediaScanIntent);
        Toast.makeText(this, "Modified image is saved to local album.", Toast.LENGTH_SHORT).show();

    }

    protected void updateFaceInfo(Face face, int reqCode) {
        this.face = face;
        if (this.face == null) {
            Toast.makeText(this, "Face Recognition Failed", Toast.LENGTH_SHORT).show();
        } else {
            if (reqCode == SMART_CROP) smartCrop();
            else if (reqCode == SMART_RECOGNITION) checkFacialFeatures(face);
        }
    }

    private void runSegmentationTask() {
        if (this.imageBitmap != null) {
            Drawable infoDrawable = getResources().getDrawable(R.drawable.ic_processing_text);
            selectedImage.setImageDrawable(infoDrawable);
            new SegmentationTask(this, imageFilePath, isModified, backgroundColorArgb).execute(imageBitmap);

        }

    }

    private void runDetectionTask() {
        if (this.imageBitmap != null) {
            new FaceRecognitionTask(this, SMART_RECOGNITION).execute(imageBitmap);

        }

    }

    private void runSmartCropTask() {
        if (this.imageBitmap != null) {
            new FaceRecognitionTask(this, SMART_CROP).execute(imageBitmap);

        }

    }

    private String convertContentUriToFileUri(Uri originalUri) {
        String filePath = null;
        if (originalUri != null && "content".equals(originalUri.getScheme())) {
            Cursor cursor = this.getContentResolver().query(originalUri, new String[]{android.provider.MediaStore.Images.ImageColumns.DATA}, null, null, null);
            cursor.moveToFirst();
            filePath = cursor.getString(0);
            cursor.close();
        } else {
            filePath = originalUri.getPath();
        }
        return filePath;
    }

    private void setColorPickerButtonTint() {
        colorPickerBtn.setImageTintList(ColorStateList.valueOf(Color.argb(255, backgroundColorArgb[1], backgroundColorArgb[2], backgroundColorArgb[3])));
    }

    private void checkFacialFeatures(Face face) {
        List<String> errorMessageList = new ArrayList<>();
        boolean isRotated = false;
        if (Math.abs(face.getHeadEulerAngleX()) > 20.0f) {
            isRotated = true;
            errorMessageList.add(String.format("Detected face is facing %s at a big angle.", face.getHeadEulerAngleX() > 0.0f ? "upward" : "downward"));
        }
        if (Math.abs(face.getHeadEulerAngleY()) > 12.0f) {
            isRotated = true;
            errorMessageList.add(String.format("Detected face is looking at %s of the camera at a big angle.", face.getHeadEulerAngleY() > 0.0f ? "right" : "left"));
        }
        if (Math.abs(face.getHeadEulerAngleZ()) > 12.0f) {
            isRotated = true;
            errorMessageList.add(String.format("Detected face is rotated %s at a big angle.", face.getHeadEulerAngleZ() > 0.0f ? "counter-clockwise" : "clockwise"));
        }
        if (face.getLeftEyeOpenProbability() < 0.8f && face.getRightEyeOpenProbability() < 0.8f) {
            errorMessageList.add("Detected face is probably closing both eyes");
        } else if (face.getLeftEyeOpenProbability() < 0.8f) {
            errorMessageList.add("Detected face's left eye is probably closed.");
        } else if (face.getRightEyeOpenProbability() < 0.8f) {
            errorMessageList.add("Detected face's right eye is probably closed.");
        }

        if (!isRotated && face.getSmilingProbability() < 0.3f) {
            try {
                PointF left = face.getLandmark(FaceLandmark.MOUTH_LEFT).getPosition();
                PointF right = face.getLandmark(FaceLandmark.MOUTH_LEFT).getPosition();
                PointF bottom = face.getLandmark(FaceLandmark.MOUTH_BOTTOM).getPosition();
                //tilted mouth
                if (((left.y - right.y) / (left.x - right.x) > 0.75f)) {
                    errorMessageList.add("Please check the facial expression.");
                }
            } catch (Exception e) {
                errorMessageList.add("Please check the facial expression.");
            }
        }

        if (errorMessageList.size() == 0) {
            new AlertDialog.Builder(this)
                    .setMessage("No problem detected in the photo")
                    .setTitle("Congratulations!")
                    .setNeutralButton("OK",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.cancel();
                                }
                            })
                    .create()
                    .show();
        } else {
            new AlertDialog.Builder(this)
                    .setMessage(String.join("\n", errorMessageList))
                    .setTitle("Suggestions to improve formal photo quality")
                    .setNeutralButton("OK",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.cancel();
                                }
                            })
                    .create()
                    .show();
        }
    }

    /**
     * Crop the photo into the passport photo format based on face recognition result. The face will be positioned at the center of the cropped photo.
     */
    private void smartCrop() {
        Rect box = face.getBoundingBox();
        int w = box.width();
        int h = box.height();
        double ratio = (double) h / (double) imageBitmap.getHeight();
        if (ratio > (1.375 / 2.0)) {
            // face is too big
            Toast.makeText(EditActivity.this, "Image cannot be cropped into passport photo. Please check the face position/photo size. ", Toast.LENGTH_SHORT).show();

        } else if (((double) h / (1.375 / 2.0)) > (double) imageBitmap.getWidth()) {
            // original width is too small
            Toast.makeText(EditActivity.this, "Image cannot be cropped into passport photo. Please check the face position/photo size. ", Toast.LENGTH_SHORT).show();

        } else {
            int dim = (int) ((double) h / (1.25 / 2.0));
            int minDim = (int) ((double) h / (1.375 / 2.0));
            if (box.centerX() - minDim / 2 < 0 || box.centerX() + minDim / 2 > imageBitmap.getWidth() || box.centerY() - minDim / 2 < 0 || box.centerY() + minDim / 2 > imageBitmap.getHeight()) {
                // face can't be positioned at the center of the cropped photo
                Toast.makeText(EditActivity.this, "Image cannot be cropped into passport photo. Please check the face position/photo size. ", Toast.LENGTH_SHORT).show();
            } else {
                dim = Math.min(imageBitmap.getWidth(), dim);
                dim = Math.min(imageBitmap.getHeight(), dim);
                int startX = Math.max(0, box.centerX() - dim / 2);
                startX = Math.min(imageBitmap.getWidth() - dim, startX);
                int startY = Math.max(0, box.centerY() - dim / 2);
                startY = Math.min(imageBitmap.getHeight() - dim, startY);
                Bitmap croppedBitmap = Bitmap.createBitmap(imageBitmap, startX, startY, dim, dim);
                this.imageBitmap = croppedBitmap;
                Log.d("smart crop", "success");
                new SaveImageTask(this, imageFilePath, isModified).execute(imageBitmap);
            }

        }
    }

}
