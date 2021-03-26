package com.raghav.paint;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.ContentValues;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.android.material.slider.RangeSlider;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import petrov.kristiyan.colorpicker.ColorPicker;

public class MainActivity extends AppCompatActivity {
    //creating the object of type DrawView
    //in order to get the reference of the View
    private DrawView paint;
    //creating objects of type button
    private ImageButton save,color,stroke,undo;
    //creating a RangeSlider object, which will
    // help in selecting the width of the Stroke
    private RangeSlider rangeSlider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //getting the reference of the views from their ids
        paint=(DrawView)findViewById(R.id.draw_view);
        rangeSlider=(RangeSlider)findViewById(R.id.rangebar);
        undo=(ImageButton)findViewById(R.id.btn_undo);
        save=(ImageButton)findViewById(R.id.btn_save);
        color=(ImageButton)findViewById(R.id.btn_color);
        stroke=(ImageButton)findViewById(R.id.btn_stroke);
        
        //creating a OnClickListener for each button, to perform certain actions

        //the undo button will remove the most recent stroke from the canvas
        undo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                paint.undo();
            }
        });
        //the save button will save the current canvas which is actually a bitmap
        //in form of PNG, in the storage
        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //getting the bitmap from DrawView class
                Bitmap bmp=paint.save();
                //opening a OutputStream to write into the file
                OutputStream imageOutStream = null;

                ContentValues cv=new ContentValues();
                //name of the file
                cv.put(MediaStore.Images.Media.DISPLAY_NAME, "drawing.png");
                //type of the file
                cv.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
                //location of the file to be saved
                cv.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES);

                //ge the Uri of the file which is to be v=created in the storage
                Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cv);
                try {
                    //open the output stream with the above uri
                    imageOutStream = getContentResolver().openOutputStream(uri);
                    //this method writes the files in storage
                    bmp.compress(Bitmap.CompressFormat.PNG, 100, imageOutStream);
                    //close the output stream after use
                    imageOutStream.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        });
        //the color button will allow the user to select the color of his brush
        color.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                final ColorPicker colorPicker=new ColorPicker(MainActivity.this);
                colorPicker.setOnFastChooseColorListener(new ColorPicker.OnFastChooseColorListener() {
                    @Override
                    public void setOnFastChooseColorListener(int position, int color) {
                        //get the integer value of color selected from the dialog box and
                        // set it as the stroke color
                       paint.setColor(color);

                    }

                    @Override
                    public void onCancel() {

                        colorPicker.dismissDialog();
                    }
                })
                        //set the number of color columns you want  to show in dialog.
                        .setColumns(5)
                        //set a default color selected in the dialog
                        .setDefaultColorButton(Color.parseColor("#000000"))
                        .show();
            }
        });
        // the button will toggle the visibility of the RangeBar/RangeSlider
        stroke.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String uploadFilePath = Environment.getExternalStorageDirectory() + "/Pictures/";
                String uploadFileName = "drawing.png";
                String sourceFileUri = uploadFilePath + uploadFileName;
                String upLoadServerUri = LoginActivity.HOST + "/api/upload";
                new Thread(new Runnable() {
                    public void run() {
                        uploadImage(uploadFilePath, uploadFileName, sourceFileUri,
                                upLoadServerUri, uploadFileName, "Image from android");
                    }
                }).start();
                Toast.makeText(getApplicationContext(), "Image has been successfully",
                        Toast.LENGTH_LONG).show();
            }
        });

        //pass the height and width of the custom view to the init method of the DrawView object
        ViewTreeObserver vto = paint.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {

                paint.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                int width = paint.getMeasuredWidth();
                int height = paint.getMeasuredHeight();
                paint.init(height, width);
            }
        });
    }



    protected int uploadImage(String uploadFilePath, String uploadFileName,
                              String sourceFileUri, String upLoadServerUri, String title, String body) {
        String fileName = sourceFileUri;
        int serverResponseCode;
        HttpURLConnection conn = null;
        DataOutputStream dos = null;
        String lineEnd = "\r\n";
        String twoHyphens = "--";
        String boundary = "qwerty";
        int bytesRead, bytesAvailable, bufferSize;
        byte[] buffer;
        int maxBufferSize = 1 * 1024 * 1024;
        File sourceFile = new File(sourceFileUri);

        if (!sourceFile.isFile()) {

            Log.e("uploadFile", "Source File not exist :"
                    + uploadFilePath + "" + uploadFileName);

            return 0;

        } else {
            serverResponseCode = 0;
            try {
                FileInputStream fileInputStream = new FileInputStream(sourceFile);
                URL url = new URL(upLoadServerUri);

                conn = (HttpURLConnection) url.openConnection();
                conn.setDoInput(true);
                conn.setDoOutput(true);
                conn.setUseCaches(false);
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Accept", "*/*");
                conn.setRequestProperty("token", LoginActivity.mToken);
                conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

                dos = new DataOutputStream(conn.getOutputStream());

                dos.writeBytes(twoHyphens + boundary + lineEnd);
                dos.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"" + uploadFileName + "\"");
                dos.writeBytes(lineEnd);
                dos.writeBytes(lineEnd);

                // create a buffer of  maximum size
                bytesAvailable = fileInputStream.available();

                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                buffer = new byte[bufferSize];

                // read file and write it into form...
                bytesRead = fileInputStream.read(buffer, 0, bufferSize);

                while (bytesRead > 0) {

                    dos.write(buffer, 0, bufferSize);
                    bytesAvailable = fileInputStream.available();
                    bufferSize = Math.min(bytesAvailable, maxBufferSize);
                    bytesRead = fileInputStream.read(buffer, 0, bufferSize);

                }

                // send multipart form data necesssary after file data...
                dos.writeBytes(lineEnd);
                dos.writeBytes(lineEnd);

                dos.writeBytes(twoHyphens + boundary + lineEnd);
                dos.writeBytes("Content-Disposition: form-data; name=\"title\"");
                dos.writeBytes(lineEnd);
                dos.writeBytes(lineEnd);
                dos.writeBytes(title);
                dos.writeBytes(lineEnd);
                dos.writeBytes(twoHyphens + boundary + lineEnd);
                dos.writeBytes("Content-Disposition: form-data; name=\"body\"");
                dos.writeBytes(lineEnd);
                dos.writeBytes(lineEnd);
                dos.writeBytes(body);
                dos.writeBytes(lineEnd);
                dos.writeBytes(twoHyphens + boundary + twoHyphens);
                dos.writeBytes(lineEnd);


                // Responses fro m the server (code and message)
                serverResponseCode = conn.getResponseCode();
                String serverResponseMessage = conn.getResponseMessage();

                Log.i(MainActivity.class.getSimpleName(), "OUTPUT HTTP Response is : "
                        + serverResponseMessage + ": " + serverResponseCode);

                if (serverResponseCode == 201) {
                    Log.i(MainActivity.class.getSimpleName(),
                            "OUTPUT File Upload Completed: " + uploadFileName);
                }

                //close the streams //
                fileInputStream.close();
                dos.flush();
                dos.close();

            } catch (MalformedURLException ex) {
                ex.printStackTrace();

                Log.i(MainActivity.class.getSimpleName(),
                        "OUTPUT File Upload error: " + ex.getMessage(), ex);
            } catch (Exception e) {

                e.printStackTrace();
                Log.i(MainActivity.class.getSimpleName(), "OUTPUT File Upload Exception : "
                        + e.getMessage(), e);
            }
            return serverResponseCode;

        }

    }
}