package com.family.schedule.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.family.schedule.service.NotFoundException;
import com.family.schedule.service.ScheduleService;
import com.family.schedule.service.ValidationException;
import com.family.schedule.web.dto.ScheduleRequest;
import com.family.schedule.web.dto.ScheduleResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ScheduleController.class)
class ScheduleControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean ScheduleService service;

    @Test
    void get_正常() throws Exception {
        when(service.findRange(LocalDate.of(2026, 4, 24), LocalDate.of(2026, 4, 25)))
                .thenReturn(List.of(new ScheduleResponse(1L, 3, "長女", LocalDate.of(2026, 4, 24), "部活")));
        mvc.perform(get("/api/schedules").param("from", "2026-04-24").param("to", "2026-04-25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].memberName").value("長女"))
                .andExpect(jsonPath("$[0].content").value("部活"));
    }

    @Test
    void get_from未指定は400() throws Exception {
        mvc.perform(get("/api/schedules").param("to", "2026-04-25"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void post_正常() throws Exception {
        when(service.create(any())).thenReturn(
                new ScheduleResponse(100L, 3, "長女", LocalDate.of(2026, 4, 25), "塾"));
        String body = objectMapper.writeValueAsString(
                new ScheduleRequest(3, LocalDate.of(2026, 4, 25), "塾"));
        mvc.perform(post("/api/schedules").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/schedules/100"))
                .andExpect(jsonPath("$.id").value(100));
    }

    @Test
    void post_バリデーションで400() throws Exception {
        when(service.create(any())).thenThrow(new ValidationException(
                "入力に誤りがあります", Map.of("content", "内容を入力してください")));
        String body = objectMapper.writeValueAsString(
                new ScheduleRequest(3, LocalDate.of(2026, 4, 25), ""));
        mvc.perform(post("/api/schedules").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION"))
                .andExpect(jsonPath("$.fields.content").value("内容を入力してください"));
    }

    @Test
    void put_存在しないID_は404() throws Exception {
        when(service.update(eq(999L), any())).thenThrow(new NotFoundException("schedule not found: 999"));
        String body = objectMapper.writeValueAsString(
                new ScheduleRequest(1, LocalDate.now(), "foo"));
        mvc.perform(put("/api/schedules/999").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    void delete_正常() throws Exception {
        mvc.perform(delete("/api/schedules/1")).andExpect(status().isNoContent());
    }

    @Test
    void delete_存在しないIDは404() throws Exception {
        doThrow(new NotFoundException("schedule not found: 999")).when(service).delete(999L);
        mvc.perform(delete("/api/schedules/999")).andExpect(status().isNotFound());
    }

    // BUG-1: Content-Type 不正 → 415
    @Test
    void post_ContentType不正は415() throws Exception {
        mvc.perform(post("/api/schedules").contentType(MediaType.TEXT_PLAIN).content("{}"))
                .andExpect(status().isUnsupportedMediaType());
    }

    // BUG-2: 未対応 HTTP メソッド → 405 + Allow ヘッダ
    @Test
    void patch_未対応メソッドは405() throws Exception {
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .patch("/api/schedules/1").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().exists("Allow"));
    }

    // BUG-3: 型変換エラー → 400 + 内部メッセージ非露出
    @Test
    void put_IDが文字列は400_内部メッセージ非露出() throws Exception {
        String body = objectMapper.writeValueAsString(new ScheduleRequest(1, LocalDate.now(), "foo"));
        mvc.perform(put("/api/schedules/abc").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("リクエストパラメータが不正です"));
    }

    // BUG-5: JSON 構文エラー → 400 + 内部メッセージ非露出
    @Test
    void post_不正JSON_内部メッセージ非露出() throws Exception {
        mvc.perform(post("/api/schedules").contentType(MediaType.APPLICATION_JSON).content("{not json}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("リクエストパラメータが不正です"));
    }

    // BL-16: POST /restore 正常
    @Test
    void restore_正常() throws Exception {
        when(service.restore(10L)).thenReturn(
                new ScheduleResponse(10L, 1, "お父さん", LocalDate.now(), "在宅"));
        mvc.perform(post("/api/schedules/10/restore"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10));
    }

    // BL-16: POST /restore 存在しない（未削除 or 削除なし）→ 404
    @Test
    void restore_存在しないIDは404() throws Exception {
        when(service.restore(999L)).thenThrow(new NotFoundException("schedule not found: 999"));
        mvc.perform(post("/api/schedules/999/restore"))
                .andExpect(status().isNotFound());
    }

    // BL-16: POST /purge 正常
    @Test
    void purge_正常() throws Exception {
        mvc.perform(post("/api/schedules/10/purge"))
                .andExpect(status().isNoContent());
    }

    // BL-16: POST /purge 存在しない → 404
    @Test
    void purge_存在しないIDは404() throws Exception {
        doThrow(new NotFoundException("schedule not found: 999")).when(service).purge(999L);
        mvc.perform(post("/api/schedules/999/purge"))
                .andExpect(status().isNotFound());
    }
}
