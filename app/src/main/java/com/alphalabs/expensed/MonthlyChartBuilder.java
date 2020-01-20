package com.alphalabs.expensed;

import android.database.Cursor;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.support.v7.widget.CardView;

import com.db.chart.animation.Animation;
import com.db.chart.model.LineSet;
import com.db.chart.model.Point;
import com.db.chart.renderer.AxisRenderer;
import com.db.chart.util.Tools;
import com.db.chart.view.LineChartView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;


public class MonthlyChartBuilder extends CardController {


    private final LineChartView mChart;

    private final String[] mLabels =
            {"", "", "", "", "START", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
                    "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "FINISH", "", "", "",
                    ""};

    private float[][] mValues =
            {{0, 0, 0, 0, 0, 99f, 80f, 83f, 65f, 68f, 28f, 55f, 58f, 50f, 53f, 53f, 57f,
                    48f, 50f, 53f, 54f, 25f, 27f, 35f, 37f, 35f, 80f, 82f, 55f, 59f, 85f, 82f, 60f,
                    55f, 63f, 65f, 58f, 60f, 63f, 60f, 0},
                    {85f, 85f, 85f, 85f, 85f, 85f, 85f, 85f, 85f, 85f, 85f, 85f, 85f, 85f, 85f, 85f,
                            85f, 85f, 85f, 85f, 85f, 85f, 85f, 85f, 85f, 85f, 85f, 85f, 85f, 85f,
                            85f, 85f, 85f, 85f, 85f, 85f, 85f, 85f, 85f, 85f, 0}};


    public MonthlyChartBuilder(CardView card) {

        super(card);
        mChart = card.findViewById(R.id.chart);
    }


    @Override
    public void show(Runnable action) {

        super.show(action);
        ArrayList al = fillvalues();
        int max_days = Calendar.getInstance().getActualMaximum(Calendar.DAY_OF_MONTH);
        for (int i = 0; i < max_days + 10; i++) {
            if (i == 4) mLabels[4] = "START";
            else if (i == 5 + max_days) mLabels[4] = "FINISH";
            else mLabels[i] = "";

            if (i <= 4 || i >= 5 + max_days) mValues[0][i] = -1f;
            else {
                mValues[0][i] = 100f * Float.parseFloat(al.get(i - 4) + "") / (1f * StatisticsActivity.limit / max_days);
                mValues[1][i] = 50f;
            }
        }
        float max = -0.5f;
        int maxidx = 0;
        System.out.println("Length=" + mValues[0].length);
        for (int i = 0; i < mValues[0].length; i++) {
            if (mValues[0][i] > max) {
                maxidx = i;
                max = mValues[0][i];
            }
        }
        for (int i = 0; i <= maxidx; i++) {
            if (mValues[0][i] == max) {
                maxidx = i;
                break;
            }
        }

        float f_temp = 1f * StatisticsActivity.limit / max_days;
        if (f_temp < max)
            for (int i = 0; i < max_days + 10; i++) {
                if (i > 4 && i < 5 + max_days) {
                    mValues[0][i] *= 100f / max;
                }
            }
        System.out.println(max + ":" + (maxidx - 4) + "" + al.get(maxidx - 4));
        LineSet dataset = new LineSet(mLabels, mValues[0]);
        dataset.setColor(Color.parseColor("#004f7f"))
                .setThickness(Tools.fromDpToPx(3))
                .setSmooth(true)
                .beginAt(5)
                .endAt(5 + max_days);
        for (int i = 0; i < mLabels.length; i++) {
            Point point = (Point) dataset.getEntry(i);
            if (i % 5 == 0) {
                point.setColor(Color.parseColor("#ffffff"));
                point.setStrokeColor(Color.parseColor("#0290c3"));
            } else {
                point.setColor(Color.parseColor("#aaaaff"));
                point.setRadius(Tools.fromDpToPx(1));

            }
        }
        Point point = (Point) dataset.getEntry(maxidx);
        point.setColor(Color.parseColor("#ffffff"));
        point.setStrokeColor(Color.parseColor("#911077"));
        point.setRadius(Tools.fromDpToPx(6));
        mChart.addData(dataset);

        drawLine(action);
    }

    private void drawLine(Runnable action) {

        Paint thresPaint = new Paint();
        thresPaint.setColor(Color.parseColor("#0079ae"));
        thresPaint.setStyle(Paint.Style.STROKE);
        thresPaint.setAntiAlias(true);
        thresPaint.setStrokeWidth(Tools.fromDpToPx(.75f));
        thresPaint.setPathEffect(new DashPathEffect(new float[]{10, 10}, 0));

        Paint gridPaint = new Paint();
        gridPaint.setColor(Color.parseColor("#ffffff"));
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setAntiAlias(true);
        gridPaint.setStrokeWidth(Tools.fromDpToPx(.75f));

        mChart.setXLabels(AxisRenderer.LabelPosition.OUTSIDE)
                .setYLabels(AxisRenderer.LabelPosition.NONE)
                .setGrid(0, 7, gridPaint)
                .setValueThreshold(80f, 80f, thresPaint)
                .setAxisBorderValues(0, 110)
                .show(new Animation().fromXY(0, .5f).withEndAction(action));
    }

    @Override
    public void update() {

        super.update();

        if (firstStage) {
            mChart.updateValues(0, mValues[1]);
        } else {
            mChart.updateValues(0, mValues[0]);
        }
        mChart.notifyDataUpdate();
    }


    @Override
    public void dismiss(Runnable action) {

        super.dismiss(action);

        mChart.dismiss(new Animation().fromXY(1, .5f).withEndAction(action));
    }

    public ArrayList fillvalues() {
        Calendar calendar = Calendar.getInstance();
        ArrayList al = new ArrayList(calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
        for (int i = 0; i < calendar.getActualMaximum(Calendar.DAY_OF_MONTH) + 1; i++) {
            al.add(0);
        }
        Cursor cursor = MainActivity.cx;
        DbConnect db = new DbConnect(StatisticsActivity.context);
        String baapstr = db.getdb();
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
                if (d1 == d2) {
                    flag_date = true;
                } else if (d1 < d2) break;

                if (baapstr.contains(message_date.getTime() + " -- -1"))
                    flag_deleted = true;

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
                    double cost_temp_1 = 0d;
                    if (!flag_deleted && baapstr.contains(message_date.getTime() + "")) {
                        cost_temp_1 = Double.parseDouble(baapstr.split(message_date.getTime() + " -- ")[1].split(" ")[0]);
                        if (!String.format("%.2f", cost_temp_1).equals(String.format("%.2f", cost_temp))) {
                            double temp = cost_temp;
                            cost_temp = Double.parseDouble(baapstr.split(message_date.getTime() + " -- ")[1].split(" ")[0]);
                        }
                    }
                    int dte = Integer.parseInt(new SimpleDateFormat("d").format(message_date));
                    al.set(dte, Double.parseDouble(al.get(dte)+"")+cost_temp);
                }
            } while (cursor.moveToNext());
        }
        return al;
    }


}
