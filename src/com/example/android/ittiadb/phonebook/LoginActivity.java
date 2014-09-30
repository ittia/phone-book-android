package com.example.android.ittiadb.phonebook;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.content.Intent;
import android.database.ittiadb.IttiaDbDatabase;
import android.database.ittiadb.IttiaDbSyncAdapter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Activity which displays a login screen to the user, offering registration as
 * well.
 */
public class LoginActivity extends AccountAuthenticatorActivity {
    // Identify which account to authenticate
    public static final String EXTRA_ACCOUNT_NAME = AccountManager.KEY_ACCOUNT_NAME;
    public static final String EXTRA_ACCOUNT_TYPE = AccountManager.KEY_ACCOUNT_TYPE;
    public static final String EXTRA_AUTH_TYPE = "authType";

    // Replication settings
    public static final String EXTRA_REPLICATION_ADDRESS = IttiaDbSyncAdapter.KEY_REPLICATION_ADDRESS;
    public static final String EXTRA_USER_NAME = IttiaDbSyncAdapter.KEY_USER_NAME;
    public static final String EXTRA_PEER_URI = IttiaDbSyncAdapter.KEY_PEER_URI;

    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private UserLoginTask mAuthTask = null;
    private AccountManager mAccountManager;
    private String mAuthTokenType;

    // Values for username and password at the time of the login attempt.
    private String mAccountType;
    private String mSyncProtocol;
    private String mSyncHost;
    private String mSyncConnection;
    private String mUsername;
    private int mReplicationAddress;
    private String mPassword;

    // UI references.
    private EditText mSyncHostView;
    private EditText mSyncConnectionView;
    private EditText mUsernameView;
    private EditText mPasswordView;
    private View mLoginFormView;
    private View mLoginStatusView;
    private TextView mLoginStatusMessageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_login);

        mAccountManager = AccountManager.get(getBaseContext());

        mAccountType = getIntent().getStringExtra(EXTRA_ACCOUNT_TYPE);
        mAuthTokenType = getIntent().getStringExtra(EXTRA_AUTH_TYPE);
        if (mAuthTokenType == null)
            mAuthTokenType = SyncAuthenticator.AUTH_TOKEN_TYPE_REPLICATE;

        final String peerUri = getIntent().getStringExtra(EXTRA_PEER_URI);

        // A peer URI has the form "protocol://host/connection".
        final int offsetEndProtocol = peerUri.indexOf("://", 0);
        final int offsetEndHost = peerUri.indexOf("/", offsetEndProtocol + 3);

        // Evaluate login form values.
        if (offsetEndProtocol >= 0 && offsetEndHost >= 0) {
            mSyncProtocol = peerUri.substring(0, offsetEndProtocol);
            mSyncHost = peerUri.substring(offsetEndProtocol + "://".length(), offsetEndHost);
            mSyncConnection = peerUri.substring(offsetEndHost + + "/".length());
        }
        else {
            mSyncProtocol = "idb+tcp";
            mSyncHost = "";
            mSyncConnection = "";
        }
        mUsername = getIntent().getStringExtra(EXTRA_USER_NAME);

        // Set up the login form views.

        mSyncHostView = (EditText) findViewById(R.id.sync_host);
        mSyncHostView.setText(mSyncHost);

        mSyncConnectionView = (EditText) findViewById(R.id.sync_connection);
        mSyncConnectionView.setText(mSyncConnection);

        mUsernameView = (EditText) findViewById(R.id.username);
        mUsernameView.setText(mUsername);

        mPasswordView = (EditText) findViewById(R.id.password);
        mPasswordView
                .setOnEditorActionListener(new TextView.OnEditorActionListener() {
                    @Override
                    public boolean onEditorAction(TextView textView, int id,
                            KeyEvent keyEvent) {
                        if (id == R.id.login || id == EditorInfo.IME_NULL) {
                            attemptLogin();
                            return true;
                        }
                        return false;
                    }
                });

        mLoginFormView = findViewById(R.id.login_form);
        mLoginStatusView = findViewById(R.id.login_status);
        mLoginStatusMessageView = (TextView) findViewById(R.id.login_status_message);

        findViewById(R.id.sign_in_button).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        attemptLogin();
                    }
                });
    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    public void attemptLogin() {
        if (mAuthTask != null) {
            return;
        }

        // Reset errors.
        mSyncHostView.setError(null);
        mSyncConnectionView.setError(null);
        mUsernameView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        mSyncHost = mSyncHostView.getText().toString();
        mSyncConnection = mSyncConnectionView.getText().toString();
        mUsername = mUsernameView.getText().toString();
        mPassword = mPasswordView.getText().toString();

        View focusView = null;

        if (TextUtils.isEmpty(mSyncHost)) {
            mSyncHostView.setError(getString(R.string.error_invalid_sync_host));
            focusView = mSyncHostView;
        }
        
        if (TextUtils.isEmpty(mSyncConnection)) {
            mSyncConnectionView.setError(getString(R.string.error_invalid_sync_connection));
            focusView = mSyncConnectionView;
        }

        if (focusView != null) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            mLoginStatusMessageView.setText(R.string.login_progress_signing_in);
            showProgress(true);
            mAuthTask = new UserLoginTask();
            mAuthTask.execute((Void) null);
        }
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    private void showProgress(final boolean show) {
        mLoginStatusView.setVisibility(show ? View.VISIBLE : View.GONE);
        mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    private void finishLogin(Intent intent) {
        mReplicationAddress = intent.getIntExtra(IttiaDbSyncAdapter.KEY_REPLICATION_ADDRESS, IttiaDbDatabase.REP_ADDRESS_NONE);
        final String userName = intent.getStringExtra(IttiaDbSyncAdapter.KEY_USER_NAME);
        final int peerAddress = intent.getIntExtra(IttiaDbSyncAdapter.KEY_PEER_ADDRESS, IttiaDbDatabase.REP_ADDRESS_NONE);
        final String peerUri = intent.getStringExtra(IttiaDbSyncAdapter.KEY_PEER_URI);
        final String accountPassword = intent.getStringExtra(UserLoginTask.PARAM_USER_PASS);

        // The account name must be unique and cannot be changed after the
        // account is created.
        final String accountName = getIntent().hasExtra(EXTRA_ACCOUNT_NAME) ?
                getIntent().getStringExtra(EXTRA_ACCOUNT_NAME) :
                (userName.isEmpty() ? "" : userName + " at ") + peerUri;

        final Account account = new Account(accountName, mAccountType);

        if (!getIntent().hasExtra(EXTRA_ACCOUNT_NAME)) {
            // Create the account and save the token, since it isn't saved
            // automatically for an explicitly created account.
            mAccountManager.addAccountExplicitly(account, accountPassword, null);
            mAccountManager.setAuthToken(account, mAuthTokenType, intent.getStringExtra(AccountManager.KEY_AUTHTOKEN));
        }
        else {
            // Update the saved password.
            mAccountManager.setPassword(account, accountPassword);
        }

        // Save connection details in the account for the sync adapter.
        mAccountManager.setUserData(account, IttiaDbSyncAdapter.KEY_REPLICATION_ADDRESS, Integer.toString(mReplicationAddress));
        mAccountManager.setUserData(account, IttiaDbSyncAdapter.KEY_USER_NAME, userName);
        mAccountManager.setUserData(account, IttiaDbSyncAdapter.KEY_PEER_ADDRESS, Integer.toString(peerAddress));
        mAccountManager.setUserData(account, IttiaDbSyncAdapter.KEY_PEER_URI, peerUri);

        // Set authenticator result.
        Bundle result = new Bundle();
        result.putString(AccountManager.KEY_ACCOUNT_NAME, accountName);
        result.putString(AccountManager.KEY_ACCOUNT_TYPE, mAccountType);
        result.putString(AccountManager.KEY_AUTHTOKEN, intent.getStringExtra(AccountManager.KEY_AUTHTOKEN));
        setAccountAuthenticatorResult(result);

        setResult(RESULT_OK, intent);
        finish();
    }

    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    private class UserLoginTask extends AsyncTask<Void, Void, Intent> {
        public static final String PARAM_USER_PASS = "authPassword";

        /** Bundle key used to return a String error message from the background task. */
        public static final String KEY_ERROR_MESSAGE = "errorMessage";

        @Override
        protected Intent doInBackground(Void... params) {
            Bundle data;

            try {
                final String uri = mSyncProtocol + "://" + mSyncHost + "/" + mSyncConnection;

                // Authenticate with ittiasync server.
                data = IttiaDbSyncAdapter.requestAccess(uri, mUsername, mPassword, mReplicationAddress);
                data.putString(PARAM_USER_PASS, mPassword);
            }
            catch (Exception ex) {
                data = new Bundle();
                data.putString(KEY_ERROR_MESSAGE, ex.getMessage());
            }

            final Intent res = new Intent();
            res.putExtras(data);
            return res;
        }

        @Override
        protected void onPostExecute(final Intent intent) {
            mAuthTask = null;
            showProgress(false);

            if (intent.hasExtra(KEY_ERROR_MESSAGE)) {
                Toast.makeText(getBaseContext(), intent.getStringExtra(KEY_ERROR_MESSAGE), Toast.LENGTH_SHORT).show();
            } else {
                finishLogin(intent);
            }
        }

        @Override
        protected void onCancelled() {
            mAuthTask = null;
            showProgress(false);
        }
    }
}
