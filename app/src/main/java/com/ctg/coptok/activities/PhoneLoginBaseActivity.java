package com.ctg.coptok.activities;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.textfield.TextInputLayout;
import com.hbb20.CountryCodePicker;

import java.util.Map;

import com.ctg.coptok.R;
import com.ctg.coptok.utils.LocaleUtil;

public class PhoneLoginBaseActivity extends AppCompatActivity {

    public static final String EXTRA_REFERRER = "referrer";
    public static final String EXTRA_TOKEN = "token";

    protected PhoneLoginActivityViewModel mModel;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleUtil.wrap(base));
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone_login);
        ImageButton close = findViewById(R.id.header_back);
        close.setImageResource(R.drawable.ic_baseline_close_24);
        close.setOnClickListener(view -> finish());
        TextView title = findViewById(R.id.header_title);
        title.setText(R.string.login_label);
        findViewById(R.id.header_more).setVisibility(View.GONE);
        int dcc = getResources().getInteger(R.integer.default_calling_code);
        mModel = new ViewModelProvider(this, new PhoneLoginActivityViewModel.Factory(dcc))
                .get(PhoneLoginActivityViewModel.class);
        mModel.referrer = getIntent().getStringExtra(EXTRA_REFERRER);
        CountryCodePicker cc = findViewById(R.id.cc);
        cc.setCountryForPhoneCode(mModel.cc);
        cc.setOnCountryChangeListener(() -> mModel.cc = cc.getSelectedCountryCodeAsInt());
        TextInputLayout phone = findViewById(R.id.phone);
        cc.registerCarrierNumberEditText(phone.getEditText());
        phone.getEditText().setText(mModel.phone);
        phone.getEditText().addTextChangedListener(new TextWatcher() {

            @Override
            public void afterTextChanged(Editable editable) {
                mModel.phone = editable.toString();
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }
        });
        TextInputLayout otp = findViewById(R.id.otp);
        otp.getEditText().setText(mModel.otp);
        otp.getEditText().addTextChangedListener(new TextWatcher() {

            @Override
            public void afterTextChanged(Editable editable) {
                mModel.otp = editable.toString();
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }
        });
        TextInputLayout name = findViewById(R.id.name);
        name.getEditText().setText(mModel.name);
        name.getEditText().addTextChangedListener(new TextWatcher() {

            @Override
            public void afterTextChanged(Editable editable) {
                mModel.name = editable.toString();
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }
        });
        View verify = findViewById(R.id.verify);
        mModel.doesExist.observe(this, exists -> {
            Boolean sent = mModel.isSent.getValue();
            name.setVisibility(sent && !exists ? View.VISIBLE : View.GONE);
        });
        mModel.isSent.observe(this, sent -> {
            Boolean exists = mModel.doesExist.getValue();
            name.setVisibility(sent && !exists ? View.VISIBLE : View.GONE);
            otp.setVisibility(sent ? View.VISIBLE : View.GONE);
            if (sent) {
                otp.requestFocus();
            }

            verify.setEnabled(sent);
        });
        mModel.errors.observe(this, errors -> {
            phone.setError(null);
            otp.setError(null);
            name.setError(null);
            if (errors == null) {
                return;
            }
            if (errors.containsKey("phone")) {
                phone.setError(errors.get("phone"));
            }
            if (errors.containsKey("otp")) {
                otp.setError(errors.get("otp"));
            }
            if (errors.containsKey("name")) {
                name.setError(errors.get("name"));
            }
        });
        phone.getEditText().requestFocus();
    }

    public static class PhoneLoginActivityViewModel extends ViewModel {

        public int cc;
        public String phone = "";
        public String otp = "";
        public String name = "";
        public String referrer;

        public MutableLiveData<Boolean> doesExist = new MutableLiveData<>(false);
        public MutableLiveData<Boolean> isSent = new MutableLiveData<>(false);

        public MutableLiveData<Map<String, String>> errors = new MutableLiveData<>();

        public PhoneLoginActivityViewModel(int cc) {
            this.cc = cc;
        }

        private static class Factory implements ViewModelProvider.Factory {

            private final int mCallingCode;

            public Factory(int cc) {
                mCallingCode = cc;
            }

            @NonNull
            @Override
            public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
                //noinspection unchecked
                return (T)new PhoneLoginActivityViewModel(mCallingCode);
            }
        }
    }
}
