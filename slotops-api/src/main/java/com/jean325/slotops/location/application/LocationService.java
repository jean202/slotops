package com.jean325.slotops.location.application;

import com.jean325.slotops.location.domain.Location;
import com.jean325.slotops.location.domain.LocationRepository;
import com.jean325.slotops.location.domain.LocationStatus;
import com.jean325.slotops.location.dto.CreateLocationRequest;
import com.jean325.slotops.location.dto.LocationResponse;
import com.jean325.slotops.location.dto.UpdateLocationRequest;
import com.jean325.slotops.organization.domain.Organization;
import com.jean325.slotops.organization.domain.OrganizationRepository;
import com.jean325.slotops.global.error.ApiException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class LocationService {
    private final LocationRepository locationRepository;
    private final OrganizationRepository organizationRepository;

    public LocationService(
        LocationRepository locationRepository,
        OrganizationRepository organizationRepository
    ) {
        this.locationRepository = locationRepository;
        this.organizationRepository = organizationRepository;
    }

    public LocationResponse createLocation(Long organizationId, CreateLocationRequest request, Long userId) {
        Organization org = organizationRepository.findById(organizationId)
            .orElseThrow(() -> new ApiException(
                "ORGANIZATION_NOT_FOUND",
                "Organization not found",
                HttpStatus.NOT_FOUND
            ));

        Location location = new Location(org, request.name(), request.timezone());
        if (request.phone() != null) {
            location.setPhone(request.phone());
        }
        if (request.addressLine1() != null) {
            location.setAddressLine1(request.addressLine1());
        }
        if (request.addressLine2() != null) {
            location.setAddressLine2(request.addressLine2());
        }

        Location savedLocation = locationRepository.save(location);
        return mapToResponse(savedLocation);
    }

    public LocationResponse getLocation(Long organizationId, Long locationId) {
        Location location = locationRepository.findByIdAndOrganizationId(locationId, organizationId)
            .orElseThrow(() -> new ApiException(
                "LOCATION_NOT_FOUND",
                "Location not found",
                HttpStatus.NOT_FOUND
            ));

        return mapToResponse(location);
    }

    public List<LocationResponse> listLocations(Long organizationId) {
        return locationRepository.findByOrganizationId(organizationId)
            .stream()
            .map(this::mapToResponse)
            .toList();
    }

    public LocationResponse updateLocation(
        Long organizationId,
        Long locationId,
        UpdateLocationRequest request
    ) {
        Location location = locationRepository.findByIdAndOrganizationId(locationId, organizationId)
            .orElseThrow(() -> new ApiException(
                "LOCATION_NOT_FOUND",
                "Location not found",
                HttpStatus.NOT_FOUND
            ));

        if (request.name() != null) {
            location.setName(request.name());
        }
        if (request.phone() != null) {
            location.setPhone(request.phone());
        }
        if (request.addressLine1() != null) {
            location.setAddressLine1(request.addressLine1());
        }
        if (request.addressLine2() != null) {
            location.setAddressLine2(request.addressLine2());
        }
        if (request.status() != null) {
            location.setStatus(request.status());
        }

        Location savedLocation = locationRepository.save(location);
        return mapToResponse(savedLocation);
    }

    public void deleteLocation(Long organizationId, Long locationId) {
        Location location = locationRepository.findByIdAndOrganizationId(locationId, organizationId)
            .orElseThrow(() -> new ApiException(
                "LOCATION_NOT_FOUND",
                "Location not found",
                HttpStatus.NOT_FOUND
            ));

        locationRepository.delete(location);
    }

    private LocationResponse mapToResponse(Location location) {
        return new LocationResponse(
            location.getId(),
            location.getOrganization().getId(),
            location.getName(),
            location.getTimezone(),
            location.getPhone(),
            location.getAddressLine1(),
            location.getAddressLine2(),
            location.getStatus(),
            location.getCreatedAt(),
            location.getUpdatedAt()
        );
    }
}
