package com.example.demo.service.impl;

import com.example.demo.exception.NotFoundException;
import com.example.demo.model.entity.User;
import com.example.demo.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    UserRepository userRepository;

    @InjectMocks
    UserServiceImpl userService;

    @Captor
    ArgumentCaptor<User> userCaptor;

    @Test
    void createUserSuccessfulPersistsTrimmedFields() {
        when(userRepository.existsByUsername("alice")) .thenReturn(false);
        when(userRepository.existsByChatId(100L)).thenReturn(false);
        userService.createUser("Alice", "  First  ", "  Last  ", 100L);
        verify(userRepository).save(userCaptor.capture());
        User saved = userCaptor.getValue();
        assertThat(saved.getUsername()).isEqualTo("Alice");
        assertThat(saved.getFirstName()).isEqualTo("First");
        assertThat(saved.getLastName()).isEqualTo("Last");
        assertThat(saved.getChatId()).isEqualTo(100L);
    }

    @Test
    void createUserOmitsBlankNames() {
        when(userRepository.existsByUsername("bob")) .thenReturn(false);
        when(userRepository.existsByChatId(200L)).thenReturn(false);
        userService.createUser("bob", "  ", "", 200L);
        verify(userRepository).save(userCaptor.capture());
        User saved = userCaptor.getValue();
        assertThat(saved.getFirstName()).isNull();
        assertThat(saved.getLastName()).isNull();
    }

    @Test
    void createUserUsernameNullThrows() {
        assertThatThrownBy(() -> userService.createUser(null, "a", "b", 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("username is required");
    }

    @Test
    void createUserUsernameBlankThrows() {
        assertThatThrownBy(() -> userService.createUser("  ", "a", "b", 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("username is required");
    }

    @Test
    void createUserChatIdNullThrows() {
        assertThatThrownBy(() -> userService.createUser("user", "a", "b", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("chatId is required");
    }

    @Test
    void createUserDuplicateUsernameThrows() {
        when(userRepository.existsByUsername("dupe")) .thenReturn(true);
        assertThatThrownBy(() -> userService.createUser("dupe", null, null, 11L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User with username already exists: dupe");
    }

    @Test
    void createUserDuplicateChatIdThrows() {
        when(userRepository.existsByUsername("unique")) .thenReturn(false);
        when(userRepository.existsByChatId(300L)).thenReturn(true);
        assertThatThrownBy(() -> userService.createUser("unique", null, null, 300L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User with chatId already exists: 300");
    }

    @Test
    void findByIdDelegatesToRepository() {
        User u = new User();
        when(userRepository.findById(5L)).thenReturn(Optional.of(u));
        assertThat(userService.findById(5L)).containsSame(u);
    }

    @Test
    void findByUsernameNullReturnsEmptyAndSkipsRepository() {
        assertThat(userService.findByUsername(null)).isEmpty();
        verify(userRepository, never()).findByUsername(any());
    }

    @Test
    void findByUsernameTrimsAndLowercases() {
        User u = new User();
        when(userRepository.findByUsername("john")) .thenReturn(Optional.of(u));
        assertThat(userService.findByUsername("  John  ")).containsSame(u);
        verify(userRepository).findByUsername("john");
    }

    @Test
    void findAllReturnsList() {
        User u1 = new User();
        User u2 = new User();
        when(userRepository.findAll()).thenReturn(List.of(u1, u2));
        assertThat(userService.findAll()).containsExactly(u1, u2);
    }

    @Test
    void findByChatIdNotFoundThrows() {
        when(userRepository.findByChatId(999L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> userService.findByChatId(999L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("User not found with chatId: 999");
    }

    @Test
    void findByChatIdReturnsUser() {
        User u = new User();
        when(userRepository.findByChatId(10L)).thenReturn(Optional.of(u));
        assertThat(userService.findByChatId(10L)).isSameAs(u);
    }

    @Test
    void deleteUserNullIdThrows() {
        assertThatThrownBy(() -> userService.deleteUser(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("userId is required");
    }

    @Test
    void deleteUserNotFoundThrows() {
        when(userRepository.existsById(77L)).thenReturn(false);
        assertThatThrownBy(() -> userService.deleteUser(77L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("User not found: 77");
    }

    @Test
    void deleteUserDeletesWhenExists() {
        when(userRepository.existsById(88L)).thenReturn(true);
        userService.deleteUser(88L);
        verify(userRepository).deleteById(88L);
    }

    @Test
    void existsByUsernameNullReturnsFalse() {
        assertThat(userService.existsByUsername(null)).isFalse();
        verify(userRepository, never()).existsByUsername(any());
    }

    @Test
    void existsByUsernameBlankReturnsFalse() {
        assertThat(userService.existsByUsername("  ")).isFalse();
        verify(userRepository, never()).existsByUsername(any());
    }

    @Test
    void existsByUsernameNormalTrimsAndLowercases() {
        when(userRepository.existsByUsername("mary")) .thenReturn(true);
        assertThat(userService.existsByUsername("  Mary  ")).isTrue();
        verify(userRepository).existsByUsername("mary");
    }

    @Test
    void existsByChatIdNullReturnsFalse() {
        assertThat(userService.existsByChatId(null)).isFalse();
        verify(userRepository, never()).existsByChatId(any());
    }

    @Test
    void existsByChatIdDelegates() {
        when(userRepository.existsByChatId(123L)).thenReturn(true);
        assertThat(userService.existsByChatId(123L)).isTrue();
    }
}

