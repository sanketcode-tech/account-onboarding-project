package com.northbridge.application.controller;

import com.northbridge.application.model.Offer;
import com.northbridge.application.service.OfferService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller exposing offer view and accept endpoints for customers.
 */
@Slf4j
@RestController
@RequestMapping("/api/offers")
@RequiredArgsConstructor
public class OfferController {

    private final OfferService offerService;
    private final com.northbridge.application.auth.AuthClient authClient;

    @GetMapping("/{applicationId}")
    public ResponseEntity<Offer> getOffer(@PathVariable String applicationId, @RequestHeader(value = "Authorization", required = false) String authorization) {
        var opt = offerService.findByApplicationId(applicationId);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        Offer offer = opt.get();
        // Verify ownership using auth-client
        try {
            String subject = authClient.validateAndGetSubject(authorization);
            if (!subject.equals(offer.getCustomerId())) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            return ResponseEntity.ok(offer);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @PostMapping("/{applicationId}/accept")
    public ResponseEntity<?> acceptOffer(@PathVariable String applicationId, @RequestHeader(value = "Authorization", required = false) String authorization) {
        try {
            var opt = offerService.findByApplicationId(applicationId);
            if (opt.isEmpty()) return ResponseEntity.notFound().build();
            Offer offer = opt.get();
            String subject = authClient.validateAndGetSubject(authorization);
            if (!subject.equals(offer.getCustomerId())) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

            Offer accepted = offerService.acceptOffer(applicationId);
            return ResponseEntity.ok(accepted);
        } catch (EntityNotFoundException ex) {
            log.warn("Offer accept requested for unknown applicationId={}", applicationId);
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException ex) {
            log.warn("Rejecting offer acceptance for applicationId={} because offer is not pending", applicationId, ex);
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", ex.getMessage()));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }
}
