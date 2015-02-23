package com.example.movie;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.netflix.config.DynamicIntProperty;
import com.netflix.config.DynamicPropertyFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import com.fasterxml.jackson.databind.ObjectMapper;
import rx.Observable;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@RestController
public class MovieController {

    private Logger logger =  LoggerFactory.getLogger(MovieController.class);
    
    @RequestMapping(value="/{id}", method= RequestMethod.GET )
    public String index(@PathVariable Integer id) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        JsonNode root = mapper.readTree(this.getClass().getClassLoader().getResourceAsStream("data.json"));
        ArrayNode movies = (ArrayNode) root.path("movies");
        TypeReference<List<Movie>> typeRef = new TypeReference<List<Movie>>(){};
        List<Movie> moviesList = mapper.readValue(movies.traverse(), typeRef);

        Movie theMovie = null;
        for (Movie movie : moviesList) {
            if(id.equals(movie.getId()))
            {
                theMovie = movie;
                RatingCommand ratingCommand = new RatingCommand(id);
                Observable<String> ratingObservable = ratingCommand.observe();
                theMovie.setRating(mapper.readTree(ratingObservable.toBlocking().first()).get("rating").asInt());
                SocialCommand socialCommand = new SocialCommand(id);
                Observable<String> socialObservable = socialCommand.observe();
                theMovie.setFriend(mapper.readTree(socialObservable.toBlocking().first()).get("friend").asText());
            }
        }

        return mapper.writeValueAsString(theMovie);
    }
}
