package com.raze.coffeeshop.service.impl;

import com.raze.coffeeshop.dto.UserRequest;
import com.raze.coffeeshop.dto.UserResponse;
import com.raze.coffeeshop.enums.UserRole;
import com.raze.coffeeshop.exception.DuplicateResourceException;
import com.raze.coffeeshop.exception.ResourceNotFoundException;
import com.raze.coffeeshop.model.User;
import com.raze.coffeeshop.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * TEST DE UNIDAD (service layer) — mismo patrón que {@link CustomerServiceImplTest}.
 * UserServiceImpl depende de UserRepository y de {@link PasswordEncoder}; este último
 * también se mockea, así que el hash real de Spring Security nunca se ejecuta aquí (eso
 * se prueba en el test de integración, con el contexto real).
 */
@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository repository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    private UUID userId;
    private User user;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = new User();
        user.setId(userId);
        user.setEmail("ana@example.com");
        user.setPasswordHash("hashed-password");
        user.setFirstName("Ana");
        user.setLastName("Lopez");
        user.setRole(UserRole.ADMIN);
        user.setActive(true);
    }

    @Test
    void findAll_devuelveSoloUsuariosActivos() {
        when(repository.findByActiveTrue(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(user)));

        Page<UserResponse> result = userService.findAll(Pageable.unpaged());

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).email()).isEqualTo("ana@example.com");
        verify(repository).findByActiveTrue(any(Pageable.class));
    }

    @Test
    void findById_devuelveUsuario_cuandoExiste() {
        when(repository.findById(userId)).thenReturn(Optional.of(user));

        UserResponse result = userService.findById(userId);

        assertThat(result.role()).isEqualTo(UserRole.ADMIN);
        verify(repository).findById(userId);
    }

    @Test
    void findById_lanzaExcepcion_cuandoNoExiste() {
        UUID missingId = UUID.randomUUID();
        when(repository.findById(missingId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.findById(missingId));
    }

    @Test
    // Verifica que la contraseña en texto plano del request NUNCA se guarda tal cual:
    // el service debe pasarla por PasswordEncoder.encode(...) antes de persistirla.
    void create_cifraLaContrasenaYGuardaUsuario_cuandoCorreoDisponible() {
        UserRequest request = new UserRequest("nuevo@example.com", "SuperSecreta123", "Nuevo", "Usuario", UserRole.BARISTA);
        when(repository.findByEmailIgnoreCase("nuevo@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("SuperSecreta123")).thenReturn("hashed-password");
        when(repository.save(any(User.class))).thenReturn(user);

        UserResponse result = userService.create(request);

        assertThat(result).isNotNull();
        verify(passwordEncoder).encode("SuperSecreta123");
        verify(repository).save(any(User.class));
    }

    @Test
    void create_lanzaExcepcion_cuandoCorreoYaExiste() {
        UserRequest request = new UserRequest("ana@example.com", "SuperSecreta123", "Ana", "Lopez", UserRole.ADMIN);
        when(repository.findByEmailIgnoreCase("ana@example.com")).thenReturn(Optional.of(user));

        assertThrows(DuplicateResourceException.class, () -> userService.create(request));
    }

    @Test
    void update_actualizaYRetornaUsuario_cuandoExiste() {
        UserRequest request = new UserRequest("ana@example.com", "OtraClave456", "Ana", "Gomez", UserRole.MANAGER);
        when(repository.findById(userId)).thenReturn(Optional.of(user));
        when(repository.findByEmailIgnoreCase("ana@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("OtraClave456")).thenReturn("nuevo-hash");
        when(repository.save(any(User.class))).thenReturn(user);

        UserResponse result = userService.update(userId, request);

        assertThat(result.lastName()).isEqualTo("Gomez");
        assertThat(result.role()).isEqualTo(UserRole.MANAGER);
        verify(passwordEncoder).encode("OtraClave456");
        verify(repository).save(user);
    }

    @Test
    void update_lanzaExcepcion_cuandoNoExiste() {
        UUID missingId = UUID.randomUUID();
        UserRequest request = new UserRequest("ana@example.com", "OtraClave456", "Ana", "Gomez", UserRole.MANAGER);
        when(repository.findById(missingId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.update(missingId, request));
    }

    @Test
    void update_lanzaExcepcion_cuandoCorreoYaUsadoPorOtroUsuario() {
        User otro = new User();
        otro.setId(UUID.randomUUID());
        otro.setEmail("otro@example.com");

        UserRequest request = new UserRequest("otro@example.com", "OtraClave456", "Ana", "Gomez", UserRole.MANAGER);
        when(repository.findById(userId)).thenReturn(Optional.of(user));
        when(repository.findByEmailIgnoreCase("otro@example.com")).thenReturn(Optional.of(otro));

        assertThrows(DuplicateResourceException.class, () -> userService.update(userId, request));
    }

    @Test
    void delete_marcaUsuarioComoInactivo() {
        when(repository.findById(userId)).thenReturn(Optional.of(user));
        when(repository.save(any(User.class))).thenReturn(user);

        userService.delete(userId);

        assertThat(user.getActive()).isFalse();
        verify(repository).save(user);
    }
}
