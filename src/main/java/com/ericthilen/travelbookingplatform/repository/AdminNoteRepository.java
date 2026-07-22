package com.ericthilen.travelbookingplatform.repository;

import com.ericthilen.travelbookingplatform.model.AdminNote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AdminNoteRepository
        extends JpaRepository<AdminNote, Long> {

    List<AdminNote> findAllByBookingIdOrderByCreatedAtDesc(Long bookingId);
}
