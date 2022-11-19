package com.ctg.coptok.utils;

import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

final public class FileUtil {

    private static final String TAG = "FileUtil";

    public static boolean copyFile(File from, File to) {
        try (InputStream is = new FileInputStream(from);
             OutputStream os = new FileOutputStream(to)) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }

            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to copy " + from + " to " + to, e);
        }

        return false;
    }
}
