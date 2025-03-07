package br.com.archbase.ddd.infraestructure.aspect;

import br.com.archbase.ddd.domain.aspect.annotations.StorageField;
import br.com.archbase.ddd.domain.contracts.ArchbaseStoragePort;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Id;
import jakarta.persistence.PersistenceContext;
import org.apache.tika.Tika;
import org.apache.tika.mime.MimeTypes;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Aspecto responsável por processar campos anotados com {@link StorageField} antes de salvar ou deletar entidades.
 */
//@Aspect
//@Component
//@ConditionalOnBean(ArchbaseStoragePort.class)
public class ArchbaseStorageFieldAspect {

    private static final Logger logger = LoggerFactory.getLogger(ArchbaseStorageFieldAspect.class);

    @Autowired
    private ArchbaseStoragePort storagePort;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    private final Tika tika = new Tika();
    private final MimeTypes allTypes = MimeTypes.getDefaultMimeTypes();

    // ThreadLocal para rastrear entidades já processadas na transação atual
    private final ThreadLocal<Set<ProcessedEntity>> processedEntities = ThreadLocal.withInitial(HashSet::new);

    // Classe auxiliar para identificar entidades já processadas
    private static class ProcessedEntity {
        private final Object entity;
        private final String operation;

        public ProcessedEntity(Object entity, String operation) {
            this.entity = entity;
            this.operation = operation;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ProcessedEntity that = (ProcessedEntity) o;
            return entity == that.entity && operation.equals(that.operation);
        }

        @Override
        public int hashCode() {
            return 31 * System.identityHashCode(entity) + operation.hashCode();
        }
    }

    /**
     * Pointcut que corresponde à execução de qualquer método save* nos repositórios.
     */
    @Pointcut("execution(* br.com.archbase.ddd.domain.contracts.Repository+.save*(..))")
    public void repositorySave() {}

    /**
     * Pointcut que corresponde à execução de qualquer método delete* nos repositórios.
     */
    @Pointcut("execution(* br.com.archbase.ddd.domain.contracts.Repository+.delete*(..))")
    public void repositoryDelete() {}

    /**
     * Pointcut para o método merge do EntityManager.
     */
    @Pointcut("execution(* jakarta.persistence.EntityManager.merge(..))")
    public void entityManagerMerge() {}

    /**
     * Pointcut para o método persist do EntityManager.
     */
    @Pointcut("execution(* jakarta.persistence.EntityManager.persist(..))")
    public void entityManagerPersist() {}

    /**
     * Pointcut para o método remove do EntityManager.
     */
    @Pointcut("execution(* jakarta.persistence.EntityManager.remove(..))")
    public void entityManagerRemove() {}

    /**
     * Advice que é executado antes de qualquer método de salvamento.
     */
    @Before("repositorySave() || entityManagerMerge() || entityManagerPersist()")
    public void beforeSave(JoinPoint joinPoint) throws Exception {
        // Registra início de nova transação se ainda não estiver ativa
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            logger.debug("Iniciando nova transação para processamento de StorageField");
            registerTransactionCleanup();
        }

        // Determina o tipo de operação
        String operationType = determineOperationType(joinPoint);

        // Processa os argumentos
        Object[] args = joinPoint.getArgs();
        for (Object arg : args) {
            if (arg != null) {
                if (arg instanceof Iterable) {
                    for (Object entity : (Iterable<?>) arg) {
                        processSingleEntity(entity, operationType);
                    }
                } else {
                    processSingleEntity(arg, operationType);
                }
            }
        }
    }

    /**
     * Processa uma única entidade, verificando se já foi processada anteriormente.
     */
    private void processSingleEntity(Object entity, String operationType) throws Exception {
        // Verifica se a entidade já foi processada nesta transação
        ProcessedEntity processedEntity = new ProcessedEntity(entity, operationType);
        if (!processedEntities.get().contains(processedEntity)) {
            if (hasStorageFields(entity.getClass())) {
                logger.debug("Processando entidade {} para operação {}", entity.getClass().getSimpleName(), operationType);
                processStorageFields(entity, isUpdate(entity));
                processedEntities.get().add(processedEntity);
            }
        } else {
            logger.debug("Entidade {} já processada para operação {}", entity.getClass().getSimpleName(), operationType);
        }
    }

    /**
     * Verifica rapidamente se a classe possui campos anotados com StorageField.
     */
    private boolean hasStorageFields(Class<?> clazz) {
        while (clazz != null) {
            for (Field field : clazz.getDeclaredFields()) {
                if (field.isAnnotationPresent(StorageField.class)) {
                    return true;
                }
            }
            clazz = clazz.getSuperclass();
        }
        return false;
    }

    /**
     * Determina o tipo de operação com base no método interceptado.
     */
    private String determineOperationType(JoinPoint joinPoint) {
        String methodName = joinPoint.getSignature().getName();
        if (methodName.startsWith("save") || methodName.equals("merge") || methodName.equals("persist")) {
            return "SAVE";
        } else if (methodName.startsWith("delete") || methodName.equals("remove")) {
            return "DELETE";
        }
        return methodName.toUpperCase();
    }

    /**
     * Registra um callback para limpar o ThreadLocal no final da transação.
     */
    private void registerTransactionCleanup() {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                @Override
                public void afterCompletion(int status) {
                    logger.debug("Limpando cache de entidades processadas após transação");
                    processedEntities.get().clear();
                }
            });
        }
    }

    /**
     * Advice que é executado antes de qualquer método de exclusão.
     */
    @Before("repositoryDelete() || entityManagerRemove()")
    public void beforeDelete(JoinPoint joinPoint) throws Exception {
        // Registra início de nova transação se ainda não estiver ativa
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            registerTransactionCleanup();
        }

        String operationType = "DELETE";

        Object[] args = joinPoint.getArgs();
        for (Object arg : args) {
            if (arg != null) {
                if (arg instanceof Iterable) {
                    for (Object entity : (Iterable<?>) arg) {
                        ProcessedEntity processedEntity = new ProcessedEntity(entity, operationType);
                        if (!processedEntities.get().contains(processedEntity)) {
                            if (hasStorageFields(entity.getClass())) {
                                deleteStorageFiles(entity);
                                processedEntities.get().add(processedEntity);
                            }
                        }
                    }
                } else {
                    ProcessedEntity processedEntity = new ProcessedEntity(arg, operationType);
                    if (!processedEntities.get().contains(processedEntity)) {
                        if (hasStorageFields(arg.getClass())) {
                            deleteStorageFiles(arg);
                            processedEntities.get().add(processedEntity);
                        }
                    }
                }
            }
        }
    }

    /**
     * Limpa o ThreadLocal após o fim da execução para evitar vazamentos de memória.
     */
    @After("(repositorySave() || entityManagerMerge() || entityManagerPersist() || " +
            "repositoryDelete() || entityManagerRemove()) && !within(ArchbaseStorageFieldAspect)")
    public void afterOperation() {
        // Se não houver uma transação ativa, limpe o ThreadLocal imediatamente
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            processedEntities.get().clear();
        }
    }

    /**
     * Processa os campos anotados com {@link StorageField} na entidade fornecida.
     */
    private void processStorageFields(Object entity, boolean isUpdate) throws Exception {
        Class<?> clazz = entity.getClass();

        Object oldEntity = null;
        if (isUpdate) {
            Object idValue = getEntityId(entity);
            if (idValue != null) {
                // Usa um EntityManager separado para evitar desvincular a entidade atual
                EntityManager tempEntityManager = entityManagerFactory.createEntityManager();
                try {
                    oldEntity = tempEntityManager.find(clazz, idValue);
                } finally {
                    tempEntityManager.close();
                }
            }
        }

        for (Field field : getAllFields(clazz)) {
            if (field.isAnnotationPresent(StorageField.class)) {
                field.setAccessible(true);
                Object newValue = field.get(entity);
                String oldFileUrl = null;

                if (isUpdate && oldEntity != null) {
                    Field oldField = getFieldFromClassHierarchy(oldEntity.getClass(), field.getName());
                    oldField.setAccessible(true);
                    Object oldValue = oldField.get(oldEntity);
                    if (oldValue instanceof byte[]) {
                        oldFileUrl = extractFileUrl((byte[]) oldValue);
                    }
                }

                if (newValue instanceof byte[]) {
                    byte[] data = (byte[]) newValue;
                    boolean isUrl = isUrlData(data);

                    if (isUrl) {
                        String newFileUrl = new String(data, StandardCharsets.UTF_8);

                        // Se a nova URL é diferente da antiga, deleta o arquivo antigo
                        if (isUpdate && oldFileUrl != null && !newFileUrl.equals(oldFileUrl)) {
                            deleteFileFromStorage(oldFileUrl);
                        }

                        // Mantém a URL como está
                        field.set(entity, data);
                    } else {
                        // É dado binário; processa e faz upload do arquivo
                        String newFileUrl = processFileData(data, oldFileUrl);

                        // Substitui o conteúdo do campo pela nova URL
                        if (newFileUrl != null) {
                            field.set(entity, newFileUrl.getBytes(StandardCharsets.UTF_8));
                            logger.debug("Campo de armazenamento processado: {} em {}", field.getName(), entity.getClass().getSimpleName());
                        } else {
                            field.set(entity, null);
                        }
                    }
                }
            }
        }
    }

    /**
     * Recupera o valor do campo ID da entidade, que está anotado com {@link Id}.
     */
    private Object getEntityId(Object entity) throws IllegalAccessException {
        Class<?> clazz = entity.getClass();
        while (clazz != null) {
            for (Field field : clazz.getDeclaredFields()) {
                if (field.isAnnotationPresent(Id.class)) {
                    field.setAccessible(true);
                    return field.get(entity);
                }
            }
            clazz = clazz.getSuperclass();
        }
        return null;
    }

    /**
     * Determina se a entidade está sendo atualizada ou criada com base na presença de um ID.
     */
    private boolean isUpdate(Object entity) {
        try {
            Object idValue = getEntityId(entity);
            return idValue != null;
        } catch (IllegalAccessException e) {
            return false;
        }
    }

    /**
     * Processa os dados do arquivo, fazendo upload para o armazenamento e retornando a URL do arquivo.
     */
    private String processFileData(byte[] fileData, String oldFileUrl) {
        if (fileData == null || fileData.length == 0) {
            // Sem novo conteúdo; deleta o arquivo antigo se necessário
            if (StringUtils.hasText(oldFileUrl)) {
                deleteFileFromStorage(oldFileUrl);
            }
            return null;
        }

        try {
            String content = new String(fileData, StandardCharsets.UTF_8);

            // Verifica se o conteúdo é uma URL de dados
            if (content.startsWith("data:")) {
                return handleDataUrl(content, oldFileUrl);
            }

            // Assume que é dado binário bruto
            String contentType = tika.detect(fileData);
            String newFileUrl = uploadContent(fileData, contentType);

            // Deleta o arquivo antigo se necessário
            if (StringUtils.hasText(oldFileUrl) && !newFileUrl.equals(oldFileUrl)) {
                logger.debug("Deletando arquivo antigo: {}", oldFileUrl);
                deleteFileFromStorage(oldFileUrl);
            }

            return newFileUrl;

        } catch (Exception e) {
            logger.error("Erro ao processar dados do arquivo", e);
            throw new RuntimeException("Erro ao processar dados do arquivo: " + e.getMessage(), e);
        }
    }

    /**
     * Manipula URLs de dados decodificando o conteúdo base64 e fazendo upload do arquivo.
     */
    private String handleDataUrl(String dataUrl, String oldFileUrl) throws Exception {
        String[] parts = dataUrl.split(",", 2);
        if (parts.length < 2) {
            throw new IllegalArgumentException("Data URL inválida");
        }
        String metadata = parts[0];
        String base64Data = parts[1];

        String contentType = "application/octet-stream"; // Tipo de conteúdo padrão
        if (metadata.contains(":") && metadata.contains(";")) {
            contentType = metadata.substring(metadata.indexOf(":") + 1, metadata.indexOf(";"));
        }

        byte[] decodedBytes = Base64.getDecoder().decode(base64Data);
        String newFileUrl = uploadContent(decodedBytes, contentType);

        // Deleta o arquivo antigo se necessário
        if (StringUtils.hasText(oldFileUrl) && !newFileUrl.equals(oldFileUrl)) {
            logger.debug("Deletando arquivo antigo após upload de data URL: {}", oldFileUrl);
            deleteFileFromStorage(oldFileUrl);
        }

        return newFileUrl;
    }

    /**
     * Deleta arquivos associados a campos anotados com {@link StorageField} na entidade fornecida.
     */
    private void deleteStorageFiles(Object entity) throws IllegalAccessException {
        Class<?> clazz = entity.getClass();
        for (Field field : getAllFields(clazz)) {
            if (field.isAnnotationPresent(StorageField.class)) {
                field.setAccessible(true);
                Object value = field.get(entity);

                if (value instanceof byte[]) {
                    String fileUrl = extractFileUrl((byte[]) value);
                    if (fileUrl != null) {
                        logger.debug("Deletando arquivo armazenado para campo {} da entidade {}: {}",
                                field.getName(), entity.getClass().getSimpleName(), fileUrl);
                        deleteFileFromStorage(fileUrl);
                    }
                }
            }
        }
    }

    /**
     * Deleta um arquivo do armazenamento dado sua URL.
     */
    private void deleteFileFromStorage(String fileUrl) {
        if (isValidUrl(fileUrl)) {
            try {
                String objectName = getObjectNameFromUrl(fileUrl);
                if (objectName != null) {
                    storagePort.deleteFile(objectName);
                    logger.debug("Arquivo excluído com sucesso: {}", objectName);
                }
            } catch (Exception e) {
                logger.error("Erro ao deletar arquivo: {}", fileUrl, e);
            }
        }
    }

    /**
     * Determina se os dados byte[] fornecidos representam uma URL.
     */
    private boolean isUrlData(byte[] data) {
        if (data == null || data.length == 0) {
            return false;
        }
        String content = new String(data, StandardCharsets.UTF_8);
        return isValidUrl(content);
    }

    /**
     * Extrai a URL do arquivo dos dados byte[] se representar uma URL válida.
     */
    private String extractFileUrl(byte[] data) {
        if (data == null || data.length == 0) {
            return null;
        }
        String content = new String(data, StandardCharsets.UTF_8);
        if (isValidUrl(content)) {
            return content;
        }
        return null;
    }

    /**
     * Verifica se a string fornecida é uma URL HTTP ou HTTPS válida.
     */
    private boolean isValidUrl(String url) {
        if (!StringUtils.hasText(url)) {
            return false;
        }
        try {
            URI uri = new URI(url);
            return "http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme());
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Extrai o nome do objeto da URL do arquivo para uso em operações de armazenamento.
     */
    private String getObjectNameFromUrl(String url) {
        // Dividir a URL por "/"
        String[] parts = url.split("/");

        // Procurar a última parte antes dos parâmetros de consulta
        String lastPart = "";
        for (String part : parts) {
            // Se encontrarmos parâmetros de consulta, quebramos a parte
            if (part.contains("?")) {
                lastPart = part.substring(0, part.indexOf("?"));
                break;
            }
            // Caso contrário, apenas atualizamos a última parte
            if (!part.isEmpty()) {
                lastPart = part;
            }
        }

        // Se não encontramos uma parte com "?", a última parte já é a que queremos
        if (lastPart.isEmpty() && parts.length > 0) {
            lastPart = parts[parts.length - 1];
            // Verificar se há parâmetros de consulta
            if (lastPart.contains("?")) {
                lastPart = lastPart.substring(0, lastPart.indexOf("?"));
            }
        }

        // Verificar se o nome do arquivo começa com "arquivos_"
        if (lastPart.startsWith("arquivos_")) {
            return lastPart;
        }

        return null; // Não encontrou um nome de arquivo válido
    }

    /**
     * Faz upload do conteúdo para o armazenamento e retorna a URL do arquivo.
     */
    private String uploadContent(byte[] content, String contentType) throws Exception {
        String extension = getExtensionFromContentType(contentType);
        // Adiciona UUID para garantir unicidade mesmo que o timestamp seja o mesmo
        String objectName = "arquivos_" + System.currentTimeMillis() + "_" +
                UUID.randomUUID().toString().substring(0, 8) +
                sanitizeFileName(extension);

        InputStream inputStream = new ByteArrayInputStream(content);
        String result = storagePort.uploadFile(objectName, inputStream, contentType);
        logger.debug("Arquivo enviado com sucesso: {} ({})", objectName, contentType);
        return result;
    }

    /**
     * Sanitiza o nome do arquivo substituindo caracteres inválidos.
     */
    private String sanitizeFileName(String fileName) {
        return fileName.replaceAll("[^a-zA-Z0-9.-]", "_");
    }

    /**
     * Recupera a extensão do arquivo com base no tipo de conteúdo.
     */
    private String getExtensionFromContentType(String contentType) throws Exception {
        try {
            return allTypes.forName(contentType).getExtension();
        } catch (Exception e) {
            logger.warn("Não foi possível determinar a extensão para o tipo de conteúdo: {}", contentType);
            // Extensão padrão para tipos de conteúdo desconhecidos
            return ".bin";
        }
    }

    /**
     * Recupera todos os campos da classe e suas superclasses.
     */
    private List<Field> getAllFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        while (clazz != null && !clazz.equals(Object.class)) {
            Field[] declaredFields = clazz.getDeclaredFields();
            for (Field field : declaredFields) {
                fields.add(field);
            }
            clazz = clazz.getSuperclass();
        }
        return fields;
    }

    /**
     * Recupera um campo da hierarquia de classes pelo nome.
     */
    private Field getFieldFromClassHierarchy(Class<?> clazz, String fieldName) throws NoSuchFieldException {
        while (clazz != null && !clazz.equals(Object.class)) {
            try {
                return clazz.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        throw new NoSuchFieldException("Campo não encontrado: " + fieldName);
    }
}