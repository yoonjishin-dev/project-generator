package com.generated.todoservice.todo;

import com.generated.todoservice.category.Category;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "todo")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Todo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "memo", columnDefinition = "TEXT")
    private String memo;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "completed", nullable = false)
    private Boolean completed;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Todo(String title, String memo, LocalDate dueDate, Boolean completed, Category category) {
        this.title = title;
        this.memo = memo;
        this.dueDate = dueDate;
        this.completed = completed;
        this.category = category;
    }

    public void update(String title, String memo, LocalDate dueDate, Boolean completed, Category category) {
        this.title = title;
        this.memo = memo;
        this.dueDate = dueDate;
        this.completed = completed;
        this.category = category;
    }
}
