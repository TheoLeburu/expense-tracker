package service;

import model.Expense;
import model.PaymentMethod;
import persistence.ExpenseRepository;
import validation.ExpenseValidator;
import validation.ValidationResult;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.Optional;

public class ExpenseService {
    private final ExpenseRepository repository;

    public ExpenseService(ExpenseRepository repository) {
        this.repository = repository;
    }

    public Expense addExpense(double amount, String category, String description,
                              LocalDate date, PaymentMethod paymentMethod) {
        // Validate input data
        ValidationResult validation = ExpenseValidator.validateExpenseData(
                amount, category, description, date);
        validation.throwIfInvalid();

        // Generate unique ID
        String id = UUID.randomUUID().toString();

        // Create and save expense
        Expense expense = new Expense(id, amount, category.trim(),
                description != null ? description.trim() : "",
                date, paymentMethod);
        repository.save(expense);
        return expense;
    }

    public List<Expense> getAllExpenses() {
        return repository.findAll();
    }

    public List<Expense> getExpensesByCategory(String category) {
        if (category == null || category.trim().isEmpty()) {
            throw new IllegalArgumentException("Category cannot be empty");
        }
        return repository.findByCategory(category.trim());
    }

    public List<Expense> getExpensesByDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Start and end dates cannot be null");
        }
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date cannot be after end date");
        }

        return repository.findByDateRange(startDate.toString(), endDate.toString());
    }

    public boolean deleteExpense(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Expense ID cannot be empty");
        }
        return repository.delete(id.trim());
    }

    public Map<String, Double> getCategorySummary() {
        return repository.findAll().stream()
                .collect(Collectors.groupingBy(
                        Expense::getCategory,
                        Collectors.summingDouble(Expense::getAmount)
                ));
    }

    public Map<String, Double> getMonthlySummary(int year, int month) {
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());

        return getExpensesByDateRange(startDate, endDate).stream()
                .collect(Collectors.groupingBy(
                        Expense::getCategory,
                        Collectors.summingDouble(Expense::getAmount)
                ));
    }

    public double getTotalSpent() {
        return repository.findAll().stream()
                .mapToDouble(Expense::getAmount)
                .sum();
    }

    public Optional<Expense> findExpenseById(String id) {
        return repository.findById(id);
    }
}