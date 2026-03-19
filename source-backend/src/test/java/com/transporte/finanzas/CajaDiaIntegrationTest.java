package com.transporte.finanzas;

import com.transporte.core.exception.BusinessException;
import com.transporte.finanzas.dto.*;
import com.transporte.finanzas.entity.CashRegister;
import com.transporte.finanzas.entity.CashTransaction;
import com.transporte.finanzas.enums.CashRegisterStatus;
import com.transporte.finanzas.enums.TransactionType;
import com.transporte.finanzas.mapper.CashRegisterMapper;
import com.transporte.finanzas.mapper.CashTransactionMapper;
import com.transporte.finanzas.repository.CashRegisterRepository;
import com.transporte.finanzas.repository.CashTransactionRepository;
import com.transporte.finanzas.service.CashRegisterService;
import com.transporte.finanzas.service.CashTransactionService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * Integration test: Caja del Día (Daily Cash Register)
 *
 * <p>Tests the complete daily cash register flow:
 * <ol>
 *   <li>Verificar si el empleado tiene caja abierta</li>
 *   <li>Abrir caja con monto inicial</li>
 *   <li>Registrar ingresos y egresos (movimientos)</li>
 *   <li>Obtener resumen financiero (totales)</li>
 *   <li>Cerrar caja con monto final y notas</li>
 * </ol>
 *
 * <p>Uses real service instances; only repositories and mappers are mocked.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("IT: Caja del Día (Daily Cash Register)")
class CajaDiaIntegrationTest {

    // ── Mocks ────────────────────────────────────────────────────────────────
    @Mock private CashRegisterRepository    cashRegisterRepository;
    @Mock private CashTransactionRepository cashTransactionRepository;
    @Mock private CashRegisterMapper        cashRegisterMapper;
    @Mock private CashTransactionMapper     cashTransactionMapper;

    // ── Services under test ──────────────────────────────────────────────────
    private CashRegisterService    cashRegisterService;
    private CashTransactionService cashTransactionService;

    // ── Fixtures ─────────────────────────────────────────────────────────────
    private UUID employeeId;
    private UUID cashRegisterId;

    @BeforeEach
    void setUp() {
        employeeId     = UUID.fromString("00100000-0000-0000-0000-000000000001");
        cashRegisterId = UUID.fromString("00200000-0000-0000-0000-000000000001");

        cashRegisterService    = new CashRegisterService(cashRegisterRepository, cashTransactionRepository, cashRegisterMapper);
        cashTransactionService = new CashTransactionService(cashTransactionRepository, cashRegisterRepository, cashTransactionMapper);
    }

    // ── Helper builders ──────────────────────────────────────────────────────

    private CashRegister buildRegister(CashRegisterStatus status) {
        CashRegister cr = new CashRegister();
        setId(cr, cashRegisterId);
        cr.setEmployeeId(employeeId);
        cr.setInitialAmount(new BigDecimal("500.00"));
        cr.setStatus(status);
        cr.setOpenedAt(LocalDateTime.now().minusHours(3));
        return cr;
    }

    private CashRegisterResponse buildRegisterResponse(CashRegisterStatus status) {
        return new CashRegisterResponse(
                cashRegisterId, employeeId,
                LocalDateTime.now().minusHours(3), null,
                new BigDecimal("500.00"), null,
                status, null, LocalDateTime.now().minusHours(3)
        );
    }

    private CashTransaction buildTransaction(TransactionType type, String concept, BigDecimal amount) {
        CashTransaction tx = new CashTransaction();
        setId(tx, UUID.randomUUID());
        tx.setCashRegisterId(cashRegisterId);
        tx.setType(type);
        tx.setConcept(concept);
        tx.setAmount(amount);
        return tx;
    }

    private CashTransactionResponse buildTransactionResponse(CashTransaction tx) {
        return new CashTransactionResponse(
                tx.getId(), cashRegisterId, tx.getType(),
                tx.getConcept(), tx.getAmount(), null, null, LocalDateTime.now()
        );
    }

    private void setId(Object entity, UUID id) {
        try {
            var f = com.transporte.core.audit.BaseEntity.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(entity, id);
        } catch (Exception ignored) {}
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 1. Verificar caja abierta del empleado
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("1. Verificar caja abierta del empleado")
    class VerificarCajaAbierta {

        @Test
        @DisplayName("Retorna la caja abierta cuando el empleado tiene una activa")
        void retornaCajaAbiertaExistente() {
            CashRegister entity = buildRegister(CashRegisterStatus.OPEN);
            CashRegisterResponse resp = buildRegisterResponse(CashRegisterStatus.OPEN);

            given(cashRegisterRepository.findByEmployeeIdAndStatus(employeeId, CashRegisterStatus.OPEN))
                    .willReturn(Optional.of(entity));
            given(cashRegisterMapper.toResponse(entity)).willReturn(resp);

            Optional<CashRegisterResponse> result = cashRegisterService.findOpenByEmployee(employeeId);

            assertThat(result).isPresent();
            assertThat(result.get().status()).isEqualTo(CashRegisterStatus.OPEN);
            assertThat(result.get().employeeId()).isEqualTo(employeeId);
            assertThat(result.get().initialAmount()).isEqualByComparingTo("500.00");
        }

        @Test
        @DisplayName("Retorna empty cuando el empleado no tiene caja abierta")
        void retornaEmptySinCajaAbierta() {
            given(cashRegisterRepository.findByEmployeeIdAndStatus(employeeId, CashRegisterStatus.OPEN))
                    .willReturn(Optional.empty());

            Optional<CashRegisterResponse> result = cashRegisterService.findOpenByEmployee(employeeId);

            assertThat(result).isEmpty();
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 2. Apertura de caja
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("2. Apertura de caja")
    class AperturaCaja {

        @Test
        @DisplayName("Abre correctamente la caja con monto inicial")
        void abreCajaConMontoInicial() {
            CashRegisterRequest request = new CashRegisterRequest(
                    employeeId, new BigDecimal("500.00"), "Apertura turno mañana"
            );
            CashRegister entity = buildRegister(CashRegisterStatus.OPEN);
            CashRegisterResponse resp = buildRegisterResponse(CashRegisterStatus.OPEN);

            given(cashRegisterRepository.existsByEmployeeIdAndStatus(employeeId, CashRegisterStatus.OPEN))
                    .willReturn(false);
            given(cashRegisterMapper.toEntity(request)).willReturn(entity);
            given(cashRegisterRepository.save(any())).willReturn(entity);
            given(cashRegisterMapper.toResponse(entity)).willReturn(resp);

            CashRegisterResponse result = cashRegisterService.open(request);

            assertThat(result.status()).isEqualTo(CashRegisterStatus.OPEN);
            assertThat(result.initialAmount()).isEqualByComparingTo("500.00");
            assertThat(result.employeeId()).isEqualTo(employeeId);

            ArgumentCaptor<CashRegister> captor = ArgumentCaptor.forClass(CashRegister.class);
            verify(cashRegisterRepository).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(CashRegisterStatus.OPEN);
            assertThat(captor.getValue().getOpenedAt()).isNotNull();
        }

        @Test
        @DisplayName("Lanza BusinessException si el empleado ya tiene caja abierta")
        void lanzaExcepcionSiYaTieneCaja() {
            CashRegisterRequest request = new CashRegisterRequest(
                    employeeId, new BigDecimal("200.00"), null
            );
            given(cashRegisterRepository.existsByEmployeeIdAndStatus(employeeId, CashRegisterStatus.OPEN))
                    .willReturn(true);

            assertThatThrownBy(() -> cashRegisterService.open(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("ya tiene una caja abierta");

            verify(cashRegisterRepository, never()).save(any());
        }

        @Test
        @DisplayName("Permite abrir caja con monto inicial de cero")
        void permiteCajaConMontoCero() {
            CashRegisterRequest request = new CashRegisterRequest(
                    employeeId, BigDecimal.ZERO, "Sin efectivo inicial"
            );
            CashRegister entity = buildRegister(CashRegisterStatus.OPEN);
            entity.setInitialAmount(BigDecimal.ZERO);
            CashRegisterResponse resp = new CashRegisterResponse(
                    cashRegisterId, employeeId,
                    LocalDateTime.now(), null,
                    BigDecimal.ZERO, null, CashRegisterStatus.OPEN, null, LocalDateTime.now()
            );

            given(cashRegisterRepository.existsByEmployeeIdAndStatus(employeeId, CashRegisterStatus.OPEN))
                    .willReturn(false);
            given(cashRegisterMapper.toEntity(request)).willReturn(entity);
            given(cashRegisterRepository.save(any())).willReturn(entity);
            given(cashRegisterMapper.toResponse(entity)).willReturn(resp);

            CashRegisterResponse result = cashRegisterService.open(request);

            assertThat(result.initialAmount()).isEqualByComparingTo("0.00");
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 3. Registro de movimientos (transacciones)
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("3. Registro de movimientos de caja")
    class RegistroMovimientos {

        @Test
        @DisplayName("Registra ingreso correctamente")
        void registraIngreso() {
            CashTransactionRequest request = new CashTransactionRequest(
                    cashRegisterId, TransactionType.INGRESO, "Venta pasaje TKT-001",
                    new BigDecimal("55.00"), null, null
            );
            CashRegister openRegister = buildRegister(CashRegisterStatus.OPEN);
            CashTransaction entity = buildTransaction(TransactionType.INGRESO, "Venta pasaje TKT-001", new BigDecimal("55.00"));
            CashTransactionResponse resp = buildTransactionResponse(entity);

            given(cashRegisterRepository.findById(cashRegisterId))
                    .willReturn(Optional.of(openRegister));
            given(cashTransactionMapper.toEntity(request)).willReturn(entity);
            given(cashTransactionRepository.save(any())).willReturn(entity);
            given(cashTransactionMapper.toResponse(entity)).willReturn(resp);

            CashTransactionResponse result = cashTransactionService.create(request);

            assertThat(result.type()).isEqualTo(TransactionType.INGRESO);
            assertThat(result.amount()).isEqualByComparingTo("55.00");
            assertThat(result.concept()).isEqualTo("Venta pasaje TKT-001");
            verify(cashTransactionRepository).save(any());
        }

        @Test
        @DisplayName("Registra egreso correctamente")
        void registraEgreso() {
            CashTransactionRequest request = new CashTransactionRequest(
                    cashRegisterId, TransactionType.EGRESO, "Gasto limpieza",
                    new BigDecimal("30.00"), null, null
            );
            CashRegister openRegister = buildRegister(CashRegisterStatus.OPEN);
            CashTransaction entity = buildTransaction(TransactionType.EGRESO, "Gasto limpieza", new BigDecimal("30.00"));
            CashTransactionResponse resp = buildTransactionResponse(entity);

            given(cashRegisterRepository.findById(cashRegisterId))
                    .willReturn(Optional.of(openRegister));
            given(cashTransactionMapper.toEntity(request)).willReturn(entity);
            given(cashTransactionRepository.save(any())).willReturn(entity);
            given(cashTransactionMapper.toResponse(entity)).willReturn(resp);

            CashTransactionResponse result = cashTransactionService.create(request);

            assertThat(result.type()).isEqualTo(TransactionType.EGRESO);
            assertThat(result.amount()).isEqualByComparingTo("30.00");
        }

        @Test
        @DisplayName("Lanza excepción al registrar movimiento en caja cerrada")
        void lanzaExcepcionEnCajaCerrada() {
            CashTransactionRequest request = new CashTransactionRequest(
                    cashRegisterId, TransactionType.INGRESO, "Test",
                    new BigDecimal("10.00"), null, null
            );
            CashRegister closedRegister = buildRegister(CashRegisterStatus.CLOSED);

            given(cashRegisterRepository.findById(cashRegisterId))
                    .willReturn(Optional.of(closedRegister));

            assertThatThrownBy(() -> cashTransactionService.create(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("not open");

            verify(cashTransactionRepository, never()).save(any());
        }

        @Test
        @DisplayName("Lanza excepción si la caja no existe")
        void lanzaExcepcionCajaNoExistente() {
            CashTransactionRequest request = new CashTransactionRequest(
                    UUID.randomUUID(), TransactionType.INGRESO, "Test",
                    new BigDecimal("10.00"), null, null
            );
            given(cashRegisterRepository.findById(any())).willReturn(Optional.empty());

            assertThatThrownBy(() -> cashTransactionService.create(request))
                    .isInstanceOf(BusinessException.class);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 4. Resumen financiero de caja
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("4. Resumen financiero de caja")
    class ResumenFinanciero {

        @Test
        @DisplayName("Calcula correctamente el resumen con ingresos y egresos")
        void calculaResumenCompleto() {
            CashRegister entity = buildRegister(CashRegisterStatus.OPEN);

            given(cashRegisterRepository.findById(cashRegisterId)).willReturn(Optional.of(entity));
            given(cashTransactionRepository.sumIncomesByCashRegisterId(cashRegisterId))
                    .willReturn(new BigDecimal("350.00"));
            given(cashTransactionRepository.sumExpensesByCashRegisterId(cashRegisterId))
                    .willReturn(new BigDecimal("80.00"));

            CashRegisterSummaryResponse summary = cashRegisterService.getSummary(cashRegisterId);

            assertThat(summary.totalIncomes()).isEqualByComparingTo("350.00");
            assertThat(summary.totalExpenses()).isEqualByComparingTo("80.00");
            // expectedFinal = initialAmount(500) + incomes(350) - expenses(80) = 770
            assertThat(summary.expectedFinalAmount()).isEqualByComparingTo("770.00");
            assertThat(summary.initialAmount()).isEqualByComparingTo("500.00");
        }

        @Test
        @DisplayName("Resumen correcto cuando no hay movimientos (ambos son 0)")
        void resumenSinMovimientos() {
            CashRegister entity = buildRegister(CashRegisterStatus.OPEN);

            given(cashRegisterRepository.findById(cashRegisterId)).willReturn(Optional.of(entity));
            given(cashTransactionRepository.sumIncomesByCashRegisterId(cashRegisterId))
                    .willReturn(BigDecimal.ZERO);
            given(cashTransactionRepository.sumExpensesByCashRegisterId(cashRegisterId))
                    .willReturn(BigDecimal.ZERO);

            CashRegisterSummaryResponse summary = cashRegisterService.getSummary(cashRegisterId);

            assertThat(summary.totalIncomes()).isEqualByComparingTo("0");
            assertThat(summary.totalExpenses()).isEqualByComparingTo("0");
            // expectedFinal = 500 + 0 - 0 = 500
            assertThat(summary.expectedFinalAmount()).isEqualByComparingTo("500.00");
        }

        @Test
        @DisplayName("Lista las transacciones de la caja con paginación")
        void listaTransacciones() {
            CashTransaction tx1 = buildTransaction(TransactionType.INGRESO, "Ticket", new BigDecimal("55.00"));
            CashTransaction tx2 = buildTransaction(TransactionType.EGRESO, "Gasto", new BigDecimal("10.00"));

            given(cashTransactionRepository.findByCashRegisterId(
                    eq(cashRegisterId), any(org.springframework.data.domain.Pageable.class)))
                    .willReturn(new PageImpl<>(List.of(tx1, tx2)));
            given(cashTransactionMapper.toResponse(tx1)).willReturn(buildTransactionResponse(tx1));
            given(cashTransactionMapper.toResponse(tx2)).willReturn(buildTransactionResponse(tx2));

            var page = cashTransactionService.findByCashRegister(cashRegisterId, PageRequest.of(0, 10));

            assertThat(page.content()).hasSize(2);
            assertThat(page.content().get(0).type()).isEqualTo(TransactionType.INGRESO);
            assertThat(page.content().get(1).type()).isEqualTo(TransactionType.EGRESO);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 5. Cierre de caja
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("5. Cierre de caja")
    class CierreCaja {

        @Test
        @DisplayName("Cierra caja correctamente con monto final y notas")
        void cierraCajaConMontoFinalYNotas() {
            CashRegister entity = buildRegister(CashRegisterStatus.OPEN);
            CloseCashRegisterRequest request = new CloseCashRegisterRequest(
                    new BigDecimal("850.00"), "Cierre sin novedades"
            );
            CashRegisterResponse closedResp = new CashRegisterResponse(
                    cashRegisterId, employeeId,
                    entity.getOpenedAt(), LocalDateTime.now(),
                    new BigDecimal("500.00"), new BigDecimal("850.00"),
                    CashRegisterStatus.CLOSED, "Cierre sin novedades", entity.getOpenedAt()
            );

            given(cashRegisterRepository.findById(cashRegisterId)).willReturn(Optional.of(entity));
            given(cashRegisterRepository.save(any())).willReturn(entity);
            given(cashRegisterMapper.toResponse(any())).willReturn(closedResp);

            CashRegisterResponse result = cashRegisterService.close(cashRegisterId, request);

            assertThat(result.status()).isEqualTo(CashRegisterStatus.CLOSED);
            assertThat(result.finalAmount()).isEqualByComparingTo("850.00");
            assertThat(result.notes()).isEqualTo("Cierre sin novedades");

            ArgumentCaptor<CashRegister> captor = ArgumentCaptor.forClass(CashRegister.class);
            verify(cashRegisterRepository).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(CashRegisterStatus.CLOSED);
            assertThat(captor.getValue().getClosedAt()).isNotNull();
            assertThat(captor.getValue().getFinalAmount()).isEqualByComparingTo("850.00");
        }

        @Test
        @DisplayName("Lanza BusinessException al intentar cerrar caja ya cerrada")
        void lanzaExcepcionAlCerrarCajaYaCerrada() {
            CashRegister closedEntity = buildRegister(CashRegisterStatus.CLOSED);
            CloseCashRegisterRequest request = new CloseCashRegisterRequest(
                    new BigDecimal("500.00"), null
            );

            given(cashRegisterRepository.findById(cashRegisterId)).willReturn(Optional.of(closedEntity));

            assertThatThrownBy(() -> cashRegisterService.close(cashRegisterId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("ya está cerrada");

            verify(cashRegisterRepository, never()).save(any());
        }

        @Test
        @DisplayName("Cierre sin monto final registra como null")
        void cierreSinMontoFinal() {
            CashRegister entity = buildRegister(CashRegisterStatus.OPEN);
            CloseCashRegisterRequest request = new CloseCashRegisterRequest(null, "Sin conteo");
            CashRegisterResponse closedResp = new CashRegisterResponse(
                    cashRegisterId, employeeId,
                    entity.getOpenedAt(), LocalDateTime.now(),
                    new BigDecimal("500.00"), null,
                    CashRegisterStatus.CLOSED, "Sin conteo", entity.getOpenedAt()
            );

            given(cashRegisterRepository.findById(cashRegisterId)).willReturn(Optional.of(entity));
            given(cashRegisterRepository.save(any())).willReturn(entity);
            given(cashRegisterMapper.toResponse(any())).willReturn(closedResp);

            CashRegisterResponse result = cashRegisterService.close(cashRegisterId, request);

            assertThat(result.status()).isEqualTo(CashRegisterStatus.CLOSED);
            assertThat(result.finalAmount()).isNull();
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 6. Flujo completo: apertura → movimientos → resumen → cierre
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("6. Flujo completo: Caja del Día E2E")
    class FlujoCajaCompletaE2E {

        @Test
        @DisplayName("Flujo exitoso: verificar → abrir → ingresos → egresos → resumen → cerrar")
        void happyPathCajaDelDia() {
            // ── Sin caja abierta al inicio ────────────────────────────────────
            given(cashRegisterRepository.findByEmployeeIdAndStatus(employeeId, CashRegisterStatus.OPEN))
                    .willReturn(Optional.empty());

            Optional<CashRegisterResponse> cajaInicial = cashRegisterService.findOpenByEmployee(employeeId);
            assertThat(cajaInicial).isEmpty();

            // ── Abrir caja ────────────────────────────────────────────────────
            CashRegisterRequest openReq = new CashRegisterRequest(
                    employeeId, new BigDecimal("500.00"), "Apertura normal"
            );
            CashRegister openEntity = buildRegister(CashRegisterStatus.OPEN);
            CashRegisterResponse openResp = buildRegisterResponse(CashRegisterStatus.OPEN);

            given(cashRegisterRepository.existsByEmployeeIdAndStatus(employeeId, CashRegisterStatus.OPEN))
                    .willReturn(false);
            given(cashRegisterMapper.toEntity(openReq)).willReturn(openEntity);
            given(cashRegisterRepository.save(any())).willReturn(openEntity);
            given(cashRegisterMapper.toResponse(openEntity)).willReturn(openResp);

            CashRegisterResponse cajaAbierta = cashRegisterService.open(openReq);
            assertThat(cajaAbierta.status()).isEqualTo(CashRegisterStatus.OPEN);

            // ── Registrar 3 ingresos ──────────────────────────────────────────
            BigDecimal ingreso1 = new BigDecimal("120.00");
            BigDecimal ingreso2 = new BigDecimal("85.00");
            BigDecimal ingreso3 = new BigDecimal("55.00");

            for (BigDecimal monto : List.of(ingreso1, ingreso2, ingreso3)) {
                CashTransactionRequest txReq = new CashTransactionRequest(
                        cashRegisterId, TransactionType.INGRESO, "Venta", monto, null, null
                );
                CashTransaction txEntity = buildTransaction(TransactionType.INGRESO, "Venta", monto);
                CashTransactionResponse txResp = buildTransactionResponse(txEntity);

                given(cashRegisterRepository.findById(cashRegisterId)).willReturn(Optional.of(openEntity));
                given(cashTransactionMapper.toEntity(txReq)).willReturn(txEntity);
                given(cashTransactionRepository.save(any())).willReturn(txEntity);
                given(cashTransactionMapper.toResponse(txEntity)).willReturn(txResp);

                CashTransactionResponse tx = cashTransactionService.create(txReq);
                assertThat(tx.type()).isEqualTo(TransactionType.INGRESO);
            }

            // ── Registrar 1 egreso ────────────────────────────────────────────
            BigDecimal egreso1 = new BigDecimal("40.00");
            CashTransactionRequest egresoReq = new CashTransactionRequest(
                    cashRegisterId, TransactionType.EGRESO, "Compra insumos", egreso1, null, null
            );
            CashTransaction egresoEntity = buildTransaction(TransactionType.EGRESO, "Compra insumos", egreso1);
            CashTransactionResponse egresoResp = buildTransactionResponse(egresoEntity);

            given(cashRegisterRepository.findById(cashRegisterId)).willReturn(Optional.of(openEntity));
            given(cashTransactionMapper.toEntity(egresoReq)).willReturn(egresoEntity);
            given(cashTransactionRepository.save(any())).willReturn(egresoEntity);
            given(cashTransactionMapper.toResponse(egresoEntity)).willReturn(egresoResp);
            cashTransactionService.create(egresoReq);

            // ── Consultar resumen ─────────────────────────────────────────────
            // ingresos = 120 + 85 + 55 = 260; egresos = 40; expected = 500 + 260 - 40 = 720
            given(cashRegisterRepository.findById(cashRegisterId)).willReturn(Optional.of(openEntity));
            given(cashTransactionRepository.sumIncomesByCashRegisterId(cashRegisterId))
                    .willReturn(new BigDecimal("260.00"));
            given(cashTransactionRepository.sumExpensesByCashRegisterId(cashRegisterId))
                    .willReturn(new BigDecimal("40.00"));

            CashRegisterSummaryResponse summary = cashRegisterService.getSummary(cashRegisterId);
            assertThat(summary.totalIncomes()).isEqualByComparingTo("260.00");
            assertThat(summary.totalExpenses()).isEqualByComparingTo("40.00");
            assertThat(summary.expectedFinalAmount()).isEqualByComparingTo("720.00");

            // ── Cerrar caja ───────────────────────────────────────────────────
            CloseCashRegisterRequest closeReq = new CloseCashRegisterRequest(
                    new BigDecimal("720.00"), "Cuadre exacto"
            );
            CashRegisterResponse closedResp = new CashRegisterResponse(
                    cashRegisterId, employeeId,
                    openEntity.getOpenedAt(), LocalDateTime.now(),
                    new BigDecimal("500.00"), new BigDecimal("720.00"),
                    CashRegisterStatus.CLOSED, "Cuadre exacto", openEntity.getOpenedAt()
            );

            given(cashRegisterRepository.findById(cashRegisterId)).willReturn(Optional.of(openEntity));
            given(cashRegisterRepository.save(any())).willReturn(openEntity);
            given(cashRegisterMapper.toResponse(any())).willReturn(closedResp);

            CashRegisterResponse cajaCerrada = cashRegisterService.close(cashRegisterId, closeReq);
            assertThat(cajaCerrada.status()).isEqualTo(CashRegisterStatus.CLOSED);
            assertThat(cajaCerrada.finalAmount()).isEqualByComparingTo("720.00");
        }

        @Test
        @DisplayName("Segundo empleado puede abrir su propia caja independiente")
        void segundoEmpleadoPuedeAbrirSuCaja() {
            UUID employee2Id = UUID.randomUUID();
            CashRegisterRequest req2 = new CashRegisterRequest(
                    employee2Id, new BigDecimal("300.00"), "Turno tarde"
            );
            CashRegister entity2 = new CashRegister();
            setId(entity2, UUID.randomUUID());
            entity2.setEmployeeId(employee2Id);
            entity2.setInitialAmount(new BigDecimal("300.00"));
            entity2.setStatus(CashRegisterStatus.OPEN);
            entity2.setOpenedAt(LocalDateTime.now());

            CashRegisterResponse resp2 = new CashRegisterResponse(
                    entity2.getId(), employee2Id,
                    entity2.getOpenedAt(), null,
                    new BigDecimal("300.00"), null,
                    CashRegisterStatus.OPEN, null, entity2.getOpenedAt()
            );

            // Empleado 1 ya tiene caja abierta
            given(cashRegisterRepository.existsByEmployeeIdAndStatus(employeeId, CashRegisterStatus.OPEN))
                    .willReturn(true);
            // Empleado 2 no tiene caja
            given(cashRegisterRepository.existsByEmployeeIdAndStatus(employee2Id, CashRegisterStatus.OPEN))
                    .willReturn(false);
            given(cashRegisterMapper.toEntity(req2)).willReturn(entity2);
            given(cashRegisterRepository.save(any())).willReturn(entity2);
            given(cashRegisterMapper.toResponse(entity2)).willReturn(resp2);

            // Empleado 1 falla
            assertThatThrownBy(() -> cashRegisterService.open(
                    new CashRegisterRequest(employeeId, BigDecimal.ZERO, null)))
                    .isInstanceOf(BusinessException.class);

            // Empleado 2 abre sin problema
            CashRegisterResponse result2 = cashRegisterService.open(req2);
            assertThat(result2.employeeId()).isEqualTo(employee2Id);
            assertThat(result2.status()).isEqualTo(CashRegisterStatus.OPEN);
        }
    }
}
