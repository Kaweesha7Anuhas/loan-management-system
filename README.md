# 🏦 Loan Management System (LMS)

A complete, full-stack Loan Management System built for a fictional finance company. This application manages the entire loan lifecycle from customer registration to loan disbursement and final repayment collection.

Built with **Java 17**, **Spring Boot 3.x**, **MySQL**, and a modern **Bootstrap 5 + jQuery** frontend.

## 🚀 Key Features
*   **Role-Based Access Control (RBAC):** Strict separation of duties between Admin, Loan Officer, and Cashier.
*   **Customer Management:** Full CRUD with NIC validation and advanced search/pagination.
*   **Loan Products:** Admin-managed interest rates and loan terms.
*   **State Machine Workflow:** Applications transition securely through `DRAFT → SUBMITTED → UNDER_REVIEW → RECOMMENDED → APPROVED/REJECTED`.
*   **Financial Engine:** Live EMI calculation using `BigDecimal` for exact precision.
*   **Repayment Engine:** Automated schedule generation with rounding correction on the final instalment.
*   **FIFO Payment Allocation:** Cashiers can record lump-sum or partial payments, and the system automatically allocates funds to the oldest unpaid instalments first.
*   **Audit Logging:** Append-only security logs tracking every critical financial action.

## 🛠️ Technology Stack
*   **Backend:** Java 17, Spring Boot 3.x, Spring Security, Spring Data JPA (Hibernate)
*   **Database:** MySQL 8.x
*   **Frontend:** HTML5, CSS3, Bootstrap 5, JavaScript, jQuery, AJAX
*   **Testing:** JUnit 5 (EMI Calculation unit tests)

## 📂 Architecture
The application follows a classic layered architecture:
*   **Controllers:** Thin REST endpoints returning DTOs wrapped in an `ApiResponse`.
*   **Services:** Core business logic, state machine transitions, and transaction management (`@Transactional`).
*   **Repositories:** Spring Data JPA interfaces.
*   **Security:** Session-based authentication, CSRF protection (XSRF-TOKEN cookie), and method-level authorization (`@PreAuthorize`).

## ⚙️ How to Run Locally

### 1. Database Setup
Create a MySQL database:
```sql
CREATE DATABASE loan_management_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

## 🧪 End-to-End Testing
This application has been thoroughly tested for the complete loan lifecycle, Role-Based Access Control (RBAC), state machine transitions, and financial edge cases. 
👉 **[View the complete UAT Testing Checklist here](TESTING.md)**