package com.jinloes.jpa_multitenancy.web;

import com.jinloes.jpa_multitenancy.data.OrderRepository;
import com.jinloes.jpa_multitenancy.model.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {
  private final OrderRepository orderRepository;

  @PostMapping
  public ResponseEntity<Order> createOrder(@RequestBody Order order) {
    return ResponseEntity.status(HttpStatus.CREATED).body(orderRepository.save(order));
  }
}
