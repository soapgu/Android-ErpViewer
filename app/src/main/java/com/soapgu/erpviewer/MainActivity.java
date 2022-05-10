package com.soapgu.erpviewer;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.orhanobut.logger.Logger;
import com.soapgu.erpviewer.restful.Api;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {
    private Api api;
    private TextView message;
    private final CompositeDisposable disposables = new CompositeDisposable();
    private final ConcurrentHashMap<String, List<Cookie>> cookieStore = new ConcurrentHashMap<>();

    private Retrofit provideRetrofit() {
        OkHttpClient client = new OkHttpClient().newBuilder()
                .cookieJar(new CookieJar() {
                    @Override
                    public void saveFromResponse(@NonNull HttpUrl url, @NonNull List<Cookie> cookies) {
                        cookies.forEach( t-> Logger.i("save cookie:%s",t.toString()) );
                        cookieStore.put(url.host(), cookies);
                    }

                    @NonNull
                    @Override
                    public List<Cookie> loadForRequest(@NonNull HttpUrl url) {
                        List<Cookie> cookies = cookieStore.get(url.host());
                        return cookies != null ? cookies : new ArrayList<>();
                    }
                }).build();
        String baseUrl = "https://erp.shgbit.com/";
        return new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
                .build();
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.message = findViewById(R.id.message_tv);
        Button loginButton = findViewById(R.id.login_button);
        api = provideRetrofit().create(Api.class);
        loginButton.setOnClickListener(v -> onLoginClick() );
    }

    private void onLoginClick(){
        disposables.add( this.api.getItCode()
                .map( t->{
                    Document doc = Jsoup.parse(t.string());
                    Elements elements = doc.getElementsByAttributeValue("name","lt");
                    if( !elements.isEmpty() ) {
                       return  Objects.requireNonNull(elements.first()).attr("value");
                    }
                    return "";
                } )
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe( t->{
                            Logger.i( "it code:%s",t );
                            this.message.setText(String.format("response ok:%s", t));
                        },
                        e ->{
                            Logger.e( e, "error with get itCode" );
                            this.message.setText( String.format("response error:%s",e.getMessage()));
                        }) );
    }
}