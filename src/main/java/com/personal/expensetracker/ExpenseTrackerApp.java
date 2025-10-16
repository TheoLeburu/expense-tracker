package com.personal.expensetracker;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import model.Expense;
import model.PaymentMethod;
import service.ExpenseService;
import persistence.FileExpenseRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class ExpenseTrackerApp extends Application {
    private ExpenseService expenseService;
    private TableView<Expense> expensesTable;
    private Label totalLabel;

    @Override
    public void start(Stage primaryStage) {
        // Initialize services
        expenseService = new ExpenseService(new FileExpenseRepository("data"));

        // Create main layout
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(15));

        // Create top section with title
        Label titleLabel = new Label("Personal Expense Tracker");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        BorderPane.setAlignment(titleLabel, Pos.CENTER);
        root.setTop(titleLabel);

        // Create center section with tabs
        TabPane tabPane = new TabPane();

        // Tab 1: Add Expense
        Tab addExpenseTab = new Tab("Add Expense");
        addExpenseTab.setClosable(false);
        addExpenseTab.setContent(createAddExpenseForm());

        // Tab 2: View Expenses
        Tab viewExpensesTab = new Tab("View Expenses");
        viewExpensesTab.setClosable(false);
        viewExpensesTab.setContent(createViewExpensesTab());

        // Tab 3: Analytics
        Tab analyticsTab = new Tab("Analytics");
        analyticsTab.setClosable(false);
        analyticsTab.setContent(createAnalyticsTab());

        tabPane.getTabs().addAll(addExpenseTab, viewExpensesTab, analyticsTab);
        root.setCenter(tabPane);

        // Create scene and stage
        Scene scene = new Scene(root, 900, 700);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());

        primaryStage.setTitle("Expense Tracker - Personal Finance Manager");
        primaryStage.setScene(scene);
        primaryStage.show();

        // Load initial data
        refreshExpensesTable();
        updateAnalytics();
    }

    private VBox createAddExpenseForm() {
        VBox form = new VBox(15);
        form.setPadding(new Insets(20));
        form.setStyle("-fx-background-color: #f8f9fa;");

        Label titleLabel = new Label("Add New Expense");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        // Form fields
        TextField amountField = new TextField();
        amountField.setPromptText("Enter amount");

        TextField categoryField = new TextField();
        categoryField.setPromptText("Enter category (e.g., Food, Transport)");

        TextArea descriptionArea = new TextArea();
        descriptionArea.setPromptText("Enter description (optional)");
        descriptionArea.setPrefHeight(60);

        DatePicker datePicker = new DatePicker();
        datePicker.setValue(LocalDate.now());

        ComboBox<PaymentMethod> paymentCombo = new ComboBox<>();
        paymentCombo.getItems().addAll(PaymentMethod.values());
        paymentCombo.setValue(PaymentMethod.CASH);

        Button addButton = new Button("Add Expense");
        addButton.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold;");
        addButton.setPrefWidth(200);

        Label messageLabel = new Label();

        // Add button action
        addButton.setOnAction(e -> {
            try {
                double amount = Double.parseDouble(amountField.getText());
                String category = categoryField.getText().trim();
                String description = descriptionArea.getText().trim();
                LocalDate date = datePicker.getValue();
                PaymentMethod paymentMethod = paymentCombo.getValue();

                Expense expense = expenseService.addExpense(amount, category, description, date, paymentMethod);

                // Clear form
                amountField.clear();
                categoryField.clear();
                descriptionArea.clear();
                datePicker.setValue(LocalDate.now());
                paymentCombo.setValue(PaymentMethod.CASH);

                messageLabel.setText("✅ Expense added successfully!");
                messageLabel.setStyle("-fx-text-fill: green;");

                // Refresh tables
                refreshExpensesTable();
                updateAnalytics();

            } catch (Exception ex) {
                messageLabel.setText("❌ Error: " + ex.getMessage());
                messageLabel.setStyle("-fx-text-fill: red;");
            }
        });

        form.getChildren().addAll(
                titleLabel, amountField, categoryField, descriptionArea,
                new Label("Date:"), datePicker,
                new Label("Payment Method:"), paymentCombo,
                addButton, messageLabel
        );

        return form;
    }

    private VBox createViewExpensesTab() {
        VBox layout = new VBox(15);
        layout.setPadding(new Insets(20));

        Label titleLabel = new Label("All Expenses");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        // Create table
        expensesTable = new TableView<>();

        TableColumn<Expense, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getDate().toString()));

        TableColumn<Expense, String> amountCol = new TableColumn<>("Amount");
        amountCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(
                        String.format("$%.2f", cellData.getValue().getAmount())));

        TableColumn<Expense, String> categoryCol = new TableColumn<>("Category");
        categoryCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getCategory()));

        TableColumn<Expense, String> paymentCol = new TableColumn<>("Payment Method");
        paymentCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(
                        cellData.getValue().getPaymentMethod().getDisplayName()));

        TableColumn<Expense, String> descCol = new TableColumn<>("Description");
        descCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getDescription()));

        expensesTable.getColumns().addAll(dateCol, amountCol, categoryCol, paymentCol, descCol);

        // Delete button
        Button deleteButton = new Button("Delete Selected Expense");
        deleteButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
        deleteButton.setOnAction(e -> deleteSelectedExpense());

        // Total label
        totalLabel = new Label();
        totalLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        layout.getChildren().addAll(titleLabel, expensesTable, deleteButton, totalLabel);
        VBox.setVgrow(expensesTable, Priority.ALWAYS);

        return layout;
    }

    private VBox createAnalyticsTab() {
        VBox layout = new VBox(15);
        layout.setPadding(new Insets(20));

        Label titleLabel = new Label("Spending Analytics");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        VBox analyticsContent = new VBox(10);
        analyticsContent.setId("analyticsContent"); // For CSS styling

        Button refreshButton = new Button("Refresh Analytics");
        refreshButton.setOnAction(e -> updateAnalytics());

        layout.getChildren().addAll(titleLabel, refreshButton, analyticsContent);
        VBox.setVgrow(analyticsContent, Priority.ALWAYS);

        return layout;
    }

    private void refreshExpensesTable() {
        List<Expense> expenses = expenseService.getAllExpenses();
        expensesTable.getItems().setAll(expenses);

        double total = expenseService.getTotalSpent();
        totalLabel.setText(String.format("Total Spent: $%.2f", total));
    }

    private void deleteSelectedExpense() {
        Expense selected = expensesTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            expenseService.deleteExpense(selected.getId());
            refreshExpensesTable();
            updateAnalytics();

            // Show confirmation
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Expense Deleted");
            alert.setHeaderText(null);
            alert.setContentText("Expense deleted successfully!");
            alert.showAndWait();
        } else {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("No Selection");
            alert.setHeaderText(null);
            alert.setContentText("Please select an expense to delete.");
            alert.showAndWait();
        }
    }

    private void updateAnalytics() {
        VBox analyticsContent = (VBox) ((VBox) ((Tab) ((TabPane)
                ((BorderPane) expensesTable.getScene().getRoot()).getCenter())
                .getTabs().get(2)).getContent()).getChildren().get(2);

        analyticsContent.getChildren().clear();

        // Category summary
        Label categoryTitle = new Label("Spending by Category:");
        categoryTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");
        analyticsContent.getChildren().add(categoryTitle);

        Map<String, Double> categorySummary = expenseService.getCategorySummary();
        if (categorySummary.isEmpty()) {
            analyticsContent.getChildren().add(new Label("No expenses recorded yet."));
        } else {
            categorySummary.entrySet().stream()
                    .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                    .forEach(entry -> {
                        HBox row = new HBox(10);
                        Label categoryLabel = new Label(entry.getKey() + ":");
                        categoryLabel.setStyle("-fx-font-weight: bold;");
                        Label amountLabel = new Label(String.format("$%.2f", entry.getValue()));
                        amountLabel.setStyle("-fx-text-fill: #2c3e50;");
                        row.getChildren().addAll(categoryLabel, amountLabel);
                        analyticsContent.getChildren().add(row);
                    });
        }

        // Monthly summary
        Label monthlyTitle = new Label("\nThis Month's Spending:");
        monthlyTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");
        analyticsContent.getChildren().add(monthlyTitle);

        LocalDate now = LocalDate.now();
        Map<String, Double> monthlySummary = expenseService.getMonthlySummary(now.getYear(), now.getMonthValue());
        if (monthlySummary.isEmpty()) {
            analyticsContent.getChildren().add(new Label("No expenses this month."));
        } else {
            monthlySummary.entrySet().stream()
                    .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                    .forEach(entry -> {
                        HBox row = new HBox(10);
                        Label categoryLabel = new Label(entry.getKey() + ":");
                        Label amountLabel = new Label(String.format("$%.2f", entry.getValue()));
                        row.getChildren().addAll(categoryLabel, amountLabel);
                        analyticsContent.getChildren().add(row);
                    });
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}