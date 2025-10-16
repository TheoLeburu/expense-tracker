// src/persistence/ExpenseRepository.java
package persistence;

import model.Expense;

import java.util.List;
import java.util.Optional;

public interface ExpenseRepository {
    void save(Expense expense);
    Optional<Expense> findById(String id);
    List<Expense> findAll();
    List<Expense> findByCategory(String category);
    List<Expense> findByDateRange(String startDate, String endDate);
    boolean delete(String id);
    void update(Expense expense);
}