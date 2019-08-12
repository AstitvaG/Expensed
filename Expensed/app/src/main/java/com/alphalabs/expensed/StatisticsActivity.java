package com.alphalabs.expensed;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.db.chart.view.LineChartView;
import com.hadiidbouk.charts.BarData;
import com.hadiidbouk.charts.ChartProgressBar;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.StringTokenizer;

public class StatisticsActivity extends AppCompatActivity {

    protected static long limit=5000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);
        ((TextView)findViewById(R.id.answ)).setText(Calculate_cost(0)+"");
        findViewById(R.id.chart_card).setVisibility(View.GONE);
        (new LineCardTwo((CardView)findViewById(R.id.chart_card))).init();
        final EditText ed1 = findViewById(R.id.mw),ed2 =findViewById(R.id.dw);
        ed1.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                Typeface type;
                if(hasFocus)
                {
                    findViewById(R.id.monthlytable).setVisibility(View.VISIBLE);
                    Create_month_chart();
                    type = Typeface.createFromAsset(getAssets(),"fonts/qanelas_soft_bold.otf");
                    ed1.setTypeface(type);
                }
                else
                {
                    findViewById(R.id.monthlytable).setVisibility(View.GONE);
                    type = Typeface.createFromAsset(getAssets(),"fonts/qanelas_soft_medium.otf");
                    ed1.setTypeface(type);
                }
            }
        });
        ed2.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                Typeface type;
                if(hasFocus)
                {
                    CardView cv = findViewById(R.id.chart_card);
                    cv.setVisibility(View.VISIBLE);
                    (new LineCardTwo(cv)).diss();
                    type = Typeface.createFromAsset(getAssets(),"fonts/qanelas_soft_bold.otf");
                    ed2.setTypeface(type);
                }
                else
                {
                    findViewById(R.id.chart_card).setVisibility(View.GONE);
                    type = Typeface.createFromAsset(getAssets(),"fonts/qanelas_soft_medium.otf");
                    ed2.setTypeface(type);
                }
            }
        });
        ed1.requestFocus();

    }

    private void Create_month_chart()
    {

        ArrayList<BarData> dataList = new ArrayList<>();

        LocalDate dte_curr = LocalDate.now(),dte_prev1,dte_prev2;
        dte_prev1 = dte_curr.minusMonths(1);
        dte_prev2 = dte_curr.minusMonths(2);

        Float f= 10*Float.valueOf(Calculate_cost(2)/limit+"");
        String s =Math.round(f*limit/10)+"";
        if(f<=2.0) f = 2f;
        BarData data = new BarData(dte_prev2.getMonth().toString(), f, s);
        dataList.add(data);

        f= 10*Float.valueOf(Calculate_cost(1)/limit+"");
        s = Math.round(f*limit/10)+"";
        if(f<=2.0) f = 2f;
        data = new BarData(dte_prev1.getMonth().toString(), f, s);
        dataList.add(data);

        f= 10*Float.valueOf(Calculate_cost(0)/limit+"");
        s = Math.round(f*limit/10)+"";
        if(f<=2.0) f = 2f;
        data = new BarData(dte_curr.getMonth().toString(), f, s);
        dataList.add(data);

        ChartProgressBar mChart = findViewById(R.id.monthlytable);
        mChart.setDataList(dataList);
        mChart.build();

    }


    private double Calculate_cost(int month_req) {
        Double reqMonthCost = 0.0;
        Cursor cursor = getContentResolver().query(Uri.parse("content://sms/inbox"), null, null, null, null);
        if (cursor.moveToFirst()) { // must check the result to prevent exception
            do {
                boolean flag_date=false,flag_paytm=false,flag_sbi=false;

                Double cost_temp=0.0;
                Date message_date = null, todays_date = new Date();
                SimpleDateFormat df = new SimpleDateFormat("M");
                int idx_req = cursor.getColumnIndex("date");
                message_date = new Date(cursor.getLong(idx_req));
                int d1 = Integer.parseInt(df.format(message_date)), d2 = Integer.parseInt(df.format(todays_date));
                if (d1+month_req==d2)
                {
                    flag_date = true;
                }
                else if(d1+month_req<d2) break;
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
                                reqMonthCost+=cost_temp;
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

                                        reqMonthCost += cost_temp;
                                    }
                                }
                            }

                        }
                    }
                }
            } while (cursor.moveToNext());
        }
        return reqMonthCost;
    }


}


