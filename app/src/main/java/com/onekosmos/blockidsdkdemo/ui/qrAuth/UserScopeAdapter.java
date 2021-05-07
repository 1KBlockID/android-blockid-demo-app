package com.onekosmos.blockidsdkdemo.ui.qrAuth;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.blockidsdkdemo.R;

import java.util.LinkedHashMap;

/**
 * Created by Pankti Mistry on 07-05-2021.
 * Copyright © 2020 1Kosmos. All rights reserved.
 */
public class UserScopeAdapter extends RecyclerView.Adapter<UserScopeAdapter.UserScopeViewHolder> {

    private Context mContext;
    private LinkedHashMap<String, Object> mScopes = null;
    private String[] tiltleList;

    public UserScopeAdapter(Context context, LinkedHashMap<String, Object> scopes) {
        mContext = context;
        mScopes = scopes;
        tiltleList = mScopes.keySet().toArray(new String[0]);
    }

    @NonNull
    @Override
    public UserScopeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View listItem = layoutInflater.inflate(R.layout.row_user_scope, parent, false);
        UserScopeViewHolder viewHolder = new UserScopeViewHolder(listItem);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull UserScopeViewHolder holder, int position) {
        holder.mTxtScopeTitle.setText(tiltleList[position]);
        holder.mTxtScopeDetails.setText(mScopes.get(tiltleList[position]).toString());
    }

    @Override
    public int getItemCount() {
        return mScopes != null ? mScopes.size() : 0;
    }

    class UserScopeViewHolder extends RecyclerView.ViewHolder {
        AppCompatTextView mTxtScopeTitle, mTxtScopeDetails;

        UserScopeViewHolder(@NonNull View itemView) {
            super(itemView);
            mTxtScopeTitle = itemView.findViewById(R.id.txt_scope_title);
            mTxtScopeDetails = itemView.findViewById(R.id.txt_scope_detail);
        }
    }
}

