-- ============================================================
-- LIFELINE BBMS — ALL SQL, COMBINED IN RUN ORDER (FINAL VERSION)
-- All later fixes are already built into the CREATE TABLE
-- statements directly — you do NOT need any separate patch files.
-- Just run this one file top to bottom (F5 in SQL Developer).
-- ============================================================


-- ============================================================
-- FILE: 01_create_tables.sql
-- ============================================================
-- ============================================================
-- BLOOD BANK MANAGEMENT SYSTEM (BBMS)
-- 01_create_tables.sql  —  FINAL VERSION
-- Run this first. Drops old tables (if any) then rebuilds all 9.
-- All later fixes (Pending statuses, Essential Test Quantity column)
-- are already built in here — no separate patch files needed anymore.
-- ============================================================

DROP TABLE BLOOD_INVENTORY CASCADE CONSTRAINTS;
DROP TABLE BLOOD_RECORDS CASCADE CONSTRAINTS;
DROP TABLE BLOOD_TESTING CASCADE CONSTRAINTS;
DROP TABLE ESSENTIAL_TESTS CASCADE CONSTRAINTS;
DROP TABLE BLOOD_REQUESTS CASCADE CONSTRAINTS;
DROP TABLE PATIENTS CASCADE CONSTRAINTS;
DROP TABLE DONORS CASCADE CONSTRAINTS;
DROP TABLE ADMINS CASCADE CONSTRAINTS;
DROP TABLE USERS CASCADE CONSTRAINTS;

-- 1. USERS  (base entity — Admin/Donor/Patient all ISA a User)
CREATE TABLE USERS (
    UserID      VARCHAR2(10)  PRIMARY KEY,
    Username    VARCHAR2(50)  NOT NULL UNIQUE,
    Email       VARCHAR2(100) NOT NULL UNIQUE,
    Password    VARCHAR2(50)  NOT NULL,
    Role        VARCHAR2(10)  NOT NULL,
    CONSTRAINT chk_user_role CHECK (Role IN ('Admin','Donor','Patient'))
);

-- 2. ADMINS
CREATE TABLE ADMINS (
    AdminID     VARCHAR2(10) PRIMARY KEY,
    UserID      VARCHAR2(10) NOT NULL UNIQUE,
    Name        VARCHAR2(50) NOT NULL,
    Phone       VARCHAR2(15),
    CONSTRAINT fk_admin_user FOREIGN KEY (UserID) REFERENCES USERS(UserID)
);

-- 3. DONORS
CREATE TABLE DONORS (
    DonorID     VARCHAR2(10) PRIMARY KEY,
    UserID      VARCHAR2(10) NOT NULL UNIQUE,
    Name        VARCHAR2(50) NOT NULL,
    CNIC        VARCHAR2(15) NOT NULL UNIQUE,
    Gender      VARCHAR2(10),
    Age         NUMBER(3),
    Phone       VARCHAR2(15),
    Address     VARCHAR2(100),
    DOB         DATE,
    Bloodgroup  VARCHAR2(3),
    CONSTRAINT fk_donor_user FOREIGN KEY (UserID) REFERENCES USERS(UserID),
    CONSTRAINT chk_donor_gender CHECK (Gender IN ('Male','Female','Other')),
    CONSTRAINT chk_donor_age CHECK (Age BETWEEN 18 AND 65),
    CONSTRAINT chk_donor_group CHECK (Bloodgroup IN ('A+','A-','B+','B-','AB+','AB-','O+','O-'))
);

-- 4. PATIENTS
CREATE TABLE PATIENTS (
    PatientID   VARCHAR2(10) PRIMARY KEY,
    UserID      VARCHAR2(10) NOT NULL UNIQUE,
    Name        VARCHAR2(50) NOT NULL,
    CNIC        VARCHAR2(15) NOT NULL UNIQUE,
    Gender      VARCHAR2(10),
    Bloodgroup  VARCHAR2(3),
    Phone       VARCHAR2(15),
    Address     VARCHAR2(100),
    CONSTRAINT fk_patient_user FOREIGN KEY (UserID) REFERENCES USERS(UserID),
    CONSTRAINT chk_patient_gender CHECK (Gender IN ('Male','Female','Other')),
    CONSTRAINT chk_patient_group CHECK (Bloodgroup IN ('A+','A-','B+','B-','AB+','AB-','O+','O-'))
);

-- 5. BLOOD_REQUESTS  (Patient Makes a request; Admin Approves it)
-- NOTE: the ERD's "Requests (specific group)" weak entity is merged in here
-- as RequiredGroup + RequestDate, since it has no independent existence.
CREATE TABLE BLOOD_REQUESTS (
    RequestID     VARCHAR2(10) PRIMARY KEY,
    PatientID     VARCHAR2(10) NOT NULL,
    AdminID       VARCHAR2(10),
    Status        VARCHAR2(10) DEFAULT 'Pending',
    RequestDate   DATE DEFAULT SYSDATE,
    RequiredGroup VARCHAR2(3),
    Quantity      NUMBER(3),
    CONSTRAINT fk_request_patient FOREIGN KEY (PatientID) REFERENCES PATIENTS(PatientID),
    CONSTRAINT fk_request_admin   FOREIGN KEY (AdminID)   REFERENCES ADMINS(AdminID),
    CONSTRAINT chk_request_status CHECK (Status IN ('Pending','Approved','Rejected','Fulfilled')),
    CONSTRAINT chk_request_group  CHECK (RequiredGroup IN ('A+','A-','B+','B-','AB+','AB-','O+','O-')),
    CONSTRAINT chk_request_qty    CHECK (Quantity > 0)
);

-- 6. ESSENTIAL_TESTS  (Donor is Screened)
-- Quantity is an addition beyond the original ERD: units of blood actually
-- drawn, entered once here when the donor is Eligible, then reused
-- automatically by Blood Record and Blood Inventory later in the pipeline.
CREATE TABLE ESSENTIAL_TESTS (
    ScreeningID       VARCHAR2(10) PRIMARY KEY,
    DonorID           VARCHAR2(10) NOT NULL,
    Hemoglobin        NUMBER(4,1),
    Pulse             NUMBER(3),
    Weights           NUMBER(5,1),
    ScreeningDate     DATE DEFAULT SYSDATE,
    BloodPressure     VARCHAR2(10),
    EligibilityStatus VARCHAR2(10),
    Quantity          NUMBER(3) DEFAULT 0,
    CONSTRAINT fk_screen_donor FOREIGN KEY (DonorID) REFERENCES DONORS(DonorID),
    CONSTRAINT chk_screen_elig CHECK (EligibilityStatus IN ('Eligible','Ineligible','Pending'))
);

-- 7. BLOOD_TESTING  (only Qualified/Eligible screenings reach here)
-- Result allows 'Pending' so a row can sit here the instant a screening
-- turns Eligible, before the lab results are actually typed in.
CREATE TABLE BLOOD_TESTING (
    TestID       VARCHAR2(10) PRIMARY KEY,
    ScreeningID  VARCHAR2(10) NOT NULL UNIQUE,
    TestDate     DATE DEFAULT SYSDATE,
    HIVTest      VARCHAR2(10),
    HepatitisB   VARCHAR2(10),
    HepatitisC   VARCHAR2(10),
    Syphilis     VARCHAR2(10),
    Result       VARCHAR2(10),
    CONSTRAINT fk_test_screening FOREIGN KEY (ScreeningID) REFERENCES ESSENTIAL_TESTS(ScreeningID),
    CONSTRAINT chk_test_result CHECK (Result IN ('Pass','Fail','Pending'))
);

-- 8. BLOOD_RECORDS  (Generated from a Pass result)
-- Quantity allows 0 so a record can be auto-created the instant a test
-- Passes, with the real number filled in moments later from Essential Test.
CREATE TABLE BLOOD_RECORDS (
    DonationID    VARCHAR2(10) PRIMARY KEY,
    TestID        VARCHAR2(10) NOT NULL,
    DonationDate  DATE DEFAULT SYSDATE,
    Bloodgroup    VARCHAR2(3),
    Quantity      NUMBER(3),
    CONSTRAINT fk_record_test FOREIGN KEY (TestID) REFERENCES BLOOD_TESTING(TestID),
    CONSTRAINT chk_record_qty CHECK (Quantity >= 0)
);

-- 9. BLOOD_INVENTORY  (Updated by new records; Managed by Admin; Fulfils requests)
-- Status allows 'Pending' for the same auto-queue reason as above.
CREATE TABLE BLOOD_INVENTORY (
    InventoryID    VARCHAR2(10) PRIMARY KEY,
    DonationID     VARCHAR2(10) NOT NULL UNIQUE,
    AdminID        VARCHAR2(10) NOT NULL,
    Status         VARCHAR2(10) DEFAULT 'Available',
    CollectionDate DATE,
    ExpiryDate     DATE,
    Quantity       NUMBER(3),
    CONSTRAINT fk_inv_record FOREIGN KEY (DonationID) REFERENCES BLOOD_RECORDS(DonationID),
    CONSTRAINT fk_inv_admin  FOREIGN KEY (AdminID)    REFERENCES ADMINS(AdminID),
    CONSTRAINT chk_inv_status CHECK (Status IN ('Available','Reserved','Expired','Pending')),
    CONSTRAINT chk_inv_qty    CHECK (Quantity >= 0)
);

COMMIT;


-- ============================================================
-- FILE: 02_insert_data.sql
-- ============================================================
-- ============================================================
-- BLOOD BANK MANAGEMENT SYSTEM (BBMS)
-- 02_insert_data.sql
-- Run this after 01_create_tables.sql. Order matters (parents first).
-- ============================================================

-- 1. USERS (14 rows: 2 Admin, 6 Donor, 6 Patient)
INSERT INTO USERS VALUES ('U01','hina.admin','hina@lifeline.pk','admin123','Admin');
INSERT INTO USERS VALUES ('U02','usman.admin','usman@lifeline.pk','admin123','Admin');
INSERT INTO USERS VALUES ('U03','bilal.khan','bilal@lifeline.pk','pass123','Donor');
INSERT INTO USERS VALUES ('U04','omar.raza','omar@lifeline.pk','pass123','Donor');
INSERT INTO USERS VALUES ('U05','ahmed.ali','ahmed@lifeline.pk','pass123','Donor');
INSERT INTO USERS VALUES ('U06','zain.malik','zain@lifeline.pk','pass123','Donor');
INSERT INTO USERS VALUES ('U07','hamza.tariq','hamza@lifeline.pk','pass123','Donor');
INSERT INTO USERS VALUES ('U08','saad.iqbal','saad@lifeline.pk','pass123','Donor');
INSERT INTO USERS VALUES ('U09','sara.ahmed','sara@lifeline.pk','pass123','Patient');
INSERT INTO USERS VALUES ('U10','ayesha.noor','ayesha@lifeline.pk','pass123','Patient');
INSERT INTO USERS VALUES ('U11','fatima.zahra','fatima@lifeline.pk','pass123','Patient');
INSERT INTO USERS VALUES ('U12','usama.sheikh','usama@lifeline.pk','pass123','Patient');
INSERT INTO USERS VALUES ('U13','maryam.khan','maryam@lifeline.pk','pass123','Patient');
INSERT INTO USERS VALUES ('U14','ali.raza','ali@lifeline.pk','pass123','Patient');

-- 2. ADMINS (2 rows)
INSERT INTO ADMINS VALUES ('A01','U01','Hina Farooq','03012223344');
INSERT INTO ADMINS VALUES ('A02','U02','Usman Tariq','03219988776');

-- 3. DONORS (6 rows)
INSERT INTO DONORS VALUES ('D01','U03','Bilal Khan','3740511223345','Male',27,'03001112233','G-9 Islamabad',TO_DATE('1999-03-14','YYYY-MM-DD'),'O+');
INSERT INTO DONORS VALUES ('D02','U04','Omar Raza','3740555443321','Male',31,'03339988776','F-10 Islamabad',TO_DATE('1995-07-02','YYYY-MM-DD'),'B-');
INSERT INTO DONORS VALUES ('D03','U05','Ahmed Ali','3740512345678','Male',24,'03451234567','I-8 Islamabad',TO_DATE('2002-01-20','YYYY-MM-DD'),'A+');
INSERT INTO DONORS VALUES ('D04','U06','Zain Malik','3740587654321','Male',29,'03007654321','G-11 Islamabad',TO_DATE('1997-11-05','YYYY-MM-DD'),'AB+');
INSERT INTO DONORS VALUES ('D05','U07','Hamza Tariq','3740598765432','Male',33,'03211234567','E-11 Islamabad',TO_DATE('1993-06-18','YYYY-MM-DD'),'O-');
INSERT INTO DONORS VALUES ('D06','U08','Saad Iqbal','3740511122233','Male',26,'03339876543','F-7 Islamabad',TO_DATE('2000-09-09','YYYY-MM-DD'),'A-');

-- 4. PATIENTS (6 rows)
INSERT INTO PATIENTS VALUES ('P01','U09','Sara Ahmed','3740577889902','Female','A+','03456677889','DHA Phase 2 Rawalpindi');
INSERT INTO PATIENTS VALUES ('P02','U10','Ayesha Noor','3740512121212','Female','O+','03007712345','Bahria Town Islamabad');
INSERT INTO PATIENTS VALUES ('P03','U11','Fatima Zahra','3740534343434','Female','B-','03219988112','Satellite Town Rawalpindi');
INSERT INTO PATIENTS VALUES ('P04','U12','Usama Sheikh','3740556565656','Male','AB+','03334455667','PWD Islamabad');
INSERT INTO PATIENTS VALUES ('P05','U13','Maryam Khan','3740578787878','Female','A+','03451122334','G-13 Islamabad');
INSERT INTO PATIENTS VALUES ('P06','U14','Ali Raza','3740590909090','Male','O+','03007788990','Chaklala Rawalpindi');

-- 5. ESSENTIAL_TESTS (12 rows: 10 Eligible, 2 Ineligible)
INSERT INTO ESSENTIAL_TESTS VALUES ('SC01','D01',14.2,72,68,TO_DATE('2026-07-29','YYYY-MM-DD'),'118/76','Eligible',1);
INSERT INTO ESSENTIAL_TESTS VALUES ('SC02','D02',13.5,75,72,TO_DATE('2026-07-30','YYYY-MM-DD'),'120/78','Eligible',1);
INSERT INTO ESSENTIAL_TESTS VALUES ('SC03','D03',14.8,70,65,TO_DATE('2026-08-01','YYYY-MM-DD'),'115/75','Eligible',1);
INSERT INTO ESSENTIAL_TESTS VALUES ('SC04','D04',13.9,68,80,TO_DATE('2026-08-02','YYYY-MM-DD'),'122/80','Eligible',1);
INSERT INTO ESSENTIAL_TESTS VALUES ('SC05','D05',15.1,74,77,TO_DATE('2026-08-03','YYYY-MM-DD'),'119/77','Eligible',1);
INSERT INTO ESSENTIAL_TESTS VALUES ('SC06','D06',13.2,71,63,TO_DATE('2026-08-04','YYYY-MM-DD'),'117/76','Eligible',1);
INSERT INTO ESSENTIAL_TESTS VALUES ('SC07','D01',14.0,73,69,TO_DATE('2026-08-05','YYYY-MM-DD'),'118/78','Eligible',1);
INSERT INTO ESSENTIAL_TESTS VALUES ('SC08','D03',14.5,69,66,TO_DATE('2026-08-06','YYYY-MM-DD'),'116/75','Eligible',1);
INSERT INTO ESSENTIAL_TESTS VALUES ('SC09','D05',13.8,76,78,TO_DATE('2026-08-07','YYYY-MM-DD'),'121/79','Eligible',1);
INSERT INTO ESSENTIAL_TESTS VALUES ('SC10','D06',13.6,72,64,TO_DATE('2026-08-08','YYYY-MM-DD'),'118/77','Eligible',1);
INSERT INTO ESSENTIAL_TESTS VALUES ('SC11','D02',11.8,88,59,TO_DATE('2026-08-09','YYYY-MM-DD'),'132/90','Ineligible',0);
INSERT INTO ESSENTIAL_TESTS VALUES ('SC12','D04',10.9,92,48,TO_DATE('2026-08-10','YYYY-MM-DD'),'138/92','Ineligible',0);

-- 6. BLOOD_TESTING (10 rows — only the 10 Eligible screenings SC01-SC10)
INSERT INTO BLOOD_TESTING VALUES ('T01','SC01',TO_DATE('2026-07-30','YYYY-MM-DD'),'Negative','Negative','Negative','Negative','Pass');
INSERT INTO BLOOD_TESTING VALUES ('T02','SC02',TO_DATE('2026-07-31','YYYY-MM-DD'),'Negative','Negative','Negative','Negative','Pass');
INSERT INTO BLOOD_TESTING VALUES ('T03','SC03',TO_DATE('2026-08-02','YYYY-MM-DD'),'Negative','Negative','Negative','Negative','Pass');
INSERT INTO BLOOD_TESTING VALUES ('T04','SC04',TO_DATE('2026-08-03','YYYY-MM-DD'),'Negative','Positive','Negative','Negative','Fail');
INSERT INTO BLOOD_TESTING VALUES ('T05','SC05',TO_DATE('2026-08-04','YYYY-MM-DD'),'Negative','Negative','Negative','Negative','Pass');
INSERT INTO BLOOD_TESTING VALUES ('T06','SC06',TO_DATE('2026-08-05','YYYY-MM-DD'),'Negative','Negative','Negative','Negative','Pass');
INSERT INTO BLOOD_TESTING VALUES ('T07','SC07',TO_DATE('2026-08-06','YYYY-MM-DD'),'Negative','Negative','Negative','Negative','Pass');
INSERT INTO BLOOD_TESTING VALUES ('T08','SC08',TO_DATE('2026-08-07','YYYY-MM-DD'),'Negative','Negative','Negative','Negative','Pass');
INSERT INTO BLOOD_TESTING VALUES ('T09','SC09',TO_DATE('2026-08-08','YYYY-MM-DD'),'Negative','Negative','Negative','Negative','Pass');
INSERT INTO BLOOD_TESTING VALUES ('T10','SC10',TO_DATE('2026-08-09','YYYY-MM-DD'),'Negative','Negative','Negative','Negative','Pass');

-- 7. BLOOD_RECORDS (9 rows — generated only from the Pass results: T01,T02,T03,T05,T06,T07,T08,T09,T10)
INSERT INTO BLOOD_RECORDS VALUES ('DN01','T01',TO_DATE('2026-08-01','YYYY-MM-DD'),'O+',1);
INSERT INTO BLOOD_RECORDS VALUES ('DN02','T02',TO_DATE('2026-08-01','YYYY-MM-DD'),'B-',1);
INSERT INTO BLOOD_RECORDS VALUES ('DN03','T03',TO_DATE('2026-08-03','YYYY-MM-DD'),'A+',1);
INSERT INTO BLOOD_RECORDS VALUES ('DN04','T05',TO_DATE('2026-08-05','YYYY-MM-DD'),'O-',1);
INSERT INTO BLOOD_RECORDS VALUES ('DN05','T06',TO_DATE('2026-08-06','YYYY-MM-DD'),'A-',1);
INSERT INTO BLOOD_RECORDS VALUES ('DN06','T07',TO_DATE('2026-08-07','YYYY-MM-DD'),'O+',1);
INSERT INTO BLOOD_RECORDS VALUES ('DN07','T08',TO_DATE('2026-08-08','YYYY-MM-DD'),'A+',1);
INSERT INTO BLOOD_RECORDS VALUES ('DN08','T09',TO_DATE('2026-08-09','YYYY-MM-DD'),'O-',1);
INSERT INTO BLOOD_RECORDS VALUES ('DN09','T10',TO_DATE('2026-08-10','YYYY-MM-DD'),'A-',1);

-- 8. BLOOD_INVENTORY (9 rows, one per record)
INSERT INTO BLOOD_INVENTORY VALUES ('INV01','DN01','A01','Available',TO_DATE('2026-08-01','YYYY-MM-DD'),TO_DATE('2026-09-12','YYYY-MM-DD'),1);
INSERT INTO BLOOD_INVENTORY VALUES ('INV02','DN02','A01','Reserved',TO_DATE('2026-08-01','YYYY-MM-DD'),TO_DATE('2026-09-12','YYYY-MM-DD'),1);
INSERT INTO BLOOD_INVENTORY VALUES ('INV03','DN03','A02','Available',TO_DATE('2026-08-03','YYYY-MM-DD'),TO_DATE('2026-09-14','YYYY-MM-DD'),1);
INSERT INTO BLOOD_INVENTORY VALUES ('INV04','DN04','A01','Available',TO_DATE('2026-08-05','YYYY-MM-DD'),TO_DATE('2026-09-16','YYYY-MM-DD'),1);
INSERT INTO BLOOD_INVENTORY VALUES ('INV05','DN05','A02','Available',TO_DATE('2026-08-06','YYYY-MM-DD'),TO_DATE('2026-09-17','YYYY-MM-DD'),1);
INSERT INTO BLOOD_INVENTORY VALUES ('INV06','DN06','A01','Reserved',TO_DATE('2026-08-07','YYYY-MM-DD'),TO_DATE('2026-09-18','YYYY-MM-DD'),1);
INSERT INTO BLOOD_INVENTORY VALUES ('INV07','DN07','A02','Available',TO_DATE('2026-08-08','YYYY-MM-DD'),TO_DATE('2026-09-19','YYYY-MM-DD'),1);
INSERT INTO BLOOD_INVENTORY VALUES ('INV08','DN08','A01','Expired',TO_DATE('2026-06-01','YYYY-MM-DD'),TO_DATE('2026-07-12','YYYY-MM-DD'),1);
INSERT INTO BLOOD_INVENTORY VALUES ('INV09','DN09','A02','Available',TO_DATE('2026-08-10','YYYY-MM-DD'),TO_DATE('2026-09-21','YYYY-MM-DD'),1);

-- 9. BLOOD_REQUESTS (10 rows)
INSERT INTO BLOOD_REQUESTS VALUES ('R01','P01','A01','Approved',TO_DATE('2026-08-15','YYYY-MM-DD'),'O+',1);
INSERT INTO BLOOD_REQUESTS VALUES ('R02','P02','A01','Fulfilled',TO_DATE('2026-08-10','YYYY-MM-DD'),'B-',1);
INSERT INTO BLOOD_REQUESTS VALUES ('R03','P03','NULL','Pending',TO_DATE('2026-08-20','YYYY-MM-DD'),'A+',2);
INSERT INTO BLOOD_REQUESTS VALUES ('R04','P04','A02','Approved',TO_DATE('2026-08-18','YYYY-MM-DD'),'AB+',1);
INSERT INTO BLOOD_REQUESTS VALUES ('R05','P05','NULL','Pending',TO_DATE('2026-08-22','YYYY-MM-DD'),'A+',1);
INSERT INTO BLOOD_REQUESTS VALUES ('R06','P06','A02','Rejected',TO_DATE('2026-08-12','YYYY-MM-DD'),'O+',3);
INSERT INTO BLOOD_REQUESTS VALUES ('R07','P01','A01','Fulfilled',TO_DATE('2026-08-05','YYYY-MM-DD'),'O+',1);
INSERT INTO BLOOD_REQUESTS VALUES ('R08','P03','NULL','Pending',TO_DATE('2026-08-25','YYYY-MM-DD'),'B-',1);
INSERT INTO BLOOD_REQUESTS VALUES ('R09','P02','A02','Approved',TO_DATE('2026-08-21','YYYY-MM-DD'),'O+',1);
INSERT INTO BLOOD_REQUESTS VALUES ('R10','P04','NULL','Pending',TO_DATE('2026-08-27','YYYY-MM-DD'),'AB+',1);

-- fix the two rows that used the placeholder text 'NULL' for AdminID (SQL needs the real keyword, not a string)
UPDATE BLOOD_REQUESTS SET AdminID = NULL WHERE AdminID = 'NULL';

COMMIT;


-- ============================================================
-- FILE: 03_queries.sql
-- ============================================================
-- ============================================================
-- 03_queries.sql — simple, join, subquery, and aggregate SELECTs
-- ============================================================

-- 1. Simple: all O+ donors
SELECT DonorID, Name, Bloodgroup, Phone
FROM DONORS
WHERE Bloodgroup = 'O+';

-- 2. Simple: all pending blood requests
SELECT RequestID, RequiredGroup, Quantity, RequestDate
FROM BLOOD_REQUESTS
WHERE Status = 'Pending';

-- 3. Join: patient name + request status + which admin approved it
SELECT br.RequestID, p.Name AS PatientName, br.RequiredGroup,
       br.Status, a.Name AS ApprovedBy
FROM BLOOD_REQUESTS br
JOIN PATIENTS p ON br.PatientID = p.PatientID
LEFT JOIN ADMINS a ON br.AdminID = a.AdminID;

-- 4. Multi-table join: full donation pipeline, donor to inventory
SELECT d.Name AS DonorName, et.ScreeningID, et.EligibilityStatus,
       bt.TestID, bt.Result, brc.DonationID, bi.InventoryID, bi.Status
FROM DONORS d
JOIN ESSENTIAL_TESTS et ON d.DonorID = et.DonorID
JOIN BLOOD_TESTING bt   ON et.ScreeningID = bt.ScreeningID
JOIN BLOOD_RECORDS brc  ON bt.TestID = brc.TestID
JOIN BLOOD_INVENTORY bi ON brc.DonationID = bi.DonationID;

-- 5. Subquery (NOT IN): donors who have never been screened
SELECT Name, Bloodgroup
FROM DONORS
WHERE DonorID NOT IN (SELECT DonorID FROM ESSENTIAL_TESTS);

-- 6. Subquery (nested): patients whose requested quantity is above the average
SELECT Name
FROM PATIENTS
WHERE PatientID IN (
    SELECT PatientID FROM BLOOD_REQUESTS
    WHERE Quantity > (SELECT AVG(Quantity) FROM BLOOD_REQUESTS)
);

-- 7. Aggregate + GROUP BY: available stock per blood group
SELECT brc.Bloodgroup, SUM(brc.Quantity) AS TotalUnits
FROM BLOOD_RECORDS brc
JOIN BLOOD_INVENTORY bi ON brc.DonationID = bi.DonationID
WHERE bi.Status = 'Available'
GROUP BY brc.Bloodgroup
ORDER BY TotalUnits DESC;


-- ============================================================
-- FILE: 05_plsql.sql
-- ============================================================
-- ============================================================
-- 04_views_index_sequence.sql
-- ============================================================

-- VIEW: one-shot report of the whole donation pipeline
CREATE OR REPLACE VIEW VW_DONATION_PIPELINE AS
SELECT d.DonorID, d.Name AS DonorName, et.ScreeningID, et.EligibilityStatus,
       bt.TestID, bt.Result, brc.DonationID, brc.Bloodgroup,
       bi.InventoryID, bi.Status AS StockStatus
FROM DONORS d
JOIN ESSENTIAL_TESTS et ON d.DonorID = et.DonorID
JOIN BLOOD_TESTING bt   ON et.ScreeningID = bt.ScreeningID
JOIN BLOOD_RECORDS brc  ON bt.TestID = brc.TestID
JOIN BLOOD_INVENTORY bi ON brc.DonationID = bi.DonationID;

-- test it:
-- SELECT * FROM VW_DONATION_PIPELINE;

-- INDEXES: columns you'd realistically search by
CREATE INDEX idx_donor_bloodgroup ON DONORS(Bloodgroup);
CREATE INDEX idx_patient_cnic     ON PATIENTS(CNIC);

-- SEQUENCE: auto-generate the numeric part of new RequestIDs
CREATE SEQUENCE seq_request_id START WITH 11 INCREMENT BY 1;
-- usage when inserting a new request:
-- INSERT INTO BLOOD_REQUESTS VALUES ('R' || seq_request_id.NEXTVAL, 'P01', NULL, 'Pending', SYSDATE, 'O+', 1);


-- ============================================================
-- 05_plsql.sql — Procedure, Function, Trigger, Cursor
-- ============================================================

-- PROCEDURE: approve a pending blood request
CREATE OR REPLACE PROCEDURE approve_request (
    p_requestid IN VARCHAR2,
    p_adminid   IN VARCHAR2
) IS
BEGIN
    UPDATE BLOOD_REQUESTS
    SET Status = 'Approved', AdminID = p_adminid
    WHERE RequestID = p_requestid;

    IF SQL%ROWCOUNT = 0 THEN
        RAISE_APPLICATION_ERROR(-20001, 'Request ID not found: ' || p_requestid);
    END IF;

    COMMIT;
END approve_request;
/

-- test it:
-- EXEC approve_request('R03', 'A01');
-- SELECT * FROM BLOOD_REQUESTS WHERE RequestID = 'R03';


-- FUNCTION: return a donor's most recent eligibility status
CREATE OR REPLACE FUNCTION get_eligibility (
    p_donorid IN VARCHAR2
) RETURN VARCHAR2 IS
    v_status VARCHAR2(10);
BEGIN
    SELECT EligibilityStatus INTO v_status
    FROM ESSENTIAL_TESTS
    WHERE DonorID = p_donorid
    AND ScreeningDate = (SELECT MAX(ScreeningDate) FROM ESSENTIAL_TESTS WHERE DonorID = p_donorid);

    RETURN v_status;
EXCEPTION
    WHEN NO_DATA_FOUND THEN
        RETURN 'Not Screened';
END get_eligibility;
/

-- test it:
-- SELECT get_eligibility('D01') FROM DUAL;


-- TRIGGER: auto-mark inventory as Expired if the expiry date has passed
CREATE OR REPLACE TRIGGER trg_check_inventory
BEFORE INSERT OR UPDATE ON BLOOD_INVENTORY
FOR EACH ROW
BEGIN
    IF :NEW.ExpiryDate < SYSDATE THEN
        :NEW.Status := 'Expired';
    END IF;
END;
/

-- test it:
-- INSERT INTO BLOOD_INVENTORY VALUES ('INV10','DN01','A01','Available',
--   TO_DATE('2026-01-01','YYYY-MM-DD'), TO_DATE('2026-02-01','YYYY-MM-DD'), 1);
-- SELECT Status FROM BLOOD_INVENTORY WHERE InventoryID = 'INV10';  -> shows 'Expired'


-- CURSOR: loop through and print every pending request
SET SERVEROUTPUT ON;
DECLARE
    CURSOR c_pending IS
        SELECT RequestID, RequiredGroup, Quantity
        FROM BLOOD_REQUESTS
        WHERE Status = 'Pending';
BEGIN
    FOR rec IN c_pending LOOP
        DBMS_OUTPUT.PUT_LINE('Pending Request: ' || rec.RequestID ||
                              ' | Group: ' || rec.RequiredGroup ||
                              ' | Qty: ' || rec.Quantity);
    END LOOP;
END;
/


