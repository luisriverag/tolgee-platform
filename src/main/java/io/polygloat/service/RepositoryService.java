package io.polygloat.service;

import io.polygloat.dtos.request.CreateRepositoryDTO;
import io.polygloat.dtos.request.EditRepositoryDTO;
import io.polygloat.dtos.request.LanguageDTO;
import io.polygloat.dtos.response.RepositoryDTO;
import io.polygloat.exceptions.NotFoundException;
import io.polygloat.model.Repository;
import io.polygloat.model.UserAccount;
import io.polygloat.repository.PermissionRepository;
import io.polygloat.repository.RepositoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RepositoryService {

    private final RepositoryRepository repositoryRepository;
    private final EntityManager entityManager;
    private final LanguageService languageService;
    private final SecurityService securityService;
    private final PermissionRepository permissionRepository;
    private final PermissionService permissionService;
    private final ApiKeyService apiKeyService;
    private final TranslationService translationService;

    private KeyService keyService;

    @Autowired
    public RepositoryService(RepositoryRepository repositoryRepository, EntityManager entityManager, LanguageService languageService, SecurityService securityService, PermissionRepository permissionRepository, PermissionService permissionService, ApiKeyService apiKeyService, TranslationService translationService) {
        this.repositoryRepository = repositoryRepository;
        this.entityManager = entityManager;
        this.languageService = languageService;
        this.securityService = securityService;
        this.permissionRepository = permissionRepository;
        this.permissionService = permissionService;
        this.apiKeyService = apiKeyService;
        this.translationService = translationService;
    }

    @Transactional
    public Optional<Repository> findByName(String name, UserAccount userAccount) {
        return repositoryRepository.findByNameAndCreatedBy(name, userAccount);
    }

    @Transactional
    public Optional<Repository> findById(Long id) {
        return repositoryRepository.findById(id);
    }


    @Transactional
    public Repository createRepository(CreateRepositoryDTO dto, UserAccount createdBy) {
        Repository repository = new Repository();
        repository.setName(dto.getName());
        repository.setCreatedBy(createdBy);

        securityService.grantFullAccessToRepo(repository);

        for (LanguageDTO language : dto.getLanguages()) {
            languageService.createLanguage(language, repository);
        }

        entityManager.persist(repository);
        return repository;
    }

    @Transactional
    public Repository editRepository(EditRepositoryDTO dto) {
        Repository repository = repositoryRepository.findById(dto.getRepositoryId())
                .orElseThrow(NotFoundException::new);
        repository.setName(dto.getName());
        entityManager.persist(repository);
        return repository;
    }


    @Transactional
    public Set<RepositoryDTO> findAllPermitted(UserAccount userAccount) {
        return permissionRepository.findAllByUser(userAccount).stream()
                .map(permission -> RepositoryDTO.fromEntityAndPermission(permission.getRepository(), permission))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @Transactional
    public void deleteRepository(Long id) {
        Repository repository = this.findById(id).orElseThrow(NotFoundException::new);
        permissionService.deleteAllByRepository(repository.getId());
        translationService.deleteAllByRepository(repository.getId());
        keyService.deleteAllByRepository(repository.getId());
        apiKeyService.deleteAllByRepository(repository.getId());
        languageService.deleteAllByRepository(repository.getId());
        repositoryRepository.delete(repository);
    }

    @Autowired
    public void setKeyService(KeyService keyService) {
        this.keyService = keyService;
    }
}
