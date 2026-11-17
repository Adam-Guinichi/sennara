package com.sennara.app.data;

import android.content.Context;
import android.os.AsyncTask;

import com.sennara.app.R;
import com.sennara.app.util.Images;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.HashMap;

public class SyncManager {

    public interface Listener {
        void onProgress(int percent, String status);

        void onDone(boolean ok, String error);
    }

    public static void sync(Context c, Listener listener) {
        SyncTask task = new SyncTask(c, listener);
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public static void push(Context c, Runnable after) {
        PushTask task = new PushTask(c, after);
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    // ================================================================ full sync

    private static class SyncTask extends AsyncTask<Void, String, String> {
        private Context context;
        private Listener listener;

        SyncTask(Context c, Listener listener) {
            this.context = c.getApplicationContext();
            this.listener = listener;
        }

        @Override
        protected String doInBackground(Void... nothing) {
            Db db = Db.get(context);

            publishProgress("10", context.getString(R.string.sync_step_data));
            Api.Answer data = Api.callNow(context, "data.php", new HashMap<String, String>());
            if (!data.ok()) {
                return data.error;
            }
            db.replaceAll(data.data);

            try {
                String credits = Api.get(Session.server(context) + "img/credits.json");
                db.setValue("credits", credits);
            } catch (Exception e) {
            }

            publishProgress("40", context.getString(R.string.sync_step_catches));
            String error = pushCatches(context);
            publishProgress("50", context.getString(R.string.sync_step_catches));
            if (error == null) {
                error = pushFavorites(context);
            }

            ArrayList<String> photos = db.imagePaths();
            int total = photos.size();
            if (total == 0) {
                total = 1;
            }
            for (int i = 0; i < photos.size(); i++) {
                Images.download(context, photos.get(i));
                int percent = 55 + (i + 1) * 45 / total;
                String status = context.getString(R.string.sync_step_photos, i + 1, photos.size());
                publishProgress(String.valueOf(percent), status);
            }

            if (error == null) {
                Session.setLastSync(context, System.currentTimeMillis());
            }
            return error;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            if (listener != null) {
                listener.onProgress(Integer.parseInt(values[0]), values[1]);
            }
        }

        @Override
        protected void onPostExecute(String error) {
            if (listener != null) {
                listener.onDone(error == null, error);
            }
        }
    }

    // ================================================================ push only

    private static class PushTask extends AsyncTask<Void, Void, Void> {
        private Context context;
        private Runnable after;

        PushTask(Context c, Runnable after) {
            this.context = c.getApplicationContext();
            this.after = after;
        }

        @Override
        protected Void doInBackground(Void... nothing) {
            pushCatches(context);
            pushFavorites(context);
            return null;
        }

        @Override
        protected void onPostExecute(Void nothing) {
            if (after != null) {
                after.run();
            }
        }
    }

    // ================================================================ the two sync steps (background thread)

    private static String pushCatches(Context c) {
        int userId = Session.userId(c);
        Db db = Db.get(c);
        JSONArray pending = db.unsyncedCatches(userId);

        HashMap<String, String> params = new HashMap<>();
        params.put("catches", pending.toString());
        Api.Answer answer = Api.callNow(c, "catches.php", params);
        if (!answer.ok()) {
            return answer.error;
        }
        db.mergeRemoteCatches(userId, answer.data.optJSONArray("catches"));
        return null;
    }

    private static String pushFavorites(Context c) {
        int userId = Session.userId(c);
        Db db = Db.get(c);

        HashMap<String, String> params = new HashMap<>();
        params.put("changes", db.pendingFavorites(userId).toString());
        Api.Answer answer = Api.callNow(c, "favorites.php", params);
        if (!answer.ok()) {
            return answer.error;
        }
        db.replaceFavorites(userId, answer.data.optJSONArray("favorites"));
        return null;
    }
}
