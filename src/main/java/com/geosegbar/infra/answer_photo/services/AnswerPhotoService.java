package com.geosegbar.infra.answer_photo.services;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.geosegbar.entities.AnswerEntity;
import com.geosegbar.entities.AnswerPhotoEntity;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.infra.answer.persistence.jpa.AnswerRepository;
import com.geosegbar.infra.answer_photo.persistence.jpa.AnswerPhotoRepository;
import com.geosegbar.infra.file_storage.FileStorageService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AnswerPhotoService {

    private final AnswerPhotoRepository answerPhotoRepository;
    private final AnswerRepository answerRepository;
    private final FileStorageService fileStorageService;

    @Transactional
    public void deleteById(Long id) {
        AnswerPhotoEntity photo = answerPhotoRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Foto da resposta não encontrada para exclusão!"));

        if (photo.getImagePath() != null) {
            fileStorageService.deleteFile(photo.getImagePath());
        }

        answerPhotoRepository.deleteById(id);
    }

    @Transactional
    public AnswerPhotoEntity savePhoto(Long answerId, MultipartFile photo) {
        AnswerEntity answer = answerRepository.findById(answerId)
                .orElseThrow(() -> new NotFoundException("Resposta não encontrada!"));

        String photoUrl = fileStorageService.storeFile(photo, "answer-photos");

        AnswerPhotoEntity answerPhoto = new AnswerPhotoEntity();
        answerPhoto.setAnswer(answer);
        answerPhoto.setImagePath(photoUrl);

        return answerPhotoRepository.save(answerPhoto);
    }

    @Transactional
    public AnswerPhotoEntity update(AnswerPhotoEntity answerPhoto) {
        answerPhotoRepository.findById(answerPhoto.getId())
                .orElseThrow(() -> new NotFoundException("Foto da resposta não encontrada para atualização!"));
        return answerPhotoRepository.save(answerPhoto);
    }

    @Transactional
    public AnswerPhotoEntity updatePhoto(Long photoId, MultipartFile photo) {
        AnswerPhotoEntity answerPhoto = answerPhotoRepository.findById(photoId)
                .orElseThrow(() -> new NotFoundException("Foto da resposta não encontrada para atualização!"));

        if (answerPhoto.getImagePath() != null) {
            fileStorageService.deleteFile(answerPhoto.getImagePath());
        }

        String photoUrl = fileStorageService.storeFile(photo, "answer-photos");
        answerPhoto.setImagePath(photoUrl);

        return answerPhotoRepository.save(answerPhoto);
    }

    public AnswerPhotoEntity findById(Long id) {
        return answerPhotoRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Foto da resposta não encontrada!"));
    }

    public List<AnswerPhotoEntity> findAll() {
        return answerPhotoRepository.findAll();
    }

    public List<AnswerPhotoEntity> findByAnswerId(Long answerId) {
        return answerPhotoRepository.findByAnswerId(answerId);
    }
}
