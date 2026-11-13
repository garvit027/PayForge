package com.payforge.account.service;

import com.payforge.account.domain.Account;
import com.payforge.account.domain.Customer;
import com.payforge.account.repo.AccountRepository;
import com.payforge.account.repo.CustomerRepository;
import com.payforge.account.web.dto.AccountView;
import com.payforge.account.web.dto.CreateCustomerRequest;
import com.payforge.account.web.dto.CustomerView;
import com.payforge.commons.outbox.OutboxRecorder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class AccountService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final CustomerRepository customers;
    private final AccountRepository accounts;
    private final OutboxRecorder outbox;

    public AccountService(CustomerRepository customers, AccountRepository accounts, OutboxRecorder outbox) {
        this.customers = customers;
        this.accounts = accounts;
        this.outbox = outbox;
    }

    @Transactional
    public CustomerView registerCustomer(CreateCustomerRequest request) {
        String email = request.email().trim().toLowerCase();
        if (customers.existsByEmail(email)) {
            throw new IllegalArgumentException("A customer with email " + email + " already exists");
        }
        Customer customer = customers.save(new Customer(request.fullName().trim(), email, request.country().toUpperCase()));
        outbox.record("customer", customer.getId().toString(), "customer.registered", CustomerView.from(customer));
        return CustomerView.from(customer);
    }

    @Transactional(readOnly = true)
    public CustomerView getCustomer(UUID customerId) {
        return customers.findById(customerId)
                .map(CustomerView::from)
                .orElseThrow(() -> new NoSuchElementException("Customer " + customerId + " not found"));
    }

    @Transactional
    public AccountView openAccount(UUID customerId, String currency) {
        Customer customer = customers.findById(customerId)
                .orElseThrow(() -> new NoSuchElementException("Customer " + customerId + " not found"));

        String accountNumber = nextAccountNumber();
        Account account = accounts.save(new Account(
                customer.getId(), accountNumber, "CUST-" + accountNumber, currency.toUpperCase()));

        outbox.record("account", account.getId().toString(), "account.opened", AccountView.from(account));
        return AccountView.from(account);
    }

    @Transactional(readOnly = true)
    public AccountView getAccount(String accountNumber) {
        return accounts.findByAccountNumber(accountNumber)
                .map(AccountView::from)
                .orElseThrow(() -> new NoSuchElementException("Account " + accountNumber + " not found"));
    }

    @Transactional(readOnly = true)
    public List<AccountView> accountsFor(UUID customerId) {
        return accounts.findByCustomerId(customerId).stream().map(AccountView::from).toList();
    }

    private String nextAccountNumber() {
        for (int attempt = 0; attempt < 10; attempt++) {
            String candidate = "PF" + String.format("%010d", RANDOM.nextLong(10_000_000_000L));
            if (!accounts.existsByAccountNumber(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Could not allocate a unique account number");
    }
}
