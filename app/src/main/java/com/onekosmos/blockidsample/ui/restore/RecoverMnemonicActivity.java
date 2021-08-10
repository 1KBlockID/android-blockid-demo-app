package com.onekosmos.blockidsample.ui.restore;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;

import com.onekosmos.blockid.sdk.BlockIDSDK;
import com.onekosmos.blockidsample.R;

import java.util.List;

public class RecoverMnemonicActivity extends AppCompatActivity {

    private AppCompatImageView mImgBack;
    private AppCompatTextView mTxtBack;
    private AppCompatButton mBtnCopyPhrase;
    private List<String> mnemonicPhrases;
    private AppCompatTextView[] mTvPhrases = new AppCompatTextView[12];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recover_mnemonic_account);
        mnemonicPhrases = BlockIDSDK.getInstance().getMnemonic();
        initView();
    }

    private void initView() {
        mImgBack = findViewById(R.id.img_back);
        mImgBack.setOnClickListener(view -> onBackPressed());

        mTxtBack = findViewById(R.id.txt_back);
        mTxtBack.setOnClickListener(view -> onBackPressed());

        mBtnCopyPhrase = findViewById(R.id.btn_copy_phrases);
        mBtnCopyPhrase.setOnClickListener(view -> {
            String mPhrases = mnemonicPhrases.toString();
            mPhrases = mPhrases.replace(",", "");
            boolean isCopied = copyToClipboard(RecoverMnemonicActivity.this,
                    mPhrases.substring(1, mPhrases.length() - 1));
            if (isCopied) {
                Toast.makeText(RecoverMnemonicActivity.this,
                        getResources().getString(R.string.label_mnemonic_copy_success),
                        Toast.LENGTH_LONG).show();
            }
        });

        for (int index = 0; index < mTvPhrases.length; index++) {
            String number;
            String layoutName;
            if (index < 9) {
                number = "0" + (index + 1);
                layoutName = "phrase_" + number;
            } else {
                number = "" + (index + 1);
                layoutName = "phrase_" + number;
            }
            int resID = getResources().getIdentifier(layoutName, "id", getPackageName());
            mTvPhrases[index] = findViewById(resID).findViewById(R.id.text_number);
            mTvPhrases[index].setText(number + ". ");

            mTvPhrases[index] = findViewById(resID).findViewById(R.id.text_phrase);
            mTvPhrases[index].setText(mnemonicPhrases.get(index));
        }
    }

    private boolean copyToClipboard(Context context, String text) {
        ClipboardManager clipboard = (android.content.ClipboardManager) context
                .getSystemService(context.CLIPBOARD_SERVICE);
        ClipData clip = android.content.ClipData
                .newPlainText("message", text);
        clipboard.setPrimaryClip(clip);
        return true;
    }
}