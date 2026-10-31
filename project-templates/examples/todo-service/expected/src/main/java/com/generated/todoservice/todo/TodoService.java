package com.generated.todoservice.todo;

import com.generated.todoservice.common.NotFoundException;
import com.generated.todoservice.category.Category;
import com.generated.todoservice.category.CategoryRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TodoService {

    private final TodoRepository todoRepository;
    private final CategoryRepository categoryRepository;

    public List<TodoResponse> findAll() {
        return todoRepository.findAll().stream()
                .map(TodoResponse::from)
                .toList();
    }

    public TodoResponse findById(Long id) {
        return TodoResponse.from(getTodo(id));
    }

    @Transactional
    public TodoResponse create(TodoRequest request) {
        Category category = findCategory(request.categoryId());
        Todo todo = new Todo(request.title(), request.memo(), request.dueDate(), request.completed(), category);
        return TodoResponse.from(todoRepository.save(todo));
    }

    @Transactional
    public TodoResponse update(Long id, TodoRequest request) {
        Todo todo = getTodo(id);
        Category category = findCategory(request.categoryId());
        todo.update(request.title(), request.memo(), request.dueDate(), request.completed(), category);
        return TodoResponse.from(todoRepository.saveAndFlush(todo));
    }

    @Transactional
    public void delete(Long id) {
        todoRepository.delete(getTodo(id));
    }

    private Todo getTodo(Long id) {
        return todoRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("할 일", id));
    }

    private Category findCategory(Long id) {
        if (id == null) {
            return null;
        }
        return categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("카테고리", id));
    }
}
