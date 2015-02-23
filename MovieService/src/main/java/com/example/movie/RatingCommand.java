package com.example.movie;

import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixObservableCommand;
import io.reactivex.netty.RxNetty;
import rx.Observable;
import java.nio.charset.Charset;

public class RatingCommand extends HystrixObservableCommand<String> {
    private int id;

    public RatingCommand(int id) {
        super(HystrixCommandGroupKey.Factory.asKey("RatingGroup"));
        this.id = id;
    }

    @Override
    protected Observable<String> construct() {
        return RxNetty.createHttpGet("http://localhost:9001/" + id)
                .flatMap(response -> response.getContent())
                .map(data -> data.toString(Charset.defaultCharset()));
    }

    @Override
    protected Observable<String> resumeWithFallback() {
        return Observable.just("{\"id\":" + id +",\"rating\":\"3\"}");
    }
}