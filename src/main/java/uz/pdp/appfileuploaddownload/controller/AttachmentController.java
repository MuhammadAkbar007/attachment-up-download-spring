package uz.pdp.appfileuploaddownload.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import uz.pdp.appfileuploaddownload.entity.Attachment;
import uz.pdp.appfileuploaddownload.entity.AttachmentContent;
import uz.pdp.appfileuploaddownload.repository.AttachmentContentRepository;
import uz.pdp.appfileuploaddownload.repository.AttachmentRepository;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Optional;
import java.util.UUID;

import javax.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/attachment")
public class AttachmentController {

    @Autowired AttachmentRepository attachmentRepository;
    @Autowired AttachmentContentRepository attachmentContentRepository;

    private static final String uploadDir = "yuklanganlar";

    @PostMapping("/uploadDb")
    public String uploadFileToDb(MultipartHttpServletRequest request) throws IOException {
        /* ko'p qismli request */
        Iterator<String> fileNames = request.getFileNames(); // fayllarni oldi iteratorga
        MultipartFile file = request.getFile(fileNames.next()); // real faylni oldi

        if (file != null) {
            /* File attribtlari table ma'lumotlari */
            String originalFilename = file.getOriginalFilename();
            long size = file.getSize();
            String contentType = file.getContentType();

            Attachment attachment = new Attachment();
            attachment.setContentType(contentType);
            attachment.setFileOriginalName(originalFilename);
            attachment.setSize(size);
            Attachment savedAttachment = attachmentRepository.save(attachment);

            /* File asosiy content(byte[]) table ma'lumotlari */
            AttachmentContent attachmentContent = new AttachmentContent();
            attachmentContent.setMainContent(file.getBytes());
            attachmentContent.setAttachment(savedAttachment);
            attachmentContentRepository.save(attachmentContent);

            return savedAttachment.getId() + "-id lik fayl muvaffaqqiyatli saqlandi !";
        }
        return "Xatolik !";
    }

    @PostMapping("/uploadSystem")
    public String uploadFileToFs(MultipartHttpServletRequest request) throws IOException {

        Iterator<String> fileNames = request.getFileNames(); // requestdan fayllarni ol
        MultipartFile file = request.getFile(fileNames.next()); // requestdan faylni ol

        if (file != null) {
            String originalFilename = file.getOriginalFilename();

            Attachment attachment = new Attachment();
            attachment.setFileOriginalName(originalFilename);
            attachment.setSize(file.getSize());
            attachment.setContentType(file.getContentType());

            assert originalFilename != null;
            String[] split = originalFilename.split("\\."); // . gacha bo'laklash
            String name =
                    UUID.randomUUID().toString()
                            + "."
                            + split[split.length - 1]; // random name ga type ni qo'sh
            attachment.setName(name);
            attachmentRepository.save(attachment);
            Path path = Paths.get(uploadDir + "/" + name); // tushadigan papkaga joyla
            Files.copy(file.getInputStream(), path);

            return attachment.getId() + "-id lik fayl saqlandi !";
        }

        return "Saqlanmadi !";
    }

    @GetMapping("/getFile/{id}")
    public void getFile(@PathVariable Integer id, HttpServletResponse response) throws IOException {

        Optional<Attachment> optionalAttachment = attachmentRepository.findById(id);

        if (optionalAttachment.isPresent()) {
            Attachment attachment = optionalAttachment.get();

            Optional<AttachmentContent> contentOptional =
                    attachmentContentRepository.findByAttachmentId(id);
            if (contentOptional.isPresent()) {
                AttachmentContent attachmentContent = contentOptional.get();

                response.setHeader(
                        "Content-Disposition",
                        "attachment; filename=\""
                                + attachment.getFileOriginalName()
                                + "\""); /* Fayl nomini berib yuborish uchun kerak */
                response.setContentType(
                        attachment
                                .getContentType()); /* Fayl content-type ni berib yuborish uchun kerak */
                /* DB dan bite[] oladi */
                /* chiquvchi responsga yozish */
                FileCopyUtils.copy(
                        attachmentContent.getMainContent(),
                        response.getOutputStream()); /* Fayl bayt[] ini berish uchun */
            }
        }
    }

    @GetMapping("/getFileFS/{id}")
    public void getFileFromFs(@PathVariable Integer id, HttpServletResponse response)
            throws IOException {

        Optional<Attachment> optionalAttachment = attachmentRepository.findById(id);

        if (optionalAttachment.isPresent()) {
            Attachment attachment = optionalAttachment.get();

            response.setHeader(
                    "Content-Disposition",
                    "attachment; filename=\"" + attachment.getFileOriginalName() + "\"");
            response.setContentType(attachment.getContentType());

            FileInputStream inputStream =
                    new FileInputStream(uploadDir + "/" + attachment.getName());

            FileCopyUtils.copy(inputStream, response.getOutputStream());
        }
    }
}
