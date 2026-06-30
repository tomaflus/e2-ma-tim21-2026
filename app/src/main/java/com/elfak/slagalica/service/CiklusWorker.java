package com.elfak.slagalica.service;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.firebase.auth.FirebaseAuth;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class CiklusWorker extends Worker {

    public CiklusWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            return Result.success(); // Korisnik nije ulogovan — preskoči
        }

        CountDownLatch latch = new CountDownLatch(1);
        new RewardService().obradiPrirodniKrajCiklusa(getApplicationContext(), latch::countDown);

        try {
            // Čekamo max 45s da Firebase async pozivi završe
            latch.await(45, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            return Result.retry();
        }

        return Result.success();
    }
}
