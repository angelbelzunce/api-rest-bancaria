package com.minibanco.accounts.account;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateAccountRequest(@NotBlank @Size(max = 255) String holder,
                                   @NotNull @PositiveOrZero @Digits(integer = 17, fraction = 2) BigDecimal initialBalance) {
}
