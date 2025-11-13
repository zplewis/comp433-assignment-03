package com.example.comp433assignment03;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class activity_sketchtagger extends AppCompatActivity {

    ArrayList<CommentItem> data;

    CommentListAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sketchtagger);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    /**
     * Erases the drawn image.
     * @param view
     */
    public void onClickClearBtn(View view) {

        MyDrawingArea myDrawingArea = findViewById(R.id.mydrawingarea_main);
        myDrawingArea.resetPath();

        TextView textView = findViewById(R.id.tagsTextbox);
        textView.setText("");

    }

    /**
     * Saves the current image to the database as long as tags are specified.
     * @param view
     */
    public void onClickSaveBtn(View view) {

        MyDrawingArea myDrawingArea = findViewById(R.id.mydrawingarea_main);

        MainActivity.saveImageToDB(this,
            myDrawingArea.getBitmap(),
            findViewById(R.id.tagsTextbox),
            MainActivity.IMAGE_TYPE_SKETCH
        );
    }

    /**
     * Use the Google Vision API to retrieve the top two tags for the image.
     * @param view
     */
    public void onClickGetTags(View view) {

        Log.v(MainActivity.TAG, "Tags clicked from from activity_sketchtagger");

        // This shows the picture on the screen
        MyDrawingArea myDrawingArea = findViewById(R.id.mydrawingarea_main);
        Bitmap bitmap = myDrawingArea.getBitmap();

        if (bitmap == null) {
            return;
        }

        // Get the EditText object that should be populated with the tags upon return
        TextView textView = findViewById(R.id.tagsTextbox);
        textView.setText("Fetching tags...");

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Log.v("takePicture", "Entering vision test try block...");
                    String[] tags = MainActivity.myVisionTester(bitmap);
                    textView.setText(String.join(", ", tags));

                    Log.v("takePicture", "The vision test has completed.");
                } catch (IOException e) {
                    Log.v("takePicture", "The vision test failed; why?");
                    e.printStackTrace();
                    textView.setText(null);
                }
            }
        });

        thread.start();
    }

    public void onClickFindBtn(View view) {

        Log.v(MainActivity.TAG, "Find clicked from from activity_sketchtagger");

        this.data = new ArrayList<>();
        this.adapter = new CommentListAdapter(this, R.layout.list_item, data);

        ListView lv = findViewById(R.id.photolist);
        lv.setAdapter(this.adapter);

        this.data = MainActivity.findImages(this, findViewById(R.id.findTextbox), MainActivity.IMAGE_TYPE_SKETCH);

        this.adapter = new CommentListAdapter(this, R.layout.list_item, data);

        // once you have the adapter, if you want to add items to the ArrayList, notify of the
        // change. Why is this required, or what is the benefit?
//        data.add(new CommentItem(R.drawable.dwayne_johnson, "Dwayne Johnson", "Some comment goes here!"));
//        adapter.notifyDataSetChanged();


        lv.setAdapter(adapter);

    }
}