package com.example.comp433assignment03;

import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.vision.v1.Vision;
import com.google.api.services.vision.v1.VisionRequestInitializer;
import com.google.api.services.vision.v1.model.AnnotateImageRequest;
import com.google.api.services.vision.v1.model.AnnotateImageResponse;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesResponse;
import com.google.api.services.vision.v1.model.Feature;
import com.google.api.services.vision.v1.model.Image;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    /**
     * This is the tag used for LogCat entries for this application.
     */
    public static final String TAG = "assignment03";

    /**
     * This is the number of tags that will be shown to the user.
     */
    public static final int TAGLIMIT = 2;

    /**
     * The name of the database created for this application.
     */
    public static final String DB_NAME = "mydb";

    /**
     * Used with the SQLite database for storing the images. This makes it so that the
     * "Find" button only returns the correct type of images.
     */
    static final int IMAGE_TYPE_PHOTO = 1;
    static final int IMAGE_TYPE_SKETCH = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Creates the tables if they do not exist already
        setupDBAndTables();
    }

    private void onClickHomeButtons(View view, Class activityClass) {

        if (activityClass == null) {
            return;
        }

        Intent intent = new Intent(this, activityClass);

        // Check to see if there is an app that can handle this intent. If not, then return.
        // There is a warning here:
        // Consider adding a <queries> declaration to your manifest when calling this method
        // Why? Never did it and this works.
        ComponentName componentName;
        componentName = intent.resolveActivity(getPackageManager());

        // Stop here if componentName is null; this means that no activity from any other app
        // matches our requested Intent type
        if (componentName == null) {
            Log.v(TAG, "Activity could not be found");
            return;
        }

        startActivity(intent);

    }


    public void onClickBtnPhotoTagger(View view) {

        onClickHomeButtons(view, activity_phototagger.class);

    }

    public void onClickBtnSketchTagger(View view) {

        onClickHomeButtons(view, activity_sketchtagger.class);

    }

    public void onClickBackToHome(View view) {

        Intent intent = new Intent(this, MainActivity.class);

        // Check to see if there is an app that can handle this intent. If not, then return.
        // There is a warning here:
        // Consider adding a <queries> declaration to your manifest when calling this method
        // Why? Never did it and this works.
        ComponentName componentName = intent.resolveActivity(getPackageManager());

        // Stop here if componentName is null; this means that no activity from any other app
        // matches our requested Intent type
        if (componentName == null) {
            Log.v("assignment03", "No app found to take the picture.");
            return;
        }

        startActivity(intent);

    }

    /**
     * This will handle clicking the "Tags" button in both the "photo tagger" and "sketch tagger"
     * activities. When clicked, the EditText to the right of this button will be populated with the top two
     * tags obtained by classifying the imae or the drawing using Google Cloud Vision API.
     * The results can be edited by the user (make sure EditText is editable).
     * @param view
     */
    public void onClickGetTags(View view) {

        Log.v(MainActivity.TAG, "Tags clicked from from MainActivity");

    }

    /**
     * Used to get a Bitmap from a Drawable!
     * @param drawable
     * @return
     */
    public static Bitmap getBitmapFromDrawable(Drawable drawable) {
        if (drawable == null) {
            // Android does not allow a 0x0 bitmap. Width and height must be at least 1 pixel.
            return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
        }

        Bitmap bitmap;

        // This should handle getting the bitmap from the photo tagger class
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        }

        // Attempt to get the drawable a different way
        // Android does not allow a 0x0 bitmap. Width and height must be at least 1 pixel.
        int width = drawable.getIntrinsicWidth() > 0 ? drawable.getIntrinsicWidth() : 1;
        int height = drawable.getIntrinsicHeight() > 0 ? drawable.getIntrinsicHeight() : 1;

        bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }

    /**
     *
     * @return
     */
    public static byte[] getBitmapAsBytes(Bitmap bitmap) {
        if (bitmap == null) {
            return new byte[0];
        }

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        return stream.toByteArray();
    }

    /**
     * 
     * @param response
     * @return A String[] of tags from the Vision API response.
     */
    public static String[] getVisionAPIDescriptions(BatchAnnotateImagesResponse response) {
        List<String> descriptions = new ArrayList<>();

        // response.getResponses() returns a List of AnnotateImageResponse objects
        for (AnnotateImageResponse annotation : response.getResponses()) {

            if (annotation == null || annotation.getLabelAnnotations() == null) {
                continue;
            }

            annotation.getLabelAnnotations().forEach(label -> {

                // This long if statement ensures only valid, unique, non-empty labels are added.
                if (label.getDescription() != null && !label.getDescription().isEmpty() &&
                        !descriptions.contains(label.getDescription())) {
                    descriptions.add(label.getDescription());
                }

                if (descriptions.size() == MainActivity.TAGLIMIT) {
                    return;
                }

            });
        }

        return descriptions.toArray(new String[0]);
    }

    /**
     * Returns the tags returned from the Vision API.
     * @param bitmap
     * @return
     * @throws IOException
     */
    public static String[] myVisionTester(Bitmap bitmap) throws IOException
    {

        if (bitmap == null) {
            Log.v(MainActivity.TAG, "The bitmap is null and cannot be used with Google Vision API.");
            return new String[0];
        }

        Log.v(MainActivity.TAG, "made it to myVisionTester function.");

        //1. ENCODE image.
        Log.v(MainActivity.TAG, "decoded the image into a Bitmap object");

        ByteArrayOutputStream bout = new ByteArrayOutputStream();

        try {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, bout);
        } catch (Exception e) {
            Log.e(MainActivity.TAG, "failed to compress the bitmap into a byte array: " + e.getMessage(), e);
            e.printStackTrace();
        }

        Log.v(MainActivity.TAG, "compressed the bitmap into a byte array (variable name 'bout').");

        Image myimage = new Image();
        myimage.encodeContent(bout.toByteArray());

        Log.v(MainActivity.TAG, "converted the bitmap into an Image object.");

        Log.v(MainActivity.TAG, "made it to creating AnnotateImageRequest.");


        //2. PREPARE AnnotateImageRequest
        AnnotateImageRequest annotateImageRequest = new AnnotateImageRequest();
        annotateImageRequest.setImage(myimage);
        Feature f = new Feature();
        f.setType("LABEL_DETECTION");
        f.setMaxResults(5);
        List<Feature> lf = new ArrayList<Feature>();
        lf.add(f);
        annotateImageRequest.setFeatures(lf);

        Log.v(MainActivity.TAG, "made it to creating the Vision.Builder object...");

        //3.BUILD the Vision
        HttpTransport httpTransport = AndroidHttp.newCompatibleTransport();
        GsonFactory jsonFactory = GsonFactory.getDefaultInstance();
        Vision.Builder builder = new Vision.Builder(httpTransport, jsonFactory, null);

        Log.v(MainActivity.TAG, "made to create the Vision object with the API key...");

        // This app now reads the API key from Gradle Scripts/local.properties, which prevents
        // committing the key accidentally to version control
        // get the api key from the BuildConfig class, which gets it from local.properties (Gradle Scripts)
        builder.setVisionRequestInitializer(new VisionRequestInitializer(BuildConfig.API_KEY));
        Vision vision = builder.build();

        Log.v(MainActivity.TAG, "made to creating the BatchAnnotateImagesRequest...");

        //4. CALL Vision.Images.Annotate
        // To understand the JSON that is returned, look at the documentation for the
        // AnnotateImageResponse object here:
        // https://cloud.google.com/vision/docs/reference/rest/v1/AnnotateImageResponse
        // Each object in the "labelAnnotations" array is an EntityAnnotation object:
        // https://cloud.google.com/vision/docs/reference/rest/v1/AnnotateImageResponse#EntityAnnotation
        BatchAnnotateImagesRequest batchAnnotateImagesRequest = new BatchAnnotateImagesRequest();
        List<AnnotateImageRequest> list = new ArrayList<AnnotateImageRequest>();
        list.add(annotateImageRequest);
        batchAnnotateImagesRequest.setRequests(list);
        Vision.Images.Annotate task = vision.images().annotate(batchAnnotateImagesRequest);
        Log.v(MainActivity.TAG, "About to execute the vision task; please wait up to 60 seconds for a response.");
        BatchAnnotateImagesResponse response = task.execute();
        Log.v(MainActivity.TAG, response.toPrettyString());

        // get a list of descriptions
        return getVisionAPIDescriptions(response);
    }

    public static String[] cslToArray(String text) {
        String[] parts = text.split(",");
        ArrayList<String> list = new ArrayList<>();

        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                list.add(trimmed);
            }
        }

        return list.toArray(new String[0]);
    }

    /**
     * Saves the specified bitmap image and tags from the specified TextView to the SQLite database.
     * @param bitmap
     * @param tagsTextView
     */
    public static void saveImageToDB(Context context, Bitmap bitmap, TextView tagsTextView, int imageType) {
        // this is all code to make sure we have valid values prior to attempting to save anything
        // to the database; input validation!
        if (context == null) {
            // This needed because you cannot call openOrCreateDatabase from a static context.
            return;
        }

        if (bitmap == null) {
            return;
        }

        byte[] bitmapByteArray = getBitmapAsBytes(bitmap);

        if (bitmapByteArray.length == 0) {
            return;
        }

        if (tagsTextView == null) {
            return;
        }

        String[] tags = cslToArray(tagsTextView.getText().toString());

        if (tags.length == 0 || tags.length > TAGLIMIT) {
            return;
        }

        if (imageType != IMAGE_TYPE_PHOTO && imageType != IMAGE_TYPE_SKETCH) {
            return;
        }

        // At this point, we should have a valid image, tags, and image type. Use a ContentValues
        // variable for inserting these items into the database.
        // 3. The "created_at" column defaults to the current timestamp,
        // so we do not have to insert that.
        // Insert the values into the database. You'll need to insert the
        // image into the database.
        ContentValues cv = new ContentValues();
        cv.put("IMAGE", bitmapByteArray);
        cv.put("IMAGE_TYPE_ID", imageType);

        // Insert the image into the database and save the new ID; that will be
        // useful for inserting the tags into the mapping table.
        SQLiteDatabase mydb = null;

        try {

            mydb = context.openOrCreateDatabase(DB_NAME, Context.MODE_PRIVATE, null);

            mydb.beginTransaction();

            // insert the image blob with its type into the "images" table
            long newId = mydb.insert("images", null, cv);

            if (newId == -1) {
                throw new SQLiteException("Failed to save image to the database.");
            }

            // Loop through all of the tags, skipping any null or empty ones.
            // Add the tags to the "image_tags" mapping table.
            for (String tag : tags) {
                if (tag == null) {
                    continue;
                }
                tag = tag.trim();
                if (tag.isEmpty()) {
                    continue;
                }

                cv.clear();
                cv.put("IMAGE_ID", newId);
                cv.put("TAG", tag);
                mydb.insert("image_tags", null, cv);
            }

            mydb.setTransactionSuccessful();

        } catch (SQLiteException e) {
            Log.e("DB_ERROR", "Database error: " + e.getMessage());
        } finally {

            if (mydb != null) {
                mydb.endTransaction();
            }

            if (mydb != null && mydb.isOpen()) {
                mydb.close();
            }
        }

    }

    /**
     * Sets up the tables for use with this application.
     */
    void setupDBAndTables() {

        SQLiteDatabase mydb = null;

        try {

            mydb = this.openOrCreateDatabase(DB_NAME, Context.MODE_PRIVATE, null);

            // allegedly, you can enable support for foreign keys, so let's do that:
            mydb.execSQL("PRAGMA foreign_keys = ON;");

            // both of these lines are required to clear the tables
//            mydb.execSQL("DROP TABLE IF EXISTS images");
//            mydb.execSQL("DROP TABLE IF EXISTS image_tags");

            // Create a table that keeps up with image blobs if it does not exist already
            // SQLite does not have a dedicated DATETIME type, so use text storing in this format:
            // YYYY-MM-DD HH:MM:SS (ISO8601)
            String sql = "CREATE TABLE IF NOT EXISTS images (" +
                    "ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "IMAGE BLOB NOT NULL, " +
                    "CREATED_AT TEXT DEFAULT CURRENT_TIMESTAMP, " +
                    "IMAGE_TYPE_ID INTEGER NOT NULL)";
            mydb.execSQL(sql);

            sql = "CREATE TABLE IF NOT EXISTS image_tags ( " +
                    "ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "IMAGE_ID INTEGER NOT NULL, " +
                    "TAG TEXT NOT NULL)";
            mydb.execSQL(sql);

            // Create a table to keep up with the image types.
            // Make sure photo = 1 and sketch = 2 for consistency
            sql = "CREATE TABLE IF NOT EXISTS image_types ( " +
                    "ID INTEGER PRIMARY KEY, " +
                    "IMAGE_TYPE TEXT NOT NULL); " +
                    "INSERT INTO image_types (ID, IMAGE_TYPE) VALUES (1, 'Photo'); " +
                    "INSERT INTO image_types (ID, IMAGE_TYPE) VALUES (2, 'Sketch') ";
            mydb.execSQL(sql);

        } catch (SQLiteException e) {
            Log.e("DB_ERROR", "Database error: " + e.getMessage());
        } finally {
            if (mydb != null && mydb.isOpen()) {
                mydb.close();
            }
        }
    }
}