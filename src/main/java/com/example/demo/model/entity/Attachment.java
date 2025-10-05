package com.example.demo.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Setter
@Getter
@Entity
@Table(name = "attachments", indexes = {
        @Index(name = "idx_attachment_trx", columnList = "transaction_id"),
        @Index(name = "idx_attachment_checksum", columnList = "checksum")
})
public class Attachment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id")
    private Transaction transaction; // может быть null до привязки

    @Column(name = "file_name", length = 256)
    private String fileName;

    @Column(name = "content_type", length = 128)
    private String contentType;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    @Column(name = "checksum", length = 64)
    private String checksum; // sha256/MD5 для дедупликации

    @Column(name = "storage_path", length = 512)
    private String storagePath; // внутренний путь в файловом/облачном хранилище

    @Column(name = "external_url", length = 1024)
    private String externalUrl; // публичная ссылка (если есть)

    @Column(name = "taken_at")
    private Instant takenAt; // когда сделано фото (exif)

}

