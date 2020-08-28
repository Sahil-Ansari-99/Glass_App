package com.example.glassapp;

import android.app.AppComponentFactory;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
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

public class EditActivity extends AppCompatActivity {
    private SeekBar contrastSeekbar, brightnessSeekbar, saturationSeekbar;
    private ImageView imageView;
    private Bitmap bitmap, currentBitmap;
    private Button resetBtn, doneBtn;
    private float currContrast, currBrightness, currSaturation;
    private String ipv4Address = "192.168.1.4";
    private int portNumber = 8080;
    private Uri editedUri;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit);

        imageView = findViewById(R.id.edit_imageView);
        contrastSeekbar = findViewById(R.id.edit_seekbar_contrast);
        brightnessSeekbar = findViewById(R.id.edit_seekbar_brightness);
        saturationSeekbar = findViewById(R.id.edit_seekbar_saturation);
        resetBtn = findViewById(R.id.edit_reset_btn);
        doneBtn = findViewById(R.id.edit_done_btn);

        Intent currIntent = getIntent();
        final String imagePath = currIntent.getStringExtra("Path");
        addPicToImageView(Uri.parse(imagePath));

        currContrast = 1f;
        currBrightness = 0f;
        currSaturation = 1f;

        brightnessSeekbar.setProgress(50);
        contrastSeekbar.setProgress(10);
        saturationSeekbar.setProgress(50);

        brightnessSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                currBrightness = (float)(seekBar.getProgress() - 50);
                Bitmap edited = changeContrastBrightness(bitmap, currContrast, currBrightness);
                currentBitmap = edited;
                imageView.setImageBitmap(edited);
            }
        });

        contrastSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                currContrast = (float) (seekBar.getProgress() / 10f);
                Bitmap edited = changeContrastBrightness(bitmap, currContrast, currBrightness);
                currentBitmap = edited;
                imageView.setImageBitmap(edited);
            }
        });

        saturationSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                currSaturation = 1 + (seekBar.getProgress() - 50) / 50f;
                Bitmap edited = changeSaturation(currentBitmap, currContrast, currBrightness, currSaturation);
                currentBitmap = edited;
                imageView.setImageBitmap(edited);
            }
        });

        resetBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currBrightness = 0f;
                currContrast = 1f;
                currSaturation = 1f;
                brightnessSeekbar.setProgress(50);
                contrastSeekbar.setProgress(10);
                saturationSeekbar.setProgress(50);
                currentBitmap = bitmap;
                imageView.setImageBitmap(bitmap);
            }
        });

        doneBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveEditedBitmap(currentBitmap);
            }
        });
    }

    private void saveEditedBitmap(Bitmap bitmapToSave) {
        try {
            File picFile = createImageFile();
            if (picFile == null) {
                System.out.println("Error making file");
                return;
            }
            FileOutputStream fos = new FileOutputStream(picFile);
            bitmapToSave.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            editedUri = FileProvider.getUriForFile(this, "com.glassapp.android.fileprovider", picFile);
            fos.close();
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            intent.putExtra("image", editedUri.toString());
            intent.putExtra("edit", true);
            startActivity(intent);
        }catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void addPicToImageView(Uri path) {
//        int targetW = imageView.getWidth();
//        int targetH = imageView.getHeight();
//
//        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
//        bmOptions.inJustDecodeBounds = true;
//
//        int photoW = bmOptions.outWidth;
//        int photoH = bmOptions.outHeight;
//
//        int scaleFactor = Math.min(photoW / targetW, photoH / targetH);
//        bmOptions.inJustDecodeBounds = false;
//        bmOptions.inSampleSize = scaleFactor;
//        bmOptions.inPurgeable = true;
//        bitmap = BitmapFactory.decodeFile(path);
        try {
            bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), path);
            currentBitmap = bitmap;
            imageView.setImageBitmap(bitmap);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Bitmap changeContrastBrightness(Bitmap bitmapToEdit, float contrast, float brightness) {
        ColorMatrix cm = new ColorMatrix(new float[]
                {
                        contrast, 0, 0, 0, brightness,
                        0, contrast, 0, 0, brightness,
                        0, 0, contrast, 0, brightness,
                        0, 0, 0, 1, 0
                });

        Bitmap editedBitmap = Bitmap.createBitmap(bitmapToEdit.getWidth(), bitmapToEdit.getHeight(), bitmapToEdit.getConfig());
        Canvas canvas = new Canvas(editedBitmap);
        Paint paint = new Paint();
        paint.setColorFilter(new ColorMatrixColorFilter(cm));
        canvas.drawBitmap(bitmapToEdit, 0, 0, paint);

        return editedBitmap;
    }

    private Bitmap changeSaturation(Bitmap bitmapToEdit, float brightness, float contrast, float saturation) {
        ColorMatrix cm = new ColorMatrix(new float[]
                {
                        contrast, 0, 0, 0, brightness,
                        0, contrast, 0, 0, brightness,
                        0, 0, contrast, 0, brightness,
                        0, 0, 0, 1, 0
                });

        cm.setSaturation(saturation);
        Bitmap editedBitmap = Bitmap.createBitmap(bitmapToEdit.getWidth(), bitmapToEdit.getHeight(), bitmapToEdit.getConfig());
        Canvas canvas = new Canvas(editedBitmap);
        Paint paint = new Paint();
        paint.setColorFilter(new ColorMatrixColorFilter(cm));
        canvas.drawBitmap(bitmapToEdit, 0, 0, paint);

        return editedBitmap;
    }

    private void sendRequest(Bitmap finalBitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        finalBitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        byte[] byteArray = stream.toByteArray();

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("image", "test.jpg", RequestBody.create(MediaType.parse("image/*jpg"), byteArray))
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
                System.out.println(e.getMessage());
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                try {
                    Bitmap res = BitmapFactory.decodeStream(response.body().byteStream());
                    imageView.setImageBitmap(res);
                    System.out.println(response.body().string());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
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
//        currentPhotoPath = image.getAbsolutePath();
        return image;
    }
}
