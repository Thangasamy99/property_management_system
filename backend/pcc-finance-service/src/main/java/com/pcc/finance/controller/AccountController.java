package com.pcc.finance.controller;

import com.pcc.finance.dto.request.AccountRequest;
import com.pcc.finance.dto.response.AccountResponse;
import com.pcc.finance.enums.AccountType;
import com.pcc.finance.service.AccountService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Cash & Bank Accounts")
@SecurityRequirement(name = "bearerAuth")
public class AccountController {

    private final AccountService accountService;

    @GetMapping("/api/v1/cash-accounts")
    @PreAuthorize("hasAuthority('CASH_ACCOUNT_VIEW')")
    public List<AccountResponse> cashAccounts() {
        return accountService.list(AccountType.CASH);
    }

    @GetMapping("/api/v1/bank-accounts")
    @PreAuthorize("hasAuthority('CASH_ACCOUNT_VIEW')")
    public List<AccountResponse> bankAccounts() {
        return accountService.list(AccountType.BANK);
    }

    @GetMapping("/api/v1/accounts/{id}")
    @PreAuthorize("hasAuthority('CASH_ACCOUNT_VIEW')")
    public AccountResponse get(@PathVariable Long id) {
        return accountService.get(id);
    }

    @PostMapping("/api/v1/accounts")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('CASH_ACCOUNT_MANAGE')")
    public AccountResponse create(@Valid @RequestBody AccountRequest req) {
        return accountService.create(req);
    }

    @PutMapping("/api/v1/accounts/{id}")
    @PreAuthorize("hasAuthority('CASH_ACCOUNT_MANAGE')")
    public AccountResponse update(@PathVariable Long id, @Valid @RequestBody AccountRequest req) {
        return accountService.update(id, req);
    }

    @PostMapping("/api/v1/accounts/{id}/toggle")
    @PreAuthorize("hasAuthority('CASH_ACCOUNT_MANAGE')")
    public AccountResponse toggle(@PathVariable Long id) {
        return accountService.toggle(id);
    }
}
