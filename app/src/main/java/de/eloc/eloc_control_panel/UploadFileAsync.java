package de.eloc.eloc_control_panel;

import android.util.Log;

import java.net.HttpURLConnection;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.util.concurrent.Executors;

import de.eloc.eloc_control_panel.ng2.interfaces.StringCallback;

public class UploadFileAsync {

    private static String filename = "";
    private static File filesDir;
    private static final String boundary = "*****";
    private static boolean success = false;

    public static void run(String filename, File filesDir, StringCallback snackHandler) {
        Executors.newSingleThreadExecutor().execute(() -> {
            UploadFileAsync.filesDir = filesDir;
            UploadFileAsync.filename = filename;
            success = false;
            doTask();
            taskCompleted(snackHandler);
        });
    }

    private static void doTask() {
        String lineEnd = "\r\n";
        String twoHyphens = "--";
        File sourceFile = new File(filename);
        if (sourceFile.isFile()) {
            HttpURLConnection connection = openConnection(filename);
            if (connection != null) {
                try (DataOutputStream dos = new DataOutputStream(connection.getOutputStream())) {
                    dos.writeBytes(twoHyphens + boundary + lineEnd);
                    dos.writeBytes("Content-Disposition: form-data; name=\"bill\";filename=\""
                            + filename + "\"" + lineEnd);
                    dos.writeBytes(lineEnd);

                    // Read from file and to network stream
                    byte[] tmp = new byte[8192];
                    try (FileInputStream fileInputStream = new FileInputStream(sourceFile)) {
                        while (true) {
                            int readCount = fileInputStream.read(tmp);
                            if (readCount > 0) {
                                dos.write(tmp, 0, readCount);
                            } else {
                                break;
                            }
                        }

                        // send multipart form data necesssary after file
                        // data...
                        dos.writeBytes(lineEnd);
                        dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

                        int serverResponseCode = connection.getResponseCode();
                        success = ((serverResponseCode >= 200) && (serverResponseCode < 300));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static HttpURLConnection openConnection(String sourceFileUri) {
        HttpURLConnection conn = null;
        try {
            String upLoadServerUri = "http://128.199.206.198/ELOC/upload.php?";
            URL url = new URL(upLoadServerUri);
            conn = (HttpURLConnection) url.openConnection();
            conn.setDoInput(true); // Allow Inputs
            conn.setDoOutput(true); // Allow Outputs
            conn.setUseCaches(false); // Don't use a Cached Copy
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Connection", "Keep-Alive");
            conn.setRequestProperty("ENCTYPE", "multipart/form-data");
            conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
            conn.setRequestProperty("bill", sourceFileUri);
        } catch (Exception ignore) {

        }
        return conn;
    }

    private static void taskCompleted(StringCallback snackHandler) {
        File temp = new File(filename);
        temp.delete();
        Log.i("elocApp", "file deleted " + filename);

        if (success) {
            Log.i("elocApp", "upload SUCCESS ");
            if (snackHandler != null) {
                snackHandler.handler("Upload Success");
            }
            // recursiveDelete(mDirectory1);
            deleteAllWithExtension(".txt");

        } else {
            Log.i("elocApp", "upload FAIL ");
            if (snackHandler != null) {
                snackHandler.handler("Upload FAIL");
            }
        }
    }

    private static void deleteAllWithExtension(String extension) {
        if (filesDir != null) {
            File[] files = filesDir.listFiles();
            for (File file : files) {
                if (file.getName().endsWith(extension)) {
                    boolean deleted = file.delete();
                    String message = deleted ? "deleted file " : "failed to delete file ";
                    Log.d("elocApp", message + file.getName());
                }
            }
        }
    }
}