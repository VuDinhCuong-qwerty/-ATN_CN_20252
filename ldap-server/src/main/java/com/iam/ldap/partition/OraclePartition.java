package com.iam.ldap.partition;

import com.iam.ldap.converter.EntryConverter;
import com.iam.ldap.model.ApplicationEntry;
import com.iam.ldap.model.UserEntry;
import com.iam.ldap.service.ApplicationService;
import com.iam.ldap.service.UserService;
import org.apache.directory.api.ldap.model.cursor.EmptyCursor;
import org.apache.directory.api.ldap.model.filter.BranchNode;
import org.apache.directory.api.ldap.model.filter.EqualityNode;
import org.apache.directory.api.ldap.model.filter.ExprNode;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.api.ldap.model.cursor.ListCursor;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapUnwillingToPerformException;
import org.apache.directory.api.ldap.model.message.ResultCodeEnum;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.server.core.api.filtering.EntryFilteringCursor;
import org.apache.directory.server.core.api.filtering.EntryFilteringCursorImpl;
import org.apache.directory.server.core.api.interceptor.context.*;
import org.apache.directory.server.core.api.partition.AbstractPartition;
import org.apache.directory.server.core.api.partition.PartitionReadTxn;
import org.apache.directory.server.core.api.partition.PartitionTxn;
import org.apache.directory.server.core.api.partition.PartitionWriteTxn;
import org.apache.directory.server.core.api.partition.Subordinates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.naming.InvalidNameException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Trái tim của LDAP server — thay thế toàn bộ backend lưu trữ bằng Oracle DB.
 *
 * Trong ApacheDS, một Partition là "nơi lưu dữ liệu" cho một subtree cụ thể.
 * Mặc định ApacheDS dùng LMDB/BTree. Ở đây ta thay thế bằng Oracle qua JDBC.
 *
 * Approach B — Service-encoded DN:
 *   GitLab config: base = ou=gitlab-server,ou=users,dc=iam,dc=bank,dc=vn
 *   SEARCH: đọc service code từ base DN → chỉ trả user có quyền gitlab-server
 *   Returned entry DN: uid=CUONGVD,ou=gitlab-server,ou=users,dc=...
 *   BIND: service code đọc từ cùng DN đó → enforce quyền đúng user
 *
 * Cây DN được xử lý:
 *   dc=iam,dc=bank,dc=vn                  ← root
 *   └── ou=users                           ← virtual OU
 *   │   └── ou=gitlab-server               ← virtual service OU (nếu app ACTIVE)
 *   │       └── uid={username}             ← user có AUTH_APP_PERMISSION cho gitlab-server
 *   └── ou=groups                          ← virtual OU
 *         └── cn={serviceCode}             ← group entry
 */
@Component
public class OraclePartition extends AbstractPartition {

    private static final Logger log = LoggerFactory.getLogger(OraclePartition.class);

    private final UserService userService;
    private final ApplicationService applicationService;
    private final EntryConverter entryConverter;

    @Value("${ldap.server.suffix:dc=iam,dc=bank,dc=vn}")
    private String suffix;

    public OraclePartition(UserService userService,
                           ApplicationService applicationService,
                           EntryConverter entryConverter) {
        this.userService = userService;
        this.applicationService = applicationService;
        this.entryConverter = entryConverter;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  Lifecycle — ApacheDS gọi khi add partition vào DirectoryService
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * ApacheDS gọi doInit() sau khi set schemaManager và suffixDn.
     * Oracle connection đã được Spring/HikariCP quản lý → không cần init thêm.
     */
    @Override
    protected void doInit() throws InvalidNameException, LdapException {
        log.info("OraclePartition initialized — suffix={}", suffixDn);
    }

    /**
     * ApacheDS gọi doDestroy() khi DirectoryService shutdown.
     * HikariCP tự đóng connection pool → không cần cleanup thêm.
     */
    @Override
    protected void doDestroy(PartitionTxn partitionTxn) throws LdapException {
        log.info("OraclePartition destroyed");
    }

    @Override
    protected void doRepair() throws LdapException {
        // Không áp dụng cho Oracle backend
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  Transaction — ApacheDS yêu cầu partition tự quản lý transaction
    //  Oracle transaction được quản lý bởi Spring JDBC → trả về object rỗng
    // ═══════════════════════════════════════════════════════════════════════════

    @Override
    public PartitionReadTxn beginReadTransaction() {
        return new PartitionReadTxn();
    }

    @Override
    public PartitionWriteTxn beginWriteTransaction() {
        return new PartitionWriteTxn();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  READ operations — đây là phần chính cần implement
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * SEARCH — GitLab gọi để tìm user hoặc liệt kê group.
     *
     * Phân nhánh dựa vào base DN của request:
     *   ou=users,... → truy vấn AUTH_USER (qua searchUsers)
     *   ou=groups,...→ truy vấn AUTH_APPLICATION (qua searchGroups)
     *   root DN      → trả về ou=users + ou=groups (searchRoot)
     */
    @Override
    public EntryFilteringCursor search(SearchOperationContext ctx) throws LdapException {
        Dn base = ctx.getDn();
        String baseDn = base.toString().toLowerCase();

        log.debug("SEARCH base='{}' filter='{}' scope={}",
                base, ctx.getFilter(), ctx.getScope());

        if (baseDn.contains("ou=users")) {
            return searchUsers(ctx);
        } else if (baseDn.contains("ou=groups")) {
            return searchGroups(ctx);
        } else {
            return searchRoot(ctx);
        }
    }

    /**
     * Tìm user — service code đọc từ base DN (Approach B).
     *
     * Logic:
     *   1. extractServiceCodeFromBase(base) → serviceCode hoặc null
     *   2. scope == OBJECT: base DN là user entry cụ thể
     *      - nếu RDN type là "uid": findByUsernameAndService hoặc findByUsername
     *      - nếu RDN type là "ou" (OU entry): trả empty — OU không phải user
     *   3. scope == SUBTREE / ONE_LEVEL: list toàn bộ users trong OU
     *      - filter parsing bỏ qua (GitLab CE truyền filter đơn giản)
     *      - findAllByService(serviceCode) nếu có, findAll() nếu không
     *   4. Build mỗi entry với service-encoded DN khi serviceCode != null
     */
    private EntryFilteringCursor searchUsers(SearchOperationContext ctx) throws LdapException {
        Dn base = ctx.getDn();
        log.debug("searchUsers — base='{}' scope={}", base, ctx.getScope());

        // 1. Đọc service code từ base DN path
        // dn có dnagj uid=<<username>>,ou=<<serviceCode>>,ou=users,dc=iam,dc=bank,dc=vn
        String serviceCode = extractServiceCodeFromBase(base);
        log.debug("searchUsers — serviceCode='{}'", serviceCode);

        List<UserEntry> users;
        try {
            if (ctx.getScope() == SearchScope.OBJECT) {
                // 2. scope=base: base có thể là user DN hoặc OU DN
                String rdnType = base.getRdn().getType();
                if (!"uid".equalsIgnoreCase(rdnType)) {
                    // Base là OU (ou=gitlab-server,...) — không có user trực tiếp tại node này
                    users = List.of();
                } else {
                    String username = base.getRdn().getValue().toUpperCase();
                    log.debug("searchUsers — scope=base, uid='{}'", username);
                    if (serviceCode != null) {
                        users = userService.findByUsernameAndService(username, serviceCode)
                                .map(List::of).orElse(List.of());
                    } else {
                        users = userService.findByUsername(username)
                                .map(List::of).orElse(List.of());
                    }
                }
            } else {
                // 3. scope=SUBTREE / ONE_LEVEL: extract uid từ filter để query Oracle targeted.
                // EntryFilteringCursorImpl KHÔNG apply filter lên ListCursor — phải xử lý server-side.
                String uidFromFilter = extractUidFromFilter(ctx.getFilter());
                if (uidFromFilter != null && serviceCode != null) {
                    users = userService.findByUsernameAndService(uidFromFilter.toUpperCase(), serviceCode)
                            .map(List::of).orElse(List.of());
                } else if (uidFromFilter != null) {
                    users = userService.findByUsername(uidFromFilter.toUpperCase())
                            .map(List::of).orElse(List.of());
                } else if (serviceCode != null) {
                    users = userService.findAllByService(serviceCode);
                } else {
                    users = userService.findAll();
                }
            }
        } catch (Exception e) {
            log.error("searchUsers — DB query failed: {}", e.getMessage(), e);
            return emptyFilteringCursor(ctx);
        }

        // 4. Build LDAP entries với service-encoded DN + permission attribute
        List<Entry> entries = new ArrayList<>();
        for (UserEntry user : users) {
            try {
                userService.enrichWithPermissions(user);
                Entry entry = entryConverter.toUserEntry(user, schemaManager, serviceCode);
                entries.add(entry);
            } catch (Exception e) {
                log.error("searchUsers — failed to convert user '{}': {}", user.getUsername(), e.getMessage(), e);
            }
        }
        log.debug("searchUsers — returning {} entries", entries.size());
        return new EntryFilteringCursorImpl(new ListCursor<>(entries), ctx, schemaManager);
    }

    /**
     * Liệt kê application (group).
     *
     * Logic:
     *   1. scope=base: lấy đúng 1 group theo cn trong base DN
     *   2. scope=sub/one: trả toàn bộ application ACTIVE
     *      Filter parsing bỏ qua — GitLab CE không dùng required_groups (EE feature)
     */
    private EntryFilteringCursor searchGroups(SearchOperationContext ctx) throws LdapException {
        log.debug("searchGroups — scope={}", ctx.getScope());

        List<ApplicationEntry> apps;

        if (ctx.getScope() == SearchScope.OBJECT) {
            // 1. scope=base: GitLab muốn đúng 1 group theo DN
            String cn = ctx.getDn().getRdn().getValue();
            log.debug("searchGroups — scope=base, cn='{}'", cn);
            Optional<ApplicationEntry> appOpt = applicationService.findByServiceCode(cn);
            apps = appOpt.map(List::of).orElse(List.of());
        } else {
            // 2. scope=sub/one: trả toàn bộ groups
            apps = applicationService.findAll();
        }

        List<Entry> entries = new ArrayList<>();
        for (ApplicationEntry app : apps) {
            entries.add(entryConverter.toApplicationEntry(app, schemaManager));
        }
        log.debug("searchGroups — returning {} entries", entries.size());
        return new EntryFilteringCursorImpl(new ListCursor<>(entries), ctx, schemaManager);
    }

    /**
     * LOOKUP — tìm một entry cụ thể theo DN đầy đủ.
     *
     * Phân nhánh theo RDN type (phần đầu tiên bên trái của DN):
     *   dc=...  → root entry
     *   uid=... → user entry: extract service code từ DN path → findByUsernameAndService / findByUsername
     *   cn=...  → group entry: findByServiceCode
     *   ou=...  → virtual OU:
     *             "users" / "groups" → luôn tồn tại
     *             other              → virtual service OU, tồn tại nếu app ACTIVE
     */
    @Override
    public Entry lookup(LookupOperationContext ctx) throws LdapException {
        Dn dn = ctx.getDn();
        log.debug("LOOKUP dn='{}'", dn);

        String rdnType  = dn.getRdn().getType().toLowerCase();
        String rdnValue = dn.getRdn().getValue();

        if ("dc".equals(rdnType)) {
            // 1. Root entry: dc=iam,dc=bank,dc=vn
            return buildRootEntry();

        } else if ("uid".equals(rdnType)) {
            // 2. User entry — đọc service code từ DN path
            String username = rdnValue.toUpperCase();
            String serviceCode = extractServiceFromUserDn(dn);
            log.debug("LOOKUP uid='{}' serviceCode='{}'", username, serviceCode);

            Optional<UserEntry> userOpt;
            if (serviceCode != null) {
                userOpt = userService.findByUsernameAndService(username, serviceCode);
            } else {
                userOpt = userService.findByUsername(username);
            }
            if (userOpt.isEmpty()) return null;
            UserEntry user = userOpt.get();
            userService.enrichWithPermissions(user);
            return entryConverter.toUserEntry(user, schemaManager, serviceCode);

        } else if ("cn".equals(rdnType)) {
            // 3. Group entry: cn=gitlab-server,ou=groups,...
            Optional<ApplicationEntry> appOpt = applicationService.findByServiceCode(rdnValue);
            if (appOpt.isEmpty()) return null;
            return entryConverter.toApplicationEntry(appOpt.get(), schemaManager);

        } else if ("ou".equals(rdnType)) {
            // 4. Virtual OU entry
            String ouLower = rdnValue.toLowerCase();
            if ("users".equals(ouLower) || "groups".equals(ouLower)) {
                // ou=users và ou=groups luôn tồn tại
                return buildVirtualOuEntry(dn, rdnValue);
            } else {
                // Virtual service OU: ou=gitlab-server,ou=users,...
                // Tồn tại nếu SERVICE_CODE có trong AUTH_APPLICATION ACTIVE
                if (applicationService.existsByServiceCode(rdnValue)) {
                    return buildVirtualOuEntry(dn, rdnValue);
                }
                return null;
            }
        }

        return null;
    }

    /**
     * HAS_ENTRY — kiểm tra DN có tồn tại không.
     * ApacheDS gọi trước BIND để xác nhận user DN hợp lệ.
     *
     * Phân nhánh theo RDN type — tương tự lookup() nhưng chỉ check tồn tại,
     * không build Entry (tối ưu hiệu suất).
     *
     *   dc=...  → true (root luôn tồn tại)
     *   uid=... → findByUsernameAndService / findByUsername → isPresent()
     *   cn=...  → existsByServiceCode
     *   ou=...  → "users"/"groups" luôn true
     *             other → kiểm tra parent là ou=users rồi existsByServiceCode
     */
    @Override
    public boolean hasEntry(HasEntryOperationContext ctx) throws LdapException {
        Dn dn = ctx.getDn();
        log.debug("HAS_ENTRY dn='{}'", dn);

        String rdnType  = dn.getRdn().getType().toLowerCase();
        String rdnValue = dn.getRdn().getValue();

        if ("dc".equals(rdnType)) {
            // 1. Root partition entry luôn tồn tại
            return true;

        } else if ("uid".equals(rdnType)) {
            // 2. User entry — kiểm tra theo service code trong DN nếu có
            String username = rdnValue.toUpperCase();
            String serviceCode = extractServiceFromUserDn(dn);
            if (serviceCode != null) {
                return userService.findByUsernameAndService(username, serviceCode).isPresent();
            } else {
                return userService.findByUsername(username).isPresent();
            }

        } else if ("cn".equals(rdnType)) {
            // 3. Group entry: tồn tại nếu SERVICE_CODE có trong AUTH_APPLICATION ACTIVE
            return applicationService.existsByServiceCode(rdnValue);

        } else if ("ou".equals(rdnType)) {
            // 4. Virtual OU
            String ouLower = rdnValue.toLowerCase();
            if ("users".equals(ouLower) || "groups".equals(ouLower)) {
                // ou=users và ou=groups luôn tồn tại
                return true;
            }
            // Virtual service OU: ou=gitlab-server,ou=users,...
            // Verify parent là ou=users để tránh nhầm với OU khác có cùng tên
            try {
                Dn parent = dn.getParent();
                if (parent != null) {
                    String parentValue = parent.getRdn().getValue();
                    if ("users".equalsIgnoreCase(parentValue)) {
                        return applicationService.existsByServiceCode(rdnValue);
                    }
                }
            } catch (Exception e) {
                log.warn("hasEntry: cannot check parent for dn '{}': {}", dn, e.getMessage());
            }
            return false;
        }

        return false;
    }

    /**
     * Trả về số lượng children và subordinates của một entry.
     * ApacheDS dùng để xây dựng cây thư mục.
     */
    @Override
    public Subordinates getSubordinates(PartitionTxn txn, Entry entry) throws LdapException {
        return new Subordinates();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  CSN (Change Sequence Number) — không dùng cho backend read-only
    // ═══════════════════════════════════════════════════════════════════════════

    @Override
    public void saveContextCsn(PartitionTxn partitionTxn) throws LdapException {
        // no-op
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  WRITE operations — server này READ-ONLY, mọi thay đổi qua IAM API
    // ═══════════════════════════════════════════════════════════════════════════

    @Override
    public void add(AddOperationContext ctx) throws LdapException {
        throw new LdapUnwillingToPerformException(ResultCodeEnum.UNWILLING_TO_PERFORM,
                "LDAP server is read-only. Use IAM API to manage users.");
    }

    @Override
    public Entry delete(DeleteOperationContext ctx) throws LdapException {
        throw new LdapUnwillingToPerformException(ResultCodeEnum.UNWILLING_TO_PERFORM,
                "LDAP server is read-only. Use IAM API to manage users.");
    }

    @Override
    public void modify(ModifyOperationContext ctx) throws LdapException {
        throw new LdapUnwillingToPerformException(ResultCodeEnum.UNWILLING_TO_PERFORM,
                "LDAP server is read-only. Use IAM API to manage users.");
    }

    @Override
    public void rename(RenameOperationContext ctx) throws LdapException {
        throw new LdapUnwillingToPerformException(ResultCodeEnum.UNWILLING_TO_PERFORM,
                "LDAP server is read-only. Use IAM API to manage users.");
    }

    @Override
    public void move(MoveOperationContext ctx) throws LdapException {
        throw new LdapUnwillingToPerformException(ResultCodeEnum.UNWILLING_TO_PERFORM,
                "LDAP server is read-only. Use IAM API to manage users.");
    }

    @Override
    public void moveAndRename(MoveAndRenameOperationContext ctx) throws LdapException {
        throw new LdapUnwillingToPerformException(ResultCodeEnum.UNWILLING_TO_PERFORM,
                "LDAP server is read-only. Use IAM API to manage users.");
    }

    @Override
    public void unbind(UnbindOperationContext ctx) throws LdapException {
        // UNBIND = client ngắt kết nối — không cần xử lý gì
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  Helper — DN parsing (Approach B)
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Đọc service code từ base DN của SEARCH request.
     *
     * Xử lý 2 dạng base DN:
     *   A. base = ou=gitlab-server,ou=users,dc=...        (SUBTREE/ONE_LEVEL search)
     *      → service code = base.getRdn().getValue() = "gitlab-server"
     *   B. base = uid=CUONGVD,ou=gitlab-server,ou=users,dc=...  (scope=OBJECT)
     *      → service code nằm ở parent: base.getParent().getRdn().getValue() = "gitlab-server"
     *
     * Logic:
     *   1. RDN type là "uid" (case B) → đọc service code từ parent RDN
     *   2. RDN type là "ou" (case A):
     *      - value là "users" / "groups" → return null (OU gốc)
     *      - verify parent là ou=users → return value
     *   Ngược lại: return null
     */
    private String extractServiceCodeFromBase(Dn base) {
        try {
            if (base == null) return null;
            String rdnType = base.getRdn().getType();

            // 1. Case B: base = uid=X,ou=SERVICE,ou=users,...
            //    service code nằm ở parent RDN — dùng lại extractServiceFromUserDn
            if ("uid".equalsIgnoreCase(rdnType)) {
                return extractServiceFromUserDn(base);
            }

            // 2. Case A: base = ou=SERVICE,ou=users,...
            if (!"ou".equalsIgnoreCase(rdnType)) return null;

            String ouValue = base.getRdn().getValue();
            if ("users".equalsIgnoreCase(ouValue) || "groups".equalsIgnoreCase(ouValue)) {
                return null;
            }

            // Verify parent là ou=users
            Dn parent = base.getParent();
            if (parent == null) return null;
            String parentType  = parent.getRdn().getType();
            String parentValue = parent.getRdn().getValue();
            if ("ou".equalsIgnoreCase(parentType) && "users".equalsIgnoreCase(parentValue)) {
                return ouValue;
            }
            return null;
        } catch (Exception e) {
            log.warn("extractServiceCodeFromBase: cannot parse base DN '{}': {}", base, e.getMessage());
            return null;
        }
    }

    /**
     * Đọc service code từ user entry DN.
     *
     * Logic:
     *   1. Lấy parent DN (direct parent OU của user)
     *   2. Parent RDN type phải là "ou"
     *   3. Parent RDN value khác "users" / "groups" → đây là service OU
     *   4. Return parent RDN value (service code)
     *   Ngược lại: return null
     *
     * Ví dụ:
     *   uid=CUONGVD,ou=gitlab-server,ou=users,dc=... → "gitlab-server"
     *   uid=CUONGVD,ou=users,dc=...                  → null (nằm thẳng trong ou=users)
     */
    private String extractServiceFromUserDn(Dn dn) {
        // 1. Lấy parent RDN (direct parent OU của user entry)
        try {
            if (dn == null) return null;
            Dn parent = dn.getParent();
            if (parent == null) return null;
            String parentType  = parent.getRdn().getType();
            String parentValue = parent.getRdn().getValue();

            // 2. Parent phải là "ou" không phải "users" / "groups"
            if ("ou".equalsIgnoreCase(parentType)
                    && !"users".equalsIgnoreCase(parentValue)
                    && !"groups".equalsIgnoreCase(parentValue)) {
                return parentValue;
            }
            return null;
        } catch (Exception e) {
            log.warn("extractServiceFromUserDn: cannot parse DN '{}': {}", dn, e.getMessage());
            return null;
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  Helper — Entry builders
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Root search: base = dc=iam,dc=bank,dc=vn.
     *
     * Phân nhánh theo scope để tránh vòng lặp vô hạn trong Directory Studio:
     *   scope=BASE      → trả về chính root entry (Studio load node)
     *   scope=ONE_LEVEL → trả về ou=users + ou=groups (Studio expand node)
     *   scope=SUBTREE   → trả về ou=users + ou=groups
     */
    private EntryFilteringCursor searchRoot(SearchOperationContext ctx) throws LdapException {
        List<Entry> entries = new ArrayList<>();

        if (ctx.getScope() == SearchScope.OBJECT) {
            entries.add(buildRootEntry());
        } else {
            DefaultEntry usersOu = new DefaultEntry(schemaManager,
                    new Dn(schemaManager, "ou=users," + suffix));
            usersOu.add("objectClass", "top", "organizationalUnit");
            usersOu.add("ou", "users");
            entries.add(usersOu);

            DefaultEntry groupsOu = new DefaultEntry(schemaManager,
                    new Dn(schemaManager, "ou=groups," + suffix));
            groupsOu.add("objectClass", "top", "organizationalUnit");
            groupsOu.add("ou", "groups");
            entries.add(groupsOu);
        }

        return new EntryFilteringCursorImpl(new ListCursor<>(entries), ctx, schemaManager);
    }

    /**
     * Tạo root entry cho partition: dc=iam,dc=bank,dc=vn.
     * ApacheDS cần entry này để xác nhận partition đã sẵn sàng.
     */
    private DefaultEntry buildRootEntry() throws LdapException {
        DefaultEntry entry = new DefaultEntry(schemaManager, suffixDn);
        entry.add("objectClass", "top", "domain");
        entry.add("dc", suffixDn.getRdn().getValue());
        return entry;
    }

    /**
     * Tạo virtual OU entry — không lưu trong Oracle, tồn tại theo cấu hình.
     * Dùng cho ou=users, ou=groups, và virtual service OUs (ou=gitlab-server,...).
     */
    private DefaultEntry buildVirtualOuEntry(Dn dn, String ouValue) throws LdapException {
        DefaultEntry entry = new DefaultEntry(schemaManager, dn);
        entry.add("objectClass", "top", "organizationalUnit");
        entry.add("ou", ouValue);
        return entry;
    }

    /**
     * Tìm giá trị uid trong LDAP filter tree (đệ quy).
     * ES LDAP realm gửi filter dạng (|(uid=CUONGNC1)(objectClass=referral)).
     * EntryFilteringCursorImpl không apply filter → phải extract uid server-side.
     */
    private String extractUidFromFilter(ExprNode filter) {
        if (filter == null) return null;
        if (filter instanceof EqualityNode<?> eq) {
            if ("uid".equalsIgnoreCase(eq.getAttribute())) {
                return eq.getValue().getString();
            }
        } else if (filter instanceof BranchNode branch) {
            for (ExprNode child : branch.getChildren()) {
                String uid = extractUidFromFilter(child);
                if (uid != null) return uid;
            }
        }
        return null;
    }

    /** Tạo cursor rỗng để trả về khi không có kết quả hoặc DB lỗi. */
    private EntryFilteringCursor emptyFilteringCursor(SearchOperationContext ctx) {
        return new EntryFilteringCursorImpl(
                new EmptyCursor<>(),
                ctx,
                schemaManager
        );
    }
}
