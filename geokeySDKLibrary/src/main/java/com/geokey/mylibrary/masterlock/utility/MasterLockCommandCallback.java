package com.geokey.mylibrary.masterlock.utility;

public interface MasterLockCommandCallback<T> {
    void result(T result, Exception error);
}