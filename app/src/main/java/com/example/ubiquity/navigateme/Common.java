package com.example.ubiquity.navigateme;

import com.example.ubiquity.navigateme.Remote.IGoogleApi;
import com.example.ubiquity.navigateme.Remote.RetrofitClient;

/**
 * Created by Ubiquity on 1/26/2018.
 */

public class Common {
    public static final String baseURL="https://www.googleapis.com";
    public static IGoogleApi getIGoogleApi(){
        return RetrofitClient.getClient(baseURL).create(IGoogleApi.class);
    }
}
