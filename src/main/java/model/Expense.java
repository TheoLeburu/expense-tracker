package model;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

public class Expense implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String id;
    private final double amount;
    private final String category;
    private final String description;
    private final LocalDate date;
    private final PaymentMethod paymentMethod;

    public Expense(String id, double amount, String category, String description,
                   LocalDate date, PaymentMethod paymentMethod) {
        this.id = Objects.requireNonNull(id, "ID cannot be null");
        this.amount = amount;
        this.category = Objects.requireNonNull(category, "Category cannot be null");
        this.description = description != null ? description : "";
        this.date = date != null ? date : LocalDate.now();
        this.paymentMethod = paymentMethod != null ? paymentMethod : PaymentMethod.CASH;
    }

    // Getters
    public String getId() { return id; }
    public double getAmount() { return amount; }
    public String getCategory() { return category; }
    public String getDescription() { return description; }
    public LocalDate getDate() { return date; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Expense expense = (Expense) o;
        return Objects.equals(id, expense.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("Expense{id='%s', amount=%.2f, category='%s', date=%s}",
                id, amount, category, date);
    }
}