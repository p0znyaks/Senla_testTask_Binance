package com.example.p0znyaks.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "price_record")
@Getter
@Setter
@ToString
@EqualsAndHashCode(of = {"symbol", "closeTime"})
@AllArgsConstructor
@NoArgsConstructor
public class PriceRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String symbol;
    private BigDecimal price;
    private Instant fetchedAt;
    private Long closeTime;
}
