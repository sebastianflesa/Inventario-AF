package com.function.models;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "productos")
public class ProductoEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String nombre;
    private Integer stock;
    private Integer precio;

    @ManyToOne
    @JoinColumn(name = "bodega_id")
    private BodegaEntity bodega;
}
