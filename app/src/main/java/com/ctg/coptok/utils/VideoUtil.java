package com.ctg.coptok.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.util.Size;

import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;

import com.otaliastudios.transcoder.source.DataSource;
import com.otaliastudios.transcoder.source.FilePathDataSource;
import com.otaliastudios.transcoder.source.UriDataSource;

import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.concurrent.TimeUnit;

import com.ctg.coptok.data.models.Clip;
import com.ctg.coptok.workers.FileDownloadWorker;
import com.ctg.coptok.workers.FixFastStartWorker3;
import com.ctg.coptok.workers.WatermarkWorker;

final public class VideoUtil {

    private static final String TAG = "VideoUtil";

    public static DataSource createDataSource(Context context, String file) {
        if (file.startsWith(ContentResolver.SCHEME_CONTENT)) {
            return new UriDataSource(context, Uri.parse(file));
        }

        if (file.startsWith(ContentResolver.SCHEME_FILE)) {
            file = Uri.parse(file).getPath();
        }

        return new FilePathDataSource(file);
    }

    public static OneTimeWorkRequest createDownloadRequest(String url, File output, boolean notification) {
        Data data = new Data.Builder()
                .putString(FileDownloadWorker.KEY_INPUT, url)
                .putString(FileDownloadWorker.KEY_OUTPUT, output.getAbsolutePath())
                .putBoolean(FileDownloadWorker.KEY_NOTIFICATION, notification)
                .build();
        return new OneTimeWorkRequest.Builder(FileDownloadWorker.class)
                .setInputData(data)
                .build();
    }

    public static OneTimeWorkRequest createFastStartRequest(File input, File output) {
        Data data = new Data.Builder()
                .putString(FixFastStartWorker3.KEY_INPUT, input.getAbsolutePath())
                .putString(FixFastStartWorker3.KEY_OUTPUT, output.getAbsolutePath())
                .build();
        return new OneTimeWorkRequest.Builder(FixFastStartWorker3.class)
                .setInputData(data)
                .build();
    }

    public static OneTimeWorkRequest createWatermarkRequest(Clip clip, File input, File output, boolean notification) {
        Data data = new Data.Builder()
                .putString(WatermarkWorker.KEY_INPUT, input.getAbsolutePath())
                .putString(WatermarkWorker.KEY_USERNAME, '@' + clip.user.username)
                .putString(WatermarkWorker.KEY_OUTPUT, output.getAbsolutePath())
                .putBoolean(WatermarkWorker.KEY_NOTIFICATION, notification)
                .build();
        return new OneTimeWorkRequest.Builder(WatermarkWorker.class)
                .setInputData(data)
                .build();
    }

    public static Size getBestFit(Size of, Size within) {
        int w = of.getWidth();
        int h = of.getHeight();
        if (h > within.getHeight()) {
            w = (w * within.getHeight()) / h;
            h = within.getHeight();
        }

        if (w > within.getWidth()) {
            h = (h * within.getWidth()) / w;
            w = within.getWidth();
        }

        if (w % 2 != 0) {
            w += 1;
        }

        if (h % 2 != 0) {
            h += 1;
        }

        return new Size(w, h);
    }

    public static Size getDimensions(String path) {
        int width = 0, height = 0;
        MediaMetadataRetriever mmr = null;
        try {
            mmr = new MediaMetadataRetriever();
            mmr.setDataSource(path);
            String w = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
            String h = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
            if (w != null && h != null) {
                width = Integer.parseInt(w);
                height = Integer.parseInt(h);
            }
        } catch (Exception e) {
            Log.e(TAG, "Unable to extract thumbnail from " + path, e);
        } finally {
            if (mmr != null) {
                mmr.release();
            }
        }

        return new Size(width, height);
    }

    public static long getDuration(Context context, Uri uri) {
        MediaMetadataRetriever mmr = null;
        try {
            mmr = new MediaMetadataRetriever();
            if (TextUtils.equals(uri.getScheme(), ContentResolver.SCHEME_FILE)) {
                mmr.setDataSource(uri.getPath());
            } else {
                mmr.setDataSource(context, uri);
            }

            String duration = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            if (duration != null) {
                return Long.parseLong(duration);
            }
        } catch (Exception e) {
            Log.e(TAG, "Unable to extract duration from " + uri, e);
        } finally {
            if (mmr != null) {
                mmr.release();
            }
        }

        return 0;
    }

    @Nullable
    public static Bitmap getFrameAtTime(String path, long micros) {
        MediaMetadataRetriever mmr = null;
        try {
            mmr = new MediaMetadataRetriever();
            mmr.setDataSource(path);
            String duration = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            if (duration != null) {
                long millis = Long.parseLong(duration);
                if (micros > TimeUnit.MILLISECONDS.toMicros(millis)) {
                    return mmr.getFrameAtTime(TimeUnit.MILLISECONDS.toMicros(millis));
                }

                return mmr.getFrameAtTime(micros);
            }
        } catch (Exception e) {
            Log.e(TAG, "Unable to extract thumbnail from " + path, e);
        } finally {
            if (mmr != null) {
                try {
                    mmr.close();
                } catch (NoSuchMethodError ignore) {
                }

                mmr.release();
            }
        }

        return null;
    }
}
