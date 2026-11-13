package com.payforge.ledger.web;

import com.payforge.ledger.service.LedgerService;
import com.payforge.ledger.web.dto.BalanceView;
import com.payforge.ledger.web.dto.EnsureAccountRequest;
import com.payforge.ledger.web.dto.JournalView;
import com.payforge.ledger.web.dto.PostJournalRequest;
import com.payforge.ledger.web.dto.SettlementRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/v1")
public class InternalLedgerController {

    private final LedgerService ledgerService;

    public InternalLedgerController(LedgerService ledgerService) {
        this.ledgerService = ledgerService;
    }

    @PostMapping("/ledger-accounts")
    public BalanceView ensureAccount(@Valid @RequestBody EnsureAccountRequest request) {
        return ledgerService.ensureAccount(request);
    }

    @PostMapping("/journals")
    public ResponseEntity<JournalView> post(@Valid @RequestBody PostJournalRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ledgerService.post(request));
    }

    @PostMapping("/settlements")
    public ResponseEntity<JournalView> settle(@Valid @RequestBody SettlementRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ledgerService.settle(request));
    }

    @GetMapping("/ledger-accounts/{code}/balance")
    public BalanceView balance(@PathVariable String code) {
        return ledgerService.balance(code);
    }
}
