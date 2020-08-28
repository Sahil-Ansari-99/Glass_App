package com.example.glassapp;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

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

public class ExamineActivity extends AppCompatActivity {
    private ImageView imageView;
    private SeekBar seekBar;
    private TextView examineArea, examinePerimeter;
    private Button examineLargest, examineSmallest, backButton;
    private int currLabel, currArea, currPerimeter;
    private static final String ipv4Address = "192.168.1.4";
    private static final int portNumber = 8080;
    private static final String callUrl = "http://" + ipv4Address + ":" + portNumber;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_examine_labels);

        seekBar = findViewById(R.id.examine_seekbar);
        imageView = findViewById(R.id.examine_imageView);
        examineArea = findViewById(R.id.examine_area);
        examinePerimeter = findViewById(R.id.examine_perimeter);
        examineLargest = findViewById(R.id.examine_largest_btn);
        examineSmallest = findViewById(R.id.examine_smallest_btn);
        backButton = findViewById(R.id.examine_back_btn);

        Intent currIntent = getIntent();
        int num_labels = currIntent.getIntExtra("labels", 100);
        final String fileName = currIntent.getStringExtra("name");

        seekBar.setMax(num_labels);
        currLabel = num_labels / 2;
        currArea = 0;
        currPerimeter = 0;

        seekBar.setProgress(currLabel);
        setSeekBarStatus(false);
        getLabel(currLabel, fileName);

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
                    getLabel(currLabel, fileName);
                }
            }
        });

        examineLargest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), AreaLabelsActivity.class);
                intent.putExtra("sort", "large");
                intent.putExtra("file_name", fileName);
                startActivity(intent);
            }
        });

        examineSmallest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), AreaLabelsActivity.class);
                intent.putExtra("sort", "small");
                intent.putExtra("file_name", fileName);
                startActivity(intent);
            }
        });

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }

    private void getLabel(int num, String fileName) {
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("method", "label_data")
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
                        System.out.println(jsonData);
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
        examineArea.setText(Integer.toString(area));
        examinePerimeter.setText(Integer.toString(perimeter));
        setSeekBarStatus(true);
    }

    private void setSeekBarStatus(boolean status) {
        seekBar.setEnabled(status);
    }
}
