package com.sqisoft.reservation.api.support;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.sqisoft.reservation.support.FixedClockConfig;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * 모든 MockMvc 통합 테스트의 부모.
 * 개별 클래스에 @SpringBootTest 를 직접 붙이지 않는다 — 컨텍스트 캐시를 1개로 유지한다 (C-3).
 * ObjectMapper 는 주입하지 않는다 — 요청 바디는 텍스트 블록으로 작성한다 (§0.7, Jackson 3 회피).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(FixedClockConfig.class)
@Transactional
public abstract class ApiTestSupport {

    @Autowired
    protected MockMvc mockMvc;

    @PersistenceContext
    protected EntityManager em;
}
