package com.payforge.account.web;

import com.payforge.account.service.AccountService;
import com.payforge.account.web.dto.AccountView;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Service-to-service surface. Not routed by the public gateway; see InternalTokenFilter.
 */
@RestController
@RequestMapping("/internal/v1")
public class InternalAccountController {

    private final AccountService accountService;

    public InternalAccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/accounts/{accountNumber}")
    public AccountView resolve(@PathVariable String accountNumber) {
        return accountService.getAccount(accountNumber);
    }
}
