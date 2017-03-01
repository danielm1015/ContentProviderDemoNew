package com.rabor.contentproviderdemo;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import java.util.HashMap;

public class BirthdayProvider extends ContentProvider {
    // fields for my content provider
    static final String PROVIDER_NAME = "com.rabor.provider.BirthdayProv";
    static final String URL = "content://" + PROVIDER_NAME + "/friends";
    static final Uri CONTENT_URI = Uri.parse(URL);

    // integer values used in content URI
    static final int FRIENDS = 1;
    static final int FRIENDS_ID = 2;

    // map content URI "patterns" to the integer values that were set above
    static final UriMatcher uriMatcher;
    static {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(PROVIDER_NAME, "friends", FRIENDS);
        uriMatcher.addURI(PROVIDER_NAME, "friends/#", FRIENDS_ID);
    }

    DBHelper dbHelper;

    // project map for a query
    private static HashMap<String, String> birthMap;

    // fields for the database
    static final String ID = "id";     // primary key
    static final String NAME = "name";
    static final String BIRTHDAY = "birthday";

    // database declarations
    private SQLiteDatabase database;
    static final String DATABASE_NAME = "BirthdayReminder.db";
    static final int DATABASE_VERSION = 1;
    static final String TABLE_NAME = "birthTable";
    static final String CREATE_TABLE =
            " CREATE TABLE " + TABLE_NAME +
                    " (id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    " name TEXT NOT NULL, " +
                    " birthday TEXT NOT NULL);";

    // class that creates and manages the provider's database
    private static class DBHelper extends SQLiteOpenHelper {

        public DBHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase sqLiteDatabase) {
            sqLiteDatabase.execSQL(CREATE_TABLE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase sqLiteDatabase,
                              int oldVersion, int newVersion) {

            Log.i(DBHelper.class.getName(),
                    "Upgrading database from version " + oldVersion +
                    " to " + newVersion + ".  Old data will be destroyed");
            sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
            onCreate(sqLiteDatabase);
        }
    }

    public BirthdayProvider() {
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // Implement this to handle requests to delete one or more rows.
        int count = 0;

        switch (uriMatcher.match(uri)) {
            case FRIENDS:
                count = database.delete(TABLE_NAME, selection, selectionArgs);
                break;
            case FRIENDS_ID:
                // textUtils.isEmpty checks whether the user entered anything in the field.
                // getLastPathSegment gets the last decoded segment in the path...so if the
                // last segment is ID, it will return the ID
                String id = uri.getLastPathSegment();       // gets the id
                count = database.delete(
                        TABLE_NAME, ID + " = " + id +
                                (!TextUtils.isEmpty(selection) ? " AND (" +
                                        selection + ')' : ""), selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Unsupported URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);

        return count;

    }

    @Override
    public String getType(Uri uri) {
        // TODO: Implement this to handle requests for the MIME type of the data
        // at the given URI.
        switch (uriMatcher.match(uri)) {
            // get all friends birthday records
            case FRIENDS:
                return "vnd.android.cusor.dir/vnd.rabor.friends";
            // get a particular friend birthday
            case FRIENDS_ID:
                return "vnd.android.cusor.item/vnd.rabor.friends";      //returns a single friend
            default:
               throw new IllegalArgumentException("Unsupported URI: " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        // TODO: Implement this to handle requests to insert a new row.
        long row = database.insert(TABLE_NAME, "", values);

        // if record is added successfully
        if(row > 0) {
            Uri newUri = ContentUris.withAppendedId(CONTENT_URI, row);
            getContext().getContentResolver().notifyChange(newUri, null);

            return newUri;
        }

        throw new SQLException("Fail to add a new record into " + uri);
    }

    @Override
    public boolean onCreate() {
        Context context = getContext();
        dbHelper = new DBHelper(context);
        // permission to be writable
        database = dbHelper.getWritableDatabase();
        if(database == null) {
            return false;
        } else {
            return true;
        }
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        // TODO: Implement this to handle query requests from clients.
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
        // the TABLE_NAME to query on
        queryBuilder.setTables(TABLE_NAME);

        switch (uriMatcher.match(uri)) {
            // maps all database column names
            case FRIENDS:
                queryBuilder.setProjectionMap(birthMap);
                break;
            case FRIENDS_ID:
                queryBuilder.appendWhere(ID + "=" + uri.getLastPathSegment());
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        if(sortOrder == null || sortOrder == "") {
            // No sorting -> sort on names by default
            sortOrder = NAME;
        }

        Cursor cursor = queryBuilder.query(database, projection, selection,
            selectionArgs, null, null, sortOrder);
        // register to watch a content URI for changes
        cursor.setNotificationUri(getContext().getContentResolver(), uri);

        return cursor;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        // TODO: Implement this to handle requests to update one or more rows.
        int count = 0;

        switch (uriMatcher.match(uri)) {
            case FRIENDS:
                count = database.update(TABLE_NAME, values, selection, selectionArgs);
                break;
            case FRIENDS_ID:
                // textUtils.isEmpty checks whether the user entered anything in the field.
                // getLastPathSegment gets the last decoded segment in the path...so if the
                // last segment is ID, it will return the ID
                count = database.update(
                        TABLE_NAME, values, ID + " = " +
                    uri.getLastPathSegment() +
                        (!TextUtils.isEmpty(selection) ? " AND (" +
                        selection + ')' : ""), selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Unsupported URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);

        return count;
    }
}
