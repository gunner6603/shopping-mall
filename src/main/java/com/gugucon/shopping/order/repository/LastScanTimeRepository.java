package com.gugucon.shopping.order.repository;

import com.gugucon.shopping.order.domain.entity.LastScanTime;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LastScanTimeRepository extends JpaRepository<LastScanTime, Long> {
}
