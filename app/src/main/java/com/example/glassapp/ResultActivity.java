package com.example.glassapp;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class ResultActivity extends AppCompatActivity {
    private ImageView imageView;
    private TextView numLabels, numEndPieces;
    private Bitmap currentBitmap;
    private Button examineButton, backButton;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        imageView = findViewById(R.id.result_imageView);
        numLabels = findViewById(R.id.result_labels);
        numEndPieces = findViewById(R.id.result_end_pieces);
        examineButton = findViewById(R.id.result_examine_button);
        backButton = findViewById(R.id.result_back_button);

        Intent currIntent = getIntent();

        String imgPath = currIntent.getStringExtra("path");
        final int labels = currIntent.getIntExtra("labels", 0);
        int end_pieces = currIntent.getIntExtra("end_pieces", 0);
        final String fileName = currIntent.getStringExtra("file_name");

        SharedPreferences pref = getApplicationContext().getSharedPreferences("file_name", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putString("name", fileName);
        editor.apply();

        setUpViews(imgPath, labels, end_pieces);

        examineButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent examineIntent = new Intent(getApplicationContext(), ExamineActivity.class);
                examineIntent.putExtra("labels", labels);
                examineIntent.putExtra("name", fileName);
                startActivity(examineIntent);
            }
        });

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }

    private void setUpViews(String path, int labels, int end_pieces) {
        if (path != null) {
            try {
                currentBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), Uri.parse(path));
                imageView.setImageBitmap(currentBitmap);
                numLabels.setText(Integer.toString(labels));
                numEndPieces.setText(Integer.toString(end_pieces));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
