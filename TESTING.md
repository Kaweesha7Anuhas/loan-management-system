# 🧪 End-to-End System Testing Guide

This document contains the User Acceptance Testing (UAT) checklist for the Loan Management System. It verifies the complete lifecycle, from authentication and role-based access control to financial calculations and repayment collection.

### Prerequisites
Before running these tests, ensure the application is running locally (`./mvnw spring-boot:run`) and the database is seeded with the default users.

---

## 🔐 Test 1: Authentication & RBAC (Role-Based Access Control)
- [ ] Go to `http://localhost:8080/login.html`. Log in as **`admin` / `Admin@123`**.
- [ ] **Check the Sidebar:** Verify you can see **Dashboard, Customers, User Management, Loan Types, Applications, Active Loans, Reports, Audit Logs**.
- [ ] Click **Loan Types**. Create a new Loan Type:
  - Code: `BL`, Name: `Business Loan`, Min: `100000`, Max: `5000000`, Rate: `15.5`, Min Tenure: `12`, Max Tenure: `60`.
  - *Expected:* The new loan type appears in the table immediately.
- [ ] Click the user dropdown (top right) and **Logout**.
- [ ] Log in as **`officer1` / `Officer@123`**.
- [ ] **Check the Sidebar:** Verify you can see **Dashboard, Customers, Applications, Active Loans**.
  - *Expected:* `User Management`, `Loan Types`, `Reports`, and `Audit Logs` are missing. (Proves frontend RBAC is working).

## 👥 Test 2: Customer Registration (Loan Officer)
- [ ] (Still logged in as Officer) Click **Customers**.
- [ ] Click **New Customer**. Enter a unique NIC (e.g., `200012345678`) and Email (e.g., `test@example.com`). Fill out the rest and click **Save**.
  - *Expected:* A success alert appears, and the customer is added to the table.
- [ ] Use the search bar to search by `test@example.com` or the NIC.
  - *Expected:* The table filters down to the matching customer.
- [ ] Clear the search. Click the **Pencil (Edit)** icon. Update the phone number and Save.
  - *Expected:* The table updates immediately.

## 📝 Test 3: The State Machine (Loan Officer)
- [ ] Click **Applications** in the sidebar, then **New Application**.
- [ ] Select your test Customer, select **Business Loan**, enter Amount: `200000`, Tenure: `24`.
- [ ] *Watch the Magic:* As you type, the **Live EMI Calculation** box updates in real-time.
- [ ] Click **Submit Application**. 
  - *Expected:* The application appears in the table as **DRAFT**.
- [ ] Click **Submit** (Status: `SUBMITTED`).
- [ ] Click **Start Review** (Status: `UNDER_REVIEW`).
- [ ] Click **Recommend**. Enter remarks ("Good credit") and confirm (Status: `RECOMMENDED`).
  - *Expected:* Buttons disappear and it says "Awaiting Admin" (because you are the Officer).

## 💰 Test 4: Approval & Disbursement (Admin)
- [ ] Logout and log back in as **`admin` / `Admin@123`**.
- [ ] Go to **Applications** and find the `RECOMMENDED` application.
  - *Expected:* **Approve** (Green) and **Reject** (Red) buttons are visible.
- [ ] Click **Approve**. Enter remarks ("Approved by Admin") and confirm (Status: `APPROVED`).
- [ ] Click **Disburse**.
  - *Expected:* Success alert. Automatically redirected to **Active Loans**, and the new loan account is listed (e.g., `LN-2026-000002`).

## 💵 Test 5: Repayment Collection (Cashier)
- [ ] Logout and log back in as **`cashier1` / `Cashier@123`**.
- [ ] Click **Active Loans**, then **View Details** on your test loan.
  - *Expected:* Full 24-month schedule displayed with `PENDING` statuses.
- [ ] Click **Record Payment**. Enter an amount to cover 2 instalments (e.g., if EMI is 10,000, enter `20000`). Select CASH and confirm.
  - *Expected:* Success alert with a receipt number (`RCP-2026-...`). **Instalments 1 and 2 turn Green (PAID)**.
- [ ] Click **Record Payment** again. Enter a partial amount (e.g., `5000`).
  - *Expected:* **Instalment 3 turns Yellow (PARTIALLY_PAID)**.

## 📊 Test 6: Dashboards, Reports & Audit Logs (Admin)
- [ ] Logout and log back in as **`admin`**.
- [ ] Go to **Dashboard**. KPI cards should reflect the current data.
- [ ] Go to **Reports**. Portfolio totals match the loan. Daily Collection shows today's transactions.
- [ ] Go to **Audit Logs**. Verify the chronological list of actions (APPLICATION_CREATED, LOAN_APPROVED, REPAYMENT_RECORDED, etc.) and the users who performed them.

## 🛑 Test 7: Validation & Security Edge Cases
- [ ] Try to create a Customer with an invalid email (e.g., `not-an-email`).
  - *Expected:* Red error alert: "Validation failed".
- [ ] Try to create a Customer with an NIC already in use.
  - *Expected:* Red error alert: "Customer already exists with NIC...".
- [ ] While logged in as Admin, view an active loan and try to pay **more than the outstanding balance**.
  - *Expected:* Red error alert: "Payment amount exceeds the outstanding balance".