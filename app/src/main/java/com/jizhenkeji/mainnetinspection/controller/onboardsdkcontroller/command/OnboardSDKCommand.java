package com.jizhenkeji.mainnetinspection.controller.onboardsdkcontroller.command;

import com.google.gson.Gson;

import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;

public class OnboardSDKCommand {

    @NotNull
    public String toString(){
        Gson gson = new Gson();
        return gson.toJson(this);
    }

    public byte[] toBytes(){
        return toString().getBytes(StandardCharsets.UTF_8);
    }

}
