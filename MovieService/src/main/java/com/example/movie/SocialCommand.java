package com.example.movie;

import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixObservableCommand;

import rx.Observable;
import io.reactivex.netty.RxNetty;

import java.nio.charset.Charset;

public class SocialCommand extends HystrixObservableCommand<String> {
    private int id;

    public SocialCommand(int id) {
        super(HystrixCommandGroupKey.Factory.asKey("SocialGroup"));
        this.id = id;
    }

    @Override
    protected Observable<String> construct() {
        return RxNetty.createHttpGet("http://localhost:9002/" + id)
                .flatMap(response -> response.getContent())
                .map(data -> data.toString(Charset.defaultCharset()));
    }

    @Override
    protected Observable<String> resumeWithFallback() {
        return Observable.just("{\"id\":" + id +",\"friend\":\"None\"}");
    }
}
