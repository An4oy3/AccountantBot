package com.example.demo.service;

import com.example.demo.model.entity.DialogStateData;
import com.example.demo.model.enums.DialogStateType;

import java.util.List;

public interface DialogStateService {
    DialogStateType getStateType(Long chatId);
    DialogStateData getState(Long chatId);
    void setDialogStateType(Long chatId, DialogStateType stateType);
    void clearState(Long chatId);
    DialogStateData saveOrUpdate(DialogStateData dialogStateData);
    List<DialogStateData> findAll();
}
