package com.example.demo.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.example.demo.entity.ChangeLog;
import com.example.demo.entity.ChangeLogDetail;

@Repository
public interface ChangeLogDetailRepository extends JpaRepository<ChangeLogDetail, Long> {
    List<ChangeLogDetail> findByChangeLog(ChangeLog changeLog);
} 