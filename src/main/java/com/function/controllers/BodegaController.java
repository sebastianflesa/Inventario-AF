package com.function.controllers;

import com.function.models.BodegaEntity;
import com.function.service.BodegaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/bodegas")
public class BodegaController {
    @PutMapping("/{id}")
    public BodegaEntity updateBodega(@PathVariable Long id, @RequestBody BodegaEntity bodega) {
        return bodegaService.update(id, bodega);
    }

    @DeleteMapping("/{id}")
    public void deleteBodega(@PathVariable Long id) {
        bodegaService.delete(id);
    }
    @Autowired
    private BodegaService bodegaService;

    @GetMapping
    public List<BodegaEntity> getAllBodegas() {
        return bodegaService.findAll();
    }

    @PostMapping
    public BodegaEntity createBodega(@RequestBody BodegaEntity bodega) {
        return bodegaService.save(bodega);
    }
}
