package com.payments.ledger.api;

import com.payments.ledger.api.dto.AccountResponse;
import com.payments.ledger.api.dto.RegisterAccountRequest;
import com.payments.ledger.api.mapper.AccountApiMapper;
import com.payments.ledger.api.mapper.RegisterAccountMapper;
import com.payments.ledger.domain.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AccountController {

  private final AccountService accountService;
  private final AccountApiMapper accountApiMapper;

  public AccountController(AccountService accountService, AccountApiMapper accountApiMapper) {
    this.accountService = accountService;
    this.accountApiMapper = accountApiMapper;
  }

  @PostMapping("/accounts")
  @ResponseStatus(HttpStatus.CREATED)
  public AccountResponse registerAccount(@Valid @RequestBody RegisterAccountRequest request) {
    return accountApiMapper.toResponse(
        accountService.registerAccount(RegisterAccountMapper.toParty(request), request.getCcy()));
  }
}
