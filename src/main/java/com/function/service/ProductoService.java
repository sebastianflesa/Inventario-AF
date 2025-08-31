package com.function.service;

import com.function.models.ProductoEntity;
import com.function.models.ProductoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductoService {
    @Autowired
    private ProductoRepository productoRepository;

    public List<ProductoEntity> findAll() {
        return productoRepository.findAll();
    }

    public ProductoEntity save(ProductoEntity producto) {
        return productoRepository.save(producto);
    }

    public ProductoEntity update(Integer id, ProductoEntity producto) {
        ProductoEntity existing = productoRepository.findById(id).orElse(null);
        if (existing == null) return null;
        if (producto.getNombre() != null) {
            existing.setNombre(producto.getNombre());
        }
        if (producto.getStock() != null) {
            existing.setStock(producto.getStock());
        }
        if (producto.getPrecio() != null) {
            existing.setPrecio(producto.getPrecio());
        }
        if (producto.getBodega() != null) {
            existing.setBodega(producto.getBodega());
        }
        return productoRepository.save(existing);
    }

    public boolean delete(Integer id) {
        if (!productoRepository.existsById(id)) return false;
        productoRepository.deleteById(id);
        return true;
    }
}
