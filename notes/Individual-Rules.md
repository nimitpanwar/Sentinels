# Individual Risk Logic

The Risk Engine evaluates each transaction using multiple individual risk checks.  
Each rule produces a risk score between **0 and 1**, which contributes to the final transaction risk score.

---

# 1. Amount Anomaly

## Purpose

Detect transactions where the amount is unusual compared to the customer's historical spending behaviour.

---

## Inputs

- Account ID
- Current transaction amount
- Historical transaction amounts (e.g., previous 90 days)
- Mean transaction amount
- Standard deviation of transaction amounts

---

## Methodology

1. Retrieve the customer's historical transaction data.
2. Calculate the average transaction amount (mean).
3. Calculate the standard deviation of historical transaction amounts.
4. Calculate the Z-score:

```
Z = (Current Amount - Mean) / Standard Deviation
```

5. Convert the Z-score into a normalized risk score between **0 and 1**.

A higher deviation from normal behaviour results in a higher risk score.

---

## Example

```
Current Amount = $8,000

Historical Mean = $250

Standard Deviation = $400

Z-score = (8000 - 250) / 400

Z-score = 19.4

Risk Score = High
```

---

# 2. Velocity Anomaly

## Purpose

Detect unusual increases in transaction frequency within a short time period.

---

## Inputs

- Account ID
- Current transaction timestamp
- Configured time window (e.g., 5 or 10 minutes)
- Number of transactions within the time window
- Historical average transaction frequency

---

## Methodology

1. Count the customer's recent transactions within the configured time window.
2. Compare the current transaction frequency against normal behaviour.
3. If transactions exceed the configured threshold, increase the risk score.
4. Larger deviations produce higher risk.

---

## Example

```
Time Window = 10 minutes

Current Transactions = 12

Historical Average = 2

Risk Score = High
```

---

# 3. New Payee Detection

## Purpose

Detect payments to payees that the customer has never interacted with before.

---

## Inputs

- Account ID
- Payee ID
- Historical payees for the account
- Previous transaction count with the payee

---

## Methodology

1. Search previous transactions for the same account and payee.
2. Check whether the customer has previously interacted with this payee.
3. If no previous transaction exists, classify the payee as new.
4. Assign a configurable risk score.
5. Increase risk when combined with other suspicious signals, such as:
   - Large transaction amount
   - High transaction velocity

---

## Example

```
Account = ACC001

Payee = PAY999

Previous Transactions = 0

Result = New Payee

Risk Score = Medium
```

---

# 4. Behaviour Changes

Behaviour analysis detects unusual changes in normal customer activity.

This consists of several independent checks.

---

# 4.1 Transaction Time Anomaly

## Purpose

Detect transactions occurring at unusual times compared to the customer's normal behaviour.

---

## Inputs

- Current transaction time
- Historical transaction times
- Customer's normal transaction hours

---

## Methodology

1. Build a profile of the customer's usual transaction times.
2. Compare the current transaction time against historical behaviour.
3. Increase risk if the transaction occurs outside the normal pattern.

---

## Example

```
Normal Hours:

08:00 - 20:00

Current Transaction:

03:15

Risk = Increased
```

---

# 4.2 Device Change Detection

## Purpose

Detect transactions made from unknown or unusual devices.

---

## Inputs

- Current device identifier
- Previously used devices

---

## Methodology

1. Compare the current device against known customer devices.
2. If the device has not been seen before, increase the risk score.

---

## Example

```
Known Devices:

- Phone A
- Laptop B

Current Device:

Unknown Device

Risk = Increased
```

---

# 4.3 Location Change Detection

## Purpose

Detect transactions occurring from unusual locations.

---

## Inputs

- Current transaction location (IP, GPS, or country)
- Historical transaction locations

---

## Methodology

1. Compare the current location against previous customer locations.
2. Detect unusual location changes.
3. Increase risk when location behaviour differs significantly from normal activity.

---

## Example

```
Previous Location:

Dublin

Current Location:

Singapore

Risk = High
```

---

# 4.4 Spending Pattern Change

## Purpose

Detect changes in normal customer spending behaviour.

---

## Inputs

- Merchant category
- Historical spending categories
- Typical transaction amounts

---

## Methodology

1. Build a customer spending profile from historical transactions.
2. Compare the current transaction against normal spending patterns.
3. Increase risk when:
   - Merchant category is unusual.
   - Transaction amount differs significantly from normal behaviour.
   - Spending behaviour changes suddenly.

---

## Example

```
Normal Spending:

- Groceries
- Fuel
- Restaurants

Current Transaction:

Cryptocurrency Exchange

Risk = Medium
```

---

# Risk Score Calculation

Each rule generates an individual risk score between **0 and 1**.

Example:

| Rule | Risk Score |
|------|------------|
| Amount Anomaly | 0.85 |
| Velocity Anomaly | 0.70 |
| New Payee | 1.00 |
| Behaviour Changes | 0.65 |

The Risk Engine combines these individual scores to generate a final transaction risk score:

```
Final Risk Score = 0 - 100
```

Example:

```
Amount Anomaly       0.85
Velocity Anomaly     0.70
New Payee            1.00
Behaviour Changes    0.65

Final Risk Score     86/100
```

---

# Risk Engine Output

The Risk Engine returns:

- Final risk score
- Triggered rules
- Individual rule contributions
- Evidence explaining the decision

Example:

```
Risk Score: 86

Triggered Rules:

- Amount Anomaly
  Reason: Transaction amount was significantly above normal behaviour

- Velocity Anomaly
  Reason: 12 transactions detected within 10 minutes

- New Payee
  Reason: First transaction to this payee
```

The result is then passed to the **Alert Manager**, which decides whether to:

- Create a new alert
- Merge with an existing alert
- Update an existing alert


# Edge Cases / Must have

1. Alert aggregation
Every transaction gets a risk score, but alerts are grouped into a single case.
if(existing) update case
else create case

2. Rule Configuration

The system provides a set of predefined fraud detection rules with sensible default values. Authorized users (such as fraud analysts or administrators) can modify these rules through the administration interface without changing the application code.

Users can configure settings such as:
Enable or disable a rule.
Detection thresholds (e.g., Z-score, transaction count).
Lookback periods (e.g., 30, 60, or 90 days).
Rule weights or risk contributions.
Other rule-specific parameters.

3. Explainable Risk Score

Every alert should explain:

Which rules triggered
Rule contributions
Supporting evidence

4. Graceful Handling of Missing Data

Exception handling, incase of missing data, location missing shouldnt break the code.

5. Rule Priority

6. Separation of Risk Engine and Alert Management

The Risk Engine and Alert Manager have different responsibilities:

Risk Engine: Evaluates each transaction against the configured rules and produces a risk score along with the rules that were triggered.
Alert Manager: Uses the risk score to decide what action to take, such as creating a new alert,
merging it with an existing alert, updating an existing alert, or ignoring it if no alert is required.


7. Case Lifecycle
Define states (Open, In Review, Escalated, Closed)
