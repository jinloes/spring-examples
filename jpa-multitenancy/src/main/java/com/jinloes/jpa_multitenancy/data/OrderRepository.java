package com.jinloes.jpa_multitenancy.data;

import com.jinloes.jpa_multitenancy.model.Order;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, UUID> {
}
