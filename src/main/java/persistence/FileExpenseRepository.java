package persistence;

import model.Expense;
import model.PaymentMethod;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class FileExpenseRepository implements ExpenseRepository {
    private final String dataFile;
    private final Map<String, Expense> expenses;

    public FileExpenseRepository(String dataDirectory) {
        // Use .txt format instead of .dat for better compatibility
        this.dataFile = dataDirectory + File.separator + "expenses.txt";
        this.expenses = new ConcurrentHashMap<>();
        loadData();
    }

    private void loadData() {
        File file = new File(dataFile);

        // Create directory if it doesn't exist
        File parentDir = file.getParentFile();
        if (!parentDir.exists()) {
            boolean dirsCreated = parentDir.mkdirs();
            if (!dirsCreated) {
                System.err.println("Warning: Could not create data directory: " + parentDir.getAbsolutePath());
                return;
            }
        }

        if (!file.exists()) {
            System.out.println("Data file doesn't exist yet. It will be created on first save.");
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            int loadedCount = 0;

            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;

                try {
                    Expense expense = parseExpense(line);
                    if (expense != null) {
                        expenses.put(expense.getId(), expense);
                        loadedCount++;
                    }
                } catch (Exception e) {
                    System.err.println("Failed to parse expense line: " + line + " - " + e.getMessage());
                }
            }

            System.out.println("Loaded " + loadedCount + " expenses from " + dataFile);

        } catch (IOException e) {
            System.err.println("Warning: Failed to load expense data from " + dataFile + ": " + e.getMessage());
        }
    }

    private Expense parseExpense(String line) {
        try {
            String[] parts = line.split("\\|");
            if (parts.length != 6) {
                System.err.println("Invalid expense format: " + line);
                return null;
            }

            String id = parts[0];
            double amount = Double.parseDouble(parts[1]);
            String category = parts[2];
            String description = parts[3];
            LocalDate date = LocalDate.parse(parts[4]);
            PaymentMethod paymentMethod = PaymentMethod.valueOf(parts[5]);

            return new Expense(id, amount, category, description, date, paymentMethod);

        } catch (Exception e) {
            System.err.println("Error parsing expense: " + e.getMessage());
            return null;
        }
    }

    private String formatExpense(Expense expense) {
        return String.join("|",
                expense.getId(),
                String.valueOf(expense.getAmount()),
                expense.getCategory(),
                expense.getDescription(),
                expense.getDate().toString(),
                expense.getPaymentMethod().name()
        );
    }

    private void saveData() {
        try {
            File file = new File(dataFile);

            // Ensure directory exists
            File parentDir = file.getParentFile();
            if (!parentDir.exists()) {
                boolean dirsCreated = parentDir.mkdirs();
                if (!dirsCreated) {
                    throw new IOException("Failed to create directory: " + parentDir.getAbsolutePath());
                }
            }

            // Write data as text
            try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
                for (Expense expense : expenses.values()) {
                    writer.println(formatExpense(expense));
                }
                System.out.println("Saved " + expenses.size() + " expenses to " + dataFile);
            }

        } catch (IOException e) {
            throw new RuntimeException("Failed to save expense data to " + dataFile + ": " + e.getMessage(), e);
        }
    }

    @Override
    public void save(Expense expense) {
        expenses.put(expense.getId(), expense);
        saveData();
    }

    @Override
    public Optional<Expense> findById(String id) {
        return Optional.ofNullable(expenses.get(id));
    }

    @Override
    public List<Expense> findAll() {
        // Return sorted by date (newest first)
        return expenses.values().stream()
                .sorted((e1, e2) -> e2.getDate().compareTo(e1.getDate()))
                .collect(Collectors.toList());
    }

    @Override
    public List<Expense> findByCategory(String category) {
        return expenses.values().stream()
                .filter(expense -> expense.getCategory().equalsIgnoreCase(category))
                .sorted((e1, e2) -> e2.getDate().compareTo(e1.getDate()))
                .collect(Collectors.toList());
    }

    @Override
    public List<Expense> findByDateRange(String startDate, String endDate) {
        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);

        return expenses.values().stream()
                .filter(expense -> !expense.getDate().isBefore(start) && !expense.getDate().isAfter(end))
                .sorted((e1, e2) -> e2.getDate().compareTo(e1.getDate()))
                .collect(Collectors.toList());
    }

    @Override
    public boolean delete(String id) {
        boolean removed = expenses.remove(id) != null;
        if (removed) {
            saveData();
        }
        return removed;
    }

    @Override
    public void update(Expense expense) {
        if (expenses.containsKey(expense.getId())) {
            expenses.put(expense.getId(), expense);
            saveData();
        }
    }
}