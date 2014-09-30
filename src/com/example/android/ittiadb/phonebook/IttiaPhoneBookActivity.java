package com.example.android.ittiadb.phonebook;

import java.io.IOException;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.ListActivity;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;

/**
 * A simple contact list activity with support for database synchronization.
 */
public class IttiaPhoneBookActivity extends ListActivity {
    private static final int ACTIVITY_CREATE=0;
    private static final int ACTIVITY_EDIT=1;

    private static final int INSERT_ID = Menu.FIRST;
    private static final int DELETE_ID = Menu.FIRST + 1;
    private static final int SYNC_ID = Menu.FIRST + 2;

    private PhoneBookDbAdapter mDbHelper;
    private ContentObserver mContentObserver;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.phonebook_list);

        // Connect to the database and fetch the contact list.
        mDbHelper = new PhoneBookDbAdapter(this);
        mDbHelper.open();
        fillData();

        // Add a context menu to the list.
        registerForContextMenu(getListView());

        // When the sync provider changes the database content, update the
        // list and display a short message. This observer is called even if
        // the sync is scheduled in the background, but only while this
        // activity is active.
        mContentObserver = new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean selfChange) {
                if (!selfChange) {
                    fillData();
                    Toast.makeText(IttiaPhoneBookActivity.this, R.string.message_sync_complete, Toast.LENGTH_SHORT).show();
                }
            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();
        // The sync provider is created with the same content URI.
        getContentResolver().registerContentObserver(SyncContentProvider.CONTENT_URI, false, mContentObserver);
    }

    @Override
    public void onPause() {
        super.onPause();
        // Always unregister content observers when the activity is paused.
        getContentResolver().unregisterContentObserver(mContentObserver);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        boolean result = super.onCreateOptionsMenu(menu);

        // Display a menu item to add contacts.
        menu.add(Menu.NONE, INSERT_ID, Menu.NONE, R.string.menu_insert)
            .setIcon(android.R.drawable.ic_menu_add)
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);

        // Display a menu item to perform synchronization.
        menu.add(Menu.NONE, SYNC_ID, Menu.NONE, R.string.menu_sync)
            .setIcon(android.R.drawable.ic_popup_sync)
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_WITH_TEXT);

        return result;
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        switch(item.getItemId()) {
            case INSERT_ID:
                createContact();
                return true;
            case SYNC_ID:
                synchronize();
                return true;
        }

        return super.onMenuItemSelected(featureId, item);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
            ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.add(0, DELETE_ID, 0, R.string.menu_delete);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case DELETE_ID:
                // Delete the contact and refresh the list.
                AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
                mDbHelper.deleteContact(info.id);
                fillData();
                return true;
        }
        return super.onContextItemSelected(item);
    }

    private void createContact() {
        // Start a ContactEdit activity to create a new contact.
        Intent i = new Intent(this, ContactEdit.class);
        startActivityForResult(i, ACTIVITY_CREATE);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        // Start a ContactEdit activity to edit the existing contact.
        Intent i = new Intent(this, ContactEdit.class);
        i.putExtra(PhoneBookDbAdapter.KEY_ROWID, id);
        startActivityForResult(i, ACTIVITY_EDIT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        // Refresh the list when the ContactEdit activity is closed.
        fillData();
    }

    private void fillData() {
        // Bind the contacts table to the list view.
        Cursor c = mDbHelper.fetchAllContacts();
        startManagingCursor(c);

        String[] from = new String[] { PhoneBookDbAdapter.KEY_NAME };

        int[] to = new int[] { R.id.contact_name };

        SimpleCursorAdapter contacts =
                new SimpleCursorAdapter(this, R.layout.contacts_row, c, from, to);
        setListAdapter(contacts);
    }

    /** The account type used to synchronize. This should match the accountType
     * attribute in res/xml/syncadapter.xml and res/xml/authenticator.xml. */
    public static final String ACCOUNT_TYPE = "com.example.android.ittiadb.phonebook";

    /**
     * Synchronize with a back-end database.
     *
     * Note: requires permissions GET_ACCOUNTS and MANAGE_ACCOUNTS.
     */
    private void synchronize() {
        // Find all sync accounts.
        final AccountManager accountManager = AccountManager.get(this);
        final Account[] accounts = accountManager.getAccountsByType(ACCOUNT_TYPE);

        // Perform a manual sync as soon as possible.
        final Bundle settingsBundle = new Bundle();
        settingsBundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        settingsBundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);

        if (accounts.length > 0) {
            for (int i = 0; i < accounts.length; ++i) {
                ContentResolver.requestSync(accounts[i], SyncContentProvider.AUTHORITY, settingsBundle);
            }
        }
        else {
            // Add a new synchronization account and request sync. The account
            // manager will start LoginActivity to prompt for credentials.
            accountManager.addAccount(ACCOUNT_TYPE,
                    SyncAuthenticator.AUTH_TOKEN_TYPE_REPLICATE, null, null, this,
                    new AccountManagerCallback<Bundle>() {
                        @Override
                        public void run(AccountManagerFuture<Bundle> future) {
                            Bundle bundle;
                            try {
                                bundle = future.getResult();
                                if (bundle.containsKey(AccountManager.KEY_ACCOUNT_NAME)) {
                                    Account account = new Account(
                                            bundle.getString(AccountManager.KEY_ACCOUNT_NAME),
                                            bundle.getString(AccountManager.KEY_ACCOUNT_TYPE));
                                    ContentResolver.requestSync(account, SyncContentProvider.AUTHORITY, settingsBundle);
                                }
                            }
                            catch (OperationCanceledException e) {
                            }
                            catch (AuthenticatorException e) {
                            }
                            catch (IOException e) {
                            }
                        }
                    }, null);
        }
    }
}
