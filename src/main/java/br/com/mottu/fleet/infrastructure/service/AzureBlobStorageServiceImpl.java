package br.com.mottu.fleet.infrastructure.service;

import br.com.mottu.fleet.domain.exception.StorageException;
import br.com.mottu.fleet.domain.service.StorageService;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobHttpHeaders;
import com.azure.storage.blob.models.PublicAccessType;
import com.azure.storage.blob.options.BlobParallelUploadOptions;

import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Implementação do StorageService que utiliza o Azure Blob Storage.
 * Esta classe se conecta ao serviço real do Azure (não ao emulador)
 * e é responsável por toda a lógica de upload e gerenciamento de blobs.
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
     * Usado pelo DataInitializer (Seeder) para carregar a planta do pátio.
     *
     * @param containerName O nome do contêiner (ex: "plantas").
     * @param blobName O nome exato do arquivo a ser salvo (ex: "planta-pateo-teste.png").
     * @param data O fluxo de dados do arquivo.
     * @param length O tamanho do arquivo em bytes.
     * @return A URL pública completa do arquivo no Azure Blob Storage.
     * @throws StorageException Se o upload falhar.
     */
    @Override
    public String upload(String containerName, String blobName, InputStream data, long length, String contentType) {
        try {
            BlobContainerClient containerClient = getOrCreateContainer(containerName);
            BlobClient blobClient = containerClient.getBlobClient(blobName);
            BlobHttpHeaders headers = new BlobHttpHeaders().setContentType(contentType);
            BlobParallelUploadOptions options = new BlobParallelUploadOptions(data, length).setHeaders(headers);
            blobClient.uploadWithResponse(options, null, null);
            
            log.info("Arquivo '{}' (Content-Type: {}) salvo com sucesso no contêiner '{}'.", blobName, contentType, containerName);

            return blobClient.getBlobUrl();

        } catch (Exception e) {
            log.error("Falha no upload via InputStream para o blob {}", blobName, e);
            throw new StorageException("Falha ao salvar arquivo (InputStream) no storage.", e);
        }
    }


    /**
     * Faz o upload de um arquivo vindo de um formulário (MultipartFile).
     * Valida o Content-Type para aceitar apenas imagens e define o Content-Type no blob
     * para que o navegador exiba a imagem em vez de forçar o download.
     *
     * @param containerName O nome do contêiner (ex: "fotos").
     * @param file O arquivo enviado pelo usuário.
     * @return A URL pública completa do arquivo no Azure Blob Storage.
     * @throws StorageException Se o upload falhar ou o tipo de arquivo for inválido.
     */
    @Override
    public String upload(String containerName, MultipartFile file) throws StorageException {
        
        // 1. VALIDAÇÃO DE SEGURANÇA: Verifica o tipo de arquivo
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType)) {
            log.warn("Tentativa de upload de tipo de arquivo inválido: {}", contentType);
            throw new StorageException("Tipo de arquivo inválido. Apenas imagens (jpeg, png, gif) são permitidas.");
        }

        try (InputStream inputStream = file.getInputStream()) {
            String originalFilename = file.getOriginalFilename();
            String extension = "";

            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            
            String uniqueBlobName = UUID.randomUUID().toString() + extension;
            BlobContainerClient containerClient = getOrCreateContainer(containerName);
            BlobClient blobClient = containerClient.getBlobClient(uniqueBlobName);

            // 2. CONFIGURAÇÃO DO CONTENT-TYPE (Para o navegador exibir a imagem)
            BlobHttpHeaders headers = new BlobHttpHeaders().setContentType(contentType);
            BlobParallelUploadOptions options = new BlobParallelUploadOptions(inputStream).setHeaders(headers);
            
            blobClient.uploadWithResponse(options, null, null);

            log.info("Arquivo '{}' (Content-Type: {}) salvo com sucesso no contêiner '{}'.", uniqueBlobName, contentType, containerName);
            
            return blobClient.getBlobUrl();
            
        } catch (Exception e) {
            log.error("Falha no upload via MultipartFile para o contêiner {}", containerName, e);
            throw new StorageException("Falha ao salvar arquivo (MultipartFile) no storage.", e);
        }
    }


    /**
     * Método auxiliar que busca um contêiner de blob pelo nome. Se não existir,
     * cria o contêiner com acesso público para leitura de blobs (nível BLOB).
     * TODO: Em prod usar URLs SAS (Shared Access Signature) geradas pela API para acesso temporário e seguro.
     *
     * @param containerName O nome do contêiner.
     * @return O cliente do contêiner (BlobContainerClient).
     */
    private BlobContainerClient getOrCreateContainer(String containerName) {
        BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);
        if (!containerClient.exists()) {
            // Cria o contêiner e já define o nível de acesso público
            containerClient.createWithResponse(
                null, // metadata
                PublicAccessType.BLOB, // public access type
                null, // timeout
                null // context
            );
            log.info("Contêiner de blob '{}' criado com acesso PÚBLICO (nível Blob).", containerName);
        }
        return containerClient;
    }

}