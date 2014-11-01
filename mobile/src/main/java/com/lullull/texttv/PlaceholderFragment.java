package com.lullull.texttv;

import android.app.Fragment;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A placeholder fragment containing a simple view.
 */
public class PlaceholderFragment extends Fragment {

    private TextView contentTextView;
    private static final String DEBUG_TAG = "PageDownloader";
    private long startPageNr;
    private long currentPageNr;

    public PlaceholderFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        currentPageNr = startPageNr = getArguments().getLong("pagenr");
        contentTextView = (TextView) rootView.findViewById(R.id.content);
        setUpPageNrView(rootView);

        Button homeButton = (Button)rootView.findViewById(R.id.home);
        homeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadPageContent(startPageNr);
            }
        });

        Button refreshButton = (Button)rootView.findViewById(R.id.refresh);
        refreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadPageContent(currentPageNr);
            }
        });

        loadPageContent(startPageNr);
        return rootView;
    }

    private void setUpPageNrView(View rootView) {
        final EditText pageNrView = (EditText) rootView.findViewById(R.id.pagenr);
        pageNrView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    loadPageContent(Long.valueOf(pageNrView.getText().toString()));
                }
                return false;
            }
        });
        pageNrView.setText(String.valueOf(startPageNr));
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connMgr = (ConnectivityManager)
                getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

    private void loadPageContent(long pageNr) {
        currentPageNr = pageNr;
        boolean isNetworkAvailable = isNetworkAvailable();
        if (isNetworkAvailable) {
            String url = "http://texttv.nu/api/get/" + String.valueOf(pageNr);
            new PageDownloaderTask().execute(url);
        } else {
            contentTextView.setText("No network connection available.");
        }
    }

    private class PageDownloaderTask extends AsyncTask<String, Void, String> {

                @Override
                protected String doInBackground(String... params) {
                    String rawContent = downloadUrl(params[0]);
                    Log.d("json", rawContent);
                    GsonBuilder gsonBuilder = new GsonBuilder();
                    //gsonBuilder.setDateFormat("M/d/yy hh:mm a");
                    Gson gson = gsonBuilder.create();
                    List<Page> pages = new ArrayList<Page>();
                    Page[] pagesArray = gson.fromJson(rawContent, Page[].class);
                    pages = Arrays.asList(pagesArray);
                    String content = Html.fromHtml(pages.get(0).content.get(0)).toString();
                    return content;
                }

                @Override
                protected void onPostExecute(String loadedContent) {
                    contentTextView.setText(loadedContent);
                }


    private String downloadUrl(String urlString) {
        InputStream is = null;

        try {
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(10000 /* milliseconds */);
            conn.setConnectTimeout(15000 /* milliseconds */);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            // Starts the query
            conn.connect();
            int response = conn.getResponseCode();
            Log.d(DEBUG_TAG, "The response is: " + response);
            is = conn.getInputStream();

            // Convert the InputStream into a string
            String contentAsString = readIt(is);
            return contentAsString;

            // Makes sure that the InputStream is closed after the app is
            // finished using it.
        } catch (IOException e) {
            Log.e(DEBUG_TAG, "Error downloading from: " + urlString, e);
        }  finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    Log.e(DEBUG_TAG, "Can't close connection", e);
                }
            }
        }
        return "Unable to get content";
    }
        public String readIt(InputStream stream) throws IOException {

            BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
            StringBuilder out = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                out.append(line);
            }
            String content = out.toString();
            reader.close();
            return content;
        }
    }

}
