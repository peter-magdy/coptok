package com.ctg.coptok.workers;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.arthenica.mobileffmpeg.FFmpeg;

import java.io.File;
import java.util.Locale;

public class MergeAudiosWorker extends Worker {

    public static final String KEY_INPUT_1 = "input_1";
    public static final String KEY_INPUT_1_VOLUME = "input_1_volume";
    public static final String KEY_INPUT_2 = "input_2";
    public static final String KEY_INPUT_2_VOLUME = "input_2_volume";
    public static final String KEY_OUTPUT = "output";
    private static final String TAG = "MergeAudiosWorker";

    public MergeAudiosWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    @SuppressWarnings("ConstantConditions")
    public Result doWork() {
        String input1 = getInputData().getString(KEY_INPUT_1);
        float volume1 = getInputData().getFloat(KEY_INPUT_1_VOLUME, 0f);
        String input2 = getInputData().getString(KEY_INPUT_2);
        float volume2 = getInputData().getFloat(KEY_INPUT_2_VOLUME, 0f);
        String output = getInputData().getString(KEY_OUTPUT);
        boolean success = doActualWork(input1, volume1, input2, volume2, output);
        File outputF = new File(output);
        if (!success && !outputF.delete()) {
            Log.w(TAG, "Could not delete failed output file: " + output);
        }

        return success ? Result.success() : Result.failure();
    }

    private boolean doActualWork(String input1, float volume1, @Nullable String input2, float volume2, String output) {
        int code;
        if (!TextUtils.isEmpty(input2)) {
            String filter = String.format(
                    Locale.US,
                    "[0:a]volume=%.2f[a0];[1:a]volume=%.2f[a1];[a0][a1]amix=inputs=2[out]",
                    volume1,
                    volume2);
            code = FFmpeg.execute(new String[]{
                    "-i", input1, "-i", input2,
                    "-filter_complex", filter, "-map", "[out]", "-vn",
                    output,
            });
        } else {
            String filter = String.format(
                    Locale.US,
                    "[0:a]volume=%.2f[a0];[a0]amix=inputs=1[out]",
                    volume1);
            code = FFmpeg.execute(new String[]{
                    "-i", input1,
                    "-filter_complex", filter, "-map", "[out]",
                    output,
            });
        }

        return code == 0;
    }
}
