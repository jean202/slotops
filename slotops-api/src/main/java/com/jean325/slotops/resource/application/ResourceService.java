package com.jean325.slotops.resource.application;

import com.jean325.slotops.location.domain.Location;
import com.jean325.slotops.location.domain.LocationRepository;
import com.jean325.slotops.resource.domain.Resource;
import com.jean325.slotops.resource.domain.ResourceImage;
import com.jean325.slotops.resource.domain.ResourceImageRepository;
import com.jean325.slotops.resource.domain.ResourceRepository;
import com.jean325.slotops.resource.dto.CreateResourceRequest;
import com.jean325.slotops.resource.dto.ResourceResponse;
import com.jean325.slotops.resource.dto.UpdateResourceRequest;
import com.jean325.slotops.organization.domain.Organization;
import com.jean325.slotops.organization.domain.OrganizationRepository;
import com.jean325.slotops.global.error.ApiException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ResourceService {
    private final ResourceRepository resourceRepository;
    private final ResourceImageRepository imageRepository;
    private final LocationRepository locationRepository;
    private final OrganizationRepository organizationRepository;

    public ResourceService(
        ResourceRepository resourceRepository,
        ResourceImageRepository imageRepository,
        LocationRepository locationRepository,
        OrganizationRepository organizationRepository
    ) {
        this.resourceRepository = resourceRepository;
        this.imageRepository = imageRepository;
        this.locationRepository = locationRepository;
        this.organizationRepository = organizationRepository;
    }

    public ResourceResponse createResource(
        Long organizationId,
        Long locationId,
        CreateResourceRequest request
    ) {
        Organization org = organizationRepository.findById(organizationId)
            .orElseThrow(() -> new ApiException(
                "ORGANIZATION_NOT_FOUND",
                "Organization not found",
                HttpStatus.NOT_FOUND
            ));

        Location location = locationRepository.findByIdAndOrganizationId(locationId, organizationId)
            .orElseThrow(() -> new ApiException(
                "LOCATION_NOT_FOUND",
                "Location not found",
                HttpStatus.NOT_FOUND
            ));

        Resource resource = new Resource(
            org,
            location,
            request.resourceType(),
            request.name(),
            request.capacity(),
            request.slotUnitMinutes(),
            request.minBookingMinutes(),
            request.maxBookingMinutes(),
            request.bufferMinutes(),
            request.basePrice(),
            request.currency()
        );

        if (request.description() != null) {
            resource.setDescription(request.description());
        }

        Resource savedResource = resourceRepository.save(resource);

        // Add images if provided
        if (request.imageUrls() != null && !request.imageUrls().isEmpty()) {
            for (int i = 0; i < request.imageUrls().size(); i++) {
                ResourceImage image = new ResourceImage(savedResource, request.imageUrls().get(i), i);
                imageRepository.save(image);
            }
        }

        return mapToResponse(savedResource);
    }

    public ResourceResponse getResource(Long organizationId, Long locationId, Long resourceId) {
        Resource resource = resourceRepository.findByIdAndLocationId(resourceId, locationId)
            .orElseThrow(() -> new ApiException(
                "RESOURCE_NOT_FOUND",
                "Resource not found",
                HttpStatus.NOT_FOUND
            ));

        if (!resource.getOrganizationId().equals(organizationId)) {
            throw new ApiException(
                "FORBIDDEN",
                "Access denied to this resource",
                HttpStatus.FORBIDDEN
            );
        }

        return mapToResponse(resource);
    }

    public List<ResourceResponse> listResources(Long organizationId, Long locationId) {
        // Verify location belongs to organization
        locationRepository.findByIdAndOrganizationId(locationId, organizationId)
            .orElseThrow(() -> new ApiException(
                "LOCATION_NOT_FOUND",
                "Location not found",
                HttpStatus.NOT_FOUND
            ));

        return resourceRepository.findByLocationId(locationId)
            .stream()
            .map(this::mapToResponse)
            .toList();
    }

    public ResourceResponse updateResource(
        Long organizationId,
        Long locationId,
        Long resourceId,
        UpdateResourceRequest request
    ) {
        Resource resource = resourceRepository.findByIdAndLocationId(resourceId, locationId)
            .orElseThrow(() -> new ApiException(
                "RESOURCE_NOT_FOUND",
                "Resource not found",
                HttpStatus.NOT_FOUND
            ));

        if (!resource.getOrganizationId().equals(organizationId)) {
            throw new ApiException(
                "FORBIDDEN",
                "Access denied to this resource",
                HttpStatus.FORBIDDEN
            );
        }

        if (request.name() != null) {
            resource.setName(request.name());
        }
        if (request.description() != null) {
            resource.setDescription(request.description());
        }
        if (request.capacity() != null) {
            resource.setCapacity(request.capacity());
        }
        if (request.basePrice() != null) {
            resource.setBasePrice(request.basePrice());
        }
        if (request.status() != null) {
            resource.setStatus(request.status());
        }

        Resource savedResource = resourceRepository.save(resource);
        return mapToResponse(savedResource);
    }

    public void deleteResource(Long organizationId, Long locationId, Long resourceId) {
        Resource resource = resourceRepository.findByIdAndLocationId(resourceId, locationId)
            .orElseThrow(() -> new ApiException(
                "RESOURCE_NOT_FOUND",
                "Resource not found",
                HttpStatus.NOT_FOUND
            ));

        if (!resource.getOrganizationId().equals(organizationId)) {
            throw new ApiException(
                "FORBIDDEN",
                "Access denied to this resource",
                HttpStatus.FORBIDDEN
            );
        }

        imageRepository.deleteByResourceId(resourceId);
        resourceRepository.delete(resource);
    }

    private ResourceResponse mapToResponse(Resource resource) {
        List<String> imageUrls = imageRepository.findByResourceIdOrderBySortOrder(resource.getId())
            .stream()
            .map(ResourceImage::getImageUrl)
            .toList();

        return new ResourceResponse(
            resource.getId(),
            resource.getOrganizationId(),
            resource.getLocationId(),
            resource.getResourceType(),
            resource.getName(),
            resource.getDescription(),
            resource.getCapacity(),
            resource.getSlotUnitMinutes(),
            resource.getMinBookingMinutes(),
            resource.getMaxBookingMinutes(),
            resource.getBufferMinutes(),
            resource.getBasePrice(),
            resource.getCurrency(),
            resource.getStatus(),
            imageUrls,
            resource.getCreatedAt(),
            resource.getUpdatedAt()
        );
    }
}
