// src/ui/ConsoleUI.java
package ui;

import model.Expense;
import model.PaymentMethod;
import service.ExpenseService;
import validation.ExpenseValidator;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Optional;

public class ConsoleUI {
    private final ExpenseService expenseService;
    private final Scanner scanner;
    private final DateTimeFormatter dateFormatter;

    public ConsoleUI(ExpenseService expenseService) {
        this.expenseService = expenseService;
        this.scanner = new Scanner(System.in);
        this.dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    }

    public void start() {
        System.out.println("=== Expense Tracker Application ===");
        System.out.println("Welcome to your personal finance manager!");

        while (true) {
            displayMainMenu();
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1":
                    addExpense();
                    break;
                case "2":
                    viewAllExpenses();
                    break;
                case "3":
                    viewExpensesByCategory();
                    break;
                case "4":
                    viewCategorySummary();
                    break;
                case "5":
                    viewMonthlySummary();
                    break;
                case "6":
                    deleteExpense();
                    break;
                case "7":
                    System.out.println("Thank you for using Expense Tracker. Goodbye!");
                    return;
                default:
                    System.out.println("Invalid choice. Please try again.");
            }

            System.out.println("\nPress Enter to continue...");
            scanner.nextLine();
        }
    }

    private void displayMainMenu() {
        System.out.println("\n=== MAIN MENU ===");
        System.out.println("1. Add New Expense");
        System.out.println("2. View All Expenses");
        System.out.println("3. View Expenses by Category");
        System.out.println("4. View Category Summary");
        System.out.println("5. View Monthly Summary");
        System.out.println("6. Delete Expense");
        System.out.println("7. Exit");
        System.out.print("Enter your choice (1-7): ");
    }

    private void addExpense() {
        System.out.println("\n=== ADD NEW EXPENSE ===");

        try {
            double amount = getValidatedAmount();
            String category = getValidatedCategory();
            String description = getValidatedDescription();
            LocalDate date = getValidatedDate();
            PaymentMethod paymentMethod = getValidatedPaymentMethod();

            Expense expense = expenseService.addExpense(amount, category, description, date, paymentMethod);
            System.out.println("✅ Expense added successfully!");
            System.out.println("Expense ID: " + expense.getId());

        } catch (Exception e) {
            System.out.println("❌ Error adding expense: " + e.getMessage());
        }
    }

    private double getValidatedAmount() {
        while (true) {
            System.out.print("Enter amount: ");
            String input = scanner.nextLine().trim();
            try {
                double amount = Double.parseDouble(input);
                if (amount <= 0) {
                    System.out.println("Amount must be greater than 0. Please try again.");
                    continue;
                }
                return amount;
            } catch (NumberFormatException e) {
                System.out.println("Invalid amount format. Please enter a valid number.");
            }
        }
    }

    private String getValidatedCategory() {
        while (true) {
            System.out.print("Enter category: ");
            String category = scanner.nextLine().trim();
            if (!category.isEmpty()) {
                return category;
            }
            System.out.println("Category cannot be empty. Please try again.");
        }
    }

    private String getValidatedDescription() {
        System.out.print("Enter description (optional): ");
        String description = scanner.nextLine().trim();
        return description.isEmpty() ? null : description;
    }

    private LocalDate getValidatedDate() {
        while (true) {
            System.out.print("Enter date (YYYY-MM-DD) or press Enter for today: ");
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) {
                return LocalDate.now();
            }
            try {
                return LocalDate.parse(input, dateFormatter);
            } catch (DateTimeParseException e) {
                System.out.println("Invalid date format. Please use YYYY-MM-DD format.");
            }
        }
    }

    private PaymentMethod getValidatedPaymentMethod() {
        while (true) {
            System.out.println("Available payment methods:");
            for (int i = 0; i < PaymentMethod.values().length; i++) {
                System.out.printf("%d. %s%n", i + 1, PaymentMethod.values()[i].getDisplayName());
            }
            System.out.print("Choose payment method (1-" + PaymentMethod.values().length + "): ");

            String input = scanner.nextLine().trim();
            try {
                int choice = Integer.parseInt(input);
                if (choice >= 1 && choice <= PaymentMethod.values().length) {
                    return PaymentMethod.values()[choice - 1];
                }
                System.out.println("Invalid choice. Please select a number between 1 and " + PaymentMethod.values().length);
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number.");
            }
        }
    }

    private void viewAllExpenses() {
        System.out.println("\n=== ALL EXPENSES ===");
        List<Expense> expenses = expenseService.getAllExpenses();

        if (expenses.isEmpty()) {
            System.out.println("No expenses found.");
            return;
        }

        displayExpenses(expenses);
        System.out.printf("Total spent: $%.2f%n", expenseService.getTotalSpent());
    }

    private void viewExpensesByCategory() {
        System.out.println("\n=== EXPENSES BY CATEGORY ===");
        String category = getValidatedCategory();

        List<Expense> expenses = expenseService.getExpensesByCategory(category);

        if (expenses.isEmpty()) {
            System.out.println("No expenses found for category: " + category);
            return;
        }

        displayExpenses(expenses);
        double categoryTotal = expenses.stream().mapToDouble(Expense::getAmount).sum();
        System.out.printf("Total for '%s': $%.2f%n", category, categoryTotal);
    }

    private void viewCategorySummary() {
        System.out.println("\n=== CATEGORY SUMMARY ===");
        Map<String, Double> summary = expenseService.getCategorySummary();

        if (summary.isEmpty()) {
            System.out.println("No expenses found.");
            return;
        }

        summary.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .forEach(entry ->
                        System.out.printf("%-20s: $%.2f%n", entry.getKey(), entry.getValue()));

        System.out.printf("%nTotal spent: $%.2f%n", expenseService.getTotalSpent());
    }

    private void viewMonthlySummary() {
        System.out.println("\n=== MONTHLY SUMMARY ===");

        int year = getValidatedYear();
        int month = getValidatedMonth();

        Map<String, Double> summary = expenseService.getMonthlySummary(year, month);

        if (summary.isEmpty()) {
            System.out.printf("No expenses found for %d-%02d%n", year, month);
            return;
        }

        System.out.printf("Expenses for %d-%02d:%n", year, month);
        summary.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .forEach(entry ->
                        System.out.printf("%-20s: $%.2f%n", entry.getKey(), entry.getValue()));

        double monthlyTotal = summary.values().stream().mapToDouble(Double::doubleValue).sum();
        System.out.printf("%nTotal for month: $%.2f%n", monthlyTotal);
    }

    private void deleteExpense() {
        System.out.println("\n=== DELETE EXPENSE ===");
        viewAllExpenses();

        System.out.print("Enter expense ID to delete: ");
        String id = scanner.nextLine().trim();

        if (id.isEmpty()) {
            System.out.println("No ID entered. Operation cancelled.");
            return;
        }

        if (expenseService.deleteExpense(id)) {
            System.out.println("✅ Expense deleted successfully!");
        } else {
            System.out.println("❌ Expense not found with ID: " + id);
        }
    }

    private void displayExpenses(List<Expense> expenses) {
        System.out.printf("%-36s %-12s %-15s %-20s %-12s %s%n",
                "ID", "Date", "Amount", "Category", "Payment", "Description");
        System.out.println("-".repeat(120));

        expenses.forEach(expense ->
                System.out.printf("%-36s %-12s $%-14.2f %-20s %-12s %s%n",
                        expense.getId().substring(0, 8) + "...",
                        expense.getDate(),
                        expense.getAmount(),
                        expense.getCategory(),
                        expense.getPaymentMethod().getDisplayName(),
                        expense.getDescription()
                )
        );
    }

    private int getValidatedYear() {
        while (true) {
            System.out.print("Enter year (e.g., 2024): ");
            String input = scanner.nextLine().trim();
            try {
                int year = Integer.parseInt(input);
                if (year >= 2000 && year <= 2100) {
                    return year;
                }
                System.out.println("Please enter a valid year between 2000 and 2100.");
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number.");
            }
        }
    }

    private int getValidatedMonth() {
        while (true) {
            System.out.print("Enter month (1-12): ");
            String input = scanner.nextLine().trim();
            try {
                int month = Integer.parseInt(input);
                if (month >= 1 && month <= 12) {
                    return month;
                }
                System.out.println("Please enter a month between 1 and 12.");
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number.");
            }
        }
    }
}