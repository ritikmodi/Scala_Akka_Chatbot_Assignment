package com.example.SpringProject1.services;

import com.example.SpringProject1.entities.Author;
import com.example.SpringProject1.repositaries.AuthorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AuthorService {

    @Autowired
    private AuthorRepository authorRepository;

    public List<Author> getAllAuthors() {
        return authorRepository.findAll();
    }

    public Optional<Author> getAuthorById(Long id) {
        return authorRepository.findById(id);
    }

    public Author setAuthor(Author author) {
        return authorRepository.save(author);
    }

    public Author updateAuthor(Long id, Author newAuthor) {
        Author author = authorRepository.findById(id).orElseThrow();
        author.setName(newAuthor.getName());
        return authorRepository.save(author);
    }

    public void deleteAuthor(Long id) {
        authorRepository.deleteById(id);
    }
}

