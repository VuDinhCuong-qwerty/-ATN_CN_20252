package com.iam.auth.engine;

import com.iam.auth.dto.request.AuthorizeRequest;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthSession {
    private String sessionId;               // id để duy trì trạng thái trung gian
    private String orgRequestUri;           // đối tượng tạm thời chưa sử dụng
    private Long clientId;                  // id client
    private Long flowId;                    // id cây xác thực (mỗi client có 1 cây kèm theo)
    private Long userId;                    // id nsd
    private Long arcLevel;
    private String ssoSession;
    private Long appId;
    private Long preNodeId;
    private Long currentNodeId;            // method xác thực cho phiên hiện tại
    private Map<Long, String> nodeStatus;
//    private List<Long> currentSiblingIds; // các phương thức xác thực clietn có thể chọn thay thế cái hiện tại
    private AuthorizeRequest authorizeRequest;
    private LocalDateTime preparedAt;
    private LocalDateTime expiredAt;
}
