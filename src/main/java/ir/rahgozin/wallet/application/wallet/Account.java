package ir.rahgozin.wallet.application.wallet;

import ir.rahgozin.wallet.application.common.AbstractEntity;
import ir.rahgozin.wallet.application.customer.Customer;
import jakarta.persistence.*;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Table
@Entity
public class Account extends AbstractEntity {
    private String number;

    private AccountType type;

    // Keep ledger entries in a List; the database rejects duplicate non-null requestIds.
    @OneToMany(mappedBy = "account")
    private List<Transaction> transactions = new ArrayList<>();
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "owner_id")
    private Customer owner;
    public Account() {
        number = UUID.randomUUID().toString();
    }

    public void addTransaction(Transaction transaction) {
        if (transactions == null) {
            transactions = new ArrayList<>();
        }
        transaction.setAccount(this);
        transactions.add(transaction);
    }

    String issueDate;

    enum AccountType {
        CHECKING, SAVING
    }
}
