package com.dotlabs.moneytransfer.service;

import com.dotlabs.moneytransfer.dto.response.AccountResponse;
import com.dotlabs.moneytransfer.entity.Account;

import java.util.List;

public interface AccountService {
    AccountResponse getAccountByNumber(String accountNumber);
    List<AccountResponse> getAllAccounts();
    Account createAccount(Account account);
}
