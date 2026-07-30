## Individual Risk Logic

1. **Amount Anomaly**

* Compare transaction amount with the account's historical behaviour.
* Use statistical deviation (Z-score) to measure how unusual the amount is.
* Higher deviation = higher risk.

2. **Velocity Anomaly**

* Check transaction frequency within a time window.
* If transactions suddenly increase beyond normal behaviour, increase risk.

3. **New Payee Detection**

* Check if the account has interacted with the payee before.
* New relationships increase risk, especially when combined with large amounts or high velocity.

5. **Behaviour Changes**

* Detect unusual changes in:

  * transaction time,
  * device,
  * location,
  * normal spending patterns.

---

## Graph Network Risk Logic

1. **Payee Concentration**

* Count how many accounts are connected to the same payee.
* A payee receiving money from an unusually large number of accounts increases risk.

2. **Shared Payee Detection**

* Detect when multiple unrelated accounts send money to the same entity.
* Shared suspicious relationships increase risk.

3. **Relationship Growth**

* Track how quickly an account creates new connections.
* A sudden increase in new payees/accounts is suspicious.

4. **Transaction Cluster Detection**

* Identify groups of similar transactions:

  * same account,
  * similar amounts,
  * short time period,
  * same destination.
* Detects patterns like transaction splitting.

5. **Circular Transaction Detection**

* Detect money movement loops such as:

  * Account A → B → C → A.
* Cycles can indicate suspicious activity.

6. **Network Exposure**

* Check whether an account is connected to already suspicious entities.
* Closer connections increase risk.

---

## Risk Calculation

* Each rule produces a risk score (0-1).
* Combine individual and graph scores.
* Generate final risk score (0-100).

Example:

```
Amount anomaly       0.85
Velocity anomaly     0.90
New payee            1.00
Shared payee         0.80
Transaction cluster  0.75
```

Final score:

```
Risk Score = 86/100
```

---

## Explainability

Every alert stores:

* Which rules triggered.
* How much each rule contributed.
* Evidence behind the decision.

Example:

> "Alert triggered because the transaction amount was 8 standard deviations above normal, the account made 12 transactions in 5 minutes, and the payee was connected to 40 other accounts."

This gives both **detection** and **reasoning**, reducing false positives compared to simple rule-based alerts.
