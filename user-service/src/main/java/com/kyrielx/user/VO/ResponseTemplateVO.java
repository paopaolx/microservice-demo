package com.kyrielx.user.VO;

import com.kyrielx.user.entity.Users;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author lixing
 * @date 2022/10/8
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResponseTemplateVO {
    private Users user;
    private Department department;
}
