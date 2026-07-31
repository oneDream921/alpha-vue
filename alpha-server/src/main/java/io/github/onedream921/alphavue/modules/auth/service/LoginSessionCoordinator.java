package io.github.onedream921.alphavue.modules.auth.service;

import java.util.function.Supplier;

/**
 * 串行化同一用户同一客户端的会话创建，避免并发登录产生双活 token。
 */
public interface LoginSessionCoordinator {

    <T> T execute(long userId, String clientId, Supplier<T> action);
}
