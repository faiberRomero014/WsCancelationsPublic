package com.app.WsCancelations.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
public class RetryScheduler {

    //Controla que no haya ejecuciones duplicadas y permite reintentar tareas después de un tiempo.

    private final ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
    private final AtomicBoolean ejecucionEnCurso = new AtomicBoolean(false);

    public RetryScheduler() {
        scheduler.setPoolSize(2);
        scheduler.initialize();
    }

    public boolean marcarInicio() {
        return ejecucionEnCurso.compareAndSet(false, true);
    }

    public void marcarFin() {
        ejecucionEnCurso.set(false);
    }

    public void scheduleRetry(Runnable task, long delayMinutes) {
        scheduler.schedule(task,
                new Date(System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(delayMinutes)));
    }

    public boolean hayEjecucionEnCurso() {
        return ejecucionEnCurso.get();
    }
}
