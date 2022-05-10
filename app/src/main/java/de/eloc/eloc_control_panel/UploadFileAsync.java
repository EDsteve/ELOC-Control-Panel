package de.eloc.eloc_control_panel;
//https://stackoverflow.com/questions/25398200/uploading-file-in-php-server-from-android-device


import android.util.Log;
import android.os.AsyncTask;

import java.net.HttpURLConnection;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.URL;

import android.widget.Toast;

import android.content.Context;

import androidx.fragment.app.Fragment;


public class UploadFileAsync extends AsyncTask<String, Void, String> {

    public String filename = "";
    public File filesDir;
    public interface StringCallback {
        void handle(String s);
    }
    private StringCallback snackHandler;

    public boolean success = false;

    public UploadFileAsync(StringCallback callback) {
        snackHandler = callback;
    }

    @Override
    protected String doInBackground(String... params) {

        try {
            String sourceFileUri = filename;

            HttpURLConnection conn = null;
            DataOutputStream dos = null;
            String lineEnd = "\r\n";
            String twoHyphens = "--";
            String boundary = "*****";
            int bytesRead, bytesAvailable, bufferSize;
            byte[] buffer;
            int maxBufferSize = 1 * 1024 * 1024;
            File sourceFile = new File(sourceFileUri);

            if (sourceFile.isFile()) {

                try {
                    String upLoadServerUri = "http://indodic.com/tom/eloc/upload.php?";
                    // php file looks like this
					
	/* 				<?php


					 if (is_uploaded_file($_FILES['bill']['tmp_name'])) {
					$uploads_dir = './files/';
											$tmp_name = $_FILES['bill']['tmp_name'];
											$pic_name = $_FILES['bill']['name'];
											move_uploaded_file($tmp_name, $uploads_dir.$pic_name);
											}
							   else{
								   echo "File not uploaded successfully.";
						   }

				   ?> */

                    // open a URL connection to the Servlet
                    FileInputStream fileInputStream = new FileInputStream(
                            sourceFile);
                    URL url = new URL(upLoadServerUri);

                    // Open a HTTP connection to the URL
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setDoInput(true); // Allow Inputs
                    conn.setDoOutput(true); // Allow Outputs
                    conn.setUseCaches(false); // Don't use a Cached Copy
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Connection", "Keep-Alive");
                    conn.setRequestProperty("ENCTYPE",
                            "multipart/form-data");
                    conn.setRequestProperty("Content-Type",
                            "multipart/form-data;boundary=" + boundary);
                    conn.setRequestProperty("bill", sourceFileUri);

                    dos = new DataOutputStream(conn.getOutputStream());

                    dos.writeBytes(twoHyphens + boundary + lineEnd);
                    dos.writeBytes("Content-Disposition: form-data; name=\"bill\";filename=\""
                            + sourceFileUri + "\"" + lineEnd);

                    dos.writeBytes(lineEnd);

                    // create a buffer of maximum size
                    bytesAvailable = fileInputStream.available();

                    bufferSize = Math.min(bytesAvailable, maxBufferSize);
                    buffer = new byte[bufferSize];

                    // read file and write it into form...
                    bytesRead = fileInputStream.read(buffer, 0, bufferSize);

                    while (bytesRead > 0) {

                        dos.write(buffer, 0, bufferSize);
                        bytesAvailable = fileInputStream.available();
                        bufferSize = Math
                                .min(bytesAvailable, maxBufferSize);
                        bytesRead = fileInputStream.read(buffer, 0,
                                bufferSize);

                    }

                    // send multipart form data necesssary after file
                    // data...
                    dos.writeBytes(lineEnd);
                    dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

                    // Responses from the server (code and message)
                    int serverResponseCode = conn.getResponseCode();
                    String serverResponseMessage = conn.getResponseMessage();

                    if (serverResponseCode == 200) {
                        success = true;
                        // messageText.setText(msg);
                        //Toast.makeText(ctx, "Status update SUCCESS", Toast.LENGTH_SHORT).show();


                    } else {

                        success = false;
                        //Toast.makeText(getActivity(), "Upload FAIL", Toast.LENGTH_SHORT).show();

                    }

                    // close the streams //
                    fileInputStream.close();
                    dos.flush();
                    dos.close();

                } catch (Exception e) {

                    // dialog.dismiss();
                    e.printStackTrace();

                }
                // dialog.dismiss();

            } // End else block


        } catch (Exception ex) {
            // dialog.dismiss();

            ex.printStackTrace();
        }
        return "Executed";
    }


    private void deleteAllWithExtension(String extension) {
        if (filesDir != null) {
            File[] files = filesDir.listFiles();
            for (int i = 0; i < files.length; ++i) {
                File file = files[i];

                if (file.getName().endsWith(extension)) {
                    file.delete();
                    Log.i("elocApp", "deleted file " + file.getName());

                }
            }
        }
    }


    @Override
    protected void onPostExecute(String result) {
        //TerminalFragment.appendReceiveText("finished");
        //success=false;
        File temp = new File(filename);
        temp.delete();
        Log.i("elocApp", "file deleted " + filename);

        if (success == true) {
            Log.i("elocApp", "upload SUCCESS ");
            if (snackHandler != null) {
                snackHandler.handle("Upload Success");
            }
            // recursiveDelete(mDirectory1);
            deleteAllWithExtension(".txt");

        } else {
            Log.i("elocApp", "upload FAIL ");
            if (snackHandler != null) {
                snackHandler.handle("Upload FAIL");
            }

            // leave the file

        }
    }

    @Override
    protected void onPreExecute() {
        success = false;
    }

    @Override
    protected void onProgressUpdate(Void... values) {
    }
}