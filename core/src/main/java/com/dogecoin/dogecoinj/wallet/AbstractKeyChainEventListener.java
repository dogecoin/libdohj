package com.dogecoin.dogecoinj.wallet;

import com.dogecoin.dogecoinj.core.ECKey;

import java.util.List;

public class AbstractKeyChainEventListener implements KeyChainEventListener {
    @Override
    public void onKeysAdded(List<ECKey> keys) {
    }
}
