# Blood Bank Management System (BBMS)

A DBMS course project: an Oracle database connected to a Java (JDBC) desktop application for managing blood donors, patients, blood tests and blood requests.

## Project Information

- **Student:** Mudassir
- **Program:** BS Software Engineering
- **Course:** Database Management Systems (DBMS)
- **Instructor:** Ms Sana Yousaf

## Technologies Used

- **Database:** Oracle (SQL and PL/SQL)
- **Frontend:** Java (Swing)
- **Connectivity:** JDBC
- **IDE:** IntelliJ IDEA, Oracle SQL Developer

## Repository Structure

```
DBMS PROJECT/
├── BBMS/                      Java frontend project (IntelliJ)
│   ├── src/                   Source code
│   │   ├── DBConnection.java  Database connection
│   │   ├── UserDAO.java
│   │   ├── DonorDAO.java
│   │   ├── PatientDAO.java
│   │   ├── EssentialTestDAO.java
│   │   ├── BloodTestingDAO.java
│   │   └── BloodRequestDAO.java
│   ├── login_bg.jpg
│   └── register_bg.jpg
├── BBMS.sql                   Database: tables, constraints, sample data
├── ALL_QUERIES_COMBINED.sql   All queries, views, index, sequence, PL/SQL
└── Project report.pdf         Full project report
```

## Main Features

- User login and registration
- Add, delete, update and search for donors
- Add, delete, update and search for blood requests
- Patient, essential test and blood testing records
- Donation workflow: Essential Test, Donation, Blood Testing, Blood Record, Blood Inventory
- Error handling in the application

## Database Contents

- Tables with primary key and foreign key constraints, with sample data
- Simple, join and subquery SELECT queries
- Views, index and sequence
- PL/SQL procedure, function, trigger and cursor

## How to Run

1. Open **Oracle SQL Developer** and connect to your Oracle database.
2. Run `BBMS.sql` to create the tables and insert the sample data.
3. Run `ALL_QUERIES_COMBINED.sql` to create the queries, views and PL/SQL objects.
4. Open the `BBMS` folder in **IntelliJ IDEA**.
5. Add the Oracle JDBC driver (`ojdbc` jar) to the project libraries.
6. Open `src/DBConnection.java` and enter your own database URL, username and password.
7. Run the main class of the application.

## Note

The database username and password are not shared in this repository. Please enter your own details in `DBConnection.java` before running.
