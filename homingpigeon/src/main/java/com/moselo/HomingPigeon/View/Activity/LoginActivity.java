package com.moselo.HomingPigeon.View.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.support.design.widget.TextInputEditText;
import android.util.Patterns;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.moselo.HomingPigeon.API.View.DefaultDataView;
import com.moselo.HomingPigeon.Helper.HomingPigeon;
import com.moselo.HomingPigeon.Helper.HomingPigeonDialog;
import com.moselo.HomingPigeon.Helper.Utils;
import com.moselo.HomingPigeon.Manager.ConnectionManager;
import com.moselo.HomingPigeon.Manager.DataManager;
import com.moselo.HomingPigeon.Model.AuthTicketResponse;
import com.moselo.HomingPigeon.Model.ErrorModel;
import com.moselo.HomingPigeon.Model.GetAccessTokenResponse;
import com.moselo.HomingPigeon.R;

import java.net.URL;

import static com.moselo.HomingPigeon.Helper.DefaultConstant.K_ACCESS_TOKEN;
import static com.moselo.HomingPigeon.Helper.DefaultConstant.K_ACCESS_TOKEN_EXPIRY;
import static com.moselo.HomingPigeon.Helper.DefaultConstant.K_AUTH_TICKET;
import static com.moselo.HomingPigeon.Helper.DefaultConstant.K_MY_USERNAME;
import static com.moselo.HomingPigeon.Helper.DefaultConstant.K_REFRESH_TOKEN;
import static com.moselo.HomingPigeon.Helper.DefaultConstant.K_REFRESH_TOKEN_EXPIRY;

public class LoginActivity extends BaseActivity {

    private static final String TAG = LoginActivity.class.getSimpleName();
    private TextInputEditText etUsername;
    private TextInputEditText etPassword;
    private TextView tvSignIn;
    private ProgressBar progressBar;
    private View vOverlay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initView();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void initView() {
        etUsername = findViewById(R.id.et_username);
        etPassword = findViewById(R.id.et_password);
        tvSignIn = findViewById(R.id.tv_sign_in);
        progressBar = findViewById(R.id.pb_signing_in);
        vOverlay = findViewById(R.id.v_signing_in);

        etPassword.setOnEditorActionListener((v, actionId, event) -> {
            attemptLogin();
            return false;
        });

        tvSignIn.setOnClickListener(v -> attemptLogin());

        vOverlay.setOnClickListener(v -> {
        });
    }

    private boolean isEmailValid(CharSequence email) {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private boolean isPasswordValid(String password) {
        return password.length() > 5;
    }

    private void attemptLogin() {
        if (etUsername.getText().toString().equals("")) {
            etUsername.setError("Please fill your username.");
        } else if (etPassword.getText().toString().equals("")) {
            etPassword.setError("Please fill your password.");
        } else if (!checkValidUsername(etUsername.getText().toString())) {
            etUsername.setError("Please enter valid username.");
        } else {
            Utils.getInstance().dismissKeyboard(this);
            progressBar.setVisibility(View.VISIBLE);
            vOverlay.setVisibility(View.VISIBLE);

            new Thread(() -> {
                try {
                    setDataAndCallAPI();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }

    private void setDataAndCallAPI() throws Exception {
        String ipAddress = Utils.getInstance().getStringFromURL(new URL("https://api.ipify.org/"));
        String userAgent = "android";
        String userPlatform = "android";
        String xcUserID = getDummyUserID(etUsername.getText().toString()) + "";
        String fullname = etUsername.getText().toString();
        String email = etUsername.getText().toString() + "@moselo.com";
        String phone = "08979809026";
        String username = etUsername.getText().toString();
        String deviceID = Settings.Secure.getString(HomingPigeon.appContext.getContentResolver(), Settings.Secure.ANDROID_ID);
        DataManager.getInstance().getAuthTicket(ipAddress, userAgent, userPlatform, deviceID, xcUserID,
                fullname, email, phone, username, authView);
    }

    // TODO: 14/09/18 nanti ini harus dihilangin (Wajib)
    private boolean checkValidUsername(String username) {
        switch (username) {
            case "ritchie":
            case "dominic":
            case "rionaldo":
            case "kevin":
            case "welly":
            case "jony":
            case "michael":
            case "richard":
            case "erwin":
            case "jefry":
            case "cundy":
            case "rizka":
            case "test1":
            case "test2":
            case "test3":
                return true;

            default:
                return false;
        }
    }

    // TODO: 14/09/18 nanti ini harus dihilangin (Wajib)
    private int getDummyUserID(String username) {
        switch (username) {
            case "ritchie":
                return 1;
            case "dominic":
                return 2;
            case "rionaldo":
                return 3;
            case "kevin":
                return 4;
            case "welly":
                return 5;
            case "jony":
                return 6;
            case "michael":
                return 7;
            case "richard":
                return 8;
            case "erwin":
                return 9;
            case "jefry":
                return 10;
            case "cundy":
                return 11;
            case "rizka":
                return 12;
            case "test1":
                return 13;
            case "test2":
                return 14;
            case "test3":
                return 15;
            default:
                return 0;
        }
    }

    DefaultDataView<AuthTicketResponse> authView = new DefaultDataView<AuthTicketResponse>() {
        @Override
        public void startLoading() {
            super.startLoading();
        }

        @Override
        public void endLoading() {
            super.endLoading();
        }

        @Override
        public void onSuccess(AuthTicketResponse response) {
            super.onSuccess(response);
            DataManager.getInstance().saveStringPreference(LoginActivity.this, response.getTicket(), K_AUTH_TICKET);
            DataManager.getInstance().getAccessTokenFromApi(accessTokenView);
        }

        @Override
        public void onError(ErrorModel error) {
            super.onError(error);
            showDialog("ERROR "+error.getCode(), error.getMessage());
        }
    };

    DefaultDataView<GetAccessTokenResponse> accessTokenView = new DefaultDataView<GetAccessTokenResponse>() {
        @Override
        public void startLoading() {
            super.startLoading();
        }

        @Override
        public void endLoading() {
            super.endLoading();
        }

        @Override
        public void onSuccess(GetAccessTokenResponse response) {
            super.onSuccess(response);
            DataManager.getInstance().deletePreference(LoginActivity.this, K_AUTH_TICKET);

            DataManager.getInstance().saveStringPreference(LoginActivity.this, response.getRefreshToken(), K_REFRESH_TOKEN);
            DataManager.getInstance().saveLongTimestampPreference(LoginActivity.this, response.getRefreshTokenExpiry(), K_REFRESH_TOKEN_EXPIRY);
            DataManager.getInstance().saveStringPreference(LoginActivity.this, response.getAccessToken(), K_ACCESS_TOKEN);
            DataManager.getInstance().saveLongTimestampPreference(LoginActivity.this, response.getAccessTokenExpiry(), K_ACCESS_TOKEN_EXPIRY);

            DataManager.getInstance().saveActiveUser(LoginActivity.this, response.getUser());
            runOnUiThread(() -> {
                Intent intent = new Intent(LoginActivity.this, RoomListActivity.class);
                intent.putExtra(K_MY_USERNAME, etUsername.getText().toString());
                startActivity(intent);
                ConnectionManager.getInstance().connect();
                finish();
            });
        }

        @Override
        public void onError(ErrorModel error) {
            super.onError(error);
            showDialog("ERROR "+error.getCode(), error.getMessage());
        }
    };

    private void showDialog(String title, String message) {
        new HomingPigeonDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPrimaryButtonTitle("OK")
                .setPrimaryButtonListener(view -> {
                    progressBar.setVisibility(View.GONE);
                    vOverlay.setVisibility(View.GONE);
                }).show();
    }
}
