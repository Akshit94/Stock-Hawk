package com.sam_chordas.android.stockhawk.ui;

import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.db.chart.model.LineSet;
import com.db.chart.view.LineChartView;
import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.rest.Utils;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;

public class DetailActivity extends AppCompatActivity {

    private static String LOG_TAG = DetailActivity.class.getSimpleName();

    private OkHttpClient client = new OkHttpClient();

    String symbol;
    TextView tvStockSymbol;
    LineChartView lineChartView;
    ProgressBar mProgressBar;
    TextView mDetailEmptyTextView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_line_graph);
        mDetailEmptyTextView = (TextView) findViewById(R.id.detail_empty_view);
        mProgressBar = (ProgressBar) findViewById(R.id.detail_progress_bar);
        lineChartView = (LineChartView) findViewById(R.id.linechart);
        mDetailEmptyTextView.setVisibility(View.GONE);

        if (Utils.isNetworkAvailable(getApplicationContext())) {
            Intent intent = getIntent();
            if (intent.hasExtra(getString(R.string.intent_extra_symbol))) {
                symbol = intent.getStringExtra(getString(R.string.intent_extra_symbol));
                tvStockSymbol = (TextView) findViewById(R.id.stock_symbol_textview);
                tvStockSymbol.setText(symbol);
                tvStockSymbol.setContentDescription(symbol);
                lineChartView.setContentDescription(symbol + getString(R.string.time_graph_content_description));

                DownloadQuoteDetailsTask quoteDetailsTask = new DownloadQuoteDetailsTask();
                quoteDetailsTask.execute(symbol);
            }
        } else {
            networkToast();
            mProgressBar.setVisibility(View.GONE);
            mDetailEmptyTextView.setVisibility(View.VISIBLE);
            mDetailEmptyTextView.setText(getString(R.string.no_network));
        }
    }

    public void networkToast() {
        Toast.makeText(getApplicationContext(), getString(R.string.network_toast), Toast.LENGTH_SHORT).show();
    }

    String fetchData(String url) throws IOException {

        Request request = new Request.Builder()
                .url(url)
                .build();

        Response response = client.newCall(request).execute();
        return response.body().string();
    }

    private class DownloadQuoteDetailsTask extends AsyncTask<String, Void, BuildChart> {
        @Override
        protected BuildChart doInBackground(String... params) {

            String stockInput = params[0];

            StringBuilder urlStringBuilder = new StringBuilder();

            try {
                urlStringBuilder.append(getString(R.string.chart_base_url));
                urlStringBuilder.append(URLEncoder.encode(stockInput, getString(R.string.utf8)));
                urlStringBuilder.append(getString(R.string.url_json));

            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

            String urlString;
            urlString = urlStringBuilder.toString();

            String getResponse = "";

            try {
                getResponse = fetchData(urlString);
            } catch (IOException e) {
                e.printStackTrace();
            }

            int startIndex = getString(R.string.finance_charts_json).length();
            int endIndex = getResponse.length() - 1;

            getResponse = getResponse.substring(startIndex, endIndex);

            JSONObject jsonObject;
            JSONArray resultsArray;

            BuildChart buildChart = new BuildChart();
            float[] results;

            try {
                jsonObject = new JSONObject(getResponse);

                jsonObject = jsonObject.getJSONObject(getString(R.string.ranges)).getJSONObject(getString(R.string.close));

                buildChart.setMinimum(jsonObject.getString(getString(R.string.min)));
                buildChart.setMaximum(jsonObject.getString(getString(R.string.max)));

                jsonObject = new JSONObject(getResponse);
                resultsArray = jsonObject.getJSONArray(getString(R.string.series));

                if (resultsArray != null && resultsArray.length() != 0) {
                    for (int i = 0; i < resultsArray.length(); i++) {
                        jsonObject = resultsArray.getJSONObject(i);
                        buildChart.addDataPoint(jsonObject);
                    }
                }

            } catch (JSONException e) {
                Log.e(LOG_TAG, getString(R.string.json_exception) + e);
            }

            return buildChart;
        }

        @Override
        protected void onPostExecute(BuildChart buildChart) {
            super.onPostExecute(buildChart);

            LineSet dataset = new LineSet(buildChart.getLabels(), buildChart.getPoints());

            dataset.setColor(Color.parseColor(getString(R.string.white_color)))
                    .setFill(Color.parseColor(getString(R.string.grey_color)))
                    .setThickness(1)
                    .setDashed(new float[]{10f, 10f})
                    .beginAt(0);


            lineChartView.addData(dataset);

            int step;

            if (buildChart.getMaximum() > 100) {
                step = 100;
            } else if (buildChart.getMaximum() > 1) {
                step = 10;
            } else {
                step = 1;
            }

            lineChartView.setAxisBorderValues(buildChart.getMinimum(), buildChart.getMaximum(), step);
            lineChartView.setAxisColor(Color.parseColor(getString(R.string.white_color)));
            lineChartView.setLabelsColor(Color.parseColor(getString(R.string.white_color)));
            lineChartView.show();
            mProgressBar.setVisibility(View.GONE);

        }
    }

    public class BuildChart {

        ArrayList<String> mLabel;
        ArrayList<Float> mValue;
        int mMinimum;
        int mMaximum;

        public BuildChart() {
            mLabel = new ArrayList<>();
            mValue = new ArrayList<>();
        }

        public void addDataPoint(JSONObject jsonObject) {
            try {
                String[] months = new String[]{getString(R.string.jan),
                        getString(R.string.feb),
                        getString(R.string.mar),
                        getString(R.string.apr),
                        getString(R.string.may),
                        getString(R.string.jun),
                        getString(R.string.jul),
                        getString(R.string.aug),
                        getString(R.string.sept),
                        getString(R.string.oct),
                        getString(R.string.nov),
                        getString(R.string.dec)};
                String close = jsonObject.getString(getString(R.string.close));
                String closeDate = jsonObject.getString(getString(R.string.date));
                int currentMonth = Integer.valueOf(closeDate.substring(4, 6)) - 1;
                mLabel.add(months[currentMonth]);
                mValue.add(Float.valueOf(close));
            } catch (JSONException e) {
                Log.e(LOG_TAG, getString(R.string.json_exception) + e);
            }
        }

        public String[] getLabels() {
            String[] mResults = new String[mLabel.size()];
            mResults = mLabel.toArray(mResults);

            String previousLabel = mResults[0];

            for (int i = 1; i < mResults.length - 1; i++) {
                if (previousLabel.equalsIgnoreCase(mResults[i])) {
                    mResults[i] = "";
                } else {
                    previousLabel = mResults[i];
                }
            }

            return mResults;
        }

        public float[] getPoints() {
            float[] mResults = new float[mValue.size()];
            int i = 0;

            for (Float f : mValue) {
                mResults[i++] = (f != null ? f : f.NaN);
            }

            return mResults;
        }

        public void setMinimum(String minimum) {

            Integer result = 0;

            try {
                String value = minimum.substring(0, minimum.indexOf("."));
                result = Integer.valueOf(value);
            } catch (NumberFormatException e) {
                Log.e(LOG_TAG, e.toString());
            }

            if (result < 100) {
                mMinimum = 0;
            } else {
                int length = minimum.indexOf(".") - 1;
                Integer firstDigit = Integer.valueOf(minimum.substring(0, 1));
                mMinimum = (int) (firstDigit * (Math.pow(10.0, (double) length)));
            }
        }

        public void setMaximum(String maximum) {

            Integer result = 1000;

            try {
                String value = maximum.substring(0, maximum.indexOf("."));
                result = Integer.valueOf(value);
            } catch (NumberFormatException e) {
                Log.e(LOG_TAG, e.toString());
            }

            int length = maximum.indexOf(".") - 1;
            Integer firstDigit = Integer.valueOf(maximum.substring(0, 1));
            mMaximum = (int) ((firstDigit + 1) * (Math.pow(10.0, (double) length)));
        }

        public int getMinimum() {
            return mMinimum;
        }

        public int getMaximum() {
            return mMaximum;
        }
    }
}
