package com.example.service;

import com.example.entity.Account;
import com.example.entity.Customer;
import com.example.entity.Payee;
import com.example.entity.Rule;
import com.example.enums.AccountType;
import com.example.enums.RuleType;
import com.example.repository.AccountRepository;
import com.example.repository.CustomerRepository;
import com.example.repository.PayeeRepository;
import com.example.repository.RuleRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Seeds the DB-driven 'rules' table (so the risk engine has active rules to
 * evaluate against out of the box) plus a handful of demo customers/
 * accounts/payees (so Postman testers have ready-made IDs to reference from
 * POST /api/transactions without first creating everything by hand).
 * Runs once at startup, only if the relevant tables are empty.
 */
@Component
public class SeedDataService {

    private static final Logger log = LoggerFactory.getLogger(SeedDataService.class);

    private final RuleRepository ruleRepository;
    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;
    private final PayeeRepository payeeRepository;

    public SeedDataService(RuleRepository ruleRepository, CustomerRepository customerRepository,
                            AccountRepository accountRepository, PayeeRepository payeeRepository) {
        this.ruleRepository = ruleRepository;
        this.customerRepository = customerRepository;
        this.accountRepository = accountRepository;
        this.payeeRepository = payeeRepository;
    }

    @PostConstruct
    public void seed() {
        seedRules();
        seedCustomersAccountsPayees();
    }

    private void seedRules() {
        if (ruleRepository.count() > 0) {
            return;
        }

        ruleRepository.save(Rule.builder()
                .ruleName("Amount Anomaly")
                .ruleType(RuleType.AMOUNT_ANOMALY)
                .active(true)
                .weight(new BigDecimal("1.000"))
                .thresholdValue(new BigDecimal("3.00"))
                .timeline(90)
                .build());

        ruleRepository.save(Rule.builder()
                .ruleName("Amount Threshold")
                .ruleType(RuleType.AMOUNT_THRESHOLD)
                .active(true)
                .weight(new BigDecimal("1.000"))
                .thresholdValue(new BigDecimal("10000.00"))
                .timeline(30)
                .build());

        ruleRepository.save(Rule.builder()
                .ruleName("Velocity Check")
                .ruleType(RuleType.VELOCITY)
                .active(true)
                .weight(new BigDecimal("1.000"))
                .thresholdValue(new BigDecimal("5.00"))
                .timeline(10)
                .build());

        ruleRepository.save(Rule.builder()
                .ruleName("New Payee")
                .ruleType(RuleType.NEW_PAYEE)
                .active(true)
                .weight(new BigDecimal("1.000"))
                .thresholdValue(new BigDecimal("0.80"))
                .timeline(30)
                .build());

        ruleRepository.save(Rule.builder()
                .ruleName("Time Anomaly")
                .ruleType(RuleType.TIME_ANOMALY)
                .active(true)
                .weight(new BigDecimal("0.500"))
                .thresholdValue(new BigDecimal("0.60"))
                .timeline(30)
                .build());

        ruleRepository.save(Rule.builder()
                .ruleName("Location Change")
                .ruleType(RuleType.LOCATION_CHANGE)
                .active(true)
                .weight(new BigDecimal("0.750"))
                .thresholdValue(new BigDecimal("0.80"))
                .timeline(30)
                .build());

        ruleRepository.save(Rule.builder()
                .ruleName("Spending Pattern")
                .ruleType(RuleType.SPENDING_PATTERN)
                .active(true)
                .weight(new BigDecimal("0.500"))
                .thresholdValue(new BigDecimal("0.50"))
                .timeline(30)
                .build());

        ruleRepository.save(Rule.builder()
                .ruleName("Device Change")
                .ruleType(RuleType.DEVICE_CHANGE)
                .active(false) // no Java rule implementation bound - kept for schema completeness only
                .weight(BigDecimal.ZERO)
                .thresholdValue(BigDecimal.ZERO)
                .timeline(30)
                .build());

        log.info("Seeded 8 rows into 'rules' table");
    }

    private void seedCustomersAccountsPayees() {
        if (customerRepository.count() > 0) {
            return;
        }

        for (int i = 1; i <= 5; i++) {
            Customer customer = new Customer();
            customer.setFirstName("Customer" + i);
            customer.setLastName("Demo");
            customer.setEmail("customer" + i + "@example.com");
            customer.setPhone("555-000" + i);
            customer.setAddress(i + " Demo Street");
            customer = customerRepository.save(customer);

            for (int j = 1; j <= 2; j++) {
                Account account = new Account();
                account.setCustomer(customer);
                account.setAccountNumber("ACC-" + String.format("%03d", (i - 1) * 2 + j));
                account.setAccountType(j == 1 ? AccountType.CHECKING : AccountType.SAVINGS);
                accountRepository.save(account);
            }
        }

        for (int i = 1; i <= 20; i++) {
            Payee payee = new Payee();
            payee.setPayeeName("Payee " + i);
            payee.setPayeeIdentifier("PAYEE-ID-" + String.format("%03d", i));
            payeeRepository.save(payee);
        }

        log.info("Seeded 5 customers, 10 accounts, and 20 payees for Postman testing (account_id 1-10, payee_id 1-20)");
    }
}
