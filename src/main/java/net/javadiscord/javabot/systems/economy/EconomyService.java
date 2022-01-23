package net.javadiscord.javabot.systems.economy;

import com.zaxxer.hikari.HikariDataSource;
import lombok.RequiredArgsConstructor;
import net.javadiscord.javabot.systems.economy.dao.AccountRepository;
import net.javadiscord.javabot.systems.economy.dao.TransactionRepository;
import net.javadiscord.javabot.systems.economy.model.Account;
import net.javadiscord.javabot.systems.economy.model.Transaction;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

/**
 * Class that handles Economy actions.
 */
@RequiredArgsConstructor
public class EconomyService {
	private final HikariDataSource dataSource;

	/**
	 * Creates a new Economy Account if none exists.
	 *
	 * @param userId The user's id.
	 * @return An {@link Account} object.
	 * @throws SQLException If an error occurs.
	 */
	public Account getOrCreateAccount(long userId) throws SQLException {
		try (Connection con = this.dataSource.getConnection()) {
			con.setAutoCommit(false);
			AccountRepository accountRepository = new AccountRepository(con);
			Account account = accountRepository.getAccount(userId);
			if (account == null) {
				account = new Account();
				account.setUserId(userId);
				account.setBalance(0);
				accountRepository.saveNewAccount(account);
			}
			con.commit();
			return account;
		}
	}

	/**
	 * Gets all recent transactions from a user.
	 *
	 * @param userId The user's id.
	 * @param count  The count of transactions to retrieve.
	 * @return A {@link List} with all transactions.
	 * @throws SQLException If an error occurs.
	 */
	public List<Transaction> getRecentTransactions(long userId, int count) throws SQLException {
		try (Connection con = this.dataSource.getConnection()) {
			con.setReadOnly(true);
			var transactions = new TransactionRepository(con).getLatestTransactions(userId, count);
			con.close();
			return transactions;
		}
	}

	/**
	 * Performs a single transaction.
	 *
	 * @param fromUserId The sender's user id.
	 * @param toUserId   The recipient's user id.
	 * @param value      The transaction's value.
	 * @param message    The transaction's message.
	 * @return A {@link Transaction} object.
	 * @throws SQLException If an error occurs.
	 */
	public Transaction performTransaction(Long fromUserId, Long toUserId, long value, String message) throws SQLException {
		if (value == 0) throw new IllegalArgumentException("Cannot create zero-value transaction.");
		if (Objects.equals(fromUserId, toUserId)) {
			throw new IllegalArgumentException("Sender and recipient cannot be the same.");
		}

		Transaction t = new Transaction();
		t.setFromUserId(fromUserId);
		t.setToUserId(toUserId);
		t.setValue(value);
		t.setMessage(message);

		try (Connection con = this.dataSource.getConnection()) {
			con.setAutoCommit(false);
			TransactionRepository transactionRepository = new TransactionRepository(con);
			AccountRepository accountRepository = new AccountRepository(con);
			// Deduct the amount from the sender's account balance.
			if (fromUserId != null) {
				Account account = accountRepository.getAccount(fromUserId);
				if (account == null) {
					account = new Account();
					account.setUserId(fromUserId);
					account.setBalance(0);
					accountRepository.saveNewAccount(account);
				}
				if (account.getBalance() < value) {
					throw new IllegalStateException("Sender account does not have the required funds.");
				}
				account.updateBalance(-value);
				accountRepository.updateAccount(account);
			}
			// Add the amount to the receiver's account balance.
			if (toUserId != null) {
				Account account = accountRepository.getAccount(toUserId);
				if (account == null) {
					account = new Account();
					account.setUserId(toUserId);
					account.setBalance(0);
					accountRepository.saveNewAccount(account);
				}
				account.updateBalance(value);
				accountRepository.updateAccount(account);
			}
			transactionRepository.saveNewTransaction(t);
			con.commit();
			return transactionRepository.getTransaction(t.getId());
		}
	}
}
