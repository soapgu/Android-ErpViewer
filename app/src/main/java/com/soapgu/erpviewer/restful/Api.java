package com.soapgu.erpviewer.restful;

import io.reactivex.rxjava3.core.Single;
import okhttp3.ResponseBody;
import retrofit2.http.GET;


public interface Api {
    @GET("jqerp")
    Single<ResponseBody> getItCode();
}
