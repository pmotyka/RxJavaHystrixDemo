Source code example from http://www.meetup.com/sfjava/events/218340112/

MovieService 
$ ./gradlew build && java -jar build/libs/movie-service-0.1.0.jar

RatingService
$ ./gradlew build && java -jar build/libs/rating-service-0.1.0.jar

SocialService
$ ./gradlew build && java -jar build/libs/social-service-0.1.0.jar

$ curl http://localhost:9000/1
{
  "id" : 1,
  "title" : "What About Bob?",
  "friend" : "Bob",
  "rating" : 5
}
