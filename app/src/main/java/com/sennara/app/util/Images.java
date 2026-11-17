package com.sennara.app.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.LruCache;
import android.widget.ImageView;

import com.sennara.app.R;
import com.sennara.app.data.Session;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class Images {

    private static LruCache<String, Bitmap> memory = new LruCache<String, Bitmap>((int) (Runtime.getRuntime().maxMemory() / 8)) {
        @Override
        protected int sizeOf(String key, Bitmap value) {
            return value.getByteCount();
        }
    };

    public static File file(Context c, String path) {
        if (path.startsWith("/")) {
            return new File(path);
        }
        File folder = new File(c.getFilesDir(), "img");
        if (!folder.exists()) {
            folder.mkdirs();
        }
        return new File(folder, path.replace('/', '_'));
    }

    public static boolean download(Context c, String path) {
        if (path == null || path.isEmpty() || path.startsWith("/")) {
            return false;
        }
        File target = file(c, path);
        if (target.exists() && target.length() > 0) {
            return true;
        }
        File temporary = new File(target.getPath() + ".part");
        HttpURLConnection con = null;
        try {
            URL url = new URL(Session.server(c) + path);
            con = (HttpURLConnection) url.openConnection();
            con.setConnectTimeout(6000);
            con.setReadTimeout(15000);
            if (con.getResponseCode() != 200) {
                return false;
            }
            InputStream in = con.getInputStream();
            FileOutputStream out = new FileOutputStream(temporary);
            byte[] buffer = new byte[16384];
            int n = in.read(buffer);
            while (n != -1) {
                out.write(buffer, 0, n);
                n = in.read(buffer);
            }
            out.close();
            in.close();
            return temporary.renameTo(target);
        } catch (Exception e) {
            temporary.delete();
            return false;
        } finally {
            if (con != null) {
                con.disconnect();
            }
        }
    }

    public static void load(ImageView view, String path) {
        load(view, path, 720);
    }

    public static void load(ImageView view, String path, int width) {
        if (path == null || path.isEmpty()) {
            view.setTag(R.id.image_key, null);
            view.setImageDrawable(null);
            return;
        }
        String key = path + "@" + width;
        view.setTag(R.id.image_key, key);
        Bitmap saved = memory.get(key);
        if (saved != null) {
            view.setImageBitmap(saved);
            return;
        }
        view.setImageDrawable(null);
        LoadTask task = new LoadTask(view, path, width, key);
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private static class LoadTask extends AsyncTask<Void, Void, Bitmap> {
        private ImageView view;
        private Context context;
        private String path;
        private int width;
        private String key;

        LoadTask(ImageView view, String path, int width, String key) {
            this.view = view;
            this.context = view.getContext().getApplicationContext();
            this.path = path;
            this.width = width;
            this.key = key;
        }

        @Override
        protected Bitmap doInBackground(Void... nothing) {
            File f = file(context, path);
            if (!f.exists()) {
                download(context, path);
            }
            Bitmap bitmap = decode(f, width);
            if (bitmap != null) {
                memory.put(key, bitmap);
            }
            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (bitmap == null) {
                return;
            }
            if (!key.equals(view.getTag(R.id.image_key))) {
                return;
            }
            view.setAlpha(0f);
            view.setImageBitmap(bitmap);
            view.animate().alpha(1f).setDuration(220).start();
        }
    }

    public static Bitmap decode(File f, int width) {
        if (f == null || !f.exists()) {
            return null;
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(f.getPath(), options);
        int sample = 1;
        while (options.outWidth / (sample * 2) >= width) {
            sample = sample * 2;
        }
        options.inJustDecodeBounds = false;
        options.inSampleSize = sample;
        return BitmapFactory.decodeFile(f.getPath(), options);
    }
}
