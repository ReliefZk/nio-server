package com.reliefzk.nio.app;

import java.util.Date;

import com.reliefzk.nio.common.Request;
import com.reliefzk.nio.common.event.EventAdapter;

public class LogHandler extends EventAdapter {
    public LogHandler() {
    }

    public void onClosed(Request request) throws Exception {
        String log = new Date().toString() + " from " + request.getAddress().toString();
        System.out.println(log);
    }

    public void onError(String error) {
        System.out.println("Error: " + error);
    }
}
