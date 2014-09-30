package com.example.android.ittiadb.phonebook;

import android.app.Service;
import android.content.Intent;
import android.database.ittiadb.IttiaDbSyncAdapter;
import android.os.IBinder;

public class SyncService extends Service {

    private static IttiaDbSyncAdapter sSyncAdapter = null;
    // Object to use as a thread-safe lock
    private static final Object sSyncAdapterLock = new Object();

    /*
     * Instantiate the sync adapter object.
     */
    @Override
    public void onCreate() {
        /*
         * Create the sync adapter as a singleton.
         * Set the sync adapter as syncable
         * Disallow parallel syncs
         */
        synchronized (sSyncAdapterLock) {
            if (sSyncAdapter == null) {
                // Open a connection to the application's database.
                // This service must run in the same process as the main
                // activity so that open() does not fail.
                sSyncAdapter = new PhoneBookDbAdapter(this).open().getSyncAdapter();
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return sSyncAdapter.getSyncAdapterBinder();
    }

}
