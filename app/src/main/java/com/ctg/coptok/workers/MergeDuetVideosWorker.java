package com.ctg.coptok.workers;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.arthenica.mobileffmpeg.FFmpeg;

import java.io.File;
import java.util.Locale;

import com.ctg.coptok.SharedConstants;
import com.ctg.coptok.utils.VideoUtil;

public class MergeDuetVideosWorker extends Worker {

    private static final String FFMPEG_FILTER = "" +
            "[0:v]scale=%1$d:%2$d:force_original_aspect_ratio=increase,crop=%1$d:%2$d[lv];" +
            "[1:v]scale=%1$d:%2$d:force_original_aspect_ratio=decrease,pad=%1$d:%2$d:-1:-1:color=black[rv];" +
            "[lv][rv]hstack=shortest=1[v];" +
            "[0:a]volume=%3$d[la];" +
            "[1:a]volume=%4$d[ra];" +
            "[la][ra]amix=inputs=2:duration=shortest[a]";

    private static final String FFMPEG_FILTER_PORTRAIT = "" +
            "[0:v]scale=%1$d:%2$d:force_original_aspect_ratio=increase,crop=%1$d:%2$d[lv];" +
            "[1:v]scale=%1$d:%2$d:force_original_aspect_ratio=increase,crop=%1$d:%2$d[rv];" +
            "[lv][rv]hstack=shortest=1[v];" +
            "[0:a]volume=%3$d[la];" +
            "[1:a]volume=%4$d[ra];" +
            "[la][ra]amix=inputs=2:duration=shortest[a]";

    public static final String KEY_AUDIO = "audio";
    public static final String KEY_ORIGINAL = "original";
    public static final String KEY_RECORDED = "recorded";
    public static final String KEY_OUTPUT = "output";

    private static final int STACK_WIDTH = (SharedConstants.MAX_VIDEO_RESOLUTION / 16) * 9;
    private static final int STACK_HEIGHT = SharedConstants.MAX_VIDEO_RESOLUTION;

    private static final String TAG = "MergeDuetVideosWorker";

    public MergeDuetVideosWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    @SuppressWarnings("ConstantConditions")
    public Result doWork() {
        String audio = getInputData().getString(KEY_AUDIO);
        File original = new File(getInputData().getString(KEY_ORIGINAL));
        File recorded = new File(getInputData().getString(KEY_RECORDED));
        File output = new File(getInputData().getString(KEY_OUTPUT));
        boolean success = doActualWork(original, recorded, output, audio);
        if (success && !recorded.delete()) {
            Log.v(TAG, "Could delete recorded file: " + recorded);
        }

        if (!success && !output.delete()) {
            Log.v(TAG, "Could delete failed output file: " + recorded);
        }

        return success ? Result.success() : Result.failure();
    }

    private boolean doActualWork(File original, File recorded, File output, String audio) {
        Size size = VideoUtil.getDimensions(original.getAbsolutePath());
        double ratio = (double) size.getWidth() / (double) size.getHeight();
        String filter = String.format(
                Locale.US,
                ratio >= .5 && ratio <= .6 /* portrait */ ? FFMPEG_FILTER_PORTRAIT : FFMPEG_FILTER,
                STACK_WIDTH,
                STACK_HEIGHT,
                TextUtils.equals("L", audio) || TextUtils.equals("LR", audio) ? 1 : 0,
                TextUtils.equals("R", audio) || TextUtils.equals("LR", audio) ? 1 : 0
        );
        int code = FFmpeg.execute(new String[]{
                "-i", recorded.getAbsolutePath(), "-i", original.getAbsolutePath(),
                "-filter_complex", filter, "-map", "[v]", "-map", "[a]", "-c:v", "libx264", "-c:a", "aac", "-preset:v", "veryfast", "-shortest",
                output.getAbsolutePath(),
        });
        return code == 0;
    }
}
