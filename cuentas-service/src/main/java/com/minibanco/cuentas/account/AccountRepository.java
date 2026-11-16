package com.minibanco.cuentas.account;

import org.springframework.data.jpa.repository.JpaRepository;

// No @Repository needed, Spring detects interfaces that extend JpaRepository
public interface AccountRepository extends JpaRepository<Account, Long> {
}
