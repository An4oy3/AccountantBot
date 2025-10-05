package com.example.demo.service.impl;

import com.example.demo.model.entity.Account;
import com.example.demo.model.entity.AccountType;
import com.example.demo.model.entity.User;
import com.example.demo.repository.AccountRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.AccountService;
import com.example.demo.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public Account createAccount(Long chatId, String name, AccountType type, String currency) {
        if (chatId == null) throw new IllegalArgumentException("ownerId is required");
        if (name == null || name.isBlank()) throw new IllegalArgumentException("name is required");
        if (type == null) throw new IllegalArgumentException("type is required");
        validateCurrency(currency);

        User owner = userRepository.findByChatId(chatId)
                .orElseThrow(() -> new NotFoundException("Owner user not found: " + chatId));

        Account acc = new Account();
        acc.setOwner(owner);
        acc.setName(name.trim());
        acc.setType(type);
        acc.setCurrency(currency.trim().toUpperCase());
        return accountRepository.save(acc);
    }

    @Override
    public Optional<Account> findById(Long id) {
        return accountRepository.findById(id);
    }

    @Override
    public List<Account> getAccountsByChatId(Long chatId, boolean includeArchived) {
        if (chatId == null) throw new IllegalArgumentException("ownerId is required");
        return includeArchived ? accountRepository.findByOwnerChatId(chatId)
                : accountRepository.findByOwnerChatIdAndArchivedFalse(chatId);
    }

    @Override
    @Transactional
    public Account updateAccountName(Long accountId, String newName) {
        if (accountId == null) throw new IllegalArgumentException("accountId is required");
        if (newName == null || newName.isBlank()) throw new IllegalArgumentException("newName is required");
        Account acc = accountRepository.findById(accountId)
                .orElseThrow(() -> new NotFoundException("Account not found: " + accountId));
        acc.setName(newName.trim());
        return accountRepository.save(acc);
    }

    @Override
    @Transactional
    public void archiveAccount(Long accountId) {
        Account acc = accountRepository.findById(accountId)
                .orElseThrow(() -> new NotFoundException("Account not found: " + accountId));
        if (!acc.isArchived()) {
            acc.setArchived(true);
            accountRepository.save(acc);
        }
    }

    @Override
    @Transactional
    public void unarchiveAccount(Long accountId) {
        Account acc = accountRepository.findById(accountId)
                .orElseThrow(() -> new NotFoundException("Account not found: " + accountId));
        if (acc.isArchived()) {
            acc.setArchived(false);
            accountRepository.save(acc);
        }
    }

    @Override
    @Transactional
    public void deleteAccount(Long accountId) {
        if (accountId == null) throw new IllegalArgumentException("accountId is required");
        if (!accountRepository.existsById(accountId)) {
            throw new NotFoundException("Account not found: " + accountId);
        }
        accountRepository.deleteById(accountId);
    }

    @Override
    @Transactional
    public Account findDefaultAccount(Long chatId) {
        User owner = userRepository.findByChatId(chatId)
                .orElseThrow(() -> new NotFoundException("Owner user not found: " + chatId));

        List<Account> accounts = getAccountsByChatId(chatId, false);
        if (accounts.isEmpty()) {
            // Create a default account if none exist
            return createAccount(chatId, owner.getFirstName() + "'s CASH Account", AccountType.CASH, "PLN");
        }
        return accounts.get(0); // TODO: расширить выбор аккаунта позже
    }

    @Override
    public InlineKeyboardMarkup buildAccountSelectionKeyboard(Long chatID, boolean includeArchived, String actionPrefix) {
        if (chatID == null) throw new IllegalArgumentException("ownerId is required");
        String prefix = (!StringUtils.hasText(actionPrefix)) ? "account:" : actionPrefix;

        List<Account> accounts = getAccountsByChatId(chatID, includeArchived);

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new java.util.ArrayList<>();

        for (Account acc : accounts) {
            StringBuilder label = new StringBuilder()
                    .append(acc.getDisplayName());
            if (acc.isArchived()) {
                label.append(" (арх.)");
            }
            InlineKeyboardButton btn = new InlineKeyboardButton();
            btn.setText(label.toString());
            btn.setCallbackData(prefix + acc.getId());
            rows.add(java.util.List.of(btn));
        }
        markup.setKeyboard(rows);
        return markup;
    }

    private void validateCurrency(String currency) {
        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException("currency is required");
        }
        String c = currency.trim();
        if (c.length() != 3 || !c.chars().allMatch(Character::isLetter)) {
            throw new IllegalArgumentException("currency must be 3 letters ISO 4217");
        }
    }
}

