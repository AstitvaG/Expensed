package com.alphalabs.expensed;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.StringTokenizer;

public class DbConnect extends SQLiteOpenHelper {


    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "expensed_db";

    DbConnect(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        //3rd argument to be passed is CursorFactory instance
    }

    // Creating Tables
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE "+TABLE_NAME+" ( "+KEY_ID+" INT, "+KEY_VALUE+" TEXT )");
    }

    // Upgrading database
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop older table if existed
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        // Create tables again
        onCreate(db);
    }
    //Username database

    private static final String TABLE_NAME = "edited";
    private static final String KEY_ID = "id";
    private static final String KEY_VALUE = "val";

    void addEntry(String id, String val) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_ID, Long.parseLong(id));
        values.put(KEY_VALUE, val);
        // Inserting Row
        db.delete(TABLE_NAME, KEY_ID + "=" + id, null);
        db.insert(TABLE_NAME, null, values);
    }
    boolean getEntry(String id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM "+ TABLE_NAME + " WHERE TRIM("+ KEY_ID + ") == ?;", new String[]{id.trim()});
        return c.getCount() >= 1;
    }
    String getdb(){
        String s="";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM "+ TABLE_NAME ,null);
        while(c.moveToNext()){
            s += c.getString(0) + " -- " + c.getString(1) + " \n";
        }
        return s;
    }
}