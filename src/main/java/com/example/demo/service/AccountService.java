package com.example.demo.service;

import com.example.demo.model.entity.Account;
import com.example.demo.model.entity.AccountType;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

import java.util.List;
import java.util.Optional;

/**
 * Service layer abstraction for working with {@link Account} entities.
 * Provides typical CRUD-style operations plus domain-specific actions
 * like archiving and participant management.
 */
public interface AccountService {

    /**
     * Create a new account for an owner.
     * @param ownerId owner user id
     * @param name human-readable name
     * @param type account type
     * @param currency ISO 4217 3-letter code
     * @return persisted account
     */
    Account createAccount(Long ownerId, String name, AccountType type, String currency);

    /**
     * Find account by id.
     */
    Optional<Account> findById(Long id);

    /**
     * Get all accounts for owner (optionally excluding archived ones).
     */
    List<Account> getAccountsByChatId(Long ownerId, boolean includeArchived);

    /**
     * Update only the name of an account.
     */
    Account updateAccountName(Long accountId, String newName);

    /** Archive (soft hide) the account. */
    void archiveAccount(Long accountId);

    /** Undo archive. */
    void unarchiveAccount(Long accountId);

    /**
     * Permanently delete account (hard delete). Use cautiously.
     */
    void deleteAccount(Long accountId);

    Account findOrCreateDefaultAccount(Long chatId);

    Optional<Account> findByNameAndOwnerId(String name, Long ownerId);

    /**
     * Build inline keyboard for selecting one of the user's accounts.
     * @param ownerId owner user id
     * @param includeArchived whether to include archived accounts
     * @param actionPrefix prefix for callback data to identify the action
     * @return inline keyboard markup with account selection buttons
     */

    InlineKeyboardMarkup buildAccountSelectionKeyboard(Long ownerId, boolean includeArchived, String actionPrefix);

//    /** Add participant user to shared account. */
//    Account addParticipant(Long accountId, Long userId);
//
//    /** Remove participant user from shared account. */
//    Account removeParticipant(Long accountId, Long userId);
}
