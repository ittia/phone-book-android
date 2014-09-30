package com.example.android.ittiadb.phonebook;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.content.Context;
import android.content.Intent;
import android.database.ittiadb.IttiaDbDatabase;
import android.database.ittiadb.IttiaDbException;
import android.database.ittiadb.IttiaDbSyncAdapter;
import android.os.Bundle;
import android.text.TextUtils;

public class SyncAuthenticator extends AbstractAccountAuthenticator {
    public static final String AUTH_TOKEN_TYPE_REPLICATE = "ITTIA Replication Exchange";

    private final Context mContext;
    private final IttiaDbDatabase mDatabase;
    private final String mPeerUri;

    public SyncAuthenticator(Context context, IttiaDbDatabase database, String peerUri) {
        super(context);
        mContext = context;
        mDatabase = database;
        mPeerUri = peerUri;
    }

    @Override
    public Bundle addAccount(AccountAuthenticatorResponse response,
            String accountType, String authTokenType,
            String[] requiredFeatures, Bundle options)
            throws NetworkErrorException
    {
        // Start LoginActivity to prompt for credentials.
        final Intent intent = new Intent(mContext, LoginActivity.class);

        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
        intent.putExtra(LoginActivity.EXTRA_ACCOUNT_TYPE, accountType);
        intent.putExtra(LoginActivity.EXTRA_AUTH_TYPE, authTokenType);

        // Accounts should always use the same replication address as the
        // local database, if it has already been assigned.
        intent.putExtra(LoginActivity.EXTRA_REPLICATION_ADDRESS, mDatabase.getReplicationAddress());

        intent.putExtra(LoginActivity.EXTRA_PEER_URI, mPeerUri);

        final Bundle bundle = new Bundle();
        bundle.putParcelable(AccountManager.KEY_INTENT, intent);
        return bundle;
    }

    @Override
    public Bundle getAuthToken(AccountAuthenticatorResponse response,
            Account account, String authTokenType, Bundle options)
            throws NetworkErrorException
    {
        final AccountManager am = AccountManager.get(mContext);

        String authToken = am.peekAuthToken(account, authTokenType);

        // Request an auth token if the account does not have one, but does
        // have the necessary credentials. This will happen if the account was
        // created explicitly or if the previous token became invalid.
        if (TextUtils.isEmpty(authToken)) {
            final String password = am.getPassword(account);
            String userName = am.getUserData(account, IttiaDbSyncAdapter.KEY_USER_NAME);
            String uri = am.getUserData(account, IttiaDbSyncAdapter.KEY_PEER_URI);

            if (password != null && userName != null && uri != null) {
                try {
                    Bundle result = IttiaDbSyncAdapter.requestAccess(uri, userName, password, mDatabase.getReplicationAddress());

                    // Update authToken and the sync addresses.
                    authToken = result.getString(AccountManager.KEY_AUTHTOKEN);
                    final int replicationAddress = result.getInt(IttiaDbSyncAdapter.KEY_REPLICATION_ADDRESS, IttiaDbDatabase.REP_ADDRESS_NONE);
                    final int peerAddress = result.getInt(IttiaDbSyncAdapter.KEY_PEER_ADDRESS, IttiaDbDatabase.REP_ADDRESS_NONE);

                    am.setAuthToken(account, authTokenType, authToken);
                    am.setUserData(account, IttiaDbSyncAdapter.KEY_REPLICATION_ADDRESS, Integer.toString(replicationAddress));
                    am.setUserData(account, IttiaDbSyncAdapter.KEY_PEER_ADDRESS, Integer.toString(peerAddress));
                }
                catch (IttiaDbException ex) {
                    // Leave authToken empty to prompt for credentials below.
                }
            }
        }

        if (!TextUtils.isEmpty(authToken)) {
            // Return the token stored in the account.
            final Bundle result = new Bundle();

            result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
            result.putString(AccountManager.KEY_ACCOUNT_TYPE, account.type);
            result.putString(AccountManager.KEY_AUTHTOKEN, authToken);

            return result;
        }
        else {
            // Prompt for credentials to obtain a token.
            final Intent intent = new Intent(mContext, LoginActivity.class);

            intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
            intent.putExtra(LoginActivity.EXTRA_ACCOUNT_NAME, account.name);
            intent.putExtra(LoginActivity.EXTRA_ACCOUNT_TYPE, account.type);
            intent.putExtra(LoginActivity.EXTRA_AUTH_TYPE, authTokenType);

            // Accounts should always use the same replication address as the
            // local database. Ignore the address saved in the account.
            intent.putExtra(LoginActivity.EXTRA_REPLICATION_ADDRESS, mDatabase.getReplicationAddress());

            // Provide existing connection details.
            intent.putExtra(LoginActivity.EXTRA_USER_NAME, am.getUserData(account, IttiaDbSyncAdapter.KEY_USER_NAME));
            intent.putExtra(LoginActivity.EXTRA_PEER_URI, am.getUserData(account, IttiaDbSyncAdapter.KEY_PEER_URI));

            final Bundle bundle = new Bundle();
            bundle.putParcelable(AccountManager.KEY_INTENT, intent);

            return bundle;
        }
    }

    @Override
    public String getAuthTokenLabel(String authTokenType) {
        return authTokenType;
    }

    @Override
    public Bundle hasFeatures(AccountAuthenticatorResponse response,
            Account account, String[] features) throws NetworkErrorException
    {
        final Bundle result = new Bundle();
        // The account has none of the requested features.
        result.putBoolean(android.accounts.AccountManager.KEY_BOOLEAN_RESULT, false);
        return result;
    }

    @Override
    public Bundle confirmCredentials(AccountAuthenticatorResponse response,
            Account account, Bundle options) throws NetworkErrorException {
        return null;
    }

    @Override
    public Bundle editProperties(AccountAuthenticatorResponse response,
            String accountType) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Bundle updateCredentials(AccountAuthenticatorResponse response,
            Account account, String authTokenType, Bundle options)
            throws NetworkErrorException {
        throw new UnsupportedOperationException();
    }
}
