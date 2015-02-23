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
    /* used to track number of connections and throttle */
    private static AtomicInteger concurrentConnections = new AtomicInteger(0);
    private static DynamicIntProperty maxConcurrentConnections = DynamicPropertyFactory.getInstance().getIntProperty("hystrix.stream.maxConcurrentConnections", 5);
    private static volatile boolean isDestroyed = false;

    @RequestMapping(value="/hystrix.stream", method= RequestMethod.GET )
    public void hystrixStream(HttpServletRequest request, HttpServletResponse response) throws Exception {
        int numberConnections = concurrentConnections.incrementAndGet();
        HystrixMetricsPoller poller = null;
        try {
            if (numberConnections > maxConcurrentConnections.get()) {
                 response.sendError(503, "MaxConcurrentConnections reached: " + maxConcurrentConnections.get());
            } else {

                int delay = 500;
                try {
                    String d = request.getParameter("delay");
                    if (d != null) {
                        delay = Integer.parseInt(d);
                    }
                } catch (Exception e) {
                    // ignore if it's not a number
                }

                /* initialize response */
                response.setHeader("Content-Type", "text/event-stream;charset=UTF-8");
                response.setHeader("Cache-Control", "no-cache, no-store, max-age=0, must-revalidate");
                response.setHeader("Pragma", "no-cache");

                MetricJsonListener jsonListener = new MetricJsonListener();
                poller = new HystrixMetricsPoller(jsonListener, delay);
                // start polling and it will write directly to the output stream
                poller.start();
                logger.info("Starting poller");

                // we will use a "single-writer" approach where the Servlet thread does all the writing
                // by fetching JSON messages from the MetricJsonListener to write them to the output
                try {
                    while (poller.isRunning() && !isDestroyed) {
                        List<String> jsonMessages = jsonListener.getJsonMetrics();
                        if (jsonMessages.isEmpty()) {
                            // https://github.com/Netflix/Hystrix/issues/85 hystrix.stream holds connection open if no metrics
                            // we send a ping to test the connection so that we'll get an IOException if the client has disconnected
                            response.getWriter().println("ping: \n");
                        } else {
                            for (String json : jsonMessages) {
                                response.getWriter().println("data: " + json + "\n");
                            }
                        }

                        /* shortcut breaking out of loop if we have been destroyed */
                        if(isDestroyed) {
                            break;
                        }

                        // after outputting all the messages we will flush the stream
                        response.flushBuffer();

                        // explicitly check for client disconnect - PrintWriter does not throw exceptions
                        if (response.getWriter().checkError()) {
                            throw new IOException("io error");
                        }

                        // now wait the 'delay' time
                        Thread.sleep(delay);
                    }
                } catch (InterruptedException e) {
                    poller.shutdown();
                    logger.debug("InterruptedException. Will stop polling.");
                    Thread.currentThread().interrupt();
                } catch (IOException e) {
                    poller.shutdown();
                    // debug instead of error as we expect to get these whenever a client disconnects or network issue occurs
                    logger.debug("IOException while trying to write (generally caused by client disconnecting). Will stop polling.", e);
                } catch (Exception e) {
                    poller.shutdown();
                    logger.error("Failed to write. Will stop polling.", e);
                }
                logger.debug("Stopping Turbine stream to connection");
            }
        } catch (Exception e) {
            logger.error("Error initializing servlet for metrics event stream.", e);
        } finally {
            concurrentConnections.decrementAndGet();
            if (poller != null) {
                poller.shutdown();
            }
        }
    }
    private static class MetricJsonListener implements HystrixMetricsPoller.MetricsAsJsonPollerListener {

        /**
         * Setting limit to 1000. In a healthy system there isn't any reason to hit this limit so if we do it will throw an exception which causes the poller to stop.
         * <p>
         * This is a safety check against a runaway poller causing memory leaks.
         */
        private final LinkedBlockingQueue<String> jsonMetrics = new LinkedBlockingQueue<>(1000);

        /**
         * Store JSON messages in a queue.
         */
        @Override
        public void handleJsonMetric(String json) {
            jsonMetrics.add(json);
        }

        /**
         * Get all JSON messages in the queue.
         *
         * @return
         */
        public List<String> getJsonMetrics() {
            ArrayList<String> metrics = new ArrayList<>();
            jsonMetrics.drainTo(metrics);
            return metrics;
        }
    }
    
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