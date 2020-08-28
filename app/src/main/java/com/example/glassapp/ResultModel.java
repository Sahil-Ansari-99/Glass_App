package com.example.glassapp;

public class ResultModel {
    public String img;
    public int labels, end_pieces;

    public ResultModel() {
    }

    public ResultModel(String img, int labels, int end_pieces) {
        this.img = img;
        this.labels = labels;
        this.end_pieces = end_pieces;
    }

    public String getImg() {
        return img;
    }

    public int getLabels() {
        return labels;
    }

    public int getEnd_pieces() {
        return end_pieces;
    }
}
