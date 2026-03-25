package com.systemforge.backend.user.mapper;

import com.systemforge.backend.user.dto.response.UserResponse;
import com.systemforge.backend.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * MapStruct mapper for User → UserResponse.
 */
@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(source = "id", target = "id")
    @Mapping(source = "name", target = "name")
    @Mapping(source = "email", target = "email")
    @Mapping(source = "role", target = "role")
    @Mapping(source = "accountStatus", target = "accountStatus")
    @Mapping(source = "authProvider", target = "authProvider")
    @Mapping(source = "emailVerified", target = "emailVerified")
    @Mapping(source = "lastLoginAt", target = "lastLoginAt")
    @Mapping(source = "createdAt", target = "createdAt")
    @Mapping(source = "updatedAt", target = "updatedAt")
    UserResponse toDto(User user);

    default Instant toInstant(LocalDateTime value) {
        return value == null ? null : value.toInstant(ZoneOffset.UTC);
    }

    default LocalDateTime toLocalDateTime(Instant value) {
        return value == null ? null : LocalDateTime.ofInstant(value, ZoneOffset.UTC);
    }
}