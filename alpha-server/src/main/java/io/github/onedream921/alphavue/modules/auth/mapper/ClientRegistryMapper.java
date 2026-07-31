package io.github.onedream921.alphavue.modules.auth.mapper;

import io.github.onedream921.alphavue.modules.auth.service.ClientRegistryService.Client;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ClientRegistryMapper {

    Client selectEnabledByClientId(@Param("clientId") String clientId);
}
