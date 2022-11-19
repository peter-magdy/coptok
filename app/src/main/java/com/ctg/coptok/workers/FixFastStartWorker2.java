package com.ctg.coptok.workers;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import net.ypresto.qtfaststart.QtFastStart;

import java.io.File;

import com.ctg.coptok.utils.FileUtil;

public class FixFastStartWorker2 extends Worker {

    public static final String KEY_INPUT = "input";
    public static final String KEY_OUTPUT = "output";
    private static final String TAG = "FixFastStartWorker2";

    public FixFastStartWorker2(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    @SuppressWarnings("ConstantConditions")
    public Result doWork() {
        File input = new File(getInputData().getString(KEY_INPUT));
        File output = new File(getInputData().getString(KEY_OUTPUT));
        boolean success = doActualWork(input, output);
        if (!input.delete()) {
            Log.w(TAG, "Could not delete input file: " + input);
        }

        if (!success && !output.delete()) {
            Log.w(TAG, "Could not delete failed output file: " + input);
        }

        return success ? Result.success() : Result.failure();
    }

    private boolean doActualWork(File input, File output) {
        try {
            if (!output.exists() && !output.createNewFile()) {
                Log.w(TAG, "Failed to create new file at " + output);
            }

            boolean fixed = QtFastStart.fastStart(input, output);
            if (!fixed) {
                FileUtil.copyFile(input, output);
            }

            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to output at " + output, e);
        }

        return false;
    }
}
