package com.generated.todoservice.todo;

import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/todos")
@RequiredArgsConstructor
public class TodoController {

    private final TodoService todoService;

    @GetMapping
    public List<TodoResponse> list() {
        return todoService.findAll();
    }

    @GetMapping("/{id}")
    public TodoResponse get(@PathVariable Long id) {
        return todoService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TodoResponse create(@Valid @RequestBody TodoRequest request) {
        return todoService.create(request);
    }

    @PutMapping("/{id}")
    public TodoResponse update(@PathVariable Long id, @Valid @RequestBody TodoRequest request) {
        return todoService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        todoService.delete(id);
    }
}
