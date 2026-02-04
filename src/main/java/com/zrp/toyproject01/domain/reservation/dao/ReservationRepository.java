package com.zrp.toyproject01.domain.reservation.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import com.zrp.toyproject01.domain.reservation.domain.Reservation;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    
}
