package com.payforge.ledger.domain;

public enum AccountType {
    /** What the platform owns: settlement cash, receivables. */
    ASSET,
    /** What the platform owes customers: their wallet balances. */
    LIABILITY,
    /** Residual equity. */
    EQUITY
}
