package com.transporte.siat.xml;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

@DisplayName("SiatCufGenerator Unit Tests")
class SiatCufGeneratorTest {

    private SiatCufGenerator cufGenerator;

    private static final String NIT = "1020304050";
    private static final LocalDateTime FECHA = LocalDateTime.of(2026, 3, 11, 10, 30, 0);
    private static final String CUFD = "ABCDEFGHIJ1234567890CUFD";

    @BeforeEach
    void setUp() {
        cufGenerator = new SiatCufGenerator();
    }

    @Nested
    @DisplayName("generate() tests")
    class GenerateTests {

        @Test
        @DisplayName("Should return non-null CUF string")
        void shouldReturnNonNullCuf() {
            String cuf = cufGenerator.generate(NIT, FECHA, 0, 2, 1, 1, 1L, 0, CUFD);
            assertThat(cuf).isNotNull().isNotBlank();
        }

        @Test
        @DisplayName("Should return uppercase hexadecimal string (SHA-256 Base16)")
        void shouldReturnUppercaseHex() {
            String cuf = cufGenerator.generate(NIT, FECHA, 0, 2, 1, 1, 1L, 0, CUFD);
            assertThat(cuf).matches("[0-9A-F]+");
        }

        @Test
        @DisplayName("Should return 64-character string (SHA-256 = 32 bytes = 64 hex chars)")
        void shouldReturn64Chars() {
            String cuf = cufGenerator.generate(NIT, FECHA, 0, 2, 1, 1, 1L, 0, CUFD);
            assertThat(cuf).hasSize(64);
        }

        @Test
        @DisplayName("Same inputs produce same CUF (deterministic)")
        void shouldBeDeterministic() {
            String cuf1 = cufGenerator.generate(NIT, FECHA, 0, 2, 1, 1, 1L, 0, CUFD);
            String cuf2 = cufGenerator.generate(NIT, FECHA, 0, 2, 1, 1, 1L, 0, CUFD);
            assertThat(cuf1).isEqualTo(cuf2);
        }

        @Test
        @DisplayName("Different NIT produces different CUF")
        void differentNitProducesDifferentCuf() {
            String cuf1 = cufGenerator.generate("1111111111", FECHA, 0, 2, 1, 1, 1L, 0, CUFD);
            String cuf2 = cufGenerator.generate("9999999999", FECHA, 0, 2, 1, 1, 1L, 0, CUFD);
            assertThat(cuf1).isNotEqualTo(cuf2);
        }

        @Test
        @DisplayName("Different invoice number produces different CUF")
        void differentNumeroFacturaProducesDifferentCuf() {
            String cuf1 = cufGenerator.generate(NIT, FECHA, 0, 2, 1, 1, 1L, 0, CUFD);
            String cuf2 = cufGenerator.generate(NIT, FECHA, 0, 2, 1, 1, 2L, 0, CUFD);
            assertThat(cuf1).isNotEqualTo(cuf2);
        }

        @Test
        @DisplayName("Different fecha produces different CUF")
        void differentFechaProducesDifferentCuf() {
            LocalDateTime fecha2 = LocalDateTime.of(2026, 3, 12, 10, 30, 0);
            String cuf1 = cufGenerator.generate(NIT, FECHA, 0, 2, 1, 1, 1L, 0, CUFD);
            String cuf2 = cufGenerator.generate(NIT, fecha2, 0, 2, 1, 1, 1L, 0, CUFD);
            assertThat(cuf1).isNotEqualTo(cuf2);
        }

        @Test
        @DisplayName("Different codigoSucursal produces different CUF")
        void differentSucursalProducesDifferentCuf() {
            String cuf1 = cufGenerator.generate(NIT, FECHA, 0, 2, 1, 1, 1L, 0, CUFD);
            String cuf2 = cufGenerator.generate(NIT, FECHA, 1, 2, 1, 1, 1L, 0, CUFD);
            assertThat(cuf1).isNotEqualTo(cuf2);
        }

        @Test
        @DisplayName("Short CUFD (< 10 chars) should work without throwing")
        void shortCufdShouldWork() {
            assertThatCode(() ->
                cufGenerator.generate(NIT, FECHA, 0, 2, 1, 1, 1L, 0, "SHORT")
            ).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("CUFD with exactly 10 chars uses full CUFD part")
        void cufdExactly10Chars() {
            String cuf = cufGenerator.generate(NIT, FECHA, 0, 2, 1, 1, 1L, 0, "1234567890");
            assertThat(cuf).isNotNull().hasSize(64);
        }

        @Test
        @DisplayName("CUFD longer than 10 chars uses only first 10")
        void cufdLongerThan10UsesFirst10() {
            String cuf1 = cufGenerator.generate(NIT, FECHA, 0, 2, 1, 1, 1L, 0, "1234567890XXXX");
            String cuf2 = cufGenerator.generate(NIT, FECHA, 0, 2, 1, 1, 1L, 0, "1234567890YYYY");
            assertThat(cuf1).isEqualTo(cuf2);
        }

        @Test
        @DisplayName("Sucursal is zero-padded to 4 digits")
        void sucursalIsZeroPadded() {
            // sucursal=5 → "0005", sucursal=1000 → "1000" — different from sucursal=50 → "0050"
            String cuf5 = cufGenerator.generate(NIT, FECHA, 5, 2, 1, 1, 1L, 0, CUFD);
            String cuf50 = cufGenerator.generate(NIT, FECHA, 50, 2, 1, 1, 1L, 0, CUFD);
            assertThat(cuf5).isNotEqualTo(cuf50);
        }
    }

    @Nested
    @DisplayName("generateQrContent() tests")
    class GenerateQrContentTests {

        @Test
        @DisplayName("Should return non-null QR content string")
        void shouldReturnNonNullQrContent() {
            String qr = cufGenerator.generateQrContent(NIT, FECHA,
                    new BigDecimal("150.00"), 1L, CUFD, "CAFCUF", "Leyenda", "CTRL123");
            assertThat(qr).isNotNull().isNotBlank();
        }

        @Test
        @DisplayName("QR content should contain NIT")
        void shouldContainNit() {
            String qr = cufGenerator.generateQrContent(NIT, FECHA,
                    new BigDecimal("150.00"), 1L, CUFD, "CAFCUF", null, null);
            assertThat(qr).contains(NIT);
        }

        @Test
        @DisplayName("QR content should contain invoice total")
        void shouldContainImporteTotal() {
            String qr = cufGenerator.generateQrContent(NIT, FECHA,
                    new BigDecimal("250.50"), 1L, CUFD, "CAFCUF", null, null);
            assertThat(qr).contains("250.50");
        }

        @Test
        @DisplayName("QR content fields separated by pipe '|'")
        void shouldHavePipeSeparatedFields() {
            String qr = cufGenerator.generateQrContent(NIT, FECHA,
                    new BigDecimal("100.00"), 1L, CUFD, "CAFCUF", "Leyenda", "CTRL");
            String[] parts = qr.split("\\|");
            assertThat(parts).hasSize(8);
        }

        @Test
        @DisplayName("First field should be NIT")
        void firstFieldIsNit() {
            String qr = cufGenerator.generateQrContent(NIT, FECHA,
                    new BigDecimal("100.00"), 1L, CUFD, "CAFCUF", "Ley", "CTRL");
            assertThat(qr.split("\\|")[0]).isEqualTo(NIT);
        }

        @Test
        @DisplayName("Null leyenda should produce empty string field")
        void nullLeyendaProducesEmptyField() {
            String qr = cufGenerator.generateQrContent(NIT, FECHA,
                    new BigDecimal("100.00"), 1L, CUFD, "CAFCUF", null, "CTRL");
            String[] parts = qr.split("\\|", -1);
            assertThat(parts[6]).isEqualTo("");
        }

        @Test
        @DisplayName("Null codigoControl should produce empty string field")
        void nullCodigoControlProducesEmptyField() {
            String qr = cufGenerator.generateQrContent(NIT, FECHA,
                    new BigDecimal("100.00"), 1L, CUFD, "CAFCUF", "Ley", null);
            String[] parts = qr.split("\\|", -1);
            assertThat(parts[7]).isEqualTo("");
        }

        @Test
        @DisplayName("Fecha should be formatted as yyyyMMddHHmmss")
        void fechaShouldBeFormatted() {
            String qr = cufGenerator.generateQrContent(NIT, FECHA,
                    new BigDecimal("100.00"), 1L, CUFD, "CAFCUF", null, null);
            assertThat(qr).contains("20260311103000");
        }
    }
}
