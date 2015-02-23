package com.example.movie;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
public class RatingController {

    private Logger log = LoggerFactory.getLogger(RatingController.class);

    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public String index(@PathVariable Integer id) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(this.getClass().getClassLoader().getResourceAsStream("data.json"));
        ArrayNode ratings = (ArrayNode) root.path("ratings");
        TypeReference<List<Rating>> typeRef = new TypeReference<List<Rating>>() {};
        List<Rating> ratingList = mapper.readValue(ratings.traverse(), typeRef);

        Rating theRating = null;
        for (Rating rating : ratingList) {
            if (id.equals(rating.getId())) {
                log.debug("Found rating with id: " + rating.getId());
                theRating = rating;
            }
        }

        return mapper.writeValueAsString(theRating);
    }
}
