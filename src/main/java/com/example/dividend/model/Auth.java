package com.example.dividend.model;

import lombok.Data;
import org.apache.catalina.LifecycleState;

import java.lang.reflect.Member;
import java.util.List;

public class Auth {

    @Data
    public static class SingIn {
        private String username;
        private String password;
    }

    @Data
    public static class SignUp {
        private String username;
        private String password;
        private List<String> roles;

        public MemberEntity toEntity() {
            return MemberEntity.builder()
                    .username(this.username)
                    .password(this.password)
                    .roles(this.roles)
                    .build();
        }
    }

}
