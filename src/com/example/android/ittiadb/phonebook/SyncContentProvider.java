package com.example.android.ittiadb.phonebook;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

/*
 * Define an implementation of ContentProvider that stubs out
 * all methods. A content provider is required to use a sync adapter,
 * but is otherwise unnecessary for this application.
 */
public class SyncContentProvider extends ContentProvider {
    /** The authority for the sync adapter's content provider. This should
     * match the contentAuthority attribute in res/xml/syncadapter.xml. */
    public static final String AUTHORITY = "com.example.android.ittiadb.phonebook.provider";

    /** The base URI for all content under the authority of this provider. */
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY);

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public String getType(Uri uri) {
        return new String();
    }

    @Override
    public Cursor query(
            Uri uri,
            String[] projection,
            String selection,
            String[] selectionArgs,
            String sortOrder) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(
            Uri uri,
            ContentValues values,
            String selection,
            String[] selectionArgs) {
        return 0;
    }
}
