package com.decosegfault.hermes.frontend;

import com.google.gson.Gson;

import javax.websocket.EncodeException;
import javax.websocket.Encoder;
import javax.websocket.EndpointConfig;

public class FrontendDataEncoder implements Encoder.Text<FrontendData> {
    private Gson gson = new Gson();

    @Override
    public String encode(FrontendData object) throws EncodeException {
        return gson.toJson(object);
    }

    @Override
    public void init(EndpointConfig config) {

    }

    @Override
    public void destroy() {

    }
}
