package com.example.demo.service.impl;

import com.example.demo.model.entity.DialogStateData;
import com.example.demo.model.enums.DialogStateType;
import com.example.demo.repository.DialogStateDataRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DialogStateServiceImplTest {

    @Mock
    DialogStateDataRepository repository;

    @InjectMocks
    DialogStateServiceImpl service;

    private static final Long CHAT_ID = 999L;

    private DialogStateData state(DialogStateType type) {
        DialogStateData d = new DialogStateData();
        d.setChatId(CHAT_ID);
        d.setState(type);
        d.setIsExpense(true);
        d.setLastUpdated(LocalDateTime.now().minusMinutes(5));
        return d;
    }

    @Captor
    ArgumentCaptor<DialogStateData> captor;

    @Test
    void getStateTypeReturnsExistingState() {
        when(repository.findByChatId(CHAT_ID)).thenReturn(Optional.of(state(DialogStateType.AWAITING_AMOUNT)));
        assertThat(service.getStateType(CHAT_ID)).isEqualTo(DialogStateType.AWAITING_AMOUNT);
    }

    @Test
    void getStateTypeReturnsNoneWhenAbsent() {
        when(repository.findByChatId(CHAT_ID)).thenReturn(Optional.empty());
        assertThat(service.getStateType(CHAT_ID)).isEqualTo(DialogStateType.NONE);
    }

    @Test
    void getStateReturnsEntityOrNull() {
        DialogStateData ds = state(DialogStateType.IDLE);
        when(repository.findByChatId(CHAT_ID)).thenReturn(Optional.of(ds));
        assertThat(service.getState(CHAT_ID)).isSameAs(ds);
        when(repository.findByChatId(CHAT_ID)).thenReturn(Optional.empty());
        assertThat(service.getState(CHAT_ID)).isNull();
    }

    @Test
    void setDialogStateTypeCreatesNewWhenAbsent() {
        when(repository.findByChatId(CHAT_ID)).thenReturn(Optional.empty());
        service.setDialogStateType(CHAT_ID, DialogStateType.IDLE);
        verify(repository).save(captor.capture());
        DialogStateData saved = captor.getValue();
        assertThat(saved.getChatId()).isEqualTo(CHAT_ID);
        assertThat(saved.getState()).isEqualTo(DialogStateType.IDLE);
        assertThat(saved.getLastUpdated()).isNotNull();
    }

    @Test
    void setDialogStateTypeUpdatesExisting() {
        DialogStateData existing = state(DialogStateType.IDLE);
        LocalDateTime prevTime = existing.getLastUpdated();
        when(repository.findByChatId(CHAT_ID)).thenReturn(Optional.of(existing));
        service.setDialogStateType(CHAT_ID, DialogStateType.SUCCESS);
        verify(repository).save(existing);
        assertThat(existing.getState()).isEqualTo(DialogStateType.SUCCESS);
        assertThat(existing.getLastUpdated()).isAfter(prevTime);
    }

    @Test
    void setDialogStateTypeNullChatIdThrows() {
        assertThatThrownBy(() -> service.setDialogStateType(null, DialogStateType.IDLE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("chatId cannot be null");
        verify(repository, never()).save(any());
    }

    @Test
    void saveOrUpdatePersistsAndRefreshesTimestamp() {
        DialogStateData ds = state(DialogStateType.IDLE);
        LocalDateTime old = ds.getLastUpdated();
        when(repository.save(any(DialogStateData.class))).thenAnswer(inv -> inv.getArgument(0));
        DialogStateData saved = service.saveOrUpdate(ds);
        assertThat(saved.getLastUpdated()).isAfter(old);
        verify(repository).save(ds);
    }

    @Test
    void saveOrUpdateNullDataThrows() {
        assertThatThrownBy(() -> service.saveOrUpdate(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dialogStateData or chatId cannot be null");
    }

    @Test
    void saveOrUpdateNullChatIdThrows() {
        DialogStateData ds = new DialogStateData();
        assertThatThrownBy(() -> service.saveOrUpdate(ds))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dialogStateData or chatId cannot be null");
    }

    @Test
    void findAllReturnsAllEntries() {
        DialogStateData d1 = state(DialogStateType.IDLE);
        DialogStateData d2 = state(DialogStateType.SUCCESS);
        when(repository.findAll()).thenReturn(List.of(d1, d2));
        assertThat(service.findAll()).containsExactly(d1, d2);
    }

    @Test
    void clearStateDeletesById() {
        service.clearState(CHAT_ID);
        verify(repository).deleteById(CHAT_ID);
    }
}

