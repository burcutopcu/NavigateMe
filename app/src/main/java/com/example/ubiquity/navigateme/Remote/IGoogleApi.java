package com.example.ubiquity.navigateme.Remote;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Url;

/**
 * Created by Ubiquity on 1/26/2018.
 */

public interface IGoogleApi {
    @GET
    Call <String> getDataFromGoogleApi (@Url String url);
}
