package com.example.social;

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
public class SocialController {

    private Logger log =  LoggerFactory.getLogger(SocialController.class);
    
    @RequestMapping(value="/{id}", method= RequestMethod.GET )
    public String index(@PathVariable Integer id) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(this.getClass().getClassLoader().getResourceAsStream("data.json"));
        ArrayNode movies = (ArrayNode) root.path("friends");
        TypeReference<List<Friend>> typeRef = new TypeReference<List<Friend>>(){};
        List<Friend> friendList = mapper.readValue(movies.traverse(), typeRef);

        Friend theFriend = null;
        for (Friend movie : friendList) {
            if(id.equals(movie.getId()))
            {
                log.debug("Found friend with id: " + movie.getId());
                theFriend = movie;
            }
        }

        return mapper.writeValueAsString(theFriend);
    }
}
