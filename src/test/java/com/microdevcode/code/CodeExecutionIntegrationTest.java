package com.microdevcode.code;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microdevcode.code.dto.CodeSubmissionRequest;
import com.microdevcode.code.entity.Framework;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureTestMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureTestMvc
class CodeExecutionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testGetSupportedLanguages() throws Exception {
        mockMvc.perform(get("/api/v1/code/languages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.languages").isArray());
    }

    @Test
    void testGetSupportedFrameworks() throws Exception {
        mockMvc.perform(get("/api/v1/code/frameworks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.frameworks").isArray());
    }

    @Test
    void testCompileSimpleJavaCode() throws Exception {
        // Create a simple Java code submission
        Map<String, String> code = new HashMap<>();
        code.put("HelloWorld.java", 
            "public class HelloWorld {\n" +
            "    public static void main(String[] args) {\n" +
            "        System.out.println(\"Hello, World!\");\n" +
            "    }\n" +
            "}");

        CodeSubmissionRequest request = new CodeSubmissionRequest();
        request.setProblemId(1L);
        request.setFramework(Framework.SPRING_BOOT);
        request.setLanguage("java");
        request.setCode(code);

        String requestJson = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/api/v1/code/compile")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").exists())
                .andExpect(jsonPath("$.status").exists());
    }

    @Test
    void testCompileSimplePythonCode() throws Exception {
        // Create a simple Python code submission
        Map<String, String> code = new HashMap<>();
        code.put("main.py", "print('Hello, World!')");

        CodeSubmissionRequest request = new CodeSubmissionRequest();
        request.setProblemId(1L);
        request.setFramework(Framework.FAST_API);
        request.setLanguage("python");
        request.setCode(code);

        String requestJson = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/api/v1/code/compile")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").exists())
                .andExpect(jsonPath("$.status").exists());
    }

    @Test
    void testCompileWithMissingCode() throws Exception {
        CodeSubmissionRequest request = new CodeSubmissionRequest();
        request.setProblemId(1L);
        request.setFramework(Framework.SPRING_BOOT);
        request.setLanguage("java");
        // No code provided

        String requestJson = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/api/v1/code/compile")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Code submission is required"));
    }

    @Test
    void testRunWithMissingProblemId() throws Exception {
        Map<String, String> code = new HashMap<>();
        code.put("main.py", "print('Hello, World!')");

        CodeSubmissionRequest request = new CodeSubmissionRequest();
        // No problem ID provided
        request.setFramework(Framework.FAST_API);
        request.setLanguage("python");
        request.setCode(code);

        String requestJson = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/api/v1/code/run")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Problem ID is required"));
    }
}