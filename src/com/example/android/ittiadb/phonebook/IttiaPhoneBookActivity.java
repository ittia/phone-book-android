package com.example.android.ittiadb.phonebook;

import com.ittia.db.IttiaDb;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.text.InputType;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.EditText;
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
    private static final int REP_CONFIG_ID = Menu.FIRST + 2;
    private static final int SYNC_ID = Menu.FIRST + 3;
	
	private PhoneBookDbAdapter mDbHelper;
    private MenuItem menu_sync;
	
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
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	boolean result = super.onCreateOptionsMenu(menu);
        menu.add(0, INSERT_ID, 0, R.string.menu_insert).setIcon(android.R.drawable.ic_menu_add);
        menu.add(0, REP_CONFIG_ID, 0, R.string.menu_rep_config).setIcon(android.R.drawable.ic_menu_preferences);
        menu_sync = menu.add(0, SYNC_ID, 0, R.string.menu_sync);
        menu_sync.setIcon(android.R.drawable.ic_popup_sync);
        updateMenu();
        return result;
    }
    
    private void updateMenu()
    {
        if (menu_sync != null)
            menu_sync.setEnabled(mDbHelper.getReplicationAddress() != 0);
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        switch(item.getItemId()) {
            case INSERT_ID:
                createContact();
                return true;
            case REP_CONFIG_ID:
                configureReplication();
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
                AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
                mDbHelper.deleteContact(info.id);
                fillData();
                return true;
        }
        return super.onContextItemSelected(item);
    }

    private void createContact() {
    	Intent i = new Intent(this, ContactEdit.class);
    	startActivityForResult(i, ACTIVITY_CREATE);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        Intent i = new Intent(this, ContactEdit.class);
        i.putExtra(PhoneBookDbAdapter.KEY_ROWID, id);
        startActivityForResult(i, ACTIVITY_EDIT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        fillData();
    }
    
	private void fillData() {
		Cursor c = mDbHelper.fetchAllContacts();
		startManagingCursor(c);
		
		String[] from = new String[] { PhoneBookDbAdapter.KEY_NAME };
		
		int[] to = new int[] { R.id.contact_name };
		
		SimpleCursorAdapter contacts =
				new SimpleCursorAdapter(this, R.layout.contacts_row, c, from, to);
		setListAdapter(contacts);
	}
    
    private void configureReplication()
    {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        alert.setTitle("Configure replication"); 
        alert.setMessage("Enter device address:");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setText(Integer.toString(mDbHelper.getReplicationAddress()));
        alert.setView(input);
        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                mDbHelper.setReplicationAddress(Integer.parseInt(input.getText().toString()));
                updateMenu();
            }
            });
        
        alert.show();
    }
    
	private void synchronize() {
	    if (mDbHelper.replicationExchange(null, null)) {
            Toast.makeText(this, "Exchange complete", Toast.LENGTH_SHORT).show();
	        fillData();
	    }
	    else {
	        Toast.makeText(this, "Exchange failed: " + IttiaDb.getErrorInfo().getName(), Toast.LENGTH_SHORT).show();
	    }
	}
}