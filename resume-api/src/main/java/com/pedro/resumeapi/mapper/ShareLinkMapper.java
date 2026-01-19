package com.pedro.resumeapi.mapper;

import com.pedro.resumeapi.domain.ShareLink;
import com.pedro.resumeapi.dto.ShareLinkDTO;

import java.util.UUID;

public class ShareLinkMapper {

    public static ShareLinkDTO toDTO(ShareLink link) {
        UUID createdById = (link.getCreatedBy() == null) ? null : link.getCreatedBy().getId();
        return new ShareLinkDTO(
                link.getId(),
                link.getPermission(),
                link.getExpiresAt(),
                link.getRevokedAt(),
                link.getMaxUses(),
                link.getUseCount(),
                link.getCreatedAt(),
                createdById
        );
    }
}
