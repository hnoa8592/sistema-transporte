package com.transporte.pasajes.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.transporte.core.exception.GlobalExceptionHandler;
import com.transporte.core.exception.ResourceNotFoundException;
import com.transporte.pasajes.dto.SeatMapResponse;
import com.transporte.pasajes.enums.SeatStatus;
import com.transporte.pasajes.service.SeatMapService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for SeatMapController.
 *
 * Uses MockMvc in standalone mode with the real GlobalExceptionHandler to verify
 * HTTP response codes, content types, and response body structure.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SeatMapController Unit Tests")
class SeatMapControllerTest {

    @Mock
    private SeatMapService seatMapService;

    @InjectMocks
    private SeatMapController seatMapController;

    private MockMvc mockMvc;

    private static final String BASE_URL = "/api/v1/seat-maps";
    private UUID scheduleId;
    private LocalDate travelDate;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        mockMvc = MockMvcBuilders.standaloneSetup(seatMapController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();

        scheduleId = UUID.randomUUID();
        travelDate = LocalDate.of(2026, 3, 10);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private SeatMapResponse buildResponse(int seatNum, int floor, SeatStatus status) {
        return new SeatMapResponse(UUID.randomUUID(), scheduleId, travelDate, seatNum, floor, status, null);
    }

    private String dateParam() {
        return travelDate.toString(); // "2026-03-10"
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /{scheduleId}
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /{scheduleId} — getSeatMap()")
    class GetSeatMapTests {

        @Test
        @DisplayName("Should return 200 with seat list for an existing seat map")
        void shouldReturn200WithExistingSeatMap() throws Exception {
            List<SeatMapResponse> seats = List.of(
                    buildResponse(1, 1, SeatStatus.AVAILABLE),
                    buildResponse(2, 1, SeatStatus.SOLD),
                    buildResponse(3, 1, SeatStatus.RESERVED),
                    buildResponse(4, 1, SeatStatus.AVAILABLE)
            );
            given(seatMapService.getSeatMap(scheduleId, travelDate)).willReturn(seats);

            mockMvc.perform(get(BASE_URL + "/{scheduleId}", scheduleId)
                            .param("travelDate", dateParam())
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data", hasSize(4)))
                    .andExpect(jsonPath("$.data[0].seatNumber").value(1))
                    .andExpect(jsonPath("$.data[0].floorNumber").value(1))
                    .andExpect(jsonPath("$.data[0].status").value("AVAILABLE"))
                    .andExpect(jsonPath("$.data[1].status").value("SOLD"))
                    .andExpect(jsonPath("$.data[2].status").value("RESERVED"));

            verify(seatMapService).getSeatMap(scheduleId, travelDate);
        }

        @Test
        @DisplayName("Should return 200 with auto-generated seats when none existed before")
        void shouldReturn200WithAutoGeneratedSeats() throws Exception {
            List<SeatMapResponse> generated = List.of(
                    buildResponse(1, 1, SeatStatus.AVAILABLE),
                    buildResponse(2, 1, SeatStatus.AVAILABLE),
                    buildResponse(3, 1, SeatStatus.AVAILABLE)
            );
            given(seatMapService.getSeatMap(scheduleId, travelDate)).willReturn(generated);

            mockMvc.perform(get(BASE_URL + "/{scheduleId}", scheduleId)
                            .param("travelDate", dateParam())
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data", hasSize(3)))
                    .andExpect(jsonPath("$.data[*].status", everyItem(is("AVAILABLE"))));
        }

        @Test
        @DisplayName("Should return 200 with empty list when service returns no seats")
        void shouldReturn200WithEmptyList() throws Exception {
            given(seatMapService.getSeatMap(scheduleId, travelDate)).willReturn(List.of());

            mockMvc.perform(get(BASE_URL + "/{scheduleId}", scheduleId)
                            .param("travelDate", dateParam())
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data", hasSize(0)));
        }

        @Test
        @DisplayName("Should return 404 when schedule is not found")
        void shouldReturn404WhenScheduleNotFound() throws Exception {
            given(seatMapService.getSeatMap(scheduleId, travelDate))
                    .willThrow(new ResourceNotFoundException("Schedule", scheduleId));

            mockMvc.perform(get(BASE_URL + "/{scheduleId}", scheduleId)
                            .param("travelDate", dateParam())
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errorCode").value("NOT_FOUND"))
                    .andExpect(jsonPath("$.message").value(containsString("Schedule")));
        }

        @Test
        @DisplayName("Should return 400 when travelDate parameter is missing")
        void shouldReturn400WhenTravelDateMissing() throws Exception {
            mockMvc.perform(get(BASE_URL + "/{scheduleId}", scheduleId)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(seatMapService);
        }

        @Test
        @DisplayName("Should return 400 when travelDate format is invalid")
        void shouldReturn400WhenTravelDateFormatIsInvalid() throws Exception {
            mockMvc.perform(get(BASE_URL + "/{scheduleId}", scheduleId)
                            .param("travelDate", "not-a-date")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(seatMapService);
        }

        @Test
        @DisplayName("Should include scheduleId and travelDate in each seat response")
        void shouldIncludeScheduleIdAndTravelDateInResponse() throws Exception {
            SeatMapResponse seat = buildResponse(5, 2, SeatStatus.AVAILABLE);
            given(seatMapService.getSeatMap(scheduleId, travelDate)).willReturn(List.of(seat));

            mockMvc.perform(get(BASE_URL + "/{scheduleId}", scheduleId)
                            .param("travelDate", dateParam())
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[0].scheduleId").value(scheduleId.toString()))
                    .andExpect(jsonPath("$.data[0].travelDate").value(dateParam()))
                    .andExpect(jsonPath("$.data[0].seatNumber").value(5))
                    .andExpect(jsonPath("$.data[0].floorNumber").value(2));
        }

        @Test
        @DisplayName("Should return seats for two-floor bus with correct floor numbers")
        void shouldReturnSeatsWithCorrectFloorNumbers() throws Exception {
            List<SeatMapResponse> seats = List.of(
                    buildResponse(1, 1, SeatStatus.AVAILABLE),
                    buildResponse(2, 1, SeatStatus.AVAILABLE),
                    buildResponse(1, 2, SeatStatus.AVAILABLE),
                    buildResponse(2, 2, SeatStatus.SOLD)
            );
            given(seatMapService.getSeatMap(scheduleId, travelDate)).willReturn(seats);

            mockMvc.perform(get(BASE_URL + "/{scheduleId}", scheduleId)
                            .param("travelDate", dateParam())
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(4)))
                    .andExpect(jsonPath("$.data[0].floorNumber").value(1))
                    .andExpect(jsonPath("$.data[2].floorNumber").value(2))
                    .andExpect(jsonPath("$.data[3].status").value("SOLD"));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /{scheduleId}/available-count
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /{scheduleId}/available-count — countAvailable()")
    class CountAvailableTests {

        @Test
        @DisplayName("Should return 200 with available seat count")
        void shouldReturn200WithAvailableCount() throws Exception {
            given(seatMapService.countAvailableSeats(scheduleId, travelDate)).willReturn(35L);

            mockMvc.perform(get(BASE_URL + "/{scheduleId}/available-count", scheduleId)
                            .param("travelDate", dateParam())
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.availableSeats").value(35));

            verify(seatMapService).countAvailableSeats(scheduleId, travelDate);
        }

        @Test
        @DisplayName("Should return 200 with zero when no seats are available")
        void shouldReturn200WithZeroCount() throws Exception {
            given(seatMapService.countAvailableSeats(scheduleId, travelDate)).willReturn(0L);

            mockMvc.perform(get(BASE_URL + "/{scheduleId}/available-count", scheduleId)
                            .param("travelDate", dateParam())
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.availableSeats").value(0));
        }

        @Test
        @DisplayName("Should return 200 with full bus capacity when all seats available")
        void shouldReturnFullCapacityCount() throws Exception {
            given(seatMapService.countAvailableSeats(scheduleId, travelDate)).willReturn(42L);

            mockMvc.perform(get(BASE_URL + "/{scheduleId}/available-count", scheduleId)
                            .param("travelDate", dateParam())
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.availableSeats").value(42));
        }

        @Test
        @DisplayName("Should return 400 when travelDate parameter is missing")
        void shouldReturn400WhenTravelDateMissing() throws Exception {
            mockMvc.perform(get(BASE_URL + "/{scheduleId}/available-count", scheduleId)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(seatMapService);
        }

        @Test
        @DisplayName("Should return 400 when travelDate format is invalid")
        void shouldReturn400WhenTravelDateFormatIsInvalid() throws Exception {
            mockMvc.perform(get(BASE_URL + "/{scheduleId}/available-count", scheduleId)
                            .param("travelDate", "2026/03/10")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(seatMapService);
        }

        @Test
        @DisplayName("Should respond with correct JSON structure in data field")
        void shouldRespondWithCorrectJsonStructure() throws Exception {
            given(seatMapService.countAvailableSeats(scheduleId, travelDate)).willReturn(20L);

            mockMvc.perform(get(BASE_URL + "/{scheduleId}/available-count", scheduleId)
                            .param("travelDate", dateParam())
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isMap())
                    .andExpect(jsonPath("$.data.availableSeats").exists())
                    .andExpect(jsonPath("$.data.availableSeats").value(20));
        }
    }
}
