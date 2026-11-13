package com.payforge.transfer.web;

import com.payforge.transfer.service.TransferService;
import com.payforge.transfer.web.dto.CreateTransferRequest;
import com.payforge.transfer.web.dto.TransferView;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/transfers")
public class TransferController {

    private final TransferService transferService;

    public TransferController(TransferService transferService) {
        this.transferService = transferService;
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('SCOPE_transfers:write', 'SCOPE_platform:admin')")
    public ResponseEntity<TransferView> create(@RequestHeader("Idempotency-Key") String idempotencyKey,
                                               @Valid @RequestBody CreateTransferRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transferService.create(idempotencyKey, request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('SCOPE_transfers:read', 'SCOPE_platform:admin')")
    public TransferView get(@PathVariable UUID id) {
        return transferService.get(id);
    }
}
