package com.ctg.coptok.workers;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.arthenica.mobileffmpeg.FFmpeg;

import java.io.File;

public class VideoTrimmerWorker4 extends Worker {

    public static final String KEY_INPUT = "input";
    public static final String KEY_OUTPUT = "output";
    public static final String KEY_START = "start";
    public static final String KEY_END = "end";
    private static final String TAG = "VideoTrimmerWorker4";

    public VideoTrimmerWorker4(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        File input = new File(getInputData().getString(KEY_INPUT));
        File output = new File(getInputData().getString(KEY_OUTPUT));
        long start = getInputData().getLong(KEY_START, 0);
        long end = getInputData().getLong(KEY_END, 0);
        boolean success = false;
        try {
            success = doActualWork(input, output, start, end);
        } catch (Exception e) {
            Log.e(TAG, "Encountered error when trimming " + input, e);
        }

        if (!success && !output.delete()) {
            Log.w(TAG, "Could not delete failed output file: " + output);
        }

        return success ? Result.success() : Result.failure();
    }

    public static boolean doActualWork(File input, File output, long start, long end) {
        int code = FFmpeg.execute(new String[] {
                "-i", input.getAbsolutePath(),
                "-ss", start + "ms", "-t", (end - start) + "ms",
                "-c:v", "libx264", "-c:a", "aac", "-preset:v", "fast",
                output.getAbsolutePath(),
        });
        return code == 0;
    }
}
