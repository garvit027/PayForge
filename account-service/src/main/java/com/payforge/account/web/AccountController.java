package com.payforge.account.web;

import com.payforge.account.service.AccountService;
import com.payforge.account.web.dto.AccountView;
import com.payforge.account.web.dto.CreateCustomerRequest;
import com.payforge.account.web.dto.CustomerView;
import com.payforge.account.web.dto.OpenAccountRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping("/customers")
    @PreAuthorize("hasAnyAuthority('SCOPE_accounts:write', 'SCOPE_platform:admin')")
    public ResponseEntity<CustomerView> registerCustomer(@Valid @RequestBody CreateCustomerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(accountService.registerCustomer(request));
    }

    @GetMapping("/customers/{customerId}")
    @PreAuthorize("hasAnyAuthority('SCOPE_accounts:read', 'SCOPE_platform:admin')")
    public CustomerView getCustomer(@PathVariable UUID customerId) {
        return accountService.getCustomer(customerId);
    }

    @PostMapping("/accounts")
    @PreAuthorize("hasAnyAuthority('SCOPE_accounts:write', 'SCOPE_platform:admin')")
    public ResponseEntity<AccountView> openAccount(@Valid @RequestBody OpenAccountRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(accountService.openAccount(request.customerId(), request.currency()));
    }

    @GetMapping("/accounts/{accountNumber}")
    @PreAuthorize("hasAnyAuthority('SCOPE_accounts:read', 'SCOPE_platform:admin')")
    public AccountView getAccount(@PathVariable String accountNumber) {
        return accountService.getAccount(accountNumber);
    }

    @GetMapping("/customers/{customerId}/accounts")
    @PreAuthorize("hasAnyAuthority('SCOPE_accounts:read', 'SCOPE_platform:admin')")
    public List<AccountView> accountsFor(@PathVariable UUID customerId) {
        return accountService.accountsFor(customerId);
    }
}
