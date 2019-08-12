package com.alphalabs.expensed;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Vibrator;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.StringTokenizer;

public class MainActivity extends AppCompatActivity {

    CameraSource css;
    TextView tv;
    static Double cost = 0.0;
    public static Cursor cx;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Refresh_amt();
        final SwipeRefreshLayout pullToRefresh = findViewById(R.id.pullToRefresh);
        pullToRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                Refresh_amt();
                pullToRefresh.setRefreshing(false);
            }
        });
    }

    public boolean isSmsPermissionGranted() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Request runtime SMS permission
     */
    private void requestReadAndSendSmsPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_SMS)) {
            // You may display a non-blocking explanation here, read more in the documentation:
            // https://developer.android.com/training/permissions/requesting.html
        }
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_SMS}, 1);
    }

    public void Refresh_amt() {
        if (!isSmsPermissionGranted()) {
            requestReadAndSendSmsPermission();
        }
        Cursor cursor = getContentResolver().query(Uri.parse("content://sms/inbox"), null, null, null, null);
        cx = cursor;
        int countr = 0;
        cost = 0.0;
        LinearLayout placeHolder = (LinearLayout) findViewById(R.id.linear);
        placeHolder.removeAllViews();
        if (cursor.moveToFirst()) { // must check the result to prevent exception
            do {
                boolean flag_date=false,flag_paytm=false,flag_sbi=false;

                String lm = "";
                Double cost_temp=0.0;
                Date message_date = null, todays_date = new Date();
                SimpleDateFormat df = new SimpleDateFormat("M");
                int idx_req = cursor.getColumnIndex("date");
                message_date = new Date(cursor.getLong(idx_req));
                int d1 = Integer.parseInt(df.format(message_date)), d2 = Integer.parseInt(df.format(todays_date));
                if (d1==d2)
                {
                    flag_date = true;
                    lm = new SimpleDateFormat("hh.mmaa, dd.MM.yyyy").format(message_date);
                    lm+= " via ";
                }
                else if(d1<d2) break;
                if (flag_date) {

                    idx_req = cursor.getColumnIndex("address");
                    if(cursor.getString(idx_req).toLowerCase().contains("paytm"))
                        flag_paytm = true;
                    if(flag_paytm) {
                        idx_req = cursor.getColumnIndex("body");
                        String chk2 = cursor.getString(idx_req);

                        StringTokenizer st = new StringTokenizer(chk2, " ");
                        boolean f = false;
                        if(chk2.toLowerCase().contains("rs.")) {
                            if(chk2.toLowerCase().contains("paid"))
                            {
                                while (st.hasMoreTokens()) {
                                    // to add value to total cost
                                    String temp = st.nextToken();
                                    if(temp.toLowerCase().contains("paid"))
                                    {
                                        temp = st.nextToken();
                                        if (temp.toLowerCase().contains("rs.")) {
                                            StringTokenizer ss = new StringTokenizer(temp, ".");
                                            ss.nextToken();
                                            cost_temp += Integer.parseInt(ss.nextToken());
                                            if(ss.hasMoreTokens()) cost_temp += Integer.parseInt(ss.nextToken())*0.01;
                                            break;
                                        }
                                    }
                                }
                                f = true;
                            }
                            else if(chk2.toLowerCase().contains("transferred"))
                            {
                                while (st.hasMoreTokens()) {
                                    // to add value to total cost
                                    String temp = st.nextToken();
                                    if(temp.toLowerCase().contains("rs."))
                                    {
                                        temp = st.nextToken();
                                        StringTokenizer ss = new StringTokenizer(temp, ".");
                                        //Toast.makeText(this, ""+temp, Toast.LENGTH_SHORT).show();
                                        cost_temp += Integer.parseInt(ss.nextToken());
                                        if(ss.hasMoreTokens()) cost_temp += Integer.parseInt(ss.nextToken())*0.01;
                                        break;
                                    }
                                }
                                f = true;
                            }
                            if(f) {
                                String rec ="";
                                if(chk2.toLowerCase().contains("at"))
                                {
                                    rec = chk2.split(" at")[0].split("to ")[1];
                                }
                                LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                                final View rowView = inflater.inflate(R.layout.add_layout, null,false);
                                // Add the new row before the add field button.
                                TextView tv = rowView.findViewById(R.id.reciever);
                                tv.setText(rec);
                                tv = rowView.findViewById(R.id.datetime);
                                tv.setText(lm.toLowerCase());
                                tv = rowView.findViewById(R.id.amount);
                                tv.setText(cost_temp+"");
                                tv = rowView.findViewById(R.id.mode1);
                                tv.setTextColor(Color.parseColor("#042D6C"));
                                tv.setText("Pay");
                                tv = rowView.findViewById(R.id.mode2);
                                tv.setTextColor(Color.parseColor("#00B5EB"));
                                tv.setText("tm");
                                placeHolder.addView(rowView);
                                countr++;
                                cost+=cost_temp;
                            }
                        }

                    }

                    if(cursor.getString(cursor.getColumnIndex("body")).toLowerCase().contains("sbi"))
                        flag_sbi = true;
                    if(flag_sbi) {
                        idx_req = cursor.getColumnIndex("body");
                        String chk2 = cursor.getString(idx_req);

                        StringTokenizer st = new StringTokenizer(chk2, " ");
                        boolean f = false;
                        if (chk2.toLowerCase().contains("rs")) {
                            if (chk2.toLowerCase().contains("debited")) {
                                while (st.hasMoreTokens()) {
                                    // to add value to total cost
                                    String temp = st.nextToken();
                                    if (temp.toLowerCase().contains("rs")) {
                                        StringTokenizer ss = new StringTokenizer(temp.toLowerCase().split("rs")[1], ".");
                                        cost_temp += Integer.parseInt(ss.nextToken());
                                        if (ss.hasMoreTokens())
                                            cost_temp += Integer.parseInt(ss.nextToken()) * 0.01;

                                        String rec = "";
                                        if (chk2.toLowerCase().contains("ref no ")) {
                                            rec = chk2.toLowerCase().split("ref no ")[1].split(" ")[0];
                                            rec = "Ref No." + rec;
                                        }
                                        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                                        final View rowView = inflater.inflate(R.layout.add_layout, null, false);
                                        // Add the new row before the add field button.
                                        TextView tv = rowView.findViewById(R.id.reciever);
                                        tv.setText(rec);
                                        tv = rowView.findViewById(R.id.datetime);
                                        tv.setText(lm.toLowerCase());
                                        tv = rowView.findViewById(R.id.amount);
                                        tv.setText(cost_temp + "");
                                        tv = rowView.findViewById(R.id.mode1);
                                        tv.setTextColor(Color.parseColor("#242978"));
                                        tv.setText("SBI");
                                        tv = rowView.findViewById(R.id.mode2);
                                        tv.setTextColor(Color.parseColor("#696A6C"));
                                        tv.setText(" UPI");
                                        placeHolder.addView(rowView);
                                        countr++;
                                        cost += cost_temp;
                                    }
                                }
                            }

                        }
                    }
                }
                TextView t3 = (TextView) findViewById(R.id.answ);
                t3.setText(cost+"");
            } while (cursor.moveToNext());
        }
        if (countr==0)
        {
            LinearLayout iv = findViewById(R.id.vg);
            iv.setVisibility(View.VISIBLE);
        }
        else findViewById(R.id.vg).setVisibility(View.GONE);
    }


    public void view_stats(View v)
    {
        Intent intent = new Intent(this,StatisticsActivity.class);
        startActivity(intent);
    }

    void qr() {


        SurfaceView sv = (SurfaceView) findViewById(R.id.camerapreview);
        tv = (TextView) findViewById(R.id.textView_1);

        BarcodeDetector bd = new BarcodeDetector.Builder(this).setBarcodeFormats(Barcode.QR_CODE).build();

        css = new CameraSource.Builder(this, bd).setRequestedPreviewSize(400, 300).build();

        sv.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[] {Manifest.permission.CAMERA},1);
                }
                try {
                    css.start(holder);}
                catch (Exception ex){
                    Toast.makeText(MainActivity.this, String.valueOf(ex), Toast.LENGTH_LONG).show();
                }

            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                css.stop();
            }
        });

        bd.setProcessor(new Detector.Processor<Barcode>() {
            @Override
            public void release() {

            }

            @Override
            public void receiveDetections(Detector.Detections<Barcode> detections) {

                final SparseArray<Barcode> qrCodes = detections.getDetectedItems();
                if(qrCodes.size()!=0)
                {
                    tv.post(new Runnable() {
                        @Override
                        public void run() {
                            Vibrator vib = (Vibrator)getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
                            vib.vibrate(1000);
                            try{tv.setText(qrCodes.valueAt((0)).displayValue);}
                            catch (Exception ex){
                                Toast.makeText(MainActivity.this, String.valueOf(ex), Toast.LENGTH_LONG).show();
                            }
                            String urlString = qrCodes.valueAt((0)).displayValue;
                            String url = "http://m.p-y.tm/pay";
                            Intent i = new Intent(Intent.ACTION_VIEW);
                            i.setData(Uri.parse(url));
                            startActivity(i);
                        }
                    });
                }
            }
        });
    }
}
