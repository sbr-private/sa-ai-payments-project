package com.payments.ledger.api.mapper;

import com.payments.ledger.api.dto.RegisterAccountRequest;
import com.payments.ledger.domain.model.Party;
import java.util.Optional;

public final class RegisterAccountMapper {

  private RegisterAccountMapper() {}

  public static Party toParty(RegisterAccountRequest request) {
    Optional<String> externalId = Optional.empty();
    if (request.getOwner().getId() != null
        && request.getOwner().getId().getOthr() != null
        && request.getOwner().getId().getOthr().getId() != null) {
      externalId = Optional.of(request.getOwner().getId().getOthr().getId());
    }
    return new Party(request.getOwner().getNm(), externalId);
  }
}
