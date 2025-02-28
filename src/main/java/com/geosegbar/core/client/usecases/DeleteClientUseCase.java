package com.geosegbar.core.client.usecases;

import com.geosegbar.adapters.client.ClientRepositoryAdapter;
import com.geosegbar.exceptions.NotFoundException;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DeleteClientUseCase {
    
    private final ClientRepositoryAdapter clientRepositoryAdapter;

    public void delete(Long id){
        clientRepositoryAdapter.findById(id)
        .orElseThrow(() -> new NotFoundException("Cliente não encontrado para exclusão!"));
        clientRepositoryAdapter.deleteById(id);
    }
}
