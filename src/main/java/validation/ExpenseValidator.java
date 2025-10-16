// src/validation/ExpenseValidator.java
package validation;

import model.Expense;
import model.PaymentMethod;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ExpenseValidator {

    public static ValidationResult validateExpenseData(double amount, String category,
                                                       String description, LocalDate date) {
        List<String> errors = new ArrayList<>();

        // Amount validation
        if (amount <= 0) {
            errors.add("Amount must be greater than 0");
        }
        if (amount > 1_000_000) {
            errors.add("Amount exceeds maximum limit of 1,000,000");
        }

        // Category validation
        if (category == null || category.trim().isEmpty()) {
            errors.add("Category cannot be empty");
        } else if (category.length() > 50) {
            errors.add("Category cannot exceed 50 characters");
        }

        // Description validation
        if (description != null && description.length() > 200) {
            errors.add("Description cannot exceed 200 characters");
        }

        // Date validation
        if (date == null) {
            errors.add("Date cannot be null");
        } else if (date.isAfter(LocalDate.now().plusDays(1))) {
            errors.add("Date cannot be in the future");
        } else if (date.isBefore(LocalDate.now().minusYears(10))) {
            errors.add("Date cannot be more than 10 years in the past");
        }

        return new ValidationResult(errors.isEmpty(), errors);
    }

    public static boolean isValidPaymentMethod(String paymentMethod) {
        if (paymentMethod == null) return false;

        for (PaymentMethod method : PaymentMethod.values()) {
            if (method.name().equalsIgnoreCase(paymentMethod) ||
                    method.getDisplayName().equalsIgnoreCase(paymentMethod)) {
                return true;
            }
        }
        return false;
    }

    public static PaymentMethod parsePaymentMethod(String paymentMethod) {
        for (PaymentMethod method : PaymentMethod.values()) {
            if (method.name().equalsIgnoreCase(paymentMethod) ||
                    method.getDisplayName().equalsIgnoreCase(paymentMethod)) {
                return method;
            }
        }
        throw new IllegalArgumentException("Invalid payment method: " + paymentMethod);
    }
}