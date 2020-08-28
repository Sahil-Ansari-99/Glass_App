package com.example.glassapp;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AreaLabelsActivity extends AppCompatActivity {
    private SeekBar seekBar;
    private ImageView imageView;
    private int currLabel, currArea, currPerimeter;
    private TextView labelArea, labelPerimeter;
    private Button backButton;
    private static final String ipv4Address = "192.168.1.4";
    private static final int portNumber = 8080;
    private static final String callUrl = "http://" + ipv4Address + ":" + portNumber;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_area_labels);

        seekBar = findViewById(R.id.area_labels_seekBar);
        imageView = findViewById(R.id.area_labels_imageView);
        labelArea = findViewById(R.id.area_labels_area);
        labelPerimeter = findViewById(R.id.area_labels_perimeter);
        backButton = findViewById(R.id.area_labels_back);

        Intent currIntent = getIntent();
        final String sortType = currIntent.getStringExtra("sort");
        final String fileName = currIntent.getStringExtra("file_name");

        currLabel = 0;
        seekBar.setMax(9);
        seekBar.setProgress(0);
        setSeekBarStatus(false);
        getLabel(currLabel, fileName, sortType);

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (currLabel != seekBar.getProgress()) {
                    setSeekBarStatus(false);
                    currLabel = seekBar.getProgress();
                    getLabel(currLabel, fileName, sortType);
                }
            }
        });

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void getLabel(int num, String fileName, String sortType) {
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("sort_type", sortType)
                .addFormDataPart("method", "area_data")
                .addFormDataPart("name", fileName)
                .addFormDataPart("label_number", String.valueOf(num))
                .build();

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(120, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(120, TimeUnit.SECONDS)
                .build();

        final Request request = new Request.Builder()
                .url(callUrl)
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
                    if (response.body() != null) {
                        String jsonData = response.body().string();
                        if (!jsonData.equals("Value Out of range")) {
                            JSONObject jsonObject = new JSONObject(jsonData);
                            final String img = jsonObject.getString("img");
                            currArea = jsonObject.getInt("area");
                            currPerimeter = jsonObject.getInt("perimeter");

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    addLabel(img, currArea, currPerimeter);
                                }
                            });
                        } else {
                            setSeekBarStatus(true);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void addLabel(String img, int area, int perimeter) {
        byte[] decodedString  = Base64.decode(img, Base64.DEFAULT);
        Bitmap decodedBitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
        imageView.setImageBitmap(decodedBitmap);
        labelArea.setText(Integer.toString(area));
        labelPerimeter.setText(Integer.toString(perimeter));
        setSeekBarStatus(true);
    }

    private void setSeekBarStatus(boolean status) {
        seekBar.setEnabled(status);
    }
}
