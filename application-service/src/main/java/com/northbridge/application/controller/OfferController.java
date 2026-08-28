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

@Slf4j
@RestController
@RequestMapping("/api/offers")
@RequiredArgsConstructor
public class OfferController {

    private final OfferService offerService;

    @GetMapping("/{applicationId}")
    public ResponseEntity<Offer> getOffer(@PathVariable String applicationId) {
        return offerService.findByApplicationId(applicationId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/{applicationId}/accept")
    public ResponseEntity<?> acceptOffer(@PathVariable String applicationId) {
        try {
            Offer offer = offerService.acceptOffer(applicationId);
            return ResponseEntity.ok(offer);
        } catch (EntityNotFoundException ex) {
            log.warn("Offer accept requested for unknown applicationId={}", applicationId);
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException ex) {
            log.warn("Rejecting offer acceptance for applicationId={} because offer is not pending", applicationId, ex);
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", ex.getMessage()));
        }
    }
}
