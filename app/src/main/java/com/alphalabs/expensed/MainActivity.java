package com.alphalabs.expensed;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.StringTokenizer;

public class MainActivity extends AppCompatActivity {

    public static Cursor cx;
    static Double cost = 0.0;
    CameraSource css;
    TextView tv;
    //Resources used by editView_popup and long_press_menu_options methods.
    View editOptions_popupView;
    PopupWindow editOptions_popupWindow;
    Long entry_id;
    String entry_val;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_new);
        Refresh_amt_new();
        final SwipeRefreshLayout pullToRefresh = findViewById(R.id.pullToRefresh);
        pullToRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                Refresh_amt_new();
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
    //

    public void Refresh_amt1() {
        if (!isSmsPermissionGranted()) {
            requestReadAndSendSmsPermission();
        }
        Cursor cursor = getContentResolver().query(Uri.parse("content://sms/inbox"), null, null, null, null);
        cx = cursor;
        int countr = 0;
        cost = 0.0;
        DbConnect db = new DbConnect(this);
        String baapstr = db.getdb();
        LinearLayout placeHolder = (LinearLayout) findViewById(R.id.linear);
        placeHolder.removeAllViews();
        if (cursor.moveToFirst()) { // must check the result to prevent exception
            do {
                boolean flag_date = false, flag_paytm = false, flag_sbi = false;

                String lm = "";
                Double cost_temp = 0.0;
                Date message_date = null, todays_date = new Date();
                SimpleDateFormat df = new SimpleDateFormat("M");
                int idx_req = cursor.getColumnIndex("date");
                message_date = new Date(cursor.getLong(idx_req));
                int d1 = Integer.parseInt(df.format(message_date)), d2 = Integer.parseInt(df.format(todays_date));
                if (d1 == d2) {
                    flag_date = true;
                    lm = new SimpleDateFormat("hh.mmaa, dd.MM.yyyy").format(message_date);
                    lm += " via ";
                } else if (d1 < d2) break;

                if (baapstr.contains(message_date.getTime() + " -- -1"))
                    continue;

                if (flag_date) {

                    idx_req = cursor.getColumnIndex("address");
                    if (cursor.getString(idx_req).toLowerCase().contains("paytm"))
                        flag_paytm = true;
                    if (flag_paytm) {
                        idx_req = cursor.getColumnIndex("body");
                        String chk2 = cursor.getString(idx_req);

                        StringTokenizer st = new StringTokenizer(chk2, " ");
                        boolean f = false;
                        if (chk2.toLowerCase().contains("rs.")) {
                            if (chk2.toLowerCase().contains("paid")) {
                                while (st.hasMoreTokens()) {
                                    // to add value to total cost
                                    String temp = st.nextToken();
                                    if (temp.toLowerCase().contains("paid")) {
                                        temp = st.nextToken();
                                        if (temp.toLowerCase().contains("rs.")) {
                                            StringTokenizer ss = new StringTokenizer(temp, ".");
                                            ss.nextToken();
                                            cost_temp += Integer.parseInt(ss.nextToken());
                                            if (ss.hasMoreTokens())
                                                cost_temp += Integer.parseInt(ss.nextToken()) * 0.01;
                                            break;
                                        }
                                    }
                                }
                                f = true;
                            } else if (chk2.toLowerCase().contains("transferred")) {
                                while (st.hasMoreTokens()) {
                                    // to add value to total cost
                                    String temp = st.nextToken();
                                    if (temp.toLowerCase().contains("rs.")) {
                                        temp = st.nextToken();
                                        StringTokenizer ss = new StringTokenizer(temp, ".");
                                        //Toast.makeText(this, ""+temp, Toast.LENGTH_SHORT).show();
                                        cost_temp += Integer.parseInt(ss.nextToken());
                                        if (ss.hasMoreTokens())
                                            cost_temp += Integer.parseInt(ss.nextToken()) * 0.01;
                                        break;
                                    }
                                }
                                f = true;
                            }
                            if (f) {
                                if (baapstr.contains(message_date.getTime() + ""))
                                    cost_temp = Double.parseDouble(baapstr.split(message_date.getTime() + " -- ")[1].split(" ")[0]);
                                String rec = "";
                                if (chk2.toLowerCase().contains("at")) {
                                    rec = chk2.split(" at")[0].split("to ")[1];
                                }
                                LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                                View rowView = inflater.inflate(R.layout.add_layout, null, false);
                                rowView.setTag(message_date.getTime() + "");
                                // Add the new row before the add field button.
                                TextView tv = rowView.findViewById(R.id.reciever);
                                tv.setText(rec);
                                tv = rowView.findViewById(R.id.datetime);
                                tv.setText(lm.toLowerCase());
                                tv = rowView.findViewById(R.id.amount);
                                tv.setText(cost_temp + "");
                                tv = rowView.findViewById(R.id.mode1);
                                tv.setTextColor(Color.parseColor("#042D6C"));
                                tv.setText("Pay");
                                tv = rowView.findViewById(R.id.mode2);
                                tv.setTextColor(Color.parseColor("#00B5EB"));
                                tv.setText("tm");
                                onLongClick_detailsPane(rowView);
                                placeHolder.addView(rowView);
                                countr++;
                                cost += cost_temp;
                            }
                        }

                    }

                    if (cursor.getString(cursor.getColumnIndex("body")).toLowerCase().contains("sbi"))
                        flag_sbi = true;
                    if (flag_sbi) {
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

                                        if (baapstr.contains(message_date.getTime() + "")) {
                                            cost_temp = Double.parseDouble(baapstr.split(message_date.getTime() + " -- ")[1].split(" ")[0]);
                                        }

                                        String rec = "";
                                        if (chk2.toLowerCase().contains("ref no ")) {
                                            rec = chk2.toLowerCase().split("ref no ")[1].split(" ")[0];
                                            rec = "Ref No." + rec;
                                        }
                                        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                                        View rowView = inflater.inflate(R.layout.add_layout, null, false);
                                        // Add the new row before the add field button.
                                        rowView.setTag(message_date.getTime() + "");
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
                                        onLongClick_detailsPane(rowView);
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
                t3.setText(String.format("%.2f", cost));
            } while (cursor.moveToNext());
        }
        if (countr == 0) {
            LinearLayout iv = findViewById(R.id.vg);
            iv.setVisibility(View.VISIBLE);
        } else findViewById(R.id.vg).setVisibility(View.GONE);
    }

    //Sets onLongClick action of detailsPane
    void onLongClick_detailsPane(View v) {
        v.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                editView_popup(v, 1);
                ((TextView) editOptions_popupView.findViewById(R.id.id)).setText(v.getTag() + "");
                ((TextView) editOptions_popupView.findViewById(R.id.val)).setText(((TextView) v.findViewById(R.id.amount)).getText() + "");
                return true;
            }
        });
    }

    //Adds the editoTView layout
    public void editView_popup(View v, int tag) {
        int iddx = 0;
        if (tag == 1) iddx = R.layout.edit_view_popup;
        else iddx = R.layout.split_popup;

        // inflate the layout of the popup window
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        editOptions_popupView = inflater.inflate(iddx, null, false);
        int width = LinearLayout.LayoutParams.MATCH_PARENT;
        int height = LinearLayout.LayoutParams.WRAP_CONTENT;
        boolean focusable = true; // lets taps outside the popup also dismiss it
        editOptions_popupWindow = new PopupWindow(editOptions_popupView, width, height, focusable);
        editOptions_popupWindow.setAnimationStyle(R.style.PopupAnimation);
        editOptions_popupWindow.showAtLocation(v, Gravity.BOTTOM, 0, 0);
        editOptions_popupWindow.setBackgroundDrawable(null);
        editOptions_popupWindow.showAsDropDown(v);
        View container = (View) editOptions_popupWindow.getContentView().getParent();
        WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        WindowManager.LayoutParams p = (WindowManager.LayoutParams) container.getLayoutParams();
        p.flags |= WindowManager.LayoutParams.FLAG_DIM_BEHIND;
        p.dimAmount = 0.8f;
        wm.updateViewLayout(container, p);
        //
    }

    public void view_stats(View v) {
        Intent intent = new Intent(this, StatisticsActivity.class);
        startActivity(intent);
//        final String EXTRA_NAME = "de.blinkt.openvpn.api.la";
//
//        Intent shortcutIntent = new Intent(Intent.ACTION_MAIN);
//        shortcutIntent.setClassName("de.blinkt.openvpn", "de.blinkt.openvpn.api.ConnectVPN");
//        shortcutIntent.putExtra(EXTRA_NAME,"upb ssl");
//        startActivity(shortcutIntent);
    }

    public void delete(View v) {
        DbConnect db = new DbConnect(this);
        String a = ((TextView) editOptions_popupView.findViewById(R.id.id)).getText() + "";
        db.addEntry(a, -1 + "");
        editOptions_popupWindow.dismiss();
        Refresh_amt_new();
    }

    public void split(View v) {
        String s = v.getTag().toString();
        if (s.equals("1")) {
            entry_id = Long.parseLong(((TextView) editOptions_popupView.findViewById(R.id.id)).getText() + "");
            entry_val = ((TextView) editOptions_popupView.findViewById(R.id.val)).getText() + "";
            editOptions_popupWindow.dismiss();
            editView_popup(v, 2);
            TextView vx = editOptions_popupView.findViewById(R.id.amt);
            vx.setText("Rs. " + entry_val);
            vx = editOptions_popupView.findViewById(R.id.finalamt);
            vx.setText("Rs. " + entry_val);
        } else {
            String sx = v.getRootView().findViewById(R.id.peeps).getTag().toString();
//            Toast.makeText(this, sx, Toast.LENGTH_SHORT).show();
            DbConnect db = new DbConnect(this);
            db.addEntry(entry_id + "", ((TextView) editOptions_popupView.findViewById(R.id.finalamt)).getText().toString().split("Rs. ")[1]);
            editOptions_popupWindow.dismiss();
            Refresh_amt_new();
        }

    }

    public void change(View v) {
        String[] numNames = {"", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten", "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen", "seventeen", "eighteen", "nineteen"};
        TextView tv = v.getRootView().findViewById(R.id.peeps);
        int ct = Integer.parseInt(tv.getTag().toString());
        String s = v.getTag().toString();
        Vibrator vx = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vx.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE));
        } else
            vx.vibrate(100);

        if (s.equals("1")) {
            tv.setTextColor(Color.parseColor("#571048"));
            if (ct < 15) ct++;
            else tv.setTextColor(Color.parseColor("#444444"));
        } else {
            tv.setTextColor(Color.parseColor("#571048"));
            if (ct > 1) ct--;
            else tv.setTextColor(Color.parseColor("#444444"));
        }
        tv.setText(numNames[ct].toUpperCase());
        tv.setTag(ct + "");
        Double cst = Double.parseDouble((((TextView) v.getRootView().findViewById(R.id.amt)).getText().toString().split("Rs. ")[1]));
        tv = v.getRootView().findViewById(R.id.finalamt);
        tv.setText("Rs. " + String.format("%.2f", cst / ct));
    }


    void setdetails(View rowView, String mode1_text, String mode1_color, String mode2_text, String mode2_color) {
        tv = rowView.findViewById(R.id.mode1);
        tv.setTextColor(Color.parseColor(mode1_color));
        tv.setText(mode1_text);
        tv = rowView.findViewById(R.id.mode2);
        tv.setTextColor(Color.parseColor(mode2_color));
        tv.setText(mode2_text);
    }

    public void Refresh_amt_new() {
        if (!isSmsPermissionGranted()) {
            requestReadAndSendSmsPermission();
        }
        Cursor cursor = getContentResolver().query(Uri.parse("content://sms/inbox"), null, null, null, null);
        cx = cursor;
        int countr = 0;
        cost = 0.0;
        DbConnect db = new DbConnect(this);
        String baapstr = db.getdb();
        LinearLayout placeHolder = (LinearLayout) findViewById(R.id.linear);
        placeHolder.removeAllViews();
        if (cursor.moveToFirst()) { // must check the result to prevent exception
            do {
                boolean flag_date = false, flag_paytm = false, flag_sbi = false, flag_pnb = false, flag_other = false;
                boolean flag_deleted = false;
                String lm = "";
                Double cost_temp = 0.0;
                Date message_date = null, todays_date = new Date();
                SimpleDateFormat df = new SimpleDateFormat("M");
                int idx_req = cursor.getColumnIndex("date");
                message_date = new Date(cursor.getLong(idx_req));
                int d1 = Integer.parseInt(df.format(message_date)), d2 = Integer.parseInt(df.format(todays_date));
                if (d1 == d2) {
                    flag_date = true;
                    lm = new SimpleDateFormat("hh.mmaa, dd.MM.yyyy").format(message_date);
                    lm += " via ";
                } else if (d1 < d2) break;

                if (baapstr.contains(message_date.getTime() + " -- -1"))
                    flag_deleted = true;

                if (flag_date) {

                    idx_req = cursor.getColumnIndex("address");
                    if (cursor.getString(idx_req).toLowerCase().contains("paytm"))
                        flag_paytm = true;
                    else if (cursor.getString(cursor.getColumnIndex("body")).toLowerCase().contains("sbi"))
                        flag_sbi = true;
                    else if ((cursor.getString(idx_req).toLowerCase().contains("pnb")) || (cursor.getString(cursor.getColumnIndex("body")).toLowerCase().contains("pnb")))
                        flag_pnb = true;
                    else
                        flag_other = true;


                    idx_req = cursor.getColumnIndex("body");
                    String msg_body = cursor.getString(idx_req);

                    String[] currency = {"inr.", "rs.", "rs", "inr"};
                    int curr_id = -1;
                    String[] checker = {"debit", "paid", "sent"};
                    int check_id = -1;
                    String[] ids = {"reference number", "ref number", "ref no", "reference no", "order id"};
                    int id_id = -1;
                    String[] company = {"Swiggy", "Zomato", "Uber Eats", "Uber"};
                    int comp_id = -1;

                    for (int i = 0; i < currency.length; i++) {
                        if (msg_body.toLowerCase().contains(currency[i])) {
                            curr_id = i;
                            break;
                        }
                    }
                    for (int i = 0; i < checker.length; i++) {
                        if (msg_body.toLowerCase().contains(checker[i])) {
                            check_id = i;
                            break;
                        }
                    }
                    for (int i = 0; i < ids.length; i++) {
                        if (msg_body.toLowerCase().contains(ids[i])) {
                            id_id = i;
                            break;
                        }
                    }
                    for (int i = 0; i < company.length; i++) {
                        if (msg_body.toLowerCase().contains(company[i].toLowerCase())) {
                            comp_id = i;
                            break;
                        }
                    }

                    if (curr_id == -1 || check_id == -1) {
                        continue;
                    }


                    //cost_handler();
                    String regex = "\\s|\\t|,|;|\\.|\\?|!|-|:|@|\\[|\\]|\\(|\\)|\\{|\\}|_|\\*|/";

                    String[] temp_curr = msg_body.toLowerCase().split(currency[curr_id])[0].trim().split(regex);
                    try {
                        cost_temp += Double.parseDouble(temp_curr[temp_curr.length - 1].trim());
                    } catch (Exception ex) {
                        try {
                            temp_curr = msg_body.toLowerCase().split(currency[curr_id])[1].trim().split(regex);
                            cost_temp += Double.parseDouble(temp_curr[0].trim());
                        } catch (Exception e) {
                            continue;
                        }
                    }
                    double cost_temp_1 = 0d;
                    boolean isEdited = false;
                    if (!flag_deleted && baapstr.contains(message_date.getTime() + "")) {
                        cost_temp_1 = Double.parseDouble(baapstr.split(message_date.getTime() + " -- ")[1].split(" ")[0]);
                        if (!String.format("%.2f", cost_temp_1).equals(String.format("%.2f", cost_temp))) {
                            double temp = cost_temp;
                            cost_temp = Double.parseDouble(baapstr.split(message_date.getTime() + " -- ")[1].split(" ")[0]);
                            cost_temp_1 = temp;
                            isEdited = true;
                        }
                    }

                    //sender/receiver_handler();

                    String rec = "";
                    if (flag_sbi)
                        if (msg_body.toLowerCase().contains(ids[id_id])) {
                            rec = msg_body.toLowerCase().split(ids[id_id])[1].trim().split(regex)[0];
                            rec = "Ref No." + rec;
                        }
                    if (flag_paytm) {
                        if (comp_id > -1)
                            rec = company[comp_id];
                        else {
                            if (msg_body.toLowerCase().contains("at")) {
                                rec = msg_body.split(" at")[0].split("to ")[1];
                            } else if (msg_body.toLowerCase().contains("on") && msg_body.toLowerCase().contains("to")) {
                                rec = msg_body.split("on")[0].trim().split("to")[1];
                            }
                        }
                    }
                    if (flag_other || flag_pnb) {
                        if (id_id > -1) {
                            rec = msg_body.toLowerCase().split(ids[id_id])[1].trim().split(regex)[0];
                            rec = "Ref No." + rec;
                        } else if (cursor.getString(cursor.getColumnIndex("body")).trim()
                                .toLowerCase().contains("cent")) {
                            rec = "CBI Payment";
                        } else
                            rec = "Card\npayment";
                    }

                    LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    View rowView;
                    if (!flag_deleted)
                        rowView = inflater.inflate(R.layout.add_layout, null, false);
                    else
                        rowView = inflater.inflate(R.layout.add_layout_deleted, null, false);
                    // Add the new row before the add field button.
                    rowView.setTag(message_date.getTime() + "");
                    TextView tv = rowView.findViewById(R.id.reciever);
                    tv.setText(rec.trim());
                    tv = rowView.findViewById(R.id.datetime);
                    tv.setText(lm.toLowerCase());
                    tv = rowView.findViewById(R.id.amount);
                    tv.setText(cost_temp + "");
                    if (isEdited) {
                        tv.setTextColor(Color.parseColor("#00B5EB"));
                        tv = rowView.findViewById(R.id.prevValue);
                        tv.setVisibility(View.VISIBLE);
                        tv.setText("Rs. " + String.format("%.2f", cost_temp_1));
                    }
                    if (flag_sbi)
                        setdetails(rowView, "SBI", "#242978", " UPI", "#696A6C");
                    else if (flag_paytm)
                        setdetails(rowView, "Pay", "#042D6C", "tm", "#00B5EB");
                    else if (flag_pnb && id_id >= 0)
                        setdetails(rowView, "PNB", "#710000", " UPI", "#FAB909");
                    else if (flag_pnb)
                        setdetails(rowView, "PNB", "#710000", " card", "#FAB909");
                    else if (flag_other && id_id > -1)
                        setdetails(rowView, "UPI", "#F87708", " mode", "#00883A");
                    else if (rec.equals("CBI Payment"))
                        setdetails(rowView, "CBI", "#CA1044", " mode", "#0075BB");
                    else if (flag_other)
                        setdetails(rowView, "Card", "#000000", " mode", "#000000");
                    onLongClick_detailsPane(rowView);
                    placeHolder.addView(rowView);
                    countr++;
                    if (!flag_deleted) cost += cost_temp;
                }
                TextView t3 = (TextView) findViewById(R.id.answ);
                t3.setText(String.format("%.2f", cost));
            } while (cursor.moveToNext());
        }
        if (countr == 0) {
            LinearLayout iv = findViewById(R.id.vg);
            iv.setVisibility(View.VISIBLE);
        } else findViewById(R.id.vg).setVisibility(View.GONE);
    }

}

