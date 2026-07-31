package io.github.onedream921.alphavue.modules.auth.service;

import io.github.onedream921.alphavue.common.exception.BusinessException;
import io.github.onedream921.alphavue.common.exception.PublicErrorMessage;
import io.github.onedream921.alphavue.modules.auth.mapper.ClientRegistryMapper;
import org.springframework.stereotype.Service;

/**
 * 登录客户端注册表。
 */
@Service
public class ClientRegistryService {

    private final ClientRegistryMapper mapper;

    public ClientRegistryService(ClientRegistryMapper mapper) {
        this.mapper = mapper;
    }

    public Client requireEnabled(String clientId) {
        Client client = mapper.selectEnabledByClientId(clientId);
        if (client == null) {
            throw new BusinessException(400, PublicErrorMessage.INVALID_REQUEST);
        }
        return client;
    }

    public record Client(String clientId, String name) {
    }
}
