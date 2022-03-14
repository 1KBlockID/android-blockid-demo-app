package com.onekosmos.blockidsample.ui.fido;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;

import com.onekosmos.blockidsample.R;

/**
 * Created by 1Kosmos Engineering
 * Copyright © 2022 1Kosmos. All rights reserved.
 */
public class FidoBaseActivity extends AppCompatActivity {
    private AppCompatImageView mImgBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fido_base);

        mImgBack = findViewById(R.id.img_back);
        mImgBack.setOnClickListener(v -> onBackPressed());
        getSupportFragmentManager().beginTransaction().
                add(R.id.fragment_fido_container, new FidoDetailsFragment()).commit();
    }
}