package com.decosegfault.hermes.frontend;

import com.google.gson.Gson;

import javax.websocket.DecodeException;
import javax.websocket.Decoder;
import javax.websocket.EndpointConfig;

public class FrontendDataDecoder implements Decoder.Text<FrontendData> {
    private Gson gson = new Gson();

    @Override
    public FrontendData decode(String s) throws DecodeException {
        return gson.fromJson(s, FrontendData.class);
    }

    @Override
    public boolean willDecode(String s) {
        return true;
    }

    @Override
    public void init(EndpointConfig config) {

    }

    @Override
    public void destroy() {

    }
}
