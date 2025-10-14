package com.example.demo.service.impl;

import com.example.demo.exception.NotFoundException;
import com.example.demo.model.entity.Account;
import com.example.demo.model.entity.AccountType;
import com.example.demo.model.entity.User;
import com.example.demo.repository.AccountRepository;
import com.example.demo.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceImplTest {

    @Mock
    AccountRepository accountRepository;
    @Mock
    UserRepository userRepository;

    @InjectMocks
    AccountServiceImpl service;

    private User owner;

    @BeforeEach
    void setUp() {
        owner = new User();
        // set chatId via reflection (field is on User as chatId)
        owner.setChatId(100L);
        owner.setFirstName("Alex");
    }

    private void setId(Account acc, long id) {
        try {
            Field f = acc.getClass().getSuperclass().getDeclaredField("id");
            f.setAccessible(true);
            f.set(acc, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void buildAccountSelectionKeyboardNullChatIdThrows() {
        assertThatThrownBy(() -> service.buildAccountSelectionKeyboard(null, false, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ownerId is required");
    }

    @Test
    void buildAccountSelectionKeyboardEmptyAccountsProducesNoRows() {
        when(accountRepository.findByOwnerChatIdAndArchivedFalse(100L)).thenReturn(List.of());
        InlineKeyboardMarkup mk = service.buildAccountSelectionKeyboard(100L, false, "account:");
        assertThat(mk.getKeyboard()).isEmpty();
    }

    @Test
    void buildAccountSelectionKeyboardIncludesArchivedWhenFlagTrueAndMarksLabel() {
        Account a1 = new Account(); a1.setName("Cash"); a1.setCurrency("USD"); a1.setArchived(false); setId(a1,1L);
        Account a2 = new Account(); a2.setName("Card"); a2.setCurrency("EUR"); a2.setArchived(true); setId(a2,2L);
        when(accountRepository.findByOwnerChatId(100L)).thenReturn(List.of(a1,a2));
        InlineKeyboardMarkup mk = service.buildAccountSelectionKeyboard(100L, true, "sel:");
        assertThat(mk.getKeyboard()).hasSize(2);
        List<String> texts = mk.getKeyboard().stream().map(r -> r.get(0).getText()).toList();
        assertThat(texts.get(0)).isEqualTo("Cash (USD)");
        assertThat(texts.get(1)).isEqualTo("Card (EUR) (арх.)");
        List<String> callbacks = mk.getKeyboard().stream().map(r -> r.get(0).getCallbackData()).toList();
        assertThat(callbacks).containsExactly("sel:1","sel:2");
    }

    @Test
    void buildAccountSelectionKeyboardBlankPrefixFallsBackToDefault() {
        Account a1 = new Account(); a1.setName("Main"); a1.setCurrency("PLN"); setId(a1,5L);
        when(accountRepository.findByOwnerChatIdAndArchivedFalse(100L)).thenReturn(List.of(a1));
        InlineKeyboardMarkup mk = service.buildAccountSelectionKeyboard(100L, false, "  ");
        InlineKeyboardButton btn = mk.getKeyboard().get(0).get(0);
        assertThat(btn.getCallbackData()).isEqualTo("account:5");
    }

    @Test
    void buildAccountSelectionKeyboardCustomPrefixUsed() {
        Account a1 = new Account(); a1.setName("Main"); a1.setCurrency("PLN"); setId(a1,7L);
        when(accountRepository.findByOwnerChatIdAndArchivedFalse(100L)).thenReturn(List.of(a1));
        InlineKeyboardMarkup mk = service.buildAccountSelectionKeyboard(100L, false, "pick:");
        assertThat(mk.getKeyboard().get(0).get(0).getCallbackData()).isEqualTo("pick:7");
    }

    @Test
    void createAccountPersistsWithTrimAndUppercaseCurrency() {
        when(userRepository.findByChatId(100L)).thenReturn(Optional.of(owner));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> {
            Account a = inv.getArgument(0);
            setId(a, 55L);
            return a;
        });
        Account saved = service.createAccount(100L, "  My Cash  ", AccountType.CASH, "pln");
        assertThat(saved.getId()).isEqualTo(55L);
        assertThat(saved.getName()).isEqualTo("My Cash");
        assertThat(saved.getCurrency()).isEqualTo("PLN");
        assertThat(saved.getType()).isEqualTo(AccountType.CASH);
        verify(accountRepository).save(any(Account.class));
    }

    @Test
    void createAccountOwnerNotFoundThrows() {
        when(userRepository.findByChatId(100L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.createAccount(100L, "Name", AccountType.CASH, "USD"))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Owner user not found");
    }

    @Test
    void createAccountInvalidArgumentsThrow() {
        assertThatThrownBy(() -> service.createAccount(null, "Name", AccountType.CASH, "USD"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.createAccount(100L, " ", AccountType.CASH, "USD"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.createAccount(100L, "Name", null, "USD"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.createAccount(100L, "Name", AccountType.CASH, "US"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void updateAccountNameTrimsAndPersists() {
        Account acc = new Account(); acc.setName("Old"); acc.setCurrency("USD"); setId(acc, 10L);
        when(accountRepository.findById(10L)).thenReturn(Optional.of(acc));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));
        Account updated = service.updateAccountName(10L, "  New Name  ");
        assertThat(updated.getName()).isEqualTo("New Name");
    }

    @Test
    void updateAccountNameNullIdThrows() {
        assertThatThrownBy(() -> service.updateAccountName(null, "Name"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("accountId");
    }

    @Test
    void updateAccountNameBlankNameThrows() {
        assertThatThrownBy(() -> service.updateAccountName(10L, "  "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("newName");
    }

    @Test
    void updateAccountNameAccountNotFoundThrows() {
        when(accountRepository.findById(999L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.updateAccountName(999L, "Name"))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Account not found");
    }

    @Test
    void archiveAccountSetsFlagOnce() {
        Account acc = new Account(); acc.setName("A"); acc.setCurrency("USD"); setId(acc, 11L);
        when(accountRepository.findById(11L)).thenReturn(Optional.of(acc));
        service.archiveAccount(11L);
        assertThat(acc.isArchived()).isTrue();
        // second call should not trigger extra save (still allowed but state unchanged)
        service.archiveAccount(11L);
        verify(accountRepository, times(1)).save(acc);
    }

    @Test
    void unarchiveAccountClearsFlag() {
        Account acc = new Account(); acc.setName("A"); acc.setCurrency("USD"); acc.setArchived(true); setId(acc, 12L);
        when(accountRepository.findById(12L)).thenReturn(Optional.of(acc));
        service.unarchiveAccount(12L);
        assertThat(acc.isArchived()).isFalse();
    }

    @Test
    void deleteAccountNonExistingThrows() {
        when(accountRepository.existsById(500L)).thenReturn(false);
        assertThatThrownBy(() -> service.deleteAccount(500L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Account not found: 500");
    }

    @Test
    void deleteAccountDeletesWhenExists() {
        when(accountRepository.existsById(600L)).thenReturn(true);
        service.deleteAccount(600L);
        verify(accountRepository).deleteById(600L);
    }

    @Test
    void findOrCreateDefaultAccountReturnsExistingFirst() {
        Account existing = new Account(); existing.setName("Primary"); existing.setCurrency("USD"); setId(existing, 70L);
        when(userRepository.findByChatId(100L)).thenReturn(Optional.of(owner));
        when(accountRepository.findByOwnerChatIdAndArchivedFalse(100L)).thenReturn(List.of(existing));
        Account result = service.findOrCreateDefaultAccount(100L);
        assertThat(result).isSameAs(existing);
    }

    @Test
    void findOrCreateDefaultAccountCreatesWhenNone() {
        when(userRepository.findByChatId(100L)).thenReturn(Optional.of(owner));
        when(accountRepository.findByOwnerChatIdAndArchivedFalse(100L)).thenReturn(List.of());
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> { Account a = inv.getArgument(0); setId(a, 99L); return a; });
        Account created = service.findOrCreateDefaultAccount(100L);
        assertThat(created.getName()).isEqualTo("Alex's CASH Account");
        assertThat(created.getCurrency()).isEqualTo("PLN");
        assertThat(created.getType()).isEqualTo(AccountType.CASH);
    }

    @Test
    void buildAccountSelectionKeyboardExcludeArchivedOmitsArchivedAccounts() {
        Account active = new Account(); active.setName("Active"); active.setCurrency("USD"); active.setArchived(false); setId(active, 1L);
        Account archived = new Account(); archived.setName("Archived"); archived.setCurrency("USD"); archived.setArchived(true); setId(archived, 2L);
        when(accountRepository.findByOwnerChatIdAndArchivedFalse(100L)).thenReturn(List.of(active));
        InlineKeyboardMarkup mk = service.buildAccountSelectionKeyboard(100L, false, "acc:");
        assertThat(mk.getKeyboard()).hasSize(1);
        String text = mk.getKeyboard().get(0).get(0).getText();
        assertThat(text).isEqualTo("Active (USD)");
    }

    @Test
    void archiveAccountNotFoundThrows() {
        when(accountRepository.findById(321L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.archiveAccount(321L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Account not found: 321");
    }

    @Test
    void unarchiveAccountNotFoundThrows() {
        when(accountRepository.findById(654L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.unarchiveAccount(654L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Account not found: 654");
    }

    @Test
    void deleteAccountNullIdThrows() {
        assertThatThrownBy(() -> service.deleteAccount(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("accountId is required");
    }

    @Test
    void findOrCreateDefaultAccountOwnerNotFoundThrows() {
        when(userRepository.findByChatId(777L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.findOrCreateDefaultAccount(777L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Owner user not found: 777");
    }

    @Test
    void createAccountNullCurrencyThrows() {
        assertThatThrownBy(() -> service.createAccount(100L, "Name", AccountType.CASH, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("currency is required");
    }

    @Test
    void createAccountBlankCurrencyThrows() {
        assertThatThrownBy(() -> service.createAccount(100L, "Name", AccountType.CASH, "   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("currency is required");
    }

    @Test
    void findByIdExistingReturnsAccount() {
        Account acc = new Account(); acc.setName("X"); acc.setCurrency("USD"); setId(acc, 901L);
        when(accountRepository.findById(901L)).thenReturn(Optional.of(acc));
        Optional<Account> result = service.findById(901L);
        assertThat(result).isPresent();
        assertThat(result.get()).isSameAs(acc);
    }

    @Test
    void findByIdNonExistingReturnsEmpty() {
        when(accountRepository.findById(902L)).thenReturn(Optional.empty());
        Optional<Account> result = service.findById(902L);
        assertThat(result).isEmpty();
    }

    @Test
    void getAccountsByChatIdNullChatIdThrows() {
        assertThatThrownBy(() -> service.getAccountsByChatId(null, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ownerId is required");
    }

    @Test
    void getAccountsByChatIdIncludeArchivedTrueReturnsAll() {
        Account a1 = new Account(); a1.setName("A1"); a1.setCurrency("USD"); setId(a1,1L);
        Account a2 = new Account(); a2.setName("A2"); a2.setCurrency("USD"); a2.setArchived(true); setId(a2,2L);
        when(accountRepository.findByOwnerChatId(100L)).thenReturn(List.of(a1,a2));
        List<Account> list = service.getAccountsByChatId(100L, true);
        assertThat(list).containsExactly(a1,a2);
        verify(accountRepository).findByOwnerChatId(100L);
        verify(accountRepository, never()).findByOwnerChatIdAndArchivedFalse(anyLong());
    }

    @Test
    void getAccountsByChatIdExcludeArchivedReturnsOnlyActive() {
        Account active = new Account(); active.setName("Active"); active.setCurrency("USD"); setId(active,3L);
        when(accountRepository.findByOwnerChatIdAndArchivedFalse(100L)).thenReturn(List.of(active));
        List<Account> list = service.getAccountsByChatId(100L, false);
        assertThat(list).containsExactly(active);
        verify(accountRepository).findByOwnerChatIdAndArchivedFalse(100L);
        verify(accountRepository, never()).findByOwnerChatId(100L);
    }

    @Test
    void updateAccountNameNullNameThrows() {
        assertThatThrownBy(() -> service.updateAccountName(10L, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("newName is required");
    }
}
