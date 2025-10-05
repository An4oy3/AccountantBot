package com.example.demo.service.impl;

import com.example.demo.model.entity.DialogStateData;
import com.example.demo.model.enums.DialogStateType;
import com.example.demo.repository.DialogStateDataRepository;
import com.example.demo.service.DialogStateService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DialogStateServiceImpl implements DialogStateService {

    private final DialogStateDataRepository dialogDataRepository;

    @Override
    public DialogStateType getStateType(Long chatId) {
        return dialogDataRepository.findByChatId(chatId)
                .map(DialogStateData::getState)
                .orElse(DialogStateType.NONE);
    }

    @Override
    public DialogStateData getState(Long chatId) {
        return dialogDataRepository.findByChatId(chatId)
                .orElse(null);
    }

    @Override
    @Transactional
    public void setDialogStateType(Long chatId, DialogStateType stateType) {
        if (chatId == null) {
            throw new IllegalArgumentException("chatId cannot be null");
        }
        DialogStateData stateData = getState(chatId);
        if (stateData == null) {
            stateData = new DialogStateData();
            stateData.setChatId(chatId);
        }
        stateData.setState(stateType);
        stateData.setLastUpdated(java.time.LocalDateTime.now());
        dialogDataRepository.save(stateData);
    }

    @Transactional
    @Override
    public DialogStateData saveOrUpdate(DialogStateData dialogStateData) {
        if (dialogStateData == null || dialogStateData.getChatId() == null) {
            throw new IllegalArgumentException("dialogStateData or chatId cannot be null");
        }
        dialogStateData.setLastUpdated(java.time.LocalDateTime.now());
        return dialogDataRepository.save(dialogStateData);
    }

    @Override
    public List<DialogStateData> findAll() {
        List<DialogStateData> list = new ArrayList<>();
        dialogDataRepository.findAll().forEach(list::add);
        return list;
    }

    @Override
    public void clearState(Long chatId) {
        dialogDataRepository.deleteById(chatId);
    }
}
