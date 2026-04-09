package com.example.chatApp.repository;

<<<<<<< HEAD
=======
import com.example.chatApp.model.MediaFile;
>>>>>>> 336049e9327ef3bc762643b5dee206ef27479048
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.chatApp.model.MediaFile;

import java.util.List;

@Repository
public interface MediaFileRepository extends JpaRepository<MediaFile, Long> {

    List<MediaFile> findByUploaderId(Long uploaderId);

    List<MediaFile> findByFileType(String fileType);
}
