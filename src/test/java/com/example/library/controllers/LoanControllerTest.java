package com.example.library.controllers;

import com.example.library.config.ResourceServerConfig;
import com.example.library.dtos.BookMinDTO;
import com.example.library.dtos.LoanRequestDTO;
import com.example.library.dtos.LoanResponseDTO;
import com.example.library.dtos.LoanUpdateReturnDateDTO;
import com.example.library.dtos.UserMinDTO;
import com.example.library.entities.enums.LoanStatus;
import com.example.library.exceptions.ActiveLoanDelinquencyException;
import com.example.library.exceptions.BookUnavailableException;
import com.example.library.exceptions.ForbiddenException;
import com.example.library.exceptions.InvalidDateException;
import com.example.library.exceptions.ResourceNotFoundException;
import com.example.library.exceptions.StatusInvalidException;
import com.example.library.services.LoanService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.h2console.autoconfigure.H2ConsoleProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LoanController.class)
@ActiveProfiles("test")
@Import(ResourceServerConfig.class)
@EnableConfigurationProperties(H2ConsoleProperties.class)
public class LoanControllerTest {

    private static final String NOT_FOUND_MESSAGE = "Resource not found, id: 99";
    private static final String STATUS_INVALID_MESSAGE = "Only active loans can be modified";
    private static final String OVERDUE_MESSAGE = "It is not possible to perform this operation with overdue loans";

    private static final String VALID_REQUEST_JSON = """
            {
                "userId": 1,
                "bookId": 1
            }
            """;

    private static final LocalDate LOAN_DATE = LocalDate.of(2026, 11, 20);
    private static final LocalDate EXPECTED_RETURN_DATE = LocalDate.of(2026, 12, 20);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LoanService loanService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    private static RequestPostProcessor admin() {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    private static RequestPostProcessor user() {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"));
    }

    private static LoanResponseDTO loanResponse(Long id, LocalDate expectedReturnDate, LocalDateTime timeReturn,
                                                LoanStatus status) {
        return new LoanResponseDTO(id, LOAN_DATE, expectedReturnDate, timeReturn, status,
                new UserMinDTO(1L, "Maria Silva", "maria@gmail.com", "(11) 99999-0001"),
                new BookMinDTO(1L, "Dom Casmurro", "9788535914849"));
    }

    private static LoanResponseDTO activeLoan(Long id) {
        return loanResponse(id, EXPECTED_RETURN_DATE, null, LoanStatus.ACTIVE);
    }

    private static String updateDateJson(LocalDate date) {
        return """
                {
                    "expectedReturnDate": "%s"
                }
                """.formatted(date);
    }

    // ---------- GET /loans ----------

    @Test
    void shouldReturn200WhenPaginatingAllLoans() throws Exception {

        //arrange
        List<LoanResponseDTO> dtos = List.of(
                activeLoan(1L),
                loanResponse(2L, EXPECTED_RETURN_DATE, LocalDateTime.of(2026, 11, 22, 10, 30, 15), LoanStatus.RETURNED)
        );
        when(loanService.findAll(any(Pageable.class), isNull(), isNull()))
                .thenReturn(new PageImpl<>(dtos, PageRequest.of(0, 20), 2));

        //act and assert
        mockMvc.perform(get("/loans").with(admin())
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].loanDate").value("2026-11-20"))
                .andExpect(jsonPath("$.content[0].expectedReturnDate").value("2026-12-20"))
                .andExpect(jsonPath("$.content[0].timeReturn").doesNotExist())
                .andExpect(jsonPath("$.content[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$.content[0].user.id").value(1))
                .andExpect(jsonPath("$.content[0].user.name").value("Maria Silva"))
                .andExpect(jsonPath("$.content[0].user.email").value("maria@gmail.com"))
                .andExpect(jsonPath("$.content[0].user.phone").value("(11) 99999-0001"))
                .andExpect(jsonPath("$.content[0].book.id").value(1))
                .andExpect(jsonPath("$.content[0].book.title").value("Dom Casmurro"))
                .andExpect(jsonPath("$.content[0].book.isbn").value("9788535914849"))
                .andExpect(jsonPath("$.content[1].id").value(2))
                .andExpect(jsonPath("$.content[1].timeReturn").value("2026-11-22T10:30:15"))
                .andExpect(jsonPath("$.content[1].status").value("RETURNED"))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1));

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(loanService).findAll(captor.capture(), isNull(), isNull());
        assertThat(captor.getValue().getPageNumber()).isEqualTo(0);
        assertThat(captor.getValue().getPageSize()).isEqualTo(20);
    }

    @Test
    void shouldReturn200WhenPaginatingLoansWithFilters() throws Exception {

        //arrange
        when(loanService.findAll(any(Pageable.class), eq("maria@gmail.com"), eq(LoanStatus.ACTIVE)))
                .thenReturn(new PageImpl<>(List.of(activeLoan(1L)), PageRequest.of(0, 20), 1));

        //act and assert
        mockMvc.perform(get("/loans").with(admin())
                        .param("email", "maria@gmail.com")
                        .param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(loanService).findAll(any(Pageable.class), eq("maria@gmail.com"), eq(LoanStatus.ACTIVE));
    }

    @Test
    void shouldReturn200WhenLoanPaginationIsEmpty() throws Exception {

        //arrange
        when(loanService.findAll(any(Pageable.class), isNull(), isNull()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        //act and assert
        mockMvc.perform(get("/loans").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());
    }

    @Test
    void shouldReturn400WhenPaginatingLoansWithInvalidStatus() throws Exception {

        //act and assert
        mockMvc.perform(get("/loans").with(admin()).param("status", "LOST"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(loanService);
    }

    // ---------- GET /loans/{id} ----------

    @Test
    void shouldReturn200WhenFindingExistingLoan() throws Exception {

        //arrange
        when(loanService.findById(1L)).thenReturn(activeLoan(1L));

        //act and assert
        mockMvc.perform(get("/loans/{id}", 1L).with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.loanDate").value("2026-11-20"))
                .andExpect(jsonPath("$.expectedReturnDate").value("2026-12-20"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.user.id").value(1))
                .andExpect(jsonPath("$.book.id").value(1));

        verify(loanService).findById(1L);
    }

    @Test
    void shouldReturn404WhenFindingNonExistentLoan() throws Exception {

        //arrange
        when(loanService.findById(99L)).thenThrow(new ResourceNotFoundException(99L));

        //act and assert
        mockMvc.perform(get("/loans/{id}", 99L).with(admin()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Resource not found"))
                .andExpect(jsonPath("$.message").value(NOT_FOUND_MESSAGE))
                .andExpect(jsonPath("$.path").value("/loans/99"));
    }

    @Test
    void shouldReturn403WhenUserFindsLoanOfAnotherUser() throws Exception {

        //arrange
        when(loanService.findById(1L)).thenThrow(new ForbiddenException("Access denied"));

        //act and assert
        mockMvc.perform(get("/loans/{id}", 1L).with(user()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("Access denied"))
                .andExpect(jsonPath("$.path").value("/loans/1"));
    }

    // ---------- GET /loans/me ----------

    @Test
    void shouldReturn200WhenFindingMyLoans() throws Exception {

        //arrange
        when(loanService.findMyLoans(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(activeLoan(1L)), PageRequest.of(0, 20), 1));

        //act and assert
        mockMvc.perform(get("/loans/me").with(user()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].user.email").value("maria@gmail.com"))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(loanService).findMyLoans(any(Pageable.class));
        verify(loanService, never()).findById(any());
    }

    // ---------- POST /loans ----------

    @Test
    void shouldReturn201WhenInsertingLoanWithValidData() throws Exception {

        //arrange
        when(loanService.insert(any(LoanRequestDTO.class))).thenReturn(activeLoan(10L));

        //act and assert
        mockMvc.perform(post("/loans").with(admin()).contentType(MediaType.APPLICATION_JSON).content(VALID_REQUEST_JSON))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/loans/10"))
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.user.id").value(1))
                .andExpect(jsonPath("$.book.id").value(1));

        ArgumentCaptor<LoanRequestDTO> captor = ArgumentCaptor.forClass(LoanRequestDTO.class);
        verify(loanService).insert(captor.capture());
        assertThat(captor.getValue()).isEqualTo(new LoanRequestDTO(1L, 1L));
    }

    @Test
    void shouldReturn422WhenInsertingLoanWithNullFields() throws Exception {

        //arrange
        String json = """
                {
                    "userId": null,
                    "bookId": null
                }
                """;

        //act and assert
        mockMvc.perform(post("/loans").with(admin()).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.error").value("Entity validation error"))
                .andExpect(jsonPath("$.message").value("Error in data validation"))
                .andExpect(jsonPath("$.errors.length()").value(2))
                .andExpect(jsonPath("$.errors[?(@.fieldName == 'userId')].message").value("User id is required"))
                .andExpect(jsonPath("$.errors[?(@.fieldName == 'bookId')].message").value("Book id is required"));

        verify(loanService, never()).insert(any(LoanRequestDTO.class));
    }

    @Test
    void shouldReturn404WhenInsertingLoanWithNonExistentResource() throws Exception {

        //arrange
        when(loanService.insert(any(LoanRequestDTO.class))).thenThrow(new ResourceNotFoundException(99L));

        //act and assert
        mockMvc.perform(post("/loans").with(admin()).contentType(MediaType.APPLICATION_JSON).content(VALID_REQUEST_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(NOT_FOUND_MESSAGE))
                .andExpect(jsonPath("$.path").value("/loans"));
    }

    @Test
    void shouldReturn409WhenInsertingLoanWithUnavailableBook() throws Exception {

        //arrange
        when(loanService.insert(any(LoanRequestDTO.class)))
                .thenThrow(new BookUnavailableException("Insufficient number of copies for this operation"));

        //act and assert
        mockMvc.perform(post("/loans").with(admin()).contentType(MediaType.APPLICATION_JSON).content(VALID_REQUEST_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Book unavailable"))
                .andExpect(jsonPath("$.message").value("Insufficient number of copies for this operation"))
                .andExpect(jsonPath("$.path").value("/loans"));
    }

    @Test
    void shouldReturn400WhenInsertingLoanForUserWithOverdueLoans() throws Exception {

        //arrange
        when(loanService.insert(any(LoanRequestDTO.class))).thenThrow(new ActiveLoanDelinquencyException(OVERDUE_MESSAGE));

        //act and assert
        mockMvc.perform(post("/loans").with(admin()).contentType(MediaType.APPLICATION_JSON).content(VALID_REQUEST_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Active loan delinquency"))
                .andExpect(jsonPath("$.message").value(OVERDUE_MESSAGE));
    }

    @Test
    void shouldReturn409WhenInsertingLoanAndBookIsLocked() throws Exception {

        //arrange
        when(loanService.insert(any(LoanRequestDTO.class)))
                .thenThrow(new PessimisticLockingFailureException("lock timeout"));

        //act and assert
        mockMvc.perform(post("/loans").with(admin()).contentType(MediaType.APPLICATION_JSON).content(VALID_REQUEST_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Resource locked"))
                .andExpect(jsonPath("$.message").value("The resource is being modified by another request, please try again"));
    }

    // ---------- PATCH /loans/{id}/expected-return-date ----------

    @Test
    void shouldReturn200WhenUpdatingExpectedReturnDate() throws Exception {

        //arrange
        LocalDate newDate = LocalDate.now().plusDays(20);
        when(loanService.updateExpectedReturnDate(eq(1L), any(LoanUpdateReturnDateDTO.class)))
                .thenReturn(loanResponse(1L, newDate, null, LoanStatus.ACTIVE));

        //act and assert
        mockMvc.perform(patch("/loans/{id}/expected-return-date", 1L).with(admin())
                        .contentType(MediaType.APPLICATION_JSON).content(updateDateJson(newDate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.expectedReturnDate").value(newDate.toString()))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        ArgumentCaptor<LoanUpdateReturnDateDTO> captor = ArgumentCaptor.forClass(LoanUpdateReturnDateDTO.class);
        verify(loanService).updateExpectedReturnDate(eq(1L), captor.capture());
        assertThat(captor.getValue()).isEqualTo(new LoanUpdateReturnDateDTO(newDate));
    }

    @Test
    void shouldReturn422WhenUpdatingExpectedReturnDateWithNullDate() throws Exception {

        //arrange
        String json = """
                {
                    "expectedReturnDate": null
                }
                """;

        //act and assert
        mockMvc.perform(patch("/loans/{id}/expected-return-date", 1L).with(admin())
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errors.length()").value(1))
                .andExpect(jsonPath("$.errors[0].fieldName").value("expectedReturnDate"))
                .andExpect(jsonPath("$.errors[0].message").value("Expected return date is required"));

        verify(loanService, never()).updateExpectedReturnDate(any(), any());
    }

    @Test
    void shouldReturn422WhenUpdatingExpectedReturnDateWithPastDate() throws Exception {

        //act and assert
        mockMvc.perform(patch("/loans/{id}/expected-return-date", 1L).with(admin())
                        .contentType(MediaType.APPLICATION_JSON).content(updateDateJson(LocalDate.now().minusDays(1))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errors.length()").value(1))
                .andExpect(jsonPath("$.errors[0].fieldName").value("expectedReturnDate"))
                .andExpect(jsonPath("$.errors[0].message").value("Invalid date"));

        verify(loanService, never()).updateExpectedReturnDate(any(), any());
    }

    @Test
    void shouldReturn422WhenUpdatingExpectedReturnDateWithTodayDate() throws Exception {

        //act and assert
        mockMvc.perform(patch("/loans/{id}/expected-return-date", 1L).with(admin())
                        .contentType(MediaType.APPLICATION_JSON).content(updateDateJson(LocalDate.now())))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errors[0].message").value("Invalid date"));

        verify(loanService, never()).updateExpectedReturnDate(any(), any());
    }

    @Test
    void shouldReturn400WhenUpdatingExpectedReturnDateBeyondTheLimit() throws Exception {

        //arrange
        when(loanService.updateExpectedReturnDate(eq(1L), any(LoanUpdateReturnDateDTO.class)))
                .thenThrow(new InvalidDateException("Impossible to update to this date"));

        //act and assert
        mockMvc.perform(patch("/loans/{id}/expected-return-date", 1L).with(admin())
                        .contentType(MediaType.APPLICATION_JSON).content(updateDateJson(LocalDate.now().plusDays(90))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Invalid date"))
                .andExpect(jsonPath("$.message").value("Impossible to update to this date"))
                .andExpect(jsonPath("$.path").value("/loans/1/expected-return-date"));
    }

    @Test
    void shouldReturn409WhenUpdatingExpectedReturnDateOfNonActiveLoan() throws Exception {

        //arrange
        when(loanService.updateExpectedReturnDate(eq(1L), any(LoanUpdateReturnDateDTO.class)))
                .thenThrow(new StatusInvalidException(STATUS_INVALID_MESSAGE));

        //act and assert
        mockMvc.perform(patch("/loans/{id}/expected-return-date", 1L).with(admin())
                        .contentType(MediaType.APPLICATION_JSON).content(updateDateJson(LocalDate.now().plusDays(10))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Status invalid"))
                .andExpect(jsonPath("$.message").value(STATUS_INVALID_MESSAGE));
    }

    @Test
    void shouldReturn400WhenUpdatingExpectedReturnDateForUserWithOverdueLoans() throws Exception {

        //arrange
        when(loanService.updateExpectedReturnDate(eq(1L), any(LoanUpdateReturnDateDTO.class)))
                .thenThrow(new ActiveLoanDelinquencyException(OVERDUE_MESSAGE));

        //act and assert
        mockMvc.perform(patch("/loans/{id}/expected-return-date", 1L).with(admin())
                        .contentType(MediaType.APPLICATION_JSON).content(updateDateJson(LocalDate.now().plusDays(10))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Active loan delinquency"))
                .andExpect(jsonPath("$.message").value(OVERDUE_MESSAGE));
    }

    @Test
    void shouldReturn404WhenUpdatingExpectedReturnDateOfNonExistentLoan() throws Exception {

        //arrange
        when(loanService.updateExpectedReturnDate(eq(99L), any(LoanUpdateReturnDateDTO.class)))
                .thenThrow(new ResourceNotFoundException(99L));

        //act and assert
        mockMvc.perform(patch("/loans/{id}/expected-return-date", 99L).with(admin())
                        .contentType(MediaType.APPLICATION_JSON).content(updateDateJson(LocalDate.now().plusDays(10))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(NOT_FOUND_MESSAGE))
                .andExpect(jsonPath("$.path").value("/loans/99/expected-return-date"));
    }

    // ---------- PATCH /loans/{id}/return ----------

    @Test
    void shouldReturn200WhenProcessingReturn() throws Exception {

        //arrange
        when(loanService.processReturn(1L)).thenReturn(
                loanResponse(1L, EXPECTED_RETURN_DATE, LocalDateTime.of(2026, 11, 24, 10, 30, 15), LoanStatus.RETURNED));

        //act and assert
        mockMvc.perform(patch("/loans/{id}/return", 1L).with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("RETURNED"))
                .andExpect(jsonPath("$.timeReturn").value("2026-11-24T10:30:15"));

        verify(loanService).processReturn(1L);
    }

    @Test
    void shouldReturn409WhenProcessingReturnOfNonActiveLoan() throws Exception {

        //arrange
        when(loanService.processReturn(1L)).thenThrow(new StatusInvalidException(STATUS_INVALID_MESSAGE));

        //act and assert
        mockMvc.perform(patch("/loans/{id}/return", 1L).with(admin()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Status invalid"))
                .andExpect(jsonPath("$.message").value(STATUS_INVALID_MESSAGE))
                .andExpect(jsonPath("$.path").value("/loans/1/return"));
    }

    @Test
    void shouldReturn404WhenProcessingReturnOfNonExistentLoan() throws Exception {

        //arrange
        when(loanService.processReturn(99L)).thenThrow(new ResourceNotFoundException(99L));

        //act and assert
        mockMvc.perform(patch("/loans/{id}/return", 99L).with(admin()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(NOT_FOUND_MESSAGE));
    }

    // ---------- PATCH /loans/{id}/cancel ----------

    @Test
    void shouldReturn200WhenCancelingLoan() throws Exception {

        //arrange
        when(loanService.cancel(1L)).thenReturn(loanResponse(1L, EXPECTED_RETURN_DATE, null, LoanStatus.CANCELED));

        //act and assert
        mockMvc.perform(patch("/loans/{id}/cancel", 1L).with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("CANCELED"))
                .andExpect(jsonPath("$.timeReturn").doesNotExist());

        verify(loanService).cancel(1L);
    }

    @Test
    void shouldReturn409WhenCancelingNonActiveLoan() throws Exception {

        //arrange
        when(loanService.cancel(1L)).thenThrow(new StatusInvalidException(STATUS_INVALID_MESSAGE));

        //act and assert
        mockMvc.perform(patch("/loans/{id}/cancel", 1L).with(admin()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Status invalid"))
                .andExpect(jsonPath("$.message").value(STATUS_INVALID_MESSAGE))
                .andExpect(jsonPath("$.path").value("/loans/1/cancel"));
    }

    @Test
    void shouldReturn404WhenCancelingNonExistentLoan() throws Exception {

        //arrange
        when(loanService.cancel(99L)).thenThrow(new ResourceNotFoundException(99L));

        //act and assert
        mockMvc.perform(patch("/loans/{id}/cancel", 99L).with(admin()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(NOT_FOUND_MESSAGE));
    }

    // ---------- SECURITY ----------

    @Test
    void shouldReturn401WhenRequestHasNoToken() throws Exception {

        //act and assert
        mockMvc.perform(get("/loans/me"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(loanService);
    }

    @Test
    void shouldReturn403WhenUserTriesToListAllLoans() throws Exception {

        //act and assert
        mockMvc.perform(get("/loans").with(user()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(loanService);
    }

    @Test
    void shouldReturn200WhenUserFindsOwnLoanById() throws Exception {

        //arrange
        when(loanService.findById(1L)).thenReturn(activeLoan(1L));

        //act and assert
        mockMvc.perform(get("/loans/{id}", 1L).with(user()))
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturn403WhenUserTriesToInsertLoan() throws Exception {

        //act and assert
        mockMvc.perform(post("/loans").with(user()).contentType(MediaType.APPLICATION_JSON).content(VALID_REQUEST_JSON))
                .andExpect(status().isForbidden());

        verifyNoInteractions(loanService);
    }

    @Test
    void shouldReturn403WhenUserTriesToUpdateExpectedReturnDate() throws Exception {

        //act and assert
        mockMvc.perform(patch("/loans/{id}/expected-return-date", 1L).with(user())
                        .contentType(MediaType.APPLICATION_JSON).content(updateDateJson(LocalDate.now().plusDays(10))))
                .andExpect(status().isForbidden());

        verifyNoInteractions(loanService);
    }

    @Test
    void shouldReturn403WhenUserTriesToProcessReturn() throws Exception {

        //act and assert
        mockMvc.perform(patch("/loans/{id}/return", 1L).with(user()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(loanService);
    }

    @Test
    void shouldReturn403WhenUserTriesToCancelLoan() throws Exception {

        //act and assert
        mockMvc.perform(patch("/loans/{id}/cancel", 1L).with(user()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(loanService);
    }
}
