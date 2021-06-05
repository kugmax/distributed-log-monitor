package com.course.bff.books.controlles;

import com.course.bff.books.db.RedisClient;
import com.course.bff.books.models.Book;
import com.course.bff.books.requests.CreateBookCommand;
import com.course.bff.books.responses.BookResponse;
import com.course.bff.books.services.BookService;
import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Counter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Timed(value = "execution_duration", extraTags = {"BookController", "books-service"})
@RestController
@RequestMapping("api/v1/books")
public class BookController {

    private final static Logger logger = LoggerFactory.getLogger(BookController.class);

    @Autowired
    private BookService bookService;

    @Autowired
    private RedisClient redisClient;

    @Autowired
    private Counter errorCounter;

    @Counted(value = "request_count", extraTags = {"BookController", "books-service"})
    @GetMapping()
    public Collection<BookResponse> getBooks() {
        logger.info("Get book list");
        List<BookResponse> bookResponses = new ArrayList<>();
        this.bookService.getBooks().forEach(book -> {
            BookResponse bookResponse = createBookResponse(book);
            bookResponses.add(bookResponse);
        });

        return bookResponses;
    }

    @Counted(value = "request_count", extraTags = {"BookController", "books-service"})
    @GetMapping("/{id}")
    public BookResponse getById(@PathVariable UUID id) {
        logger.info(String.format("Find book by id %s", id));
        Optional<Book> bookSearch = this.bookService.findById(id);
        if (bookSearch.isEmpty()) {
            throw new RuntimeException("Book isn't found");
        }


        return createBookResponse(bookSearch.get());
    }

    @Counted(value = "request_count", extraTags = {"BookController", "books-service"})
    @PostMapping()
    public BookResponse createBooks(@RequestBody CreateBookCommand createBookCommand) {
        logger.info("Create books");
        Book book = this.bookService.create(createBookCommand);
        BookResponse authorResponse = createBookResponse(book);
        redisClient.sendPushNotification(authorResponse);
        return authorResponse;
    }

    @ExceptionHandler(Exception.class)
    public void handleError(HttpServletRequest req, Exception ex) throws Exception {
        errorCounter.increment();
        throw ex;
    }

    private BookResponse createBookResponse(Book book) {
        BookResponse bookResponse = new BookResponse();
        bookResponse.setId(book.getId());
        bookResponse.setAuthorId(book.getAuthorId());
        bookResponse.setPages(book.getPages());
        bookResponse.setTitle(book.getTitle());
        return bookResponse;
    }
}
