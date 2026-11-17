package com.sennara.app.data;

import android.content.Context;
import android.os.AsyncTask;

import com.sennara.app.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

public class Api {

    public interface Listener {
        void onResult(JSONObject data, String error);
    }

    public static class Answer {
        public JSONObject data;
        public String error;

        public boolean ok() {
            return error == null;
        }
    }

    // ================================================================ AsyncTask

    private static class RequestTask extends AsyncTask<Void, Void, Answer> {
        private Context context;
        private String file;
        private HashMap<String, String> params;
        private File photo;
        private Listener listener;

        RequestTask(Context context, String file, HashMap<String, String> params, File photo, Listener listener) {
            this.context = context.getApplicationContext();
            this.file = file;
            this.params = params;
            this.photo = photo;
            this.listener = listener;
        }

        @Override
        protected Answer doInBackground(Void... nothing) {
            if (photo == null) {
                return callNow(context, file, params);
            } else {
                return uploadNow(context, file, params, photo);
            }
        }

        @Override
        protected void onPostExecute(Answer answer) {
            if (listener != null) {
                listener.onResult(answer.data, answer.error);
            }
        }
    }

    public static void call(Context c, String file, HashMap<String, String> params, Listener listener) {
        RequestTask task = new RequestTask(c, file, params, null, listener);
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public static void upload(Context c, String file, HashMap<String, String> params, File photo, Listener listener) {
        RequestTask task = new RequestTask(c, file, params, photo, listener);
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    // ================================================================ the HTTP work itself (background thread only)

    public static Answer callNow(Context c, String file, HashMap<String, String> params) {
        HttpURLConnection con = null;
        try {
            HashMap<String, String> all = addToken(c, params);
            String body = "";
            for (Map.Entry<String, String> entry : all.entrySet()) {
                if (body.length() > 0) {
                    body = body + "&";
                }
                body = body + URLEncoder.encode(entry.getKey(), "UTF-8") + "=" + URLEncoder.encode(entry.getValue(), "UTF-8");
            }

            con = openPost(c, file);
            con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
            OutputStream out = con.getOutputStream();
            out.write(body.getBytes("UTF-8"));
            out.close();

            return readAnswer(con);
        } catch (IOException e) {
            return errorAnswer("network");
        } finally {
            if (con != null) {
                con.disconnect();
            }
        }
    }

    public static Answer uploadNow(Context c, String file, HashMap<String, String> params, File photo) {
        String boundary = "----Sennara" + System.currentTimeMillis();
        HttpURLConnection con = null;
        try {
            con = openPost(c, file);
            con.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
            OutputStream out = con.getOutputStream();

            HashMap<String, String> all = addToken(c, params);
            for (Map.Entry<String, String> entry : all.entrySet()) {
                String part = "--" + boundary + "\r\n"
                        + "Content-Disposition: form-data; name=\"" + entry.getKey() + "\"\r\n\r\n"
                        + entry.getValue() + "\r\n";
                out.write(part.getBytes("UTF-8"));
            }

            if (photo != null && photo.exists()) {
                String header = "--" + boundary + "\r\n"
                        + "Content-Disposition: form-data; name=\"photo\"; filename=\"photo.jpg\"\r\n"
                        + "Content-Type: image/jpeg\r\n\r\n";
                out.write(header.getBytes("UTF-8"));
                FileInputStream in = new FileInputStream(photo);
                byte[] buffer = new byte[8192];
                int n = in.read(buffer);
                while (n != -1) {
                    out.write(buffer, 0, n);
                    n = in.read(buffer);
                }
                in.close();
                out.write("\r\n".getBytes("UTF-8"));
            }

            out.write(("--" + boundary + "--\r\n").getBytes("UTF-8"));
            out.close();
            return readAnswer(con);
        } catch (IOException e) {
            return errorAnswer("network");
        } finally {
            if (con != null) {
                con.disconnect();
            }
        }
    }

    private static HashMap<String, String> addToken(Context c, HashMap<String, String> params) {
        HashMap<String, String> all = new HashMap<>();
        if (params != null) {
            all.putAll(params);
        }
        String token = Session.token(c);
        if (!token.isEmpty()) {
            all.put("token", token);
        }
        return all;
    }

    private static HttpURLConnection openPost(Context c, String file) throws IOException {
        URL url = new URL(Session.server(c) + file);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setConnectTimeout(6000);
        con.setReadTimeout(15000);
        con.setRequestMethod("POST");
        con.setDoOutput(true);
        return con;
    }

    private static Answer readAnswer(HttpURLConnection con) throws IOException {
        InputStream in;
        if (con.getResponseCode() >= 400) {
            in = con.getErrorStream();
        } else {
            in = con.getInputStream();
        }
        String text = readText(in);

        Answer answer = new Answer();
        try {
            JSONObject json = new JSONObject(text);
            if (json.optBoolean("ok")) {
                answer.data = json.optJSONObject("data");
                if (answer.data == null) {
                    answer.data = new JSONObject();
                }
            } else {
                answer.error = json.optString("error", "server_error");
            }
        } catch (JSONException e) {
            answer.error = "server_error";
        }
        return answer;
    }

    private static Answer errorAnswer(String code) {
        Answer answer = new Answer();
        answer.error = code;
        return answer;
    }

    public static String get(String address) throws IOException {
        URL url = new URL(address);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        try {
            con.setConnectTimeout(8000);
            con.setReadTimeout(12000);
            con.setRequestProperty("User-Agent", "Sennara/2.0 (Android; university project)");
            if (con.getResponseCode() >= 400) {
                throw new IOException("HTTP " + con.getResponseCode());
            }
            return readText(con.getInputStream());
        } finally {
            con.disconnect();
        }
    }

    public static String readText(InputStream in) throws IOException {
        if (in == null) {
            return "";
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
        StringBuilder text = new StringBuilder();
        String line = reader.readLine();
        while (line != null) {
            text.append(line).append("\n");
            line = reader.readLine();
        }
        reader.close();
        return text.toString();
    }

    public static String message(Context c, String code) {
        if (code == null) {
            return "";
        }
        switch (code) {
            case "network":
                return c.getString(R.string.error_network);
            case "invalid_input":
                return c.getString(R.string.error_invalid);
            case "email_taken":
                return c.getString(R.string.error_email_taken);
            case "bad_credentials":
                return c.getString(R.string.error_bad_credentials);
            case "forbidden":
                return c.getString(R.string.error_forbidden);
            case "not_logged_in":
                return c.getString(R.string.error_session);
            case "no_shop":
                return c.getString(R.string.error_no_shop);
            case "cannot_change_self":
                return c.getString(R.string.error_self);
            case "photo_too_big":
            case "invalid_photo":
                return c.getString(R.string.error_photo);
            default:
                return c.getString(R.string.error_server);
        }
    }
}
