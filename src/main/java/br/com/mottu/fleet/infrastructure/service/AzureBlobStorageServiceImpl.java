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
import com.azure.storage.blob.sas.BlobSasPermission;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;

import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Implementação do StorageService que se conecta ao Azure Blob Storage.
 * Esta classe é profile-aware:
 * - Em "dev", usa o emulador do Azure Blob Storage (Azurite), cria contêineres públicos e ajusta as URLs para localhost.
 * - Em "prod", conecta ao Azure real, cria contêineres privados e expõe a lógica para gerar URLs SAS.
 */
@Service
@Primary
public class AzureBlobStorageServiceImpl implements StorageService {

    private static final Logger log = LoggerFactory.getLogger(AzureBlobStorageServiceImpl.class);

    private final String connectionString;
    private BlobServiceClient blobServiceClient;

    private static final List<String> ALLOWED_IMAGE_TYPES = List.of("image/jpeg", "image/png", "image/gif");

    @Value("${spring.profiles.active:prod}")
    private String activeProfile;

    /**
     * Construtor que injeta a string de conexão.
     * O valor injetado virá de 'application-dev.properties' (Azurite) ou
     * 'application.properties' (Azure real).
     * @param connectionString A string de conexão do storage.
     */
    public AzureBlobStorageServiceImpl(@Value("${azure.storage.connection-string}") String connectionString) {
        this.connectionString = connectionString;
    }


    /**
     * Inicializa o cliente do Azure Blob Service usando a string de conexão
     */
    @PostConstruct
    public void init() {
        this.blobServiceClient = new BlobServiceClientBuilder()
                .connectionString(this.connectionString)
                .buildClient();
    }

    /**
     * Implementação principal de upload.
     * Método base usado tanto pelo seeder quanto pelo upload de formulário.
     * @param containerName O nome do contêiner (ex: "plantas").
     * @param blobName O nome exato do arquivo a ser salvo.
     * @param data O fluxo de dados do arquivo.
     * @param length O tamanho do arquivo em bytes.
     * @param contentType O tipo de mídia do arquivo (ex: "image/png").
     * @return A URL acessível do arquivo.
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
            
            if ("dev".equals(activeProfile)) {
                return blobClient.getBlobUrl().replace("azurite", "localhost");
            }

            return blobClient.getBlobUrl();

        } catch (Exception e) {
            log.error("Falha no upload via InputStream para o blob {}", blobName, e);
            throw new StorageException("Falha ao salvar arquivo (InputStream) no storage.", e);
        }
    }


    /**
     * Implementação do método para uploads vindos de MultipartFile.
     * Valida o Content-Type e chama o método de upload principal.
     * @param containerName O nome do contêiner (ex: "fotos").
     * @param file O arquivo enviado pelo usuário.
     * @return A URL acessível do arquivo.
     * @throws StorageException Se o upload falhar ou o tipo de arquivo for inválido.
     */
    @Override
    public String upload(String containerName, MultipartFile file) throws StorageException {
        
        // VALIDAÇÃO DE SEGURANÇA: Verifica o tipo de arquivo
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

            return this.upload(containerName, uniqueBlobName, inputStream, file.getSize(), contentType);
            
        } catch (Exception e) {
            log.error("Falha no upload via MultipartFile para o contêiner {}", containerName, e);
            throw new StorageException("Falha ao salvar arquivo (MultipartFile) no storage.", e);
        }
    }


    /**
     * Gera uma URL de acesso temporário (SAS Token) para um blob privado. (Prod)
     * @param containerName O nome do contêiner.
     * @param blobName O nome do arquivo (ex: "foto-uuid.png").
     * @return Uma URL completa com um token de acesso válido por 5 minutos.
     */
    @Override
    public String gerarUrlAcessoTemporario(String containerName, String blobName) {

        // Pega o cliente para o blob específico
        BlobClient blobClient = blobServiceClient.getBlobContainerClient(containerName).getBlobClient(blobName);

        // Define as permissões (apenas Leitura) e a validade (5 minutos)
        BlobSasPermission permission = new BlobSasPermission().setReadPermission(true);
        OffsetDateTime expiryTime = OffsetDateTime.now().plusMinutes(5);
        BlobServiceSasSignatureValues sasValues = new BlobServiceSasSignatureValues(expiryTime, permission);

        // Gera o token SAS e o anexa à URL do blob
        String sasToken = blobClient.generateSas(sasValues);
        return blobClient.getBlobUrl() + "?" + sasToken;
    }


    /**
     * Método auxiliar que cria um contêiner se ele não existir.
     * @param containerName O nome do contêiner.
     * @return O cliente do contêiner (BlobContainerClient).
     */
    private BlobContainerClient getOrCreateContainer(String containerName) {
        BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);
        if (!containerClient.exists()) {
            
            if ("dev".equals(activeProfile)) {
                containerClient.createWithResponse(null, PublicAccessType.BLOB, null, null);
                log.info("Contêiner de blob '{}' criado com acesso PÚBLICO (Perfil DEV).", containerName);
            } else {
                containerClient.create();
                log.info("Contêiner de blob '{}' criado com acesso PRIVADO (Perfil PROD).", containerName);
            }
        }
        return containerClient;
    }

}