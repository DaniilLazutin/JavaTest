package com.raghav.paint;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends Activity {
    private Button btnLinkToLogin;
    private EditText inputName;
    private EditText inputPassword;
    public static final String HOST = "http://172.16.89.6:3000";
    public String mToken;

    class Request {
        public URL url;
        public String method;
        public HashMap<String, String> props;
        public JSONObject data;

        public Request(URL url, String method, HashMap<String, String> props, JSONObject data) {
            this.url = url;
            this.method = method;
            this.props = props;
            this.data = data;
        }
    }

    class NetworkTask extends AsyncTask<Request, Void, JSONObject> {
        @Override
        protected JSONObject doInBackground(Request[] objects) {
            return connectAndSend(objects[0]);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        inputName = findViewById(R.id.name);
        inputPassword = findViewById(R.id.password);
        btnLinkToLogin = findViewById(R.id.btnLinkToLoginScreen);

        btnLinkToLogin.setOnClickListener(new View.OnClickListener() {

            public void onClick(View view) {
                Intent i = new Intent(getApplicationContext(),
                        LoginActivity.class);
                startActivity(i);
                finish();
            }
        });

    }

    private JSONObject connectAndSend(Request request) {
        try {
            HttpURLConnection con = (HttpURLConnection) request.url.openConnection();
            con.setRequestMethod(request.method);
            for (Map.Entry<String, String> entry : request.props.entrySet()) {
                con.setRequestProperty(entry.getKey(), entry.getValue());
            }

            if (request.data.length() > 0) {
                OutputStream os = con.getOutputStream();
                os.write(request.data.toString().getBytes());
            }
            InputStream is = con.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            StringBuilder builder = new StringBuilder();

            String line;
            while ((line = br.readLine()) != null) {
                builder.append(line);
            }
            String result = builder.toString();
            return new JSONObject(result);
        } catch (IOException | JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void onSignup(View view) {
        System.out.println(inputName.getText().toString().trim());
        if (!inputName.toString().isEmpty() && !inputPassword.toString().isEmpty()) {
            URL url = null;
            try {
                url = new URL(HOST + "/api/signup");
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            NetworkTask t = new NetworkTask();

            try {
                JSONObject data = new JSONObject();
                data.put("login", inputName.getText().toString().trim());
                data.put("password", inputPassword.getText().toString().trim());
                HashMap<String, String> props = new HashMap<>();
                props.put("Content-Type", "application/json");
                Request request = new Request(url, "POST", props, data);

                t.execute(request);

                JSONObject result = t.get();
                Log.i(MainActivity.class.getSimpleName(), "OUTPUT Result: " + result);
                String status;
                try {
                    status = result.getString("status");
                    System.out.println(status);
                    System.out.println(status.equals("success"));
                } catch (Exception e) {
                    e.printStackTrace();
                    status = "denied";
                }
                if (status.equals("success")) {
//                    mToken = result.getString("token");
                    Toast.makeText(getApplicationContext(), "Registration was successful",
                            Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(
                            RegisterActivity.this,
                            LoginActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(getApplicationContext(), "User already exists!",
                            Toast.LENGTH_SHORT).show();
                }
                Log.i(MainActivity.class.getSimpleName(), "OUTPUT Token: " + mToken);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            Toast.makeText(getApplicationContext(), "Please, enter user and password",
                    Toast.LENGTH_LONG).show();
        }
    }

}