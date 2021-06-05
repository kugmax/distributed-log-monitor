package com.course.bff.authors.controlles;

import com.course.bff.authors.models.Author;
import com.course.bff.authors.requests.CreateAuthorCommand;
import com.course.bff.authors.responses.AuthorResponse;
import com.course.bff.authors.services.AuthorService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Counter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Timed(value = "execution_duration", extraTags = {"AuthorController", "authors-service"})
@RestController
@RequestMapping("api/v1/authors")
public class AuthorController {

    @Value("${redis.topic}")
    private String redisTopic;

    private final static Logger logger = LoggerFactory.getLogger(AuthorController.class);

    @Autowired
    private AuthorService authorService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private Counter errorCounter;

    @Counted(value = "request_count", extraTags = {"AuthorController", "authors-service"})
    @GetMapping()
    public Collection<AuthorResponse> getAuthors() {
        logger.info("Get authors");
        List<AuthorResponse> authorResponses = new ArrayList<>();
        this.authorService.getAuthors().forEach(author -> {
            AuthorResponse authorResponse = createAuthorResponse(author);
            authorResponses.add(authorResponse);
        });

        return authorResponses;
    }

    @Counted(value = "request_count", extraTags = {"AuthorController", "authors-service"})
    @GetMapping("/{id}")
    public AuthorResponse getById(@PathVariable UUID id) {
        logger.info(String.format("Find authors by %s", id));
        Optional<Author> authorSearch = this.authorService.findById(id);
        if (authorSearch.isEmpty()) {
            throw new RuntimeException("Author isn't found");
        }

        return createAuthorResponse(authorSearch.get());
    }

    @Counted(value = "request_count", extraTags = {"AuthorController", "authors-service"})
    @PostMapping()
    public AuthorResponse createAuthors(@RequestBody CreateAuthorCommand createAuthorCommand) {
        logger.info("Create authors");
        Author author = this.authorService.create(createAuthorCommand);
        AuthorResponse authorResponse = createAuthorResponse(author);
        this.sendPushNotification(authorResponse);
        return authorResponse;
    }

    @ExceptionHandler(Exception.class)
    public void handleError(HttpServletRequest req, Exception ex) throws Exception {
        errorCounter.increment();
        throw ex;
    }

    private void sendPushNotification(AuthorResponse authorResponse) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try {
            redisTemplate.convertAndSend(redisTopic, gson.toJson(authorResponse));
        } catch (Exception e) {
            logger.error("Push Notification Error", e);
        }
    }

    private AuthorResponse createAuthorResponse(Author author) {
        AuthorResponse authorResponse = new AuthorResponse();
        authorResponse.setId(author.getId());
        authorResponse.setFirstName(author.getFirstName());
        authorResponse.setLastName(author.getLastName());
        authorResponse.setAddress(author.getAddress());
        authorResponse.setLanguage(author.getLanguage());
        return authorResponse;
    }
}
