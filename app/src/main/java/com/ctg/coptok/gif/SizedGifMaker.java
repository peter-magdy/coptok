package com.ctg.coptok.gif;

import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.util.Size;

import com.beak.gifmakerlib.AnimatedGifEncoder;
import com.beak.gifmakerlib.GifMaker;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class SizedGifMaker extends GifMaker {

    private final Size mSize;

    public SizedGifMaker(Size size) {
        super(1);
        mSize = size;
    }

    @Override
    public boolean makeGif(List<Bitmap> source, String outputPath) throws IOException {
        AnimatedGifEncoder encoder = new AnimatedGifEncoder();
        encoder.setQuality(20);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        encoder.start(bos);
        encoder.setRepeat(0);
        final int length = source.size();
        for (int i = 0; i < length; i++) {
            Bitmap bmp = source.get(i);
            if (bmp == null) {
                continue;
            }

            Bitmap thumb = ThumbnailUtils.extractThumbnail(
                    bmp,
                    mSize.getWidth(),
                    mSize.getHeight(),
                    ThumbnailUtils.OPTIONS_RECYCLE_INPUT);
            try {
                encoder.addFrame(thumb);
            } catch (Exception e) {
                e.printStackTrace();
                System.gc();
                break;
            }
        }
        encoder.finish();
        source.clear();
        byte[] data = bos.toByteArray();
        File file = new File(outputPath);
        if (!file.getParentFile().exists()) {
            //noinspection ResultOfMethodCallIgnored
            file.getParentFile().mkdirs();
        }

        FileOutputStream fos = new FileOutputStream(file);
        fos.write(data);
        fos.flush();
        fos.close();
        return file.exists();
    }
}
