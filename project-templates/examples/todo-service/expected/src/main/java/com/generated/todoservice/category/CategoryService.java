package com.generated.todoservice.category;

import com.generated.todoservice.common.NotFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public List<CategoryResponse> findAll() {
        return categoryRepository.findAll().stream()
                .map(CategoryResponse::from)
                .toList();
    }

    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        Category category = new Category(request.name());
        return CategoryResponse.from(categoryRepository.save(category));
    }

    @Transactional
    public void delete(Long id) {
        categoryRepository.delete(getCategory(id));
    }

    private Category getCategory(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("카테고리", id));
    }
}
