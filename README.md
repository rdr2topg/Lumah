# Lumah
☕ Lumah Cafe POS & Management System

Lumah Cafe POS is a lightweight, single-file Java Swing desktop application designed for coffee shop point-of-sale operations, recipe-driven inventory tracking, staff commission calculations, cash drawer controls, and comprehensive sales reporting.

🌟 Key Features

Tap-Based POS Register: Touch-friendly ordering interface with customizable drinks (Hot/Iced), add-ons, and automated milk substitutions (e.g., Oatside oat milk).

Role-Based Access Control:

Admin: Full access protected by secure SHA-256 salted password authentication. Password for ADMIN as of now is (6900)

Barista: Access restricted to POS ordering and order history.

Recipe-Driven Inventory Management: Tracks real-time ingredient levels (espresso shots, syrups, powders, cups) and automatically adjusts stock when orders are placed or voided.

Staff Payout & Commission Calculator: Calculates staff commission earnings based on a 7-working-day cycle (Mon–Thu schedule with flexible date adjustments) and configurable per-category rates.

Cash Drawer & Expense Tracking: Manage starting floats, cash drops, payouts/expenses, and end-of-day drawer reconciliations with automated variance calculation.

Analytics & Audit Logging: Features comprehensive sales reports (best-selling items, add-on breakdown, revenue metrics), low-stock restocking alerts, and complete admin audit logs.

Cross-System Data Sync: Secure encrypted clipboard payload transfer for easy database backup and restoration across multiple computers.

Dynamic Theme Engine: Instant toggle between Light Mode and Dark Mode.

Zero External Dependencies: Built using standard Java Swing/AWT components with local CSV data storage in ./data.

🛠️ Requirements

Java Development Kit (JDK): Version 8 or higher.

Operating System: Windows, macOS, or Linux.

🚀 Quick Start

1. Compilation

Compile the single source file using the Java Compiler:

javac Lumah.java


2. Execution

Run the compiled application:

java Lumah


Note: On the first run, the system will automatically create the ./data directory and seed initial menu items, ingredients, and configuration settings.

🔐 Default Credentials

Role: Admin

Default Password: 6900

(Note: Admin credentials can be updated inside the app via Top Bar → Change Password)

📂 Data Storage

All system data is stored locally in human-readable CSV files within the ./data directory:

File

Description

menu.csv

Menu items, categories, prices, and recipe compositions

stock.csv

Ingredient stock levels, units, and low-stock alert thresholds

orders.csv

Transaction history, line items, payment methods, and statuses

drawer.csv

Cash drawer entries (floats, drops, payouts, counts)

payout_history.csv

Recorded staff commission payout archives

audit.csv

Historical administrative action log

config.properties

System settings, commission rates, and hashed admin security credentials

💻 Tech Stack

Language: Java (JDK 8+)

UI Framework: Standard Java Swing & AWT

Data Persistence: Custom CSV Engine / Properties File

Security: SHA-256 with SecureRandom Salting
