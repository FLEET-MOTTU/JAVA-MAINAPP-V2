package br.com.mottu.fleet.domain.service;

import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

public interface StorageService {

    /**
     * Método genérico para upload. Recebe os dados brutos do arquivo.
     * @return A URL pública do arquivo após o upload.
     */
    String upload(String containerName, String blobName, InputStream data, long length);

    /**
     * Método de conveniência para uploads vindos de formulários web.
     * Ele extrai os dados do MultipartFile e chama o método principal.
     * @return A URL pública do arquivo após o upload.
     */
    default String upload(String containerName, MultipartFile file) throws IOException {
        String originalFilename = file.getOriginalFilename();
        String extension = "";

        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        String uniqueBlobName = UUID.randomUUID().toString() + extension;
        return this.upload(containerName, uniqueBlobName, file.getInputStream(), file.getSize());
    }
}