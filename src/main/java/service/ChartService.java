package service;

import model.Expense;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PiePlot;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ChartService {
    private final ExpenseService expenseService;

    public ChartService(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    public JPanel createCategoryPieChart() {
        DefaultPieDataset dataset = new DefaultPieDataset();

        Map<String, Double> categorySummary = expenseService.getCategorySummary();

        if (categorySummary.isEmpty()) {
            // Create empty dataset with message
            dataset.setValue("No Data Available", 1);
        } else {
            categorySummary.forEach((category, amount) -> {
                if (amount > 0) {
                    dataset.setValue(category + " ($" + String.format("%.2f", amount) + ")", amount);
                }
            });
        }

        JFreeChart chart = ChartFactory.createPieChart(
                "Spending by Category",
                dataset,
                true,
                true,
                false
        );

        PiePlot plot = (PiePlot) chart.getPlot();
        plot.setSectionOutlinesVisible(false);
        plot.setLabelFont(new Font("SansSerif", Font.PLAIN, 12));
        plot.setNoDataMessage("No data available");
        plot.setCircular(true);

        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(500, 300));
        return chartPanel;
    }

    public JPanel createMonthlyBarChart() {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        LocalDate now = LocalDate.now();
        Map<String, Double> monthlySummary = expenseService.getMonthlySummary(now.getYear(), now.getMonthValue());

        if (monthlySummary.isEmpty()) {
            dataset.addValue(0, "Spending", "No Data");
        } else {
            monthlySummary.forEach((category, amount) -> {
                dataset.addValue(amount, "Spending", category);
            });
        }

        JFreeChart chart = ChartFactory.createBarChart(
                "Monthly Spending by Category",
                "Category",
                "Amount ($)",
                dataset
        );

        chart.setBackgroundPaint(Color.white);

        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(500, 300));
        return chartPanel;
    }

    public JPanel createSpendingTrendChart() {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusMonths(5).withDayOfMonth(1);

        List<Expense> recentExpenses = expenseService.getExpensesByDateRange(startDate, endDate);

        if (recentExpenses.isEmpty()) {
            dataset.addValue(0, "Total Spending", "No Data");
        } else {
            Map<String, Double> monthlyTrends = recentExpenses.stream()
                    .collect(Collectors.groupingBy(
                            expense -> expense.getDate().getMonth().toString() + " " + expense.getDate().getYear(),
                            Collectors.summingDouble(Expense::getAmount)
                    ));

            monthlyTrends.forEach((month, amount) -> {
                dataset.addValue(amount, "Total Spending", month);
            });
        }

        JFreeChart chart = ChartFactory.createLineChart(
                "Spending Trend (Last 6 Months)",
                "Month",
                "Amount ($)",
                dataset
        );

        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(500, 300));
        return chartPanel;
    }

    public JPanel createPaymentMethodChart() {
        DefaultPieDataset dataset = new DefaultPieDataset();

        List<Expense> allExpenses = expenseService.getAllExpenses();

        if (allExpenses.isEmpty()) {
            dataset.setValue("No Data Available", 1);
        } else {
            Map<String, Double> paymentSummary = allExpenses.stream()
                    .collect(Collectors.groupingBy(
                            expense -> expense.getPaymentMethod().getDisplayName(),
                            Collectors.summingDouble(Expense::getAmount)
                    ));

            paymentSummary.forEach((method, amount) -> {
                if (amount > 0) {
                    dataset.setValue(method + " ($" + String.format("%.2f", amount) + ")", amount);
                }
            });
        }

        JFreeChart chart = ChartFactory.createRingChart(
                "Spending by Payment Method",
                dataset,
                true,
                true,
                false
        );

        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(500, 300));
        return chartPanel;
    }

    // Method to refresh all charts
    public void refreshCharts(JTabbedPane chartTabs) {
        // Remove all existing tabs
        chartTabs.removeAll();

        // Recreate all chart panels
        JPanel pieChartPanel = new JPanel(new BorderLayout());
        pieChartPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        pieChartPanel.add(createCategoryPieChart(), BorderLayout.CENTER);
        chartTabs.addTab("Category Breakdown", pieChartPanel);

        JPanel barChartPanel = new JPanel(new BorderLayout());
        barChartPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        barChartPanel.add(createMonthlyBarChart(), BorderLayout.CENTER);
        chartTabs.addTab("Monthly Spending", barChartPanel);

        JPanel trendChartPanel = new JPanel(new BorderLayout());
        trendChartPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        trendChartPanel.add(createSpendingTrendChart(), BorderLayout.CENTER);
        chartTabs.addTab("Spending Trends", trendChartPanel);

        JPanel paymentChartPanel = new JPanel(new BorderLayout());
        paymentChartPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        paymentChartPanel.add(createPaymentMethodChart(), BorderLayout.CENTER);
        chartTabs.addTab("Payment Methods", paymentChartPanel);

        JPanel textAnalyticsPanel = new JPanel(new BorderLayout());
        textAnalyticsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Recreate analytics area
        JTextArea analyticsArea = new JTextArea();
        analyticsArea.setEditable(false);
        analyticsArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        updateAnalyticsText(analyticsArea);
        JScrollPane analyticsScroll = new JScrollPane(analyticsArea);
        textAnalyticsPanel.add(analyticsScroll, BorderLayout.CENTER);

        JButton refreshButton = new JButton("Refresh Analytics");
        refreshButton.addActionListener(e -> {
            updateAnalyticsText(analyticsArea);
            refreshCharts(chartTabs); // Refresh charts when button is clicked
        });
        textAnalyticsPanel.add(refreshButton, BorderLayout.SOUTH);

        chartTabs.addTab("Detailed Analytics", textAnalyticsPanel);

        // Refresh the UI
        chartTabs.revalidate();
        chartTabs.repaint();
    }

    private void updateAnalyticsText(JTextArea analyticsArea) {
        StringBuilder analytics = new StringBuilder();

        analytics.append("=== SPENDING BY CATEGORY ===\n\n");
        Map<String, Double> categorySummary = expenseService.getCategorySummary();

        if (categorySummary.isEmpty()) {
            analytics.append("No expenses recorded yet.\n");
        } else {
            categorySummary.entrySet().stream()
                    .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                    .forEach(entry -> {
                        analytics.append(String.format("%-20s: $%10.2f\n",
                                entry.getKey(), entry.getValue()));
                    });
        }

        analytics.append("\n=== THIS MONTH'S SPENDING ===\n\n");
        LocalDate now = LocalDate.now();
        Map<String, Double> monthlySummary = expenseService.getMonthlySummary(now.getYear(), now.getMonthValue());

        if (monthlySummary.isEmpty()) {
            analytics.append("No expenses this month.\n");
        } else {
            monthlySummary.entrySet().stream()
                    .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                    .forEach(entry -> {
                        analytics.append(String.format("%-20s: $%10.2f\n",
                                entry.getKey(), entry.getValue()));
                    });

            double monthlyTotal = monthlySummary.values().stream().mapToDouble(Double::doubleValue).sum();
            analytics.append(String.format("\n%-20s: $%10.2f\n", "MONTHLY TOTAL", monthlyTotal));
        }

        double overallTotal = expenseService.getTotalSpent();
        analytics.append(String.format("\n%-20s: $%10.2f\n", "OVERALL TOTAL", overallTotal));

        analyticsArea.setText(analytics.toString());
    }
}