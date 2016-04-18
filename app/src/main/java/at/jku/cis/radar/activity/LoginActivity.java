package at.jku.cis.radar.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.Serializable;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import at.jku.cis.radar.R;
import at.jku.cis.radar.model.AuthenticationToken;
import at.jku.cis.radar.model.Credentials;
import at.jku.cis.radar.rest.AuthenticationRestApi;
import at.jku.cis.radar.rest.RestServiceGenerator;

public class LoginActivity extends AppCompatActivity {

    private AutoCompleteTextView emailView;
    private EditText passwordView;

    private AuthenticationTask authenticationTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Set up the login form.
        emailView = (AutoCompleteTextView) findViewById(R.id.email);
        passwordView = (EditText) findViewById(R.id.password);
        passwordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

        Button mEmailSignInButton = (Button) findViewById(R.id.email_sign_in_button);
        mEmailSignInButton.setOnClickListener(new OnClickListener() {
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
        if (authenticationTask != null) {
            return;
        }
        emailView.setError(null);
        passwordView.setError(null);

        // Store values at the time of the login attempt.
        String email = emailView.getText().toString();
        String password = passwordView.getText().toString();

        authenticationTask = new AuthenticationTask(email, password);
        authenticationTask.execute((Void) null);
    }

    private class AuthenticationTask extends AsyncTask<Void, Void, Boolean> {
        private String username;
        private String password;

        private AuthenticationToken authenticationToken;

        AuthenticationTask(String username, String password) {
            this.username = username;
            this.password = password;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                Credentials credentials = new Credentials();
                credentials.setUsername(username);
                credentials.setPassword(toMd5HexString(password));
                authenticationToken = RestServiceGenerator.createService(AuthenticationRestApi.class).authenticate(credentials);
            } catch (Exception ex) {
                return false;
            }
            return true;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            if (success) {
                Intent intent = new Intent(getBaseContext(), RadarActivity.class);
                intent.putExtra(AuthenticationToken.class.getSimpleName(), (Serializable) authenticationToken);
                startActivity(intent);
                finishActivity(Intent.FLAG_ACTIVITY_NO_HISTORY);
            } else {
                passwordView.setError(getString(R.string.error_incorrect_password));
                passwordView.requestFocus();
            }
            authenticationTask = null;
        }

        private String toMd5HexString(String value) throws NoSuchAlgorithmException {
            MessageDigest md = MessageDigest.getInstance("MD5");
            return String.format("%032X", new BigInteger(1, md.digest(value.getBytes())));
        }
    }
}

