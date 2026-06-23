package com.payments.ledger.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class RegisterAccountRequest {

  @NotNull @Valid private PartyIdentificationDto owner;

  @NotBlank
  @Pattern(regexp = "^[A-Z]{3}$")
  private String ccy;

  public PartyIdentificationDto getOwner() {
    return owner;
  }

  public void setOwner(PartyIdentificationDto owner) {
    this.owner = owner;
  }

  public String getCcy() {
    return ccy;
  }

  public void setCcy(String ccy) {
    this.ccy = ccy;
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class PartyIdentificationDto {

    @NotBlank
    @Size(max = 140)
    private String nm;

    @Valid private PartyIdDto id;

    public String getNm() {
      return nm;
    }

    public void setNm(String nm) {
      this.nm = nm;
    }

    public PartyIdDto getId() {
      return id;
    }

    public void setId(PartyIdDto id) {
      this.id = id;
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class PartyIdDto {

    @Valid private OtherIdDto othr;

    public OtherIdDto getOthr() {
      return othr;
    }

    public void setOthr(OtherIdDto othr) {
      this.othr = othr;
    }
  }

  public static class OtherIdDto {

    @NotBlank
    @Size(max = 35)
    private String id;

    public String getId() {
      return id;
    }

    public void setId(String id) {
      this.id = id;
    }
  }
}
