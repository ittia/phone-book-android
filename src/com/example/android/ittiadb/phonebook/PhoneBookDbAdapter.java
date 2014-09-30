package com.example.android.ittiadb.phonebook;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.ittiadb.IttiaDbDatabase;
import android.database.ittiadb.IttiaDbOpenHelper;
import android.database.ittiadb.IttiaDbSyncAdapter;

public class PhoneBookDbAdapter {
    public static final String KEY_ROWID = "id";
    public static final String KEY_NAME = "name";
    public static final String KEY_RING_ID = "ring_id";
    public static final String KEY_PICTURE_NAME = "picture_name";
    public static final String KEY_PICTURE = "picture";

    public static final String TAG = "PhoneBookDbAdapter";
    private DatabaseHelper mDbHelper;
    private IttiaDbDatabase mDb;

    private static final String DATABASE_NAME = "phone_book.db";
    private static final int DATABASE_VERSION = 1;
    private static final String CONTACT_TABLE = "contact";
    private static final String SEQUENCE_CONTACT_ID = "contact_id";

    // The Android emulator maps 10.0.2.2 to the host's 127.0.0.1 adapter.
    // When deploying to an actual device, replace it with the host's network
    // host name or IP address.
    private static final String PEER_URI = "idb+tcp://10.0.2.2/backend1";

    private Context mCtx;

    private static class DatabaseHelper extends IttiaDbOpenHelper
    {
        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(IttiaDbDatabase db) {
            db.execSQL(
                "create table contact (" +
                "  id uint64 not null," +
                "  name utf16str(50) not null," +
                "  ring_id uint64," +
                "  picture_name varchar(50)," +
                "  picture blob," +
                "  constraint by_id primary key (id)" +
                ")");
            db.execSQL("create index by_name on contact (name)");
            db.execSQL("create sequence contact_id start with 1");

            // Replicate the contact table in and out
            IttiaDbDatabase.ReplicationTable contactRep = db.getReplicationTable("contact");
            contactRep.replicationMode = IttiaDbDatabase.ReplicationMode.INOUT;
            contactRep.apply();
        }

        @Override
        public void onUpgrade(IttiaDbDatabase db, int oldVersion, int newVersion) {

        }
    }

	public PhoneBookDbAdapter(Context context) {
		this.mCtx = context;
	}
	
	public PhoneBookDbAdapter open() throws SQLException {
		mDbHelper = new DatabaseHelper(mCtx);
		mDb = mDbHelper.getWritableDatabase();
		return this;
	}
	
	public void close() {
		mDbHelper.close();
	}

	public long createContact(String name, Long ring_id) {
		mDb.beginTransaction();
		try {
			long id = mDb.sequenceNext(SEQUENCE_CONTACT_ID) + mDb.getReplicationAddress() * Integer.MAX_VALUE;
	
			ContentValues initialValues = new ContentValues();
			initialValues.put(KEY_ROWID, id);
			initialValues.put(KEY_NAME, name);
			initialValues.put(KEY_RING_ID, ring_id);
			
			if (mDb.insert(CONTACT_TABLE, null, initialValues) >= 0) {
				mDb.setTransactionSuccessful();
				return id;
			}
			else {
				return -1;
			}
		}
		finally {
			mDb.endTransaction();
		}
	}
	
	public boolean deleteContact(long rowId) {
		return mDb.delete(CONTACT_TABLE, KEY_ROWID + "=" + rowId, null) > 0;
	}

	public Cursor fetchAllContacts() {
		return mDb.query(CONTACT_TABLE, new String[] { KEY_ROWID + " as _id", KEY_NAME,
				KEY_RING_ID, KEY_PICTURE }, null, null, null, null, null);
	}
	
	public Cursor fetchContact(long rowId) throws SQLException {
		Cursor mCursor =
				mDb.query(true, CONTACT_TABLE, new String[] { KEY_ROWID + " as _id",
						KEY_NAME, KEY_RING_ID, KEY_PICTURE }, KEY_ROWID + "=" + rowId, null, null,
						null, null, null);
		
		if (mCursor != null) {
			mCursor.moveToFirst();
		}
		return mCursor;
	}
	
	public boolean updateContact(long rowId, String name, Long ring_id) {
		ContentValues args = new ContentValues();
		args.put(KEY_NAME, name);
		args.put(KEY_RING_ID, ring_id);
		
		return mDb.update(CONTACT_TABLE, args, KEY_ROWID + "=" + rowId, null) > 0;
	}

    public boolean updateContactPicture(long rowId, byte[] picture) {
        ContentValues args = new ContentValues();
        args.put(KEY_PICTURE, picture);

        return mDb.update(CONTACT_TABLE, args, KEY_ROWID + "=" + rowId, null) > 0;
    }

    public IttiaDbSyncAdapter getSyncAdapter() {
        // Create a sync adapter for the database that is initially syncable
        // and that requests replicate tokens from a sync authenticator.
        return new IttiaDbSyncAdapter(mCtx, true, mDb, SyncContentProvider.CONTENT_URI, SyncAuthenticator.AUTH_TOKEN_TYPE_REPLICATE);
    }

    public SyncAuthenticator getSyncAuthenticator() {
        // Create a sync authenticator for the database that connects to
        // PEER_URI by default.
        return new SyncAuthenticator(mCtx, mDb, PEER_URI);
    }
}
