package com.example.chatApp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.chatApp.model.MediaFile;

import java.util.List;

@Repository
public interface MediaFileRepository extends JpaRepository<MediaFile, Long> {

    List<MediaFile> findByUploaderId(Long uploaderId);

    List<MediaFile> findByFileType(String fileType);
}
