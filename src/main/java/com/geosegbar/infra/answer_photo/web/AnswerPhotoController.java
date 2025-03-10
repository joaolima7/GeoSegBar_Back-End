package com.geosegbar.infra.answer_photo.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.geosegbar.common.response.WebResponseEntity;
import com.geosegbar.entities.AnswerPhotoEntity;
import com.geosegbar.infra.answer_photo.services.AnswerPhotoService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/answer-photos")
@RequiredArgsConstructor
public class AnswerPhotoController {

    private final AnswerPhotoService answerPhotoService;

    @GetMapping
    public ResponseEntity<WebResponseEntity<List<AnswerPhotoEntity>>> getAllAnswerPhotos() {
        List<AnswerPhotoEntity> photos = answerPhotoService.findAll();
        WebResponseEntity<List<AnswerPhotoEntity>> response = WebResponseEntity.success(photos, "Fotos de resposta obtidas com sucesso!");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<WebResponseEntity<AnswerPhotoEntity>> getAnswerPhotoById(@PathVariable Long id) {
        AnswerPhotoEntity photo = answerPhotoService.findById(id);
        WebResponseEntity<AnswerPhotoEntity> response = WebResponseEntity.success(photo, "Foto de resposta obtida com sucesso!");
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/answer/{answerId}")
    public ResponseEntity<WebResponseEntity<List<AnswerPhotoEntity>>> getAnswerPhotosByAnswerId(@PathVariable Long answerId) {
        List<AnswerPhotoEntity> photos = answerPhotoService.findByAnswerId(answerId);
        WebResponseEntity<List<AnswerPhotoEntity>> response = WebResponseEntity.success(photos, "Fotos da resposta obtidas com sucesso!");
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<WebResponseEntity<AnswerPhotoEntity>> uploadAnswerPhoto(
            @RequestParam("answerId") Long answerId,
            @RequestParam("photo") MultipartFile photo) {
        AnswerPhotoEntity created = answerPhotoService.savePhoto(answerId, photo);
        WebResponseEntity<AnswerPhotoEntity> response = WebResponseEntity.success(created, "Foto de resposta criada com sucesso!");
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<WebResponseEntity<AnswerPhotoEntity>> updateAnswerPhoto(
            @PathVariable Long id,
            @RequestParam("photo") MultipartFile photo) {
        AnswerPhotoEntity updated = answerPhotoService.updatePhoto(id, photo);
        WebResponseEntity<AnswerPhotoEntity> response = WebResponseEntity.success(updated, "Foto de resposta atualizada com sucesso!");
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<WebResponseEntity<Void>> deleteAnswerPhoto(@PathVariable Long id) {
        answerPhotoService.deleteById(id);
        WebResponseEntity<Void> response = WebResponseEntity.success(null, "Foto de resposta excluída com sucesso!");
        return ResponseEntity.ok(response);
    }
}