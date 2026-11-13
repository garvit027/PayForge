package com.payforge.ledger.web;

import com.payforge.ledger.service.LedgerService;
import com.payforge.ledger.web.dto.BalanceView;
import com.payforge.ledger.web.dto.JournalView;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ledger")
public class LedgerQueryController {

    private final LedgerService ledgerService;

    public LedgerQueryController(LedgerService ledgerService) {
        this.ledgerService = ledgerService;
    }

    @GetMapping("/accounts/{code}/balance")
    @PreAuthorize("hasAnyAuthority('SCOPE_ledger:read', 'SCOPE_platform:admin')")
    public BalanceView balance(@PathVariable String code) {
        return ledgerService.balance(code);
    }

    @GetMapping("/accounts/{code}/statement")
    @PreAuthorize("hasAnyAuthority('SCOPE_ledger:read', 'SCOPE_platform:admin')")
    public List<JournalView> statement(@PathVariable String code) {
        return ledgerService.statement(code);
    }
}
