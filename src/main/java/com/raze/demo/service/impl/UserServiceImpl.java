package com.raze.demo.service.impl;

import com.raze.demo.dto.UserRequest;
import com.raze.demo.dto.UserResponse;
import com.raze.demo.exception.DuplicateResourceException;
import com.raze.demo.exception.ResourceNotFoundException;
import com.raze.demo.model.User;
import com.raze.demo.repository.UserRepository;
import com.raze.demo.service.UserService;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Servicio encargado de manejar la lógica de negocio para los usuarios.
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Recupera todos los usuarios activos registrados en el sistema.
     *
     * @return Lista de {@link UserResponse}
     */
    @Transactional(readOnly = true)
    public Page<UserResponse> findAll(Pageable pageable) {
        return repository.findByActiveTrue(pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public UserResponse findById(UUID id) {
        return toResponse(getUser(id));
    }

    /**
     * Crea un nuevo usuario, validando que el correo no esté duplicado y
     * cifrando la contraseña antes de persistirla.
     *
     * @param request Datos del nuevo usuario
     * @return {@link UserResponse} con el usuario creado
     * @throws DuplicateResourceException si ya existe un usuario con el mismo correo
     */
    @Transactional
    public UserResponse create(UserRequest request) {
        ensureEmailIsAvailable(request.email(), null);

        User user = new User();
        user.setEmail(request.email().trim());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setRole(request.role());
        user = repository.save(user);
        return toResponse(user);
    }

    /**
     * Actualiza los datos de un usuario existente. La contraseña se vuelve a
     * cifrar en cada actualización.
     *
     * @param id      Identificador UUID del usuario a actualizar
     * @param request Nuevos datos del usuario
     * @return {@link UserResponse} con los datos actualizados
     * @throws ResourceNotFoundException si el usuario no existe
     * @throws DuplicateResourceException si el nuevo correo ya está en uso por otro usuario
     */
    @Transactional
    public UserResponse update(UUID id, UserRequest request) {
        User user = getUser(id);
        ensureEmailIsAvailable(request.email(), id);

        user.setEmail(request.email().strip());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setRole(request.role());
        user = repository.save(user);
        return toResponse(user);
    }

    @Transactional
    public void delete(UUID id) {
        User user = getUser(id);
        user.setActive(false);
        repository.save(user);
    }

    private User getUser(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
    }

    private void ensureEmailIsAvailable(String email, UUID currentId) {
        repository.findByEmailIgnoreCase(email.strip())
                .filter(user -> !user.getId().equals(currentId))
                .ifPresent(user -> {
                    throw new DuplicateResourceException("User already exists: " + email);
                });
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole(),
                user.getActive(),
                user.getCreatedAt()
        );
    }

}
