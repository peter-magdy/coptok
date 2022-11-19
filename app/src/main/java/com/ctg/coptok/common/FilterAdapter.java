package com.ctg.coptok.common;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import jp.co.cyberagent.android.gpuimage.GPUImage;
import jp.co.cyberagent.android.gpuimage.filter.GPUImageBrightnessFilter;
import jp.co.cyberagent.android.gpuimage.filter.GPUImageColorInvertFilter;
import jp.co.cyberagent.android.gpuimage.filter.GPUImageExposureFilter;
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter;
import jp.co.cyberagent.android.gpuimage.filter.GPUImageGammaFilter;
import jp.co.cyberagent.android.gpuimage.filter.GPUImageGrayscaleFilter;
import jp.co.cyberagent.android.gpuimage.filter.GPUImageHazeFilter;
import jp.co.cyberagent.android.gpuimage.filter.GPUImageMonochromeFilter;
import jp.co.cyberagent.android.gpuimage.filter.GPUImagePixelationFilter;
import jp.co.cyberagent.android.gpuimage.filter.GPUImagePosterizeFilter;
import jp.co.cyberagent.android.gpuimage.filter.GPUImageSepiaToneFilter;
import jp.co.cyberagent.android.gpuimage.filter.GPUImageSharpenFilter;
import jp.co.cyberagent.android.gpuimage.filter.GPUImageSolarizeFilter;
import jp.co.cyberagent.android.gpuimage.filter.GPUImageVignetteFilter;
import com.ctg.coptok.R;

public class FilterAdapter extends RecyclerView.Adapter<FilterAdapter.FilterViewHolder> {

    private final Context mContext;
    private final List<VideoFilter> mFilters = Arrays.asList(VideoFilter.values());
    private final GPUImage mGpuImage;
    private OnFilterSelectListener mListener;

    public FilterAdapter(Context context, Bitmap thumbnail) {
        mContext = context;
        mGpuImage = new GPUImage(context);
        mGpuImage.setImage(thumbnail);
    }

    @Override
    public int getItemCount() {
        return mFilters.size();
    }

    @NonNull
    @Override
    public FilterViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext)
                .inflate(R.layout.item_filter, parent, false);
        FilterViewHolder holder = new FilterViewHolder(view);
        holder.setIsRecyclable(false);
        return holder;
    }

    @Override
    @SuppressLint("SetTextI18n")
    public void onBindViewHolder(@NonNull FilterViewHolder holder, int position) {
        final VideoFilter filter = mFilters.get(position);
        switch (filter) {
            case BRIGHTNESS: {
                GPUImageBrightnessFilter glf = new GPUImageBrightnessFilter();
                glf.setBrightness(0.2f);
                mGpuImage.setFilter(glf);
                break;
            }
            case EXPOSURE:
                mGpuImage.setFilter(new GPUImageExposureFilter());
                break;
            case GAMMA: {
                GPUImageGammaFilter glf = new GPUImageGammaFilter();
                glf.setGamma(2f);
                mGpuImage.setFilter(glf);
                break;
            }
            case GRAYSCALE:
                mGpuImage.setFilter(new GPUImageGrayscaleFilter());
                break;
            case HAZE: {
                GPUImageHazeFilter glf = new GPUImageHazeFilter();
                glf.setSlope(-0.5f);
                mGpuImage.setFilter(glf);
                break;
            }
            case INVERT:
                mGpuImage.setFilter(new GPUImageColorInvertFilter());
                break;
            case MONOCHROME:
                mGpuImage.setFilter(new GPUImageMonochromeFilter());
                break;
            case PIXELATED: {
                GPUImagePixelationFilter glf = new GPUImagePixelationFilter();
                glf.setPixel(5);
                mGpuImage.setFilter(glf);
                break;
            }
            case POSTERIZE:
                mGpuImage.setFilter(new GPUImagePosterizeFilter());
                break;
            case SEPIA:
                mGpuImage.setFilter(new GPUImageSepiaToneFilter());
                break;
            case SHARP: {
                GPUImageSharpenFilter glf = new GPUImageSharpenFilter();
                glf.setSharpness(1f);
                mGpuImage.setFilter(glf);
                break;
            }
            case SOLARIZE:
                mGpuImage.setFilter(new GPUImageSolarizeFilter());
                break;
            case VIGNETTE:
                mGpuImage.setFilter(new GPUImageVignetteFilter());
                break;
            default:
                mGpuImage.setFilter(new GPUImageFilter());
                break;
        }

        holder.image.setImageBitmap(mGpuImage.getBitmapWithFilterApplied());
        String name = filter.name().toLowerCase(Locale.US);
        holder.name.setText(name.substring(0, 1).toUpperCase() + name.substring(1));
        holder.itemView.setOnClickListener(view -> {
            if (mListener != null) {
                mListener.onSelectFilter(filter);
            }
        });
    }

    public void setListener(OnFilterSelectListener listener) {
        mListener = listener;
    }

    static class FilterViewHolder extends RecyclerView.ViewHolder {

        public ImageView image;
        public TextView name;

        public FilterViewHolder(@NonNull View root) {
            super(root);
            image = root.findViewById(R.id.image);
            name = root.findViewById(R.id.name);
        }
    }

    public interface OnFilterSelectListener {

        void onSelectFilter(VideoFilter filter);
    }
}
