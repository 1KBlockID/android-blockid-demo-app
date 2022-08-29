package com.onekosmos.blockidsample.ui.walletconnect;


import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.walletconnect.sign.client.Sign;

import java.util.ArrayList;
import java.util.List;

public class DAppViewModel extends ViewModel {
    private final MutableLiveData<List<Sign.Model.Session>> liveData;
    private List<Sign.Model.Session> appList;

    public DAppViewModel() {
        liveData = new MutableLiveData<>();
        liveData.setValue(appList);
    }

    public MutableLiveData<List<Sign.Model.Session>> getUserMutableLiveData() {
        return liveData;
    }

    public void update(List<Sign.Model.Session> sessionList) {
        if (appList == null)
            appList = new ArrayList<>();
        appList.clear();
        appList.addAll(sessionList);
        liveData.postValue(appList);
    }
}
