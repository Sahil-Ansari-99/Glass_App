package com.example.glassapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageActivity;
import com.theartofdev.edmodo.cropper.CropImageView;

import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.BitSet;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    private Button selectImage, upload, clickPicture, editPicture;
    private ImageView imageView;
    private String imgPath;
    private String currentPhotoPath;
    private String ipv4Address = "192.168.1.4";
    private static final int CAMERA_REQUEST = 1888;
    private static final int UPLOAD_IMAGE = 1500;
    private static final int CROP_PICTURE = 2000;
    private int portNumber = 8080;
    private Uri photoUri;
    private byte[] byteArray;
    private Bitmap currentBitmap;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        selectImage = findViewById(R.id.main_pic_select);
        upload = findViewById(R.id.main_upload);
        imageView = findViewById(R.id.main_imageView);
        clickPicture = findViewById(R.id.main_click_picture);
        editPicture = findViewById(R.id.main_edit_btn);
        progressBar = findViewById(R.id.main_progress_bar);

        Intent intent = getIntent();
        boolean calledAfterEditing = intent.getBooleanExtra("edit", false);

        if (calledAfterEditing) {
            String path = intent.getStringExtra("image");
            try {
                currentBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), Uri.parse(path));
                imageView.setImageBitmap(currentBitmap);
            }catch (Exception e) {
                e.printStackTrace();
            }
        }
        checkRequiredPermissions();
        clickPicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean cameraPermission = checkPermission(Manifest.permission.CAMERA);
                System.out.println(cameraPermission);
                if (cameraPermission) {
                    takePicture();
                } else {
                    requestPermission(Manifest.permission.CAMERA, 1);
                }
            }
        });

        selectImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                imageSelector();
            }
        });

        upload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                progressBar.setVisibility(View.VISIBLE);
                sendRequest(currentBitmap);
            }
        });

        editPicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentPhotoPath != null) {
                    Intent intent = new Intent(getApplicationContext(), EditActivity.class);
                    intent.putExtra("Path", photoUri.toString());
                    startActivity(intent);
                }
            }
        });
    }

    private void takePicture() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (cameraIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (photoFile != null) {
                photoUri = FileProvider.getUriForFile(this, "com.glassapp.android.fileprovider", photoFile);
                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                startActivityForResult(cameraIntent, CAMERA_REQUEST);
            }
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void galleryAddPic() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(currentPhotoPath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
    }

    private void setPic() {
        int targetW =  imageView.getWidth();
        int targetH = imageView.getHeight();

        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;

        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        int scaleFactor = Math.min(photoW / targetW, photoH / targetH);
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        bmOptions.inPurgeable = true;

        Bitmap bitmap = BitmapFactory.decodeFile(currentPhotoPath);
        imageView.setImageBitmap(bitmap);
    }

    private void imageSelector() {
//        Intent intent = new Intent();
//        intent.setType("*/*");
//        intent.setAction(Intent.ACTION_GET_CONTENT);
//        startActivityForResult(intent, UPLOAD_IMAGE);
        Intent getIntent = new Intent(Intent.ACTION_GET_CONTENT);
        getIntent.setType("image/*");
        Intent pickIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickIntent.setType("image/*");
        Intent chooserIntent = Intent.createChooser(getIntent, "Select Image");
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{pickIntent});
        startActivityForResult(chooserIntent, UPLOAD_IMAGE);
    }

    private void cropSelectedImage() {
        try {
            Intent cropIntent = new Intent("com.android.camera.action.CROP");
            cropIntent.setDataAndType(photoUri, "image/*");

            cropIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
            cropIntent.putExtra("crop", "true");
            cropIntent.putExtra("outputX", 180);
            cropIntent.putExtra("outputY", 180);
            cropIntent.putExtra("aspectX", 3);
            cropIntent.putExtra("aspectY", 4);
            cropIntent.putExtra("scaleUpIfNeeded", true);
            cropIntent.putExtra("return-data", true);

            startActivityForResult(cropIntent, CROP_PICTURE);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == UPLOAD_IMAGE && data != null) {
            Uri uri = data.getData();
            photoUri = data.getData();
            CropImage.activity(photoUri)
                    .start(this);
//            String[] projection = {MediaStore.Images.Media.DATA};
//            Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
//            cursor.moveToFirst();
//            int columnIndex = cursor.getColumnIndex(projection[0]);
            currentPhotoPath = uri.getPath();
//            try {
//                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
//                ByteArrayOutputStream stream = new ByteArrayOutputStream();
//                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
//                byteArray = stream.toByteArray();
//                imageView.setImageBitmap(bitmap);
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
        }
        else if (requestCode == CAMERA_REQUEST) {
//            cropSelectedImage();
            CropImage.activity(photoUri)
                    .start(this);
        }
        else if (requestCode == CROP_PICTURE) {
            Bundle extras = data.getExtras();
            if (extras != null) {
                currentBitmap = extras.getParcelable("data");
                imageView.setImageBitmap(currentBitmap);
            }
        }
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                Uri resultUri = result.getUri();
                photoUri = resultUri;
                try {
                    currentBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), resultUri);
                    imageView.setImageBitmap(currentBitmap);
                }catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
            }
        }
    }

//    private void sendRequest(String url, RequestBody requestBody) {
//        OkHttpClient client = new OkHttpClient.Builder()
//                .connectTimeout(60, TimeUnit.SECONDS)
//                .readTimeout(60, TimeUnit.SECONDS)
//                .writeTimeout(60, TimeUnit.SECONDS)
//                .build();
//
//
//        Request request = new Request.Builder()
//                .url(url)
//                .post(requestBody)
//                .build();
//
//        client.newCall(request).enqueue(new Callback() {
//            @Override
//            public void onFailure(@NotNull Call call, @NotNull IOException e) {
//                System.out.println(e.getMessage());
//            }
//
//            @Override
//            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
//                try {
//                    System.out.println(response.body().string());
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            }
//        });
//    }

    private void sendRequest(Bitmap finalBitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        finalBitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        byte[] byteArray = stream.toByteArray();

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("image", "test.jpg", RequestBody.create(MediaType.parse("image/*jpg"), byteArray))
                .addFormDataPart("method", "make_labels")
                .build();

        String url = "http://" + ipv4Address + ":" + portNumber;

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(120, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(120, TimeUnit.SECONDS)
                .build();


        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                progressBar.setVisibility(View.GONE);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "Some error occurred", Toast.LENGTH_LONG).show();
                    }
                });
                System.out.println(e.getMessage());
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
//                try {
//                    final Bitmap res = BitmapFactory.decodeStream(response.body().byteStream());
//                    runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            updateViews(res);
//                        }
//                    });
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
                try {
                    String jsonData = response.body().string();
                    JSONObject jsonObject = new JSONObject(jsonData);
                    String img = jsonObject.getString("img");
                    String fileName = jsonObject.getString("file_name");
                    int labels = jsonObject.getInt("labels");
                    int end_pieces = jsonObject.getInt("end_pieces");
                    final ResultModel model = new ResultModel(img, labels, end_pieces);
//                    runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            updateViews(model);
//                        }
//                    });
                    saveResultBitmap(img, labels, end_pieces, fileName);
                }catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void updateViews(ResultModel model) {
        progressBar.setVisibility(View.GONE);
        byte[] decodeString = Base64.decode(model.getImg(), Base64.DEFAULT);
        Bitmap decodedBitmap = BitmapFactory.decodeByteArray(decodeString, 0, decodeString.length);
        imageView.setImageBitmap(decodedBitmap);
        System.out.println(model.getLabels());
    }

    private void saveResultBitmap(String encodedImage, int labels, int end_pieces, String fileName) {
        try {
            byte[] decodeString = Base64.decode(encodedImage, Base64.DEFAULT);
            Bitmap decodedBitmap = BitmapFactory.decodeByteArray(decodeString, 0, decodeString.length);
            File picFile = createImageFile();
            if (picFile == null) {
                System.out.println("Error making file");
                return;
            }
            FileOutputStream fos = new FileOutputStream(picFile);
            decodedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            Uri editedUri = FileProvider.getUriForFile(this, "com.glassapp.android.fileprovider", picFile);
            fos.close();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    progressBar.setVisibility(View.GONE);
                }
            });
            Intent intent = new Intent(getApplicationContext(), ResultActivity.class);
            intent.putExtra("path", editedUri.toString());
            intent.putExtra("labels", labels);
            intent.putExtra("end_pieces", end_pieces);
            intent.putExtra("file_name", fileName);
            startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void checkRequiredPermissions() {
        boolean writeStoragePermission = checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        boolean readStoragePermission = checkPermission(Manifest.permission.READ_EXTERNAL_STORAGE);
        if (writeStoragePermission && readStoragePermission) {
            System.out.println("Permissions granted");
        } else {
            System.out.println("Permissions requested");
            requestPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, 2);
            requestPermission(Manifest.permission.READ_EXTERNAL_STORAGE, 2);
        }
    }

    private boolean checkPermission(String permission) {
        boolean isGranted = ContextCompat.checkSelfPermission(getApplicationContext(), permission) == PackageManager.PERMISSION_GRANTED;
//        if (!isGranted) {
//            ActivityCompat.requestPermissions(this,
//                    new String[]{permission},
//                    requestCode);
//        }
        return isGranted;
    }

    private void requestPermission(String permission, int requestCode) {
        ActivityCompat.requestPermissions(this,
                new String[]{permission},
                requestCode);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    System.out.println("Camera permission granted");
                    takePicture();
                } else {
                    Toast.makeText(getApplicationContext(), "Camera Permission is required!", Toast.LENGTH_LONG).show();
                }
                return;
            case 2:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    System.out.println("Storage permission granted");
                } else {
                    Toast.makeText(getApplicationContext(), "Storage Permission is required", Toast.LENGTH_LONG).show();
                }
        }
    }
}
