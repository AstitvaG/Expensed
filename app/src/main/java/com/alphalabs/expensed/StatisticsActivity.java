package com.alphalabs.expensed;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.hadiidbouk.charts.BarData;
import com.hadiidbouk.charts.ChartProgressBar;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;

public class StatisticsActivity extends AppCompatActivity {

    protected static long limit = 5000;
    static Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);
        context = this;
        ((TextView) findViewById(R.id.answ)).setText(Calculate_cost(0) + "");
        findViewById(R.id.chart_card).setVisibility(View.GONE);
        (new MonthlyChartBuilder((CardView) findViewById(R.id.chart_card))).init();
        final EditText ed1 = findViewById(R.id.mw), ed2 = findViewById(R.id.dw);
        ed1.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                Typeface type;
                if (hasFocus) {
                    findViewById(R.id.monthlytable).setVisibility(View.VISIBLE);
                    Create_month_chart();
                    type = Typeface.createFromAsset(getAssets(), "fonts/qanelas_soft_bold.otf");
                    ed1.setTypeface(type);
                } else {
                    findViewById(R.id.monthlytable).setVisibility(View.GONE);
                    type = Typeface.createFromAsset(getAssets(), "fonts/qanelas_soft_medium.otf");
                    ed1.setTypeface(type);
                }
            }
        });
        ed2.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                Typeface type;
                if (hasFocus) {
                    CardView cv = findViewById(R.id.chart_card);
                    cv.setVisibility(View.VISIBLE);
                    (new MonthlyChartBuilder(cv)).diss();
                    type = Typeface.createFromAsset(getAssets(), "fonts/qanelas_soft_bold.otf");
                    ed2.setTypeface(type);
                } else {
                    findViewById(R.id.chart_card).setVisibility(View.GONE);
                    type = Typeface.createFromAsset(getAssets(), "fonts/qanelas_soft_medium.otf");
                    ed2.setTypeface(type);
                }
            }
        });
        ed1.requestFocus();

    }

    private void Create_month_chart() {

        ArrayList<BarData> dataList = new ArrayList<>();

        LocalDate dte_curr = LocalDate.now(), dte_prev1, dte_prev2;
        dte_prev1 = dte_curr.minusMonths(1);
        dte_prev2 = dte_curr.minusMonths(2);

        Float f = 10 * Float.valueOf(Calculate_cost(2) / limit + "");
        String s = Math.round(f * limit / 10) + "";
        if (f <= 2.0) f = 2f;
        BarData data = new BarData(dte_prev2.getMonth().toString(), f, s);
        dataList.add(data);

        f = 10 * Float.valueOf(Calculate_cost(1) / limit + "");
        s = Math.round(f * limit / 10) + "";
        if (f <= 2.0) f = 2f;
        data = new BarData(dte_prev1.getMonth().toString(), f, s);
        dataList.add(data);

        f = 10 * Float.valueOf(Calculate_cost(0) / limit + "");
        s = Math.round(f * limit / 10) + "";
        if (f <= 2.0) f = 2f;
        data = new BarData(dte_curr.getMonth().toString(), f, s);
        dataList.add(data);

        ChartProgressBar mChart = findViewById(R.id.monthlytable);
        mChart.setDataList(dataList);
        mChart.build();

    }


    private double Calculate_cost(int month_req) {
        Double reqMonthCost = 0.0;
        DbConnect db = new DbConnect(this);
        String baapstr = db.getdb();
        Cursor cursor = getContentResolver().query(Uri.parse("content://sms/inbox"), null, null, null, null);
        if (cursor.moveToFirst()) { // must check the result to prevent exception
            do {
                boolean flag_date = false, flag_paytm = false, flag_sbi = false, flag_pnb = false, flag_other = false;
                boolean flag_deleted = false;
                Double cost_temp = 0.0;
                Date message_date = null, todays_date = new Date();
                SimpleDateFormat df = new SimpleDateFormat("M");
                int idx_req = cursor.getColumnIndex("date");
                message_date = new Date(cursor.getLong(idx_req));
                int d1 = Integer.parseInt(df.format(message_date)), d2 = Integer.parseInt(df.format(todays_date));
                if (d1 + month_req == d2) {
                    flag_date = true;
                } else if (d1 + month_req < d2) break;

                if (baapstr.contains(message_date.getTime() + " -- -1"))
                    continue;

                if (flag_date) {

                    idx_req = cursor.getColumnIndex("body");
                    String msg_body = cursor.getString(idx_req);

                    String[] currency = {"inr.", "rs.", "rs", "inr"};
                    int curr_id = -1;
                    String[] checker = {"debit", "paid", "sent"};
                    int check_id = -1;
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
                    if (!flag_deleted && baapstr.contains(message_date.getTime() + "")) {
                        cost_temp = Double.parseDouble(baapstr.split(message_date.getTime() + " -- ")[1].split(" ")[0]);
                    }
                    reqMonthCost+=cost_temp;
                }
            }
            while (cursor.moveToNext());
        }
        return reqMonthCost;
    }


}


