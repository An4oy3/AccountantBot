package com.example.demo.repository;

import com.example.demo.model.entity.DialogStateData;
import com.example.demo.model.enums.DialogStateType;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DialogStateDataRepository extends CrudRepository<DialogStateData, Long> {
    List<DialogStateData> findByState(DialogStateType state);
    Optional<DialogStateData> findByChatId(Long chatId);

}
