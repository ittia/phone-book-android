package com.example.android.ittiadb.phonebook;

import java.io.ByteArrayOutputStream;

import android.app.Activity;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;

/**
 * A simple contact editing activity.
 */
public class ContactEdit extends Activity {
    private EditText mNameText;
    private Spinner mRingIdSpinner;
    private ImageView mPictureImage;
    private Long mRowId;
    private PhoneBookDbAdapter mDbHelper;

    private boolean pictureChanged;
    private byte[] picture;

    private int[] availablePictures;
    private int currentPictureIndex;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Access the database.
        mDbHelper = new PhoneBookDbAdapter(this);
        mDbHelper.open();

        // Show the Up button in the action bar.
        getActionBar().setDisplayHomeAsUpEnabled(true);

        // Show info for a single contact.
        setContentView(R.layout.contact_edit);
        setTitle(R.string.edit_contact);

        mNameText = (EditText) findViewById(R.id.name);
        mRingIdSpinner = (Spinner) findViewById(R.id.ring_id);
        mPictureImage = (ImageView) findViewById(R.id.picture);

        Button pictureEraseButton = (Button) findViewById(R.id.contact_picture_erase);
        Button pictureNextButton = (Button) findViewById(R.id.contact_picture_next);

        // Get contact ID from saved state, if possible.
        mRowId = (savedInstanceState == null) ? null :
            (Long) savedInstanceState.getSerializable(PhoneBookDbAdapter.KEY_ROWID);
        if (mRowId == null) {
            // Otherwise, get contact ID from intent extras.
            Bundle extras = getIntent().getExtras();
            mRowId = extras != null ? extras.getLong(PhoneBookDbAdapter.KEY_ROWID)
                                    : null;
        }

        // Provide a fixed collection of pictures that can be assigned to
        // contacts by this application. When selected, the picture is stored
        // in the contact record in PNG format.
        availablePictures = new int[] {
                R.drawable.woman1,
                R.drawable.man1,
                R.drawable.man2,
                R.drawable.woman2,
                R.drawable.woman3,
                R.drawable.man3,
        };
        currentPictureIndex = 0;

        // Populate ring_id spinner with a list of amusing ring tones.
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.ringtones, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mRingIdSpinner.setAdapter(adapter);

        // Load contact record from the database into the member views.
        populateFields();

        pictureEraseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                erasePicture();
            }
        });

        pictureNextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                nextPicture();
            }
        });
    }

    private void erasePicture()
    {
        pictureChanged = true;
        showPicture(null);
    }

    private void nextPicture()
    {
        currentPictureIndex = (currentPictureIndex + 1) % availablePictures.length;
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), availablePictures[currentPictureIndex]);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 0, stream);

        pictureChanged = true;
        showPicture(stream.toByteArray());
    }

    private void showPicture(byte picture[])
    {
        this.picture = picture;
        if (picture != null) {
            Bitmap bitmap = BitmapFactory.decodeByteArray(picture, 0, picture.length);
            mPictureImage.setImageBitmap(bitmap);
            mPictureImage.setVisibility(View.VISIBLE);
        }
        else {
            mPictureImage.setVisibility(View.GONE);
        }
    }

    private void populateFields() {
        pictureChanged = false;

        if (mRowId == null) {
            mNameText.setText(getString(R.string.default_contact_name));
            mNameText.selectAll();
            mRingIdSpinner.setSelection(0);
            showPicture(null);
        }
        else {
            Cursor contact = mDbHelper.fetchContact(mRowId);
            startManagingCursor(contact);
            mNameText.setText(contact.getString(
                    contact.getColumnIndexOrThrow(PhoneBookDbAdapter.KEY_NAME)));

            int ring_id_index = contact.getColumnIndexOrThrow(PhoneBookDbAdapter.KEY_RING_ID);
            int picture_index = contact.getColumnIndexOrThrow(PhoneBookDbAdapter.KEY_PICTURE);

            if (contact.isNull(ring_id_index) || contact.getInt(ring_id_index) + 1 >= mRingIdSpinner.getCount()) {
                mRingIdSpinner.setSelection(0);
            }
            else {
                mRingIdSpinner.setSelection(contact.getInt(ring_id_index) + 1);
            }

            if (contact.isNull(picture_index))
                showPicture(null);
            else
                showPicture(contact.getBlob(picture_index));
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        saveState();
        outState.putSerializable(PhoneBookDbAdapter.KEY_ROWID, mRowId);
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveState();
    }

    @Override
    protected void onResume() {
        super.onResume();
        populateFields();
    }

    private void saveState() {
        String name = mNameText.getText().toString();
        Long ring_id = mRingIdSpinner.getSelectedItemId() == 0 ? null : mRingIdSpinner.getSelectedItemId() - 1;

        if (name.isEmpty()) {
            name = getString(R.string.default_contact_name);
        }

        if (mRowId == null) {
            long id = mDbHelper.createContact(name, ring_id);
            if (id > 0) {
                mRowId = id;
            }
        }
        else {
            mDbHelper.updateContact(mRowId, name, ring_id);
        }

        // Update a BLOB column only when necessary.
        if (pictureChanged) {
            mDbHelper.updateContactPicture(mRowId, picture);
        }
    }
}
