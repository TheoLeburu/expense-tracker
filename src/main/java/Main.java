import persistence.FileExpenseRepository;
import service.ExpenseService;
import service.ChartService;
import model.Expense;
import model.PaymentMethod;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main {
    private static ExpenseService expenseService;
    private static ChartService chartService;
    private static JTable expensesTable;
    private static DefaultTableModel tableModel;
    private static JLabel totalLabel;
    private static JTabbedPane mainTabbedPane;
    private static JTabbedPane chartTabs;

    public static void main(String[] args) {
        try {
            expenseService = new ExpenseService(new FileExpenseRepository("data"));
            chartService = new ChartService(expenseService);

            SwingUtilities.invokeLater(() -> createAndShowGUI());

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,
                    "Failed to start application: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private static void createAndShowGUI() {
        JFrame frame = new JFrame("Personal Expense Tracker");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1000, 700);
        frame.setLocationRelativeTo(null);

        mainTabbedPane = new JTabbedPane();
        mainTabbedPane.addTab("Add Expense", createAddExpensePanel());
        mainTabbedPane.addTab("View Expenses", createViewExpensesPanel());
        mainTabbedPane.addTab("Analytics & Charts", createAnalyticsPanel());

        frame.add(mainTabbedPane);
        frame.setVisible(true);
    }

    private static JPanel createAddExpensePanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        JLabel titleLabel = new JLabel("Add New Expense");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        panel.add(titleLabel, gbc);

        gbc.gridwidth = 1;
        gbc.gridy = 1; gbc.gridx = 0;
        panel.add(new JLabel("Amount:"), gbc);
        JTextField amountField = new JTextField(15);
        gbc.gridx = 1;
        panel.add(amountField, gbc);

        gbc.gridy = 2; gbc.gridx = 0;
        panel.add(new JLabel("Category:"), gbc);
        JTextField categoryField = new JTextField(15);
        gbc.gridx = 1;
        panel.add(categoryField, gbc);

        gbc.gridy = 3; gbc.gridx = 0;
        panel.add(new JLabel("Description:"), gbc);
        JTextArea descriptionArea = new JTextArea(3, 15);
        JScrollPane descriptionScroll = new JScrollPane(descriptionArea);
        gbc.gridx = 1;
        panel.add(descriptionScroll, gbc);

        gbc.gridy = 4; gbc.gridx = 0;
        panel.add(new JLabel("Payment Method:"), gbc);
        JComboBox<PaymentMethod> paymentCombo = new JComboBox<>(PaymentMethod.values());
        gbc.gridx = 1;
        panel.add(paymentCombo, gbc);

        gbc.gridy = 5; gbc.gridx = 0; gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        JButton addButton = new JButton("Add Expense");
        addButton.setBackground(new Color(46, 204, 113));
        addButton.setForeground(Color.WHITE);
        addButton.setFont(new Font("Arial", Font.BOLD, 14));
        panel.add(addButton, gbc);

        gbc.gridy = 6;
        JLabel messageLabel = new JLabel(" ");
        panel.add(messageLabel, gbc);

        addButton.addActionListener(e -> {
            try {
                double amount = Double.parseDouble(amountField.getText());
                String category = categoryField.getText().trim();
                String description = descriptionArea.getText().trim();
                PaymentMethod paymentMethod = (PaymentMethod) paymentCombo.getSelectedItem();

                if (category.isEmpty()) {
                    messageLabel.setText("Please enter a category");
                    messageLabel.setForeground(Color.RED);
                    return;
                }

                if (amount <= 0) {
                    messageLabel.setText("Amount must be greater than 0");
                    messageLabel.setForeground(Color.RED);
                    return;
                }

                Expense expense = expenseService.addExpense(amount, category, description, LocalDate.now(), paymentMethod);

                amountField.setText("");
                categoryField.setText("");
                descriptionArea.setText("");
                paymentCombo.setSelectedIndex(0);

                messageLabel.setText("Expense added successfully!");
                messageLabel.setForeground(Color.GREEN);

                // Refresh everything
                refreshExpensesTable();
                refreshCharts();

            } catch (NumberFormatException ex) {
                messageLabel.setText("Please enter a valid amount");
                messageLabel.setForeground(Color.RED);
            } catch (Exception ex) {
                messageLabel.setText("Error: " + ex.getMessage());
                messageLabel.setForeground(Color.RED);
            }
        });

        return panel;
    }

    private static JPanel createViewExpensesPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel titleLabel = new JLabel("All Expenses");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        panel.add(titleLabel, BorderLayout.NORTH);

        String[] columns = {"Date", "Amount", "Category", "Payment Method", "Description"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        expensesTable = new JTable(tableModel);
        expensesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane tableScroll = new JScrollPane(expensesTable);
        panel.add(tableScroll, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout());

        totalLabel = new JLabel("Total: $0.00");
        totalLabel.setFont(new Font("Arial", Font.BOLD, 14));
        bottomPanel.add(totalLabel, BorderLayout.WEST);

        JButton deleteButton = new JButton("Delete Selected Expense");
        deleteButton.setBackground(new Color(231, 76, 60));
        deleteButton.setForeground(Color.WHITE);
        deleteButton.addActionListener(e -> deleteSelectedExpense());
        bottomPanel.add(deleteButton, BorderLayout.EAST);

        panel.add(bottomPanel, BorderLayout.SOUTH);

        refreshExpensesTable();

        return panel;
    }

    private static JPanel createAnalyticsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel titleLabel = new JLabel("Spending Analytics & Charts");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        panel.add(titleLabel, BorderLayout.NORTH);

        chartTabs = new JTabbedPane();
        refreshCharts(); // Initial chart creation

        panel.add(chartTabs, BorderLayout.CENTER);

        return panel;
    }

    private static void refreshExpensesTable() {
        tableModel.setRowCount(0);

        List<Expense> expenses = expenseService.getAllExpenses();

        for (Expense expense : expenses) {
            tableModel.addRow(new Object[]{
                    expense.getDate().toString(),
                    String.format("$%.2f", expense.getAmount()),
                    expense.getCategory(),
                    expense.getPaymentMethod().getDisplayName(),
                    expense.getDescription()
            });
        }

        double total = expenseService.getTotalSpent();
        totalLabel.setText(String.format("Total Spent: $%.2f", total));
    }

    private static void refreshCharts() {
        if (chartTabs != null) {
            chartService.refreshCharts(chartTabs);
        }
    }

    private static void deleteSelectedExpense() {
        int selectedRow = expensesTable.getSelectedRow();
        if (selectedRow >= 0) {
            try {
                // Get expense details directly from the table
                String date = (String) tableModel.getValueAt(selectedRow, 0);
                String amountStr = (String) tableModel.getValueAt(selectedRow, 1);
                String category = (String) tableModel.getValueAt(selectedRow, 2);

                // Find the actual expense to get the ID
                List<Expense> allExpenses = expenseService.getAllExpenses();
                Expense expenseToDelete = null;

                for (Expense expense : allExpenses) {
                    if (expense.getDate().toString().equals(date) &&
                            String.format("$%.2f", expense.getAmount()).equals(amountStr) &&
                            expense.getCategory().equals(category)) {
                        expenseToDelete = expense;
                        break;
                    }
                }

                if (expenseToDelete != null) {
                    int confirm = JOptionPane.showConfirmDialog(null,
                            "Are you sure you want to delete this expense?\n\n" +
                                    "Amount: $" + expenseToDelete.getAmount() + "\n" +
                                    "Category: " + expenseToDelete.getCategory() + "\n" +
                                    "Description: " + expenseToDelete.getDescription() + "\n" +
                                    "Date: " + expenseToDelete.getDate() + "\n" +
                                    "Payment: " + expenseToDelete.getPaymentMethod().getDisplayName(),
                            "Confirm Delete",
                            JOptionPane.YES_NO_OPTION);

                    if (confirm == JOptionPane.YES_OPTION) {
                        boolean deleted = expenseService.deleteExpense(expenseToDelete.getId());
                        if (deleted) {
                            refreshExpensesTable();
                            refreshCharts();
                            JOptionPane.showMessageDialog(null,
                                    "Expense deleted successfully!",
                                    "Success",
                                    JOptionPane.INFORMATION_MESSAGE);
                        } else {
                            JOptionPane.showMessageDialog(null,
                                    "Failed to delete expense.",
                                    "Error",
                                    JOptionPane.ERROR_MESSAGE);
                        }
                    }
                } else {
                    JOptionPane.showMessageDialog(null,
                            "Could not find the expense to delete.",
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                }

            } catch (Exception e) {
                JOptionPane.showMessageDialog(null,
                        "Error deleting expense: " + e.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        } else {
            JOptionPane.showMessageDialog(null,
                    "Please select an expense to delete.",
                    "No Selection",
                    JOptionPane.WARNING_MESSAGE);
        }
    }
}