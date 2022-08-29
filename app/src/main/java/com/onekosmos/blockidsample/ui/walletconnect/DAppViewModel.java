package com.onekosmos.blockidsample.ui.walletconnect;


import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.walletconnect.sign.client.Sign;

import java.util.ArrayList;
import java.util.List;

public class DAppViewModel extends ViewModel {
    private final MutableLiveData<List<Sign.Model.Session>> userLiveData;
    private List<Sign.Model.Session> userArrayList;

    public DAppViewModel() {
        userLiveData = new MutableLiveData<>();
        userLiveData.setValue(userArrayList);
    }

    public MutableLiveData<List<Sign.Model.Session>> getUserMutableLiveData() {
        return userLiveData;
    }

    public void update(List<Sign.Model.Session> sessionList) {
        if (userArrayList == null)
            userArrayList = new ArrayList<>();
        userArrayList.clear();
        userArrayList.addAll(sessionList);
        userLiveData.postValue(userArrayList);
    }
}
