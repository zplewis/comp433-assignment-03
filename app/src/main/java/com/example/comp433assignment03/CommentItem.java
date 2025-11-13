package com.example.comp433assignment03;

import android.graphics.Bitmap;

public class CommentItem {
    Bitmap photo;
    String tags, date;

    CommentItem(Bitmap photo, String tags, String date) {
        this.photo = photo;
        this.tags = tags;
        this.date = date;
    }
}
