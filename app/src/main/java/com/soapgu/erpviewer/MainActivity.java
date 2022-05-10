package com.soapgu.erpviewer;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.orhanobut.logger.Logger;
import com.soapgu.erpviewer.restful.Api;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {
    private static final String domain = "erp.shgbit.com";
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
                        Logger.i( "save cookie host:%s,count %s",url.host(),cookies.size() );
                        List<Cookie> saveCookies = cookieStore.get(url.host());
                        if( saveCookies == null ){
                            saveCookies = new ArrayList<>(cookies);
                        }
                        else{
                            saveCookies.addAll( cookies );
                        }
                        cookieStore.put(url.host(), saveCookies);
                    }

                    @NonNull
                    @Override
                    public List<Cookie> loadForRequest(@NonNull HttpUrl url) {
                        List<Cookie> cookies = cookieStore.get(url.host());

                        List<Cookie> retValue = cookies != null ? cookies : new ArrayList<>();
                        Logger.i( "load cookie count: %s",retValue.size() );
                        return retValue;
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
        cookieStore.clear();
        disposables.add( this.api.getItCode()
                .flatMapMaybe( t->{
                    Document doc = Jsoup.parse(t.string());
                    Elements elements = doc.getElementsByAttributeValue("name","lt");
                    if( !elements.isEmpty() ) {
                       return Maybe.just(Objects.requireNonNull(elements.first()).attr("value") );
                    }
                    return Maybe.empty();
                } )
                .flatMap( t -> this.api.login( t, "guhui", "******", "e1s1","submit" )
                        .flatMapMaybe( r->{
                            Pattern pattern = Pattern.compile("(?<=_userInfo=).*(?=;)");
                            Matcher matcher = pattern.matcher(r.string());
                            if( matcher.find() ){
                                JsonArray jsonArray = new JsonParser().parse(matcher.group()).getAsJsonArray();
                                addLoginCodeCookie( jsonArray.get(0).getAsJsonObject().get("logonCode").getAsString() );
                                return Maybe.just(true);
                            }
                            return Maybe.empty();
                        } ))
                .flatMapSingle( t -> this.api.getTasks( "loadTaskListJSON" )
                        .map(ResponseBody::string))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe( t->{
                            Logger.i( "login response:%s",t );
                            this.message.setText(String.format("response ok:%s", "ok"));
                        },
                        e ->{
                            Logger.e( e, "error with login" );
                            this.message.setText( String.format("response error:%s",e.getMessage()));
                        }) );
    }

    private void addLoginCodeCookie( String loginCode ){
        Objects.requireNonNull(cookieStore.get(domain))
                .add( new Cookie.Builder()
                    .name("_CUNAME")
                    .value(loginCode)
                    .path("/jqerp")
                    .domain(domain)
                    .build());
    }
}