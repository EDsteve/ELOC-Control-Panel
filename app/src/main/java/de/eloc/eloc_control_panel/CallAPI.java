package de.eloc.eloc_control_panel;
//https://stackoverflow.com/questions/2938502/sending-post-data-in-android

/* 
import android.util.Log;
import android.os.AsyncTask;
//import 	java.net.HttpURLConnection;
//import 	java.io.DataOutputStream;
//import 	java.io.File;
//import java.io.FileInputStream;
import 	java.net.*;
import java.io.*;


public class CallAPI extends AsyncTask<String, String, String> {

	public CallAPI(){
		//set context variables if required
	}

	@Override
	protected void onPreExecute() {
		super.onPreExecute();
	}

	 @Override
	 protected String doInBackground(String... params) {
		String urlString = params[0]; // URL to call
		String data = params[1]; //data to post
		OutputStream out = null;

		try {
			URL url = new URL(urlString);
			HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
			out = new BufferedOutputStream(urlConnection.getOutputStream());

			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, "UTF-8"));
			writer.write(data);
			writer.flush();
			writer.close();
			out.close();

			urlConnection.connect();
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}
}
 */
	
	
	