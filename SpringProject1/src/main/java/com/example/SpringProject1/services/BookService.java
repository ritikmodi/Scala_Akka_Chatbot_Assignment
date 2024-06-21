package com.example.SpringProject1.services;

import com.example.SpringProject1.entities.Book;
import com.example.SpringProject1.repositaries.BookRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class BookService {

    @Autowired
    private BookRepository bookRepository;

    public List<Book> getAllBooks() {
        return bookRepository.findAll();
    }

    public Optional<Book> getBookById(Long id) {
        return bookRepository.findById(id);
    }

    public Book setBook(Book book) {
        return bookRepository.save(book);
    }

    public Book updateBook(Long id, Book newBook) {
        Book book = bookRepository.findById(id).orElseThrow();
        book.setTitle(newBook.getTitle());
        return bookRepository.save(book);
    }

    public void deleteBook(Long id) {
        bookRepository.deleteById(id);
    }
}
