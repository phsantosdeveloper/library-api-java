package com.example.library.specifications;

import com.example.library.entities.Book;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class BookSpecifications {

    private BookSpecifications() {
    }

    public static Specification<Book> filter(String title, String isbn, String author, String category) {
        Specification<Book> spec = Specification.unrestricted();

        if (StringUtils.hasText(title))    spec = spec.and(titleContains(title));
        if (StringUtils.hasText(isbn))     spec = spec.and(isbnEquals(isbn));
        if (StringUtils.hasText(author))   spec = spec.and(authorNameContains(author));
        if (StringUtils.hasText(category)) spec = spec.and(categoryNameContains(category));

        return spec;
    }

    private static Specification<Book> titleContains(String title) {
        return (root, query, cb) ->
                cb.like(cb.upper(root.get("title")), like(title));
    }

    private static Specification<Book> isbnEquals(String isbn) {
        return (root, query, cb) ->
                cb.equal(root.get("isbn"), isbn.trim());
    }

    private static Specification<Book> authorNameContains(String name) {
        return (root, query, cb) ->
                cb.like(cb.upper(root.get("author").get("name")), like(name));
    }

    private static Specification<Book> categoryNameContains(String name) {
        return (root, query, cb) ->
                cb.like(cb.upper(root.get("category").get("name")), like(name));
    }

    private static String like(String value) {
        return "%" + value.trim().toUpperCase() + "%";
    }
}
