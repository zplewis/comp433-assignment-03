package com.example.comp433assignment03;

import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

public class activity_phototagger extends AppCompatActivity {

    ArrayList<String> imageList = new ArrayList<>();
    int imageIndex = -1;

    String currentPhotoPath;

    // Used to uniquely identify the "session of using the camera" to capture an image
    int REQUEST_IMAGE_CAPTURE = 1000;

    ArrayList<CommentItem> data;

    CommentListAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_phototagger);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        ImageView iv = findViewById(R.id.ivMain);
        iv.setImageBitmap(null);
        iv.setBackgroundColor(Color.DKGRAY);
    }

    /**
     * Activates the camera for taking pictures. You MUST save the photo as a file first to
     * get the full-sized version of the photo.
     * @param view
     */
    public void onClickCameraBtn(View view) {

        Log.v(MainActivity.TAG, "Camera clicked from from activity_phototagger");

        // There are two types of Intent objects: explicit (when you specify the class),
        // and implicit, when you are asking for whether an app can meet the need without having
        // to know the class
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        // Check to see if there is an app that can handle this intent. If not, then return.
        // There is a warning here:
        // Consider adding a <queries> declaration to your manifest when calling this method
        // Why? Never did it and this works.
        ComponentName componentName = takePictureIntent.resolveActivity(getPackageManager());

        // Stop here if componentName is null; this means that no activity from any other app
        // matches our requested Intent type
        if (componentName == null) {
            Log.v(MainActivity.TAG, "No app found to take the picture.");
            return;
        }

        // Create the File where the photo should go
        File photoFile;

        try {
            // This will always be not null unless an error occurs
            photoFile = createImageFile();

        } catch (IOException ex) {

            Log.v(MainActivity.TAG, "Error occurred creating the image file.");
            return;
        }

        Uri photoURI = FileProvider.getUriForFile(this,
                "com.example.comp433assignment03.fileprovider", // "com.example.android.fileprovider",
                photoFile);

        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
        startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);

    }

    /**
     * Returns a File object for saving the full-size photo.
     * @return File
     * @throws IOException
     * <a href="https://developer.android.com/media/camera/camera-deprecated/photobasics#TaskPath">...</a>
     */
    private File createImageFile() throws IOException {
        // Create the filename first
        // The Locale.US is optional, sets the timezone for the date
        String timeStamp = new SimpleDateFormat(
                "yyyyMMdd_HHmmss",
                Locale.US
        ).format(new Date());
        String imageFileName = "IMG_" + timeStamp + ".png";

        // Seems like you have to create a File object for the parent directory of the photo
        // that will be returned from the camera
        File imageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);

        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                imageDir      /* directory */
        );

        // save the absolute path of the image file (just in case, I'm not sure it's needed)
        currentPhotoPath = image.getAbsolutePath();

        return image;
    }

    /**
     * This method waits for the picture to be returned from the camera and then updates
     * the imageview. Without using this, the application will be checking for the photo
     * before it exists yet.
     * @param requestCode The integer request code originally supplied to
     *                    startActivityForResult(), allowing you to identify who this
     *                    result came from.
     * @param resultCode The integer result code returned by the child activity
     *                   through its setResult().
     * @param data An Intent, which can return result data to the caller
     *               (various data can be attached to Intent "extras").
     *
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Log.v(MainActivity.TAG, "The camera activity has been returned.");

            File recentPhoto = new File(currentPhotoPath);

            if (!recentPhoto.isFile()) {
                Log.v(MainActivity.TAG, "The file for the newest photo does NOT exist.");
                return;
            }

            // This shows the picture on the screen
            ImageView iv = findViewById(R.id.ivMain);
            // Update the imageview with the appropriate image
            Bitmap image = BitmapFactory.decodeFile(currentPhotoPath);
            iv.setImageBitmap(image);

        }

        // reset the absolute path for the next photo
        currentPhotoPath = "";

        // set the image index
        imageIndex = 0;

        // increment the REQUEST_IMAGE_CAPTURE by 1
        REQUEST_IMAGE_CAPTURE++;
    }

    /**
     *
     * @return
     */
    private Bitmap getBitmapFromCurrentImage() {

        // This shows the picture on the screen
        ImageView iv = findViewById(R.id.ivMain);
        Drawable currentImage = iv.getDrawable();

        if (currentImage == null) {
            return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
        }

        // Get the current image from the ImageView
        return MainActivity.getBitmapFromDrawable(currentImage);
    }

    /**
     * Saves the current image to the database as long as tags are specified
     * @param view
     */
    public void onClickSaveBtn(View view) {

        Log.v(MainActivity.TAG, "Save clicked from from activity_phototagger");

        MainActivity.saveImageToDB(this,
                getBitmapFromCurrentImage(),
                findViewById(R.id.tagsTextbox),
                MainActivity.IMAGE_TYPE_PHOTO
        );

        // Clear the tags after saving
        TextView textView = findViewById(R.id.tagsTextbox);
        textView.setText("");
    }

    /**
     * Use the Google Vision API to retrieve the top two tags for the image.
     * @param view
     */
    public void onClickGetTags(View view) {

        Log.v(MainActivity.TAG, "Tags clicked from from activity_phototagger");

        // Get the current image from the ImageView
        Bitmap bitmap = getBitmapFromCurrentImage();

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
                    Log.v(MainActivity.TAG, "Entering vision test try block...");
                    String[] tags = MainActivity.myVisionTester(bitmap);
                    textView.setText(String.join(", ", tags));

                    Log.v(MainActivity.TAG, "The vision test has completed.");
                } catch (IOException e) {
                    Log.v(MainActivity.TAG, "The vision test failed; why?");
                    e.printStackTrace();
                    textView.setText(null);
                }
            }
        });

        thread.start();
    }

    public void onClickFindBtn(View view) {

        Log.v(MainActivity.TAG, "Find clicked from from activity_phototagger");

        this.data = new ArrayList<>();
        this.adapter = new CommentListAdapter(this, R.layout.list_item, data);

        ListView lv = findViewById(R.id.photolist);
        lv.setAdapter(this.adapter);

        this.data = MainActivity.findImages(this, findViewById(R.id.findTextbox), MainActivity.IMAGE_TYPE_PHOTO);

        this.adapter = new CommentListAdapter(this, R.layout.list_item, data);

        // once you have the adapter, if you want to add items to the ArrayList, notify of the
        // change. Why is this required, or what is the benefit?
//        data.add(new CommentItem(R.drawable.dwayne_johnson, "Dwayne Johnson", "Some comment goes here!"));
//        adapter.notifyDataSetChanged();


        lv.setAdapter(adapter);
    }
}