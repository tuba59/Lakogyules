package szoftarch.com.lakogyules;

import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.script.model.ExecutionRequest;
import com.google.api.services.script.model.Operation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class MainMenu extends GoogleConnect  {
    ProgressDialog mProgress;
    private TextView mOutputText;

    private String voteResult;
    private String currentSheetName;
    private String currentVoterName;
    private String currentVoterShare;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);
        mOutputText = (TextView) findViewById(R.id.textViewDebug);
        mProgress = new ProgressDialog(this);
        mProgress.setMessage("Google Apps Script futtatása ...");

        // Initialize credentials and service object.
        SharedPreferences settings = getPreferences(Context.MODE_PRIVATE);
        mCredential = GoogleAccountCredential.usingOAuth2(
                getApplicationContext(), Arrays.asList(SCOPES))
                .setBackOff(new ExponentialBackOff())
                .setSelectedAccountName(settings.getString(PREF_ACCOUNT_NAME, null));

        final Button setUsers = (Button) findViewById(R.id.button_users);
        setUsers.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isGooglePlayServicesAvailable()) {
                    executeScript(API_SET_USERS_SCRIPT);
                } else {
                    mOutputText.setText("Google Play Services required: " +
                            "after installing, close and relaunch this app.");
                }
            }
        });

        final Button startPoll = (Button) findViewById(R.id.button_start);
        startPoll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                executeScript(API_START_POLL_SCRIPT);
            }
        });

        final Button createCarts = (Button) findViewById(R.id.button_carts);
        createCarts.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                executeScript(API_GET_USERS_AND_SHARES);
            }
        });

        final Button aboutButton = (Button) findViewById(R.id.button_about);
        aboutButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

            }
        });

    }

    /**
     * Called when an activity launched here (specifically, AccountPicker
     * and authorization) exits, giving you the requestCode you started it with,
     * the resultCode it returned, and any additional data from it.
     *
     * @param requestCode code indicating which activity result is incoming.
     * @param resultCode  code indicating the result of the incoming
     *                    activity result.
     * @param data        Intent (containing result data) returned by incoming
     *                    activity result.
     */
    @Override
    protected void onActivityResult(
            int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_GOOGLE_PLAY_SERVICES:
                if (resultCode != RESULT_OK) {
                    isGooglePlayServicesAvailable();
                }
                break;
            case REQUEST_ACCOUNT_PICKER:
                if (resultCode == RESULT_OK && data != null &&
                        data.getExtras() != null) {
                    String accountName =
                            data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                    if (accountName != null) {
                        mCredential.setSelectedAccountName(accountName);
                        SharedPreferences settings =
                                getPreferences(Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putString(PREF_ACCOUNT_NAME, accountName);
                        editor.apply();
                    }
                    executeScript(API_SET_USERS_SCRIPT);
                } else if (resultCode == RESULT_CANCELED) {
                    mOutputText.setText("Account unspecified.");
                }
                break;
            case REQUEST_AUTHORIZATION:
                if (resultCode != RESULT_OK) {
                    chooseAccount();
                }
                break;
            case REQUEST_QR_SCAN:
                if (resultCode == RESULT_OK) {
                    final String contents = data.getStringExtra("SCAN_RESULT");
                    int indexOfSeparator = contents.indexOf("=");
                    currentVoterName = contents.substring(0,indexOfSeparator);
                    currentVoterShare = contents.substring(indexOfSeparator+1);

                    // 1. Instantiate an AlertDialog.Builder with its constructor
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainMenu.this);

                    // 2. Chain together various setter methods to set the dialog characteristics
                    builder.setTitle("Szavazat megadása")
                            .setItems(R.array.choice_array, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    switch (which){
                                        case 0:
                                            voteResult = "1";
                                            executeScript(API_DO_VOTE_SCRIPT);
                                            break;
                                        case 1:
                                            voteResult = "-1";
                                            executeScript(API_DO_VOTE_SCRIPT);
                                            break;
                                        case 2:
                                            voteResult = "0";
                                            executeScript(API_DO_VOTE_SCRIPT);
                                            break;
                                    }
                                    mOutputText.append("\n" + voteResult);
                                    try {
                                        Intent intent = new Intent("com.google.zxing.client.android.SCAN");
                                        intent.putExtra("SCAN_MODE", "QR_CODE_MODE"); // "PRODUCT_MODE for bar codes
                                        startActivityForResult(intent, REQUEST_QR_SCAN);
                                    } catch (Exception e) {
                                        Uri marketUri = Uri.parse("market://details?id=com.google.zxing.client.android");
                                        Intent marketIntent = new Intent(Intent.ACTION_VIEW,marketUri);
                                        startActivity(marketIntent);
                                    }
                                }
                            })
                            .create()
                            .show();
                } else if(resultCode == RESULT_CANCELED){
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainMenu.this);
                    builder.setMessage("Biztosan befejezi a szavazást?")
                            .setNegativeButton("Nem", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    try {
                                        Intent intent = new Intent("com.google.zxing.client.android.SCAN");
                                        intent.putExtra("SCAN_MODE", "QR_CODE_MODE"); // "PRODUCT_MODE for bar codes
                                        startActivityForResult(intent, REQUEST_QR_SCAN);
                                    } catch (Exception e) {
                                        Uri marketUri = Uri.parse("market://details?id=com.google.zxing.client.android");
                                        Intent marketIntent = new Intent(Intent.ACTION_VIEW, marketUri);
                                        startActivity(marketIntent);
                                    }
                                }
                            })
                            .setPositiveButton("Igen", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    executeScript(API_END_POLL_SCRIPT);
                                }
                            })
                            .create()
                            .show();
                }
                break;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * Attempt to get a set of data from the Google Apps Script Execution API to display. If the
     * email address isn't known yet, then call chooseAccount() method so the
     * user can pick an account.
     */
    private void executeScript(String script_name) {
        if (mCredential.getSelectedAccountName() == null) {
            chooseAccount();
        } else {
            if (isDeviceOnline()) {
                new MakeRequestTask(mCredential, script_name).execute();
            } else {
                mOutputText.setText("No network connection available.");
            }
        }
    }




    /**
     * An asynchronous task that handles the Google Apps Script Execution API call.
     * Placing the API calls in their own task ensures the UI stays responsive.
     */
    private class MakeRequestTask extends AsyncTask<Void, Void, List<String>> {
        private com.google.api.services.script.Script mService = null;
        private Exception mLastError = null;
        private String scriptName = null;

        public MakeRequestTask(GoogleAccountCredential credential, String script_name) {
            HttpTransport transport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            scriptName = script_name;
            mService = new com.google.api.services.script.Script.Builder(
                    transport, jsonFactory, setHttpTimeout(credential))
                    .setApplicationName("Lakogyules API")
                    .build();
        }

        /**
         * Background task to call Google Apps Script Execution API.
         *
         * @param params no parameters needed for this task.
         */
        @Override
        protected List<String> doInBackground(Void... params) {
            try {
                return getDataFromApi();
            } catch (Exception e) {
                mLastError = e;
                cancel(true);
                return null;
            }
        }

        /**
         * Call the API to run an Apps Script function that returns a list
         * of folders within the user's root directory on Drive.
         *
         * @return list of String folder names and their IDs
         * @throws IOException
         */
        private List<String> getDataFromApi()
                throws IOException, GoogleAuthException {

            List<Object> params = new ArrayList<Object>();
            List<String> returnList = new ArrayList<String>();

            // Ezzel lehet felparaméterezni a különböző Apps script függvényeket
            switch (scriptName) {
                case API_EXAMPLE_SCRIPT:
                    String sheetId = "1234";
                    params.add(sheetId);
                    break;
                case API_END_POLL_SCRIPT:
                    // itt tudni kell a sheet nevet (tipikusan 'Szavazás x')
                    params.add(currentSheetName);
                    break;
                case API_DO_VOTE_SCRIPT:
                    // itt tudni kell a sheet nevet (tipikusan 'Szavazás x'), kicsoda, tulajdona (szám), mire szavaz (-1,0,1)
                    params.add(currentSheetName);
                    params.add(currentVoterName);
                    params.add(currentVoterShare);
                    params.add(voteResult);
                    break;
            }


            // Create an execution request object.
            ExecutionRequest request = new ExecutionRequest()
                    .setFunction(scriptName)
                    .setParameters(params)
                    .setDevMode(true);

            // Make the request.
            Operation op =
                    mService.scripts().run(scriptId, request).execute();

            // Print results of request.
            if (op.getError() != null) {
                throw new IOException(getScriptError(op));
            }
            if (op.getResponse() != null &&
                    op.getResponse().get("result") != null) {
                // The result provided by the API needs to be cast into
                // the correct type, based upon what types the Apps Script
                // function returns. Here, the function returns an Apps
                // Script Object with String keys and values, so must be
                // cast into a Java Map (folderSet).

                // Apps script feldolgozások
                switch (scriptName) {
                    case API_EXAMPLE_SCRIPT:
                        Map<String, String> folderSet =
                                (Map<String, String>) (op.getResponse().get("result"));

                        for (String id : folderSet.keySet()) {
                            returnList.add(
                                    String.format("%s (%s)", folderSet.get(id), id));
                        }
                        break;
                    case API_SET_USERS_SCRIPT:
                        // megnyitja a google sheets-et
                        Intent i = new Intent(Intent.ACTION_VIEW);
                        returnList.add((String) op.getResponse().get("result"));
                        i.setData(Uri.parse((String) op.getResponse().get("result")));
                        startActivity(i);
                        break;
                    case API_START_POLL_SCRIPT:
                        // elindítja a beolvasást, meg kiolvassa a Sheet nevet.
                        returnList.add((String) op.getResponse().get("result"));
                        currentSheetName = (String) op.getResponse().get("result");
                        try {
                            Intent intent = new Intent("com.google.zxing.client.android.SCAN");
                            intent.putExtra("SCAN_MODE", "QR_CODE_MODE"); // "PRODUCT_MODE for bar codes
                            startActivityForResult(intent, REQUEST_QR_SCAN);
                        } catch (Exception e) {
                            Uri marketUri = Uri.parse("market://details?id=com.google.zxing.client.android");
                            Intent marketIntent = new Intent(Intent.ACTION_VIEW,marketUri);
                            startActivity(marketIntent);
                        }
                        break;
                    case API_DO_VOTE_SCRIPT:
                        returnList.add((String) op.getResponse().get("result"));
                        // Visszatér a QR olvasóhoz

                        break;
                    case API_END_POLL_SCRIPT:
                        // megnyitja a megfelelő szavazási eredményeket google sheets-en
                        Intent in = new Intent(Intent.ACTION_VIEW);
                        returnList.add((String) op.getResponse().get("result"));
                        in.setData(Uri.parse((String) op.getResponse().get("result")));
                        startActivity(in);
                        break;
                    case API_GET_USERS_AND_SHARES:
                        Map<String, String> userSet =
                                (Map<String, String>) (op.getResponse().get("result"));
                        for (String id : userSet.keySet()) {
                            returnList.add(
                                    String.format("%s (%s)", userSet.get(id), id));
                        }
                        if(!userSet.isEmpty())
                            PdfUtility.generatePDF(userSet, MainMenu.this);

                        break;
                }

            }

            return returnList;
        }

        /**
         * Interpret an error response returned by the API and return a String
         * summary.
         *
         * @param op the Operation returning an error response
         * @return summary of error response, or null if Operation returned no
         * error
         */
        private String getScriptError(Operation op) {
            if (op.getError() == null) {
                return null;
            }

            // Extract the first (and only) set of error details and cast as a Map.
            // The values of this map are the script's 'errorMessage' and
            // 'errorType', and an array of stack trace elements (which also need to
            // be cast as Maps).
            Map<String, Object> detail = op.getError().getDetails().get(0);
            List<Map<String, Object>> stacktrace =
                    (List<Map<String, Object>>) detail.get("scriptStackTraceElements");

            java.lang.StringBuilder sb =
                    new StringBuilder("\nScript error message: ");
            sb.append(detail.get("errorMessage"));

            if (stacktrace != null) {
                // There may not be a stacktrace if the script didn't start
                // executing.
                sb.append("\nScript error stacktrace:");
                for (Map<String, Object> elem : stacktrace) {
                    sb.append("\n  ");
                    sb.append(elem.get("function"));
                    sb.append(":");
                    sb.append(elem.get("lineNumber"));
                }
            }
            sb.append("\n");
            return sb.toString();
        }


        @Override
        protected void onPreExecute() {
            mOutputText.setText("");
            mProgress.show();
        }

        @Override
        protected void onPostExecute(List<String> output) {
            mProgress.hide();
            if (output == null || output.size() == 0) {
                mOutputText.setText("No results returned.");
            } else {
                output.add(0, "Data retrieved using the Google Apps Script Execution API:");
                mOutputText.setText(TextUtils.join("\n", output));
            }
        }

        @Override
        protected void onCancelled() {
            mProgress.hide();
            if (mLastError != null) {
                if (mLastError instanceof GooglePlayServicesAvailabilityIOException) {
                    showGooglePlayServicesAvailabilityErrorDialog(
                            ((GooglePlayServicesAvailabilityIOException) mLastError)
                                    .getConnectionStatusCode());
                } else if (mLastError instanceof UserRecoverableAuthIOException) {
                    startActivityForResult(
                            ((UserRecoverableAuthIOException) mLastError).getIntent(),
                            MainMenu.REQUEST_AUTHORIZATION);
                } else {
                    mOutputText.setText("The following error occurred:\n"
                            + mLastError.getMessage());
                }
            } else {
                mOutputText.setText("Request cancelled.");
            }
        }
    }
}
