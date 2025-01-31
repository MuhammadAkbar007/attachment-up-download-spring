package uz.pdp.appfileuploaddownload.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import uz.pdp.appfileuploaddownload.entity.Attachment;

public interface AttachmentRepository extends JpaRepository<Attachment, Integer> {}
