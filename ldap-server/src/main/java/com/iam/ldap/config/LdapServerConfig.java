package com.iam.ldap.config;

import com.iam.ldap.authenticator.OracleAuthenticator;
import com.iam.ldap.partition.OraclePartition;
import org.apache.directory.server.core.authn.AuthenticationInterceptor;
import org.apache.directory.server.core.authn.SimpleAuthenticator;
import org.apache.directory.server.core.api.interceptor.Interceptor;
import org.apache.directory.api.ldap.model.entry.DefaultModification;
import org.apache.directory.api.ldap.model.entry.ModificationOperation;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.factory.DefaultDirectoryServiceFactory;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.server.protocol.shared.transport.TcpTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.util.Collections;

/**
 * Khởi động ApacheDS embedded — tương đương TomcatServletWebServerFactory trong Spring Web.
 *
 * Thứ tự tạo bean (Spring đảm bảo):
 *   1. directoryService()  — lõi LDAP: load schema, tạo thư mục làm việc
 *   2. ldapServer()        — protocol handler: bind TCP port 10389
 *
 * Thứ tự destroy (LIFO — ngược với tạo):
 *   1. ldapServer.stop()           — ngừng nhận connection mới
 *   2. directoryService.shutdown() — đóng schema + partition an toàn
 */
@Configuration
public class LdapServerConfig {

    private static final Logger log = LoggerFactory.getLogger(LdapServerConfig.class);

    @Value("${ldap.server.port:10389}")
    private int port;

    @Value("${ldap.server.suffix:dc=iam,dc=bank,dc=vn}")
    private String suffix;

    @Value("${ldap.server.work-dir:${java.io.tmpdir}/apacheds-iam}")
    private String workDir;

    @Value("${ldap.server.allow-anonymous:false}")
    private boolean allowAnonymous;

    @Value("${ldap.server.max-size-limit:500}")
    private int maxSizeLimit;

    @Value("${ldap.server.admin.password:secret}")
    private String adminPassword;

    /**
     * DirectoryService — lõi xử lý LDAP.
     *
     * DefaultDirectoryServiceFactory.init() thực hiện:
     *   1. Đọc System.property("workingDirectory") → tạo thư mục ${workDir}/iam-ldap/
     *   2. Load toàn bộ LDAP schema từ classpath bên trong apacheds-all.jar
     *   3. Gọi service.startup() — sẵn sàng nhận request
     */
    @Bean(destroyMethod = "shutdown")
    DirectoryService directoryService(OraclePartition oraclePartition,
                                      OracleAuthenticator oracleAuthenticator) throws Exception {

        new File(workDir).mkdirs();

        // Factory đọc property này để xác định instance directory → ${workDir}/iam-ldap/
        System.setProperty("workingDirectory", workDir);

        DefaultDirectoryServiceFactory factory = new DefaultDirectoryServiceFactory();
        factory.init("iam-ldap");   // ← load schema + gọi startup() tự động

        System.clearProperty("workingDirectory");

        DirectoryService service = factory.getDirectoryService();

        // Tắt access control — ta tự kiểm soát qua OracleAuthenticator
        service.setAccessControlEnabled(false);
        service.setAllowAnonymousAccess(allowAnonymous);

        // Không cần change log cho backend read-only
        service.getChangeLog().setEnabled(false);

        // Trả về operational attributes đã được normalize (createTimestamp, v.v.)
        service.setDenormalizeOpAttrsEnabled(true);

        // Override mật khẩu admin mặc định của ApacheDS bằng giá trị từ config
        updateAdminPassword(service, adminPassword);

        // Gắn OraclePartition vào DirectoryService
        // suffixDn phải được set trước khi gọi initialize()
        Dn suffixDn = new Dn(service.getSchemaManager(), suffix);
        oraclePartition.setId("iam");
        oraclePartition.setSuffixDn(suffixDn);
        oraclePartition.setSchemaManager(service.getSchemaManager());
        service.addPartition(oraclePartition);

        // Đăng ký OracleAuthenticator vào AuthenticationInterceptor
        // isValid() của OracleAuthenticator chỉ nhận DN thuộc ou=users
        // → Admin BIND (ou=system) vẫn đi qua SimpleAuthenticator mặc định
        oracleAuthenticator.init(service);
        for (Interceptor interceptor : service.getInterceptors()) {
            if (interceptor instanceof AuthenticationInterceptor authInterceptor) {
                var authenticators = new java.util.HashSet<>(authInterceptor.getAuthenticators());
                // Xóa SimpleAuthenticator — nó chặn OracleAuthenticator khi không có userPassword
                // trong entry (hoặc khi có {BCRYPT} mà nó không hiểu → exception sai loại)
                authenticators.removeIf(a -> a instanceof SimpleAuthenticator);
                authenticators.add(oracleAuthenticator);
                authInterceptor.setAuthenticators(
                        authenticators.toArray(new org.apache.directory.server.core.authn.Authenticator[0])
                );
                log.info("OracleAuthenticator registered");
                break;
            }
        }

        log.info("DirectoryService started — workDir={}/iam-ldap, suffix={}", workDir, suffix);
        return service;
    }

    /**
     * LdapServer — LDAP protocol handler (dùng Apache MINA làm NIO layer).
     *
     * Phụ thuộc vào DirectoryService → Spring đảm bảo:
     *   - directoryService tạo trước, ldapServer tạo sau
     *   - ldapServer stop trước, directoryService shutdown sau
     */
    @Bean(destroyMethod = "stop")
    LdapServer ldapServer(DirectoryService directoryService) throws Exception {
        LdapServer server = new LdapServer();
        server.setDirectoryService(directoryService);
        server.setTransports(new TcpTransport(port));
        server.setMaxSizeLimit(maxSizeLimit);

        server.start();
        log.info("LdapServer started — port={}", port);
        return server;
    }

    /**
     * Override mật khẩu admin built-in của ApacheDS bằng giá trị từ config.
     * BCrypt chỉ áp dụng cho user entries qua OracleAuthenticator, không phải admin.
     */
    private void updateAdminPassword(DirectoryService service, String password) {
        try {
            Dn adminDn = service.getDnFactory().create("uid=admin,ou=system");
            service.getAdminSession().modify(
                    adminDn,
                    Collections.singletonList(new DefaultModification(
                            ModificationOperation.REPLACE_ATTRIBUTE,
                            "userPassword", password
                    ))
            );
            log.debug("Admin password synced from config");
        } catch (Exception e) {
            // Có thể xảy ra lần đầu chạy khi schema chưa ổn định — không nghiêm trọng
            log.warn("Could not update admin password (safe on first run): {}", e.getMessage());
        }
    }
}
