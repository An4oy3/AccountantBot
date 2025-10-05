package com.example.demo.repository;

import com.example.demo.model.entity.TransactionSplit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionSplitRepository extends JpaRepository<TransactionSplit, Long> {
}

