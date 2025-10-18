package br.com.mottu.fleet.infrastructure.service;

import br.com.mottu.fleet.domain.exception.StorageException;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.PublicAccessType;
import com.azure.storage.blob.models.BlobHttpHeaders;
import com.azure.storage.blob.options.BlobParallelUploadOptions;

import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Implementação do StorageService que utiliza o Azure Blob Storage.
 * Responsável por toda a lógica de upload e gerenciamento de blobs.
 */
@Service
@Primary
public class AzureBlobStorageServiceImpl implements StorageService {

    private static final Logger log = LoggerFactory.getLogger(AzureBlobStorageServiceImpl.class);

    private final String connectionString;
    private BlobServiceClient blobServiceClient;
    private static final List<String> ALLOWED_IMAGE_TYPES = List.of("image/jpeg", "image/png", "image/gif");

    public AzureBlobStorageServiceImpl(@Value("${azure.storage.connection-string}") String connectionString) {
        this.connectionString = connectionString;
    }


    /**
     * Inicializa o cliente do Azure Blob Service usando a string de conexão
     * assim que o bean é construído.
     */
    @PostConstruct
    public void init() {
        this.blobServiceClient = new BlobServiceClientBuilder()
                .connectionString(this.connectionString)
                .buildClient();
    }


    /**
     * Faz o upload de um arquivo genérico a partir de um InputStream.
     * Usado pelo DataInitializer (Seeder).
     * @param containerName O nome do contêiner (ex: "plantas").
     * @param blobName O nome exato do arquivo a ser salvo (ex: "planta-pateo-teste.png").
     * @param data O fluxo de dados do arquivo.
     * @param length O tamanho do arquivo em bytes.
     * @return A URL pública do arquivo.
     * @throws StorageException Se o upload falhar.
     */
    @Override
    public String upload(String containerName, String blobName, InputStream data, long length) {
        BlobContainerClient containerClient = getOrCreateContainer(containerName);
        BlobClient blobClient = containerClient.getBlobClient(blobName);
        
        blobClient.upload(data, length, true);

        log.info("Arquivo '{}' salvo via InputStream no contêiner '{}'.", blobName, containerName);
        return blobClient.getBlobUrl().replace("azurite", "localhost");
    }

    @Override
    public String upload(String containerName, MultipartFile file) throws IOException {
        String originalFilename = file.getOriginalFilename();
        String extension = "";

        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        
        String uniqueBlobName = UUID.randomUUID().toString() + extension;        
        BlobContainerClient containerClient = getOrCreateContainer(containerName);
        BlobClient blobClient = containerClient.getBlobClient(uniqueBlobName);
        BlobHttpHeaders headers = new BlobHttpHeaders().setContentType(file.getContentType());
        BlobParallelUploadOptions options = new BlobParallelUploadOptions(file.getInputStream()).setHeaders(headers);
        
        blobClient.uploadWithResponse(options, null, null);

        log.info("Arquivo '{}' (Content-Type: {}) salvo com sucesso no contêiner '{}'.", uniqueBlobName, file.getContentType(), containerName);
        return blobClient.getBlobUrl().replace("azurite", "localhost");
    }

    /**
     * Método auxiliar que busca um contêiner de blob pelo nome. Se não existir,
     * cria o contêiner com acesso público para leitura de blobs.
     * @param containerName O nome do contêiner.
     * @return O cliente do contêiner.
     */
    private BlobContainerClient getOrCreateContainer(String containerName) {
        BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);
        if (!containerClient.exists()) {
            containerClient.createWithResponse(
                null, // metadata
                PublicAccessType.BLOB, // public access type
                null, // timeout
                null  // context
            );
            log.info("Contêiner de blob '{}' criado com acesso PÚBLICO.", containerName);
        }
        return containerClient;
    }
}
