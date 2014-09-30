package com.example.android.ittiadb.phonebook;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class SyncAuthenticatorService extends Service {

    private SyncAuthenticator mAuthenticator;

    @Override
    public void onCreate() {
        // Create a new authenticator object
        mAuthenticator = new PhoneBookDbAdapter(this).open().getSyncAuthenticator();
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return mAuthenticator.getIBinder();
    }
}
