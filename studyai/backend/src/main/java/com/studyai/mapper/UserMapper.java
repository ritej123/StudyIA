package com.studyai.mapper;

import com.studyai.dto.response.AdminUserResponse;
import com.studyai.dto.response.UserProfileResponse;
import com.studyai.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

/**
 * Mapper MapStruct pour les entités et DTOs utilisateur.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserMapper {

    @Mapping(target = "photoUrl", source = "photoUrl")
    UserProfileResponse toProfileResponse(User user);

    AdminUserResponse toAdminResponse(User user);
}
