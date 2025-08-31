package com.function.service;

import com.function.models.BodegaEntity;
import com.function.models.BodegaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BodegaService {
    @Autowired
    private BodegaRepository bodegaRepository;

    public List<BodegaEntity> findAll() {
        return bodegaRepository.findAll();
    }

    public BodegaEntity save(BodegaEntity bodega) {
        return bodegaRepository.save(bodega);
    }

    public BodegaEntity update(Long id, BodegaEntity bodega) {
        BodegaEntity existing = bodegaRepository.findById(id).orElse(null);
        if (existing == null) return null;
        // Solo actualiza los campos que vienen en el body (no null)
        if (bodega.getNombre() != null) {
            existing.setNombre(bodega.getNombre());
        }
        if (bodega.getDireccion() != null) {
            existing.setDireccion(bodega.getDireccion());
        }
        return bodegaRepository.save(existing);
    }

    public boolean delete(Long id) {
        if (!bodegaRepository.existsById(id)) return false;
        bodegaRepository.deleteById(id);
        return true;
    }
}
