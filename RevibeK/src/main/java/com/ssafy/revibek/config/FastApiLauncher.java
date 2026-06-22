package com.ssafy.revibek.config;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;

@Slf4j
@Component
public class FastApiLauncher {

    @Value("${fastapi.project.path:../RevibeK_AI}")
    private String projectPath;

    @Value("${fastapi.launcher.enabled:false}")
    private boolean launcherEnabled;

    private Process process;

    @PostConstruct
    public void start() {
        if (!launcherEnabled) {
            log.info("FastAPI 자동 실행 비활성화");
            return;
        }

        new Thread(() -> {
            try {
                File workDir = new File(projectPath).getAbsoluteFile();
                File venvPython = new File(workDir, ".venv/Scripts/python.exe");
                String python = venvPython.exists() ? venvPython.getAbsolutePath() : "python";

                ProcessBuilder pb = new ProcessBuilder(
                    python, "-m", "uvicorn", "app.main:app",
                    "--host", "0.0.0.0", "--port", "8000"
                );
                pb.directory(workDir);
                pb.inheritIO();
                process = pb.start();
                log.info("FastAPI 서버 시작 ({})", workDir);
                process.waitFor();
            } catch (IOException | InterruptedException e) {
                log.error("FastAPI 서버 시작 실패: {}", e.getMessage());
            }
        }, "fastapi-launcher").start();
    }

    @PreDestroy
    public void stop() {
        if (process != null && process.isAlive()) {
            process.destroy();
            log.info("FastAPI 서버 종료");
        }
    }
}
