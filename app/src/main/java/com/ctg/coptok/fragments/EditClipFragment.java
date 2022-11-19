package com.ctg.coptok.fragments;

import android.content.res.Resources;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.os.ConfigurationCompat;
import androidx.core.os.LocaleListCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.TypeFilter;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputLayout;
import com.jakewharton.rxbinding4.widget.RxTextView;
import com.kaopiz.kprogresshud.KProgressHUD;
import com.pixplicity.easyprefs.library.Prefs;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.rxjava3.disposables.Disposable;
import com.ctg.coptok.MainApplication;
import com.ctg.coptok.R;
import com.ctg.coptok.SharedConstants;
import com.ctg.coptok.activities.MainActivity;
import com.ctg.coptok.common.LoadingState;
import com.ctg.coptok.data.api.REST;
import com.ctg.coptok.data.models.Clip;
import com.ctg.coptok.data.models.Wrappers;
import com.ctg.coptok.utils.AutocompleteUtil;
import com.ctg.coptok.utils.SocialSpanUtil;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EditClipFragment extends Fragment {

    private static final String ARG_CLIP = "clip";
    private static final String TAG = "EditClipFragment";

    private int mClip;
    private final List<Disposable> mDisposables = new ArrayList<>();
    private EditClipFragmentViewModel mModel;

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == SharedConstants.REQUEST_CODE_PICK_LOCATION && resultCode == Activity.RESULT_OK) {
            Place place = Autocomplete.getPlaceFromIntent(data);
            setLocation(place);
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mClip = requireArguments().getInt(ARG_CLIP);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_edit_clip, container, false);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        for (Disposable disposable : mDisposables) {
            disposable.dispose();
        }

        mDisposables.clear();
    }

    @Override
    public void onResume() {
        super.onResume();
        LoadingState state = mModel.state.getValue();
        Clip clip = mModel.clip.getValue();
        if (clip == null && state != LoadingState.LOADING) {
            loadClip();
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mModel = new ViewModelProvider(this).get(EditClipFragmentViewModel.class);
        ImageButton back = view.findViewById(R.id.header_back);
        back.setOnClickListener(v -> ((MainActivity)requireActivity()).popBackStack());
        TextView title = view.findViewById(R.id.header_title);
        title.setText(R.string.edit_label);
        ImageButton done = view.findViewById(R.id.header_more);
        done.setImageResource(R.drawable.ic_baseline_check_24);
        done.setOnClickListener(v -> updateWithServer());
        TextInputLayout location = view.findViewById(R.id.location);
        location.getEditText().setText(mModel.location);
        Disposable disposable;
        //noinspection ConstantConditions
        disposable = RxTextView.afterTextChangeEvents(location.getEditText())
                .skipInitialValue()
                .subscribe(e -> {
                    if (TextUtils.isEmpty(e.getEditable())) {
                        mModel.location = null;
                        mModel.latitude = null;
                        mModel.longitude = null;
                    }
                });
        mDisposables.add(disposable);
        if (!getResources().getBoolean(R.bool.locations_enabled)) {
            location.setVisibility(View.GONE);
        }

        location.setEndIconOnClickListener(v -> pickLocation());
        TextInputLayout description = view.findViewById(R.id.description);
        description.getEditText().setText(mModel.description);
        disposable = RxTextView.afterTextChangeEvents(description.getEditText())
                .skipInitialValue()
                .subscribe(e -> {
                    Editable editable = e.getEditable();
                    mModel.description = editable != null ? editable.toString() : null;
                });
        mDisposables.add(disposable);
        ArrayAdapter<CharSequence> adapter1 = ArrayAdapter.createFromResource(
                requireContext(), R.array.language_names, android.R.layout.simple_spinner_item);
        adapter1.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        Spinner language = view.findViewById(R.id.language);
        language.setAdapter(adapter1);
        List<String> codes1 = Arrays.asList(
                getResources().getStringArray(R.array.language_codes)
        );
        language.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mModel.language = codes1.get(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });
        if (TextUtils.isEmpty(mModel.language)) {
            LocaleListCompat locales =
                    ConfigurationCompat.getLocales(Resources.getSystem().getConfiguration());
            String locale = locales.get(0).getISO3Language();
            if (codes1.contains(locale)) {
                mModel.language = locale;
            } else {
                mModel.language = codes1.get(0);
            }
        }

        language.setSelection(codes1.indexOf(mModel.language));
        SwitchMaterial isPrivate = view.findViewById(R.id.private2);
        isPrivate.setChecked(mModel.isPrivate);
        isPrivate.setOnCheckedChangeListener((button, checked) -> mModel.isPrivate = checked);
        SwitchMaterial hasComments = view.findViewById(R.id.comments);
        hasComments.setChecked(mModel.hasComments);
        hasComments.setOnCheckedChangeListener((button, checked) -> mModel.hasComments = checked);
        SwitchMaterial allowDuet = view.findViewById(R.id.duet);
        allowDuet.setChecked(mModel.allowDuet);
        allowDuet.setOnCheckedChangeListener((button, checked) -> mModel.allowDuet = checked);
        allowDuet.setVisibility(getResources().getBoolean(R.bool.duet_enabled) ? View.VISIBLE : View.GONE);
        ArrayAdapter<CharSequence> adapter2 = ArrayAdapter.createFromResource(
                requireContext(), R.array.cta_label_names, android.R.layout.simple_spinner_item);
        adapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        Spinner ctaLabel = view.findViewById(R.id.cta_label);
        ctaLabel.setAdapter(adapter2);
        List<String> codes2 = Arrays.asList(getResources().getStringArray(R.array.cta_label_codes));
        ctaLabel.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mModel.ctaLabel.postValue(codes2.get(position));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });
        ctaLabel.setSelection(codes2.indexOf(mModel.ctaLabel.getValue()));
        TextInputLayout ctaLink = view.findViewById(R.id.cta_link);
        ctaLink.getEditText().setText(mModel.ctaLink);
        disposable = RxTextView.afterTextChangeEvents(ctaLink.getEditText())
                .skipInitialValue()
                .subscribe(e -> {
                    Editable editable = e.getEditable();
                    mModel.ctaLink = editable != null ? editable.toString() : null;
                });
        mDisposables.add(disposable);
        mModel.ctaLabel.observe(getViewLifecycleOwner(), value ->
                ctaLink.setVisibility(TextUtils.isEmpty(value) ? View.GONE : View.VISIBLE));
        boolean business = Prefs.getBoolean(SharedConstants.PREF_BUSINESS_USER, false);
        view.findViewById(R.id.cta).setVisibility(business ? View.VISIBLE : View.GONE);
        mModel.errors.observe(getViewLifecycleOwner(), errors -> {
            description.setError(null);
            isPrivate.setError(null);
            hasComments.setError(null);
            allowDuet.setError(null);
            ctaLink.setError(null);
            if (errors == null) {
                return;
            }

            if (errors.containsKey("location")) {
                location.setError(errors.get("location"));
            }

            if (errors.containsKey("description")) {
                description.setError(errors.get("description"));
            }

            if (errors.containsKey("private")) {
                isPrivate.setError(errors.get("private"));
            }

            if (errors.containsKey("comments")) {
                hasComments.setError(errors.get("comments"));
            }

            if (errors.containsKey("duet")) {
                allowDuet.setError(errors.get("duet"));
            }

            if (errors.containsKey("cta_link")) {
                ctaLink.setError(errors.get("cta_link"));
            }
        });
        View content = view.findViewById(R.id.content);
        View loading = view.findViewById(R.id.loading);
        mModel.state.observe(getViewLifecycleOwner(), state -> {
            loading.setVisibility(state == LoadingState.LOADING ? View.VISIBLE : View.GONE);
            content.setVisibility(state == LoadingState.LOADED ? View.VISIBLE : View.GONE);
        });
        mModel.clip.observe(getViewLifecycleOwner(), clip -> {
            location.getEditText().setText(mModel.location = clip.location);
            mModel.latitude = clip.latitude;
            mModel.longitude = clip.longitude;
            description.getEditText().setText(mModel.description = clip.description);
            language.setSelection(codes1.indexOf(mModel.language = clip.language));
            isPrivate.setChecked(mModel.isPrivate = clip._private);
            hasComments.setChecked(mModel.hasComments = clip.comments);
            allowDuet.setChecked(mModel.allowDuet = clip.duet);
            ctaLabel.setSelection(codes2.indexOf(clip.ctaLabel));
            mModel.ctaLabel.setValue(clip.ctaLabel);
            ctaLink.getEditText().setText(mModel.ctaLink = clip.ctaLink);
        });
        EditText input = description.getEditText();
        SocialSpanUtil.apply(input, mModel.description, null);
        if (getResources().getBoolean(R.bool.autocomplete_enabled)) {
            AutocompleteUtil.setupForHashtags(requireContext(), input);
            AutocompleteUtil.setupForUsers(requireContext(), input);
        }
    }

    private void loadClip() {
        mModel.state.setValue(LoadingState.LOADING);
        REST rest = MainApplication.getContainer().get(REST.class);
        rest.clipsShow(mClip)
                .enqueue(new Callback<Wrappers.Single<Clip>>() {

                    @Override
                    public void onResponse(
                            @Nullable Call<Wrappers.Single<Clip>> call,
                            @Nullable Response<Wrappers.Single<Clip>> response
                    ) {
                        int code = response != null ? response.code() : -1;
                        Log.v(TAG, "Fetching clip returned " + code + '.');
                        if (response != null && response.isSuccessful()) {
                            //noinspection ConstantConditions
                            Clip clip = response.body().data;
                            mModel.clip.setValue(clip);
                            mModel.state.setValue(LoadingState.LOADED);
                        } else {
                            mModel.state.setValue(LoadingState.ERROR);
                        }
                    }

                    @Override
                    public void onFailure(
                            @Nullable Call<Wrappers.Single<Clip>> call,
                            @Nullable Throwable t
                    ) {
                        Log.e(TAG, "Failed when trying to fetch clip.", t);
                        mModel.state.setValue(LoadingState.ERROR);
                    }
                });
    }

    public static EditClipFragment newInstance(Clip clip) {
        Bundle arguments = new Bundle();
        arguments.putParcelable(ARG_CLIP, clip);
        EditClipFragment fragment = new EditClipFragment();
        fragment.setArguments(arguments);
        return fragment;
    }

    private void pickLocation() {
        List<Place.Field> fields =
                Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG);
        Intent intent = new Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields)
                .setTypeFilter(TypeFilter.ESTABLISHMENT)
                .build(requireContext());
        startActivityForResult(intent, SharedConstants.REQUEST_CODE_PICK_LOCATION);
    }

    private void setLocation(Place place) {
        Log.v(TAG, "User chose " + place.getId() + " place.");
        mModel.location = place.getName();
        mModel.latitude = place.getLatLng().latitude;
        mModel.longitude = place.getLatLng().longitude;
        TextInputLayout location = getView().findViewById(R.id.location);
        location.getEditText().setText(mModel.location);
    }

    private void showErrors(JSONObject json) throws Exception {
        JSONObject errors = json.getJSONObject("errors");
        Map<String, String> messages = new HashMap<>();
        String[] keys = new String[]{
                "description", "language", "private", "comments", "duet", "cta_label", "cta_link",
                "location", "latitude", "longitude"};
        for (String key : keys) {
            JSONArray fields = errors.optJSONArray(key);
            if (fields != null) {
                messages.put(key, fields.getString(0));
            }
        }

        mModel.errors.postValue(messages);
    }

    private void updateWithServer() {
        KProgressHUD progress = KProgressHUD.create(requireActivity())
                .setStyle(KProgressHUD.Style.SPIN_INDETERMINATE)
                .setLabel(getString(R.string.progress_title))
                .setCancellable(false)
                .show();
        mModel.errors.postValue(null);
        REST rest = MainApplication.getContainer().get(REST.class);
        Log.v(TAG, "CTA label: " + mModel.ctaLabel.getValue());
        Call<Wrappers.Single<Clip>> call = rest.clipsUpdate(
                mClip,
                mModel.description,
                mModel.language,
                mModel.isPrivate ? 1 : 0,
                mModel.hasComments ? 1 : 0,
                mModel.allowDuet ? 1 : 0,
                mModel.ctaLabel.getValue(),
                mModel.ctaLink,
                mModel.location,
                mModel.latitude,
                mModel.longitude
        );
        call.enqueue(new Callback<Wrappers.Single<Clip>>() {

            @Override
            public void onResponse(
                    @Nullable Call<Wrappers.Single<Clip>> call,
                    @Nullable Response<Wrappers.Single<Clip>> response
            ) {
                progress.dismiss();
                if (response != null) {
                    if (response.isSuccessful()) {
                        Toast.makeText(requireContext(), R.string.message_clip_updated, Toast.LENGTH_SHORT).show();
                        ((MainActivity)requireActivity()).popBackStack();
                    } else if (response.code() == 422) {
                        try {
                            String content = response.errorBody().string();
                            showErrors(new JSONObject(content));
                        } catch (Exception ignore) {
                        }
                    }
                }
            }

            @Override
            public void onFailure(
                    @Nullable Call<Wrappers.Single<Clip>> call,
                    @Nullable Throwable t
            ) {
                Log.e(TAG, "Failed when trying to update clip.", t);
                progress.dismiss();
            }
        });
    }

    public static class EditClipFragmentViewModel extends ViewModel {

        public String description;
        public String language;
        public boolean isPrivate;
        public boolean hasComments;
        public boolean allowDuet;
        public MutableLiveData<String> ctaLabel = new MutableLiveData<>();
        public String ctaLink;

        public String location;
        public Double latitude;
        public Double longitude;

        public final MutableLiveData<Map<String, String>> errors = new MutableLiveData<>();
        public final MutableLiveData<LoadingState> state = new MutableLiveData<>();
        public final MutableLiveData<Clip> clip = new MutableLiveData<>();
    }
}
