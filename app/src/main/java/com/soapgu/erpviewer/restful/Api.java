package com.soapgu.erpviewer.restful;

import io.reactivex.rxjava3.core.Single;
import okhttp3.ResponseBody;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;


public interface Api {
    @GET("jqerp")
    Single<ResponseBody> getItCode();

    @FormUrlEncoded
    @POST("cas/login?service=https://erp.shgbit.com/jqerp/")
    Single<ResponseBody> login(@Field("lt") String lt,@Field("username") String username, @Field("password") String password, @Field("execution") String execution,@Field("_eventId") String _eventId);

    @FormUrlEncoded
    @POST("jqerp/web/taskInfoService")
    Single<ResponseBody> getTasks(@Field("actType") String actType);
}
