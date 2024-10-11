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
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Aspecto responsável por processar campos anotados com {@link StorageField} antes de salvar ou deletar entidades.
 * <p>
 * Este aspecto intercepta métodos de repositórios para salvar e deletar entidades. Ele processa campos anotados com
 * {@link StorageField} para manipular o armazenamento de dados de arquivos. As principais funcionalidades incluem:
 * <ul>
 *   <li>Fazer upload de novos arquivos para o armazenamento e substituir o valor do campo pela URL do arquivo.</li>
 *   <li>Deletar arquivos antigos do armazenamento quando não são mais necessários.</li>
 *   <li>Manipular tanto dados binários quanto URLs armazenados em campos do tipo byte[].</li>
 * </ul>
 * O aspecto trabalha com campos do tipo byte[], que podem conter:
 * <ul>
 *   <li>Dados binários do arquivo.</li>
 *   <li>URL armazenada como bytes codificados em UTF-8.</li>
 * </ul>
 */
@Aspect
@Component
@ConditionalOnBean(ArchbaseStoragePort.class)
public class ArchbaseStorageFieldAspect {

    @Autowired
    private ArchbaseStoragePort storagePort;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    private final Tika tika = new Tika();
    private final MimeTypes allTypes = MimeTypes.getDefaultMimeTypes();

    /**
     * Pointcut que corresponde à execução de qualquer método save* em repositórios que estendem a interface base Repository.
     */
    @Pointcut("execution(* br.com.archbase.ddd.domain.contracts.Repository+.save*(..))")
    public void repositorySave() {}

    /**
     * Pointcut que corresponde à execução de qualquer método delete* em repositórios que estendem a interface base Repository.
     */
    @Pointcut("execution(* br.com.archbase.ddd.domain.contracts.Repository+.delete*(..))")
    public void repositoryDelete() {}

    /**
     * Advice que é executado antes de qualquer método save do repositório. Processa os campos de armazenamento na entidade para manipular os dados de arquivo.
     *
     * @param joinPoint o join point representando a execução do método
     * @throws Exception se ocorrer um erro durante o processamento
     */
    @Before("repositorySave()")
    public void beforeSave(JoinPoint joinPoint) throws Exception {
        Object[] args = joinPoint.getArgs();
        for (Object arg : args) {
            if (arg != null) {
                if (arg instanceof Iterable) {
                    for (Object entity : (Iterable<?>) arg) {
                        processStorageFields(entity, isUpdate(entity));
                    }
                } else {
                    processStorageFields(arg, isUpdate(arg));
                }
            }
        }
    }

    /**
     * Advice que é executado antes de qualquer método delete do repositório. Deleta arquivos associados do armazenamento.
     *
     * @param joinPoint o join point representando a execução do método
     * @throws Exception se ocorrer um erro durante o processamento
     */
    @Before("repositoryDelete()")
    public void beforeDelete(JoinPoint joinPoint) throws Exception {
        Object[] args = joinPoint.getArgs();
        for (Object arg : args) {
            if (arg != null) {
                if (arg instanceof Iterable) {
                    for (Object entity : (Iterable<?>) arg) {
                        deleteStorageFiles(entity);
                    }
                } else {
                    deleteStorageFiles(arg);
                }
            }
        }
    }

    /**
     * Processa os campos anotados com {@link StorageField} na entidade fornecida.
     * <p>
     * Se o campo contiver dados binários, faz o upload do arquivo para o armazenamento e substitui o valor do campo pela URL do arquivo.
     * Se o campo contiver uma URL, mantém como está ou atualiza se necessário.
     *
     * @param entity   a entidade contendo os campos de armazenamento
     * @param isUpdate indica se a operação é uma atualização (true) ou uma criação (false)
     * @throws Exception se ocorrer um erro durante o processamento
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

                        // Salva a nova URL como está
                        field.set(entity, data);
                    } else {
                        // É dado binário; processa e faz upload do arquivo
                        String newFileUrl = processFileData(data, oldFileUrl);

                        // Substitui o conteúdo do campo pela nova URL
                        if (newFileUrl != null) {
                            field.set(entity, newFileUrl.getBytes(StandardCharsets.UTF_8));
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
     *
     * @param entity a entidade da qual recuperar o ID
     * @return o valor do campo ID, ou null se não encontrado
     * @throws IllegalAccessException se não for possível acessar o campo ID
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
     *
     * @param entity a entidade a ser verificada
     * @return true se a entidade tem um ID (atualização), false caso contrário (criação)
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
     * <p>
     * Manipula URLs de dados (por exemplo, "data:image/png;base64,...") e dados binários brutos.
     *
     * @param fileData   os dados do arquivo a serem processados
     * @param oldFileUrl a URL do arquivo antigo a ser deletado, se necessário
     * @return a URL do arquivo enviado, ou null se nenhum arquivo foi enviado
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
            if (StringUtils.hasText(oldFileUrl)) {
                deleteFileFromStorage(oldFileUrl);
            }

            return newFileUrl;

        } catch (Exception e) {
            throw new RuntimeException("Erro ao processar dados do arquivo: " + e.getMessage(), e);
        }
    }

    /**
     * Manipula URLs de dados decodificando o conteúdo base64 e fazendo upload do arquivo.
     *
     * @param dataUrl    a URL de dados contendo os dados do arquivo
     * @param oldFileUrl a URL do arquivo antigo a ser deletado, se necessário
     * @return a URL do arquivo enviado
     * @throws Exception se ocorrer um erro durante o processamento
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
        if (StringUtils.hasText(oldFileUrl)) {
            deleteFileFromStorage(oldFileUrl);
        }

        return newFileUrl;
    }

    /**
     * Deleta arquivos associados a campos anotados com {@link StorageField} na entidade fornecida.
     *
     * @param entity a entidade cujos arquivos serão deletados
     * @throws IllegalAccessException se não for possível acessar os valores dos campos
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
                        deleteFileFromStorage(fileUrl);
                    }
                }
            }
        }
    }

    /**
     * Deleta um arquivo do armazenamento dado sua URL.
     *
     * @param fileUrl a URL do arquivo a ser deletado
     */
    private void deleteFileFromStorage(String fileUrl) {
        if (isValidUrl(fileUrl)) {
            try {
                String objectName = getObjectNameFromUrl(fileUrl);
                storagePort.deleteFile(objectName);
            } catch (Exception e) {
                // Logar a exceção usando um framework de logging
                System.err.println("Erro ao deletar arquivo antigo: " + e.getMessage());
            }
        }
    }

    /**
     * Determina se os dados byte[] fornecidos representam uma URL.
     *
     * @param data os dados a serem verificados
     * @return true se os dados representam uma URL, false caso contrário
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
     *
     * @param data os dados contendo a URL
     * @return a URL do arquivo como uma string, ou null se não for uma URL válida
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
     *
     * @param url a string a ser verificada
     * @return true se válida, false caso contrário
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
     *
     * @param url a URL do arquivo
     * @return o nome do objeto no armazenamento
     */
    private String getObjectNameFromUrl(String url) {
        try {
            URI uri = new URI(url);
            String path = uri.getPath();

            String storagePath = storagePort.getStoragePath();
            int index = path.indexOf(storagePath);
            if (index == -1) {
                throw new IllegalArgumentException("URL não contém o caminho de armazenamento esperado: " + url);
            }
            return path.substring(index + storagePath.length());
        } catch (Exception e) {
            throw new IllegalArgumentException("URL inválida: " + url, e);
        }
    }

    /**
     * Faz upload do conteúdo para o armazenamento e retorna a URL do arquivo.
     *
     * @param content     o conteúdo a ser enviado
     * @param contentType o tipo MIME do conteúdo
     * @return a URL do arquivo enviado
     * @throws Exception se ocorrer um erro durante o upload
     */
    private String uploadContent(byte[] content, String contentType) throws Exception {
        String extension = getExtensionFromContentType(contentType);
        String objectName = "arquivos/" + System.currentTimeMillis() + sanitizeFileName(extension);

        InputStream inputStream = new ByteArrayInputStream(content);
        return storagePort.uploadFile(objectName, inputStream, contentType);
    }

    /**
     * Sanitiza o nome do arquivo substituindo caracteres inválidos.
     *
     * @param fileName o nome original do arquivo
     * @return o nome do arquivo sanitizado
     */
    private String sanitizeFileName(String fileName) {
        return fileName.replaceAll("[^a-zA-Z0-9.-]", "_");
    }

    /**
     * Recupera a extensão do arquivo com base no tipo de conteúdo.
     *
     * @param contentType o tipo MIME do conteúdo
     * @return a extensão do arquivo
     * @throws Exception se não for possível determinar a extensão
     */
    private String getExtensionFromContentType(String contentType) throws Exception {
        return allTypes.forName(contentType).getExtension();
    }

    /**
     * Recupera todos os campos da classe e suas superclasses.
     *
     * @param clazz a classe a ser inspecionada
     * @return uma lista de campos
     */
    private List<Field> getAllFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        while (clazz != null) {
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
     *
     * @param clazz     a classe para iniciar a busca
     * @param fieldName o nome do campo
     * @return o objeto Field
     * @throws NoSuchFieldException se o campo não for encontrado
     */
    private Field getFieldFromClassHierarchy(Class<?> clazz, String fieldName) throws NoSuchFieldException {
        while (clazz != null) {
            try {
                return clazz.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        throw new NoSuchFieldException("Campo não encontrado: " + fieldName);
    }
}
