package com.raze.coffeeshop.service.impl;

import com.raze.coffeeshop.dto.BranchRequest;
import com.raze.coffeeshop.dto.BranchResponse;
import com.raze.coffeeshop.exception.ResourceNotFoundException;
import com.raze.coffeeshop.model.Branch;
import com.raze.coffeeshop.repository.BranchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * TEST DE UNIDAD (service layer), el tipo más "barato" de los tres que hay en el proyecto.
 *
 * No se levanta Spring en absoluto: no hay ApplicationContext, no hay servidor web, no hay
 * base de datos. Solo se instancia el objeto Java {@link BranchServiceImpl} de verdad y se le
 * inyectan dependencias falsas (mocks) para poder probar SU lógica de negocio de forma aislada,
 * sin depender de que la base de datos, JPA o la red funcionen.
 *
 * Por eso corre en milisegundos: no hay arranque de contexto, no hay Docker, no hay red.
 */
@ExtendWith(MockitoExtension.class)
// Activa la integración de Mockito con JUnit 5. Es lo que hace que las anotaciones
// @Mock y @InjectMocks de abajo realmente se procesen antes de cada @Test.
// Sin esta extensión, esos campos simplemente quedarían en null.
class BranchServiceImplTest {

    @Mock
    // Crea una implementación falsa (proxy) de la interfaz BranchRepository.
    // No hay conexión a Postgres: cada método devuelve lo que nosotros le digamos
    // con when(...) más abajo, y por defecto (si no se configura) devuelve null/vacío.
    private BranchRepository branchRepository;

    @InjectMocks
    // Crea una instancia REAL de BranchServiceImpl (la clase que queremos probar)
    // e inyecta ahí el mock de arriba (por constructor, gracias a @RequiredArgsConstructor
    // en la clase original). Así probamos la lógica real del service, con datos falsos.
    private BranchServiceImpl branchService;

    private UUID branchId;
    private Branch branch;

    @BeforeEach
    // JUnit ejecuta este método ANTES de cada @Test (no una sola vez para toda la clase).
    // Esto garantiza que cada test arranque con datos frescos y que un test no pueda
    // "contaminar" a otro reutilizando el mismo objeto ya modificado.
    void setUp() {
        branchId = UUID.randomUUID();
        branch = new Branch();
        branch.setId(branchId);
        branch.setName("Sucursal Centro");
        branch.setAddress("Av. Principal 123");
        branch.setCity("Ciudad de Mexico");
        branch.setState("CDMX");
        branch.setActive(true);
    }

    @Test
    // Para que este método se ejecute solo necesita: el mock configurado con when(...)
    // (Arrange), llamar al método real del service (Act) y comprobar el resultado (Assert).
    // No hay red, no hay JSON, no hay validaciones @Valid: eso no es responsabilidad del service.
    void findById_devuelveSucursal_cuandoExiste() {
        when(branchRepository.findById(branchId)).thenReturn(Optional.of(branch));

        BranchResponse result = branchService.findById(branchId);

        assertThat(result.name()).isEqualTo("Sucursal Centro");
        // verify() confirma que el service SÍ llamó al repositorio (y no, por ejemplo,
        // que adivinó el resultado). Es una aserción sobre el comportamiento, no solo el dato.
        verify(branchRepository).findById(branchId);
    }

    @Test
    void findById_lanzaExcepcion_cuandoNoExiste() {
        UUID missingId = UUID.randomUUID();
        when(branchRepository.findById(missingId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> branchService.findById(missingId));
    }

    @Test
    void create_guardaYRetornaSucursal() {
        BranchRequest request = new BranchRequest("Sucursal Norte", "Calle 45", "Monterrey", "NL");
        // any(Branch.class) le dice al mock "acepta cualquier Branch que te pasen, no me importa
        // el contenido exacto, y cuando eso pase, responde con este branch de prueba".
        when(branchRepository.save(any(Branch.class))).thenReturn(branch);

        BranchResponse result = branchService.create(request);

        assertThat(result).isNotNull();
        verify(branchRepository).save(any(Branch.class));
    }

    @Test
    void delete_marcaSucursalComoInactiva() {
        when(branchRepository.findById(branchId)).thenReturn(Optional.of(branch));
        when(branchRepository.save(any(Branch.class))).thenReturn(branch);

        branchService.delete(branchId);

        // Como el mock no persiste nada de verdad, verificamos el efecto directamente
        // sobre el objeto Java en memoria que el service modificó (soft delete).
        assertThat(branch.isActive()).isFalse();
        verify(branchRepository).save(branch);
    }
}
