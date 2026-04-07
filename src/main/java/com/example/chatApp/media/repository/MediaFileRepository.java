package com.example.chatApp.media.repository;

import com.example.chatApp.media.model.MediaFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MediaFileRepository extends JpaRepository<MediaFile, Long> {

    List<MediaFile> findByUploaderId(Long uploaderId);

    List<MediaFile> findByFileType(String fileType);
}
