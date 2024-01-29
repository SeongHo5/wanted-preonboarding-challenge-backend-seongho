package com.wanted.preonboarding.ticket.application.reservation.service;

import com.wanted.preonboarding.core.domain.response.ResponseHandler;
import com.wanted.preonboarding.ticket.application.common.exception.ServiceFailedException;
import com.wanted.preonboarding.ticket.application.reservation.event.ReservationCancelledEvent;
import com.wanted.preonboarding.ticket.application.common.exception.ArgumentNotValidException;
import com.wanted.preonboarding.ticket.application.common.exception.EntityNotFoundException;
import com.wanted.preonboarding.ticket.application.common.exception.SeatNotAvailableException;
import com.wanted.preonboarding.ticket.application.performance.repository.PerformanceSeatInfoRepository;
import com.wanted.preonboarding.ticket.application.reservation.repository.ReservationRepository;
import com.wanted.preonboarding.ticket.domain.dto.response.PaymentResponse;
import com.wanted.preonboarding.ticket.domain.dto.request.RequestReservation;
import com.wanted.preonboarding.ticket.domain.dto.response.ReservationResponse;
import com.wanted.preonboarding.ticket.domain.entity.Performance;
import com.wanted.preonboarding.ticket.domain.entity.PerformanceSeatInfo;
import com.wanted.preonboarding.ticket.domain.entity.Reservation;
import com.wanted.preonboarding.ticket.domain.enums.ReservationAvailability;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static com.wanted.preonboarding.core.domain.response.ResponseHandler.MESSAGE_SUCCESS;
import static com.wanted.preonboarding.core.domain.response.ResponseHandler.createResponse;
import static com.wanted.preonboarding.ticket.application.common.exception.ExceptionStatus.*;
import static com.wanted.preonboarding.ticket.application.common.util.CodeGenerator.generateRandomCode;
import static com.wanted.preonboarding.ticket.domain.enums.ReservationAvailability.AVAILABLE;
import static com.wanted.preonboarding.ticket.domain.enums.ReservationAvailability.OCCUPIED;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationService {
    // 책임 : 예약 진행 및 취소, 예약 내역 조회
    public static final int RESERVATION_CODE_LENGTH = 6;

    private final PaymentService paymentService;
    private final ReservationRepository reservationRepository;
    private final PerformanceSeatInfoRepository seatInfoRepository;
    private final ApplicationEventPublisher eventPublisher;
    @Transactional(readOnly = true)
    public ResponseEntity<ResponseHandler<ReservationResponse>> getReservationInfo(String code) {
        log.info("--- Get Reservation Info ---");

        Reservation reservation = getReservationEntity(code);
        Performance performance = reservation.getPerformanceSeatInfo().getPerformance();

        return createResponse(HttpStatus.OK, MESSAGE_SUCCESS, ReservationResponse.of(reservation, performance));
    }
    @Transactional
    public ResponseEntity<ResponseHandler<ReservationResponse>> proceedReservation(
            RequestReservation requestReservation
    ) {
        log.info("--- Proceed Reservation ---");
        PerformanceSeatInfo seatInfo = getSeatInfoEntity(requestReservation);
        Performance performance = seatInfo.getPerformance();
        Reservation reservation = Reservation.of(requestReservation, seatInfo, createUniqueCode());

        checkPerformanceAvailability(performance);
        checkSeatAvailability(seatInfo);
        processReservation(reservation, seatInfo);
        PaymentResponse paymentResponse = paymentService.processPayment(reservation, requestReservation.balance());
        return createResponse(HttpStatus.CREATED, MESSAGE_SUCCESS, ReservationResponse.of(reservation, performance, paymentResponse));
    }

    @Transactional
    public ResponseEntity<ResponseHandler<Void>> cancelReservation(String code) {
        log.info("--- Cancel Reservation ---");
        Reservation reservation = getReservationEntity(code);
        PerformanceSeatInfo seatInfo = reservation.getPerformanceSeatInfo();

        processCancellation(reservation, seatInfo);
        return createResponse(HttpStatus.OK, MESSAGE_SUCCESS, null);
    }

    // ========== PRIVATE METHODS ========== //

    private void processReservation(Reservation reservation, PerformanceSeatInfo seatInfo) {
        reservationRepository.save(reservation);
        seatInfo.modifyReservationAvailability(OCCUPIED);
    }

    private void processCancellation(Reservation reservation, PerformanceSeatInfo seatInfo) {
        reservationRepository.delete(reservation);
        seatInfo.modifyReservationAvailability(AVAILABLE);
        eventPublisher.publishEvent(new ReservationCancelledEvent(this, reservation));
    }

    private Reservation getReservationEntity(String code) {
        return reservationRepository.findByCode(code)
                .orElseThrow(() -> new EntityNotFoundException(NOT_FOUND_INFO));
    }

    private PerformanceSeatInfo getSeatInfoEntity(RequestReservation requestReservation) {
        UUID id = requestReservation.performanceId();
        Integer round = requestReservation.round();
        Character line = requestReservation.line();
        Integer seat = requestReservation.seat();

        return seatInfoRepository.findByPerformanceIdAndRoundAndLineAndSeat(id, round, String.valueOf(line), seat)
                .orElseThrow(() -> new EntityNotFoundException(NOT_FOUND_INFO));
    }

    private String createUniqueCode() {
        String code;
        do {
            code = generateRandomCode(RESERVATION_CODE_LENGTH);
        } while (reservationRepository.existsByCode(code));

        return code;
    }

    private void validateNameAndPhoneNumber(String name, String phone) {
        String nameRegex = "^[가-힣]{2,4}$";
        String phoneRegex = "^01(?:0|1|[6-9])-?(?:\\d{3}|\\d{4})-?\\d{4}$";

        if (!name.matches(nameRegex) || !phone.matches(phoneRegex)) {
            throw new ArgumentNotValidException(ARGUMENT_NOT_VALID);
        }
    }

    private void checkPerformanceAvailability(Performance performance) {
        if (performance.getIsReserve().equals(ReservationAvailability.DISABLED)) {
            throw new ServiceFailedException(PERFORMANCE_DISABLED);
        }
    }

    private void checkSeatAvailability(PerformanceSeatInfo seatInfo) {
        if (seatInfo.getIsReserve().equals(OCCUPIED)) {
            throw new SeatNotAvailableException(SEAT_ALREADY_OCCUPIED);
        }
        if (seatInfo.getIsReserve().equals(ReservationAvailability.DISABLED)) {
            throw new SeatNotAvailableException(SEAT_DISABLED);
        }
    }

}
