package com.example.ikea.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "notice_attachments")
public class NoticeAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long noticeAttachmentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notice_id", nullable = false)
    private Notice notice;

    @Column(nullable = false, length = 255)
    private String originalName;

    @Column(length = 255)
    private String storedName;

    @Column(length = 255)
    private String fileUrl;

    @Column(length = 120)
    private String contentType;

    @Column(nullable = false)
    private Long fileSize;

    @Column(name = "file_data", columnDefinition = "bytea")
    private byte[] fileData;
}
