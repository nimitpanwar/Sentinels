customers 
customer_id - PK 
first_name - VARCHAR 
last_name - VARCHAR 
email - VARCHAR 
phone - VARCHAR 
address - VARCHAR/TEXT 
created_at - TIMESTAMP

accounts
 account_id - PK 
customer_id - FK -> customers.customer_id 
account_number - VARCHAR 
account_type - VARCHAR/ENUM 
currency - VARCHAR(3) s
status - ENUM (ACTIVE/CLOSED/FROZEN) 
opened_at - TIMESTAMP

payees 
payee_id - PK 
payee_name - VARCHAR 
payee_identifier - VARCHAR


transactions
 transaction_id - PK 
 account_id - FK -> accounts.account_id 
 payee_id - FK -> payees.payee_id 
 amount - DECIMAL currency - VARCHAR(3) 
 type - ENUM (DEBIT/CREDIT) 
 status - ENUM (PENDING/COMPLETED/FAILED) 
 description - VARCHAR merchant_category - VARCHAR 
 location - VARCHAR 
 transaction_timestamp - TIMESTAMP 
 created_at - TIMESTAMP 


transaction_queue_status 
id - PK transaction_id - FK -> transactions.transaction_id 
queue_status - ENUM (PENDING/PROCESSING/EVALUATED/FAILED) 
picked_up_at - TIMESTAMP 
evaluated_at - TIMESTAMP 
retry_count - INT


rule_evaluations 
evaluation_id - PK 
transaction_id - FK -> transactions.transaction_id 
rule_id - FK -> rules.rule_id 
risk_score - DECIMAL 
evaluated_at - TIMESTAMP
triggered - BOOLEAN

rules 
rule_id - PK 
rule_name - VARCHAR 
rule_type - ENUM (AMOUNT_ANOMALY/VELOCITY/NEW_PAYEE/TIME_ANOMALY/DEVICE_CHANGE/LOCATION_CHANGE/SPENDING_PATTERN) 
is_active - BOOLEAN  
weight - DECIMAL 

alerts
alert_id - PK
transaction_id - FK -> transactions.transaction_id   
risk_score - DECIMAL
severity - ENUM (HIGH/MID/LOW)
status - ENUM (OPEN/IN_REVIEW/ESCALATED/CLOSED)
created_at - TIMESTAMP
closed_at - TIMESTAMP
resolution_notes - TEXT

cases 
case_id - PK 
risk_score - DECIMAL 
severity - ENUM (HIGH/MID/LOW) 
status - ENUM (OPEN/IN_REVIEW/ESCALATED/CLOSED) 
created_at - TIMESTAMP 
closed_at - TIMESTAMP 
resolution_notes - TEXT




TO BE APPENDED

alerts -> case_id
cases -> account_id
rule_evaluations -> reason
rules -> threshold_value
rules -> timeline

