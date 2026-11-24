package com.example.library.controllers;

import com.example.library.dtos.LoanRequestDTO;
import com.example.library.dtos.LoanResponseDTO;
import com.example.library.dtos.LoanUpdateReturnDateDTO;
import com.example.library.entities.enums.LoanStatus;
import com.example.library.services.LoanService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping(value = "/loans")
public class LoanController {

    private final LoanService loanService;

    public LoanController(LoanService loanService) {
        this.loanService = loanService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Page<LoanResponseDTO>> findAll(Pageable pageable,
                                                         @RequestParam(name = "email", required = false) String email,
                                                         @RequestParam(name = "status", required = false) LoanStatus status) {

        Page<LoanResponseDTO> dtos = loanService.findAll(pageable, email, status);
        return ResponseEntity.ok().body(dtos);
    }

    @GetMapping(value = "/{id}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_USER')")
    public ResponseEntity<LoanResponseDTO> findById(@PathVariable Long id) {
        LoanResponseDTO dto = loanService.findById(id);
        return ResponseEntity.ok().body(dto);
    }

    @GetMapping(value = "/me")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_USER')")
    public ResponseEntity<Page<LoanResponseDTO>> findMyLoans(Pageable pageable) {
        Page<LoanResponseDTO> dtos = loanService.findMyLoans(pageable);
        return ResponseEntity.ok().body(dtos);
    }

    @PostMapping
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<LoanResponseDTO> insert(@Valid @RequestBody LoanRequestDTO request) {
        LoanResponseDTO dto = loanService.insert(request);
        URI uri = ServletUriComponentsBuilder.fromCurrentRequestUri().path("/{id}").buildAndExpand(dto.id()).toUri();
        return ResponseEntity.created(uri).body(dto);
    }

    @PatchMapping(value = "/{id}/expected-return-date")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<LoanResponseDTO> updateExpectedReturnDate(@PathVariable Long id,
                                                                    @Valid @RequestBody LoanUpdateReturnDateDTO request) {
        LoanResponseDTO dto = loanService.updateExpectedReturnDate(id, request);
        return ResponseEntity.ok().body(dto);
    }

    @PatchMapping(value = "/{id}/return")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<LoanResponseDTO> processReturn(@PathVariable Long id) {
        LoanResponseDTO dto = loanService.processReturn(id);
        return ResponseEntity.ok().body(dto);
    }

    @PatchMapping(value = "/{id}/cancel")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<LoanResponseDTO> cancel(@PathVariable Long id) {
        LoanResponseDTO dto = loanService.cancel(id);
        return ResponseEntity.ok().body(dto);
    }

}
