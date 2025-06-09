package com.jinloes.jpa_multitenancy.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Data;
import lombok.experimental.Accessors;
import org.hibernate.annotations.TenantId;

@Entity
@Table(name = "orders")
@Data
@Accessors(chain = true)
public class Order {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;
  @Column(nullable = false)
  private String name;

  @TenantId
  @Column(nullable = false)
  private String customerId;
}
