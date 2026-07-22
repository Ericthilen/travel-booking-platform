package com.ericthilen.travelbookingplatform.repository;

import com.ericthilen.travelbookingplatform.dto.TravelSearchFilters;
import com.ericthilen.travelbookingplatform.model.Departure;
import com.ericthilen.travelbookingplatform.model.ManagementStatus;
import com.ericthilen.travelbookingplatform.model.Travel;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class TravelSearchRepositoryImpl implements TravelSearchRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<Travel> searchTravels(TravelSearchFilters filters) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Travel> query = builder.createQuery(Travel.class);
        Root<Travel> travel = query.from(Travel.class);
        List<Predicate> predicates = new ArrayList<>();

        predicates.add(activeTravelPredicate(
                builder,
                travel
        ));

        addTextPredicate(
                builder,
                predicates,
                travel.get("destination"),
                filters.getDestination()
        );
        addExactTextPredicate(
                builder,
                predicates,
                travel.get("country"),
                filters.getCountry()
        );
        addExactTextPredicate(
                builder,
                predicates,
                travel.get("mealType"),
                filters.getMealType()
        );

        if (filters.getMaxPrice() != null) {
            predicates.add(builder.lessThanOrEqualTo(
                    travel.get("price"),
                    filters.getMaxPrice()
            ));
        }

        if (filters.getHotelStars() != null) {
            predicates.add(builder.greaterThanOrEqualTo(
                    travel.get("hotelStars"),
                    filters.getHotelStars()
            ));
        }

        if (filters.getNights() != null) {
            predicates.add(builder.equal(
                    travel.get("nights"),
                    filters.getNights()
            ));
        }

        if (filters.isPool()) {
            predicates.add(keywordPredicate(
                    builder,
                    query,
                    travel,
                    "pool"
            ));
        }

        if (filters.isBeach()) {
            predicates.add(keywordPredicate(
                    builder,
                    query,
                    travel,
                    "strand"
            ));
        }

        if (filters.isFamily()) {
            predicates.add(builder.or(
                    keywordPredicate(
                            builder,
                            query,
                            travel,
                            "familj"
                    ),
                    keywordPredicate(
                            builder,
                            query,
                            travel,
                            "barn"
                    )
            ));
        }

        if (needsDepartureMatch(filters)) {
            predicates.add(departureExistsPredicate(
                    builder,
                    query,
                    travel,
                    filters
            ));
        }

        query
                .select(travel)
                .distinct(true)
                .where(predicates.toArray(Predicate[]::new))
                .orderBy(sortOrder(
                        builder,
                        query,
                        travel,
                        filters
                ));

        return entityManager
                .createQuery(query)
                .getResultList();
    }

    private Predicate activeTravelPredicate(
            CriteriaBuilder builder,
            Root<Travel> travel
    ) {
        return builder.or(
                builder.equal(
                        travel.get("status"),
                        ManagementStatus.ACTIVE
                ),
                builder.isNull(travel.get("status"))
        );
    }

    private void addTextPredicate(
            CriteriaBuilder builder,
            List<Predicate> predicates,
            Expression<String> field,
            String value
    ) {
        String cleanedValue = clean(value);

        if (cleanedValue.isBlank()) {
            return;
        }

        predicates.add(builder.like(
                builder.lower(field),
                "%" + cleanedValue.toLowerCase() + "%"
        ));
    }

    private void addExactTextPredicate(
            CriteriaBuilder builder,
            List<Predicate> predicates,
            Expression<String> field,
            String value
    ) {
        String cleanedValue = clean(value);

        if (cleanedValue.isBlank()) {
            return;
        }

        predicates.add(builder.equal(
                builder.lower(field),
                cleanedValue.toLowerCase()
        ));
    }

    private Predicate keywordPredicate(
            CriteriaBuilder builder,
            CriteriaQuery<Travel> query,
            Root<Travel> travel,
            String keyword
    ) {
        Subquery<Long> facilityQuery = query.subquery(Long.class);
        Root<Travel> facilityTravel = facilityQuery.from(Travel.class);
        Expression<String> facility = facilityTravel
                .join("facilities")
                .as(String.class);
        String match = "%" + keyword.toLowerCase() + "%";

        facilityQuery
                .select(facilityTravel.get("id"))
                .where(
                        builder.equal(
                                facilityTravel.get("id"),
                                travel.get("id")
                        ),
                        builder.like(
                                builder.lower(facility),
                                match
                        )
                );

        return builder.or(
                builder.like(
                        builder.lower(travel.get("description")),
                        match
                ),
                builder.exists(facilityQuery)
        );
    }

    private boolean needsDepartureMatch(TravelSearchFilters filters) {
        return !clean(filters.getDepartureAirport()).isBlank()
                || filters.getEarliestDepartureDate() != null
                || filters.getLatestDepartureDate() != null
                || filters.isOnlyAvailable()
                || filters.getTravelers() != null;
    }

    private Predicate departureExistsPredicate(
            CriteriaBuilder builder,
            CriteriaQuery<Travel> query,
            Root<Travel> travel,
            TravelSearchFilters filters
    ) {
        Subquery<Long> departureQuery = query.subquery(Long.class);
        Root<Departure> departure = departureQuery.from(Departure.class);
        List<Predicate> predicates = departurePredicates(
                builder,
                departure,
                travel,
                filters
        );

        departureQuery
                .select(departure.get("id"))
                .where(predicates.toArray(Predicate[]::new));

        return builder.exists(departureQuery);
    }

    private List<Predicate> departurePredicates(
            CriteriaBuilder builder,
            Root<Departure> departure,
            Root<Travel> travel,
            TravelSearchFilters filters
    ) {
        List<Predicate> predicates = new ArrayList<>();

        predicates.add(builder.equal(
                departure.get("travel").get("id"),
                travel.get("id")
        ));
        predicates.add(builder.or(
                builder.equal(
                        departure.get("status"),
                        ManagementStatus.ACTIVE
                ),
                builder.isNull(departure.get("status"))
        ));

        String departureAirport = clean(filters.getDepartureAirport());
        if (!departureAirport.isBlank()) {
            predicates.add(builder.equal(
                    builder.lower(departure.get("departureAirport")),
                    departureAirport.toLowerCase()
            ));
        }

        LocalDate earliestDate = filters.getEarliestDepartureDate();
        if (earliestDate != null) {
            predicates.add(builder.greaterThanOrEqualTo(
                    departure.get("departureDate"),
                    earliestDate
            ));
        }

        LocalDate latestDate = filters.getLatestDepartureDate();
        if (latestDate != null) {
            predicates.add(builder.lessThanOrEqualTo(
                    departure.get("departureDate"),
                    latestDate
            ));
        }

        Integer travelers = filters.getTravelers();
        if (filters.isOnlyAvailable()) {
            predicates.add(builder.greaterThan(
                    departure.get("availableSeats"),
                    0
            ));
        }

        if (travelers != null) {
            predicates.add(builder.greaterThanOrEqualTo(
                    departure.get("availableSeats"),
                    travelers
            ));
        }

        return predicates;
    }

    private List<Order> sortOrder(
            CriteriaBuilder builder,
            CriteriaQuery<Travel> query,
            Root<Travel> travel,
            TravelSearchFilters filters
    ) {
        String sort = clean(filters.getSort());
        List<Order> orders = new ArrayList<>();

        if ("price-low".equals(sort)) {
            orders.add(builder.asc(travel.get("price")));
        } else if ("price-high".equals(sort)) {
            orders.add(builder.desc(travel.get("price")));
        } else if ("earliest-departure".equals(sort)) {
            orders.add(builder.asc(earliestDepartureDate(
                    builder,
                    query,
                    travel,
                    filters
            )));
        } else if ("hotel-rating".equals(sort)) {
            orders.add(builder.desc(travel.get("hotelStars")));
        }

        orders.add(builder.asc(travel.get("destination")));
        orders.add(builder.asc(travel.get("hotelName")));

        return orders;
    }

    private Subquery<LocalDate> earliestDepartureDate(
            CriteriaBuilder builder,
            CriteriaQuery<Travel> query,
            Root<Travel> travel,
            TravelSearchFilters filters
    ) {
        Subquery<LocalDate> departureQuery = query.subquery(LocalDate.class);
        Root<Departure> departure = departureQuery.from(Departure.class);
        List<Predicate> predicates = departurePredicates(
                builder,
                departure,
                travel,
                filters
        );

        departureQuery
                .select(builder.least(departure.<LocalDate>get("departureDate")))
                .where(predicates.toArray(Predicate[]::new));

        return departureQuery;
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
